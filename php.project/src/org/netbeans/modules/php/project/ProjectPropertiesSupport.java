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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.project;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.event.ChangeListener;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.modules.php.api.executable.InvalidPhpExecutableException;
import org.netbeans.modules.php.api.executable.PhpInterpreter;
import org.netbeans.modules.php.api.util.Pair;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.modules.php.project.api.PhpLanguageProperties;
import org.netbeans.modules.php.project.api.PhpOptions;
import org.netbeans.modules.php.project.ui.BrowseTestSources;
import org.netbeans.modules.php.project.ui.customizer.PhpProjectProperties;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 * Helper class for getting <b>all</b> the properties of a PHP project.
 * <p>
 * <b>This class is the preferred way to get PHP project properties.</b>
 * @author Tomas Mysik
 */
public final class ProjectPropertiesSupport {

    private ProjectPropertiesSupport() {
    }

    // XXX use it everywhere!
    /**
     * Produce a machine-independent relativized version of a filename from a dir.
     * If path cannot be relative, the full path of the given file is returned.
     * @param dir base directory
     * @param file file to be relativized
     * @return relativized version of a filename from a dir or full path of the given file if the path cannot be relativized
     * @see PropertyUtils#relativizeFile(File, File)
     */
    public static String relativizeFile(File dir, File file) {
        String relativePath = PropertyUtils.relativizeFile(dir, file);
        if (relativePath == null) {
            // path cannot be relativized => use absolute path (any VCS can be hardly use, of course)
            relativePath = file.getAbsolutePath();
        }
        return relativePath;
    }

    /**
     * <b>This method should not be used, use other methods in this class.</b>
     * <p>
     * Use this method only if you don't want to show customizer automatically
     * or if you understand what you are doing ;)
     * @see #addWeakPropertyEvaluatorListener(org.netbeans.modules.php.project.PhpProject, java.beans.PropertyChangeListener)
     */
    public static PropertyEvaluator getPropertyEvaluator(PhpProject project) {
        return project.getEvaluator();
    }

    public static void addWeakPropertyEvaluatorListener(PhpProject project, PropertyChangeListener listener) {
        project.addWeakPropertyEvaluatorListener(listener);
    }

    public static void addWeakIgnoredFilesListener(PhpProject project, ChangeListener listener) {
        project.addWeakIgnoredFilesListener(listener);
    }

    public static boolean addWeakProjectPropertyChangeListener(PhpProject project, PropertyChangeListener listener) {
        return project.addWeakPropertyChangeListener(listener);
    }

    public static void addProjectPropertyChangeListener(PhpProject project, PropertyChangeListener listener) {
        project.addPropertyChangeListener(listener);
    }

    public static void removeProjectPropertyChangeListener(PhpProject project, PropertyChangeListener listener) {
        project.removePropertyChangeListener(listener);
    }

    public static FileObject getProjectDirectory(PhpProject project) {
        return project.getProjectDirectory();
    }

    public static FileObject getSourcesDirectory(PhpProject project) {
        return project.getSourcesDirectory();
    }

    /**
     * @return test sources directory or <code>null</code> (if not set up yet e.g.)
     */
    public static FileObject getTestDirectory(PhpProject project, boolean showFileChooser) {
        FileObject testsDirectory = project.getTestsDirectory();
        if (testsDirectory != null && testsDirectory.isValid()) {
            return testsDirectory;
        }
        if (showFileChooser) {
            BrowseTestSources panel = new BrowseTestSources(project, NbBundle.getMessage(ProjectPropertiesSupport.class, "LBL_BrowseTests"));
            if (panel.open()) {
                File tests = new File(panel.getTestSources());
                assert tests.isDirectory();
                testsDirectory = FileUtil.toFileObject(tests);
                saveTestSources(project, PhpProjectProperties.TEST_SRC_DIR, tests);
            }
        }
        return testsDirectory;
    }

    /**
     * @return selenium test sources directory or <code>null</code> (if not set up yet e.g.)
     */
    public static FileObject getSeleniumDirectory(PhpProject project, boolean showFileChooser) {
        FileObject seleniumDirectory = project.getSeleniumDirectory();
        if (seleniumDirectory != null && seleniumDirectory.isValid()) {
            return seleniumDirectory;
        }
        if (showFileChooser) {
            BrowseTestSources panel = new BrowseTestSources(project, NbBundle.getMessage(ProjectPropertiesSupport.class, "LBL_BrowseSelenium"));
            if (panel.open()) {
                File selenium = new File(panel.getTestSources());
                assert selenium.isDirectory();
                seleniumDirectory = FileUtil.toFileObject(selenium);
                saveTestSources(project, PhpProjectProperties.SELENIUM_SRC_DIR, selenium);
            }
        }
        return seleniumDirectory;
    }

    public static String getWebRoot(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.WEB_ROOT);
    }

    public static FileObject getWebRootDirectory(PhpProject project) {
        return project.getWebRootDirectory();
    }

    public static File getSourceSubdirectory(PhpProject project, String subdirectoryPath) {
        return getSubdirectory(project, project.getSourcesDirectory(), subdirectoryPath);
    }

    public static File getSubdirectory(PhpProject project, FileObject rootDirectory, String subdirectoryPath) {
        File rootDir = FileUtil.toFile(rootDirectory);
        if (!StringUtils.hasText(subdirectoryPath)) {
            return rootDir;
        }
        // first try to resolve fileobject
        FileObject fo = rootDirectory.getFileObject(subdirectoryPath);
        if (fo != null) {
            return FileUtil.toFile(fo);
        }
        // fallback for OS specific paths (should be changed everywhere, my fault, sorry)
        return PropertyUtils.resolveFile(FileUtil.toFile(rootDirectory), subdirectoryPath);
    }

    public static PhpInterpreter getValidPhpInterpreter(PhpProject project) throws InvalidPhpExecutableException {
        String interpreter = project.getEvaluator().getProperty(PhpProjectProperties.INTERPRETER);
        if (StringUtils.hasText(interpreter)) {
            return PhpInterpreter.getCustom(interpreter);
        }
        return PhpInterpreter.getDefault();
    }

    public static String getPhpInterpreter(PhpProject project) {
        String interpreter = project.getEvaluator().getProperty(PhpProjectProperties.INTERPRETER);
        if (StringUtils.hasText(interpreter)) {
            return interpreter;
        }
        return PhpOptions.getInstance().getPhpInterpreter();
    }

    public static boolean isCopySourcesEnabled(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.COPY_SRC_FILES, false);
    }

    /**
     * @return file or <code>null</code>.
     */
    public static File getCopySourcesTarget(PhpProject project) {
        String targetString = project.getEvaluator().getProperty(PhpProjectProperties.COPY_SRC_TARGET);
        if (targetString != null && targetString.trim().length() > 0) {
            return FileUtil.normalizeFile(new File(targetString));
        }
        return null;
    }

    public static String getEncoding(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.SOURCE_ENCODING);
    }

    public static boolean areShortTagsEnabled(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.SHORT_TAGS, PhpLanguageProperties.SHORT_TAGS_ENABLED);
    }

    public static boolean areAspTagsEnabled(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.ASP_TAGS, PhpLanguageProperties.ASP_TAGS_ENABLED);
    }

    public static PhpLanguageProperties.PhpVersion getPhpVersion(PhpProject project) {
        return getPhpVersion(project.getEvaluator().getProperty(PhpProjectProperties.PHP_VERSION));
    }

    public static PhpLanguageProperties.PhpVersion getPhpVersion(String value) {
        if (value != null) {
            try {
                return PhpLanguageProperties.PhpVersion.valueOf(value);
            } catch (IllegalArgumentException iae) {
                // ignored
            }
        }
        return PhpLanguageProperties.PhpVersion.getDefault();
    }

    /**
     * @return run as type, {@link PhpProjectProperties.RunAsType#LOCAL} is the default.
     */
    public static PhpProjectProperties.RunAsType getRunAs(PhpProject project) {
        PhpProjectProperties.RunAsType runAsType = null;
        String runAs = project.getEvaluator().getProperty(PhpProjectProperties.RUN_AS);
        if (runAs != null) {
            try {
                runAsType = PhpProjectProperties.RunAsType.valueOf(runAs);
            } catch (IllegalArgumentException iae) {
                // ignored
            }
        }
        return runAsType != null ? runAsType : PhpProjectProperties.RunAsType.LOCAL;
    }

    /**
     * @return url or <code>null</code>.
     */
    public static String getUrl(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.URL);
    }

    /**
     * @return index file or <code>null</code>.
     */
    public static String getIndexFile(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.INDEX_FILE);
    }

    /**
     * @return arguments or <code>null</code>.
     */
    public static String getArguments(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.ARGS);
    }

    /**
     * @return PHP arguments or <code>null</code>.
     */
    public static String getPhpArguments(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.PHP_ARGS);
    }

    /**
     * @return working directory or <code>null</code>.
     */
    public static String getWorkDir(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.WORK_DIR);
    }

    /**
     * @return remote connection (configuration) name or <code>null</code>.
     */
    public static String getRemoteConnection(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.REMOTE_CONNECTION);
    }

    /**
     * @return remote (upload) directory or <code>null</code>.
     */
    public static String getRemoteDirectory(PhpProject project) {
        return project.getEvaluator().getProperty(PhpProjectProperties.REMOTE_DIRECTORY);
    }

    /**
     * @return <code>true</code> if permissions should be preserved; default is <code>false</code>.
     */
    public static boolean areRemotePermissionsPreserved(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.REMOTE_PERMISSIONS, false);
    }

    /**
     * @return <code>true</code> if upload is direct (and not using a temporary file); default is <code>false</code>.
     */
    public static boolean isRemoteUploadDirectly(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.REMOTE_UPLOAD_DIRECTLY, false);
    }

    /**
     * @return remote upload or <code>null</code>.
     */
    public static PhpProjectProperties.UploadFiles getRemoteUpload(PhpProject project) {
        PhpProjectProperties.UploadFiles uploadFiles = null;
        String remoteUpload = project.getEvaluator().getProperty(PhpProjectProperties.REMOTE_UPLOAD);
        assert remoteUpload != null;
        try {
            uploadFiles = PhpProjectProperties.UploadFiles.valueOf(remoteUpload);
        } catch (IllegalArgumentException iae) {
            // ignored
        }
        return uploadFiles;
    }

    /**
     * @return debug url (default is DEFAULT_URL).
     */
    public static PhpProjectProperties.DebugUrl getDebugUrl(PhpProject project) {
        String debugUrl = project.getEvaluator().getProperty(PhpProjectProperties.DEBUG_URL);
        if (debugUrl == null) {
            return PhpProjectProperties.DebugUrl.DEFAULT_URL;
        }
        return PhpProjectProperties.DebugUrl.valueOf(debugUrl);
    }

    /**
     * @return list of pairs of remote path (as a String) and local path (absolute path, as a String); empty remote paths are skipped
     *         as well as invalid local paths
     */
    public static List<Pair<String, String>> getDebugPathMapping(PhpProject project) {
        List<String> remotes = StringUtils.explode(
                getString(project, PhpProjectProperties.DEBUG_PATH_MAPPING_REMOTE, null), PhpProjectProperties.DEBUG_PATH_MAPPING_SEPARATOR);
        List<String> locals = StringUtils.explode(
                getString(project, PhpProjectProperties.DEBUG_PATH_MAPPING_LOCAL, null), PhpProjectProperties.DEBUG_PATH_MAPPING_SEPARATOR);
        int remotesSize = remotes.size();
        int localsSize = locals.size();
        List<Pair<String, String>> paths = new ArrayList<Pair<String, String>>(remotesSize);
        for (int i = 0; i < remotesSize; ++i) {
            String remotePath = remotes.get(i);
            if (StringUtils.hasText(remotePath)) {
                // if user has only 1 path and local == sources => property is not stored at all!
                String l = ""; // NOI18N
                if (i < localsSize) {
                    l = locals.get(i);
                }
                String localPath = null;
                File local = new File(l);
                if (local.isAbsolute()) {
                    if (local.isDirectory()) {
                        localPath = local.getAbsolutePath();
                    }
                } else {
                    File subDir = getSourceSubdirectory(project, l);
                    if (subDir.exists()) {
                        localPath = subDir.getAbsolutePath();
                    }
                }

                if (localPath != null) {
                    paths.add(Pair.of(remotePath, localPath));
                }
            }
        }
        Pair<String, String> copySupportPair = getCopySupportPair(project);
        if (copySupportPair != null) {
            paths.add(copySupportPair);
        }
        return paths;
    }

    /**
     * Get debugger proxy (as pair of host, port) or <code>null</code> if it's not set.
     * @return debugger proxy (as pair of host, port) or <code>null</code> if it's not set.
     */
    public static Pair<String, Integer> getDebugProxy(PhpProject project) {
        String host = getString(project, PhpProjectProperties.DEBUG_PROXY_HOST, null);
        if (!StringUtils.hasText(host)) {
            return null;
        }
        return Pair.of(host, getInt(project, PhpProjectProperties.DEBUG_PROXY_PORT, PhpProjectProperties.DEFAULT_DEBUG_PROXY_PORT));
    }

    /**
     * @return file (which can be invalid!) or <code>null</code>
     */
    public static File getPhpUnitBootstrap(PhpProject project) {
        return getFile(project, PhpProjectProperties.PHP_UNIT_BOOTSTRAP);
    }

    /**
     * @return {@code true} if bootstrap file should be used for creating new unit tests, {@code false} otherwise; the default value is {@code false}
     */
    public static boolean usePhpUnitBootstrapForCreateTests(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.PHP_UNIT_BOOTSTRAP_FOR_CREATE_TESTS, false);
    }

    /**
     * @return file (which can be invalid!) or <code>null</code>
     */
    public static File getPhpUnitConfiguration(PhpProject project) {
        return getFile(project, PhpProjectProperties.PHP_UNIT_CONFIGURATION);
    }

    /**
     * @return file (which can be invalid!) or <code>null</code>
     */
    public static File getPhpUnitSuite(PhpProject project) {
        return getFile(project, PhpProjectProperties.PHP_UNIT_SUITE);
    }

    /**
     * @return file (which can be invalid!) or <code>null</code>
     */
    public static File getPhpUnitScript(PhpProject project) {
        return getFile(project, PhpProjectProperties.PHP_UNIT_SCRIPT);
    }

    /**
     * @return {@code true} if all *Test files should be run via PhpUnit (default is {@code false})
     */
    public static boolean runAllTestFilesUsingPhpUnit(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.PHP_UNIT_RUN_TEST_FILES, false);
    }

    public static boolean askForTestGroups(PhpProject project) {
        return getBoolean(project, PhpProjectProperties.PHP_UNIT_ASK_FOR_TEST_GROUPS, false);
    }

    public static String getPhpUnitLastUsedTestGroups(PhpProject project) {
        return getString(project, PhpProjectProperties.PHP_UNIT_LAST_USED_TEST_GROUPS, null);
    }

    public static String getHostname(PhpProject project) {
        return getString(project, PhpProjectProperties.HOSTNAME, null);
    }

    public static String getPort(PhpProject project) {
        return getString(project, PhpProjectProperties.PORT, null);
    }

    public static String getInternalRouter(PhpProject project) {
        return getString(project, PhpProjectProperties.ROUTER, null);
    }

    /**
     * @return instance of Pair<String, String> or null
     */
    private static Pair<String, String> getCopySupportPair(PhpProject project) {
        Pair<String, String> copySupportPair = null;
        if (ProjectPropertiesSupport.isCopySourcesEnabled(project)) {
            File copyTarget = ProjectPropertiesSupport.getCopySourcesTarget(project);
            if (copyTarget != null && copyTarget.exists()) {
                FileObject copySourceFo = ProjectPropertiesSupport.getSourcesDirectory(project);
                File copySource = FileUtil.toFile(copySourceFo);
                if (copySource != null && copySource.exists()) {
                    copySupportPair = Pair.of(copyTarget.getAbsolutePath(), copySource.getAbsolutePath());
                }
            }
        }
        return copySupportPair;
    }

    private static boolean getBoolean(PhpProject project, String property, boolean defaultValue) {
        String boolValue = project.getEvaluator().getProperty(property);
        if (StringUtils.hasText(boolValue)) {
            return Boolean.parseBoolean(boolValue);
        }
        return defaultValue;
    }

    private static String getString(PhpProject project, String property, String defaultValue) {
        String stringValue = project.getEvaluator().getProperty(property);
        if (stringValue == null) {
            return defaultValue;
        }
        return stringValue;
    }

    private static int getInt(PhpProject project, String property, int defaultValue) {
        String stringValue = project.getEvaluator().getProperty(property);
        if (stringValue != null) {
            try {
                return Integer.valueOf(stringValue);
            } catch (NumberFormatException exc) {
                // ignored
            }
        }
        return defaultValue;
    }

    private static File getFile(PhpProject project, String property) {
        String file = project.getEvaluator().getProperty(property);
        if (!StringUtils.hasText(file)) {
            return null;
        }
        return project.getHelper().resolveFile(file);
    }

    @NbBundle.Messages("ProjectPropertiesSupport.project.metadata.saving=Saving project metadata...")
    private static void saveTestSources(final PhpProject project, final String propertyName, final File testDir) {
        ProgressUtils.showProgressDialogAndRun(new Runnable() {
            @Override
            public void run() {
                // XXX reference helper
                // relativize text path
                File projectDirectory = FileUtil.toFile(project.getProjectDirectory());
                String testPath = PropertyUtils.relativizeFile(projectDirectory, testDir);
                if (testPath == null) {
                    // path cannot be relativized => use absolute path (any VCS can be hardly use, of course)
                    testPath = testDir.getAbsolutePath();
                }
                PhpProjectProperties.save(project, Collections.singletonMap(propertyName, testPath), Collections.<String, String>emptyMap());
            }
        }, Bundle.ProjectPropertiesSupport_project_metadata_saving());
    }

}