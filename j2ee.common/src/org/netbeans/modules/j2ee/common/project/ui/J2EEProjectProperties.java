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

package org.netbeans.modules.j2ee.common.project.ui;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.j2ee.core.Profile;
import org.netbeans.api.project.Project;
import org.netbeans.modules.j2ee.common.Util;
import org.netbeans.modules.j2ee.deployment.devmodules.api.AntDeploymentHelper;
import org.netbeans.modules.j2ee.deployment.devmodules.api.Deployment;
import org.netbeans.modules.j2ee.deployment.devmodules.api.InstanceRemovedException;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eePlatform;
import org.netbeans.modules.j2ee.deployment.devmodules.api.ServerInstance;
import org.netbeans.modules.java.api.common.classpath.ClassPathSupport;
import org.netbeans.modules.javaee.specs.support.api.JaxWs;
import org.netbeans.modules.websvc.wsstack.api.WSStack;
import org.netbeans.modules.websvc.wsstack.api.WSTool;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Parameters;


/** Helper class. Defines constants for properties. Knows the proper
 *  place where to store the properties.
 * 
 * @author Petr Hrebejk, Radko Najman, David Konecny
 */
public final class J2EEProjectProperties {

    public static final String J2EE_PLATFORM_CLASSPATH = "j2ee.platform.classpath"; //NOI18N
    public static final String J2EE_SERVER_HOME = "j2ee.server.home"; //NOI18N
    public static final String J2EE_DOMAIN_HOME = "j2ee.server.domain"; //NOI18N
    public static final String J2EE_MIDDLEWARE_HOME = "j2ee.server.middleware"; //NOI18N
    public static final String J2EE_SERVER_INSTANCE = "j2ee.server.instance"; //NOI18N
    public static final String J2EE_SERVER_TYPE = "j2ee.server.type"; //NOI18N
    public static final String J2EE_PLATFORM_EMBEDDABLE_EJB_CLASSPATH = "j2ee.platform.embeddableejb.classpath"; //NOI18N
    public static final String ANT_DEPLOY_BUILD_SCRIPT = "nbproject/ant-deploy.xml"; // NOI18N
    public static final String DEPLOY_ANT_PROPS_FILE = "deploy.ant.properties.file"; //NOI18N
    
    public static final String J2EE_PLATFORM_WSCOMPILE_CLASSPATH = "j2ee.platform.wscompile.classpath"; //NOI18N
    public static final String J2EE_PLATFORM_JWSDP_CLASSPATH = "j2ee.platform.jwsdp.classpath"; //NOI18N
    public static final String J2EE_PLATFORM_WSIT_CLASSPATH = "j2ee.platform.wsit.classpath"; //NOI18N
    public static final String J2EE_PLATFORM_WSGEN_CLASSPATH = "j2ee.platform.wsgen.classpath"; //NOI18N
    public static final String J2EE_PLATFORM_WSIMPORT_CLASSPATH = "j2ee.platform.wsimport.classpath"; //NOI18N
    public static final String J2EE_PLATFORM_JSR109_SUPPORT = "j2ee.platform.is.jsr109"; //NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(J2EEProjectProperties.class.getName());
    
    /**
     * Remove obsolete properties from private properties.
     * @param privateProps private properties
     */
    public static void removeObsoleteLibraryLocations(EditableProperties privateProps) {
        // remove special properties from private.properties:
        Iterator<String> propKeys = privateProps.keySet().iterator();
        while (propKeys.hasNext()) {
            String key = propKeys.next();
            if (key.endsWith(".libdirs") || key.endsWith(".libfiles") || //NOI18N
                    (key.indexOf(".libdir.") > 0) || (key.indexOf(".libfile.") > 0)) { //NOI18N
                propKeys.remove();
            }
        }
    }
            
    
    /**
     * Returns <code>true</code> if the server library is used for j2ee instead
     * of the classpath pointing to the server installation.
     *
     * @param projectProperties project properties
     * @param j2eePlatformClasspathProperty name of the classpath property
     * @return <code>true</code> if the server library is used for j2ee instead
     *             of the classpath pointing to the server installation
     */
    public static boolean isUsingServerLibrary(EditableProperties projectProperties, String j2eePlatformClasspathProperty) {
        String value = projectProperties.getProperty(j2eePlatformClasspathProperty);
        return (value != null && !"".equals(value.trim())
                && value.indexOf(J2EE_SERVER_HOME) == -1
                && value.indexOf(J2EE_DOMAIN_HOME) == -1
                && value.indexOf(J2EE_MIDDLEWARE_HOME) == -1);
    }

    /**
     * Server is using server library if it is on the classpath or if certain property is presented in project,properties.
     */
    public static boolean isUsingServerLibrary(EditableProperties projectProperties, String j2eePlatformClasspathProperty,
            Iterable<ClassPathSupport.Item> items) {
        return isUsingServerLibrary(projectProperties, j2eePlatformClasspathProperty) ||
                (items !=null && !getServerLibraries(items).isEmpty());
    }

    public static void setServerProperties(EditableProperties ep, EditableProperties epPriv,
            String serverLibraryName, ClassPathSupport cs, Iterable<ClassPathSupport.Item> items,
            String serverInstanceID, Profile j2eeProfile, J2eeModule.Type moduleType) {
        setServerProperties(null, ep, epPriv, serverLibraryName, cs, items, serverInstanceID, j2eeProfile, moduleType);
    }

    /**
     * Finds server instance matching parameters.
     * TODO Integrate to j2eeserver together with fix for 198372.
     * 
     * @param serverType type of the server
     * @param moduleType type of the module
     * @param profile java ee profile
     * @return matching instance url or <code>null</code>
     * @since 1.64
     */
    @CheckForNull
    public static String getMatchingInstance(String serverType, J2eeModule.Type moduleType, Profile profile) {
        String[] servInstIDs = Deployment.getDefault().getInstancesOfServer(serverType);
        for (String instanceID : servInstIDs) {
            try {
                J2eePlatform platformLocal = Deployment.getDefault().getServerInstance(instanceID).getJ2eePlatform();
                if (platformLocal.getSupportedProfiles(moduleType).contains(profile)) {
                    return instanceID;
                }
            } catch (InstanceRemovedException ex) {
                continue;
            }
        }
        return null;
    }
    
    /**
     * Sets all server related properties.
     */
    public static void setServerProperties(Project project, EditableProperties ep, EditableProperties epPriv,
            String serverLibraryName, ClassPathSupport cs, Iterable<ClassPathSupport.Item> items,
            String serverInstanceID, Profile j2eeProfile, J2eeModule.Type moduleType) {
        Deployment deployment = Deployment.getDefault();
        String serverType = deployment.getServerID(serverInstanceID);

        J2eePlatform j2eePlatform = Deployment.getDefault().getJ2eePlatform(serverInstanceID);
        if (!j2eePlatform.getSupportedProfiles(moduleType).contains(j2eeProfile)) {
            Logger.getLogger("global").log(Level.WARNING, "J2EE level: {0} not supported by server {1} for module type WAR",
                    new Object[] {j2eeProfile != null ? j2eeProfile.getDisplayName() : "Unknown J2EE profile version - ", Deployment.getDefault().getServerInstanceDisplayName(serverInstanceID)}); // NOI18N
        }

        // set *always* sharable server properties:
        ep.setProperty(J2EE_SERVER_TYPE, serverType);

        // set *always* private server properties:
        epPriv.setProperty(J2EE_SERVER_INSTANCE, serverInstanceID);

        // different properties are set for server library:
        if (serverLibraryName != null || isUsingServerLibrary(ep, J2EE_PLATFORM_CLASSPATH, items)) {
            if (cs != null) {
                // use data from model (called eg. from project properties customizer)
                setSharableServerPropertiesFromModel(ep, epPriv, cs, items);
            } else if (serverLibraryName != null) {
                // init data for given sever library name (called eg. from project wizard instantiation)
                setSharableServerProperties(ep, epPriv, serverLibraryName);
            }
        } else {
            Map<String, String> roots = extractPlatformLibrariesRoot(j2eePlatform);
            if (roots != null) {
                // path will be relative and therefore stored in project.properties:
                setLocalServerProperties(project, epPriv, ep, j2eePlatform, roots);
            } else {
                // store absolute paths in private.properties:
                setLocalServerProperties(project, ep, epPriv, j2eePlatform, null);
            }
        }

        // set j2ee.platform.jsr109 support
        if (j2eePlatform.isToolSupported(J2eePlatform.TOOL_JSR109)) {
            epPriv.setProperty(J2EE_PLATFORM_JSR109_SUPPORT, "true"); //NOI18N
        }
    }

    /**
     * Update deployment script.
     */
    public static void createDeploymentScript(FileObject dirFO, EditableProperties ep, 
            EditableProperties epPriv, String serverInstanceID, J2eeModule.Type moduleType) {
        // ant deployment support
        File projectFolder = FileUtil.toFile(dirFO);
        try {
            AntDeploymentHelper.writeDeploymentScript(new File(projectFolder, ANT_DEPLOY_BUILD_SCRIPT),
                    moduleType, serverInstanceID);
        } catch (IOException ioe) {
            Logger.getLogger("global").log(Level.INFO, null, ioe);
        }
        File deployAntPropsFile = AntDeploymentHelper.getDeploymentPropertiesFile(serverInstanceID);
        if (deployAntPropsFile != null) {
            epPriv.setProperty(DEPLOY_ANT_PROPS_FILE, deployAntPropsFile.getAbsolutePath());
        }
    }

    /**
     * Callback to a project type for project specific functionality.
     */
    public static interface Callback {
        void registerJ2eePlatformListener(J2eePlatform platform);
        void unregisterJ2eePlatformListener(J2eePlatform platform);
    }

    /**
     * Update server properties. Apart from calling setServerProperties() it update listeners etc.
     * Called when server is changed in project properties or when broken server reference is being updated.
     */
    public static void updateServerProperties(EditableProperties projectProps, EditableProperties privateProps, String newServInstID,
            ClassPathSupport cs, Iterable<ClassPathSupport.Item> items,
            Callback callback, Project proj, Profile profile, J2eeModule.Type moduleType) {

        assert newServInstID != null : "Server isntance id to set can't be null"; // NOI18N

        // update j2ee.platform.classpath
        String oldServInstID = privateProps.getProperty(J2EE_SERVER_INSTANCE);
        if (oldServInstID != null) {
            J2eePlatform oldJ2eePlatform = Deployment.getDefault().getJ2eePlatform(oldServInstID);
            if (oldJ2eePlatform != null) {
                callback.unregisterJ2eePlatformListener(oldJ2eePlatform);
            }
        }
        J2eePlatform j2eePlatform = Deployment.getDefault().getJ2eePlatform(newServInstID);
        if (j2eePlatform == null) {
            // probably missing server error
            Logger.getLogger("global").log(Level.INFO, "J2EE platform is null."); // NOI18N

            // update j2ee.server.type (throws NPE)
            //projectProps.setProperty(J2EE_SERVER_TYPE, Deployment.getDefault().getServerID(newServInstID));

            // update j2ee.server.instance
            privateProps.setProperty(J2EE_SERVER_INSTANCE, newServInstID);
            removeServerClasspathProperties(privateProps);
            privateProps.remove(J2EE_PLATFORM_JSR109_SUPPORT);
            privateProps.remove(DEPLOY_ANT_PROPS_FILE);
            return;
        }
        callback.registerJ2eePlatformListener(j2eePlatform);

        setServerProperties(proj, projectProps, privateProps, null, cs, items, newServInstID, profile, moduleType);

        // ant deployment support
        createDeploymentScript(proj.getProjectDirectory(), projectProps, privateProps, newServInstID, moduleType);
    }

    public static void setSharableServerProperties(EditableProperties ep, EditableProperties epPriv, String serverLibraryName) {
        Parameters.notNull("serverLibraryName", serverLibraryName);

        // project properties will point to the library:
        ep.setProperty(J2EE_PLATFORM_CLASSPATH,
                "${libs." + serverLibraryName + "." + "classpath" + "}"); //NOI18N
        ep.setProperty(J2EE_PLATFORM_EMBEDDABLE_EJB_CLASSPATH,
                "${libs." + serverLibraryName + "." + "embeddableejb" + "}"); //NOI18N
        
        ep.setProperty(J2EE_PLATFORM_WSCOMPILE_CLASSPATH,
                "${libs." + serverLibraryName + "." + "wscompile" + "}"); //NOI18N
        ep.setProperty(J2EE_PLATFORM_WSIMPORT_CLASSPATH,
                "${libs." + serverLibraryName + "." + "wsimport" + "}"); //NOI18N
        ep.setProperty(J2EE_PLATFORM_WSGEN_CLASSPATH,
                "${libs." + serverLibraryName + "." + "wsgenerate" + "}"); //NOI18N
        ep.setProperty(J2EE_PLATFORM_WSIT_CLASSPATH,
                "${libs." + serverLibraryName + "." + "wsinterop" + "}"); //NOI18N
        ep.setProperty(J2EE_PLATFORM_JWSDP_CLASSPATH,
                "${libs." + serverLibraryName + "." + "wsjwsdp" + "}"); //NOI18N

        // should not be necessary: make sure private properties are empty:
        removeServerClasspathProperties(epPriv);
    }

    private static List<ClassPathSupport.Item> getServerLibraries(Iterable<ClassPathSupport.Item> items) {
        List<ClassPathSupport.Item> serverItems = new ArrayList<ClassPathSupport.Item>();
        for (ClassPathSupport.Item item : items) {
            if (item.getType() == ClassPathSupport.Item.TYPE_LIBRARY
                    && !item.isBroken()
                    && item.getLibrary().getType().equals(J2eePlatform.LIBRARY_TYPE)) {
                serverItems.add(ClassPathSupport.Item.create(item.getLibrary(), null));
            }
        }
        return serverItems;
    }

    private static void setSharableServerPropertiesFromModel(EditableProperties ep, EditableProperties epPriv,
            ClassPathSupport cs, Iterable<ClassPathSupport.Item> items) {
        Parameters.notNull("cs", cs);
        Parameters.notNull("items", items);
        List<ClassPathSupport.Item> serverItems = getServerLibraries(items);
        if (serverItems.isEmpty()) {
            return;
        }
        ep.setProperty(J2EE_PLATFORM_CLASSPATH, cs.encodeToStrings(serverItems, null, "classpath")); // NOI18N
        removeReferences(serverItems);
        ep.setProperty(J2EE_PLATFORM_WSCOMPILE_CLASSPATH,
                cs.encodeToStrings(serverItems, null, "wscompile")); // NOI18N
        removeReferences(serverItems);
        ep.setProperty(J2EE_PLATFORM_WSGEN_CLASSPATH,
                cs.encodeToStrings(serverItems, null, "wsgenerate")); // NOI18N
        removeReferences(serverItems);
        ep.setProperty(J2EE_PLATFORM_WSIMPORT_CLASSPATH,
                cs.encodeToStrings(serverItems, null, "wsimport")); // NOI18N
        removeReferences(serverItems);
        ep.setProperty(J2EE_PLATFORM_WSIT_CLASSPATH,
                cs.encodeToStrings(serverItems, null, "wsinterop")); // NOI18N
        removeReferences(serverItems);
        ep.setProperty(J2EE_PLATFORM_JWSDP_CLASSPATH,
                cs.encodeToStrings(serverItems, null, "wsjwsdp")); // NOI18N
        // private properties must be empty:
        removeServerClasspathProperties(epPriv);
    }

    private static void removeReferences(Iterable<ClassPathSupport.Item> items) {
        for (ClassPathSupport.Item item : items) {
            item.setReference(null);
        }
    }

    private static void removeServerClasspathProperties(EditableProperties ep) {
        ep.remove(J2EE_PLATFORM_CLASSPATH);
        ep.remove(J2EE_PLATFORM_EMBEDDABLE_EJB_CLASSPATH);
        ep.remove(J2EE_PLATFORM_WSCOMPILE_CLASSPATH);
        ep.remove(J2EE_PLATFORM_WSIMPORT_CLASSPATH);
        ep.remove(J2EE_PLATFORM_WSGEN_CLASSPATH);
        ep.remove(J2EE_PLATFORM_WSIT_CLASSPATH);
        ep.remove(J2EE_PLATFORM_JWSDP_CLASSPATH);
        ep.remove(J2EE_SERVER_HOME);
    }    

    private static void setLocalServerProperties(Project project, EditableProperties epToClean,
            EditableProperties epTarget, J2eePlatform j2eePlatform, Map<String, String> roots) {
        // remove all props first:
        removeServerClasspathProperties(epTarget);

        String classpath = toClasspathString(
                Util.getJ2eePlatformClasspathEntries(project, j2eePlatform), roots);
        epTarget.setProperty(J2EE_PLATFORM_CLASSPATH, classpath);

        // set j2ee.platform.embeddableejb.classpath
        if (j2eePlatform.isToolSupported(J2eePlatform.TOOL_EMBEDDABLE_EJB)) {
            File[] ejbClasspath = j2eePlatform.getToolClasspathEntries(J2eePlatform.TOOL_EMBEDDABLE_EJB);
            epTarget.setProperty(J2EE_PLATFORM_EMBEDDABLE_EJB_CLASSPATH,
                    toClasspathString(ejbClasspath, roots));
        }

        // set j2ee.platform.wscompile.classpath
        if (j2eePlatform.isToolSupported(J2eePlatform.TOOL_WSCOMPILE)) {
            File[] wsClasspath = j2eePlatform.getToolClasspathEntries(J2eePlatform.TOOL_WSCOMPILE);
            epTarget.setProperty(J2EE_PLATFORM_WSCOMPILE_CLASSPATH,
                    toClasspathString(wsClasspath, roots));
        }

        // set j2ee.platform.wsimport.classpath, j2ee.platform.wsgen.classpath
        WSStack<JaxWs> wsStack = WSStack.findWSStack(j2eePlatform.getLookup(), JaxWs.class);
        if (wsStack != null) {
            WSTool wsTool = wsStack.getWSTool(JaxWs.Tool.WSIMPORT); // the same as for WSGEN
            if (wsTool!= null && wsTool.getLibraries().length > 0) {
                String librariesList = toClasspathString(wsTool.getLibraries(), roots);
                epTarget.setProperty(J2EE_PLATFORM_WSGEN_CLASSPATH, librariesList);
                epTarget.setProperty(J2EE_PLATFORM_WSIMPORT_CLASSPATH, librariesList);
            }
        }

        if (j2eePlatform.isToolSupported(J2eePlatform.TOOL_WSIT)) {
            File[] wsClasspath = j2eePlatform.getToolClasspathEntries(J2eePlatform.TOOL_WSIT);
            epTarget.setProperty(J2EE_PLATFORM_WSIT_CLASSPATH,
                    toClasspathString(wsClasspath, roots));
        }

        if (j2eePlatform.isToolSupported(J2eePlatform.TOOL_JWSDP)) {
            File[] wsClasspath = j2eePlatform.getToolClasspathEntries(J2eePlatform.TOOL_JWSDP);
            epTarget.setProperty(J2EE_PLATFORM_JWSDP_CLASSPATH,
                    toClasspathString(wsClasspath, roots));
        }

        // remove everything from shared project properties
        removeServerClasspathProperties(epToClean);

        if (roots != null) {
            for (Map.Entry<String, String> entry : roots.entrySet()) {
                    epToClean.setProperty(entry.getValue(), entry.getKey());
            }
        }
    }

    public static Map<String, String> extractPlatformLibrariesRoot(J2eePlatform j2eePlatform) {
        Set<FileObject> toCheck = new HashSet<FileObject>();
        Map<String, String> roots = new HashMap<String, String>();
        File serverFile = j2eePlatform.getServerHome();
        if (serverFile != null) {
            serverFile = FileUtil.normalizeFile(serverFile);
            FileObject server = FileUtil.toFileObject(serverFile);
            if (server != null) {
                roots.put(serverFile.getAbsolutePath().replace('\\', '/'), J2EE_SERVER_HOME); // NOI18N
                toCheck.add(server);
            }
        }
        File domainFile = j2eePlatform.getDomainHome();
        if (domainFile != null) {
            domainFile = FileUtil.normalizeFile(domainFile);
            FileObject domain = FileUtil.toFileObject(domainFile);
            if (domain != null) {
                roots.put(domainFile.getAbsolutePath().replace('\\', '/'), J2EE_DOMAIN_HOME); // NOI18N
                toCheck.add(domain);
            }
        }
        File middlewareFile = j2eePlatform.getMiddlewareHome();
        if (middlewareFile != null) {
            middlewareFile = FileUtil.normalizeFile(middlewareFile);
            FileObject middleware = FileUtil.toFileObject(middlewareFile);
            if (middleware != null) {
                roots.put(middlewareFile.getAbsolutePath().replace('\\', '/'), J2EE_MIDDLEWARE_HOME); // NOI18N
                toCheck.add(middleware);
            }
        }
        
        if (roots.isEmpty()) {
            return extractPlatformLibrariesRootHeuristic(j2eePlatform);
        }
        
        boolean ok = true;
        for (File file : j2eePlatform.getClasspathEntries()) {
            FileObject fo = FileUtil.toFileObject(file);
            if (fo == null) {
                // if some file from server classpath does not exist then let's
                // ignore it
                continue;
            }
            boolean hit = false;
            for (FileObject root : toCheck) {
                if (FileUtil.isParentOf(root, fo)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                ok = false;
                break;
            }
        }
        if (!ok) {
            return null;
        }
        return roots;
    }
    
    @SuppressWarnings("deprecated")
    private static Map<String, String> extractPlatformLibrariesRootHeuristic(J2eePlatform j2eePlatform) {
        if (j2eePlatform.getPlatformRoots() == null || j2eePlatform.getPlatformRoots().length == 0) {
            return null;
        }
        File rootFile = FileUtil.normalizeFile(j2eePlatform.getPlatformRoots()[0]);
        FileObject root = FileUtil.toFileObject(rootFile);
        if (root == null) {
            return null;
        }
        boolean ok = true;
        for (File file : j2eePlatform.getClasspathEntries()) {
            FileObject fo = FileUtil.toFileObject(file);
            if (fo != null && !FileUtil.isParentOf(root, fo)) {
                ok = false;
                break;
            }
        }
        if (!ok) {
            return null;
        }
        return Collections.singletonMap(rootFile.getAbsolutePath().replace('\\', '/'), J2EE_SERVER_HOME); // NOI18N
    }

    public static String toClasspathString(File[] classpathEntries) {
        if (classpathEntries == null) {
            return "";
        }
        StringBuilder classpath = new StringBuilder();
        for (int i = 0; i < classpathEntries.length; i++) {
            classpath.append(classpathEntries[i].getAbsolutePath().replace('\\', '/')); // NOI18N
            if (i + 1 < classpathEntries.length) {
                classpath.append(':'); // NOI18N
            }
        }
        return classpath.toString();
    }

    public static String toClasspathString(File[] classpathEntries, Map<String, String> roots) {
        if (classpathEntries == null) {
            return "";
        }
        StringBuilder classpath = new StringBuilder();
        for (int i = 0; i < classpathEntries.length; i++) {
            String path = classpathEntries[i].getAbsolutePath().replace('\\', '/'); // NOI18N
            
            if (roots != null) {
                Map.Entry<String,String> replacement = null;
                for (Map.Entry<String, String> entry : roots.entrySet()) {
                    if (path.startsWith(entry.getKey())
                            && (replacement == null || replacement.getKey().length() < entry.getKey().length())) {
                        replacement = entry;
                    }                
                }
                if (replacement != null) {
                    path = "${" + replacement.getValue() + "}"  // NOI18N
                            + path.substring(replacement.getKey().length());
                }
            }
           
            if (classpath.length() > 0) {
                classpath.append(':'); // NOI18N
            }            
            classpath.append(path);
        }
        return classpath.toString();
    }

    public static String toClasspathString(URL[] classpathEntries) {
        if (classpathEntries == null) {
            return "";
        }
        StringBuilder classpath = new StringBuilder();
        for (int i = 0; i < classpathEntries.length; i++) {
            try {
                classpath.append(new File(classpathEntries[i].toURI()).getAbsolutePath().replace('\\', '/')); // NOI18N
            } catch (URISyntaxException ex) {

            }
            if (i + 1 < classpathEntries.length) {
                classpath.append(':'); // NOI18N
            }
        }
        return classpath.toString();
    }

    public static String toClasspathString(URL[] classpathEntries, Map<String, String> roots) {
        if (classpathEntries == null) {
            return "";
        }
        List<File> files = new ArrayList<File>();
        for (URL url : classpathEntries) {
            try {
                File file = new File(url.toURI()).getAbsoluteFile();
                files.add(file);
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.INFO, null, ex);
            }
        }
        
        return toClasspathString(files.toArray(new File[files.size()]), roots);
    }


}