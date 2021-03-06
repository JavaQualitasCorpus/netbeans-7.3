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

import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
public class IllegalInstanceOf extends AbstractHint {

    public IllegalInstanceOf() {
        super(false, true, HintSeverity.WARNING);
    }

    public Set<Kind> getTreeKinds() {
        return EnumSet.of(Kind.INSTANCE_OF);
    }

    public List<ErrorDescription> run(CompilationInfo info, TreePath treePath) {
        if (treePath.getLeaf().getKind() != Kind.INSTANCE_OF) {
            return null;
        }
        
        InstanceOfTree iot = (InstanceOfTree) treePath.getLeaf();
        TypeMirror     leftTypeMirror = info.getTrees().getTypeMirror(new TreePath(treePath, iot.getExpression()));
        Element        rightType = info.getTrees().getElement(new TreePath(treePath, iot.getType()));
        
        if (leftTypeMirror == null || leftTypeMirror.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        Element leftType = ((DeclaredType) leftTypeMirror).asElement();
        
        if (leftType == null || rightType == null || !leftType.getKind().isInterface() || !rightType.getKind().isInterface()) {
            //no problem:
            return null;
        }
        
        TypeElement left = (TypeElement) leftType;
        TypeElement right = (TypeElement) rightType;
        
        if (   left.getEnclosingElement().getKind() != ElementKind.PACKAGE
            || right.getEnclosingElement().getKind() != ElementKind.PACKAGE) {
            return null;
        }
        
        PackageElement leftPackage = (PackageElement) left.getEnclosingElement();
        PackageElement rightPackage = (PackageElement) right.getEnclosingElement();
        
        String leftPackageFQN = leftPackage.getQualifiedName().toString();
        String rightPackageFQN = rightPackage.getQualifiedName().toString();
        
        if (packagesToCheck.containsKey(leftPackageFQN) && leftPackageFQN.equals(rightPackageFQN)) {
            String verifyClass = packagesToCheck.get(leftPackageFQN);
            TypeElement loadedVerify = info.getElements().getTypeElement(verifyClass);
            
            if (!info.getTypes().isSubtype(left.asType(), loadedVerify.asType()))
                return null;
            
            if (!info.getTypes().isSubtype(right.asType(), loadedVerify.asType()))
                return null;
            
            int start = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), iot);
            int end   = (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), iot);
            return Collections.<ErrorDescription>singletonList(
                       ErrorDescriptionFactory.createErrorDescription(getSeverity().toEditorSeverity(),
                                                                      NbBundle.getMessage(IllegalInstanceOf.class, "MSG_IllegalInstanceOf"),
                                                                      info.getFileObject(),
                                                                      start,
                                                                      end
                                                                     )
                   );
        }
        
        return null;
    }
    
    private static Map<String, String> packagesToCheck = new HashMap<String, String>();
    
    static {
        packagesToCheck.put("javax.lang.model.element", "javax.lang.model.element.Element"); //NOI18N
        packagesToCheck.put("javax.lang.model.type", "javax.lang.model.type.TypeMirror"); //NOI18N
        packagesToCheck.put("com.sun.source.tree", "com.sun.source.tree.Tree"); //NOI18N
    }

    public String getId() {
        return IllegalInstanceOf.class.getName();
    }

    public String getDisplayName() {
        return NbBundle.getMessage(IllegalInstanceOf.class, "LBL_IllegalInstanceOf");
    }

    public String getDescription() {
        return NbBundle.getMessage(IllegalInstanceOf.class, "DSC_IllegalInstanceOf");
    }

    public void cancel() {}

}
