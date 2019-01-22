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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.java.api.common.util;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.modules.java.api.common.project.ui.customizer.MainClassChooser;
import org.netbeans.spi.project.libraries.LibraryImplementation3;
import org.netbeans.spi.project.libraries.support.LibrariesSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Parameters;

/**
 * Common project utilities. This is a helper class; all methods are static.
 * @author Tomas Mysik
 */
public final class CommonProjectUtils {

    private CommonProjectUtils() {
    }

    // XXX copied from J2SEProjectUtilities, should be part of some API probably (JavaPlatformManager?)
    /**
     * Returns the active platform used by the project or null if the active
     * project platform is broken.
     * @param activePlatformId the name of platform used by Ant script or null
     * for default platform.
     * @return active {@link JavaPlatform} or null if the project's platform
     * is broken
     */
    public static JavaPlatform getActivePlatform(final String activePlatformId) {
        final JavaPlatformManager pm = JavaPlatformManager.getDefault();
        if (activePlatformId == null) {
            return pm.getDefaultPlatform();
        }

        JavaPlatform[] installedPlatforms = pm.getPlatforms(null, new Specification("j2se", null)); //NOI18N
        for (JavaPlatform javaPlatform : installedPlatforms) {
            String antName = javaPlatform.getProperties().get("platform.ant.name"); //NOI18N
            if (antName != null && antName.equals(activePlatformId)) {
                return javaPlatform;
            }
        }
        return null;
    }

    /**
     * Converts the ant reference to the name of the referenced property.
     * @param property the name of the referenced property.
     * @return the referenced property.
     */
    public static String getAntPropertyName(String property) {
        if (property != null
                && property.startsWith("${") // NOI18N
                && property.endsWith("}")) { // NOI18N
            return property.substring(2, property.length() - 1);
        }
        return property;
    }

    public static Collection<ElementHandle<TypeElement>> getMainMethods (final FileObject fo) {
        // support for unit testing
        if (fo == null || MainClassChooser.unitTestingSupport_hasMainMethodResult != null) {
            return Collections.<ElementHandle<TypeElement>>emptySet();
        }
        return SourceUtils.getMainClasses(fo);
    }

    /** Check if the given file object represents a source with the main method.
     *
     * @param fo source
     * @return true if the source contains the main method
     */
    public static boolean hasMainMethod(FileObject fo) {
        // support for unit testing
        if (MainClassChooser.unitTestingSupport_hasMainMethodResult != null) {
            return MainClassChooser.unitTestingSupport_hasMainMethodResult.booleanValue ();
        }
        if (fo == null) {
            // ??? maybe better should be thrown IAE
            return false;
        }
        return !SourceUtils.getMainClasses(fo).isEmpty();
    }

    public static boolean isMainClass (final String className, ClassPath bootPath, ClassPath compilePath, ClassPath sourcePath) {
        ClasspathInfo cpInfo = ClasspathInfo.create(bootPath, compilePath, sourcePath);
        return SourceUtils.isMainClass(className, cpInfo);
    }

    /**
     * Creates a {@link LibraryImplementation3} that can subsequently be used with
     * both Ant and Maven based Java projects.
     * @param classpath local library classpath for use by Ant
     * @param src local library sources for use by Ant
     * @param javadoc local Javadoc path for use by Ant
     * @param mavendeps list of maven dependencies in the form of groupId:artifactId:version:type,
     *    for example org.eclipse.persistence:eclipselink:2.3.2:jar
     * @param mavenrepos list of maven repositories in the form of layout:url,
     *    for example default:http://download.eclipse.org/rt/eclipselink/maven.repo/
     * @return {@link LibraryImplementation3} representing the information passed as parameters
     * @since 1.40
     */
    public static LibraryImplementation3 createJavaLibraryImplementation(
            @NonNull final String name,
            @NonNull final URL[] classPath,
            @NonNull final URL[] sources,
            @NonNull final URL[] javadoc,
            @NonNull final String[] mavendeps,
            @NonNull final String[] mavenrepos) {
        Parameters.notNull("name", name);   //NOI18N
        Parameters.notNull("classPath", classPath); //NOI18N
        Parameters.notNull("src", sources); //NOI18N
        Parameters.notNull("javadoc", javadoc); //NOI18N
        Parameters.notNull("mavendeps", mavendeps);  //NOI18N
        Parameters.notNull("mavenrepos", mavenrepos);   //NOI18N
        final LibraryImplementation3 impl = LibrariesSupport.createLibraryImplementation3(
                "j2se",         //NOI18N
                "classpath",    //NOI18N
                "src",          //NOI18N
                "javadoc"       //NOI18N
                );
        impl.setName(name);
        impl.setContent("classpath", Arrays.asList(classPath)); //NOI18N
        impl.setContent("src", Arrays.asList(sources));     //NOI18N
        impl.setContent("javadoc", Arrays.asList(javadoc));     //NOI18N
        final Map<String,String> props = new HashMap<String, String>();
        // properties: "maven-dependencies", "maven-repositories"
        props.put("maven-dependencies", getPropertyValue(mavendeps));  //NOI18N
        props.put("maven-repositories", getPropertyValue(mavenrepos)); //NOI18N
        impl.setProperties(props);
        return impl;
    }

    @NonNull
    private static String getPropertyValue(@NonNull final String[] values) {
        final StringBuilder result = new StringBuilder();
        for (String value : values) {
            result.append(value);
            result.append(' '); //NOI18N
        }
        return result.length() == 0 ?
           result.toString() :
           result.substring(0, result.length()-1);
    }

}