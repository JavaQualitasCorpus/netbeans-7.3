/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javawebstart.ui.customizer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.j2seproject.J2SEProject;
import org.netbeans.modules.java.j2seproject.J2SEProjectGenerator;
import org.netbeans.modules.java.platform.JavaPlatformProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.SpecificationVersion;
import org.openide.util.test.MockLookup;

/**
 * 
 * @author Petr Somol
 * @author Tomas Zezula
 */
public class JWSProjectPropertiesTest extends NbTestCase {

    public JWSProjectPropertiesTest(String name) {
        super(name);
    }

    private J2SEProject p;
    File wsPrimary = null;
    File wsSecondary = null;
    File pgPrimary = null;
    File pgSecondary = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockLookup.setLayersAndInstances(new TestPlatformProvider());
        clearWorkDir();
        J2SEProjectGenerator.createProject(getWorkDir(), "test", null, null, null, false);
        File workDir = getWorkDir();
        p = (J2SEProject) ProjectManager.getDefault().findProject(FileUtil.toFileObject(workDir));
        
        File jdkDirPrimary = new File(workDir.getPath() + "/foo/bar/jdk");
        jdkDirPrimary.mkdirs();
        setProperty("jdk.home", jdkDirPrimary.getPath());
        System.out.println("Mockup jdk.home = " + getProperty("jdk.home"));
        
        File jdkDirSecondary = new File(workDir.getPath() + "/foo/bar/otherjdk");
        jdkDirSecondary.mkdirs();
        setProperty("platforms.otherjdk.home", jdkDirSecondary.getPath());
        System.out.println("Mockup platforms.otherjdk.home = " + getProperty("platforms.otherjdk.home"));

        File libDirPrimary = new File(jdkDirPrimary.getPath() + "/jre/lib");
        File libDirSecondary = new File(jdkDirSecondary.getPath() + "/jre/lib");
        libDirPrimary.mkdirs();
        libDirSecondary.mkdirs();
        wsPrimary = new File(libDirPrimary.getPath() + "/javaws.jar");
        pgPrimary = new File(libDirPrimary.getPath() + "/plugin.jar");
        wsSecondary = new File(libDirSecondary.getPath() + "/javaws.jar");
        pgSecondary = new File(libDirSecondary.getPath() + "/plugin.jar");
        wsPrimary.createNewFile();
        pgPrimary.createNewFile();
        wsSecondary.createNewFile();
        pgSecondary.createNewFile();
    }

    public void testUpdateWebStartJarsOnOpen() throws Exception {
        System.out.println("Test updateWebStartJarsOnOpen():");

        setProperty("platform.active", "default_platform");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, "");
        updateWebStartJarsOnOpen(false);
        assertEquals("", getProperty(JWSProjectProperties.ENDORSED_CLASSPATH));

        setProperty("platform.active", "default_platform");
        System.out.println("Test preserving of all items except the non-existing and disabled WS references on open:");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:", // WS unrelated
            "${jdk.home}/jre/lib/javaws.jar:", // exists
            "${jdk.home}/jre/lib/plugin.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnOpen(false);
        assertEquals("foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");

        setProperty("platform.active", "default_platform");
        setProperty("jnlp.descriptor", "application");        
        System.out.println("Test substitute of missing WS application items on open with WS enabled:");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:",// WS unrelated
            "${platforms.otherjdk.home}/jre/lib/javaws.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnOpen(true);
        assertEquals("${jdk.home}/jre/lib/javaws.jar:" + 
                "${platforms.otherjdk.home}/jre/lib/javaws.jar:" +
                "foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");
        
        setProperty("platform.active", "default_platform");
        setProperty("jnlp.descriptor", "applet");
        System.out.println("Test substitute of missing WS applet items on open with WS enabled while preserving existing form of references:");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:",// WS unrelated
            wsPrimary.getAbsolutePath()+":", // exists
            "${platforms.otherjdk.home}/jre/lib/javaws.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnOpen(true);
        assertEquals(wsPrimary.getAbsolutePath()+":" + 
                "${platforms.otherjdk.home}/jre/lib/javaws.jar:" +
                "${jdk.home}/jre/lib/plugin.jar:" + 
                "foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");

        setProperty("platform.active", "otherjdk");
        setProperty("jnlp.descriptor", "applet");
        System.out.println("Test substitute of missing WS applet items on open with non-default active platform:");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:", // WS unrelated
            wsPrimary.getAbsolutePath()+":", // exists
            "${platforms.otherjdk.home}/jre/lib/javaws.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnOpen(true);
        assertEquals(wsPrimary.getAbsolutePath()+":" + 
                "${platforms.otherjdk.home}/jre/lib/javaws.jar:" + 
                "${platforms.otherjdk.home}/jre/lib/plugin.jar:" +
                "foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");
    }

    public void testUpdateWebStartJarsOnChange() throws Exception {
        System.out.println("Test updateWebStartJarsOnChange():");

        setProperty("platform.active", "default_platform");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, "");
        updateWebStartJarsOnChange(false);
        assertEquals("", getProperty(JWSProjectProperties.ENDORSED_CLASSPATH));

        setProperty("platform.active", "default_platform");
        System.out.println("Test preserving of all items except WS references on change (WS disabled):");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:", // WS unrelated
            "${jdk.home}/jre/lib/javaws.jar:", // exists
            "${platforms.otherjdk.home}/jre/lib/plugin.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnChange(false);
        assertEquals("foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");

        setProperty("platform.active", "default_platform");
        setProperty("jnlp.descriptor", "application");        
        System.out.println("Test replace all WS items by only the necessary WS application items in reference form (WS enabled):");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:",// WS unrelated
            "${platforms.otherjdk.home}/jre/lib/javaws.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnChange(true);
        assertEquals("${jdk.home}/jre/lib/javaws.jar:" + 
                "foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");
        
        setProperty("platform.active", "default_platform");
        setProperty("jnlp.descriptor", "applet");
        System.out.println("Test replace all WS items by only the necessary WS applet items in reference form (WS enabled):");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:",// WS unrelated
            wsPrimary.getAbsolutePath()+":", // exists
            "${platforms.otherjdk.home}/jre/lib/javaws.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnChange(true);
        assertEquals("${jdk.home}/jre/lib/javaws.jar:" +
                "${jdk.home}/jre/lib/plugin.jar:" + 
                "foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");

        setProperty("platform.active", "otherjdk");
        setProperty("jnlp.descriptor", "applet");
        System.out.println("Test replace all WS items by only the necessary WS applet items in reference form (WS enabled) with non-default active platform:");
        setProperty(JWSProjectProperties.ENDORSED_CLASSPATH, new String[]{
            "foo:", // WS unrelated
            wsPrimary.getAbsolutePath()+":", // exists
            "${platforms.otherjdk.home}/jre/lib/javaws.jar:", // exists
            "${foo.bar}:", // WS unrelated
            "foo.bar/javaws.jar:", // does not exist
            "${foo.bar}/javaws.jar"}); // does not exist
        updateWebStartJarsOnChange(true);
        assertEquals("${platforms.otherjdk.home}/jre/lib/javaws.jar:" + 
                "${platforms.otherjdk.home}/jre/lib/plugin.jar:" +
                "foo:${foo.bar}", getRawProperty(JWSProjectProperties.ENDORSED_CLASSPATH));
        System.out.println("OK");
    }
    
    private String getProperty(String prop){
         return p.evaluator().getProperty(prop);
    }    

    private String getRawProperty(String prop){
        EditableProperties ep = p.getUpdateHelper().getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        return ep.getProperty(prop);
    }    
    
    private void setProperty(String prop, String value) throws IOException {
        EditableProperties ep = p.getUpdateHelper().getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        ep.setProperty(prop, value);
        p.getUpdateHelper().putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, ep);
        ProjectManager.getDefault().saveProject(p);
    }

    private void setProperty(String prop, String[] values) throws IOException {
        EditableProperties ep = p.getUpdateHelper().getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        ep.setProperty(prop, values);
        p.getUpdateHelper().putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, ep);
        ProjectManager.getDefault().saveProject(p);
    }

    private void updateWebStartJarsOnOpen(boolean isWebStart) throws IOException {
        EditableProperties ep = p.getUpdateHelper().getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        JWSProjectProperties.updateWebStartJarsOnOpen(ep, p.evaluator(), isWebStart);
        p.getUpdateHelper().putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, ep);
        ProjectManager.getDefault().saveProject(p);
    }

    private void updateWebStartJarsOnChange(boolean isWebStart) throws IOException {
        EditableProperties ep = p.getUpdateHelper().getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        JWSProjectProperties.updateWebStartJarsOnChange(ep, p.evaluator(), isWebStart);
        p.getUpdateHelper().putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, ep);
        ProjectManager.getDefault().saveProject(p);
    }

    private static class TestPlatformProvider implements JavaPlatformProvider {

        private JavaPlatform platformDefault;
        private JavaPlatform platformOther;

        @Override
        public void removePropertyChangeListener(java.beans.PropertyChangeListener listener) {
        }

        @Override
        public void addPropertyChangeListener(java.beans.PropertyChangeListener listener) {
        }

        @Override
        public JavaPlatform[] getInstalledPlatforms()  {
            return new JavaPlatform[] {
                getDefaultPlatform(),
                getOtherPlatform(),
            };
        }

        @Override
        public JavaPlatform getDefaultPlatform()  {
            if (this.platformDefault == null) {
                this.platformDefault = new TestPlatformDefault ();
            }
            return this.platformDefault;
        }

        public JavaPlatform getOtherPlatform()  {
            if (this.platformOther == null) {
                this.platformOther = new TestPlatformOther ();
            }
            return this.platformOther;
        }
    }

    private static class TestPlatformDefault extends JavaPlatform {

        @Override
        public FileObject findTool(String toolName) {
            return null;
        }

        @Override
        public String getVendor() {
            return "me";
        }

        @Override
        public ClassPath getStandardLibraries() {
            return ClassPathSupport.createClassPath(new URL[0]);
        }

        @Override
        public Specification getSpecification() {
            return new Specification ("j2se", new SpecificationVersion ("1.6"));
        }

        @Override
        public ClassPath getSourceFolders() {
            return null;
        }

        @Override
        public Map<String,String> getProperties() {
            return Collections.singletonMap("platform.ant.name","default_platform");
        }

        @Override
        public List<URL> getJavadocFolders() {
            return null;
        }

        @Override
        public Collection<FileObject> getInstallFolders() {
            return Collections.emptySet();
        }

        @Override
        public String getDisplayName() {
            return "TestPlatformDefault";
        }

        @Override
        public ClassPath getBootstrapLibraries() {
            return ClassPathSupport.createClassPath(new URL[0]);
        }

    }

    private static class TestPlatformOther extends TestPlatformDefault {

        @Override
        public Map<String,String> getProperties() {
            return Collections.singletonMap("platform.ant.name","otherjdk");
        }

        @Override
        public String getDisplayName() {
            return "TestPlatformOther";
        }

    }

}

