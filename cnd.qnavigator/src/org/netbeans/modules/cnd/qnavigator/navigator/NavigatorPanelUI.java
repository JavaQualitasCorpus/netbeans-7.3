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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.qnavigator.navigator;

import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Alexander Simon
 */
public class NavigatorPanelUI extends JPanel implements ExplorerManager.Provider, PropertyChangeListener {
    private NavigatorContent content = new NavigatorContent();
    private BeanTreeView navigatorPane;
    private ExplorerManager explorerManager = new ExplorerManager();
    private final InstanceContent selectedNodes = new InstanceContent();
    private final Lookup lookup = new AbstractLookup(selectedNodes);
    private boolean isBusy = false;
    
    /** Creates new form NavigatorPanel */
    public NavigatorPanelUI() {
        initComponents();
        explorerManager.addPropertyChangeListener(this);
        
        navigatorPane = new BeanTreeView();
        navigatorPane.setRootVisible(false);
        navigatorPane.setDropTarget(false);
        navigatorPane.setDragSource(false);
        add(navigatorPane, java.awt.BorderLayout.CENTER);
        explorerManager.setRootContext(content.getRoot());
        expandAll();
    }

    private void expandAll() {
        NavigatorModel model = content.getModel();
        if (model != null) {
            if (model.getFilter().isExpandAll()) {
                navigatorPane.expandAll();
            }
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());

    }// </editor-fold>//GEN-END:initComponents
    
    void setBusyState(boolean busy){
        Cursor cursor;
        if (isBusy) {
            cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        } else {
            cursor = Cursor.getDefaultCursor();
        }
        navigatorPane.setCursor(cursor);
    }
    
    void newContentReady(){
        explorerManager.setRootContext(content.getRoot());
        if (SwingUtilities.isEventDispatchThread()){
            expandAll();
        } else {
            SwingUtilities.invokeLater(new Runnable(){
                @Override
                public void run() {
                    expandAll();
                }
            });
        }
    }
    
    NavigatorContent getContent() {
        return content;
    }
    
    void selectNode(Node node){
        try {
            explorerManager.setSelectedNodes(new Node[] {node});
        } catch (IllegalArgumentException ex) {
            // FIXUP me
            //ex.printStackTrace();
        } catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    @Override
    public ExplorerManager getExplorerManager() {
        return explorerManager;
    }

    /** Overridden to pass focus directly to main content, which in
     * turn assures that some element is always selected
     */ 
    @Override
    public boolean requestFocusInWindow () {
        boolean result = super.requestFocusInWindow();
        navigatorPane.requestFocusInWindow();
        return result;
    }

    /*package*/final Lookup getLookup() {
        return lookup;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
            for (Node n : (Node[]) evt.getOldValue()) {
                selectedNodes.remove(n);
            }
            for (Node n : (Node[]) evt.getNewValue()) {
                selectedNodes.add(n);
            }
        }
    }
}
