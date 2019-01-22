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
package org.netbeans.modules.web.inspect.webkit.actions;

import java.awt.EventQueue;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.css.lib.api.CssParserResult;
import org.netbeans.modules.css.model.api.Model;
import org.netbeans.modules.css.model.api.Rule;
import org.netbeans.modules.css.model.api.StyleSheet;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.web.inspect.CSSUtils;
import org.netbeans.modules.web.inspect.actions.Resource;
import org.netbeans.modules.web.inspect.webkit.Utilities;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

/**
 * Go to rule action.
 *
 * @author Jan Stola
 */
public class GoToRuleSourceAction extends NodeAction {

    @Override
    protected void performAction(Node[] activatedNodes) {
        Lookup lookup = activatedNodes[0].getLookup();
        org.netbeans.modules.web.webkit.debugging.api.css.Rule rule =
                lookup.lookup(org.netbeans.modules.web.webkit.debugging.api.css.Rule.class);
        Resource resource = lookup.lookup(Resource.class);
        FileObject fob = resource.toFileObject();
        try {
            Source source = Source.create(fob);
            ParserManager.parse(Collections.singleton(source), new GoToRuleTask(rule, fob));
        } catch (ParseException ex) {
            Logger.getLogger(GoToRuleSourceAction.class.getName()).log(Level.INFO, null, ex);
        }
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        boolean enabled = false;
        if (activatedNodes.length == 1) {
            Lookup lookup = activatedNodes[0].getLookup();
            org.netbeans.modules.web.webkit.debugging.api.css.Rule rule =
                    lookup.lookup(org.netbeans.modules.web.webkit.debugging.api.css.Rule.class);
            if (rule != null) {
                Resource resource = lookup.lookup(Resource.class);
                enabled = (resource != null) && (resource.toFileObject() != null);
            }
        }
        return enabled;
    }

    @Override
    protected boolean asynchronous() {
        return true;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(GoToRuleSourceAction.class, "GoToRuleSourceAction.displayName"); // NOI18N
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    /**
     * Task that jumps on the declaration of the given rule
     * in the specified file.
     */
    static class GoToRuleTask extends UserTask {
        /** Rule to jump to. */
        private org.netbeans.modules.web.webkit.debugging.api.css.Rule rule;
        /** File to jump into. */
        private FileObject fob;

        /**
         * Creates a new {@code GoToRuleTask}.
         * 
         * @param rule rule to jump to.
         * @param fob file to jump into.
         */
        GoToRuleTask(org.netbeans.modules.web.webkit.debugging.api.css.Rule rule, FileObject fob) {
            this.rule = rule;
            this.fob = fob;
        }

        @Override
        public void run(ResultIterator resultIterator) throws Exception {
            final boolean[] found = new boolean[1];
            for (final CssParserResult result : Utilities.cssParserResults(resultIterator)) {
                final Model sourceModel = Model.getModel(result);
                sourceModel.runReadTask(new Model.ModelTask() {
                    @Override
                    public void run(StyleSheet styleSheet) {
                        Rule modelRule = Utilities.findRuleInStyleSheet(sourceModel, styleSheet, rule);
                        if (modelRule != null) {
                            found[0] = true;
                            int snapshotOffset = modelRule.getStartOffset();
                            final int offset = result.getSnapshot().getOriginalOffset(snapshotOffset);
                            EventQueue.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    CSSUtils.open(fob, offset);
                                }
                            });
                        }
                    }
                });
                if (found[0]) {
                    break;
                }
            }
        }

    }

}