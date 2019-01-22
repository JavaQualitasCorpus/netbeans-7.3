/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.classpath;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.netbeans.modules.maven.api.Constants;
import org.netbeans.modules.maven.api.FileUtilities;
import org.netbeans.modules.maven.api.PluginPropertyUtils;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryPreferences;
import org.netbeans.modules.maven.indexer.api.RepositoryQueries;
import org.netbeans.modules.maven.indexer.api.RepositoryUtil;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

/**
 * NO listening on changes here, let the BootClassPath deal with it..
 * @author  Milos Kleint
 */
@org.netbeans.api.annotations.common.SuppressWarnings("DMI_COLLECTION_OF_URLS")
public final class EndorsedClassPathImpl implements ClassPathImplementation, FileChangeListener {

    private static final Logger LOG = Logger.getLogger(EndorsedClassPathImpl.class.getName());
    static final RequestProcessor RP = new RequestProcessor(EndorsedClassPathImpl.class);

    private List<? extends PathResourceImplementation> resourcesCache;
    private PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final NbMavenProjectImpl project;
    private BootClassPathImpl bcp;
    private String[] current;
    private final File endorsed;
    private final Map<File,File/*|null*/> endorsed2Repo = new HashMap<File,File>();

    @SuppressWarnings("LeakingThisInConstructor")
    EndorsedClassPathImpl(NbMavenProjectImpl project) {
        this.project = project;
        endorsed = new File(project.getPOMFile().getParentFile(), "target/endorsed"); // NOI18N
        FileUtil.addFileChangeListener(this, endorsed);
    }

    public @Override List<? extends PathResourceImplementation> getResources() {
        assert bcp != null;
        synchronized (bcp.LOCK) {
            if (this.resourcesCache == null) {
                ArrayList<PathResourceImplementation> result = new ArrayList<PathResourceImplementation> ();
                String[] boot = getBootClasspath();
                if (boot != null) {
                    for (URL u :  stripDefaultJavaPlatform(boot)) {
                        result.add (ClassPathSupport.createResource(u));
                    }
                }
                File[] jars = endorsed.listFiles();
                if (jars != null) {
                    for (final File jar : jars) {
                        if (jar.isFile()) {
                            if (endorsed2Repo.containsKey(jar)) {
                                File toScan = endorsed2Repo.get(jar);
                                if (toScan != null) {
                                    result.add(ClassPathSupport.createResource(FileUtil.urlForArchiveOrDir(toScan)));
                                }
                            } else {
                                // #197510: blocking, must do this asynch
                                LOG.log(Level.FINE, "looking up {0}", jar);
                                RP.post(new Runnable() {
                                    public @Override void run() {
                                        synchronized (bcp.LOCK) {
                                            if (endorsed2Repo.containsKey(jar)) {
                                                // Another task beat us to it.
                                                return;
                                            }
                                        }
                                        if (!jar.isFile()) {
                                            return;
                                        }
                                        File toScan = null;
                                        REPO: for (RepositoryInfo repo : RepositoryPreferences.getInstance().getRepositoryInfos()) {
                                            LOG.log(Level.FINE, "checking {0}", repo);
                                            for (NBVersionInfo analogue : RepositoryQueries.findBySHA1Result(jar, Collections.singletonList(repo)).getResults()) {
                                                toScan = RepositoryUtil.createArtifact(analogue).getFile();
                                                LOG.log(Level.FINE, "found {0}", toScan);
                                                break REPO;
                                            }
                                        }
                                        if (toScan == null) {
                                            try {
                                                toScan = FileUtil.normalizeFile(new File(System.getProperty("java.io.tmpdir"), RepositoryUtil.calculateSHA1Checksum(jar) + ".jar"));
                                                if (!toScan.isFile()) {
                                                    FileUtils.copyFile(jar, toScan);
                                                }
                                            } catch (IOException x) {
                                                LOG.log(Level.INFO, "copying " + jar + " to " + toScan, x);
                                            }
                                        }
                                        LOG.log(Level.FINE, "mapping {0} -> {1}", new Object[] {jar, toScan});
                                        synchronized (bcp.LOCK) {
                                            endorsed2Repo.put(jar, toScan);
                                            resourcesCache = null;
                                        }
                                        support.firePropertyChange(PROP_RESOURCES, null, null);
                                    }
                                });
                            }
                        }
                    }
                }
                current = boot;
                resourcesCache = Collections.unmodifiableList (result);
            }
            return this.resourcesCache;
        }
    }

    public @Override void addPropertyChangeListener(PropertyChangeListener listener) {
        this.support.addPropertyChangeListener (listener);
    }

    public @Override void removePropertyChangeListener(PropertyChangeListener listener) {
        this.support.removePropertyChangeListener (listener);
    }

    private String[] getBootClasspath() {
        String carg = PluginPropertyUtils.getPluginProperty(project, Constants.GROUP_APACHE_PLUGINS, Constants.PLUGIN_COMPILER, "compilerArgument", "compile", null);
        if (carg != null) {
            //TODO
        }
        Properties cargs = PluginPropertyUtils.getPluginPropertyParameter(project, Constants.GROUP_APACHE_PLUGINS, Constants.PLUGIN_COMPILER, "compilerArguments", "compile");
        if (cargs != null) {
            String carg2 = cargs.getProperty("bootclasspath");
            if (carg2 != null) {
                return StringUtils.split(carg2, File.pathSeparator);
            }
        }
        return null;
    }

    /**
     * Resets the cache and firesPropertyChange
     */
    boolean resetCache () {
        String[] newones = getBootClasspath();
        boolean fire = false;
        assert bcp != null;
        synchronized (bcp.LOCK) {
            if (!Arrays.equals(newones, current)) {
                resourcesCache = null;
                fire = true;
            }
        }
        if (fire) {
            support.firePropertyChange(PROP_RESOURCES, null, null);
        }
        return fire;
    }

    void setBCP(BootClassPathImpl aThis) {
        bcp = aThis;
    }

    private List<URL> stripDefaultJavaPlatform(String[] boot) {
        List<URL> toRet = new ArrayList<URL>();
        Set<URL> defs = getDefJavaPlatBCP();
        for (String s : boot) {
            File f = FileUtilities.convertStringToFile(s);
            URL entry = FileUtil.urlForArchiveOrDir(f);
            if (entry != null && !defs.contains(entry)) {
                toRet.add(entry);
            }
        }
        return toRet;
    }

    private final Set<URL> djpbcp = new HashSet<URL>();

    private Set<URL> getDefJavaPlatBCP() {
        synchronized (djpbcp) {
            if (djpbcp.isEmpty()) {
                JavaPlatformManager mngr = JavaPlatformManager.getDefault();
                JavaPlatform jp = mngr.getDefaultPlatform();
                ClassPath cp = jp.getBootstrapLibraries();
                for (ClassPath.Entry ent : cp.entries()) {
                    djpbcp.add(ent.getURL());
                }
            }
            return Collections.unmodifiableSet(djpbcp);
        }
    }

    private void fileChange() {
        assert bcp != null;
        synchronized (bcp.LOCK) {
            resourcesCache = null;
        }
        support.firePropertyChange(PROP_RESOURCES, null, null);
    }
    public @Override void fileFolderCreated(FileEvent fe) {
        fileChange();
    }
    public @Override void fileDataCreated(FileEvent fe) {
        fileChange();
    }
    public @Override void fileChanged(FileEvent fe) {
        fileChange();
    }
    public @Override void fileDeleted(FileEvent fe) {
        fileChange();
    }
    public @Override void fileRenamed(FileRenameEvent fe) {
        fileChange();
    }
    public @Override void fileAttributeChanged(FileAttributeEvent fe) {}

}