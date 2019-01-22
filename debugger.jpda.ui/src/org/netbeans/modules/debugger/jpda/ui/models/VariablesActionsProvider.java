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

package org.netbeans.modules.debugger.jpda.ui.models;

import javax.swing.Action;

import org.netbeans.api.debugger.jpda.*;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.viewmodel.Models;
import org.netbeans.spi.viewmodel.NodeActionsProvider;
import org.netbeans.spi.viewmodel.TreeModel;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.UnknownTypeException;

import org.netbeans.modules.debugger.jpda.ui.SourcePath;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.DebuggerServiceRegistrations;

import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;


/**
 * @author   Jan Jancura
 */
@DebuggerServiceRegistrations({
    @DebuggerServiceRegistration(path="netbeans-JPDASession/LocalsView",
                                 types={NodeActionsProvider.class},
                                 position=650),
    @DebuggerServiceRegistration(path="netbeans-JPDASession/WatchesView",
                                 types={NodeActionsProvider.class},
                                 position=650),
    @DebuggerServiceRegistration(path="netbeans-JPDASession/ResultsView",
                                 types={NodeActionsProvider.class},
                                 position=650),
    @DebuggerServiceRegistration(path="netbeans-JPDASession/ToolTipView",
                                 types={NodeActionsProvider.class},
                                 position=650)
})
public class VariablesActionsProvider implements NodeActionsProvider {
    
    
    private final Action GO_TO_SOURCE_ACTION = Models.createAction (
        NbBundle.getMessage(VariablesActionsProvider.class, "CTL_GoToSource"), 
        new Models.ActionPerformer () {
            public boolean isEnabled (Object node) {
                return true;
            }
            public void perform (final Object[] nodes) {
                lookupProvider.lookupFirst(null, RequestProcessor.class).post(new Runnable() {
                    public void run() {
                        goToSource ((Field) nodes [0]);
                    }
                });
            }
        },
        Models.MULTISELECTION_TYPE_EXACTLY_ONE
    );
        
        
    private ContextProvider lookupProvider;

    
    public VariablesActionsProvider (ContextProvider lookupProvider) {
        this.lookupProvider = lookupProvider;
    }

    public Action[] getActions (Object node) throws UnknownTypeException {
        if (node == TreeModel.ROOT)
            return new Action[] {
                WatchesActionsProvider.NEW_WATCH_ACTION
            };
        if (node instanceof Field)
            return new Action [] {
                GO_TO_SOURCE_ACTION
            };
        if (node instanceof Variable)
            return new Action [] {
            };
        if (node.toString().startsWith ("SubArray")) // NOI18N
            return new Action [] {
            };
        if (node.equals ("NoInfo")) // NOI18N
            return new Action [] {
            };
        throw new UnknownTypeException (node);
    }
    
    public void performDefaultAction (final Object node) throws UnknownTypeException {
        if (node == TreeModel.ROOT) 
            return;
        if (node instanceof Field) {
            lookupProvider.lookupFirst(null, RequestProcessor.class).post(new Runnable() {
                public void run() {
                    goToSource ((Field) node);
                }
            });
            return;
        }
        if (node.toString().startsWith ("SubArray")) // NOI18N
            return ;
        if (node.equals ("NoInfo")) // NOI18N
            return;
        throw new UnknownTypeException (node);
    }

    /** 
     *
     * @param l the listener to add
     */
    public void addModelListener (ModelListener l) {
    }

    /** 
     *
     * @param l the listener to remove
     */
    public void removeModelListener (ModelListener l) {
    }
    
    private void goToSource (Field variable) {
        SourcePath ectx = lookupProvider.lookupFirst(null, SourcePath.class);
        ectx.showSource (variable);
    }

    private boolean isSourceAvailable (Field v) {
        SourcePath ectx = lookupProvider.lookupFirst(null, SourcePath.class);
        return ectx.sourceAvailable (v);
    }
}