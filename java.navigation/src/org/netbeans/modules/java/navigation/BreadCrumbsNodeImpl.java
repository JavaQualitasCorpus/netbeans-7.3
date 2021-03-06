/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011-2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.navigation;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.io.CharConversionException;
import java.io.IOException;
import java.util.List;
import javax.lang.model.element.Element;
import javax.swing.Action;
import javax.swing.Icon;
import org.netbeans.api.actions.Openable;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.UiUtils;
import org.netbeans.api.java.source.ui.ElementIcons;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.breadcrumbs.spi.BreadcrumbsController;
import org.openide.actions.OpenAction;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;

/**
 *
 * @author Dusan Balek
 */
public class BreadCrumbsNodeImpl extends AbstractNode {

    private static final String COLOR = "#707070";
    private final Image icon;
    private String htmlDisplayName;
//    private OpenAction openAction;

    public BreadCrumbsNodeImpl(TreePathHandle tph, Image icon, String htmlDisplayName, FileObject fileObject, int[] pos) {
        super(Children.create(new ChildrenFactoryImpl(tph), false), Lookups.fixed(tph, pos, new OpenableImpl(fileObject, pos[0])));
        this.icon = icon;
        this.htmlDisplayName = htmlDisplayName;
//        this.openAction = new OpenAction(fileObject, pos);
    }

    @Override
    public String getHtmlDisplayName() {
        return htmlDisplayName;
    }

    @Override
    public Action getPreferredAction() {
        return OpenAction.get(OpenAction.class);
    }

    @Override
    public boolean canCopy() {
        return false;
    }

    @Override
    public boolean canCut() {
        return false;
    }

    @Override
    public boolean canDestroy() {
        return false;
    }

    @Override
    public boolean canRename() {
        return false;
    }

    @Override
    public PasteType getDropType(Transferable t, int action, int index) {
        return null;
    }

    @Override
    public Transferable drag() throws IOException {
        return null;
    }

    @Override
    protected void createPasteTypes(Transferable t, List<PasteType> s) {
        // Do nothing
    }

    @Override
    public Image getIcon(int type) {
        return icon;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return icon;
    }

    private static final String CONSTRUCTOR_NAME = "<init>";
    
    public static Node createBreadcrumbs(final CompilationInfo info, TreePath path, boolean elseSection) {
        final Trees trees = info.getTrees();
        final SourcePositions sp = trees.getSourcePositions();
        int[] pos = new int[] {(int) sp.getStartPosition(path.getCompilationUnit(), path.getLeaf()), (int) sp.getEndPosition(path.getCompilationUnit(), path.getLeaf())};
        TreePathHandle tph = TreePathHandle.create(path, info);
            final Tree leaf = path.getLeaf();
            switch (leaf.getKind()) {
                case COMPILATION_UNIT:
                    return new BreadCrumbsNodeImpl(tph, null, FileUtil.getFileDisplayName(info.getFileObject()), info.getFileObject(), pos);
                case CLASS:
                case INTERFACE:
                case ENUM:
                case ANNOTATION_TYPE:
                    return new BreadCrumbsNodeImpl(tph, iconFor(info, path), className(path), info.getFileObject(), pos);
                case METHOD:
                    MethodTree mt = (MethodTree) leaf;
                    CharSequence name;
                    if (mt.getName().contentEquals(CONSTRUCTOR_NAME)) {
                        name = ((ClassTree) path.getParentPath().getLeaf()).getSimpleName();
                    } else {
                        name = mt.getName();
                    }
                    return new BreadCrumbsNodeImpl(tph, iconFor(info, path), name.toString(), info.getFileObject(), pos);
                case VARIABLE:
                    return new BreadCrumbsNodeImpl(tph, iconFor(info, path), ((VariableTree) leaf).getName().toString(), info.getFileObject(), pos);
                case CASE:
                    ExpressionTree expr = ((CaseTree) leaf).getExpression();
                    StringBuilder sb = new StringBuilder(expr == null ? "default:" : "case "); //NOI18N
                    if (expr != null) {
                        sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                        sb.append(escape(((CaseTree) leaf).getExpression().toString()));
                        sb.append(":"); //NOI18N
                        sb.append("</font>"); //NOI18N
                    }
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case CATCH:
                    sb = new StringBuilder("catch "); //NOI18N
                    sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                    sb.append(escape(((CatchTree) leaf).getParameter().toString()));
                    sb.append("</font>"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case DO_WHILE_LOOP:
                    sb = new StringBuilder("do ... while "); //NOI18N
                    sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                    sb.append(escape(((DoWhileLoopTree) leaf).getCondition().toString()));
                    sb.append("</font>"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, null, sb.toString(), info.getFileObject(), pos);
                case ENHANCED_FOR_LOOP:
                    sb = new StringBuilder("for "); //NOI18N
                    sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                    sb.append("("); //NOI18N
                    sb.append(escape(((EnhancedForLoopTree) leaf).getVariable().toString()));
                    sb.append(" : "); //NOI18N
                    sb.append(escape(((EnhancedForLoopTree) leaf).getExpression().toString()));
                    sb.append(")"); //NOI18N
                    sb.append("</font>"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case FOR_LOOP:
                    sb = new StringBuilder("for "); //NOI18N
                    sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                    sb.append("("); //NOI18N
                    sb.append(escape(((ForLoopTree) leaf).getInitializer().toString()));
                    sb.append("; "); //NOI18N
                    sb.append(escape(((ForLoopTree) leaf).getCondition().toString()));
                    sb.append("; "); //NOI18N
                    sb.append(escape(((ForLoopTree) leaf).getUpdate().toString()));
                    sb.append(")"); //NOI18N
                    sb.append("</font>"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case IF:
                    sb = new StringBuilder(""); //NOI18N
                    Tree last = leaf;
                    while (path != null && path.getLeaf().getKind() == Kind.IF) {
                        StringBuilder temp = new StringBuilder(""); //NOI18N
                        temp.append("if "); //NOI18N
                        temp.append("<font color=").append(COLOR).append(">"); // NOI18N
                        temp.append(escape(((IfTree) path.getLeaf()).getCondition().toString()));
                        temp.append("</font>"); //NOI18N
                        
                        if (((IfTree) path.getLeaf()).getElseStatement() == last || (path.getLeaf() == leaf && elseSection)) {
                            temp.append(" else");
                        }
                        temp.append(" ");
                        sb.insert(0, temp.toString());
                        last = path.getLeaf();
                        path = path.getParentPath();
                    }
                    sb.delete(sb.length() - 1, sb.length());
                    IfTree it = (IfTree) leaf;
                    int elseStart = pos[1] + 1;
                    if (it.getElseStatement() != null) {
                        TokenSequence<JavaTokenId> ts = info.getTokenHierarchy().tokenSequence(JavaTokenId.language());
                        ts.move(elseStart = (int) sp.getStartPosition(path.getCompilationUnit(), it.getElseStatement()));
                        boolean success;
                        while ((success = ts.movePrevious()) && ts.token().id() != JavaTokenId.ELSE)
                            ;
                        elseStart = success ? Math.min(ts.offset(), elseStart) : elseStart;
                    }
                    if (elseSection) {
                        int endPos = (int) sp.getEndPosition(path.getCompilationUnit(), it.getElseStatement());
                        if (it.getElseStatement().getKind() == Kind.IF) {
                            endPos = (int) sp.getStartPosition(path.getCompilationUnit(), it.getElseStatement()) - 1;
                        }
                        pos = new int[] {elseStart, endPos};
                    } else {
                        pos[1] = elseStart - 1;
                    }
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case SWITCH:
                    sb = new StringBuilder("switch "); //NOI18N
                    sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                    sb.append(escape(((SwitchTree) leaf).getExpression().toString()));
                    sb.append("</font>"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case SYNCHRONIZED:
                    sb = new StringBuilder("synchronized "); //NOI18N
                    sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                    sb.append(escape(((SynchronizedTree) leaf).getExpression().toString()));
                    sb.append("</font>"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case TRY:
                    sb = new StringBuilder("try"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case WHILE_LOOP:
                    sb = new StringBuilder("while "); //NOI18N
                    sb.append("<font color=").append(COLOR).append(">"); // NOI18N
                    sb.append(escape(((WhileLoopTree) leaf).getCondition().toString()));
                    sb.append("</font>"); //NOI18N
                    return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                case BLOCK:
                    if (path.getParentPath().getLeaf().getKind() == Kind.TRY && ((TryTree) path.getParentPath().getLeaf()).getFinallyBlock() == leaf) {
                        sb = new StringBuilder("finally"); //NOI18N
                        return new BreadCrumbsNodeImpl(tph, DEFAULT_ICON, sb.toString(), info.getFileObject(), pos);
                    }
                    break;
            }

            return null;
    }

    static String escape(String s) {
        if (s != null) {
            try {
                return XMLUtil.toAttributeValue(s);
            } catch (CharConversionException ex) {
            }
        }
        return null;
    }
    
    private static final Image DEFAULT_ICON = BreadcrumbsController.NO_ICON;
    
    private static Image iconFor(CompilationInfo info, TreePath path) {
        Element el = info.getTrees().getElement(path);
        if (el == null) return DEFAULT_ICON;
        Icon icon = ElementIcons.getElementIcon(el.getKind(), el.getModifiers());
        if (icon == null) return DEFAULT_ICON;
        return ImageUtilities.icon2Image(icon);
    }
    
    private static String className(TreePath path) {
        ClassTree ct = (ClassTree) path.getLeaf();
        
        if (path.getParentPath().getLeaf().getKind() == Kind.NEW_CLASS) {
            NewClassTree nct = (NewClassTree) path.getParentPath().getLeaf();
            
            if (nct.getClassBody() == ct) {
                return simpleName(nct.getIdentifier());
            }
        }
        
        return ct.getSimpleName().toString();
    }
    
    private static String simpleName(Tree t) {
        switch (t.getKind()) {
            case PARAMETERIZED_TYPE:
                return simpleName(((ParameterizedTypeTree) t).getType());
            case IDENTIFIER:
                return ((IdentifierTree) t).getName().toString();
            case MEMBER_SELECT:
                return ((MemberSelectTree) t).getIdentifier().toString();
            default:
                return "";//XXX
        }
    }
    
    private static final class ChildrenFactoryImpl extends ChildFactory<Node> {

        private final TreePathHandle tph;

        public ChildrenFactoryImpl(TreePathHandle tph) {
            this.tph = tph;
        }

        @Override
        protected boolean createKeys(final List<Node> toPopulate) {
            try {
                JavaSource.forFileObject(tph.getFileObject()).runUserActionTask(new Task<CompilationController>() {
                    @Override public void run(final CompilationController cc) throws Exception {
                        cc.toPhase(Phase.RESOLVED); //XXX: resolved?

                        final TreePath tp = tph.resolve(cc);
                        
                        if (tp == null) {
                             //XXX: log
                            return;
                        }

                        tp.getLeaf().accept(new TreeScanner<Void, TreePath>() {
                            @Override public Void scan(Tree node, TreePath p) {
                                if (node == null) return null;
                                if (node.getKind() == Kind.IF) {
                                    IfTree it = (IfTree) node;
                                    Node n = createBreadcrumbs(cc, new TreePath(p, node), false);
                                    assert n != null;
                                    toPopulate.add(n);
                                    if (it.getElseStatement() != null) {
                                        n = createBreadcrumbs(cc, new TreePath(p, node), true);
                                        assert n != null;
                                        toPopulate.add(n);
                                        
                                        if (it.getElseStatement().getKind() == Kind.IF) {
                                            scan((IfTree) it.getElseStatement(), new TreePath(p, node));
                                        }
                                    }
                                    return null;
                                }
                                p = new TreePath(p, node);
                                if (cc.getTreeUtilities().isSynthetic(p)) return null;
                                Node n = createBreadcrumbs(cc, p, false);
                                if (n != null) {
                                    toPopulate.add(n);
                                } else {
                                    return super.scan(node, p);
                                }
                                return null;
                            }
                            @Override public Void visitMethod(MethodTree node, TreePath p) {
                                return scan(node.getBody(), p);
                            }
                        }, tp);
                    }
                }, true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            return true;
        }

        @Override
        protected Node createNodeForKey(Node key) {
            return key;
        }
        
    }
    
    private static final class OpenableImpl implements Openable, OpenCookie {

        private final FileObject file;
        private final int pos;

        public OpenableImpl(FileObject file, int pos) {
            this.file = file;
            this.pos = pos;
        }
        
        @Override
        public void open() {
            UiUtils.open(file, pos);
        }
        
    }
}
