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

package org.netbeans.modules.web.javascript.debugger.watches;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.netbeans.api.debugger.Watch;
import org.netbeans.modules.web.javascript.debugger.locals.VariablesModel;
import org.netbeans.modules.web.webkit.debugging.api.debugger.CallFrame;
import org.netbeans.modules.web.webkit.debugging.api.debugger.RemoteObject;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import static org.netbeans.spi.debugger.ui.Constants.*;
import org.netbeans.spi.viewmodel.ExtendedNodeModel;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.TreeModelFilter;
import org.netbeans.spi.viewmodel.UnknownTypeException;

@DebuggerServiceRegistration(path="javascript-debuggerengine/WatchesView", types={ TreeModelFilter.class, ExtendedNodeModel.class, TableModel.class })
public final class WatchesModel extends VariablesModel implements TreeModelFilter {

    public static final String WATCH =
            "org/netbeans/modules/debugger/resources/watchesView/watch_16.png"; // NOI18N

    private static final Logger LOG = Logger.getLogger( 
            WatchesModel.class.getCanonicalName());
    
    private WatchesListener listener;
    
    public WatchesModel(final ContextProvider contextProvider) {
        super(contextProvider);
        listener = new WatchesListener(this);
    }

    // TreeModelFilter implementation ................................................

    @Override
    public Object[] getChildren(Object parent, int from, int to)
            throws UnknownTypeException {
        throw new IllegalStateException("TreeModelFilter.getChildren() should be called instead!");
    }

    @Override
    public Object[] getChildren(TreeModel original, Object parent, int from, int to)
            throws UnknownTypeException {
        CallFrame frame = getCurrentStack();
        if (parent == ROOT) {
            evaluateWatches(frame);
            return original.getChildren(parent, from, to);
        } else if (parent instanceof Watch && frame != null) {
            ScopedRemoteObject var = evaluateWatch(frame, (Watch)parent);
            if (var == null) {
                return new Object[0];
            } else {
                return super.getChildren(var, from, to);
            }
        } else {
            return super.getChildren(parent, from, to);
        }
    }
    
    private void evaluateWatches(CallFrame frame) throws UnknownTypeException {
        if (frame == null) {
            return;
        }
        for (Watch w : DebuggerManager.getDebuggerManager().getWatches()) {
            // this triggers call to WebKit engine and outcome is cached:
            ScopedRemoteObject sro = evaluateWatch(frame, w);
        }
    }
    
    public ScopedRemoteObject evaluateWatch(CallFrame frame, Watch watch) {
        return evaluator.evaluateExpression(frame, watch.getExpression(), true);
    }
    
    @Override
    public boolean isLeaf(Object node) throws UnknownTypeException {
        throw new IllegalStateException("TreeModelFilter.isLeaf() should be called instead!");
    }

    @Override
    public boolean isLeaf(TreeModel original, Object node) throws UnknownTypeException {
        CallFrame frame = getCurrentStack();
        if (node instanceof Watch) {
            if (frame == null) {
                return true;
            }
            ScopedRemoteObject sro = evaluateWatch(frame, (Watch)node);
            if (sro != null) {
                RemoteObject var = sro.getRemoteObject();
                if (var.getType() == RemoteObject.Type.OBJECT) {
                    if (var.hasFetchedProperties()) {
                        return var.getProperties().isEmpty();
                    } else {
                        updateNodeOnBackground(node, var);
                        return true;
                    }
                }
            }
            return true;
        } else if (node instanceof ScopedRemoteObject) {
            return super.isLeaf(node);
        } else {
            return original.isLeaf(node);
        }
    }
    
    @Override
    public int getChildrenCount(Object node) throws UnknownTypeException {
        throw new IllegalStateException("TreeModelFilter.getChildrenCount() should be called instead!");
    }

    @Override
    public int getChildrenCount(TreeModel original, Object node) throws UnknownTypeException {
        if (node == ROOT) {
            return original.getChildrenCount(node);
        } else if (node instanceof Watch) {
            CallFrame frame = getCurrentStack();
            if (frame == null) {
                return 0;
            }
            ScopedRemoteObject var = evaluateWatch(frame, (Watch)node);
            if (var == null) {
                return 0;
            } else {
                return super.getChildrenCount(var);
            }
        } else {
            return super.getChildrenCount(node);
        }
    }
    
    @Override
    public Object getRoot(TreeModel original) {
        return super.getRoot();
    }

    // NodeModel implementation ................................................

    @Override
    public String getDisplayName(Object node) throws UnknownTypeException {
        if (node instanceof Watch) {
            return ((Watch) node).getExpression();
        } else {
            return super.getDisplayName(node);
        }
    }

    @Override
    public String getIconBaseWithExtension(Object node) throws UnknownTypeException {
        if (node == ROOT || node instanceof Watch) {
            return WATCH;
        } else {
            return super.getIconBaseWithExtension(node);
        }
    }

    @Override
    public String getShortDescription(Object node)
            throws UnknownTypeException {
        if (node instanceof Watch) {
            return "";
        } else {
            return super.getShortDescription(node);
        }
    }


    // TableModel implementation ...............................................

    @Override
    public Object getValueAt(Object node, String columnID) throws
            UnknownTypeException {
        if(node instanceof Watch) {
            CallFrame frame = getCurrentStack();
            ScopedRemoteObject var = null;
            if (frame != null) {
                var = evaluateWatch(frame, (Watch)node);
            }
            if (var != null) {
                return getValueAt(var, columnID);
            } else {
                if (WATCH_VALUE_COLUMN_ID.equals(columnID)) {
                    return "";
                } else if(WATCH_TYPE_COLUMN_ID.equals(columnID)) {
                    return "";
                } else if (WATCH_TO_STRING_COLUMN_ID.equals(columnID)) {
                    return "";
                }
            }
        } else {
            if (WATCH_VALUE_COLUMN_ID.equals(columnID)) {
                return super.getValueAt(node, LOCALS_VALUE_COLUMN_ID);
            } else if(WATCH_TYPE_COLUMN_ID.equals(columnID)) {
                return super.getValueAt(node, LOCALS_TYPE_COLUMN_ID);
            } else if (WATCH_TO_STRING_COLUMN_ID.equals(columnID) ||
                    LOCALS_TO_STRING_COLUMN_ID.equals(columnID)) {
                return super.getValueAt(node, LOCALS_TO_STRING_COLUMN_ID);
            }
        }
        throw new UnknownTypeException(node);
    }

    @Override
    public boolean isReadOnly(Object node, String columnID) throws UnknownTypeException {
        if (WATCH_VALUE_COLUMN_ID.equals(columnID) && node instanceof Watch) {
            CallFrame frame = getCurrentStack();
            ScopedRemoteObject var = null;
            if (frame != null) {
                var = evaluateWatch(frame, (Watch)node);
            }
            if (var == null) {
                return false;
            } else {
                return super.isReadOnly(var, columnID);
            }
        }
        return true;
    }

    @Override
    public void setValueAt(Object node, String columnID, Object value)
            throws UnknownTypeException {
        if (WATCH_VALUE_COLUMN_ID.equals(columnID) && node instanceof Watch) {
            CallFrame frame = getCurrentStack();
            if (frame == null) {
                return;
            }
            ScopedRemoteObject var = evaluateWatch(frame, (Watch)node);
            assert var != null;
            super.setValueAt(var, columnID, value);
        }        
        throw new UnknownTypeException(node);
    }

    
    private static class WatchesListener extends DebuggerManagerAdapter implements PropertyChangeListener {

        // XXX: check whether model has to be hold with WeakReference here to prevent memory leaks
        private WatchesModel model;

        public WatchesListener(WatchesModel watchesModel) {
            this.model = watchesModel;
            DebuggerManager.getDebuggerManager().addDebuggerListener(DebuggerManager.PROP_WATCHES, this);
            Watch[] watches = DebuggerManager.getDebuggerManager().getWatches();
            for (Watch watch : watches) {
                watch.addPropertyChangeListener(this);
            }
        }

        @Override
        public void watchAdded(Watch watch) {
            watch.addPropertyChangeListener(this);
            model.refresh();
        }

        @Override
        public void watchRemoved(Watch watch) {
            watch.removePropertyChangeListener(this);
            model.refresh();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (!(evt.getSource() instanceof Watch)) {
                return;
            }
            Watch w = (Watch) evt.getSource();
            model.fireChangeEvent(new ModelEvent.NodeChanged(this, w));
        }
    }

}
