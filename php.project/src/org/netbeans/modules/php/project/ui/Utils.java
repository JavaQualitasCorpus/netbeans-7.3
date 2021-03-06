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

package org.netbeans.modules.php.project.ui;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.MutableComboBoxModel;
import javax.swing.UIManager;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.ProjectProblems;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.project.PhpProject;
import org.netbeans.modules.php.project.PhpVisibilityQuery;
import org.netbeans.modules.php.project.ProjectPropertiesSupport;
import org.netbeans.modules.php.project.api.PhpLanguageProperties.PhpVersion;
import org.netbeans.modules.php.project.ui.options.PhpOptionsPanelController;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Miscellaneous UI utils.
 * @author Tomas Mysik
 */
public final class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    // protocol://[user[:password]@]domain[:port]/rel/path?query#anchor
    public static final String URL_REGEXP = "^https?://([^/?#: ]+(:[^/?#: ]+)?@)?[^/?#: ]+(:\\d+)?(/[^?# ]*(\\?[^#]*)?(#\\w*)?)?$"; // NOI18N
    public static final URL PLACEHOLDER_BADGE = Utils.class.getResource("/org/netbeans/modules/php/project/ui/resources/placeholder-badge.png"); // NOI18N

    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEXP);
    private static final char[] INVALID_FILENAME_CHARS = new char[] {'/', '\\', '|', ':', '*', '?', '"', '<', '>'}; // NOI18N

    private Utils() {
    }

    @NbBundle.Messages({
        "# {0} - project name",
        "Utils.metadata.corrupted=<html><b>Project {0} is corrupted.</b><br><br>Do you want to open Project Problems dialog?"
    })
    public static void warnInvalidSourcesDirectory(PhpProject project) {
        String name = project.getName();
        NotifyDescriptor descriptor = new NotifyDescriptor.Confirmation(
                Bundle.Utils_metadata_corrupted(name),
                name,
                NotifyDescriptor.YES_NO_OPTION,
                NotifyDescriptor.WARNING_MESSAGE);
        if (DialogDisplayer.getDefault().notify(descriptor) == NotifyDescriptor.YES_OPTION) {
            ProjectProblems.showCustomizer(project);
        }
    }

    // XXX use everywhere
    @NonNull
    public static Color getErrorForeground() {
        Color result = UIManager.getDefaults().getColor("nb.errorForeground");  //NOI18N
        if (result == null) {
            result = Color.RED;
        }
        return getSafeColor(result.getRed(), result.getGreen(), result.getBlue());
    }

    public static Color getSafeColor(int red, int green, int blue) {
        red = Math.max(red, 0);
        red = Math.min(red, 255);
        green = Math.max(green, 0);
        green = Math.min(green, 255);
        blue = Math.max(blue, 0);
        blue = Math.min(blue, 255);
        return new Color(red, green, blue);
    }

    public static Color getHintBackground() {
        Color panelBackground = UIManager.getColor("Panel.background"); // NOI18N
        return getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10, panelBackground.getBlue() - 10);
    }

    public static Image getIncludePathIcon(boolean opened) {
        Image badge = ImageUtilities.loadImage("org/netbeans/modules/php/project/ui/resources/libraries-badge.png", false); // NOI18N
        return ImageUtilities.mergeImages(UiUtils.getTreeFolderIcon(opened), badge, 8, 8);
    }

    // XXX move it (with its test) to RunConfigWebValidator
    /**
     * Return <code>true</code> if the URL is valid, <code>false</code> otherwise (as well as for <code>null</code>).
     * @param url URL, can be <code>null</code>.
     * @return <code>true</code> if the URL is valid, <code>false</code> otherwise.
     */
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static boolean isValidUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            new URL(url);
        } catch (MalformedURLException ex) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * @return the selected file or <code>null</code>.
     */
    public static File browseFileAction(String dirKey, String title) {
        return browseAction(dirKey, title, true, null);
    }

    /**
     * @return the selected file or <code>null</code>.
     */
    public static File browseFileAction(String dirKey, String title, File workDir) {
        return browseAction(dirKey, title, true, workDir);
    }

    /**
     * @return the selected folder or <code>null</code>.
     */
    public static File browseLocationAction(String dirKey, String title) {
        return browseAction(dirKey, title, false, null);
    }

    /**
     * @return the selected folder or <code>null</code>.
     */
    public static File browseLocationAction(String dirKey, String title, File workDir) {
        return browseAction(dirKey, title, false, workDir);
    }

    private static File browseAction(String dirKey, String title, boolean filesOnly, File workDir) {
        FileChooserBuilder builder = new FileChooserBuilder(dirKey)
                .setTitle(title);
        if (workDir != null) {
            builder.setDefaultWorkingDirectory(workDir)
                    .forceUseOfDefaultWorkingDirectory(true);
        }
        if (filesOnly) {
            builder.setFilesOnly(true);
        } else {
            builder.setDirectoriesOnly(true);
        }
        File selectedFile = builder.showOpenDialog();
        if (selectedFile != null) {
            return FileUtil.normalizeFile(selectedFile);
        }
        return null;
    }

    public static void browseLocalServerAction(final JComboBox localServerComboBox,
            final MutableComboBoxModel localServerComboBoxModel, String newSubfolderName, String title, String dirKey) {
        File preselected = null;
        LocalServer ls = (LocalServer) localServerComboBox.getSelectedItem();
        if (ls.getDocumentRoot() != null && ls.getDocumentRoot().length() > 0) {
            preselected = new File(ls.getDocumentRoot());
        }
        File newLocation = new FileChooserBuilder(dirKey)
                .setTitle(title)
                .setDirectoriesOnly(true)
                .setDefaultWorkingDirectory(preselected)
                .showOpenDialog();
        if (newLocation == null) {
            return;
        }

        File file;
        if (newSubfolderName == null) {
            file = newLocation;
        } else {
            file = new File(newLocation, newSubfolderName);
        }
        String projectLocation = file.getAbsolutePath();
        for (int i = 0; i < localServerComboBoxModel.getSize(); i++) {
            LocalServer element = (LocalServer) localServerComboBoxModel.getElementAt(i);
            if (projectLocation.equals(element.getSrcRoot())) {
                localServerComboBox.setSelectedIndex(i);
                break;
            }
        }
        LocalServer localServer = new LocalServer(newLocation.getAbsolutePath(), projectLocation);
        localServerComboBoxModel.addElement(localServer);
        localServerComboBox.setSelectedItem(localServer);
    }

    // XXX
    public static File browseTestSources(JTextField textField, PhpProject phpProject) {
        File selectedFile = new FileChooserBuilder(LastUsedFolders.TEST_DIR)
                .setTitle(NbBundle.getMessage(Utils.class, "LBL_SelectUnitTestFolder", ProjectUtils.getInformation(phpProject).getDisplayName()))
                .setDirectoriesOnly(true)
                .setDefaultWorkingDirectory(FileUtil.toFile(phpProject.getProjectDirectory()))
                .forceUseOfDefaultWorkingDirectory(true)
                .showOpenDialog();
        if (selectedFile != null) {
            selectedFile = FileUtil.normalizeFile(selectedFile);
            if (textField != null) {
                textField.setText(selectedFile.getAbsolutePath());
            }
        }
        return selectedFile;
    }

    public static String validateTestSources(PhpProject project, String testDirPath) {
        if (!StringUtils.hasText(testDirPath)) {
            return NbBundle.getMessage(Utils.class, "MSG_FolderEmpty");
        }

        File testSourcesFile = new File(testDirPath);
        if (!testSourcesFile.isAbsolute()) {
            return NbBundle.getMessage(Utils.class, "MSG_TestNotAbsolute");
        } else if (!testSourcesFile.isDirectory()) {
            return NbBundle.getMessage(Utils.class, "MSG_TestNotDirectory");
        }
        FileObject nbproject = project.getProjectDirectory().getFileObject("nbproject"); // NOI18N
        FileObject testSourcesFo = FileUtil.toFileObject(testSourcesFile);
        if (testSourcesFile.equals(FileUtil.toFile(ProjectPropertiesSupport.getSourcesDirectory(project)))) {
            return NbBundle.getMessage(Utils.class, "MSG_TestEqualsSources");
        } else if (FileUtil.isParentOf(nbproject, testSourcesFo)
                || nbproject.equals(testSourcesFo)) {
            return NbBundle.getMessage(Utils.class, "MSG_TestUnderneathNBMetadata");
        } else if (!FileUtils.isDirectoryWritable(testSourcesFile)) {
            return NbBundle.getMessage(Utils.class, "MSG_TestNotWritable");
        }
        return null;
    }

    public static String warnTestSources(PhpProject project, String testDirPath) {
        File testSourcesFile = new File(testDirPath);
        FileObject testSourcesFo = FileUtil.toFileObject(testSourcesFile);
        if (!FileUtil.isParentOf(project.getProjectDirectory(), testSourcesFo)) {
            return NbBundle.getMessage(Utils.class, "MSG_TestNotUnderneathProjectFolder");
        }
        return null;
    }

    public static File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            // ignored
        }
        return null;
    }

    /**
     * Check whether the provided String is valid file name. An empty String is considered to be invalid.
     * @param fileName file name.
     * @return <code>true</true> if the provided String is valid file name.
     */
    public static boolean isValidFileName(String fileName) {
        assert fileName != null;
        if (fileName.trim().length() == 0) {
            return false;
        }
        for (char ch : INVALID_FILENAME_CHARS) {
            if (fileName.indexOf(ch) != -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether the provided File has a valid file name. Only the non-existing file names in the file paths are checked.
     * It means that if you pass existing directory, no check is done.
     * <p>
     * For example for <em>C:\Documents And Settings\ExistingDir\NonExistingDir\NonExistingDir2\Newdir</em> the last free file names
     * are checked.
     * <p>
     * File is not {@link FileUtil#normalizeFile(java.io.File) normalized}, caller should do it if needed.
     * @param file File to check.
     * @return <code>true</code> if the provided File has valid file name.
     * @see #isValidFileName(java.lang.String)
     */
    public static boolean isValidFileName(File file) {
        assert file != null;
        File tmp = file;
        while (tmp != null && !tmp.exists()) {
            // #132520
            if (tmp.isAbsolute() && tmp.getParentFile() == null) {
                return true;
            } else if (!isValidFileName(tmp.getName())) {
                return false;
            }
            tmp = tmp.getParentFile();
        }
        return true;
    }

    /**
     * Validate the path and get the error message or <code>null</code> if it's all right.
     * @param projectPath the path to validate.
     * @param type the type for error messages, currently "Project", "Sources" and "Folder".
     *             Add other to Bundle.properties file if more types are needed.
     * @param allowNonEmpty <code>true</code> if the folder can exist and can be non empty.
     * @param allowInRoot  <code>true</code> if the folder can exist and can be a root directory "/"
     *                     (this parameter is taken into account only for *NIX OS).
     * @return localized error message in case of error, <code>null</code> otherwise.
     * @see #validateProjectDirectory(java.io.File, java.lang.String, boolean, boolean)
     */
    public static String validateProjectDirectory(String projectPath, String type, boolean allowNonEmpty,
            boolean allowInRoot) {
        return validateProjectDirectory(new File(projectPath), type, allowNonEmpty, allowInRoot);
    }

    /**
     * Validate the file and get the error message or <code>null</code> if it's all right.
     * @param project the file to validate.
     * @param type the type for error messages, currently "Project", "Sources" and "Folder".
     *             Add other to Bundle.properties file if more types are needed.
     * @param allowNonEmpty <code>true</code> if the folder can exist and can be non empty.
     * @param allowInRoot  <code>true</code> if the folder can exist and can be a root directory "/"
     *                     (this parameter is taken into account only for *NIX OS).
     * @return localized error message in case of error, <code>null</code> otherwise.
     */
    public static String validateProjectDirectory(File project, String type, boolean allowNonEmpty,
            boolean allowInRoot) {
        assert project != null;
        assert type != null;

        // #131753
        if (!project.isAbsolute()) {
            return NbBundle.getMessage(Utils.class, "MSG_" + type + "NotAbsolute");
        }

        // not allow to create project on unix root folder, see #82339
        if (!allowInRoot && Utilities.isUnix()) {
            File cfl = Utils.getCanonicalFile(project);
            if (cfl != null && (cfl.getParentFile() == null || cfl.getParentFile().getParent() == null)) {
                return NbBundle.getMessage(Utils.class, "MSG_" + type + "InRootNotSupported");
            }
        }

        final File destFolder = project.getAbsoluteFile();
        if (Utils.getCanonicalFile(destFolder) == null) {
            return NbBundle.getMessage(Utils.class, "MSG_Illegal" + type + "Location");
        }

        File projLoc = FileUtil.normalizeFile(destFolder);
        while (projLoc != null && !projLoc.exists()) {
            projLoc = projLoc.getParentFile();
        }
        if (projLoc == null || !FileUtils.isDirectoryWritable(projLoc)) {
            return NbBundle.getMessage(Utils.class, "MSG_" + type + "FolderReadOnly");
        }

        if (FileUtil.toFileObject(projLoc) == null) {
            return NbBundle.getMessage(Utils.class, "MSG_Illegal" + type + "Location");
        }

        if (!allowNonEmpty) {
            File[] kids = destFolder.listFiles();
            if (destFolder.exists() && kids != null && kids.length > 0) {
                // Folder exists and is not empty
                return NbBundle.getMessage(Utils.class, "MSG_" + type + "FolderExists");
            }
        }
        return null;
    }

    /**
     * Validate that the project sources directory and directory for copying files are "independent". It means
     * that the sources isn't underneath the target directory and vice versa. Both paths have to be normalized.
     * @param sources project sources.
     * @param copyTarget directory for copying files.
     * @return <code>true</code> if the directories are "independent".
     * @see #subdirectories(java.lang.String, java.lang.String)
     */
    public static String validateSourcesAndCopyTarget(String sources, String copyTarget) {
        if (subdirectories(sources, copyTarget)) {
            return NbBundle.getMessage(Utils.class, "MSG_SourcesEqualCopyTarget");
        }
        return null;
    }

    /**
     * Check whether the <em>dir1</em> is underneath the <em>dir2</em> and vice versa. Both paths have to be normalized.
     * @param dir1 a directory.
     * @param dir2 a directory.
     * @return <code>true</code> if the directories are subdirectories.
     */
    public static boolean subdirectories(String dir1, String dir2) {
        assert dir1 != null;
        assert dir2 != null;
        // handle "/myDir" and "/myDirectory"
        if (!dir1.endsWith(File.separator)) {
            dir1 = dir1 + File.separator;
        }
        if (!dir2.endsWith(File.separator)) {
            dir2 = dir2 + File.separator;
        }
        return dir1.startsWith(dir2) || dir2.startsWith(dir1);
    }

    /**
     * Validate that the text contains only ASCII characters. If not, return an error message.
     * @param text the text to validate, can be <code>null</code>.
     * @param propertyName property name of the given text, e.g. "Project folder name".
     * @return an error message in case that the text contains non-ASCII characters, <code>null</code> otherwise.
     * @see #isAsciiPrintable(char)
     */
    public static String validateAsciiText(String text, String propertyName) {
        assert propertyName != null;
        if (text == null) {
            return null;
        }
        for (int i = 0; i < text.length(); ++i) {
            if (!isAsciiPrintable(text.charAt(i))) {
                return NbBundle.getMessage(Utils.class, "MSG_NonAsciiCharacterFound", propertyName);
            }
        }
        return null;
    }

    // from commons-lang
    /**
     * <p>Checks whether the character is ASCII 7 bit printable.</p>
     *
     * <pre>
     *   Utils.isAsciiPrintable('a')  = true
     *   Utils.isAsciiPrintable('A')  = true
     *   Utils.isAsciiPrintable('3')  = true
     *   Utils.isAsciiPrintable('-')  = true
     *   Utils.isAsciiPrintable('\n') = false
     *   Utils.isAsciiPrintable('&copy;') = false
     * </pre>
     *
     * @param ch the character to check.
     * @return <code>true</code> if between 32 and 126 inclusive.
     */
    public static boolean isAsciiPrintable(char ch) {
        return ch >= 32 && ch < 127;
    }

    /**
     * Get a platform independent path (uses '/' as a file separator, similarly to file objects).
     * @param path path to unify
     * @return a platform independent path
     */
    public static String unifyPath(String path) {
        assert path != null;
        return path.replace(File.separatorChar, '/'); // NOI18N
    }

    /**
     * Browse for a file from the given directory and update the content of the text field.
     * @param folder folder to browse files from.
     * @param textField textfield to update.
     */
    public static void browseFolderFile(PhpVisibilityQuery phpVisibilityQuery, FileObject folder, JTextField textField) throws FileNotFoundException {
        String selected = browseFolderFile(phpVisibilityQuery, folder, textField.getText());
        if (selected != null) {
            textField.setText(selected);
        }
    }

    /**
     * @see #browseFolderFile(org.openide.filesystems.FileObject, javax.swing.JTextField)
     */
    public static void browseFolderFile(PhpVisibilityQuery phpVisibilityQuery, File folder, JTextField textField) throws FileNotFoundException {
        browseFolderFile(phpVisibilityQuery, FileUtil.toFileObject(folder), textField);
    }

    /**
     * Browse for a file from the given directory and return the relative path or <code>null</code> if nothing selected.
     * @param folder folder to browse files from.
     * @param preselected the preselected value, can be null.
     * @return the relative path to folder or <code>null</code> if nothing selected.
     */
    public static String browseFolderFile(PhpVisibilityQuery phpVisibilityQuery, FileObject folder, String preselected) throws FileNotFoundException {
        if (folder == null) {
            throw new FileNotFoundException();
        }
        FileObject selected = BrowseFolders.showDialog(phpVisibilityQuery, new FileObject[] {folder}, DataObject.class, securePreselected(preselected, true));
        if (selected != null) {
            return PropertyUtils.relativizeFile(FileUtil.toFile(folder), FileUtil.toFile(selected));
        }
        return null;
    }

    /**
     * Browse for a file from sources of a project and update the content of the text field.
     * @param project project to get sources from.
     * @param textField textfield to update.
     */
    public static void browseSourceFile(PhpProject project, JTextField textField) {
        String selected = browseSource(project, textField.getText(), false);
        if (selected != null) {
            textField.setText(selected);
        }
    }

    /**
     * Browse for a file from sources of a project and return the relative path or <code>null</code> if nothing selected.
     * @param project project to get sources from.
     * @param preselected the preselected value, can be null.
     * @return the relative path to folder or <code>null</code> if nothing selected.
     */
    public static String browseSourceFile(PhpProject project, String preselected) {
        return browseSource(project, preselected, false);
    }

    /**
     * Browse for a directory from sources of a project and update the content of the text field.
     * @param project project to get sources from.
     * @param textField textfield to update.
     */
    public static void browseSourceFolder(PhpProject project, JTextField textField) {
        String selected = browseSource(project, textField.getText(), true);
        if (selected != null) {
            textField.setText(selected);
        }
    }

    /**
     * Browse for a directory from sources of a project and return the relative path or <code>null</code> if nothing selected.
     * @param project project to get sources from.
     * @param preselected the preselected value, can be null.
     * @return the relative path to folder or <code>null</code> if nothing selected.
     */
    public static String browseSourceFolder(PhpProject project, String preselected) {
        return browseSource(project, preselected, true);
    }

    private static String browseSource(PhpProject project, String preselected, boolean selectDirectory) {
        FileObject rootFolder = ProjectPropertiesSupport.getSourcesDirectory(project);
        FileObject selected = BrowseFolders.showDialog(PhpVisibilityQuery.forProject(project), new FileObject[] {rootFolder},
                selectDirectory ? DataFolder.class : DataObject.class, securePreselected(preselected, !selectDirectory));
        if (selected != null) {
            return PropertyUtils.relativizeFile(FileUtil.toFile(rootFolder), FileUtil.toFile(selected));
        }
        return null;
    }

    private static String securePreselected(String preselected, boolean removeExtension) {
        if (preselected == null) {
            return null;
        }
        String secure = null;
        if (preselected.length() > 0) {
            secure = unifyPath(preselected);
            if (removeExtension) {
                // e.g. searching in nodes => no file extension can be there
                int idx = secure.lastIndexOf('.'); // NOI18N
                if (idx != -1) {
                    secure = secure.substring(0, idx);
                }
            }
        }
        return secure;
    }

    /**
     * Display Options dialog with PHP > General panel preselected.
     */
    public static void showGeneralOptionsPanel() {
        UiUtils.showOptions(PhpOptionsPanelController.ID);
    }

    public static class PhpVersionComboBoxModel extends DefaultComboBoxModel {
        private static final long serialVersionUID = -203741202171115527L;

        public PhpVersionComboBoxModel() {
            this(null);
        }

        public PhpVersionComboBoxModel(PhpVersion preselected) {
            super(PhpVersion.values());

            if (preselected != null) {
                setSelectedItem(preselected);
            } else {
                setSelectedItem(PhpVersion.getDefault());
            }
        }
    }
}
