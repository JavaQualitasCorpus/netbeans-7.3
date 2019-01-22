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

package org.netbeans.modules.java.api.common.project.ui.customizer;

import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ant.AntArtifact;
import org.netbeans.api.project.ant.AntArtifactQuery;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 * Accessory component used in the ProjectChooser for choosing project
 * artifacts.
 * @author Petr Hrebejk
 */
final class AntArtifactChooser extends JPanel implements PropertyChangeListener {
    

    private String[] artifactTypes;
    
    /** Creates new form JarArtifactChooser */
    public AntArtifactChooser( String[] artifactTypes, JFileChooser chooser ) {
        this.artifactTypes = artifactTypes;
        
        initComponents();
        jListArtifacts.setModel( new DefaultListModel() );
        chooser.addPropertyChangeListener( this );
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jLabelName = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jLabelJarFiles = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jListArtifacts = new javax.swing.JList();

        setLayout(new java.awt.GridBagLayout());

        jLabelName.setLabelFor(jTextFieldName);
        org.openide.awt.Mnemonics.setLocalizedText(jLabelName, org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectName_JLabel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 2, 0);
        add(jLabelName, gridBagConstraints);

        jTextFieldName.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 6, 0);
        add(jTextFieldName, gridBagConstraints);
        jTextFieldName.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectName_JLabel")); // NOI18N

        jLabelJarFiles.setLabelFor(jListArtifacts);
        org.openide.awt.Mnemonics.setLocalizedText(jLabelJarFiles, org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectJarFiles_JLabel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 2, 0);
        add(jLabelJarFiles, gridBagConstraints);

        jScrollPane1.setViewportView(jListArtifacts);
        jListArtifacts.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(AntArtifactChooser.class, "LBL_AACH_ProjectJarFiles_JLabel")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 12, 0, 0);
        add(jScrollPane1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents
    
    
    public void propertyChange(PropertyChangeEvent e) {
        if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(e.getPropertyName())) {
            // We have to update the Accessory
            JFileChooser chooser = (JFileChooser) e.getSource();
            File dir = chooser.getSelectedFile(); // may be null (#46744)
            Project project = getProject(dir); // may be null
            populateAccessory(project);
        }
    }
    
    private Project getProject( File projectDir ) {
        
        if (projectDir == null) { // #46744
            return null;
        }
        
        try {            
            File normProjectDir = FileUtil.normalizeFile(projectDir);
            FileObject fo = FileUtil.toFileObject(normProjectDir);
            if (fo != null) {
                return ProjectManager.getDefault().findProject(fo);
            }
        } catch (IOException e) {
            Logger.getLogger("global").log(Level.INFO, null, e);
            // Return null
        }
        
        return null;
    }    
    
    /**
     * Set up GUI fields according to the requested project.
     * @param project a subproject, or null
     */
    private void populateAccessory( Project project ) {
        
        DefaultListModel model = (DefaultListModel)jListArtifacts.getModel();
        model.clear();
        jTextFieldName.setText(project == null ? "" : ProjectUtils.getInformation(project).getDisplayName()); //NOI18N
        
        if ( project != null ) {
            
            List<AntArtifact> artifacts = new ArrayList<AntArtifact>();
            for (int i=0; i<artifactTypes.length; i++) {
                artifacts.addAll (Arrays.asList(AntArtifactQuery.findArtifactsByType(project, artifactTypes[i])));
            }
            
            for(AntArtifact artifact : artifacts) {
                URI uris[] = artifact.getArtifactLocations();
                for( int y = 0; y < uris.length; y++ ) {
                    model.addElement( new AntArtifactItem(artifact, uris[y]));
                }
            }
            jListArtifacts.setSelectionInterval(0, model.size());
        }
        
    }
        
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabelJarFiles;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JList jListArtifacts;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextFieldName;
    // End of variables declaration//GEN-END:variables

    
    /** Shows dialog with the artifact chooser 
     * @return null if canceled selected jars if some jars selected
     */
    static AntArtifactItem[] showDialog( String[] artifactTypes, Project master, Component parent ) {
        
        JFileChooser chooser = ProjectChooser.projectChooser();
        chooser.setDialogTitle( NbBundle.getMessage( AntArtifactChooser.class, "LBL_AACH_Title" ) ); // NOI18N
        chooser.setApproveButtonText( NbBundle.getMessage( AntArtifactChooser.class, "LBL_AACH_SelectProject" ) ); // NOI18N
        chooser.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage (AntArtifactChooser.class,"AD_AACH_SelectProject"));
        AntArtifactChooser accessory = new AntArtifactChooser( artifactTypes, chooser );
        chooser.setAccessory( accessory );
        chooser.setPreferredSize( new Dimension( 650, 380 ) );        
        File defaultFolder = null;
        FileObject defFo = master.getProjectDirectory();
        if (defFo != null) {
            defFo = defFo.getParent();
            if (defFo != null) {
                defaultFolder = FileUtil.toFile(defFo);
            }
        }
        chooser.setCurrentDirectory (getLastUsedArtifactFolder(defaultFolder));

        int option = chooser.showOpenDialog( parent ); // Show the chooser
              
        if ( option == JFileChooser.APPROVE_OPTION ) {

            File dir = chooser.getSelectedFile();
            dir = FileUtil.normalizeFile (dir);
            Project selectedProject = accessory.getProject( dir );

            if ( selectedProject == null ) {
                return null;
            }
            
            if ( selectedProject.getProjectDirectory().equals( master.getProjectDirectory() ) ) {
                DialogDisplayer.getDefault().notify( new NotifyDescriptor.Message( 
                    NbBundle.getMessage( AntArtifactChooser.class, "MSG_AACH_RefToItself" ),
                    NotifyDescriptor.INFORMATION_MESSAGE ) );
                return null;
            }
            
            if ( ProjectUtils.hasSubprojectCycles( master, selectedProject ) ) {
                DialogDisplayer.getDefault().notify( new NotifyDescriptor.Message( 
                    NbBundle.getMessage( AntArtifactChooser.class, "MSG_AACH_Cycles" ),
                    NotifyDescriptor.INFORMATION_MESSAGE ) );
                return null;
            }

            if (AntArtifactQuery.findArtifactsByType(selectedProject, JavaProjectConstants.ARTIFACT_TYPE_JAR).length == 0) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
                        NbBundle.getMessage (AntArtifactChooser.class,"MSG_NO_JAR_OUTPUT")));
                return null;
            }
            
            setLastUsedArtifactFolder (FileUtil.normalizeFile(chooser.getCurrentDirectory()));
            
            Object[] tmp = new Object[accessory.jListArtifacts.getModel().getSize()];
            int count = 0;
            for(int i = 0; i < tmp.length; i++) {
                if (accessory.jListArtifacts.isSelectedIndex(i)) {
                    tmp[count] = accessory.jListArtifacts.getModel().getElementAt(i);
                    count++;
                }
            }
            AntArtifactItem artifactItems[] = new AntArtifactItem[count];
            System.arraycopy(tmp, 0, artifactItems, 0, count);
            return artifactItems;
        }
        else {
            return null; 
        }
                
    }
       
    private static final String LAST_USED_ARTIFACT_FOLDER = "lastUsedArtifactFolder"; //NOI18N

    private static Preferences getPreferences() {
        return NbPreferences.forModule(AntArtifactChooser.class);
    }
    
    private static File getLastUsedArtifactFolder (final File defaultValue) {
        String val = getPreferences().get(LAST_USED_ARTIFACT_FOLDER, null);
        if (val != null) {
            return FileUtil.normalizeFile(new File (val));
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        return FileUtil.normalizeFile(new File (System.getProperty("user.home")));
    }

    private static void setLastUsedArtifactFolder (File folder) {
        assert folder != null : "Folder can not be null";
        String path = folder.getAbsolutePath();
        getPreferences().put(LAST_USED_ARTIFACT_FOLDER, path);        
    }
}