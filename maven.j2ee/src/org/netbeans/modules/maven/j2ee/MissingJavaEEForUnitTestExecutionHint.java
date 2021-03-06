/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.j2ee;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;


/**
 * Warn user when Java EE APIs are missing.
 */
public class MissingJavaEEForUnitTestExecutionHint extends AbstractHint {

    private static final Set<Tree.Kind> TREE_KINDS =
            EnumSet.<Tree.Kind>of(Kind.MEMBER_SELECT, Kind.IDENTIFIER);
    
    public MissingJavaEEForUnitTestExecutionHint() {
        super(true, true, AbstractHint.HintSeverity.ERROR);
    }
    
    @Override
    public String getDescription() {
        return NbBundle.getMessage(MissingJavaEEForUnitTestExecutionHint.class, "MissingJavaEEForUnitTestExecutionHint_Description"); // NOI18N
    }

    @Override
    public Set<Kind> getTreeKinds() {
        return TREE_KINDS;
    }

    @Override
    public List<org.netbeans.spi.editor.hints.ErrorDescription> run(CompilationInfo info, TreePath treePath) {
        Element el = info.getTrees().getElement(treePath);
        if (el == null) {
            return null;
        }
        //Logger.getAnonymousLogger().log(Level.SEVERE, "---"+el+"  "+(treePath.getLeaf() != null ? treePath.getLeaf().getKind() : "no kind"));
        if (el.asType() == null || !el.asType().getKind().equals(TypeKind.DECLARED)) {
            return null;
        }
        if (!isEEType(info, el.asType())) {
            return null;
        }
        
        String name = el.asType().toString();
        FileObject testFile = info.getFileObject();
        Project prj = FileOwnerQuery.getOwner(testFile);
        if (prj == null) {
            return null;
        }
        NbMavenProject mp = prj.getLookup().lookup(NbMavenProject.class);
        if (mp == null) {
            // handles only Maven projects; Ant projects solves this issue differently
            return null;
        }

        List<String> testRoots = mp.getMavenProject().getTestCompileSourceRoots();
        String path = FileUtil.getFileDisplayName(testFile);
        boolean unitTest = false;
        for (String testRoot : testRoots) {
            if (path.startsWith(testRoot)) {
                unitTest = true;
                break;
            }
        }
        if (!unitTest) {
            // relevant only for unit tests which are going to be executed
            return null;
        }
        ClassPath cp = ClassPath.getClassPath(testFile, ClassPath.EXECUTE);
        if (cp == null) {
            return null;
        }
        boolean javaeeJar = false;
        boolean gfServer = false;
        for (FileObject cpRoot : cp.getRoots()) {
            FileObject fo = FileUtil.getArchiveFile(cpRoot);
            if (fo == null) {
                continue;
            }
            if (fo.getNameExt().toLowerCase().contains("javaee-web-api-6.0") || // NOI18N
                    fo.getNameExt().toLowerCase().contains("javaee-api-6.0")) { // NOI18N
                javaeeJar = true;
            }
            
            if (fo.getNameExt().toLowerCase().contains("glassfish-embedded-static-shell") || // NOI18N
                    fo.getNameExt().toLowerCase().contains("glassfish-embedded-all")) { // NOI18N
                // GF is on project classpath; everything should be OK
                return null;
            }
        }
        try {
            cp.getClassLoader(true).loadClass(name); // NOI18N
            return null;
        } catch (ClassFormatError tt) {
            // OK, show hint to add JavaEE API
        } catch (ClassNotFoundException tt) {
            // #196713 - ignore this exception; it can happen for example when project classes are not compiled
            return null;
        }
        
        Tree t = treePath.getLeaf();
        return Collections.<ErrorDescription>singletonList(
                ErrorDescriptionFactory.createErrorDescription(
                getSeverity().toEditorSeverity(),
                getDisplayName(javaeeJar),
                new ArrayList<Fix>(),
                info.getFileObject(),
                (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), t),
                (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), t)));
    }

    private boolean isEEType(CompilationInfo info, TypeMirror type) {
        if (type == null) {
            return false;
        }
        if (isEEType(type)) {
            return true;
        }
        List<? extends TypeMirror> l = info.getTypes().directSupertypes(type);
        for (TypeMirror m : l) {
            if (isEEType(info, m)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isEEType(TypeMirror type) {
        if (type == null) {
            return false;
        }
        String name = type.toString();
        return (name.startsWith("javax.")); // NOI18N
    }

    @Override
    public String getId() {
        return "MissingJavaEEForUnitTestExecutionHint"; // NOI18N
    }

    @Override
    public String getDisplayName() {
        return getDisplayName(false);
    }

    public String getDisplayName(boolean javaeeJar) {
        if (javaeeJar) {
            return NbBundle.getMessage(MissingJavaEEForUnitTestExecutionHint.class, "MissingJavaEEForUnitTestExecutionHint_DisplayName2"); // NOI18N
        } else {
            return NbBundle.getMessage(MissingJavaEEForUnitTestExecutionHint.class, "MissingJavaEEForUnitTestExecutionHint_DisplayName"); // NOI18N
        }
    }
    
    @Override
    public void cancel() {
    }

}
