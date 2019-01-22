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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.web.project.test;

import java.beans.PropertyVetoException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import junit.framework.Assert;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.project.Project;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.j2ee.deployment.impl.ServerRegistry;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.netbeans.modules.java.source.parsing.JavacParser;
import org.netbeans.modules.java.source.parsing.JavacParserFactory;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.ant.AntBasedProjectType;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.LocalFileSystem;
import org.openide.filesystems.MultiFileSystem;
import org.openide.filesystems.Repository;
import org.openide.filesystems.URLMapper;
import org.openide.filesystems.XMLFileSystem;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;
import org.xml.sax.SAXException;

/**
 * Help set up org.netbeans.api.project.*Test.
 * @author Jesse Glick
 */
public final class TestUtil extends ProxyLookup {
    
    static {
        TestUtil.class.getClassLoader().setDefaultAssertionStatus(true);
        System.setProperty("org.openide.util.Lookup", TestUtil.class.getName());
        Assert.assertEquals(TestUtil.class, Lookup.getDefault().getClass());
        Lookup p = Lookups.forPath("Services/AntBasedProjectTypes/");
        p.lookupAll(AntBasedProjectType.class);
        projects = p;
        setLookup(new Object[0]);
    }
    
    private static TestUtil DEFAULT;
    private static final int BUFFER = 2048;
    private static final Lookup projects;
    
    /** Do not call directly */
    public TestUtil() {
        Assert.assertNull(DEFAULT);
        DEFAULT = this;
        ClassLoader l = TestUtil.class.getClassLoader();
        setLookups(new Lookup[] {
            Lookups.metaInfServices(l),
            Lookups.singleton(l)
        });
    }
    
    /**
     * Set the global default lookup.
     * Caution: if you don't include Lookups.metaInfServices, you may have trouble,
     * e.g. {@link #makeScratchDir} will not work.
     */
    public static void setLookup(Lookup l) {
        DEFAULT.setLookups(new Lookup[] {l});
    }
    
    /**
     * Set the global default lookup with some fixed instances including META-INF/services/*.
     */
    public static void setLookup(Object... instances) {
        ClassLoader l = TestUtil.class.getClassLoader();
        DEFAULT.setLookups(new Lookup[] {
            Lookups.fixed(instances),
            Lookups.metaInfServices(l),
            Lookups.singleton(l),
            projects
        });
    }
    
    private static boolean warned = false;
    /**
     * Create a scratch directory for tests.
     * Will be in /tmp or whatever, and will be empty.
     * If you just need a java.io.File use clearWorkDir + getWorkDir.
     */
    public static FileObject makeScratchDir(NbTestCase test) throws IOException {
        test.clearWorkDir();
        File root = test.getWorkDir();
        assert root.isDirectory() && root.list().length == 0;
        FileObject fo = FileUtil.toFileObject(root);
        if (fo != null) {
            return fo;
        } else {
            if (!warned) {
                warned = true;
                System.err.println("No FileObject for " + root + " found.\n" +
                                    "Maybe you need ${openide/masterfs.dir}/modules/org-netbeans-modules-masterfs.jar\n" +
                                    "in test.unit.run.cp.extra, or make sure Lookups.metaInfServices is included in Lookup.default, so that\n" +
                                    "Lookup.default<URLMapper>=" + Lookup.getDefault().lookup(new Lookup.Template(URLMapper.class)).allInstances() + " includes MasterURLMapper\n" +
                                    "e.g. by using TestUtil.setLookup(Object[]) rather than TestUtil.setLookup(Lookup).");
            }
            // For the benefit of those not using masterfs.
            LocalFileSystem lfs = new LocalFileSystem();
            try {
                lfs.setRootDirectory(root);
            } catch (PropertyVetoException e) {
                assert false : e;
            }
            Repository.getDefault().addFileSystem(lfs);
            return lfs.getRoot();
        }
    }
    
    /**
     * Delete a file and all subfiles.
     */
    public static void deleteRec(File f) throws IOException {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids == null) {
                throw new IOException("List " + f);
            }
            for (int i = 0; i < kids.length; i++) {
                deleteRec(kids[i]);
            }
        }
        if (!f.delete()) {
            throw new IOException("Delete " + f);
        }
    }
    
    /**
     * Create a testing project factory which recognizes directories containing
     * a subdirectory called "testproject".
     * If that subdirectory contains a file named "broken" then loading the project
     * will fail with an IOException.
     */
    public static ProjectFactory testProjectFactory() {
        return new TestProjectFactory();
    }
    
    /**
     * Try to force a GC.
     */
    public static void gc() {
        System.gc();
        System.runFinalization();
        System.gc();
    }
    
    private static final Map<FileObject,Integer> loadCount = new WeakHashMap();
    
    /**
     * Check how many times {@link ProjectFactory#loadProject} has been called
     * (with any outcome) on a given project directory.
     */
    public static int projectLoadCount(FileObject dir) {
        Integer i = (Integer)loadCount.get(dir);
        if (i != null) {
            return i.intValue();
        } else {
            return 0;
        }
    }
    
    /**
     * Mark a test project to fail with a given error when it is next saved.
     * The error only applies to the next save, not subsequent ones.
     * @param p a test project
     * @param error an error to throw (IOException or Error or RuntimeException),
     *              or null if it should succeed
     */
    public static void setProjectSaveWillFail(Project p, Throwable error) {
        ((TestProject)p).error = error;
    }
    
    /**
     * Get the number of times a test project was successfully saved with no error.
     * @param p a test project
     * @return the save count
     */
    public static int projectSaveCount(Project p) {
        return ((TestProject)p).saveCount;
    }
    
    /**
     * Mark a test project as modified.
     * @param p a test project
     */
    public static void modify(Project p) {
        ((TestProject)p).state.markModified();
    }
    
    /**
     * Mark a test project as modified.
     * @param p a test project
     */
    public static void notifyDeleted(Project p) {
        ((TestProject)p).state.notifyDeleted();
    }
    
    /**
     * Register Sun Application Server in the "IDE" to be used by unit test.
     * This method creates dummy userdir as well as dummy NetBeans home
     * in test's working directory. Both properties - <code>netbeans.home</code>
     * and <code>netbeans.user</code> - will be set by this method if they are
     * not already defined.
     *
     * @param test a test which requires SunAppServer
     * @return id of registered server
     */
    public static String registerSunAppServer(NbTestCase test) throws Exception {
        return registerSunAppServer(test, new Object[0]);
    }
    
    public static String registerSunAppServer(NbTestCase test, Object[] additionalLookupItems) throws Exception {
        String oldNbHome = System.getProperty("netbeans.home"); // NOI18N
        String oldNbUser = System.getProperty("netbeans.user"); // NOI18N
        File root = test.getWorkDir();
        File systemDir = new File(root, "ud/system"); // NOI18N
        FileUtil.createFolder(new File(systemDir, "J2EE/InstalledServers"));
        FileUtil.createFolder(new File(systemDir, "J2EE/DeploymentPlugins"));
        FileUtil.createFolder(new File(root, "nb"));
        System.setProperty("netbeans.home", new File(test.getWorkDir(), "nb").getAbsolutePath()); // NOI18N
        System.setProperty("netbeans.user", new File(test.getWorkDir(), "ud").getAbsolutePath()); // NOI18N
        
        // lookup content
        Object[] appServerNeed = new Object[] { new Repo(test), new IFL() };
        Object[] instances = new Object[additionalLookupItems.length + appServerNeed.length];
        System.arraycopy(additionalLookupItems, 0, instances, 0, additionalLookupItems.length);
        System.arraycopy(appServerNeed, 0, instances, additionalLookupItems.length, appServerNeed.length);
        TestUtil.setLookup(instances);
        
        File asRoot = null;
        if (System.getProperty("appserv.home") != null) { // NOI18N
            asRoot = new File(System.getProperty("appserv.home")); // NOI18N
        } else {
            asRoot = extractAppSrv(test.getWorkDir(), new File(test.getDataDir(), "SunAppServer.zip")); // NOI18N
        }
        FileObject dir = FileUtil.getConfigFile("J2EE/InstalledServers"); // NOI18N
        String name = FileUtil.findFreeFileName(dir, "instance", null); // NOI18N
        FileObject instanceFO = dir.createData(name);
        String serverID = "[" + asRoot.getAbsolutePath() + "]deployer:Sun:AppServer::localhost:4848"; // NOI18N
        instanceFO.setAttribute(InstanceProperties.URL_ATTR, serverID);
        instanceFO.setAttribute(InstanceProperties.USERNAME_ATTR, "admin"); // NOI18N
        instanceFO.setAttribute(InstanceProperties.PASSWORD_ATTR, "adminadmin"); // NOI18N
        instanceFO.setAttribute(InstanceProperties.DISPLAY_NAME_ATTR, "testdname"); // NOI18N
        instanceFO.setAttribute(InstanceProperties.HTTP_PORT_NUMBER, "4848"); // NOI18N
        instanceFO.setAttribute("DOMAIN", "testdomain1"); // NOI18N
        instanceFO.setAttribute("LOCATION", new File(asRoot, "domains").getAbsolutePath()); // NOI18N
        ServerRegistry sr = ServerRegistry.getInstance();
        sr.addInstance(instanceFO);
        if (oldNbHome != null) {
            System.setProperty("netbeans.home", oldNbHome); // NOI18N
        }
        if (oldNbUser != null) {
            System.setProperty("netbeans.user", oldNbUser); // NOI18N
        }
        return serverID;
    }

    public static void copyDir(File src,File tgt) throws IOException {
        if (src.isDirectory()) {
            if (!tgt.exists()) {
                FileUtil.createFolder(tgt);
            }
            
            String[] dirList = src.list();
            for (int i=0; i < dirList.length; i++) {
                copyDir(new File(src, dirList[i]), 
                        new File(tgt, dirList[i]));
            }
        } else {
            InputStream is = new FileInputStream(src);
            OutputStream os = new FileOutputStream(tgt);
            byte[] buffer = new byte[3 * 1024]; // 3 KB buffer
            int len = -1;
            try {
                len = is.read(buffer) ;
                while (len  > 0) {
                    os.write(buffer, 0, len);
                    len = is.read(buffer) ;                
                }
            }finally {
                if (is != null){
                    try {
                        is.close();                        
                    } catch (Exception ex){
                        // log
                    }
                }

                if (os != null){
                    try {
                        os.close();
                    } catch (Exception ex){
                        // log
                    }
                }
            }
        }
    }    
    
    private static File extractAppSrv(File destDir, File archiveFile) throws IOException {
        ZipInputStream zis = null;
        BufferedOutputStream dest = null;
        try {
            FileInputStream fis = new FileInputStream(archiveFile);
            zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while((entry = zis.getNextEntry()) != null) {
                byte data[] = new byte[BUFFER];
                File entryFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    FileUtil.createFolder(entryFile);
                } else {
                    FileUtil.createFolder(entryFile.getParentFile());
                    FileOutputStream fos = new FileOutputStream(entryFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    int count;
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                }
            }
        } finally {
            if (zis != null) { zis.close(); }
            if (dest != null) { dest.close(); }
        }
        return new File(destDir, archiveFile.getName().substring(0, archiveFile.getName().length() - 4));
    }

    public static EditableProperties loadProjectProperties(
            final FileObject projectDir) throws IOException {
        FileObject propsFO = projectDir.getFileObject(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        InputStream propsIS = propsFO.getInputStream();
        EditableProperties props = new EditableProperties(true);
        try {
            props.load(propsIS);
        } finally {
            propsIS.close();
        }
        return props;
    }
    
    public static void storeProjectProperties(FileObject projectDir, EditableProperties props) throws IOException {
        FileObject propsFO = projectDir.getFileObject(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        FileLock lock = propsFO.lock();
        try {
            OutputStream os = propsFO.getOutputStream(lock);
            try {
                props.store(os);
            } finally {
                os.close();
            }
        } finally {
            lock.releaseLock();
        }
    }
    
    /**
     * If set to something non-null, loading a broken project will wait for
     * notification on this monitor before throwing an exception.
     * @see ProjectManagerTest#testLoadExceptionWithConcurrentLoad
     */
    public static Object BROKEN_PROJECT_LOAD_LOCK = null;
    
    private static final class TestProjectFactory implements ProjectFactory {
        
        TestProjectFactory() {}
        
        public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
            Integer i = loadCount.get(projectDirectory);
            if (i == null) {
                i = 1;
            } else {
                i++;
            }
            loadCount.put(projectDirectory, i);
            FileObject testproject = projectDirectory.getFileObject("testproject");
            if (testproject != null && testproject.isFolder()) {
                if (testproject.getFileObject("broken") != null) {
                    if (BROKEN_PROJECT_LOAD_LOCK != null) {
                        synchronized (BROKEN_PROJECT_LOAD_LOCK) {
                            try {
                                BROKEN_PROJECT_LOAD_LOCK.wait();
                            } catch (InterruptedException e) {
                                assert false : e;
                            }
                        }
                    }
                    throw new IOException("Load failed of " + projectDirectory);
                } else {
                    return new TestProject(projectDirectory, state);
                }
            } else {
                return null;
            }
        }
        
        public void saveProject(Project project) throws IOException, ClassCastException {
            TestProject p = (TestProject)project;
            Throwable t = p.error;
            if (t != null) {
                p.error = null;
                if (t instanceof IOException) {
                    throw (IOException)t;
                } else if (t instanceof Error) {
                    throw (Error)t;
                } else {
                    throw (RuntimeException)t;
                }
            }
            p.saveCount++;
        }
        
        public boolean isProject(FileObject dir) {
            FileObject testproject = dir.getFileObject("testproject");
            return testproject != null && testproject.isFolder();
        }
        
    }
    
    private static final class TestProject implements Project {
        
        private final FileObject dir;
        final ProjectState state;
        Throwable error;
        int saveCount = 0;
        
        public TestProject(FileObject dir, ProjectState state) {
            this.dir = dir;
            this.state = state;
        }
        
        public Lookup getLookup() {
            return Lookup.EMPTY;
        }
        
        public FileObject getProjectDirectory() {
            return dir;
        }
        
        public String toString() {
            return "testproject:" + getProjectDirectory().getNameExt();
        }

        /* Probably unnecessary to have a ProjectInformation here:
        public String getName() {
            return "testproject:" + getProjectDirectory().getNameExt();
        }
        
        public String getDisplayName() {
            return "Test Project in " + getProjectDirectory().getNameExt();
        }
        
        public Image getIcon() {
            return null;
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {}
        public void removePropertyChangeListener(PropertyChangeListener listener) {}
         */
        
    }
    
    /**
     * Open a URL of content (for example from {@link Class#getResource}) and copy it to a named file.
     * The new file can be given as a parent directory plus a relative (slash-separated) path.
     * The file may not already exist, but intermediate directories may or may not.
     * If the content URL is null, the file is just created, no more; if it already existed
     * it is touched (timestamp updated) and its contents are cleared.
     * @return the file object
     */
    public static FileObject createFileFromContent(URL content, FileObject parent, String path) throws IOException {
        if (parent == null) {
            throw new IllegalArgumentException("null parent");
        }
        Assert.assertTrue("folder", parent.isFolder());
        FileObject fo = parent;
        StringTokenizer tok = new StringTokenizer(path, "/");
        boolean touch = false;
        while (tok.hasMoreTokens()) {
            Assert.assertNotNull("fo is null (parent=" + parent + " path=" + path + ")", fo);
            String name = tok.nextToken();
            if (tok.hasMoreTokens()) {
                FileObject sub = fo.getFileObject(name);
                if (sub == null) {
                    FileObject fo2 = fo.createFolder(name);
                    Assert.assertNotNull("createFolder(" + fo + ", " + name + ") -> null", fo2);
                    fo = fo2;
                } else {
                    Assert.assertTrue("folder", sub.isFolder());
                    fo = sub;
                }
            } else {
                FileObject sub = fo.getFileObject(name);
                if (sub == null) {
                    FileObject fo2 = fo.createData(name);
                    Assert.assertNotNull("createData(" + fo + ", " + name + ") -> null", fo2);
                    fo = fo2;
                } else {
                    fo = sub;
                    touch = true;
                }
            }
        }
        assert fo.isData();
        if (content != null || touch) {
            FileLock lock = fo.lock();
            try {
                OutputStream os = fo.getOutputStream(lock);
                try {
                    if (content != null) {
                        InputStream is = content.openStream();
                        try {
                            FileUtil.copy(is, os);
                        } finally {
                            is.close();
                        }
                    }
                } finally {
                    os.close();
                }
            } finally {
                lock.releaseLock();
            }
        }
        return fo;
    }
    
    private static final class Repo extends Repository {
        
        public Repo(NbTestCase t) throws Exception {
            super(mksystem(t));
        }
        
        private static FileSystem mksystem(NbTestCase t) throws Exception {
            LocalFileSystem lfs = new LocalFileSystem();
            File systemDir = new File(t.getWorkDir(), "ud/system");
            FileUtil.createFolder(systemDir);
            lfs.setRootDirectory(systemDir);
            lfs.setReadOnly(false);
            List<FileSystem> layers = new ArrayList<FileSystem>();
            layers.add(lfs);
            /*
            //get layer for the generic server
            java.net.URL layerFile = Repo.class.getClassLoader().getResource("org/netbeans/modules/j2ee/genericserver/resources/layer.xml");
            assert layerFile != null;
            layers.add(new XMLFileSystem(layerFile));
             */
            
            // get layer for the AS/GlassFish
//            addLayer(layers, "org/netbeans/modules/j2ee/sun/ide/j2ee/layer.xml");
//            addLayer(layers, "org/netbeans/modules/tomcat5/resources/layer.xml");
            MultiFileSystem mfs = new MultiFileSystem((FileSystem[]) layers.toArray(new FileSystem[layers.size()]));
            return mfs;
        }
        
        private static void addLayer(List<FileSystem> layers, String layerRes) throws SAXException {
            URL layerFile = Repo.class.getClassLoader().getResource(layerRes);
            assert layerFile != null;
            layers.add(new XMLFileSystem(layerFile));
        }
        
    }
    
    /** Copied from AntLoggerTest. */
    private static final class IFL extends InstalledFileLocator {
        
        public IFL() {}
        
        public File locate(String relativePath, String codeNameBase, boolean localized) {
            if (relativePath.equals("modules/ext/appsrvbridge.jar")) {
                String path = System.getProperty("test.appsrvbridge.jar");
                Assert.assertNotNull("must set test.appsrvbridge.jar", path);
                return new File(path);
            }
            return null;
        }
    }
    
    @ServiceProvider(service=MimeDataProvider.class)
    public static final class JavacParserProvider implements MimeDataProvider {

        private Lookup javaLookup = Lookups.fixed(new JavacParserFactory());

        public Lookup getLookup(MimePath mimePath) {
            if (mimePath.getPath().endsWith(JavacParser.MIME_TYPE)) {
                return javaLookup;
            }

            return Lookup.EMPTY;
        }

    }
}