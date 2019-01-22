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

package org.netbeans.modules.websvc.saas.ui.actions;

import org.netbeans.modules.websvc.saas.model.SaasGroup;
import org.netbeans.modules.websvc.saas.model.SaasServicesModel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.actions.RenameAction;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * To rename a web service group
 */
public class RenameGroupAction extends RenameAction {
    
    protected boolean isEnabled(Node[] nodes) {
        if (nodes != null && nodes.length == 1) {
            SaasGroup g = nodes[0].getLookup().lookup(SaasGroup.class);
            return g != null && g.isUserDefined();
        }
        return false;
    }
    
    @Override
    protected void performAction(Node[] nodes) {
        if (nodes != null && nodes.length == 1) {
            SaasGroup group = nodes[0].getLookup().lookup(SaasGroup.class);
            if (group == null) {
                return;
            }

            Node n = nodes[0];
            NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                    NbBundle.getMessage(RenameAction.class, "CTL_RenameLabel"),
                    NbBundle.getMessage(RenameAction.class, "CTL_RenameTitle"));
            dlg.setInputText(n.getName());
            if (NotifyDescriptor.OK_OPTION.equals(DialogDisplayer.getDefault().notify(dlg))) {
                String name = dlg.getInputText().trim();
                if (group.getParent().getChildGroup(name) != null) {
                    String msg = NbBundle.getMessage(RenameGroupAction.class, "MSG_DuplicateGroupName");
                    DialogDisplayer.getDefault().notify(
                            new NotifyDescriptor.Message(msg, NotifyDescriptor.ERROR_MESSAGE));
                    return;
                }
                SaasServicesModel.getInstance().renameGroup(group, name);
                n.setName(name);
            }
        }
    }
}