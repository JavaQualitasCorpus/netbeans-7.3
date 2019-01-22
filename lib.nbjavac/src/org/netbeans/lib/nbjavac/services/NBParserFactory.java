/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.lib.nbjavac.services;

import com.sun.tools.javac.parser.EndPosParser;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.Lexer;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.netbeans.lib.nbjavac.services.NBTreeMaker.IndexedClassDecl;

/**
 *
 * @author lahvac
 */
public class NBParserFactory extends ParserFactory {

    public static void preRegister(Context context) {
        context.put(parserFactoryKey, new Context.Factory<ParserFactory>() {
            public ParserFactory make(Context c) {
                return new NBParserFactory(c);
            }
        });
    }

    private final ScannerFactory scannerFactory;
    private final Names names;
    private final CancelService cancelService;

    protected NBParserFactory(Context context) {
        super(context);
        this.scannerFactory = ScannerFactory.instance(context);
        this.names = Names.instance(context);
        this.cancelService = CancelService.instance(context);
    }

    @Override
    public Parser newParser(CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap) {
        return newParser (input, keepDocComments, keepEndPos, keepLineMap, false);
    }

    @Override
    public Parser newParser(CharSequence input, int startPos, Map<JCTree,Integer> endPos) {
        Lexer lexer = scannerFactory.newScanner(input, true);
        ((Scanner)lexer).seek(startPos);
        JavacParser p = new NBEndPosParser(this, lexer, true, false, endPos, cancelService);
        return p;
    }

    @Override
    public Parser newParser(CharSequence input, boolean keepDocComments, boolean keepEndPos, boolean keepLineMap, boolean partial) {
        Lexer lexer = scannerFactory.newScanner(input, keepDocComments);
        JavacParser p;
        if (keepEndPos) {
            p = new NBEndPosParser(this, lexer, keepDocComments, keepLineMap, cancelService);
        } else {
            p = new NBJavacParser(this, lexer, keepDocComments, keepLineMap, cancelService);
        }
        return p;
    }

    protected static class NBJavacParser extends JavacParser {

        private final Names names;
        private final CancelService cancelService;

        public NBJavacParser(NBParserFactory fac, Lexer S, boolean keepDocComments, boolean keepLineMap, CancelService cancelService) {
            super(fac, S, keepDocComments, keepLineMap);
            this.names = fac.names;
            this.cancelService = cancelService;
        }

        @Override
        protected JCClassDecl classDeclaration(JCModifiers mods, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.classDeclaration(mods, dc);
        }

        @Override
        protected JCClassDecl interfaceDeclaration(JCModifiers mods, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.interfaceDeclaration(mods, dc);
        }

        @Override
        protected JCClassDecl enumDeclaration(JCModifiers mods, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.enumDeclaration(mods, dc);
        }

        @Override
        protected JCTree methodDeclaratorRest(int pos, JCModifiers mods, JCExpression type, Name name, List<JCTypeParameter> typarams, boolean isInterface, boolean isVoid, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.methodDeclaratorRest(pos, mods, type, name, typarams, isInterface, isVoid, dc);
        }

        @Override
        public JCCompilationUnit parseCompilationUnit() {
            JCCompilationUnit toplevel = super.parseCompilationUnit();
            assignAnonymousClassIndices(names, toplevel, null, -1);
            return toplevel;
        }

    }

    public static class NBEndPosParser extends EndPosParser {

        private final Names names;
        private final CancelService cancelService;

        public NBEndPosParser(NBParserFactory fac, Lexer S, boolean keepDocComments, boolean keepLineMap, CancelService cancelService) {
            super(fac, S, keepDocComments, keepLineMap);
            this.names = fac.names;
            this.cancelService = cancelService;
        }

        public NBEndPosParser(NBParserFactory fac, Lexer S, boolean keepDocComments, boolean keepLineMap, Map<JCTree, Integer> endPositions, CancelService cancelService) {
            super(fac, S, keepDocComments, keepLineMap, endPositions);
            this.names = fac.names;
            this.cancelService = cancelService;
        }

        @Override
        protected JCClassDecl classDeclaration(JCModifiers mods, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.classDeclaration(mods, dc);
        }

        @Override
        protected JCClassDecl interfaceDeclaration(JCModifiers mods, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.interfaceDeclaration(mods, dc);
        }

        @Override
        protected JCClassDecl enumDeclaration(JCModifiers mods, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.enumDeclaration(mods, dc);
        }

        @Override
        protected JCTree methodDeclaratorRest(int pos, JCModifiers mods, JCExpression type, Name name, List<JCTypeParameter> typarams, boolean isInterface, boolean isVoid, String dc) {
            if (cancelService != null) {
                cancelService.abortIfCanceled();
            }
            return super.methodDeclaratorRest(pos, mods, type, name, typarams, isInterface, isVoid, dc);
        }
        
        @Override
        public JCCompilationUnit parseCompilationUnit() {
            JCCompilationUnit toplevel = super.parseCompilationUnit();
            assignAnonymousClassIndices(names, toplevel, null, -1);
            return toplevel;
        }

    }

    public static void assignAnonymousClassIndices(Names names, JCTree tree, Name name, int startNumber) {
        AssignAnonymousIndices aai = new AssignAnonymousIndices(names);

        if (name != null) {
            aai.newAnonScope(name, startNumber);
        }

        aai.scan(tree);
    }

    private static final class AssignAnonymousIndices extends TreeScanner {
        private final Names names;

        public AssignAnonymousIndices(Names names) {
            this.names = names;
        }

        /**
         *Represents a scope for anon class number assignment
         */
        private static class AnonScope {
            public boolean localClass;
            private final Name parentDecl;
            private int currentNumber;
            private Map<Name,Integer> localClasses;

            private AnonScope (final Name name, final int startNumber) {
                assert name != null;
                this.parentDecl = name;
                this.currentNumber = startNumber;
            }

            public int assignNumber () {
                int ret = this.currentNumber;
                if (this.currentNumber != -1) {
                    this.currentNumber++;
                }
                return ret;
            }

            public int assignLocalNumber (final Name name) {
                if (localClasses == null) {
                    localClasses = new HashMap<Name,Integer> ();
                }
                Integer num = localClasses.get(name);
                if (num == null) {
                    num = 1;
                }
                else {
                    num += 1;
                }
                localClasses.put(name, num);
                return num.intValue();
            }

            @Override
            public String toString () {
                return String.format("%s : %d",this.parentDecl.toString(), this.currentNumber);
            }
        }

        private final Map<Name, AnonScope> anonScopeMap = new HashMap<Name, AnonScope>();
        private final Stack<AnonScope> anonScopes = new Stack<AnonScope> ();

        void newAnonScope(final Name name) {
            newAnonScope(name, 1);
        }

        public void newAnonScope(final Name name, final int startNumber) {
            AnonScope parent = anonScopes.isEmpty() ? null : anonScopes.peek();
            Name fqn = parent != null && parent.parentDecl != names.empty ? parent.parentDecl.append('.', name) : name;
            AnonScope scope = anonScopeMap.get(fqn);
            if (scope == null) {
                scope = new AnonScope(name, startNumber);
                anonScopeMap.put(fqn, scope);
            }
            anonScopes.push(scope);
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            if (tree.name == names.empty) {
                ((IndexedClassDecl) tree).index = this.anonScopes.peek().assignNumber();
            }
            newAnonScope(tree.name);
            try {
                super.visitClassDef(tree);
            } finally {
                this.anonScopes.pop();
            }
            if (!this.anonScopes.isEmpty() && this.anonScopes.peek().localClass && tree.name != names.empty) {
                ((IndexedClassDecl) tree).index = this.anonScopes.peek().assignLocalNumber(tree.name);
            }
        }
        @Override
        public void visitBlock(JCBlock tree) {
            final AnonScope as = this.anonScopes.peek();
            boolean old = as.localClass;
            as.localClass = true;
            try {
                super.visitBlock(tree);
            } finally {
                as.localClass = old;
            }
        }
        @Override
        public void visitApply(JCMethodInvocation tree) {
            scan(tree.args);
            scan(tree.meth);
        }
    }

}