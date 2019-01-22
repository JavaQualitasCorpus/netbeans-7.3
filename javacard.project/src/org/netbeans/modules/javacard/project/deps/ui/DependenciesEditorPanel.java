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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.javacard.project.deps.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import org.netbeans.api.validation.adapters.DialogBuilder;
import org.netbeans.modules.javacard.common.GuiUtils;
import org.netbeans.modules.javacard.project.JCProject;
import org.netbeans.modules.javacard.project.deps.ArtifactKind;
import org.netbeans.modules.javacard.project.deps.ResolvedDependencies;
import org.netbeans.modules.javacard.project.deps.ResolvedDependency;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.ListView;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 * Panel that appears in the project customizer dialog for handling dependencies.
 *
 * @author Tim Boudreau
 */
class DependenciesEditorPanel extends javax.swing.JPanel implements ExplorerManager.Provider, ActionListener, PropertyChangeListener {

    private final ExplorerManager mgr = new ExplorerManager();
    private ResolvedDependencies deps;
    private JCProject project;

    public DependenciesEditorPanel() {
        initComponents();
        GuiUtils.prepareContainer(this);
        mgr.addPropertyChangeListener(this);
        ((ListView) depsList).setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HelpCtx.setHelpIDString(addButton, "org.netbeans.modules.javacard.AddLibrary"); //NOI18N
        HelpCtx.setHelpIDString(remButton, "org.netbeans.modules.javacard.RemoveLibrary"); //NOI18N
        HelpCtx.setHelpIDString(editButton, "org.netbeans.modules.javacard.ChangeLibraryDeploymentStrategy"); //NOI18N
        HelpCtx.setHelpIDString(this, "org.netbeans.modules.javacard.DependenciesPanel"); //NOI18N
    }

    public DependenciesEditorPanel(JCProject project, ResolvedDependencies deps) {
        this();
        this.deps = deps;
        this.project = project;
        Parameters.notNull("deps", deps); //NOI18N
        mgr.setRootContext(new DependenciesNode(project, deps));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        depsList = new ListView();
        libsLabel = new javax.swing.JLabel();
        addButton = new javax.swing.JButton();
        remButton = new javax.swing.JButton();
        moveUpButton = new javax.swing.JButton();
        moveDownButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();
        pathLabel = new javax.swing.JLabel();
        depStrategyLabel = new javax.swing.JLabel();
        depStrategyField = new javax.swing.JTextField();
        pathField = new javax.swing.JTextField();

        depsList.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("controlShadow")));

        libsLabel.setLabelFor(depsList);
        libsLabel.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.libsLabel.text")); // NOI18N

        addButton.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.addButton.text")); // NOI18N
        addButton.addActionListener(this);

        remButton.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.remButton.text")); // NOI18N
        remButton.setEnabled(false);
        remButton.addActionListener(this);

        moveUpButton.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.moveUpButton.text")); // NOI18N
        moveUpButton.setEnabled(false);
        moveUpButton.addActionListener(this);

        moveDownButton.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.moveDownButton.text")); // NOI18N
        moveDownButton.setEnabled(false);
        moveDownButton.addActionListener(this);

        editButton.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.editButton.text")); // NOI18N
        editButton.setEnabled(false);
        editButton.addActionListener(this);

        pathLabel.setLabelFor(pathField);
        pathLabel.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.pathLabel.text")); // NOI18N

        depStrategyLabel.setLabelFor(depStrategyField);
        depStrategyLabel.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.depStrategyLabel.text")); // NOI18N

        depStrategyField.setEditable(false);
        depStrategyField.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.depStrategyField.text")); // NOI18N

        pathField.setEditable(false);
        pathField.setText(org.openide.util.NbBundle.getMessage(DependenciesEditorPanel.class, "DependenciesEditorPanel.pathField.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(libsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(depsList, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(depStrategyLabel)
                                    .addComponent(pathLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(pathField, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE)
                                    .addComponent(depStrategyField, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(addButton)
                            .addComponent(remButton)
                            .addComponent(editButton)
                            .addComponent(moveUpButton)
                            .addComponent(moveDownButton))))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addButton, editButton, moveDownButton, moveUpButton, remButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(11, 11, 11)
                .addComponent(libsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(remButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editButton)
                        .addGap(18, 18, 18)
                        .addComponent(moveUpButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(moveDownButton))
                    .addComponent(depsList, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pathLabel)
                    .addComponent(pathField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(depStrategyLabel)
                    .addComponent(depStrategyField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }

    // Code for dispatching events from components to event handlers.

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        if (evt.getSource() == addButton) {
            DependenciesEditorPanel.this.onAdd(evt);
        }
        else if (evt.getSource() == remButton) {
            DependenciesEditorPanel.this.onRemove(evt);
        }
        else if (evt.getSource() == moveUpButton) {
            DependenciesEditorPanel.this.onMoveUp(evt);
        }
        else if (evt.getSource() == moveDownButton) {
            DependenciesEditorPanel.this.onMoveDown(evt);
        }
        else if (evt.getSource() == editButton) {
            DependenciesEditorPanel.this.onEdit(evt);
        }
    }// </editor-fold>//GEN-END:initComponents

    private void onAdd(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onAdd
        postReselect(AddDependencyWizardIterator.show(deps, project));
    }//GEN-LAST:event_onAdd

    private void onRemove(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onRemove
        ResolvedDependency rd = getSelection();
        deps.remove(rd);
    }//GEN-LAST:event_onRemove

    private void onEdit(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onEdit
        ResolvedDependency dep = getSelection();
        assert dep != null;
        new DialogBuilder(DependenciesEditorPanel.class).setTitle(NbBundle.getMessage(DependenciesEditorPanel.class, "TTL_EDIT_LIBRARY")). //NOI18N
                setButtonSet(DialogBuilder.ButtonSet.CLOSE).
                setContent(new EditOneDependencyPanel(dep)).
                showDialog();
        onSelectionChanged();
    }//GEN-LAST:event_onEdit

    private void onMoveUp(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onMoveUp
        ResolvedDependency rd = getSelection();
        deps.moveUp(rd);
        postReselect(rd);
    }//GEN-LAST:event_onMoveUp

    private void onMoveDown(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onMoveDown
        ResolvedDependency rd = getSelection();
        deps.moveDown(rd);
        postReselect(rd);
    }//GEN-LAST:event_onMoveDown

    private void postReselect(final ResolvedDependency rd) {
        if (rd == null) {
            return;
        }
        //XXX would be nicer to do this in a NodeListener on children changed,
        //but this appears not to work
        Timer t = new Timer (200, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (Node n : mgr.getRootContext().getChildren().getNodes(true)) {
                    if (rd.equals(n.getLookup().lookup(ResolvedDependency.class))) {
                        try {
                            mgr.setSelectedNodes(new Node[]{n});
                        } catch (PropertyVetoException ex) {
                            //do nothing
                        }
                        break;
                    }
                }
            }
        });
        t.setRepeats(false);
        t.start();
    }

    private ResolvedDependency getSelection() {
        Node[] n = mgr.getSelectedNodes();
        return n == null || n.length == 0 ? null : n[0].getLookup().lookup(ResolvedDependency.class);
    }

    public ExplorerManager getExplorerManager() {
        return mgr;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JTextField depStrategyField;
    private javax.swing.JLabel depStrategyLabel;
    private javax.swing.JScrollPane depsList;
    private javax.swing.JButton editButton;
    private javax.swing.JLabel libsLabel;
    private javax.swing.JButton moveDownButton;
    private javax.swing.JButton moveUpButton;
    private javax.swing.JTextField pathField;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JButton remButton;
    // End of variables declaration//GEN-END:variables

    public void propertyChange(PropertyChangeEvent evt) {
        if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
            onSelectionChanged();
        }
    }

    private void onSelectionChanged() {
        Node[] n = mgr.getSelectedNodes();
        if (n != null && n.length == 1) {
            ResolvedDependency dep = n[0].getLookup().lookup(ResolvedDependency.class);
            setSelectedDependency(dep);
        } else {
            setSelectedDependency(null);
        }
    }

    private void setSelectedDependency(ResolvedDependency d) {
        editButton.setEnabled(d != null);
        moveUpButton.setEnabled(d != null && deps.canMoveUp(d));
        moveDownButton.setEnabled(d != null && deps.canMoveDown(d));
        remButton.setEnabled(d != null);
        pathField.setEnabled(d != null);
        pathLabel.setEnabled(d != null);
        depStrategyLabel.setEnabled(d != null);
        depStrategyField.setEnabled(d != null);
        pathField.setText(d == null ? "" : d.getPath(ArtifactKind.ORIGIN)); //NOI18N
        depStrategyField.setText(d == null ? "" : d.getDeploymentStrategy().toString()); //NOI18N
    }
}