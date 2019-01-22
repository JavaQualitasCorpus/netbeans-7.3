/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.glassfish.javaee;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.deploy.shared.ActionType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;
import org.netbeans.api.server.ServerInstance;
import org.netbeans.modules.glassfish.eecommon.api.Utils;
import org.netbeans.modules.glassfish.javaee.ide.Hk2DeploymentStatus;
import org.netbeans.modules.glassfish.javaee.ide.Hk2PluginProperties;
import org.netbeans.modules.glassfish.javaee.ui.DebugPortQuery;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.netbeans.modules.j2ee.deployment.plugins.api.ServerDebugInfo;
import org.netbeans.modules.j2ee.deployment.plugins.spi.StartServer;
import org.netbeans.modules.glassfish.spi.GlassfishModule;
import org.netbeans.modules.glassfish.spi.GlassfishModule.OperationState;
import org.netbeans.modules.glassfish.spi.GlassfishModule3;
import org.netbeans.modules.glassfish.spi.OperationStateListener;
import org.netbeans.modules.j2ee.deployment.profiler.api.ProfilerSupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 *
 * @author Ludovic Champenois
 * @auther Peter Williams
 * @author vince kraemer
 */
public class Hk2StartServer extends StartServer implements ProgressObject {
   
    private DeploymentStatus deploymentStatus;
    private Hk2DeploymentManager dm;
    private String serverName;
    private List<ProgressListener> listeners =
            new CopyOnWriteArrayList<ProgressListener>();
    private InstanceProperties ip;
    
    public Hk2StartServer(DeploymentManager jdm) {
        if (!(jdm instanceof Hk2DeploymentManager)) {
            throw new IllegalArgumentException("Only GlassFish v3 is supported"); //NOI18N
        }
        this.dm = (Hk2DeploymentManager) jdm;
        this.ip = dm.getInstanceProperties();
        if (null != ip) {
            this.serverName = ip.getProperty(GlassfishModule.DISPLAY_NAME_ATTR);
        }
    }
    
    public InstanceProperties getInstanceProperties() {
        return ip;
    }
    
    private GlassfishModule getCommonServerSupport() {
        ServerInstance si = dm.getServerInstance();
        return si.getBasicNode().getLookup().lookup(GlassfishModule.class);
    }
    
    @Override
    public boolean supportsStartDeploymentManager() {
        // a local instance always supports starting the deployment manager
        // a remote instance supports start deployment manager via the restart-domain command
        GlassfishModule commonSupport = getCommonServerSupport();
        assert commonSupport != null : "commonSupport is null??";
        if (null == commonSupport) {
            Logger.getLogger("glassfish-javaee").log(Level.WARNING, "commonSupport is null??");
            return false;
        }
        boolean local = !commonSupport.isRemote();
        return local ? local : isRunning() || GlassfishModule.ServerState.STARTING.equals(commonSupport.getServerState());
    }

    // start server
    @Override
    public ProgressObject startDeploymentManager() {
        if(ProfilerSupport.getState() == ProfilerSupport.STATE_BLOCKING) {
            fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                    CommandType.START, StateType.FAILED, ActionType.EXECUTE,
                    NbBundle.getMessage(Hk2StartServer.class, "MSG_SERVER_PROFILING_IN_PROGRESS", serverName) // NOI18N
                    ));
        } else {
            fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                    CommandType.START, StateType.RUNNING, ActionType.EXECUTE,
                    NbBundle.getMessage(Hk2StartServer.class, "MSG_START_SERVER_IN_PROGRESS", serverName) // NOI18N
                    ));
            GlassfishModule commonSupport = getCommonServerSupport();
            if(commonSupport != null && !commonSupport.isRemote()) {
                commonSupport.setEnvironmentProperty(GlassfishModule.JVM_MODE, GlassfishModule.NORMAL_MODE, true);
                commonSupport.startServer(new OperationStateListener() {
                    @Override
                    public void operationStateChanged(OperationState newState, String message) {
                        fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                                CommandType.START, translateState(newState), ActionType.EXECUTE,
                                message));
                    }
                }, GlassfishModule.ServerState.RUNNING);
            } else if (commonSupport != null) { // this is the remote case
                commonSupport.setEnvironmentProperty(GlassfishModule.JVM_MODE, GlassfishModule.NORMAL_MODE, true);
                commonSupport.restartServer(new OperationStateListener() {
                    @Override
                    public void operationStateChanged(OperationState newState, String message) {
                        fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                                CommandType.START, translateState(newState), ActionType.EXECUTE,
                                message));
                    }
                });
            }
        }
        return this;
    }
    
    @Override
    public ProgressObject stopDeploymentManager() {
        fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                CommandType.STOP, StateType.RUNNING, ActionType.EXECUTE,
                NbBundle.getMessage(Hk2StartServer.class, "MSG_STOP_SERVER_IN_PROGRESS", serverName) // NOI18N
                ));
        GlassfishModule commonSupport = getCommonServerSupport();
        if(commonSupport != null && !commonSupport.isRemote()) {
            commonSupport.stopServer(new OperationStateListener() {
                @Override
                public void operationStateChanged(OperationState newState, String message) {
                    fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                            CommandType.STOP, translateState(newState), ActionType.EXECUTE, 
                            message));
                }
            });
        } else if (null != commonSupport) { // this is the remote case
            // we lie, since a start is going to happen right after this
            fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                    CommandType.STOP, StateType.COMPLETED, ActionType.EXECUTE,
                    NbBundle.getMessage(Hk2StartServer.class, "MSG_SERVER_STOPPED", serverName) // NOI18N
                    ));
        }
        return this;
    }
    
    private static StateType translateState(OperationState commonState) {
        switch(commonState) {
            case RUNNING:
                return StateType.RUNNING;
            case COMPLETED:
                return StateType.COMPLETED;
            case FAILED:
                return StateType.FAILED;
        }
        // Should never happen, but we have to return something.  UNKNOWN state
        // would be convenient, but again, this should never happen.
        return StateType.FAILED;
    }
    
    @Override
    public boolean supportsStartDebugging(Target target) {
        GlassfishModule commonSupport = getCommonServerSupport();
        assert null != commonSupport : "commonSupport is null?"; // NOI18N
        if (null == commonSupport) {
            Logger.getLogger("glassfish-javaee").log(Level.WARNING, "commonSupport is null??"); // NOI18N
            return false;
        }
        boolean retVal = supportsStartDeploymentManager() && !isClusterOrInstance(commonSupport);
        return retVal;
    }

    @Override
    public ProgressObject startDebugging(Target target) {
        if (ProfilerSupport.getState() == ProfilerSupport.STATE_BLOCKING) {
            fireHandleProgressEvent(null,new Hk2DeploymentStatus(
                    CommandType.START, StateType.FAILED, ActionType.EXECUTE,
                    NbBundle.getMessage(Hk2StartServer.class, "MSG_SERVER_PROFILING_IN_PROGRESS", serverName) // NOI18N
                    ));
        } else {
            fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                    CommandType.START, StateType.RUNNING, ActionType.EXECUTE,
                    NbBundle.getMessage(Hk2StartServer.class, "MSG_START_SERVER_IN_PROGRESS", serverName) // NOI18N
                    ));
            final GlassfishModule commonSupport = getCommonServerSupport();
            if(commonSupport != null && !commonSupport.isRemote()) {
                commonSupport.setEnvironmentProperty(GlassfishModule.JVM_MODE, GlassfishModule.DEBUG_MODE, true);
                commonSupport.startServer(new OperationStateListener() {
                    @Override
                    public void operationStateChanged(OperationState newState, String message) {
                        fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                                CommandType.START, translateState(newState), ActionType.EXECUTE,
                                message));
                    }
                }, GlassfishModule.ServerState.RUNNING);
            } else if  (null != commonSupport) { // this is the remote case
                commonSupport.setEnvironmentProperty(GlassfishModule.JVM_MODE, GlassfishModule.DEBUG_MODE, true);
                commonSupport.restartServer(new OperationStateListener() {
                    @Override
                    public void operationStateChanged(OperationState newState, String message) {
                        if (OperationState.COMPLETED.equals(newState)) {
                            try {
                                Thread.sleep(1000);
                                while (GlassfishModule.ServerState.STARTING.equals(commonSupport.getServerState())) {
                                    Thread.sleep(500);
                                }
                            } catch (InterruptedException ie) {
                                Logger.getLogger("glassfish-javaee").log(Level.INFO,"",ie);
                            }
                        }
                        fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                                CommandType.START, translateState(newState), ActionType.EXECUTE,
                                message));
                    }
                });
            }
        }
        return this;
    }
    
    @Override
    public boolean isDebuggable(Target target) {
        GlassfishModule commonSupport = getCommonServerSupport();
        if (!isRunning()) {
            return false;
        }
        if(commonSupport != null) {
                return GlassfishModule.DEBUG_MODE.equals(
                    commonSupport.getInstanceProperties().get(GlassfishModule.JVM_MODE));
        }
        return false;
    }
    
    @Override
    public boolean isAlsoTargetServer(Target target) {
        return true;
    }
    
    @Override
    public ServerDebugInfo getDebugInfo(Target target) {
        GlassfishModule commonSupport = getCommonServerSupport();
        String debugPort = commonSupport.getInstanceProperties().get(GlassfishModule.DEBUG_PORT);
        ServerDebugInfo retVal = null;
        if(Utils.strEmpty(debugPort) && commonSupport.isRemote()) {
            debugPort = queryDebugPort();
        }
        if(Utils.notEmpty(debugPort)) {
            retVal = new ServerDebugInfo(ip.getProperty(GlassfishModule.HOSTNAME_ATTR), 
                Integer.parseInt(debugPort));
        }
        return retVal;
    }

    private String queryDebugPort() {
        String debugPort = null;
        String name = getCommonServerSupport().getInstanceProperties().get(GlassfishModule.DISPLAY_NAME_ATTR);
        DebugPortQuery debugPortQuery = new DebugPortQuery();
        DialogDescriptor desc = new DialogDescriptor(debugPortQuery, 
                NbBundle.getMessage(Hk2StartServer.class, "TITLE_QueryDebugPort", name)); // NOI18N
        if(DialogDisplayer.getDefault().notify(desc) == NotifyDescriptor.OK_OPTION) {
            debugPort = debugPortQuery.getDebugPort();
            if(debugPortQuery.shouldPersist()) {
                getCommonServerSupport().setEnvironmentProperty(
                        GlassfishModule.DEBUG_PORT, debugPort, true);
            }
        }
        return debugPort;
    }
    
    @Override
    public boolean needsRestart(Target target) {
         return false;
    }

    @Override
    public boolean needsStartForTargetList() {
        return false;
    }
    
    @Override
    public boolean needsStartForConfigure() {
        return false;
    }
    
    @Override
    public boolean needsStartForAdminConfig() {
        return false;
    }

    @Override
    public boolean isRunning() {
        GlassfishModule commonSupport = getCommonServerSupport();
        if(commonSupport != null) {
            GlassfishModule.ServerState s = commonSupport.getServerState();
            return GlassfishModule.ServerState.RUNNING.equals(s) ||
                    GlassfishModule.ServerState.RUNNING_JVM_DEBUG.equals(s) ||
                    GlassfishModule.ServerState.RUNNING_JVM_PROFILER.equals(s);
        } else {
            return Hk2PluginProperties.isRunning(ip.getProperty(GlassfishModule.HOSTNAME_ATTR),
                    ip.getProperty(InstanceProperties.HTTP_PORT_NUMBER));
        }
    }
    
    @Override
    public DeploymentStatus getDeploymentStatus() {
        return deploymentStatus;
    }
    
    @Override
    public TargetModuleID[] getResultTargetModuleIDs() {
        return new TargetModuleID[0];
    }
    
    @Override
    public ClientConfiguration getClientConfiguration(TargetModuleID targetModuleID) {
        return null;
    }
    
    @Override
    public boolean isCancelSupported() {
        return false;
    }
    
    @Override
    public void cancel() throws OperationUnsupportedException {
        assert false : "client called cancel() even though isCancelSupported() returned FALSE.";
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean isStopSupported() {
        return false;
    }
    
    @Override
    public void stop() throws OperationUnsupportedException {
        assert false : "client called stop() even though isStopSupported() returned FALSE.";
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addProgressListener(ProgressListener progressListener) {
        // a new listener should hear what the current status is...
        listeners.add(progressListener);
        if (null != lastEvent) {
            progressListener.handleProgressEvent(lastEvent);
        }
    }
    
    @Override
    public void removeProgressListener(ProgressListener progressListener) {
        listeners.remove(progressListener);
    }

    private ProgressEvent lastEvent = null;
    
    public void fireHandleProgressEvent(TargetModuleID targetModuleID, DeploymentStatus deploymentStatus) {
        lastEvent = new ProgressEvent(this, targetModuleID, deploymentStatus);
        this.deploymentStatus = deploymentStatus;

        Iterator<ProgressListener> iter = listeners.iterator();
        while(iter.hasNext()) {
            iter.next().handleProgressEvent(lastEvent);
        }
    }
    
    @Override
    public boolean supportsStartProfiling(Target target) {
        GlassfishModule commonSupport = getCommonServerSupport();
        assert null != commonSupport : "commonSupport is null?";
        if (null == commonSupport) {
            Logger.getLogger("glassfish-javaee").log(Level.WARNING, "commonSupport is null??");
            return false;
        }
        boolean retVal = !commonSupport.isRemote()  && !isClusterOrInstance(commonSupport);
        return retVal;
    }

    public boolean isProfiling(Target target) {
        return isRunning();
    }

    @Override
    public ProgressObject startProfiling(Target target) {
        if (ProfilerSupport.getState() == ProfilerSupport.STATE_BLOCKING) {
            fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                    CommandType.START, StateType.RUNNING, ActionType.EXECUTE,
                    NbBundle.getMessage(Hk2StartServer.class, "MSG_SERVER_PROFILING_IN_PROGRESS", serverName))); // NOI18N
            return this; //we failed to start the server.
        }

        final GlassfishModule commonSupport = getCommonServerSupport();
        if (commonSupport != null) {
            if (isClusterOrInstance(commonSupport)) {
                fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                    CommandType.START, StateType.FAILED, ActionType.EXECUTE,
                    NbBundle.getMessage(Hk2StartServer.class, "MSG_SERVER_PROFILING_CLUSTER_NOT_SUPPORTED", serverName))); // NOI18N
                return this; //we failed to start the server.
            }
            fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                CommandType.START, StateType.RUNNING, ActionType.EXECUTE,
                NbBundle.getMessage(Hk2StartServer.class, "MSG_START_SERVER_IN_PROGRESS", serverName))); // NOI18N
//            String domainLocation = commonSupport.getInstanceProperties().get(GlassfishModule.DOMAINS_FOLDER_ATTR);
//            String domainName = commonSupport.getInstanceProperties().get(GlassfishModule.DOMAIN_NAME_ATTR);
            commonSupport.setEnvironmentProperty(GlassfishModule.JVM_MODE, GlassfishModule.PROFILE_MODE, true);
            commonSupport.startServer(new OperationStateListener() {

                @Override
                public void operationStateChanged(OperationState newState, String message) {
                    if (newState == OperationState.RUNNING) {
                        // wait for the profiler agent to initialize
                        int t = 0;
                        Logger.getLogger("glassfish-javaee").log(Level.FINE,"t == {0}", t); // NOI18N

                        // Leave as soon as the profiler reaches state STATE_BLOCKING - 
                        //   we need the ant execution thread to for the profiler client;
                        // Note: It does not make sense to wait for STATE_RUNNING or STATE_PROFILING
                        //       as the profiler won't reach them unless the client is connected
                        try {
                            while (!(ProfilerSupport.getState() == ProfilerSupport.STATE_BLOCKING)
                                    && t < 30000) {
                                Thread.sleep(1000);
                                t += 1000;
                                Logger.getLogger("glassfish-javaee").log(Level.FINE, "t.1 == {0}", t);  // NOI18N
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    fireHandleProgressEvent(null, new Hk2DeploymentStatus(
                        CommandType.START, translateState(newState), ActionType.EXECUTE,
                        message));

                    // FIXME this is pretty ugly workaround and if this is still
                    // needed once GF plugin is rewritten we should introduce
                    // some API to notify about external changes of server state
                    final ScheduledExecutorService statusUpdater = Executors.newSingleThreadScheduledExecutor();
                    statusUpdater.scheduleAtFixedRate(new Runnable() {

                        @Override
                        public void run() {
                            if (ProfilerSupport.getState() == ProfilerSupport.STATE_INACTIVE) {
                                statusUpdater.shutdownNow();
                                if (commonSupport instanceof GlassfishModule3) {
                                    ((GlassfishModule3) commonSupport).refresh();
                                }
                            }
                        }
                    }, 50, 100, TimeUnit.MILLISECONDS);
                }
            }, GlassfishModule.ServerState.STOPPED_JVM_PROFILER);
        }
        return this;
    }

    private boolean isClusterOrInstance(GlassfishModule commonSupport) {
        String uri = commonSupport.getInstanceProperties().get(GlassfishModule.URL_ATTR);
        if (null == uri) {
            Logger.getLogger("glassfish-javaee").log(Level.WARNING, "{0} has a null URI??",
                    commonSupport.getInstanceProperties().get(GlassfishModule.DISPLAY_NAME_ATTR)); // NOI18N
            return true;
        }
        String target = Hk2DeploymentManager.getTargetFromUri(uri);
        return null == target ? false : !"server".equals(target);
    }
}