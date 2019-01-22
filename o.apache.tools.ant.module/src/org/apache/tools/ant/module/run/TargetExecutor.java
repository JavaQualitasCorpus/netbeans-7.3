/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.tools.ant.module.run;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.apache.tools.ant.module.AntModule;
import org.apache.tools.ant.module.AntPanelController;
import org.apache.tools.ant.module.AntSettings;
import org.apache.tools.ant.module.api.AntProjectCookie;
import org.apache.tools.ant.module.api.support.TargetLister;
import org.apache.tools.ant.module.bridge.AntBridge;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.options.java.api.JavaOptions;
import org.openide.ErrorManager;
import org.openide.LifecycleManager;
import org.openide.awt.Actions;
import org.openide.execution.ExecutionEngine;
import org.openide.execution.ExecutorTask;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.io.ReaderInputStream;
import org.openide.windows.IOProvider;
import org.openide.windows.IOSelect;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;
import org.w3c.dom.Element;

/** Executes an Ant Target asynchronously in the IDE.
 */
public final class TargetExecutor implements Runnable {

    private static final RequestProcessor RP = new RequestProcessor(TargetExecutor.class.getName(), Integer.MAX_VALUE);

    /**
     * All tabs which were used for some process which has now ended.
     * These are closed when you start a fresh process.
     * Map from tab to tab display name.
     * @see "#43001"
     */
    private static final Map<InputOutput,String> freeTabs = new WeakHashMap<InputOutput,String>();
    
    /**
     * Display names of currently active processes.
     */
    private static final Set<String> activeDisplayNames = new HashSet<String>();
    
    private AntProjectCookie pcookie;
    private InputOutput io;
    private OutputStream outputStream;
    private boolean ok = false;
    private int verbosity = AntSettings.getVerbosity ();
    private Map<String,String> properties = AntSettings.getProperties();
    private List<String> targetNames;
    /** used for the tab etc. */
    private String displayName;
    private String suggestedDisplayName;

    /** targets may be null to indicate default target */
    public TargetExecutor (AntProjectCookie pcookie, String[] targets) {
        this.pcookie = pcookie;
        targetNames = ((targets == null) ? null : Arrays.asList(targets));
    }
  
    public void setVerbosity (int v) {
        verbosity = v;
    }
    
    public synchronized void setProperties(Map<String,String> p) {
        properties = new HashMap<String,String>(p);
    }

    void setDisplayName(String n) {
        suggestedDisplayName = n;
    }
    
    private static String getProcessDisplayName(AntProjectCookie pcookie, List<String> targetNames) {
        Element projel = pcookie.getProjectElement();
        String projectName;
        if (projel != null) {
            // remove & if available.
            projectName = Actions.cutAmpersand(projel.getAttribute("name")); // NOI18N
        } else {
            projectName = NbBundle.getMessage(TargetExecutor.class, "LBL_unparseable_proj_name");
        }
        String fileName;
        if (pcookie.getFileObject() != null) {
            fileName = pcookie.getFileObject().getNameExt();
        } else if (pcookie.getFile() != null) {
            fileName = pcookie.getFile().getName();
        } else {
            fileName = ""; // last resort for #84874
        }
        if (projectName.equals("")) { // NOI18N
            // No name="..." given, so try the file name instead.
            projectName = fileName;
        }
        if (targetNames != null) {
            StringBuffer targetList = new StringBuffer();
            Iterator<String> it = targetNames.iterator();
            if (it.hasNext()) {
                targetList.append(it.next());
            }
            while (it.hasNext()) {
                targetList.append(NbBundle.getMessage(TargetExecutor.class, "SEP_output_target"));
                targetList.append(it.next());
            }
            return NbBundle.getMessage(TargetExecutor.class, "TITLE_output_target", projectName, fileName, targetList);
        } else {
            return NbBundle.getMessage(TargetExecutor.class, "TITLE_output_notarget", projectName, fileName);
        }
    }
    
    private static final Map<InputOutput,StopAction> stopActions = new HashMap<InputOutput,StopAction>();
    private static final Map<InputOutput,RerunAction[]> rerunActions = new HashMap<InputOutput,RerunAction[]>();

    private static final class StopAction extends AbstractAction {

        public LastTargetExecuted t;

        public StopAction() {
            setEnabledEQ(this, false); // initially, until ready
        }

        @Override
        public Object getValue(String key) {
            if (key.equals(Action.SMALL_ICON)) {
                return new ImageIcon(TargetExecutor.class.getResource("/org/apache/tools/ant/module/resources/stop.png"));
            } else if (key.equals(Action.SHORT_DESCRIPTION)) {
                return NbBundle.getMessage(TargetExecutor.class, "TargetExecutor.StopAction.stop");
            } else {
                return super.getValue(key);
            }
        }

        public void actionPerformed(ActionEvent e) {
            setEnabled(false); // discourage repeated clicking
            if (t != null) { // #84688
                t.stopRunning();
            }
        }

    }

    private static final class RerunAction extends AbstractAction implements FileChangeListener {

        private final boolean withModifications;
        private AntProjectCookie pcookie;
        private List<String> targetNames;
        private int verbosity;
        private Map<String,String> properties;
        private String displayName;

        public RerunAction(TargetExecutor prototype, boolean withModifications) {
            this.withModifications = withModifications;
            reinit(prototype);
            setEnabledEQ(this, false); // initially, until ready
            FileObject script = pcookie.getFileObject();
            if (script != null) {
                script.addFileChangeListener(FileUtil.weakFileChangeListener(this, script));
            }
        }

        private void reinit(TargetExecutor prototype) {
            pcookie = prototype.pcookie;
            targetNames = prototype.targetNames;
            verbosity = prototype.verbosity;
            properties = prototype.properties;
            displayName = prototype.suggestedDisplayName;
        }

        @Override
        public Object getValue(String key) {
            if (key.equals(Action.SMALL_ICON)) {
                if (withModifications) {
                    return new ImageIcon(TargetExecutor.class.getResource("/org/apache/tools/ant/module/resources/rerun-mod.png"));
                } else {
                    return new ImageIcon(TargetExecutor.class.getResource("/org/apache/tools/ant/module/resources/rerun.png"));
                }
            } else if (key.equals(Action.SHORT_DESCRIPTION)) {
                if (withModifications) {
                    return NbBundle.getMessage(TargetExecutor.class, "TargetExecutor.RerunAction.rerun_different");
                } else {
                    return NbBundle.getMessage(TargetExecutor.class, "TargetExecutor.RerunAction.rerun");
                }
            } else {
                return super.getValue(key);
            }
        }

        public void actionPerformed(ActionEvent e) {
            setEnabled(false);
            try {
                if (withModifications) {
                    AdvancedActionPanel panel = new AdvancedActionPanel(pcookie, TargetLister.getTargets(pcookie));
                    panel.setTargets(targetNames);
                    panel.setVerbosity(verbosity);
                    panel.setProperties(properties);
                    if (!panel.display()) {
                        setEnabled(true);
                    }
                } else {
                    TargetExecutor exec = new TargetExecutor(pcookie,
                            targetNames != null ? targetNames.toArray(new String[targetNames.size()]) : null);
                    //exec.setVerbosity(verbosity);
                    exec.setProperties(properties);
                    if (displayName != null) {
                        exec.setDisplayName(displayName);
                    }
                    exec.execute();
                }
            } catch (IOException x) {
                Logger.getLogger(TargetExecutor.class.getName()).log(Level.INFO, null, x);
            }
        }

        public void fileDeleted(FileEvent fe) {
            firePropertyChange("enabled", null, false); // NOI18N
        }

        public void fileFolderCreated(FileEvent fe) {}

        public void fileDataCreated(FileEvent fe) {}

        public void fileChanged(FileEvent fe) {}

        public void fileRenamed(FileRenameEvent fe) {}

        public void fileAttributeChanged(FileAttributeEvent fe) {}

        public @Override boolean isEnabled() {
            // #84874: should be disabled in case the original Ant script is now gone.
            return super.isEnabled() && pcookie.getFileObject() != null && pcookie.getFileObject().isValid();
        }

    }

    private static final class OptionsAction extends AbstractAction { // #59396

        @Override
        public Object getValue(String key) {
            if (key.equals(Action.SMALL_ICON)) {
                return new ImageIcon(TargetExecutor.class.getResource("/org/apache/tools/ant/module/resources/options.png"));
            } else if (key.equals(Action.SHORT_DESCRIPTION)) {
                return NbBundle.getMessage(TargetExecutor.class, "TargetExecutor.OptionsAction");
            } else {
                return super.getValue(key);
            }
        }

        public void actionPerformed(ActionEvent e) {
            OptionsDisplayer.getDefault().open(JavaOptions.JAVA + "/" + AntPanelController.OPTIONS_SUBPATH); // NOI18N
        }

    }

    /**
     * Actually start the process.
     */
    public ExecutorTask execute () throws IOException {
        String dn = suggestedDisplayName != null ? suggestedDisplayName : getProcessDisplayName(pcookie, targetNames);
        synchronized (activeDisplayNames) {
        if (activeDisplayNames.contains(dn)) {
            // Uniquify: "prj (targ) #2", "prj (targ) #3", etc.
            int i = 2;
            String testdn;
            do {
                testdn = NbBundle.getMessage(TargetExecutor.class, "TargetExecutor.uniquified", dn, i++);
            } while (activeDisplayNames.contains(testdn));
            dn = testdn;
        }
        assert !activeDisplayNames.contains(dn);
        displayName = dn;
        activeDisplayNames.add(displayName);
        }
        
        final ExecutorTask task;
        synchronized (this) {
            // OutputWindow
            if (AntSettings.getAutoCloseTabs()) { // #47753
            synchronized (freeTabs) {
                for (Map.Entry<InputOutput,String> entry : freeTabs.entrySet()) {
                    InputOutput free = entry.getKey();
                    String freeName = entry.getValue();
                    if (io == null && freeName.equals(displayName)) {
                        // Reuse it.
                        io = free;
                        io.getOut().reset();
                        // Apparently useless and just prints warning: io.getErr().reset();
                        // useless: io.flushReader();
                    } else {
                        // Discard it.
                        free.closeInputOutput();
                        stopActions.remove(free);
                        rerunActions.remove(free);
                    }
                }
                freeTabs.clear();
            }
            }
            if (io == null) {
                StopAction sa = new StopAction();
                RerunAction[] ras = {new RerunAction(this, false), new RerunAction(this, true)};
                io = IOProvider.getDefault().getIO(displayName, new Action[] {ras[0], ras[1], sa, new OptionsAction()});
                stopActions.put(io, sa);
                rerunActions.put(io, ras);
            }
            task = ExecutionEngine.getDefault().execute(displayName, this, InputOutput.NULL);
        }
        WrapperExecutorTask wrapper = new WrapperExecutorTask(task, io);
        RP.post(wrapper);
        return wrapper;
    }
    
    public ExecutorTask execute(OutputStream outputStream) throws IOException {
        this.outputStream = outputStream;
        ExecutorTask task = ExecutionEngine.getDefault().execute(null, this, InputOutput.NULL);
        return new WrapperExecutorTask(task, null);
    }
    
    private class WrapperExecutorTask extends ExecutorTask {
        private ExecutorTask task;
        private InputOutput io;
        public WrapperExecutorTask(ExecutorTask task, InputOutput io) {
            super(new WrapperRunnable(task));
            this.task = task;
            this.io = io;
        }
        @Override
        public void stop () {
            StopAction sa = stopActions.get(io);
            if (sa != null) {
                sa.actionPerformed(null);
            } else { // just in case
                task.stop();
            }
        }
        @Override
        public int result () {
            return task.result () + (ok ? 0 : 1);
        }
        @Override
        public InputOutput getInputOutput () {
            return io;
        }
    }
    private static class WrapperRunnable implements Runnable {
        private final ExecutorTask task;
        public WrapperRunnable(ExecutorTask task) {
            this.task = task;
        }
        public void run () {
            task.waitFinished ();
        }
    }
  
    /** Call execute(), not this method directly!
     */
    @SuppressWarnings("NestedSynchronizedStatement")
    synchronized public @Override void run () {
        final LastTargetExecuted[] thisExec = new LastTargetExecuted[1];
        final StopAction sa = stopActions.get(io);
        assert sa != null;
        RerunAction[] ras = rerunActions.get(io);
        assert ras != null;
        try {
            
        final AtomicBoolean displayed = new AtomicBoolean(AntSettings.getAlwaysShowOutput());
        
        if (outputStream == null) {
            if (displayed.get()) {
                io.select();
            } else if (IOSelect.isSupported(io)) {
                boolean onlyProcessRunning;
                synchronized (activeDisplayNames) {
                    onlyProcessRunning = activeDisplayNames.size() == 1;
                }
                if (onlyProcessRunning) {
                    IOSelect.select(io, EnumSet.noneOf(IOSelect.AdditionalOperation.class));
                }
            }
        }
        
        if (AntSettings.getSaveAll()) {
            LifecycleManager.getDefault ().saveAll ();
        }
        
        final OutputWriter out;
        final OutputWriter err;
        if (outputStream == null) {
            out = io.getOut();
            err = io.getErr();
        } else {
            throw new RuntimeException("XXX No support for outputStream currently!"); // NOI18N
        }
        
        final File buildFile = pcookie.getFile ();
        if (buildFile == null) {
            err.println(NbBundle.getMessage(TargetExecutor.class, "EXC_non_local_proj_file"));
            return;
        }

        // #139185: do not record verbosity level; always pick it up from Ant Settings.
        thisExec[0] = LastTargetExecuted.record(buildFile, /*verbosity,*/
                targetNames != null ? targetNames.toArray(new String[targetNames.size()]) : null,
                properties,
                suggestedDisplayName != null ? suggestedDisplayName : getProcessDisplayName(pcookie, targetNames), Thread.currentThread());
        sa.t = thisExec[0];
        
        // Don't hog the CPU, the build might take a while:
        Thread.currentThread().setPriority((Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2);
        
        final Runnable interestingOutputCallback = new Runnable() {
            public void run() {
                // #58513: display output now.
                if (!displayed.getAndSet(true)) {
                    io.select();
                }
            }
        };
        
        final AtomicReference<InputStream> in = new AtomicReference<InputStream>();
        if (outputStream == null) { // #43043
            try {
                in.set(new ReaderInputStream(io.getIn()) {
                    // Show the output when an input field is displayed, if it hasn't already.
                    @Override
                    public int read() throws IOException {
                        interestingOutputCallback.run();
                        return super.read();
                    }
                    @Override
                    public int read(byte[] b) throws IOException {
                        interestingOutputCallback.run();
                        return super.read(b);
                    }
                    @Override
                    public int read(byte[] b, int off, int len) throws IOException {
                        interestingOutputCallback.run();
                        return super.read(b, off, len);
                    }
                    @Override
                    public long skip(long n) throws IOException {
                        interestingOutputCallback.run();
                        return super.skip(n);
                    }
                });
            } catch (IOException e) {
                AntModule.err.notify(ErrorManager.INFORMATIONAL, e);
            }
        }
        
	    // #58513, #87801: register a progress handle for the task too.
        final ProgressHandle handle = ProgressHandleFactory.createHandle(displayName, new Cancellable() {
            public boolean cancel() {
                sa.actionPerformed(null);
                return true;
            }
        }, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                io.select();
            }
        });
        handle.setInitialDelay(0); // #92436
        handle.start();
        setEnabledEQ(sa, true);
        for (RerunAction ra : ras) {
            setEnabledEQ(ra, false);
        }
        ok = AntBridge.getInterface().run(buildFile, targetNames, in.get(), out, err, properties, verbosity, displayName, interestingOutputCallback, handle, io);
        
        } finally {
            if (io != null) {
                synchronized (freeTabs) {
                    freeTabs.put(io, displayName);
                }
            }
            if (thisExec[0] != null) {
                LastTargetExecuted.finish(thisExec[0]);
            }
            sa.t = null;
            setEnabledEQ(sa, false);
            for (RerunAction ra : ras) {
                setEnabledEQ(ra, true);
                ra.reinit(this);
            }
            synchronized (activeDisplayNames) {
                activeDisplayNames.remove(displayName);
            }
        }
    }
    
    /** Try to stop a build. */
    static void stopProcess(Thread t) {
        AntBridge.getInterface().stop(t);
    }

    private static void setEnabledEQ(final Action a, final boolean enabled) { // #133025
        Mutex.EVENT.readAccess(new Runnable() {
            public void run() {
                a.setEnabled(enabled);
            }
        });
    }

}