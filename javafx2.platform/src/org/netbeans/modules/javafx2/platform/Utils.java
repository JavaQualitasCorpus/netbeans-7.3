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
package org.netbeans.modules.javafx2.platform;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.modules.java.j2seplatform.api.J2SEPlatformCreator;
import org.netbeans.modules.javafx2.platform.api.JavaFXPlatformUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 * Utility class for platform properties manipulation
 * 
 * @author Anton Chechel
 * @author Petr Somol
 */
public final class Utils {
    /**
     * Default name for automatically registered JavaFX platform
     */
    public static final String DEFAULT_FX_PLATFORM_NAME = NbBundle.getMessage(Utils.class, "Default_JavaFX_Platform"); // NOI18N
    
    /**
     * Property for not checking matching JavaFX and running JVM architecture
     */
    public static final String NO_PLATFORM_CHECK_PROPERTY = "org.netbeans.modules.javafx2.platform.NoPlatformCheck"; // NOI18N

    private static final String PLATFORM_PREFIX = "platforms"; // NOI18N
    private static final String JAVAFX_SDK_PREFIX = "javafx.sdk.home"; // NOI18N
    private static final String JAVAFX_RUNTIME_PREFIX = "javafx.runtime.home"; // NOI18N
    private static final String JAVAFX_SOURCES_PREFIX = "javafx.src"; // NOI18N
    private static final String JAVAFX_JAVADOC_PREFIX = "javafx.javadoc"; // NOI18N

    private static final Logger LOGGER = Logger.getLogger("org.netbeans.modules.javafx2.platform.Utils"); // NOI18N
    
    private Utils() {
    }

    /**
     * Indicates whether running inside a test.
     * Used to bypass J2SE platform creation
     * which causes problems in test environment.
     */
    private static boolean isTest = false;
    
    /**
     * Returns isTest flag value
     * 
     * @return isTest flag value
     */
    public static boolean isTest() {
        return isTest;
    }

    /**
     * Sets isTest flag, unit test should set it to true
     * 
     * @param isTest flag
     */
    public static void setIsTest(boolean test) {
        isTest = test;
    }
    
    /**
     * Returns key for <b>JavaFX SDK location</b> IDE global property value for given java platform
     * 
     * @param IDE java platform instance
     * @return key for JavaFX SDK location
     */
    @NonNull
    public static String getSDKPropertyKey(@NonNull JavaPlatform platform) {
        Parameters.notNull("platform", platform); // NOI18N
        String platformName = platform.getProperties().get(JavaFXPlatformUtils.PLATFORM_ANT_NAME);
        return PLATFORM_PREFIX + '.' + platformName + '.' + JAVAFX_SDK_PREFIX; // NOI18N
    }
    
    /**
     * Returns key for <b>JavaFX Runtime location</b> IDE global property value for given java platform
     * 
     * @param IDE java platform instance
     * @return key for JavaFX Runtime location
     */
    @NonNull
    public static String getRuntimePropertyKey(@NonNull JavaPlatform platform) {
        Parameters.notNull("platform", platform); // NOI18N
        String platformName = platform.getProperties().get(JavaFXPlatformUtils.PLATFORM_ANT_NAME);
        return PLATFORM_PREFIX + '.' + platformName + '.' + JAVAFX_RUNTIME_PREFIX; // NOI18N
    }

    /**
     * Returns key for <b>JavaFX SDK location</b> IDE global property value for given java platform
     * 
     * @param IDE java platform name
     * @return key for JavaFX SDK location
     */
    @NonNull
    public static String getSDKPropertyKey(@NonNull String platformName) {
        return PLATFORM_PREFIX + '.' + platformName + '.' + JAVAFX_SDK_PREFIX; // NOI18N
    }

    /**
     * Returns key for <b>JavaFX Runtime location</b> IDE global property value for given java platform
     * 
     * @param IDE java platform name
     * @return key for JavaFX Runtime location
     */
    @NonNull
    public static String getRuntimePropertyKey(@NonNull String platformName) {
        return PLATFORM_PREFIX + '.' + platformName + '.' + JAVAFX_RUNTIME_PREFIX; // NOI18N
    }

    /**
     * Returns key for <b>JavaFX Javadoc location</b> IDE global property value for given java platform
     * 
     * @param IDE java platform instance
     * @return key for JavaFX Javadoc location
     */
    @NonNull
    public static String getJavadocPropertyKey(@NonNull JavaPlatform platform) {
        Parameters.notNull("platform", platform); // NOI18N
        String platformName = platform.getProperties().get(JavaFXPlatformUtils.PLATFORM_ANT_NAME);
        return PLATFORM_PREFIX + '.' + platformName + '.' + JAVAFX_JAVADOC_PREFIX; // NOI18N
    }

    /**
     * Returns key for <b>JavaFX Sources location</b> IDE global property value for given java platform
     * 
     * @param IDE java platform instance
     * @return key for JavaFX Sources location
     */
    @NonNull
    public static String getSourcesPropertyKey(@NonNull JavaPlatform platform) {
        Parameters.notNull("platform", platform); // NOI18N
        String platformName = platform.getProperties().get(JavaFXPlatformUtils.PLATFORM_ANT_NAME);
        return PLATFORM_PREFIX + '.' + platformName + '.' + JAVAFX_SOURCES_PREFIX; // NOI18N
    }
    
    /**
     * Creates new instance of Java Platform and registers JavaFX extension for it.
     * If Java Platform with a given name already exists, just registers JavaFX extension.
     * If <b>isTest</b> is set to true, this method does nothing and returns null.
     * 
     * @param platformName the desired display name
     * @param sdkPath JavaFX SDK location
     * @param runtimePath JavaFX Runtime location
     * @param javadocPath JavaFX javadoc location
     * @param srcPath JavaFX sources location
     * @return instance of created Java Platform, or null if creation was not successful
     * @throws IOException if the platform was invalid or its definition could not be stored
     */
    @CheckForNull
    public static JavaPlatform createJavaFXPlatform(@NonNull String platformName, @NonNull String sdkPath,
            @NonNull String runtimePath, @NullAllowed String javadocPath, @NullAllowed String srcPath)
            throws IOException, IllegalArgumentException {

        Parameters.notNull("platformName", platformName); // NOI18N
        Parameters.notNull("sdkPath", sdkPath); // NOI18N
        Parameters.notNull("runtimePath", runtimePath); // NOI18N
        
        // 32b vs 64b check
        if (!isArchitechtureCorrect(runtimePath) && !isTest) {
            return null;
        }

        JavaPlatform defaultPlatform = JavaPlatformManager.getDefault().getDefaultPlatform();
        FileObject platformFolder = defaultPlatform.getInstallFolders().iterator().next();
        JavaPlatform platform = null;

        if(isTest) {
            // Re-using default platform as FX platform because createJ2SEPlatform fails when called from test
            // Note that outside tests the default platform must not be used as FX platform
            platform = defaultPlatform;
        } else {
            try {
                platform = J2SEPlatformCreator.createJ2SEPlatform(platformFolder, platformName);
                LOGGER.log(Level.INFO, "\"{0}\" has been created successfully", platformName); // NOI18N
            } catch (IllegalArgumentException illegalArgumentException) {
                LOGGER.log(Level.WARNING, "Java Platform \"{0}\" already exists, skipping platform creation", platformName); // NOI18N
                JavaPlatform[] installedPlatforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
                for (JavaPlatform jp : installedPlatforms) {
                    String name = jp.getProperties().get(JavaFXPlatformUtils.PLATFORM_ANT_NAME);
                    if (name.equals(platformName)) {
                        platform = jp;
                        break;
                    }
                }
            }
        }
        
        if (platform != null) {
            Map<String, String> map = new HashMap<String, String>(2);
            map.put(Utils.getSDKPropertyKey(platform), sdkPath);
            map.put(Utils.getRuntimePropertyKey(platform), runtimePath);
            if (javadocPath != null) {
                map.put(Utils.getJavadocPropertyKey(platform), javadocPath);
            }
            if (srcPath != null) {
                map.put(Utils.getSourcesPropertyKey(platform), srcPath);
            }
            PlatformPropertiesHandler.saveGlobalProperties(map);
            LOGGER.log(Level.INFO, "Java FX extension for \"{0}\" has been registered successfully", platformName); // NOI18N
        } else {
            LOGGER.log(Level.WARNING, "Java FX extension for \"{0}\" has not been registered!", platformName); // NOI18N
        }
        
        return platform;
    }
    
    /**
     * Determines whether architecture (32b vs 64b) of currently running VM
     * matches given JavaFX Runtime
     * 
     * @param runtimePath JavaFX Runtime location
     * @return is correct architecture
     */
    public static boolean isArchitechtureCorrect(@NonNull String runtimePath) {
        Parameters.notNull("runtimePath", runtimePath); // NOI18N
        
        if (Boolean.getBoolean(NO_PLATFORM_CHECK_PROPERTY)) { 
            return true;
        }
        
//        try {
//            if (Utilities.isUnix() || Utilities.isMac()) {
//                System.load(runtimePath + File.separatorChar + "bin" + File.separatorChar + "libmat.jnilib"); // NOI18N
//                return true;
//            } else if (Utilities.isWindows()) {
//                System.load(runtimePath + File.separatorChar + "bin" + File.separatorChar + "mat.dll"); // NOI18N
//            }
//        } catch (Throwable t) {
//            return false;
//        }
        return true;
    }

    // TODO what if jar names/locations will be changed?
    @NonNull
    public static List<? extends URL> getRuntimeClassPath(@NonNull final File javafxRuntime) {
        Parameters.notNull("javafxRuntime", javafxRuntime); //NOI18N
        final List<URL> result = new ArrayList<URL>();
        final File lib = new File (javafxRuntime,"lib");    //NOI18N
        final File[] children = lib.listFiles(new FileFilter() {
            @Override
            public boolean accept(@NonNull final File pathname) {
                return pathname.getName().toLowerCase().endsWith(".jar");  //NOI18N
            }
        });
        if (children != null) {
            for (File f : children) {
                final URL root = FileUtil.urlForArchiveOrDir(f);
                if (root != null) {
                    result.add(root);
                }
            }
        }
        return result;
    }

}