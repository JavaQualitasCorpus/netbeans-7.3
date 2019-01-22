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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
package org.netbeans.modules.java.editor.codegen;

import java.util.Map.Entry;
import org.netbeans.api.java.source.TreeUtilities;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.awt.Dialog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.swing.text.JTextComponent;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.editor.java.Utilities;
import org.netbeans.modules.java.editor.codegen.ui.ElementNode;
import org.netbeans.modules.java.editor.codegen.ui.EqualsHashCodePanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.MapFormat;
import org.openide.util.NbBundle;

/**
 *
 * @author Dusan Balek
 */
public class EqualsHashCodeGenerator implements CodeGenerator {

    private static final String ERROR = "<error>"; //NOI18N

    public static class Factory implements CodeGenerator.Factory {
        
        public List<? extends CodeGenerator> create(Lookup context) {
            ArrayList<CodeGenerator> ret = new ArrayList<CodeGenerator>();
            JTextComponent component = context.lookup(JTextComponent.class);
            CompilationController controller = context.lookup(CompilationController.class);
            TreePath path = context.lookup(TreePath.class);
            path = path != null ? Utilities.getPathElementOfKind(TreeUtilities.CLASS_TREE_KINDS, path) : null;
            if (component == null || controller == null || path == null)
                return ret;
            try {
                controller.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                Element elem = controller.getTrees().getElement(path);
                if (elem != null) {
                    EqualsHashCodeGenerator gen = createEqualsHashCodeGenerator(component, controller, elem);
                    if (gen != null)
                        ret.add(gen);
                }
            } catch (IOException ioe) {
            }
            return ret;
        }
    }

    private JTextComponent component;
    final ElementNode.Description description;
    final boolean generateEquals;
    final boolean generateHashCode;
    
    /** Creates a new instance of EqualsHashCodeGenerator */
    private EqualsHashCodeGenerator(JTextComponent component, ElementNode.Description description, boolean generateEquals, boolean generateHashCode) {
        this.component = component;
        this.description = description;        
        this.generateEquals = generateEquals;
        this.generateHashCode = generateHashCode;
        
    }

    public String getDisplayName() {
        if( generateEquals && generateHashCode )
            return org.openide.util.NbBundle.getMessage(EqualsHashCodeGenerator.class, "LBL_equals_and_hashcode"); //NOI18N
        if( !generateEquals )
            return org.openide.util.NbBundle.getMessage(EqualsHashCodeGenerator.class, "LBL_hashcode"); //NOI18N
        return org.openide.util.NbBundle.getMessage(EqualsHashCodeGenerator.class, "LBL_equals"); //NOI18N
    }
    
    static EqualsHashCodeGenerator createEqualsHashCodeGenerator(JTextComponent component, CompilationController cc, Element el) throws IOException {
        if (el.getKind() != ElementKind.CLASS)
            return null;
        //#125114: ignore anonymous innerclasses:
        if (el.getSimpleName() == null || el.getSimpleName().length() == 0) {
            return null;
        }
        TypeElement typeElement = (TypeElement)el;
        
        ExecutableElement[] equalsHashCode = overridesHashCodeAndEquals(cc, typeElement, null);
        
        List<ElementNode.Description> descriptions = new ArrayList<ElementNode.Description>();
        for (VariableElement variableElement : ElementFilter.fieldsIn(typeElement.getEnclosedElements())) {
            if (!ERROR.contentEquals(variableElement.getSimpleName()) && !variableElement.getModifiers().contains(Modifier.STATIC))
                descriptions.add(ElementNode.Description.create(cc, variableElement, null, true, isUsed(cc, variableElement, equalsHashCode)));
        }
        if (descriptions.isEmpty() || (equalsHashCode[0] != null && equalsHashCode[1] != null))
            return null;
        return new EqualsHashCodeGenerator(
            component,
            ElementNode.Description.create(cc, typeElement, descriptions, false, false),
            equalsHashCode[0] == null,
            equalsHashCode[1] == null
        );
    }
    
    /** Checks whether a field is used inside given methods.
     */
    private static boolean isUsed(CompilationInfo cc, VariableElement field, ExecutableElement... methods) {
        class Used extends TreePathScanner<Void, VariableElement> {
            boolean found;
            
            @Override
            public Void visitIdentifier(IdentifierTree id, VariableElement what) {
                if (id.getName().equals(what.getSimpleName())) {
                    found = true;
                }
                
                return super.visitIdentifier(id, what);
            }

            @Override
            public Void visitMemberSelect(MemberSelectTree sel, VariableElement what) {
                if (sel.getIdentifier().equals(what.getSimpleName())) {
                    found = true;
                }
                return super.visitMemberSelect(sel, what);
            }
        }
        for (ExecutableElement e : methods) {
            if (e == null) {
                continue;
            }
            Trees tree = cc.getTrees();
            TreePath path = tree.getPath(e);
            Used used = new Used();
            used.scan(path, field);
            if (used.found) {
                return true;
            }
        }
        return false;
    }

    /** Computes whether a class defines equals and hashcode or not.
     * @param compilationInfo context 
     * @param type the class element to check
     * @param stop array of booleans that is checked for [0], if true the method imediatelly returns
     * @return array of two elements [0] is equals, if it exists, [1] is hashCode, if it exists, otherwise the indexes are null
     */
    public static ExecutableElement[] overridesHashCodeAndEquals(CompilationInfo compilationInfo, Element type, Cancel stop) {
        ExecutableElement[] ret = new ExecutableElement[2];

        TypeElement el = compilationInfo.getElements().getTypeElement("java.lang.Object"); // NOI18N
        
        if (el == null) {
            return ret;
        }
        if (type == null || type.getKind() != ElementKind.CLASS) {
            return ret;
        }

        TypeMirror objAsType = el.asType();

        if (objAsType == null || objAsType.getKind() != TypeKind.DECLARED) {
            return ret;
        }

        ExecutableElement hashCode = null;
        ExecutableElement equals = null;
        
        for (ExecutableElement method : ElementFilter.methodsIn(el.getEnclosedElements())) {
            if (stop != null && stop.isCanceled()) {
                return ret;
            }
            if (method.getSimpleName().contentEquals("equals") && method.getParameters().size() == 1 && !method.getModifiers().contains(Modifier.STATIC)) { // NOI18N
                if (compilationInfo.getTypes().isSameType(objAsType, method.getParameters().get(0).asType())) {
                    assert equals == null;
                    equals = (ExecutableElement)method;
                }
            }
            if (method.getSimpleName().contentEquals("hashCode") && method.getParameters().isEmpty() && !method.getModifiers().contains(Modifier.STATIC)) { // NOI18N
                assert hashCode == null;
                hashCode = (ExecutableElement)method;
            }
        }

        //#162267: With Java Card's runtime, there *is* no Object.hashCode() method
        if (hashCode == null || equals == null) {
            return ret;
        }
        
        TypeElement clazz = (TypeElement)type;
        for (Element ee : type.getEnclosedElements()) {
            if (stop != null && stop.isCanceled()) {
                return ret;
            }
            if (ee.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement)ee;
            
            if (compilationInfo.getElements().overrides(method, hashCode, clazz)) {
                ret[1] = method;
            }
            
            if (compilationInfo.getElements().overrides(method, equals, clazz)) {
                ret[0] = method;
            }
        }
        
        return ret;
    }
    
    public static void invokeEqualsHashCode(final TreePathHandle handle, final JTextComponent component) {
        JavaSource js = JavaSource.forDocument(component.getDocument());
        if (js != null) {
            class FillIn implements Task<CompilationController> {
                EqualsHashCodeGenerator gen;
                
                public void run(CompilationController cc) throws Exception {
                    cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    Element e = handle.resolveElement(cc);
                    
                    gen = createEqualsHashCodeGenerator(component, cc, e);
                }
                
                public void invoke() {
                    if (gen != null) {
                        gen.invoke();
                    }
                }

            }
            FillIn fillIn = new FillIn();
            try {
                js.runUserActionTask(fillIn, true);
                fillIn.invoke();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
    public void invoke() {
        final int caretOffset = component.getCaretPosition();
        final EqualsHashCodePanel panel = new EqualsHashCodePanel(description, generateEquals, generateHashCode);
        String title = NbBundle.getMessage(ConstructorGenerator.class, "LBL_generate_equals_and_hashcode"); //NOI18N
        if( !generateEquals )
            title = NbBundle.getMessage(ConstructorGenerator.class, "LBL_generate_hashcode"); //NOI18N
        else if( !generateHashCode )
            title = NbBundle.getMessage(ConstructorGenerator.class, "LBL_generate_equals"); //NOI18N
        DialogDescriptor dialogDescriptor = GeneratorUtils.createDialogDescriptor(panel, title);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.setVisible(true);
        if (dialogDescriptor.getValue() == dialogDescriptor.getDefaultValue()) {
            JavaSource js = JavaSource.forDocument(component.getDocument());
            if (js != null) {
                try {
                    ModificationResult mr = js.runModificationTask(new Task<WorkingCopy>() {
                        public void run(WorkingCopy copy) throws IOException {
                            copy.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                            Element e = description.getElementHandle().resolve(copy);
                            TreePath path = e != null ? copy.getTrees().getPath(e) : copy.getTreeUtilities().pathFor(caretOffset);
                            path = Utilities.getPathElementOfKind(TreeUtilities.CLASS_TREE_KINDS, path);
                            if (path == null) {
                                String message = NbBundle.getMessage(EqualsHashCodeGenerator.class, "ERR_CannotFindOriginalClass"); //NOI18N
                                org.netbeans.editor.Utilities.setStatusBoldText(component, message);
                            } else {
                                ArrayList<VariableElement> equalsElements = new ArrayList<VariableElement>();
                                if( generateEquals ) {
                                    for (ElementHandle<? extends Element> elementHandle : panel.getEqualsVariables())
                                        equalsElements.add((VariableElement)elementHandle.resolve(copy));
                                    }
                                ArrayList<VariableElement> hashCodeElements = new ArrayList<VariableElement>();
                                if( generateHashCode ) {
                                    for (ElementHandle<? extends Element> elementHandle : panel.getHashCodeVariables())
                                        hashCodeElements.add((VariableElement)elementHandle.resolve(copy));
                                }
                                generateEqualsAndHashCode(
                                    copy, path, 
                                    generateEquals ? equalsElements : null, 
                                    generateHashCode ? hashCodeElements : null,
                                    caretOffset
                                );
                            }
                        }
                    });
                    GeneratorUtils.guardedCommit(component, mr);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
    
    public static void generateEqualsAndHashCode(WorkingCopy wc, TreePath path) {
        ExecutableElement[] arr = overridesHashCodeAndEquals(wc, wc.getTrees().getElement(path), null);

        Collection<VariableElement> e = arr[0] == null ? Collections.<VariableElement>emptySet() : null;
        Collection<VariableElement> h = arr[1] == null ? Collections.<VariableElement>emptySet() : null;

        generateEqualsAndHashCode(wc, path, e, h, -1);
    }
    
    static void generateEqualsAndHashCode(WorkingCopy wc, TreePath path, Iterable<? extends VariableElement> equalsFields, Iterable<? extends VariableElement> hashCodeFields, int offset) {
        assert TreeUtilities.CLASS_TREE_KINDS.contains(path.getLeaf().getKind());
        TypeElement te = (TypeElement)wc.getTrees().getElement(path);
        if (te != null) {
            ClassTree nue = (ClassTree)path.getLeaf();
            Scope scope = wc.getTrees().getScope(path);
            GeneratorUtilities gu = GeneratorUtilities.get(wc);
            List<Tree> members = new ArrayList<Tree>();
            if (hashCodeFields != null) {
                members.add(createHashCodeMethod(wc, hashCodeFields, (DeclaredType)te.asType(), scope));
            }
            if (equalsFields != null) {
                members.add(createEqualsMethod(wc, equalsFields, (DeclaredType)te.asType(), scope));
            }
            wc.rewrite(nue, GeneratorUtils.insertClassMembers(wc, nue, members, offset));
        }        
    }

    private static MethodTree createEqualsMethod(WorkingCopy wc, Iterable<? extends VariableElement> equalsFields, DeclaredType type, Scope scope) {
        TreeMaker make = wc.getTreeMaker();
        Set<Modifier> mods = EnumSet.of(Modifier.PUBLIC);
        TypeElement objElement = wc.getElements().getTypeElement("java.lang.Object"); //NOI18N
        List<VariableTree> params = Collections.singletonList(make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "obj", objElement != null ? make.Type(objElement.asType()) : make.Identifier("Object"), null)); //NOI18N
        
        List<StatementTree> statements = new ArrayList<StatementTree>();
        //if (obj == null) return false;
        statements.add(make.If(make.Binary(Tree.Kind.EQUAL_TO, make.Identifier("obj"), make.Identifier("null")), make.Return(make.Identifier("false")), null)); //NOI18N
        //if (getClass() != obj.getClass()) return false;
        statements.add(make.If(make.Binary(Tree.Kind.NOT_EQUAL_TO, make.MethodInvocation(Collections.<ExpressionTree>emptyList(), make.Identifier("getClass"), Collections.<ExpressionTree>emptyList()), //NOI18N
                make.MethodInvocation(Collections.<ExpressionTree>emptyList(), make.MemberSelect(make.Identifier("obj"), "getClass"), Collections.<ExpressionTree>emptyList())), make.Return(make.Identifier("false")), null)); //NOI18N
        //<this type> other = (<this type>) o;
        statements.add(make.Variable(make.Modifiers(EnumSet.of(Modifier.FINAL)), "other", make.Type(type), make.TypeCast(make.Type(type), make.Identifier("obj")))); //NOI18N
        for (VariableElement ve : equalsFields) {
            TypeMirror tm = ve.asType();
            ExpressionTree condition = prepareExpression(wc, EQUALS_PATTERNS, tm, ve, scope);

            statements.add(make.If(condition, make.Return(make.Identifier("false")), null)); //NOI18N
        }
        statements.add(make.Return(make.Identifier("true")));
        BlockTree body = make.Block(statements, false);
        ModifiersTree modifiers = prepareModifiers(wc, mods,make);
        
        return make.Method(modifiers, "equals", make.PrimitiveType(TypeKind.BOOLEAN), Collections.<TypeParameterTree> emptyList(), params, Collections.<ExpressionTree>emptyList(), body, null); //NOI18N
    }    
    
    private static MethodTree createHashCodeMethod(WorkingCopy wc, Iterable<? extends VariableElement> hashCodeFields, DeclaredType type, Scope scope) {
        TreeMaker make = wc.getTreeMaker();
        Set<Modifier> mods = EnumSet.of(Modifier.PUBLIC);        

        int startNumber = generatePrimeNumber(2, 10);
        int multiplyNumber = generatePrimeNumber(10, 100);
        List<StatementTree> statements = new ArrayList<StatementTree>();
        //int hash = <startNumber>;
        statements.add(make.Variable(make.Modifiers(EnumSet.noneOf(Modifier.class)), "hash", make.PrimitiveType(TypeKind.INT), make.Literal(startNumber))); //NOI18N        
        for (VariableElement ve : hashCodeFields) {
            TypeMirror tm = ve.asType();
            ExpressionTree variableRead = prepareExpression(wc, HASH_CODE_PATTERNS, tm, ve, scope);
            statements.add(make.ExpressionStatement(make.Assignment(make.Identifier("hash"), make.Binary(Tree.Kind.PLUS, make.Binary(Tree.Kind.MULTIPLY, make.Literal(multiplyNumber), make.Identifier("hash")), variableRead)))); //NOI18N
        }
        statements.add(make.Return(make.Identifier("hash"))); //NOI18N        
        BlockTree body = make.Block(statements, false);
        ModifiersTree modifiers = prepareModifiers(wc, mods,make);
        
        return make.Method(modifiers, "hashCode", make.PrimitiveType(TypeKind.INT), Collections.<TypeParameterTree> emptyList(), Collections.<VariableTree>emptyList(), Collections.<ExpressionTree>emptyList(), body, null); //NOI18N
    }

    private static boolean isPrimeNumber(int n) {
        int squareRoot = (int) Math.sqrt(n) + 1;
        if (n % 2 == 0)
            return false;
        for (int cntr = 3; cntr < squareRoot; cntr++) {
            if (n % cntr == 0)
                return false;
        }
        return true;
    }

    static int randomNumber = -1;
    
    private static int generatePrimeNumber(int lowerLimit, int higherLimit) {
        if (randomNumber > 0) {
            return randomNumber;
        }
        
        Random r = new Random(System.currentTimeMillis());
        int proposed = r.nextInt(higherLimit - lowerLimit) + lowerLimit;        
        while (!isPrimeNumber(proposed))
            proposed++;
        if (proposed > higherLimit) {
            proposed--;
            while (!isPrimeNumber(proposed))
                proposed--;
        }
        return proposed;
    }
    
    private static ModifiersTree prepareModifiers(WorkingCopy wc, Set<Modifier> mods, TreeMaker make) {

        List<AnnotationTree> annotations = new LinkedList<AnnotationTree>();

        if (GeneratorUtils.supportsOverride(wc)) {
            TypeElement override = wc.getElements().getTypeElement("java.lang.Override");

            if (override != null) {
                annotations.add(wc.getTreeMaker().Annotation(wc.getTreeMaker().QualIdent(override), Collections.<ExpressionTree>emptyList()));
            }
        }

        ModifiersTree modifiers = make.Modifiers(mods, annotations);

        return modifiers;
    }

    private static KindOfType detectKind(CompilationInfo info, TypeMirror tm) {
        if (tm.getKind().isPrimitive()) {
            return KindOfType.valueOf(tm.getKind().name());
        }

        if (tm.getKind() == TypeKind.ARRAY) {
            return ((ArrayType) tm).getComponentType().getKind().isPrimitive() ? KindOfType.ARRAY_PRIMITIVE : KindOfType.ARRAY;
        }

        if (tm.getKind() == TypeKind.DECLARED) {
            Types t = info.getTypes();
            TypeElement en = info.getElements().getTypeElement("java.lang.Enum");

            if (en != null) {
                if (t.isSubtype(tm, t.erasure(en.asType()))) {
                    return KindOfType.ENUM;
                }
            }

            if (((DeclaredType)tm).asElement().getKind().isClass() && ((TypeElement) ((DeclaredType) tm).asElement()).getQualifiedName().contentEquals("java.lang.String")) {
                return KindOfType.STRING;
            }
        }

        return KindOfType.OTHER;
    }

    private static String choosePattern(CompilationInfo info, TypeMirror tm, Map<Acceptor, String> patterns) {
        for (Entry<Acceptor, String> e : patterns.entrySet()) {
            if (e.getKey().accept(info, tm)) {
                return e.getValue();
            }
        }

        throw new IllegalStateException();
    }
    
    private static ExpressionTree prepareExpression(WorkingCopy wc, Map<Acceptor, String> patterns, TypeMirror tm, VariableElement ve, Scope scope) {
        String pattern = choosePattern(wc, tm, patterns);

        assert pattern != null;
        
        String conditionText = MapFormat.format(pattern, Collections.singletonMap("VAR", ve.getSimpleName().toString()));
        ExpressionTree exp = wc.getTreeUtilities().parseExpression(conditionText, new SourcePositions[1]);

        exp = GeneratorUtilities.get(wc).importFQNs(exp);
        wc.getTreeUtilities().attributeTree(exp, scope);
        
        return exp;
    }
    
    private enum KindOfType {
        BOOLEAN,
        BYTE,
        SHORT,
        INT,
        LONG,
        CHAR,
        FLOAT,
        DOUBLE,
        ENUM,
        ARRAY_PRIMITIVE,
        ARRAY,
        STRING,
        OTHER;
    }

    private static final Map<Acceptor, String> EQUALS_PATTERNS;
    private static final Map<Acceptor, String> HASH_CODE_PATTERNS;

    static {
        EQUALS_PATTERNS = new LinkedHashMap<Acceptor, String>();

        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.BOOLEAN, KindOfType.BYTE, KindOfType.SHORT, KindOfType.INT, KindOfType.LONG, KindOfType.CHAR), "this.{VAR} != other.{VAR}");
        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.FLOAT), "java.lang.Float.floatToIntBits(this.{VAR}) != java.lang.Float.floatToIntBits(other.{VAR})");
        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.DOUBLE), "java.lang.Double.doubleToLongBits(this.{VAR}) != java.lang.Double.doubleToLongBits(other.{VAR})");
        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.ENUM), "this.{VAR} != other.{VAR}");
        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.ARRAY_PRIMITIVE), "! java.util.Arrays.equals(this.{VAR}, other.{VAR}");
        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.ARRAY), "! java.util.Arrays.deepEquals(this.{VAR}, other.{VAR}");
        EQUALS_PATTERNS.put(new MethodExistsAcceptor("java.util.Objects", "equals", SourceVersion.RELEASE_7), "! java.util.Objects.equals(this.{VAR}, other.{VAR})");
        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.STRING), "(this.{VAR} == null) ? (other.{VAR} != null) : !this.{VAR}.equals(other.{VAR})");
        EQUALS_PATTERNS.put(new SimpleAcceptor(KindOfType.OTHER), "this.{VAR} != other.{VAR} && (this.{VAR} == null || !this.{VAR}.equals(other.{VAR}))");

        HASH_CODE_PATTERNS = new LinkedHashMap<Acceptor, String>();

        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.BYTE, KindOfType.SHORT, KindOfType.INT, KindOfType.CHAR), "this.{VAR}");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.LONG), "(int) (this.{VAR} ^ (this.{VAR} >>> 32))");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.FLOAT), "java.lang.Float.floatToIntBits(this.{VAR})");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.DOUBLE), "(int) (Double.doubleToLongBits(this.{VAR}) ^ (Double.doubleToLongBits(this.{VAR}) >>> 32))");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.BOOLEAN), "(this.{VAR} ? 1 : 0)");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.ENUM), "(this.{VAR} != null ? this.{VAR}.hashCode() : 0)");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.ARRAY_PRIMITIVE), "java.util.Arrays.hashCode(this.{VAR}");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.ARRAY), "java.util.Arrays.deepHashCode(this.{VAR}");
        HASH_CODE_PATTERNS.put(new MethodExistsAcceptor("java.util.Objects", "hashCode", SourceVersion.RELEASE_7), "java.util.Objects.hashCode(this.{VAR})");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.STRING), "(this.{VAR} != null ? this.{VAR}.hashCode() : 0)");
        HASH_CODE_PATTERNS.put(new SimpleAcceptor(KindOfType.OTHER), "(this.{VAR} != null ? this.{VAR}.hashCode() : 0)");
    }

    private static interface Acceptor {
        public boolean accept(CompilationInfo info, TypeMirror tm);
    }

    private static final class SimpleAcceptor implements Acceptor {
        private final Set<KindOfType> kinds;

        public SimpleAcceptor(KindOfType kind) {
            kinds = EnumSet.of(kind);
        }

        public SimpleAcceptor(KindOfType kind, KindOfType... moreKinds) {
            this.kinds = EnumSet.of(kind);
            this.kinds.addAll(Arrays.asList(moreKinds));
        }

        public boolean accept(CompilationInfo info, TypeMirror tm) {
            return kinds.contains(detectKind(info, tm));
        }

    }

    private static final class MethodExistsAcceptor implements Acceptor {
        private final String fqn;
        private final String methodName;
        private final SourceVersion minimalVersion;

        public MethodExistsAcceptor(String fqn, String methodName) {
            this(fqn, methodName, null);
        }

        public MethodExistsAcceptor(String fqn, String methodName, SourceVersion minimalVersion) {
            this.fqn = fqn;
            this.methodName = methodName;
            this.minimalVersion = minimalVersion;
        }

        public boolean accept(CompilationInfo info, TypeMirror tm) {
            if (minimalVersion != null && minimalVersion.compareTo(info.getSourceVersion()) > 0) return false;
            
            TypeElement clazz = info.getElements().getTypeElement(fqn);

            if (clazz == null) {
                return false;
            }

            for (ExecutableElement m : ElementFilter.methodsIn(clazz.getEnclosedElements())) {
                if (m.getSimpleName().contentEquals(methodName)) {
                    return true;
                }
            }

            return false;
        }

    }

    public interface Cancel {
        public boolean isCanceled();
    }

}