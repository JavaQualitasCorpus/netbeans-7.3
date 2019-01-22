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

/*
 * TestRestTargetPanel.java
 *
 * Created on 06.05.2011, 14:58:15
 */

package org.netbeans.modules.websvc.rest.support;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.web.api.webmodule.WebProjectConstants;
import org.netbeans.modules.websvc.rest.client.RESTExplorerPanel;
import org.netbeans.modules.websvc.rest.client.RESTResourcesView;
import org.netbeans.modules.websvc.rest.client.RESTExplorerPanel.ProjectNodeFactory;
import org.netbeans.modules.websvc.rest.client.RESTResourcesPanel;
import org.netbeans.modules.websvc.rest.spi.RestSupport;
import org.netbeans.modules.websvc.rest.wizard.ClientStubsSetupPanelVisual;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;


/**
 *
 * @author den
 */
public class TestRestTargetPanel extends javax.swing.JPanel {
    
    private static final String TEST_CLIENT_PROJECT ="test.client.project"; // NOI18N
    
    private static final ProjectNodeFactory WEB_PROJECT_FACTORY = new WebProjectFactory();

    public TestRestTargetPanel(Project project) {
        myProject = project;
        initComponents();
        myNotice.setText( NbBundle.getMessage(TestRestTargetPanel.class, 
                "TXT_Notice"));
        myLocal.setSelected(true);
        myBrowse.setEnabled( false );
        ActionListener listener = new ActionListener() {
            
            @Override
            public void actionPerformed( ActionEvent event ) {
                Object source = event.getSource();
                if ( source == myLocal ){
                    myBrowse.setEnabled( false );
                    setTargetProject(myProject, false);
                    storeSelectedProject();
                }
                else if ( source == myRemote ){
                    myBrowse.setEnabled( true );
                    setTargetProject(null, false);
                    storeSelectedProject();
                }
            }
        };
        myLocal.addActionListener(listener); 
        myRemote.addActionListener(listener);
        myBrowse.addActionListener( new ActionListener() {
            
            @Override
            public void actionPerformed( ActionEvent e ) {
                RESTExplorerPanel explorerPanel = new RESTExplorerPanel(
                        WEB_PROJECT_FACTORY);
                DialogDescriptor desc = new DialogDescriptor(explorerPanel,
                        NbBundle.getMessage(TestRestTargetPanel.class,
                                "TTL_Projects"));       //NOI18N
                explorerPanel.setDescriptor(desc); 
                if (DialogDisplayer.getDefault().notify(desc).equals(
                        NotifyDescriptor.OK_OPTION)) 
                {
                    Node node = explorerPanel.getSelectedService();
                    setTargetProject( node.getLookup().lookup(Project.class), true);
                    storeSelectedProject();
                }
            }
        });
        initTargetProject();
    }
    
    Project getProject(){
        return myChosenProject;
    }
    
    boolean isRemote(){
        return isRemote;
    }
    
    void setDescriptor( DialogDescriptor descriptor ) {
        myDescriptor = descriptor;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        myButtonGroup = new javax.swing.ButtonGroup();
        myLocal = new javax.swing.JRadioButton();
        myRemote = new javax.swing.JRadioButton();
        myBrowse = new javax.swing.JButton();
        myTargetLbl = new javax.swing.JLabel();
        myTarget = new javax.swing.JTextField();
        myNoticeLbl = new javax.swing.JLabel();
        myScrollPane = new javax.swing.JScrollPane();
        myNotice = new javax.swing.JEditorPane();

        myButtonGroup.add(myLocal);
        org.openide.awt.Mnemonics.setLocalizedText(myLocal, org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "LBL_Local")); // NOI18N

        myButtonGroup.add(myRemote);
        org.openide.awt.Mnemonics.setLocalizedText(myRemote, org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "LBL_Remote")); // NOI18N
        myRemote.setActionCommand(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "TestRestTargetPanel.myRemote.actionCommand")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(myBrowse, org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "LBL_Browse")); // NOI18N

        myTargetLbl.setLabelFor(myTarget);
        org.openide.awt.Mnemonics.setLocalizedText(myTargetLbl, org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "LBL_Target")); // NOI18N

        myTarget.setEditable(false);

        myNoticeLbl.setLabelFor(myNotice);
        org.openide.awt.Mnemonics.setLocalizedText(myNoticeLbl, org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "LBL_Notion")); // NOI18N

        myNotice.setContentType("text/html");
        myNotice.setEditable(false);
        myScrollPane.setViewportView(myNotice);
        myNotice.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSN_NoticeDescr")); // NOI18N
        myNotice.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSD_NoticeDescr")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(myScrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                    .addComponent(myLocal)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(myRemote)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(myBrowse))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(myTargetLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(myTarget, javax.swing.GroupLayout.DEFAULT_SIZE, 476, Short.MAX_VALUE))
                    .addComponent(myNoticeLbl))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(myLocal)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(myRemote)
                    .addComponent(myBrowse))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(myTargetLbl)
                    .addComponent(myTarget, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(myNoticeLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(myScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                .addContainerGap())
        );

        myLocal.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSN_Local")); // NOI18N
        myLocal.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSD_Local")); // NOI18N
        myRemote.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSN_Remote")); // NOI18N
        myRemote.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSD_Remote")); // NOI18N
        myBrowse.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSN_Browse")); // NOI18N
        myBrowse.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSD_Browse")); // NOI18N
        myTargetLbl.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSN_Target")); // NOI18N
        myTargetLbl.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSD_Target")); // NOI18N
        myTarget.getAccessibleContext().setAccessibleName(myTargetLbl.getAccessibleContext().getAccessibleName());
        myTarget.getAccessibleContext().setAccessibleDescription(myTargetLbl.getAccessibleContext().getAccessibleDescription());
        myNoticeLbl.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSN_Notice")); // NOI18N
        myNoticeLbl.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(TestRestTargetPanel.class, "ACSD_Notice")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private static class WebProjectFactory implements ProjectNodeFactory {

        /* (non-Javadoc)
         * @see org.netbeans.modules.websvc.rest.client.RESTExplorerPanel.ProjectNodeFactory#createNode(org.netbeans.api.project.Project)
         */
        @Override
        public Node createNode( Project project ) {
            LogicalViewProvider logicalProvider = (LogicalViewProvider)project.
                getLookup().lookup(LogicalViewProvider.class);
            RestSupport support = project.getLookup().lookup(RestSupport.class);
            SourceGroup[] sourceGroups = ProjectUtils.getSources(project).
                getSourceGroups(WebProjectConstants.TYPE_DOC_ROOT);
            if (logicalProvider!=null && support != null && sourceGroups != null &&
                    sourceGroups.length >0 ) 
            {
                Node rootNode = logicalProvider.createLogicalView();
                return new FilterNode( rootNode , Children.LEAF );
            }
            return null;
        }

        /* (non-Javadoc)
         * @see org.netbeans.modules.websvc.rest.client.RESTExplorerPanel.ProjectNodeFactory#canSelect(org.openide.nodes.Node)
         */
        @Override
        public boolean canSelect( Node node ) {
            return true;
        }
        
    }
    
    private void initTargetProject(){
        RestSupport support = myProject.getLookup().lookup(RestSupport.class);
        String clientProject = support.getProjectProperty(TEST_CLIENT_PROJECT);
        if ( clientProject == null ){
            setTargetProject(myProject, false);
        }
        else {
            myLocal.setSelected(false);
            myRemote.setSelected(true);
            myBrowse.setEnabled(true);
            File file = new File( clientProject );
            if ( file.exists() ){
                FileObject fileObject = FileUtil.toFileObject( 
                        FileUtil.normalizeFile(file));
                Project project = FileOwnerQuery.getOwner(fileObject);
                setTargetProject(project, true);
            }
        }
    }
    
    private void storeSelectedProject(){
        RestSupport support = myProject.getLookup().lookup(RestSupport.class);
        if (isRemote) {
            FileObject projectDir = myChosenProject.getProjectDirectory();
            try {
                String path = FileUtil.toFile(projectDir).getCanonicalPath();
                support.setPrivateProjectProperty(TEST_CLIENT_PROJECT, path);
            }
            catch (IOException e) {
                Logger.getLogger(TestRestTargetPanel.class.getCanonicalName())
                        .log(Level.INFO, null, e);
            }
        }
        else {
            support.removeProjectProperties(new String[]{ TEST_CLIENT_PROJECT});
        }
    }
    
    private void setTargetProject(Project project, boolean remote ){
        isRemote = remote;
        myChosenProject = project;
        if ( myDescriptor != null ){
            myDescriptor.setValid( project != null );
        }
        if (project == null){
            myTarget.setText( "" );
            return;
        }
        if ( remote ){
            SourceGroup[] sourceGroups = ProjectUtils.getSources(project).
                getSourceGroups(WebProjectConstants.TYPE_DOC_ROOT);
            SourceGroup sourceGroup = sourceGroups[0];
            FileObject rootFolder = sourceGroup.getRootFolder();
            File file = FileUtil.toFile(rootFolder);
            myTarget.setText( file.getPath());
        }
        else {
            RestSupport support = project.getLookup().lookup(RestSupport.class);
            File file = support.getLocalTargetTestRest();
            if ( file!=null){
                myTarget.setText( file.getPath());
            }
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton myBrowse;
    private javax.swing.ButtonGroup myButtonGroup;
    private javax.swing.JRadioButton myLocal;
    private javax.swing.JEditorPane myNotice;
    private javax.swing.JLabel myNoticeLbl;
    private javax.swing.JRadioButton myRemote;
    private javax.swing.JScrollPane myScrollPane;
    private javax.swing.JTextField myTarget;
    private javax.swing.JLabel myTargetLbl;
    // End of variables declaration//GEN-END:variables
    
    private Project myProject;
    private Project myChosenProject;
    private DialogDescriptor myDescriptor;
    private boolean isRemote;
}