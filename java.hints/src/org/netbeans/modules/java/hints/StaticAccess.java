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
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.hints;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav tulach
 */
@Hint(displayName="#MSG_StaticAccessName", description="#HINT_StaticAccess", category="general", id="org.netbeans.modules.java.hints.StaticAccess", suppressWarnings=StaticAccess.SUPPRESS_WARNINGS_KEY)
public class StaticAccess {
    
    static final String SUPPRESS_WARNINGS_KEY = "static-access";
    
    protected static Fix computeFixes(CompilationInfo info, TreePath treePath, int[] bounds, int[] kind, String[] simpleName) {
        if (treePath.getLeaf().getKind() != Kind.MEMBER_SELECT) {
            return null;
        }
        MemberSelectTree mst = (MemberSelectTree)treePath.getLeaf();
        Tree expression = mst.getExpression();
        TreePath expr = new TreePath(treePath, expression);
        
        TypeMirror tm = info.getTrees().getTypeMirror(expr);
        if (tm == null) {
            return null;
        }
        Element el = info.getTypes().asElement(tm);
        if (el == null || (!el.getKind().isClass() && !el.getKind().isInterface())) {
            return null;
        }
        
        TypeElement type = (TypeElement)el;
        
        if (isError(type)) {
            return null;
        }
        
        Name idName = null;
        
        if (expression.getKind() == Kind.MEMBER_SELECT) {
            MemberSelectTree exprSelect = (MemberSelectTree)expression;
            idName = exprSelect.getIdentifier();
        }
        
        if (expression.getKind() == Kind.IDENTIFIER) {
            IdentifierTree idt = (IdentifierTree)expression;
            idName = idt.getName();
        }
        
        if (idName != null) {
            if (idName.equals(type.getSimpleName())) {
                return null;
            }
            if (idName.equals(type.getQualifiedName())) {
                return null;
            }
        }
        
        Element used = info.getTrees().getElement(treePath);
        
        if (used == null || !used.getModifiers().contains(Modifier.STATIC)) {
            return null;
        }
        
        if (isError(used)) {
            return null;
        }
        
        if (used.getKind().isField()) {
            kind[0] = 0;
        } else {
            if (used.getKind() == ElementKind.METHOD) {
                kind[0] = 1;
            } else {
                kind[0] = 2;
            }
        }
        
        simpleName[0] = used.getSimpleName().toString();
        
        return new FixImpl(info, expr, type).toEditorFix();
    }
    
    private static boolean isError(Element e) {
        if (e == null) {
            return true;
        }
        
        if (e.getKind() != ElementKind.CLASS) {
            return false;
        }
        
        TypeMirror type = ((TypeElement) e).asType();
        
        return type == null || type.getKind() == TypeKind.ERROR;
    }
    
    @TriggerTreeKind(Kind.MEMBER_SELECT)
    public static List<ErrorDescription> run(HintContext ctx) {
        CompilationInfo compilationInfo = ctx.getInfo();
        TreePath treePath = ctx.getPath();
        int[] span = new int[2];
        int[] kind = new int[1];
        String[] simpleName = new String[1];
        Fix fix = computeFixes(compilationInfo, treePath, span, kind, simpleName);
        if (fix == null) {
            return null;
        }

        ErrorDescription ed = ErrorDescriptionFactory.forName(
            ctx,
            ctx.getPath(),
            NbBundle.getMessage(StaticAccess.class, "MSG_StaticAccess", kind[0], simpleName[0]), // NOI18N
            fix
        );

        return Collections.singletonList(ed);
    }

    static final class FixImpl extends JavaFix {
        private final ElementHandle<TypeElement> desiredType;
        public FixImpl(CompilationInfo info, TreePath expr, TypeElement desiredType) {
            super(info, expr);
            this.desiredType = ElementHandle.create(desiredType);
        }
        
        public String getText() {
            return NbBundle.getMessage(DoubleCheck.class, "MSG_StaticAccessText"); // NOI18N
        }
        
        @Override
        protected void performRewrite(TransformationContext ctx) {
            WorkingCopy wc = ctx.getWorkingCopy();
            TreePath tp = ctx.getPath();
            Element element = desiredType.resolve(wc);

            if (element == null) {
                Logger.getLogger("org.netbeans.modules.java.hints").log(Level.INFO, "Cannot resolve target element.");
                return;
            }

            ExpressionTree idt = wc.getTreeMaker().QualIdent(element);
            wc.rewrite(tp.getLeaf(), idt);
        }
    }
    
}
