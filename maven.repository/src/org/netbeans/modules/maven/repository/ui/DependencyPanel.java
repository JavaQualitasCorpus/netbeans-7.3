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


package org.netbeans.modules.maven.repository.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.core.spi.multiview.CloseOperationState;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.MultiViewElementCallback;
import org.netbeans.modules.maven.indexer.api.ui.ArtifactViewer;
import org.openide.awt.Actions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 *
 * @author mkleint
 */
public class DependencyPanel extends TopComponent implements MultiViewElement, LookupListener {

    private static final @StaticResource String DEPENDENCY = "org/netbeans/modules/maven/repository/ui/DependencyIcon.png";
    private static final @StaticResource String TRANSITIVE_DEPENDENCY = "org/netbeans/modules/maven/repository/ui/TransitiveDependencyIcon.png";

    private Lookup.Result<DependencyNode> result;
    private JToolBar toolbar;

    DependencyPanel(Lookup lookup) {
        super(lookup);
        initComponents();
        Rend r = new Rend();
        lstTest.setCellRenderer(r);
        lstRuntime.setCellRenderer(r);
        lstCompile.setCellRenderer(r);
        MouseListener ml = new MouseAdapter() {
            @Override
            @SuppressWarnings("unchecked")
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    DependencyNode nd = (DependencyNode)((JList)e.getComponent()).getSelectedValue();
                    if (nd != null) {
                        MavenProject prj = getLookup().lookup(MavenProject.class);
                        if (prj != null) {
                            ArtifactViewer.showArtifactViewer(nd.getArtifact(), prj.getRemoteArtifactRepositories(), ArtifactViewer.HINT_DEPENDENCIES);
                        }
                    }
                }
            }
        };
        lstTest.addMouseListener(ml);
        lstRuntime.addMouseListener(ml);
        lstCompile.addMouseListener(ml);
        if( "Aqua".equals(UIManager.getLookAndFeel().getID()) ) { //NOI18N
            setBackground(UIManager.getColor("NbExplorerView.background")); //NOI18N
            jPanel1.setBackground(UIManager.getColor("NbExplorerView.background")); //NOI18N
        }
    }

    public @Override int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblCompile = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstCompile = new javax.swing.JList();
        lblRuntime = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstRuntime = new javax.swing.JList();
        lblTest = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstTest = new javax.swing.JList();
        lblHint = new javax.swing.JLabel();

        setFocusable(true);
        setLayout(new java.awt.BorderLayout());

        lblCompile.setLabelFor(lstCompile);
        org.openide.awt.Mnemonics.setLocalizedText(lblCompile, org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.lblCompile.text")); // NOI18N

        jScrollPane1.setViewportView(lstCompile);
        lstCompile.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.lstCompile.AccessibleContext.accessibleDescription")); // NOI18N

        lblRuntime.setLabelFor(lstRuntime);
        org.openide.awt.Mnemonics.setLocalizedText(lblRuntime, org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.lblRuntime.text")); // NOI18N

        jScrollPane2.setViewportView(lstRuntime);
        lstRuntime.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.lstRuntime.AccessibleContext.accessibleDescription")); // NOI18N

        lblTest.setLabelFor(lstTest);
        org.openide.awt.Mnemonics.setLocalizedText(lblTest, org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.lblTest.text")); // NOI18N

        jScrollPane3.setViewportView(lstTest);
        lstTest.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.lstTest.AccessibleContext.accessibleDescription")); // NOI18N

        lblHint.setText(org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.lblHint.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(lblHint, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 511, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblCompile)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblRuntime)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 172, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(lblTest)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE))))
                    .addContainerGap()))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(lblCompile)
                        .addComponent(lblRuntime)
                        .addComponent(lblTest))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 234, Short.MAX_VALUE))
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(lblHint)
                    .addContainerGap()))
        );

        add(jPanel1, java.awt.BorderLayout.CENTER);

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.AccessibleContext.accessibleName")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DependencyPanel.class, "DependencyPanel.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblCompile;
    private javax.swing.JLabel lblHint;
    private javax.swing.JLabel lblRuntime;
    private javax.swing.JLabel lblTest;
    private javax.swing.JList lstCompile;
    private javax.swing.JList lstRuntime;
    private javax.swing.JList lstTest;
    // End of variables declaration//GEN-END:variables

    @Override
    public JComponent getVisualRepresentation() {
        return this;
    }

    @Override
    public JComponent getToolbarRepresentation() {
        if (toolbar == null) {
            toolbar = new JToolBar();
            toolbar.setFloatable(false);
            Action[] a = new Action[1];
            Action[] actions = getLookup().lookup(a.getClass());
            Dimension space = new Dimension(3, 0);
            toolbar.addSeparator(space);
            for (Action act : actions) {
                JButton btn = new JButton();
                Actions.connect(btn, act);
                toolbar.add(btn);
                toolbar.addSeparator(space);
            }
        }
        return toolbar;
    }


    @Override
    public void componentOpened() {
        super.componentOpened();
        result = getLookup().lookup(new Lookup.Template<DependencyNode>(DependencyNode.class));
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                populateFields();
            }
        });
        result.addLookupListener(this);
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        result.removeLookupListener(this);
    }

    @Override
    public void componentShowing() {
        super.componentShowing();
    }

    @Override
    public void componentHidden() {
        super.componentHidden();
    }

    @Override
    public void componentActivated() {
        super.componentActivated();
    }

    @Override
    public void componentDeactivated() {
        super.componentDeactivated();
    }

    public @Override void setMultiViewCallback(MultiViewElementCallback callback) {}

    @Override
    public CloseOperationState canCloseElement() {
        return CloseOperationState.STATE_OK;
    }

    private void populateFields() {
        Iterator<? extends DependencyNode> iter = result.allInstances().iterator();
        if (iter.hasNext()) {
            final DependencyNode root = iter.next();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    setDepModel(lstCompile, root, Arrays.asList(new String[]{ Artifact.SCOPE_COMPILE, Artifact.SCOPE_PROVIDED}));
                    setDepModel(lstRuntime, root, Arrays.asList(new String[]{ Artifact.SCOPE_RUNTIME}));
                    setDepModel(lstTest, root, Arrays.asList(new String[]{ Artifact.SCOPE_TEST}));
                }
            });
        } else {

        }
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        populateFields();
    }

    private void setDepModel(JList lst, DependencyNode root, List<String> scopes) {
        DefaultListModel dlm = new DefaultListModel();
        NodeVisitor vis = new NodeVisitor(scopes);
        root.accept(vis);
        for (DependencyNode d : vis.getDirects()) {
            dlm.addElement(d);
        }
        for (DependencyNode d : vis.getTransitives()) {
            dlm.addElement(d);
        }
        lst.setModel(dlm);
        lst.putClientProperty("directs", vis.getDirects());
        lst.putClientProperty("trans", vis.getTransitives());
    }

    private static class Rend extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof DependencyNode) {
                DependencyNode d = (DependencyNode)value;
                JLabel lbl = (JLabel)cmp;
                lbl.setText(d.getArtifact().getArtifactId() + ":" + d.getArtifact().getVersion());
                @SuppressWarnings("unchecked")
                List<DependencyNode> dirs = (List<DependencyNode>)list.getClientProperty("directs");
                if (dirs.contains(d)) {
                    lbl.setIcon(ImageUtilities.image2Icon(ImageUtilities.loadImage(DEPENDENCY, true)));
                } else {
                    lbl.setIcon(ImageUtilities.image2Icon(ImageUtilities.loadImage(TRANSITIVE_DEPENDENCY, true)));
                }
            }
            return cmp;
        }

    }

    private static class NodeVisitor implements DependencyNodeVisitor {
        private List<DependencyNode> directs;
        private List<DependencyNode> trans;
        private List<String> scopes;
        private DependencyNode root;
        private Stack<DependencyNode> path;

        private NodeVisitor(List<String> scopes) {
            this.scopes = scopes;
        }

        @Override
    public boolean visit(DependencyNode node) {
        if (root == null) {
            root = node;
            directs = new ArrayList<DependencyNode>();
            trans = new ArrayList<DependencyNode>();
            path = new Stack<DependencyNode>();
            return true;
        }
        if (node.getState() == DependencyNode.INCLUDED &&
                scopes.contains(node.getArtifact().getScope())) {
            if (path.empty()) {
                directs.add(node);
            } else {
                trans.add(node);
            }
        }
        path.push(node);
        return true;
    }

        @Override
    public boolean endVisit(DependencyNode node) {
        if (root == node) {
            root = null;
            path = null;
            return true;
        }
        path.pop();
        return true;
    }

        private Iterable<DependencyNode> getDirects() {
            return directs;
        }

        private Iterable<DependencyNode> getTransitives() {
            return trans;
        }

    }


}