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

package org.netbeans.modules.debugger.jpda.ui.debugging;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionListener;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.FixedHeightLayoutCache;
import javax.swing.tree.RowMapper;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.api.debugger.jpda.JPDAThreadGroup;
import org.netbeans.modules.debugger.jpda.ui.models.DebuggingTreeModel;
import org.netbeans.spi.viewmodel.TreeExpansionModel;

import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.NodeRenderer;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 *
 * @author Dan
 */
public class DebugTreeView extends BeanTreeView {

    private static final Logger logger = Logger.getLogger(DebugTreeView.class.getName());

    private int thickness = 0;
    private Color highlightColor = new Color(233, 239, 248);
    private Color currentThreadColor = new Color(233, 255, 230);
    
    private JPDAThread focusedThread;
    
    DebugTreeView() {
        super();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Debugging view panel background color = "+javax.swing.UIManager.getDefaults().getColor("Panel.background"));
            logger.fine("Debugging view tree text background color = "+javax.swing.UIManager.getDefaults().getColor("Tree.textBackground"));
            logger.fine("Tree color = "+tree.getBackground()+", tree is opaque = "+tree.isOpaque());
            logger.fine("Tree parent color = "+((JComponent)tree.getParent()).getBackground()+", tree parent is opaque = "+((JComponent)tree.getParent()).isOpaque());
            logger.fine("Tree parent = "+tree.getParent());
        }

        NodeRenderer rend = new DebugTreeNodeRenderer();
        tree.setCellRenderer(rend);

        setBackground(tree.getBackground());
        if (System.getProperty("java.version").startsWith("1.6") &&
            "GTK".equals(UIManager.getLookAndFeel().getID())) {
            // leave the tree as opaque to paint the whole area
        } else {
            tree.setOpaque(false);
            ((JComponent)tree.getParent()).setOpaque(false);
        }
        ((JComponent)tree.getParent()).setBackground(tree.getBackground());
        setWheelScrollingEnabled(false);
    }

    public JTree getTree() {
        return tree;
    }

    void resetSelection() {
        tree.getSelectionModel().clearSelection(); // To flush selection cache
        clearSelectionCache(tree.getSelectionModel().getRowMapper());
        clearDrawingCache(tree);
        tree.repaint(); // To flush SynthTreeUI.drawingCache
    }

    // HACK to clear Swing caches
    private static void clearSelectionCache(RowMapper rm) {
        if (rm instanceof FixedHeightLayoutCache) {
            try {
                Field infoField = rm.getClass().getDeclaredField("info");
                infoField.setAccessible(true);
                Object searchInfo = infoField.get(rm);
                if (searchInfo != null) {
                    Field nodeField = searchInfo.getClass().getDeclaredField("node");
                    nodeField.setAccessible(true);
                    nodeField.set(searchInfo, null);
                }
            } catch (Exception ex) {}
        }
    }

    // HACK http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6258067
    private static void clearDrawingCache(JTree tree) {
        TreeUI tui = tree.getUI();
        if (tui instanceof BasicTreeUI) {
            try {
                Field drawingCacheField = BasicTreeUI.class.getDeclaredField("drawingCache");
                drawingCacheField.setAccessible(true);
                Map table = (Map) drawingCacheField.get(tui);
                table.clear();
            } catch (Exception ex) {}
        }
     }

    public List<TreePath> getVisiblePaths() {
        synchronized(tree) {
            List<TreePath> result = new ArrayList<TreePath>();
            int count = tree.getRowCount();
            for (int x = 0; x < count; x++) {
                TreePath path = tree.getPathForRow(x);
                if (tree.isVisible(path)) {
                    result.add(path);
                }
            }
            return result;
        }
    }

    public Object getJPDAObject(TreePath path) {
        Node node = Visualizer.findNode(path.getLastPathComponent());
        JPDAThread jpdaThread = node.getLookup().lookup(JPDAThread.class);
        if (jpdaThread != null) {
            return jpdaThread;
        }
        JPDAThreadGroup jpdaThreadGroup = node.getLookup().lookup(JPDAThreadGroup.class);
        return jpdaThreadGroup;
    }

    public int getUnitHeight() {
        return thickness;
    }

    public void addTreeExpansionListener(TreeExpansionListener listener) {
        tree.addTreeExpansionListener(listener);
    }
    
    public void removeTreeExpansionListener(TreeExpansionListener listener) {
        tree.removeTreeExpansionListener(listener);
    }

    void setExpansionModel(TreeExpansionModel model) {
        // [TODO] ???
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintStripes(g, this);
    }

    // [TODO] optimize paintStripes() method
    void paintStripes(Graphics g, JComponent comp) {
        List<TreePath> paths = getVisiblePaths(); // [TODO] do not call getVisiblePaths()
        int linesNumber = paths.size();
        Rectangle rect = paths.size() > 0 ? tree.getRowBounds(tree.getRowForPath(paths.get(0))) : null;
        if (rect != null) {
            thickness = (int)Math.round(rect.getHeight());
        }
        int rowHeight;
        if (thickness > 0) { // [TODO] compute height for each particular row
            rowHeight = thickness;
        } else if (tree.getRowHeight() > 0) {
            rowHeight = tree.getRowHeight() + 2;
        } else {
            rowHeight = 18;
        }
        int zebraHeight = linesNumber * rowHeight;
        
        int width = comp.getWidth();
        int height = comp.getHeight();
        
        if ((width <= 0) || (height <= 0)) {
            return;
        }

        Rectangle clipRect = g.getClipBounds();
        int clipX;
        int clipY;
        int clipW;
        int clipH;
        if (clipRect == null) {
            clipX = clipY = 0;
            clipW = width;
            clipH = height;
        }
        else {
            clipX = clipRect.x;
            clipY = clipRect.y;
            clipW = clipRect.width;
            clipH = clipRect.height;
        }

        if(clipW > width) {
            clipW = width;
        }
        if(clipH > height) {
            clipH = height;
        }

        Color origColor = g.getColor();
        ThreadsListener threadsListener = ThreadsListener.getDefault();
        JPDADebugger debugger = threadsListener != null ? threadsListener.getDebugger() : null;
        JPDAThread currentThread = (debugger != null) ? debugger.getCurrentThread() : null;
        if (currentThread != null && !currentThread.isSuspended() &&
                !DebuggingTreeModel.isMethodInvoking(currentThread)) {
            currentThread = null;
        }
        boolean isHighlighted = false;
        boolean isCurrent = false;
        Iterator<TreePath> iter = paths.iterator();
        int firstGroupNumber = clipY / rowHeight;
        for (int x = 0; x <= firstGroupNumber && iter.hasNext(); x++) {
            Node node = Visualizer.findNode(iter.next().getLastPathComponent());
            JPDAThread thread = node.getLookup().lookup(JPDAThread.class);
            isHighlighted = focusedThread != null && thread == focusedThread;
            if (thread != null) {
                isCurrent = currentThread == thread;
            }
        }
        
        int sy = (clipY / rowHeight) * rowHeight;
        int limit = Math.min(clipY + clipH - 1, zebraHeight);
        while (sy < limit) {
            int y1 = Math.max(sy, clipY);
            int y2 = Math.min(clipY + clipH, y1 + rowHeight);
            if (isHighlighted || isCurrent) {
                //g.setColor(isHighlighted ? highlightColor : (isCurrent ? currentThreadColor : whiteColor));
                g.setColor(isHighlighted ? highlightColor : currentThreadColor);
                g.fillRect(clipX, y1, clipW, y2 - y1);
            }
            sy += rowHeight;
            if (iter.hasNext()) {
                Node node = Visualizer.findNode(iter.next().getLastPathComponent());
                JPDAThread thread = node.getLookup().lookup(JPDAThread.class);
                isHighlighted = focusedThread != null && thread == focusedThread;
                if (thread != null) {
                    isCurrent = currentThread == thread;
                }
            } else {
                isHighlighted = false;
                isCurrent = false;
            }
        }
//        if (sy < clipY + clipH - 1) {
//            g.setColor(whiteColor);
//            g.fillRect(clipX, sy, clipW, clipH + clipY - sy);
//        }
        g.setColor(origColor);
    }

    boolean threadFocuseGained(JPDAThread jpdaThread) {
        if (jpdaThread != null && focusedThread != jpdaThread) {
            focusedThread = jpdaThread;
            repaint();
            return true;
        }
        return false;
    }

    boolean threadFocuseLost(JPDAThread jpdaThread) {
        if (jpdaThread != null && focusedThread == jpdaThread) {
            focusedThread = null;
            repaint();
            return true;
        }
        return false;
    }

    private class DebugTreeNodeRenderer extends NodeRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Color background = null;
            if (value instanceof TreeNode) {
                try {
                    java.lang.reflect.Field fnode = value.getClass().getDeclaredField("node");
                    fnode.setAccessible(true);
                    value = fnode.get(value);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            if (value instanceof Node) {
                Node node = (Node) value;
                JPDAThread thread;
                do {
                    thread = node.getLookup().lookup(JPDAThread.class);
                    if (thread == null) {
                        node = node.getParentNode();
                    }
                } while (thread == null && node != null);
                if (thread != null) {
                    ThreadsListener threadsListener = ThreadsListener.getDefault();
                    JPDADebugger debugger = threadsListener != null ? threadsListener.getDebugger() : null;
                    JPDAThread currentThread = (debugger != null) ? debugger.getCurrentThread() : null;
                    if (currentThread != null && !currentThread.isSuspended() &&
                            !DebuggingTreeModel.isMethodInvoking(currentThread)) {
                        currentThread = null;
                    }
                    boolean isHighlighted = focusedThread != null && thread == focusedThread && node == value;
                    boolean isCurrent = currentThread == thread;
                    if (isHighlighted || isCurrent) {
                        background = isHighlighted ? highlightColor : currentThreadColor;
                    }
                }
            }
            Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (background != null) {
                component.setBackground(background);
                if (component instanceof JComponent) {
                    ((JComponent) component).setOpaque(true);
                }
            }
            return component;
        }
        
    }

}