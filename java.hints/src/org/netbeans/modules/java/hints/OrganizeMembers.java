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
package org.netbeans.modules.java.hints;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;

import org.netbeans.api.editor.EditorActionNames;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.ModificationResult.Difference;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.editor.BaseAction;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.GuardedDocument;
import org.netbeans.editor.MarkBlock;
import org.netbeans.editor.MarkBlockChain;
import org.netbeans.modules.editor.java.JavaKit;
import org.netbeans.modules.editor.java.Utilities;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Dusan Balek
 */
@Hint(displayName = "#DN_org.netbeans.modules.java.hints.OrganizeMembers", description = "#DESC_org.netbeans.modules.java.hints.OrganizeMembers", category = "class_structure", enabled = false)
public class OrganizeMembers {

    @TriggerTreeKind(Kind.CLASS)
    public static ErrorDescription checkMembers(final HintContext context) {
        Source source = context.getInfo().getSnapshot().getSource();
        try {
            ModificationResult result = ModificationResult.runModificationTask(Collections.singleton(source), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    WorkingCopy copy = WorkingCopy.get(resultIterator.getParserResult());
                    copy.toPhase(Phase.RESOLVED);
                    doOrganizeMembers(copy, context.getPath());
                }
            });
            List<? extends Difference> diffs = result.getDifferences(source.getFileObject());
            if (diffs != null && !diffs.isEmpty() && !checkGuarded(context.getInfo().getDocument(), diffs)) {
                Fix fix = new OrganizeMembersFix(context.getInfo(), context.getPath()).toEditorFix();
                SourcePositions sp = context.getInfo().getTrees().getSourcePositions();
                int offset = diffs.get(0).getStartPosition().getOffset();
                CompilationUnitTree cut = context.getPath().getCompilationUnit();
                ClassTree clazz = (ClassTree) context.getPath().getLeaf();
                for (Tree member : clazz.getMembers()) {
                    if (context.getInfo().getTreeUtilities().isSynthetic(new TreePath(context.getPath(), member))) continue;
                    if (sp.getStartPosition(cut, member) >= offset) {
                        return ErrorDescriptionFactory.forTree(context, member, NbBundle.getMessage(OrganizeMembers.class, "MSG_OragnizeMembers"), fix); //NOI18N
                    }
                }
                return ErrorDescriptionFactory.forTree(context, clazz, NbBundle.getMessage(OrganizeMembers.class, "MSG_OragnizeMembers"), fix); //NOI18N
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private static void doOrganizeMembers(WorkingCopy copy, TreePath path) {
        ClassTree clazz = (ClassTree) path.getLeaf();
        TreeMaker maker = copy.getTreeMaker();
        ClassTree nue = maker.Class(clazz.getModifiers(), clazz.getSimpleName(), clazz.getTypeParameters(), clazz.getExtendsClause(), clazz.getImplementsClause(), Collections.<Tree>emptyList());
        List<Tree> members = new ArrayList<Tree>(clazz.getMembers().size());
        for (Tree tree : clazz.getMembers()) {
            if (copy.getTreeUtilities().isSynthetic(new TreePath(path, tree))) continue;
            Tree member;
            switch (tree.getKind()) {
                case VARIABLE:
                    member = maker.setLabel(tree, ((VariableTree)tree).getName());
                    break;
                case METHOD:
                    member = maker.setLabel(tree, ((MethodTree)tree).getName());
                    break;
                case BLOCK:
                    member = maker.Block(((BlockTree)tree).getStatements(), ((BlockTree)tree).isStatic());
                    break;
                default:
                    member = tree;    
            }
            members.add(member);
        }
        nue = GeneratorUtilities.get(copy).insertClassMembers(nue, members);
        copy.rewrite(clazz, nue);
    }
    
    private static boolean checkGuarded(Document doc, List<? extends Difference> diffs) {
        if (doc instanceof GuardedDocument) {
            MarkBlockChain chain = ((GuardedDocument) doc).getGuardedBlockChain();
            for (Difference diff : diffs) {
                if ((chain.compareBlock(diff.getStartPosition().getOffset(), diff.getEndPosition().getOffset()) & MarkBlock.OVERLAP) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class OrganizeMembersFix extends JavaFix {

        public OrganizeMembersFix(CompilationInfo info, TreePath tp) {
            super(info, tp);
        }

        @Override
        public String getText() {
            return NbBundle.getMessage(OrganizeMembers.class, "FIX_OrganizeMembers"); //NOI18N
        }

        @Override
        protected void performRewrite(TransformationContext ctx) {
            doOrganizeMembers(ctx.getWorkingCopy(), ctx.getPath());
        }
    }

    @EditorActionRegistration(name = EditorActionNames.organizeMembers,
                              mimeType = JavaKit.JAVA_MIME_TYPE,
                              menuPath = "Source",
                              menuPosition = 2437,
                              menuText = "#" + EditorActionNames.organizeMembers + "_menu_text")
    public static class OrganizeMembersAction extends BaseAction {

        @Override
        public void actionPerformed(final ActionEvent evt, final JTextComponent component) {
            if (component == null || !component.isEditable() || !component.isEnabled()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            final BaseDocument doc = (BaseDocument) component.getDocument();
            final Source source = Source.create(doc);
            if (source != null) {
                final AtomicBoolean cancel = new AtomicBoolean();
                ProgressUtils.runOffEventDispatchThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ModificationResult result = ModificationResult.runModificationTask(Collections.singleton(source), new UserTask() {
                                @Override
                                public void run(ResultIterator resultIterator) throws Exception {
                                    WorkingCopy copy = WorkingCopy.get(resultIterator.getParserResult());
                                    copy.toPhase(Phase.RESOLVED);
                                    TreePath path = copy.getTreeUtilities().pathFor(component.getCaretPosition());
                                    path = Utilities.getPathElementOfKind(EnumSet.of(Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.ANNOTATION_TYPE), path);
                                    if (path != null) {
                                        doOrganizeMembers(copy, path);
                                    } else {
                                        CompilationUnitTree cut = copy.getCompilationUnit();
                                        List<? extends Tree> typeDecls = cut.getTypeDecls();
                                        if (typeDecls.isEmpty()) {
                                            Toolkit.getDefaultToolkit().beep();
                                        } else {
                                            doOrganizeMembers(copy, copy.getTrees().getPath(cut, typeDecls.get(0)));
                                        }
                                    }
                                }
                            });
                            List<? extends Difference> diffs = result.getDifferences(source.getFileObject());
                            if (diffs != null && !diffs.isEmpty() && !checkGuarded(doc, diffs)) {
                                result.commit();
                            } else {
                                Toolkit.getDefaultToolkit().beep();
                            }
                        } catch (Exception ex) {
                            Toolkit.getDefaultToolkit().beep();
                        }
                    }
                }, NbBundle.getMessage(OrganizeMembers.class, "MSG_OragnizeMembers"), cancel, false); //NOI18N
            }
        }
    }
}