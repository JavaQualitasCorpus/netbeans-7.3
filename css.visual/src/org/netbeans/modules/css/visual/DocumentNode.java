/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.css.visual;

import java.util.Collection;
import java.util.Collections;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * Root node of the document section of CSS Styles view.
 *
 * @author Marek Fukala
 */
@NbBundle.Messages({
    "DocumentNode.displayName=Style Sheets"
})
public class DocumentNode extends AbstractNode {

    /**
     * Icon base of the node.
     */
    static final String ICON_BASE = "org/netbeans/modules/css/visual/resources/style_sheet_16.png"; // NOI18N

    /**
     * Creates a new {@code DocumentNode}.
     *
     * @param pageModel owning model of the node.
     * @param filter filter for the subtree of the node.
     */
    DocumentNode(DocumentViewModel pageModel, Filter filter) {
        super(new DocumentChildren(pageModel, filter));
        setDisplayName(Bundle.DocumentNode_displayName());
        setIconBaseWithExtension(ICON_BASE);
    }

    void setModel(final DocumentViewModel model) {
        DocumentChildren children = (DocumentChildren) getChildren();
        children.setModel(model); //this will re-set the children keys
        //re-set model to all its children (stylesheets)
        Node[] nodes = getChildren().getNodes(true); //force create nodes
        for(Node node : nodes) {
            StyleSheetNode sn = (StyleSheetNode)node;
            StyleSheetNode.StyleSheetChildren snChildren = (StyleSheetNode.StyleSheetChildren)sn.getChildren();
            snChildren.setModel(model);
        }
    }

    /**
     * Factory for children of {@code DocumentNode}.
     */
    static class DocumentChildren extends Children.Keys<FileObject> implements ChangeListener {

        private static boolean first_run = true;
        private Filter filter;
        private DocumentViewModel model;

        private DocumentChildren(DocumentViewModel model, Filter filter) {
            this.model = model;
            this.filter = filter;
        }

        private void setModel(DocumentViewModel newModel) {
            if (model != null) {
                model.removeChangeListener(this);
            }
            model = newModel;
            if (model != null) {
                model.addChangeListener(this);
            }
            refreshKeys();
        }

        private void refreshKeys() {
            if (first_run) {
                //postpone the initialization until scanning finishes as we need to wait for some data
                //to be properly initialized. If CssIndex.create() is called to soon during
                //the startup then it won't obtain proper source roots
                try {
                    ParserManager.parseWhenScanFinished("text/css", new UserTask() {
                        @Override
                        public void run(ResultIterator resultIterator) throws Exception {
                            refreshKeysImpl();
                            first_run = false;
                        }
                    });
                } catch (ParseException ex) {
                    Exceptions.printStackTrace(ex);
                }

            } else {
                refreshKeysImpl();
            }
        }

        private void refreshKeysImpl() {
            Collection<FileObject> keys = model != null
                    ? model.getFilesToRulesMap().keySet()
                    : Collections.<FileObject>emptyList();

            setKeys(keys);
        }

        @Override
        protected Node[] createNodes(FileObject key) {
            return new Node[]{new StyleSheetNode(model, key, filter)};
        }

        //document model change listener
        @Override
        public void stateChanged(ChangeEvent ce) {
            refreshKeys();
        }
    }
}
