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

package org.netbeans.modules.project.libraries;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.TestUtil;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.RandomlyFails;
import org.netbeans.modules.project.libraries.ui.LibrariesCustomizer;
import org.netbeans.modules.project.libraries.ui.LibrariesModel;
import org.netbeans.modules.project.libraries.ui.ProxyLibraryImplementation;
import org.netbeans.spi.project.libraries.LibraryImplementation;
import org.netbeans.spi.project.libraries.LibraryImplementation3;
import org.netbeans.spi.project.libraries.LibraryProvider;
import org.netbeans.spi.project.libraries.LibraryTypeProvider;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.InstanceDataObject;
import org.openide.util.Lookup;
import org.openide.util.test.MockLookup;
import org.openide.xml.EntityCatalog;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



/**
 *
 * @author Tomas Zezula
 */
public class LibrariesStorageTest extends NbTestCase {
    
    private FileObject storageFolder;
    LibrariesStorage storage;
    
    public LibrariesStorageTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockLookup.setInstances(new TestEntityCatalog());
        registerLibraryTypeProvider();
        this.storageFolder = TestUtil.makeScratchDir(this);
        createLibraryDefinition(this.storageFolder,"Library1", null);
        this.storage = new LibrariesStorage (this.storageFolder);
    }

    @RandomlyFails
    /* ergonomics-3373:
     * junit.framework.AssertionFailedError: Event count expected:<1> but was:<2>
	 * at org.netbeans.modules.project.libraries.LibrariesStorageTest.testGetLibraries(LibrariesStorageTest.java:127)
	 * at org.netbeans.junit.NbTestCase.access$200(NbTestCase.java:99)
	 * at org.netbeans.junit.NbTestCase$2.doSomething(NbTestCase.java:405)
	 * at org.netbeans.junit.NbTestCase$1Guard.run(NbTestCase.java:331)
	 * at java.lang.Thread.run(Thread.java:662)
     */
    public void testGetLibraries() throws Exception {
        this.storage.getLibraries();
        LibraryImplementation[] libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);
        assertLibEquals(libs, new String[] {"Library1"});
        createLibraryDefinition(this.storageFolder,"Library2", null);
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",2,libs.length);
        assertLibEquals(libs, new String[] {"Library1", "Library2"});
        TestListener l = new TestListener ();
        this.storage.addPropertyChangeListener(l);
        TestLibraryTypeProvider tlp = (TestLibraryTypeProvider) LibraryTypeRegistry.getDefault().getLibraryTypeProvider (TestLibraryTypeProvider.TYPE);
        tlp.reset();
        createLibraryDefinition(this.storageFolder,"Library3", null);
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",3,libs.length);
        assertLibEquals(libs, new String[] {"Library1", "Library2", "Library3"});
        assertEquals("Event count",1,l.getEventNames().size()); // line 127@ergonomics-3373
        assertEquals("Event names",LibraryProvider.PROP_LIBRARIES,l.getEventNames().get(0));
        assertTrue("Library created called",tlp.wasCreatedCalled());
    }
    public void testGetDisplayNameLibraries() throws Exception {
        this.storage.getLibraries();
        LibraryImplementation[] libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);
        assertLibEquals(libs, new String[] {"Library1"});
        createLibraryDefinition(this.storageFolder,"Library2", "MyName");
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",2,libs.length);
        LibraryImplementation impl = libs[0].getName().equals("Library2") ? libs[0] : libs[1];

        assertEquals("MyName", Util.getLocalizedName(impl));

        LibrariesModel model = new LibrariesModel();
        ProxyLibraryImplementation proxy = ProxyLibraryImplementation.createProxy(impl, model);

        assertEquals("MyName", Util.getLocalizedName(proxy));
    }

    public void testAddLibrary() throws Exception {
        this.storage.getLibraries();
        LibraryImplementation[] libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);
        assertLibEquals(libs, new String[] {"Library1"});
        TestLibraryTypeProvider tlp = (TestLibraryTypeProvider) LibraryTypeRegistry.getDefault().getLibraryTypeProvider (TestLibraryTypeProvider.TYPE);
        tlp.reset();
        LibraryImplementation impl = new TestLibrary("Library2");
        this.storage.addLibrary(impl);
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",2,libs.length);
        assertLibEquals(libs, new String[] {"Library1","Library2"});
        assertTrue (tlp.wasCreatedCalled());
    }

    public void testRemoveLibrary() throws Exception {
        this.storage.getLibraries();
        LibraryImplementation[] libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);
        assertLibEquals(libs, new String[] {"Library1"});
        TestLibraryTypeProvider tlp = (TestLibraryTypeProvider) LibraryTypeRegistry.getDefault().getLibraryTypeProvider (TestLibraryTypeProvider.TYPE);
        tlp.reset();
        this.storage.removeLibrary(libs[0]);
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",0,libs.length);
        assertTrue ("Library deleted called",  tlp.wasDeletedCalled());
    }

    public void testUpdateLibrary() throws Exception {
        this.storage.getLibraries();
        LibraryImplementation[] libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);
        assertLibEquals(libs, new String[] {"Library1"});
        TestLibraryTypeProvider tlp = (TestLibraryTypeProvider) LibraryTypeRegistry.getDefault().getLibraryTypeProvider (TestLibraryTypeProvider.TYPE);
        tlp.reset();
        LibraryImplementation newLib = new TestLibrary ((TestLibrary)libs[0]);
        newLib.setName ("NewLibrary");
        this.storage.updateLibrary(libs[0],newLib);
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);
        assertLibEquals(libs, new String[] {"NewLibrary"});
        assertTrue ("Library created called",  tlp.wasCreatedCalled());
    }
    
    public void testNamedLibraryImplementation() throws Exception {
        this.storage.getLibraries();
        LibraryImplementation[] libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);  //NOI18N
        assertLibEquals(libs, new String[] {"Library1"});   //NOI18N
        LibraryImplementation3 lib3 = (LibraryImplementation3)libs[0];
        assertNull(lib3.getDisplayName());
        
        LibraryImplementation3 newLib = new TestLibrary ((TestLibrary)lib3);
        newLib.setDisplayName("FooLib");    //NOI18N
        this.storage.updateLibrary(lib3,newLib);
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);  //NOI18N
        assertLibEquals(libs, new String[] {"Library1"});   //NOI18N
        lib3 = (LibraryImplementation3)libs[0];
        assertEquals("FooLib",lib3.getDisplayName());   //NOI18N
    }
    
    public void testLibraryImplementation3() throws Exception {
        this.storage.getLibraries();
        LibraryImplementation[] libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);  //NOI18N
        assertLibEquals(libs, new String[] {"Library1"});   //NOI18N
        LibraryImplementation3 lib3 = (LibraryImplementation3)libs[0];
        assertTrue(lib3.getProperties().isEmpty());
        
        LibraryImplementation3 newLib = new TestLibrary ((TestLibrary)lib3);
        Map<String,String> props = new HashMap<String, String>();
        props.put("test_prop","test_value");    //NOI18N
        newLib.setProperties(props);
        this.storage.updateLibrary(lib3,newLib);
        libs = this.storage.getLibraries();
        assertEquals("Libraries count",1,libs.length);  //NOI18N
        assertLibEquals(libs, new String[] {"Library1"});   //NOI18N
        lib3 = (LibraryImplementation3)libs[0];
        assertEquals(1, lib3.getProperties().size());
        assertEquals("test_value", lib3.getProperties().get("test_prop"));  //NOI18N
    }
    
    private static void assertLibEquals (LibraryImplementation[] libs, String[] names) {
        assertEquals("Libraries Equals (size)",names.length,libs.length);
        Set<String> s = new HashSet<String>(Arrays.asList(names)); //Ordering is not important
        for (LibraryImplementation lib : libs) {
            String name = lib.getName();
            assertTrue("Libraries Equals (unknown library "+name+")", s.remove(name));
        }
    }
    
    static void registerLibraryTypeProvider () throws Exception {
        registerLibraryTypeProvider(TestLibraryTypeProvider.class);
    }
    static void registerLibraryTypeProvider (Class<? extends LibraryTypeProvider> type) throws Exception {
        StringTokenizer tk = new StringTokenizer("org-netbeans-api-project-libraries/LibraryTypeProviders","/");
        FileObject root = FileUtil.getConfigRoot();
        while (tk.hasMoreElements()) {
            String pathElement = tk.nextToken();
            FileObject tmp = root.getFileObject(pathElement);
            if (tmp == null) {
                tmp = root.createFolder(pathElement);
            }
            root = tmp;
        }
        if (root.getChildren().length == 0) {
//            FileObject inst = root.createData("TestLibraryTypeProvider","instance");
//            inst.setAttribute("newvalue","")
            InstanceDataObject.create (DataFolder.findFolder(root),"TestLibraryTypeProvider", type);
        }
    }
    
    static FileObject createLibraryDefinition (final FileObject storageFolder, final String libName, final String displayName) throws IOException {
        final FileObject[] ret = new FileObject[1];

        storageFolder.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
            public void run () throws IOException {
                FileObject defFile = storageFolder.createData(libName,"xml");
                ret[0] = defFile;
                FileLock lock = null;
                PrintWriter out = null;
                try {
                    lock = defFile.lock();
                    out = new PrintWriter(new OutputStreamWriter(defFile.getOutputStream (lock),"UTF-8"));
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");      //NOI18N
                    out.println("<!DOCTYPE library PUBLIC \"-//NetBeans//DTD Library Declaration 1.0//EN\" \"http://www.netbeans.org/dtds/library-declaration-1_0.dtd\">"); 
                    out.println("<library version=\"1.0\">");
                    out.println("\t<name>"+libName+"</name>");
                    out.println("\t<type>"+TestLibraryTypeProvider.TYPE+"</type>");
                    for (int i = 0; i < TestLibraryTypeProvider.supportedTypes.length; i++) {
                        out.println("\t<volume>");
                        out.println ("\t\t<type>"+TestLibraryTypeProvider.supportedTypes[i]+"</type>");
                        out.println("\t</volume>");
                    }
                    out.println("</library>");
                    if (displayName != null) {
                        defFile.setAttribute("displayName", displayName);
                    }
                } finally {
                    if (out !=  null)
                        out.close();
                    if (lock != null)
                        lock.releaseLock();
                }
            }
        });
        return ret[0];
    }
    
    static class TestListener implements PropertyChangeListener {
        
        private List<String> eventNames = new ArrayList<String>();
        
        public List<String> getEventNames () {
            return this.eventNames;
        }
        
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            this.eventNames.add (propertyChangeEvent.getPropertyName());
        }
        
        public void reset () {
            this.eventNames.clear();
        }
        
    }
    
    
    static class TestEntityCatalog extends EntityCatalog {        
        
        private static final String DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!ELEMENT library (name, type, description?, localizing-bundle?, volume*) >\n" +
            "<!ATTLIST library version CDATA #FIXED \"1.0\" >\n" +
            "<!ELEMENT name (#PCDATA) >\n" +
            "<!ELEMENT description (#PCDATA) >\n" +
            "<!ELEMENT localizing-bundle (#PCDATA)>\n" +
            "<!ELEMENT volume (type, resource*) >\n" +
            "<!ELEMENT type (#PCDATA) >\n" +
            "<!ELEMENT resource (#PCDATA) >\n";
        
        public InputSource resolveEntity(String str, String str1) throws SAXException, IOException {
            if ("-//NetBeans//DTD Library Declaration 1.0//EN".equals(str)) {
                InputSource in = new InputSource (new StringReader(DTD));            
                return in;
            }
            else {
                return null;
            }
        }                
    }
    
    public static class TestLibraryTypeProvider implements LibraryTypeProvider, java.io.Serializable {
        
        static final String[] supportedTypes = new String[] {"bin","src"};
        
        static final String TYPE = "Test";
        
        private boolean createdCalled;
        
        private boolean deletedCalled;
        
        public java.beans.Customizer getCustomizer(String volumeType) {
            return null;
        }

        public void libraryDeleted(LibraryImplementation libraryImpl) {
            this.deletedCalled = true;
        }

        public void libraryCreated(LibraryImplementation libraryImpl) {
            this.createdCalled = true;
        }
        
        public void reset () {
            this.createdCalled = false;
            this.deletedCalled = false;
        }
        
        public boolean wasCreatedCalled () {
            return this.createdCalled;
        }
        
        public boolean wasDeletedCalled () {
            return this.deletedCalled;
        }

        public String[] getSupportedVolumeTypes() {
            return supportedTypes;
        }

        public Lookup getLookup() {
            return Lookup.EMPTY;
        }

        public String getLibraryType() {
            return TYPE;
        }

        public String getDisplayName() {
            return "Test Library Type";
        }

        public LibraryImplementation createLibrary() {
            assert !ProjectManager.mutex().isReadAccess();
            return new TestLibrary ();
        }
        
    }

    private static class TestLibraryOld implements LibraryImplementation {
        
        private String name;
        private String locBundle;
        private String description;
        private Map<String,List<URL>> contents;
        private PropertyChangeSupport support;
        
        public TestLibraryOld () {
            this.support = new PropertyChangeSupport (this);
            this.contents = new HashMap<String,List<URL>>(2);
        }
        
        public TestLibraryOld (String name) {
            this ();            
            this.name = name;
        }
        
        public TestLibraryOld (TestLibraryOld lib) {
            this ();
            this.name = lib.name;
            this.locBundle = lib.locBundle;
            this.description = lib.description;
            this.contents = lib.contents;
        }
        
        @Override
        public String getType() {
            return TestLibraryTypeProvider.TYPE;
        }
        
        @Override
        public String getName () {
            return this.name;
        }
        
        @Override
        public void setName(String name) {
            this.name = name;
            this.support.firePropertyChange(PROP_NAME,null,null);
        }
        
        @Override
        public String getLocalizingBundle() {
            return this.locBundle;
        }

        @Override
        public void setLocalizingBundle(String resourceName) {
            this.locBundle = resourceName;
            this.support.firePropertyChange("localizingBundle",null,null);
        }
        
        @Override
        public String getDescription() {
            return this.description;
        }

        @Override
        public void setDescription(String text) {
            this.description = text;
            this.support.firePropertyChange(PROP_DESCRIPTION,null,null);
        }

        @Override
        public List<URL> getContent(String volumeType) throws IllegalArgumentException {
            for (String t : TestLibraryTypeProvider.supportedTypes) {
                if (t.equals(volumeType)) {
                    List<URL> l = this.contents.get(volumeType);
                    if (l == null) {
                        l = Collections.emptyList();
                    }
                    return l;
                }
            }
            throw new IllegalArgumentException ();
        }

        @Override
        public void setContent(String volumeType, List<URL> path) throws IllegalArgumentException {
            for (String t : TestLibraryTypeProvider.supportedTypes) {
                if (t.equals(volumeType)) {
                    List<URL> l = this.contents.put(volumeType, path);
                    this.support.firePropertyChange(PROP_CONTENT,null,null);
                    return;
                }
            }
            throw new IllegalArgumentException ();
        }                       
        
        @Override
        public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
            this.support.addPropertyChangeListener(l);
        }
        
        @Override
        public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
            this.support.removePropertyChangeListener(l);
        }

        @Override
        public int hashCode() {
            int hash = 31;
            hash = hash*17 + (name == null ? 0 : name.hashCode());
            return hash;
        }

        @Override
        public boolean equals (final Object other) {
            if (other instanceof TestLibraryOld) {
                final TestLibraryOld otherLib = (TestLibraryOld) other;
                return name == null ? otherLib.name == null : name.equals(otherLib.name);
            }
            return false;
        }
    }
    
    private static class TestLibrary extends TestLibraryOld implements LibraryImplementation3 {
        
        private String dName;
        private Map<String,String> props = new HashMap<String, String>();
        
        public TestLibrary () {
            super();
        }
        
        public TestLibrary (String name) {
            super(name);
        }
        
        public TestLibrary (TestLibrary lib) {
            super(lib);
        }
        
        @Override
        public Map<String, String> getProperties() {
            return props;
        }

        @Override
        public void setProperties(Map<String, String> properties) {
            this.props = properties;
        }

        @Override
        public void setDisplayName(String displayName) {
            this.dName = displayName;
        }

        @Override
        public String getDisplayName() {
            return dName;
        }
    }
    
}
