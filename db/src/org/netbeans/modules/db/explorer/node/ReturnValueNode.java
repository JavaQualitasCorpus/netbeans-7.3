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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.db.explorer.node;

import org.netbeans.api.db.explorer.node.BaseNode;
import org.netbeans.api.db.explorer.node.NodeProvider;
import org.netbeans.modules.db.explorer.DatabaseConnection;
import org.netbeans.modules.db.metadata.model.api.Action;
import org.netbeans.modules.db.metadata.model.api.Metadata;
import org.netbeans.modules.db.metadata.model.api.MetadataElementHandle;
import org.netbeans.modules.db.metadata.model.api.MetadataModel;
import org.netbeans.modules.db.metadata.model.api.MetadataModelException;
import org.netbeans.modules.db.metadata.model.api.Value;
import org.openide.nodes.PropertySupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author Rob Englander
 */
public class ReturnValueNode  extends BaseNode {
    private static final String RETURN = "org/netbeans/modules/db/resources/paramReturn.gif";
    private static final String FOLDER = "ProcedureParam"; //NOI18N

    /**
     * Create an instance of ReturnValueNode.
     *
     * @param dataLookup the lookup to use when creating node providers
     * @return the ReturnValueNode instance
     */
    public static ReturnValueNode create(NodeDataLookup dataLookup, NodeProvider provider) {
        ReturnValueNode node = new ReturnValueNode(dataLookup, provider);
        node.setup();
        return node;
    }

    private String name = ""; // NOI18N
    private final MetadataElementHandle<Value> valueHandle;
    private final DatabaseConnection connection;

    @SuppressWarnings("unchecked")
    private ReturnValueNode(NodeDataLookup lookup, NodeProvider provider) {
        super(lookup, FOLDER, provider);
        valueHandle = getLookup().lookup(MetadataElementHandle.class);
        connection = getLookup().lookup(DatabaseConnection.class);
    }

    @Override
    public synchronized void refresh() {
        setupNames();
        super.refresh();
    }

    private void setupNames() {
        boolean connected = !connection.getConnector().isDisconnected();
        MetadataModel metaDataModel = connection.getMetadataModel();
        if (connected && metaDataModel != null) {
            try {
                metaDataModel.runReadAction(
                    new Action<Metadata>() {
                        public void run(Metadata metaData) {
                            Value parameter = valueHandle.resolve(metaData);
                            if (parameter != null) {
                                name = parameter.getName();
                                updateProperties(parameter);
                            }
                        }
                    }
                );
            } catch (MetadataModelException e) {
                NodeRegistry.handleMetadataModelException(this.getClass(), connection, e, true);
            }
        }
    }

    private void updateProperties(Value param) {
        PropertySupport ps = new PropertySupport.Name(this);
        addProperty(ps);

        addProperty(TYPE, TYPEDESC, String.class, false, NbBundle.getMessage (ReturnValueNode.class, "Return")); // NOI18N
        addProperty(DATATYPE, DATATYPEDESC, String.class, false, param.getType().toString());
    }

    protected void initialize() {
        setupNames();
    }

    @Override
    public String getIconBase() {
        return RETURN;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getShortDescription() {
        return NbBundle.getMessage (ReturnValueNode.class, "ND_ProcedureParam"); //NOI18N
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(ReturnValueNode.class);
    }
}
