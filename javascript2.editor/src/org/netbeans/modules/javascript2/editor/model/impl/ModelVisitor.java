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
package org.netbeans.modules.javascript2.editor.model.impl;


import jdk.nashorn.internal.ir.AccessNode;
import jdk.nashorn.internal.ir.BinaryNode;
import jdk.nashorn.internal.ir.CallNode;
import jdk.nashorn.internal.ir.CatchNode;
import jdk.nashorn.internal.ir.FunctionNode;
import jdk.nashorn.internal.ir.IdentNode;
import jdk.nashorn.internal.ir.IndexNode;
import jdk.nashorn.internal.ir.LiteralNode;
import jdk.nashorn.internal.ir.Node;
import jdk.nashorn.internal.ir.ObjectNode;
import jdk.nashorn.internal.ir.PropertyNode;
import jdk.nashorn.internal.ir.ReferenceNode;
import jdk.nashorn.internal.ir.ReturnNode;
import jdk.nashorn.internal.ir.TernaryNode;
import jdk.nashorn.internal.ir.UnaryNode;
import jdk.nashorn.internal.ir.VarNode;
import jdk.nashorn.internal.parser.Token;
import jdk.nashorn.internal.parser.TokenType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.javascript2.editor.doc.DocumentationUtils;
import org.netbeans.modules.javascript2.editor.doc.spi.DocIdentifier;
import org.netbeans.modules.javascript2.editor.doc.spi.DocParameter;
import org.netbeans.modules.javascript2.editor.doc.spi.JsComment;
import org.netbeans.modules.javascript2.editor.doc.spi.JsDocumentationHolder;
import org.netbeans.modules.javascript2.editor.embedding.JsEmbeddingProvider;
import org.netbeans.modules.javascript2.editor.lexer.LexUtilities;
import org.netbeans.modules.javascript2.editor.model.DeclarationScope;
import org.netbeans.modules.javascript2.editor.model.Identifier;
import org.netbeans.modules.javascript2.editor.model.JsElement;
import org.netbeans.modules.javascript2.editor.model.JsFunction;
import org.netbeans.modules.javascript2.editor.model.JsObject;
import org.netbeans.modules.javascript2.editor.model.Model;
import org.netbeans.modules.javascript2.editor.model.Occurrence;
import org.netbeans.modules.javascript2.editor.model.Type;
import org.netbeans.modules.javascript2.editor.model.TypeUsage;
import org.netbeans.modules.javascript2.editor.parser.JsParserResult;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Petr Pisl
 */
public class ModelVisitor extends PathNodeVisitor {

    private final ModelBuilder modelBuilder;
    /**
     * Keeps the name of the visited properties
     */
    private final List<List<FunctionNode>> functionStack;
    private final JsParserResult parserResult;

    private JsObjectImpl fromAN = null;
    private boolean inVarNode = false;

    public ModelVisitor(JsParserResult parserResult) {
        FileObject fileObject = parserResult.getSnapshot().getSource().getFileObject();
        this.modelBuilder = new ModelBuilder(JsFunctionImpl.createGlobal(fileObject, Integer.MAX_VALUE));
        this.functionStack = new ArrayList<List<FunctionNode>>();
        this.parserResult = parserResult;
    }

    public JsObject getGlobalObject() {
        return modelBuilder.getGlobal();
    }

    @Override
    public Node enter(AccessNode accessNode) {
        BinaryNode node = getPath().get(getPath().size() - 1) instanceof BinaryNode
                ? (BinaryNode)getPath().get(getPath().size() - 1) : null;
        if (!(node != null && node.tokenType() == TokenType.ASSIGN)) {
            if (accessNode.getBase() instanceof IdentNode && "this".equals(((IdentNode)accessNode.getBase()).getName())) { //NOI18N
                IdentNode iNode = (IdentNode)accessNode.getProperty();
                JsObject current = modelBuilder.getCurrentDeclarationFunction();
                JsObject property = current.getProperty(iNode.getName());
                if (property == null && current.getParent() != null && (current.getParent().getJSKind() == JsElement.Kind.CONSTRUCTOR
                        || current.getParent().getJSKind() == JsElement.Kind.OBJECT)) {
                    current = current.getParent();
                    if (current.getName().equals("prototype")) {
                        current = current.getParent();
                    }
                    property = current.getProperty(iNode.getName());
                }
                if (property == null && current.getParent() == null) {
                    // probably we are in global space and there is used this
                    property = modelBuilder.getGlobal().getProperty(iNode.getName());
                }
                if (property != null) {
                    ((JsObjectImpl)property).addOccurrence(ModelUtils.documentOffsetRange(parserResult, iNode.getStart(), iNode.getFinish()));
                }
            }
        }
        return super.enter(accessNode);
    }

    @Override
    public Node leave(AccessNode accessNode) {
        if (accessNode.getBase() instanceof IdentNode) {
            IdentNode base = (IdentNode)accessNode.getBase();
            if (!"this".equals(base.getName())) {
                Identifier name = ModelElementFactory.create(parserResult, (IdentNode)accessNode.getBase());
                if (name != null) {
                    List<Identifier> fqname = new ArrayList<Identifier>();
                    fqname.add(name); 
                    Collection<? extends JsObject> variables = ModelUtils.getVariables(modelBuilder.getCurrentDeclarationFunction());
                    fromAN = null;
                    for(JsObject variable : variables) {
                        if (variable.getName().equals(name.getName())) {
                            fromAN = (JsObjectImpl)variable;
                            break;
                        }
                    }
                    if (fromAN == null) {
                        fromAN = ModelUtils.getJsObject(modelBuilder, fqname, false);
                    }
                    fromAN.addOccurrence(name.getOffsetRange());
                }
            } else {
                JsObject current = modelBuilder.getCurrentDeclarationFunction();
                JsObject property = current.getProperty(accessNode.getProperty().getName());
                if (property == null && current.getParent() != null && (current.getParent().getJSKind() == JsElement.Kind.CONSTRUCTOR
                        || current.getParent().getJSKind() == JsElement.Kind.OBJECT
                        || current.getParent().getJSKind() == JsElement.Kind.OBJECT_LITERAL)) {
                    Node previous = getPreviousFromPath(2);
                    // check whether is not a part of method in constructor
                    if (!(previous instanceof BinaryNode && ((BinaryNode)previous).rhs() instanceof ReferenceNode)) {
                        current = current.getParent();
                        if (current.getName().equals("prototype")) {
                            current = current.getParent();
                        }
                    }
                }
                fromAN = (JsObjectImpl)current;

            }
        }
        if (fromAN != null) {
            JsObjectImpl property = (JsObjectImpl)fromAN.getProperty(accessNode.getProperty().getName());
            int pathSize = getPath().size();
            Node lastVisited = getPath().get(pathSize - 2);
            boolean onLeftSite =  lastVisited instanceof BinaryNode && ((BinaryNode)lastVisited).lhs().equals(accessNode);
            if (property != null) {
                if(onLeftSite && !property.isDeclared()) {
                    property.setDeclared(true);
                }
                property.addOccurrence(ModelUtils.documentOffsetRange(parserResult, accessNode.getProperty().getStart(), accessNode.getProperty().getFinish()));
            } else {
                Identifier name = ModelElementFactory.create(parserResult, (IdentNode)accessNode.getProperty());
                if (name != null) {
                    if (pathSize > 1 && getPath().get(pathSize - 2) instanceof CallNode) {
                        CallNode cNode = (CallNode)getPath().get(pathSize - 2);
                        if (!cNode.getArgs().contains(accessNode)) {
                            property = ModelElementFactory.createVirtualFunction(parserResult, fromAN, name, cNode.getArgs().size());
                            //property.addOccurrence(name.getOffsetRange());
                        } else {
                            property = new JsObjectImpl(fromAN, name, name.getOffsetRange(), onLeftSite);
                            property.addOccurrence(name.getOffsetRange());
                        }
                    } else {
                        property = new JsObjectImpl(fromAN, name, name.getOffsetRange(), onLeftSite);
                        property.addOccurrence(name.getOffsetRange());
                    }
                    fromAN.addProperty(name.getName(), property);
                }
            }
            if(property != null) {
                fromAN = property;
            }
        }
        if (!(getPath().get(getPath().size() - 1) instanceof AccessNode)) {
            fromAN = null;
        }
        return super.leave(accessNode);
    }

    @Override
    public Node enter(BinaryNode binaryNode) {
        if (binaryNode.tokenType() == TokenType.ASSIGN
                && !(binaryNode.rhs() instanceof ReferenceNode || binaryNode.rhs() instanceof ObjectNode)
                && (binaryNode.lhs() instanceof AccessNode || binaryNode.lhs() instanceof IdentNode)) {
            // TODO probably not only assign
            JsObjectImpl parent = modelBuilder.getCurrentDeclarationFunction();
            if (binaryNode.lhs() instanceof AccessNode) {
                AccessNode aNode = (AccessNode)binaryNode.lhs();
                JsObjectImpl property = null;
                if (aNode.getBase() instanceof IdentNode && "this".equals(((IdentNode)aNode.getBase()).getName())) { //NOI18N
                    // a usage of field
                    String fieldName = aNode.getProperty().getName();
                    if(!ModelUtils.isGlobal(parent) && !ModelUtils.isGlobal(parent.getParent()) &&
                        (parent.getParent() instanceof JsFunctionImpl
                            || isInPropertyNode() 
                            || parent instanceof JsFunctionImpl)) {
                        if (isInPropertyNode() || !(parent.getParent() instanceof JsFunction)
                                || (parent instanceof JsFunctionImpl && !parent.getModifiers().contains(Modifier.PRIVATE))) {
                            parent = (JsObjectImpl)parent.getParent();
                        }
                        if ("prototype".equals(parent.getName())) {
                            parent = (JsObjectImpl)parent.getParent();
                        }
                    }
                    property = (JsObjectImpl)parent.getProperty(fieldName);
                    if(property == null) {
                        Identifier identifier = ModelElementFactory.create(parserResult, (IdentNode)aNode.getProperty());
                        if (identifier != null) {
                            property = new JsObjectImpl(parent, identifier, identifier.getOffsetRange(), true);
                            parent.addProperty(fieldName, property);
                            JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
                            if (docHolder != null) {
                                property.setDocumentation(docHolder.getDocumentation(aNode));
                                property.setDeprecated(docHolder.isDeprecated(aNode));
                            }
                        }
                    }
                } else {
                    // probably a property of an object
                    List<Identifier> fqName = getName(aNode);
                    if (fqName != null) {
                        property = ModelUtils.getJsObject(modelBuilder, fqName, true);
                        if (property.getParent().getJSKind().isFunction() && !property.getModifiers().contains(Modifier.STATIC)) {
                            property.getModifiers().add(Modifier.STATIC);
                        }
                    }
                }
                if (property != null) {
                    String parameter = null;
                    if(binaryNode.rhs() instanceof IdentNode) {
                        IdentNode rhs = (IdentNode)binaryNode.rhs();
                        JsFunction function = (JsFunction)modelBuilder.getCurrentDeclarationFunction();
                        if(/*function.getProperty(rhs.getName()) == null &&*/ function.getParameter(rhs.getName()) != null) {
                            parameter = "@param;" + ModelUtils.createFQN(function) + ":" + rhs.getName();
                        }
                    }
                    Collection<TypeUsage> types; 
                    if (parameter == null) {
                            types =  ModelUtils.resolveSemiTypeOfExpression(parserResult, binaryNode.rhs());
                    } else {
                        types = new ArrayList<TypeUsage>();
                        types.add(new TypeUsageImpl(parameter, LexUtilities.getLexerOffset(parserResult, binaryNode.rhs().getStart()), false));
                    }

                    for (TypeUsage type : types) {
                        // plus 5 due to the this.
                        property.addAssignment(type, LexUtilities.getLexerOffset(parserResult, binaryNode.getStart() + 5));
                    }
                }

            } else {
                IdentNode ident = (IdentNode)binaryNode.lhs();
                final Identifier name = ModelElementFactory.create(parserResult, ident);
                if (name != null) {
                    final String newVarName = name.getName();
                    boolean hasParent = parent.getProperty(newVarName) != null ;
                    boolean hasGrandParent = parent.getJSKind() == JsElement.Kind.METHOD && parent.getParent().getProperty(newVarName) != null;
                    JsObject lhs = null;
                    if (!hasParent && !hasGrandParent && modelBuilder.getGlobal().getProperty(newVarName) == null) {
                        addOccurence(ident, true);
                    } else {
                        lhs = hasParent ? parent.getProperty(newVarName) : hasGrandParent ? parent.getParent().getProperty(newVarName) : null;
                        if (lhs != null) {
                            ((JsObjectImpl)lhs).addOccurrence(name.getOffsetRange());
                            if (binaryNode.rhs() instanceof UnaryNode && Token.descType(binaryNode.rhs().getToken()) == TokenType.NEW) {
                                // new XXXX() statement
                                modelBuilder.setCurrentObject((JsObjectImpl)lhs);
                                binaryNode.rhs().accept(this);
                                modelBuilder.reset();
                                return null;
                            }
                        } else {
                            addOccurence(ident, true);
                        }
                    }
                    JsObjectImpl jsObject = (JsObjectImpl)parent.getProperty(newVarName);
                    if (jsObject == null) {
                        // it's not a property of the parent -> try to find in different context
                        Model model = parserResult.getModel();
                        Collection<? extends JsObject> variables = model.getVariables(name.getOffsetRange().getStart());
                        for(JsObject variable : variables) {
                            if(variable.getName().equals(newVarName)) {
                                jsObject = (JsObjectImpl)variable;
                                break;
                            }
                        }
                        if (jsObject == null) {
                            // the object with the name wasn't find yet -> create in global scope
                            jsObject = new JsObjectImpl(model.getGlobalObject(), name, name.getOffsetRange(), false);
                        }
                    }

                    if (jsObject != null) {
                        Collection<TypeUsage> types = ModelUtils.resolveSemiTypeOfExpression(parserResult, binaryNode.rhs());
                        for (TypeUsage type : types) {
                            jsObject.addAssignment(type, LexUtilities.getLexerOffset(parserResult, binaryNode.lhs().getFinish()));
                        }
                        if (!(lhs != null && jsObject.getName().equals(lhs.getName()))) {
                            addOccurence(ident, true);
                        }
                    }
                }
            }
            if (binaryNode.rhs() instanceof IdentNode) {
                addOccurence((IdentNode)binaryNode.rhs(), false);
            }
        } else if(binaryNode.tokenType() != TokenType.ASSIGN
                || (binaryNode.tokenType() == TokenType.ASSIGN && binaryNode.lhs() instanceof IndexNode)) {
            if (binaryNode.lhs() instanceof IdentNode) {
                addOccurence((IdentNode)binaryNode.lhs(), true);
            }
            if (binaryNode.rhs() instanceof IdentNode) {
                addOccurence((IdentNode)binaryNode.rhs(), false);
            }
        }
        return super.enter(binaryNode);
    }

    @Override
    public Node enter(CallNode callNode) {
        if (callNode.getFunction() instanceof IdentNode) {
            IdentNode iNode = (IdentNode)callNode.getFunction();
            addOccurence(iNode, false, true, callNode.getArgs().size());
        }
        for (Node argument : callNode.getArgs()) {
            if (argument instanceof IdentNode) {
                addOccurence((IdentNode) argument, false);
            }
        }
        return super.enter(callNode);
    }

    @Override
    public Node enter(CatchNode catchNode) {
        Identifier exception = ModelElementFactory.create(parserResult, catchNode.getException());
        if (exception != null) {
            DeclarationScopeImpl inScope = modelBuilder.getCurrentDeclarationScope();
            CatchBlockImpl catchBlock  = new CatchBlockImpl(inScope, exception,
                    ModelUtils.documentOffsetRange(parserResult, catchNode.getStart(), catchNode.getFinish()));
            inScope.addDeclaredScope(catchBlock);
            modelBuilder.setCurrentObject(catchBlock);
        }
        return super.enter(catchNode);
    }

    @Override
    public Node leave(CatchNode catchNode) {
        if (!JsEmbeddingProvider.containsGeneratedIdentifier(catchNode.getException().getName())) {
            modelBuilder.reset();
        }
        return super.leave(catchNode);
    }

    
    @Override
    public Node enter(IdentNode identNode) {
        Node previousVisited = getPath().get(getPath().size() - 1);
        if(!(previousVisited instanceof AccessNode
                || previousVisited instanceof VarNode
                || previousVisited instanceof BinaryNode
                || previousVisited instanceof PropertyNode
                || previousVisited instanceof CatchNode)) {
            //boolean declared = previousVisited instanceof CatchNode;
            addOccurence(identNode, false);
        }
        return super.enter(identNode);
    }

    @Override
    public Node leave(IndexNode indexNode) {
        if (indexNode.getIndex() instanceof LiteralNode) {
            Node base = indexNode.getBase();
            JsObjectImpl parent = null;
            if (base instanceof AccessNode) {
                parent = fromAN;
            } else if (base instanceof IdentNode) {
                IdentNode iNode = (IdentNode)base;
                if (!"this".equals(iNode.getName())) {
                    Identifier parentName = ModelElementFactory.create(parserResult, iNode);
                    if (parentName != null) {
                        List<Identifier> fqName = new ArrayList<Identifier>();
                        fqName.add(parentName);
                        parent = ModelUtils.getJsObject(modelBuilder, fqName, false);
                        parent.addOccurrence(parentName.getOffsetRange());
                    }
                } else {
                    JsObject current = modelBuilder.getCurrentDeclarationFunction();
                    JsObject property = current.getProperty(iNode.getName());
                    if (property == null && current.getParent() != null && (current.getParent().getJSKind() == JsElement.Kind.CONSTRUCTOR
                            || current.getParent().getJSKind() == JsElement.Kind.OBJECT
                            || current.getParent().getJSKind() == JsElement.Kind.OBJECT_LITERAL)) {
                        Node previous = getPreviousFromPath(2);
                        // check whether is not a part of method in constructor
                        if (!(previous instanceof BinaryNode && ((BinaryNode)previous).rhs() instanceof ReferenceNode)) {
                            current = current.getParent();
                            if (current.getName().equals("prototype")) {
                                current = current.getParent();
                            }
                        }
                    }
                    fromAN = (JsObjectImpl)current;
                }
            }
            if (parent != null && indexNode.getIndex() instanceof LiteralNode) {
                LiteralNode literal = (LiteralNode)indexNode.getIndex();
                if (literal.isString()) {
                    String index = literal.getPropertyName();
                    JsObjectImpl property = (JsObjectImpl)parent.getProperty(index);
                    if (property != null) {
                        property.addOccurrence(ModelUtils.documentOffsetRange(parserResult, indexNode.getIndex().getStart(), indexNode.getIndex().getFinish()));
                    } else {
                        Identifier name = ModelElementFactory.create(parserResult, (LiteralNode)indexNode.getIndex());
                        property = new JsObjectImpl(parent, name, name.getOffsetRange());
                        parent.addProperty(name.getName(), property);
                    }
                }
            }
        }
        return super.leave(indexNode);
    }

    @Override
    public Node enter(FunctionNode functionNode) {
        addToPath(functionNode);
        List<FunctionNode> functions = new ArrayList<FunctionNode>(functionNode.getFunctions().size());
        // store all function nodes in cache
        for (FunctionNode fn : functionNode.getFunctions()) {
            functions.add(fn);
        }

        List<Identifier> name = null;
        boolean isPrivate = false;
        boolean isStatic = false;
        boolean isPrivilage = false;
        int pathSize = getPath().size();
        if (pathSize > 1 && getPath().get(pathSize - 2) instanceof ReferenceNode) {
            // is the function declared as variable or field
            //      var fn = function () {} or in object literal or this.fn = function () {}
            List<FunctionNode> siblings = functionStack.get(functionStack.size() - 1);
            siblings.remove(functionNode);

            if (pathSize > 3) {
                Node node = getPath().get(pathSize - 3);
                if (node instanceof PropertyNode) {
                    name = getName((PropertyNode)node);
                } else if (node instanceof BinaryNode) {
                    BinaryNode bNode = (BinaryNode)node;
                    if (bNode.lhs() instanceof AccessNode ) {
                        AccessNode aNode = (AccessNode)bNode.lhs();
                        if (aNode.getBase() instanceof IdentNode) {
                            IdentNode iNode = (IdentNode)aNode.getBase();
                            if ("this".equals(iNode.getName())) {
                                isPrivilage = true;
                            }
                        }
                    }
                    name = getName((BinaryNode)node);
                } else if (node instanceof VarNode) {
                   name = getName((VarNode)node);
                    // private method
                    // It can be only if it's in a function
                    isPrivate = functionStack.size() > 1;
                }
            }
        }

        if (name == null || name.isEmpty()) {
            // function is declared as
            //      function fn () {}
            name = new ArrayList<Identifier>(1);
            int start = functionNode.getIdent().getStart();
            int end = functionNode.getIdent().getFinish();
            if(end == 0) {
                end = parserResult.getSnapshot().getText().length();
            }
            name.add(new IdentifierImpl(functionNode.getIdent().getName(),
                    ModelUtils.documentOffsetRange(parserResult, start, end)));
            if (pathSize > 2 && getPath().get(pathSize - 2) instanceof FunctionNode) {
                isPrivate = true;
                //isStatic = true;
            }
        }
        functionStack.add(functions);

        JsFunctionImpl fncScope = (JsFunctionImpl)modelBuilder.getCurrentDeclarationFunction();
        if (functionNode.getKind() != FunctionNode.Kind.SCRIPT) {
            // create the function object
            DeclarationScopeImpl scope = modelBuilder.getCurrentDeclarationFunction();
            boolean isAnonymous = false;
            if (getPreviousFromPath(2) instanceof ReferenceNode) {
                Node node = getPreviousFromPath(3);
                if (node instanceof CallNode) {
                    isAnonymous = true;
                } else if (node instanceof AccessNode && getPreviousFromPath(4) instanceof CallNode) {
                    String methodName = ((AccessNode)node).getProperty().getName();
                    if ("call".equals(methodName) || "apply".equals(methodName)) {  //NOI18N
                        isAnonymous = true;
                    }
                }
            }
            fncScope = ModelElementFactory.create(parserResult, functionNode, name, modelBuilder, isAnonymous);
            if (fncScope != null) {
                Set<Modifier> modifiers = fncScope.getModifiers();
                if (isPrivate || isPrivilage) {
                    modifiers.remove(Modifier.PUBLIC);
                    if (isPrivate) {
                        modifiers.add(Modifier.PRIVATE);
                    } else {
                        modifiers.add(Modifier.PROTECTED);
                    }
                }
                if (isStatic) {
                    modifiers.add(Modifier.STATIC);
                }
                scope.addDeclaredScope(fncScope);
                // push the current function in the model builder stack
                modelBuilder.setCurrentObject((JsObjectImpl)fncScope);
            }
        }
        JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
        // create variables that are declared in the function
        // They has to be created here for tracking occurrences
        for (VarNode varNode : functionNode.getDeclarations()) {
            Identifier varName = new IdentifierImpl(varNode.getName().getName(),
                    ModelUtils.documentOffsetRange(parserResult, varNode.getName().getStart(), varNode.getName().getFinish()));
            OffsetRange range = varNode.getInit() instanceof ObjectNode ? 
                    ModelUtils.documentOffsetRange(parserResult, varNode.getName().getStart(), ((ObjectNode)varNode.getInit()).getFinish()) 
                    : varName.getOffsetRange();
            JsObjectImpl variable = new JsObjectImpl(fncScope, varName, range);
            variable.setDeclared(true);
            if (functionNode.getKind() != FunctionNode.Kind.SCRIPT) {
                // here are the variables allways private
                variable.getModifiers().remove(Modifier.PUBLIC);
                variable.getModifiers().add(Modifier.PRIVATE);
            }
            variable.addOccurrence(varName.getOffsetRange());
            modelBuilder.getCurrentObject().addProperty(varName.getName(), variable);
            if (docHolder != null) {
                variable.setDocumentation(docHolder.getDocumentation(varNode));
                variable.setDeprecated(docHolder.isDeprecated(varNode));
            }
            
        }
                
        for (FunctionNode fn : functions) {
            if (fn.getIdent().getStart() < fn.getIdent().getFinish()) {
                // go through all functions defined via reference
                String functionName = fn.getIdent().getName();
                if (!(functionName.startsWith("get ") || functionName.startsWith("set "))) {  //NOI18N
                    // don't visit setter and getters in object literal
                    fn.accept(this);
                }
            }
        }

        // go through all function statements
        for (Node node : functionNode.getStatements()) {
            node.accept(this);
        }


        if (fncScope != null) {
            // check parameters and return types of the function.
            fncScope.setDeprecated(docHolder.isDeprecated(functionNode));
            List<Type> types = docHolder.getReturnType(functionNode);
            if (types != null && !types.isEmpty()) {
                for(Type type : types) {
                    fncScope.addReturnType(new TypeUsageImpl(type.getType(), type.getOffset(), true));
                }
            }
            if (fncScope.areReturnTypesEmpty()) {
                // the function doesn't have return statement -> returns undefined
                fncScope.addReturnType(new TypeUsageImpl(Type.UNDEFINED, -1, false));
            }

            List<DocParameter> docParams = docHolder.getParameters(functionNode);
            for (DocParameter docParameter : docParams) {
                DocIdentifier paramName = docParameter.getParamName();
                if (paramName != null) {
                    String sParamName = paramName.getName();
                    if(sParamName != null && !sParamName.isEmpty()) {
                        JsObjectImpl param = (JsObjectImpl) fncScope.getParameter(sParamName);
                        if (param != null) {
                            for (Type type : docParameter.getParamTypes()) {
                                param.addAssignment(new TypeUsageImpl(type.getType(), type.getOffset(), true), param.getOffset());
                            }
                            // param occurence in the doc
                            addDocNameOccurence(param);
                        }
                    }
                }
            }

            // mark constructors
            if (functionNode.getKind() != FunctionNode.Kind.SCRIPT && docHolder.isClass(functionNode)) {
                fncScope.setJsKind(JsElement.Kind.CONSTRUCTOR);
            }
//            Set<JsModifier> modifiers = docHolder.getModifiers(functionNode);
//            for(JsModifier modifier : modifiers) {
//                switch(modifier) {
//                    case PUBLIC:
//                        fncScope.getModifiers().add(Modifier.PUBLIC);
//                        fncScope.getModifiers().remove(Modifier.PRIVATE);
//                        break;
//                    case PRIVATE:
//                        fncScope.getModifiers().add(Modifier.PRIVATE);
//                        fncScope.getModifiers().remove(Modifier.PUBLIC);
//                        break;
//                    case STATIC:
//                        fncScope.getModifiers().add(Modifier.STATIC);
//                        break;
//                }
//                
//            }
        }

        for (FunctionNode fn : functions) {
            // go through all functions defined as function fn () {...}
            if (fn.getIdent().getStart() >= fn.getIdent().getFinish()) {
                fn.accept(this);
            }
        }
        if (fncScope != null && functionNode.getKind() != FunctionNode.Kind.SCRIPT) {
            // pop the current level from model builder stack
            modelBuilder.reset();
        }
        functionStack.remove(functionStack.size() - 1);
        removeFromPathTheLast();
        return null;
    }

    @Override
    public Node enter(ObjectNode objectNode) {
        Node previousVisited = getPath().get(getPath().size() - 1);
        if(previousVisited instanceof CallNode
                || previousVisited instanceof LiteralNode.ArrayLiteralNode) {
            // TODO there should be handled anonymous object that are going as parameter to a funciton
            //create anonymous object
            JsObjectImpl object = ModelElementFactory.createAnonymousObject(parserResult, objectNode,  modelBuilder);
            modelBuilder.setCurrentObject(object);
            object.setJsKind(JsElement.Kind.OBJECT_LITERAL);
            return super.enter(objectNode);
        } else if (previousVisited instanceof ReturnNode) {
            JsObjectImpl objectScope = ModelElementFactory.createAnonymousObject(parserResult, objectNode, modelBuilder);
            modelBuilder.setCurrentObject(objectScope);
            objectScope.setJsKind(JsElement.Kind.OBJECT_LITERAL);
        } else {
            List<Identifier> fqName = null;
            int pathSize = getPath().size();
            boolean isDeclaredInParent = false;
            boolean isPrivate = false;
            boolean treatAsAnonymous = false;
            
            Node lastVisited = getPath().get(pathSize - 1);
            VarNode varNode = null;

            if ( lastVisited instanceof VarNode) {
                fqName = getName((VarNode)lastVisited);
                isDeclaredInParent = true;
                JsObject declarationScope = modelBuilder.getCurrentDeclarationFunction();
                varNode = (VarNode)lastVisited;
                if (fqName.size() == 1 && !ModelUtils.isGlobal(declarationScope)) {
                    isPrivate = true;
                }
            } else if (lastVisited instanceof PropertyNode) {
                fqName = getName((PropertyNode) lastVisited);
                isDeclaredInParent = true;
            } else if (lastVisited instanceof BinaryNode) {
                BinaryNode binNode = (BinaryNode) lastVisited;
                if (binNode.lhs() instanceof IndexNode) {
                    Node index =  ((IndexNode)binNode.lhs()).getIndex();
                    if (!(index instanceof LiteralNode && ((LiteralNode)index).isString())) {
                        treatAsAnonymous = true;
                    }
                } 
                if (!treatAsAnonymous) {
                    if (getPath().size() > 1) {
                        lastVisited = getPath().get(getPath().size() - 2);
                        if (lastVisited instanceof VarNode) {
                            varNode = (VarNode) lastVisited;
                        }
                    }
                    fqName = getName(binNode);
                    if (binNode.lhs() instanceof IdentNode || (binNode.lhs() instanceof AccessNode
                            && ((AccessNode) binNode.lhs()).getBase() instanceof IdentNode
                            && ((IdentNode) ((AccessNode) binNode.lhs()).getBase()).getName().equals("this"))) {
                        isDeclaredInParent = true;
                    }
                }
            }
            if (!treatAsAnonymous) {
                if (fqName == null || fqName.isEmpty()) {
                    fqName = new ArrayList<Identifier>(1);
                    fqName.add(new IdentifierImpl("UNKNOWN", //NOI18N
                            ModelUtils.documentOffsetRange(parserResult, objectNode.getStart(), objectNode.getFinish())));
                }
                JsObjectImpl objectScope;
                if (varNode != null) {
                    objectScope = modelBuilder.getCurrentObject();
                } else {
                    JsObject alreadyThere = ModelUtils.getJsObjectByName(modelBuilder.getCurrentDeclarationFunction(), fqName.get(fqName.size() - 1).getName());
                    objectScope = ModelElementFactory.create(parserResult, objectNode, fqName, modelBuilder, isDeclaredInParent);
                    if(alreadyThere != null && objectScope != null) {
                        for(Occurrence occurrence :alreadyThere.getOccurrences()) {
                            objectScope.addOccurrence(occurrence.getOffsetRange());
                        }
                    }
                }
                if (objectScope != null) {
                    objectScope.setJsKind(JsElement.Kind.OBJECT_LITERAL);
                    modelBuilder.setCurrentObject(objectScope);
                    if (isPrivate) {
                        objectScope.getModifiers().remove(Modifier.PUBLIC);
                        objectScope.getModifiers().add(Modifier.PRIVATE);
                    }
                }
            } else {
                JsObjectImpl objectScope = ModelElementFactory.createAnonymousObject(parserResult, objectNode, modelBuilder);
                modelBuilder.setCurrentObject(objectScope);
            }
            
        }

        return super.enter(objectNode);
    }

    @Override
    public Node leave(ObjectNode objectNode) {
        modelBuilder.reset();
        return super.leave(objectNode);
    }

    @Override
    public Node enter(PropertyNode propertyNode) {
        if ((propertyNode.getKey() instanceof IdentNode || propertyNode.getKey() instanceof LiteralNode)
                && !(propertyNode.getValue() instanceof ObjectNode)) {
            JsObjectImpl scope = modelBuilder.getCurrentObject();
            Identifier name = null;
            Node key = propertyNode.getKey();
            if (key instanceof IdentNode) {
                name = ModelElementFactory.create(parserResult, (IdentNode)key);
            } else if (key instanceof LiteralNode) {
                name = ModelElementFactory.create(parserResult, (LiteralNode)key);
            }

            if (name != null) {
                JsObjectImpl property = (JsObjectImpl)scope.getProperty(name.getName());
                if (property == null) {
                    property = ModelElementFactory.create(parserResult, propertyNode, name, modelBuilder, true);
                } else {
                    // The property can be already defined, via a usage before declaration (see testfiles/model/simpleObject.js - called property)
                    JsObjectImpl newProperty = ModelElementFactory.create(parserResult, propertyNode, name, modelBuilder, true);
                    if (newProperty != null) {
                        newProperty.addOccurrence(property.getDeclarationName().getOffsetRange());
                        for(Occurrence occurrence : property.getOccurrences()) {
                            newProperty.addOccurrence(occurrence.getOffsetRange());
                        }
                        property = newProperty;
                    }
                }

                if (property != null) {
                    if (propertyNode.getGetter() != null) {
                        FunctionNode getter = ((FunctionNode)((ReferenceNode)propertyNode.getGetter()).getReference());
                        property.addOccurrence(ModelUtils.documentOffsetRange(parserResult, getter.getIdent().getStart(), getter.getIdent().getFinish()));
                    }

                    if (propertyNode.getSetter() != null) {
                        FunctionNode setter = ((FunctionNode)((ReferenceNode)propertyNode.getSetter()).getReference());
                        property.addOccurrence(ModelUtils.documentOffsetRange(parserResult, setter.getIdent().getStart(), setter.getIdent().getFinish()));
                    }
                    scope.addProperty(name.getName(), property);
                    property.setDeclared(true);
                    Node value = propertyNode.getValue();
                    if(value instanceof CallNode) {
                        // TODO for now, don't continue. There shoudl be handled cases liek
                        // in the testFiles/model/property02.js file
                        //return null;
                    } else {
                        Collection<TypeUsage> types = ModelUtils.resolveSemiTypeOfExpression(parserResult, value);
                        if (!types.isEmpty()) {
                            property.addAssignment(types, LexUtilities.getLexerOffset(parserResult, name.getOffsetRange().getStart()));
                        }
                        if (value instanceof IdentNode) {
                            IdentNode iNode = (IdentNode)value;
                            JsFunction function = (JsFunction)ModelUtils.getDeclarationScope(property);
                            String iName = iNode.getName();
                            JsObjectImpl param = (JsObjectImpl)function.getParameter(iName);
                            if(param != null) {
                                param.addOccurrence(ModelUtils.documentOffsetRange(parserResult, LexUtilities.getLexerOffset(parserResult, iNode.getStart()), iNode.getFinish()));
                            } else {
                                Collection<? extends JsObject> variables = ModelUtils.getVariables((DeclarationScope)function);
                                for (JsObject variable : variables) {
                                    if (iName.equals(variable.getName())) {
                                        ((JsObjectImpl)variable).addOccurrence(ModelUtils.documentOffsetRange(parserResult, LexUtilities.getLexerOffset(parserResult, iNode.getStart()), iNode.getFinish()));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return super.enter(propertyNode);
    }

    @Override
    public Node enter(ReferenceNode referenceNode) {
        if (referenceNode.getReference() instanceof FunctionNode) {
            addToPath(referenceNode);
            ((FunctionNode) referenceNode.getReference()).accept(this);
            removeFromPathTheLast();
            return null;
        }
        return super.enter(referenceNode);
    }

    @Override
    public Node enter(ReturnNode returnNode) {
        Node expression = returnNode.getExpression();
        if (expression instanceof IdentNode) {
            addOccurence((IdentNode)expression, false);
        }
        Collection<TypeUsage> types = ModelUtils.resolveSemiTypeOfExpression(parserResult, expression);
        if(types.isEmpty()) {
           types.add(new TypeUsageImpl(Type.UNRESOLVED, LexUtilities.getLexerOffset(parserResult, returnNode.getStart()), true));
        }
        JsFunctionImpl function = modelBuilder.getCurrentDeclarationFunction();
        function.addReturnType(types);
        return super.enter(returnNode);
    }

    @Override
    public Node enter(TernaryNode ternaryNode) {
        if (ternaryNode.lhs() instanceof IdentNode) {
            addOccurence((IdentNode)ternaryNode.lhs(), false);
        }
        if (ternaryNode.rhs() instanceof IdentNode) {
            addOccurence((IdentNode)ternaryNode.rhs(), false);
        }
        if (ternaryNode.third() instanceof IdentNode) {
            addOccurence((IdentNode)ternaryNode.third(), false);
        }
        return super.enter(ternaryNode);
    }

    @Override
    public Node enter(UnaryNode unaryNode) {
        if (Token.descType(unaryNode.getToken()) == TokenType.NEW) {
            Node lastNode = getPath().get(getPath().size() -1);
            if (unaryNode.rhs() instanceof CallNode
                    && ((CallNode)unaryNode.rhs()).getFunction() instanceof IdentNode
                    && !(lastNode instanceof PropertyNode) && !inVarNode) {
                if (modelBuilder.getCurrentObject() != null) {
                    int start = LexUtilities.getLexerOffset(parserResult, unaryNode.getStart());
                    if (getPath().get(getPath().size() - 1) instanceof VarNode) {
                        start = LexUtilities.getLexerOffset(parserResult, ((VarNode)getPath().get(getPath().size() - 1)).getName().getFinish());
                    }
                    Collection<TypeUsage> types = ModelUtils.resolveSemiTypeOfExpression(parserResult, unaryNode);
                    for (TypeUsage type : types) {
                        modelBuilder.getCurrentObject().addAssignment(type, start);
                    }
                }
            }
        } else {
            if (unaryNode.rhs() instanceof IdentNode) {
                addOccurence((IdentNode)unaryNode.rhs(), false);
            }
        }
        return super.enter(unaryNode);
    }

    @Override
    public Node enter(VarNode varNode) {
         if (!(varNode.getInit() instanceof ObjectNode || varNode.getInit() instanceof ReferenceNode)) {
            JsObject parent = modelBuilder.getCurrentObject();
            if (parent instanceof CatchBlockImpl) {
                parent = parent.getParent();
            } 
            JsObjectImpl variable = (JsObjectImpl)parent.getProperty(varNode.getName().getName());
            Identifier name = ModelElementFactory.create(parserResult, varNode.getName());
            if (name != null) {
                if (variable == null) {
                    // variable si not defined, so it has to be from global scope
                    // or from a code structure like for cycle

                    variable = new JsObjectImpl(parent, name, name.getOffsetRange(), true);
                    if (parent.getJSKind() != JsElement.Kind.FILE) {
                        variable.getModifiers().remove(Modifier.PUBLIC);
                        variable.getModifiers().add(Modifier.PRIVATE);
                    }
                    parent.addProperty(name.getName(), variable);
                    variable.addOccurrence(name.getOffsetRange());
                } else if (!variable.isDeclared()){
                    // the variable was probably created as temporary before, now we
                    // need to replace it with the real one
                    JsObjectImpl newVariable = new JsObjectImpl(parent, name, name.getOffsetRange(), true);
                    newVariable.addOccurrence(name.getOffsetRange());
                    for(String propertyName: variable.getProperties().keySet()) {
                        JsObject property = variable.getProperty(propertyName);
                        if (property instanceof JsObjectImpl) {
                            ((JsObjectImpl)property).setParent(newVariable);
                        }
                        newVariable.addProperty(propertyName, property);
                    }
                    if (parent.getJSKind() != JsElement.Kind.FILE) {
                        newVariable.getModifiers().remove(Modifier.PUBLIC);
                        newVariable.getModifiers().add(Modifier.PRIVATE);
                    }
                    for(TypeUsage type : variable.getAssignments()) {
                        newVariable.addAssignment(type, type.getOffset());
                    }
                    for(Occurrence occurrence: variable.getOccurrences()){
                        newVariable.addOccurrence(occurrence.getOffsetRange());
                    }
                    parent.addProperty(name.getName(), newVariable);
                    variable = newVariable;
                }
                JsDocumentationHolder docHolder = parserResult.getDocumentationHolder();
                variable.setDeprecated(docHolder.isDeprecated(varNode));
                variable.setDocumentation(docHolder.getDocumentation(varNode));
                modelBuilder.setCurrentObject(variable);
                if (varNode.getInit() instanceof IdentNode) {
                    addOccurence((IdentNode)varNode.getInit(), false);
                }
                if (!(varNode.getInit() instanceof UnaryNode &&
                        Token.descType(((UnaryNode)varNode.getInit()).getToken()) == TokenType.NEW)) {
                    inVarNode = true;
                    Collection<TypeUsage> types = ModelUtils.resolveSemiTypeOfExpression(parserResult, varNode.getInit());
                    for (TypeUsage type : types) {
                        variable.addAssignment(type, LexUtilities.getLexerOffset(parserResult, varNode.getName().getFinish()));
                    }
                }
                List<Type> returnTypes = docHolder.getReturnType(varNode);
                if (returnTypes != null && !returnTypes.isEmpty()) {
                    for (Type type : returnTypes) {
                        variable.addAssignment(new TypeUsageImpl(type.getType(), type.getOffset(), true), varNode.getName().getFinish());
                    }
                }
            }
        } else if(varNode.getInit() instanceof ObjectNode) {
            JsObjectImpl function = modelBuilder.getCurrentDeclarationFunction();
            Identifier name = ModelElementFactory.create(parserResult, varNode.getName());
            if (name != null) {
                JsObjectImpl variable = (JsObjectImpl)function.getProperty(name.getName());
                if (variable != null) {
                    modelBuilder.setCurrentObject(variable);
                    variable.setDeclared(true);
                    variable.setJsKind(JsElement.Kind.OBJECT_LITERAL);
                }
            }
        }
        return super.enter(varNode);
    }

    @Override
    public Node leave(VarNode varNode) {
        if (!(varNode.getInit() instanceof ReferenceNode)
                // XXX can we avoid creation of object ?
                && ModelElementFactory.create(parserResult, varNode.getName()) != null) {
            modelBuilder.reset();
        }
        inVarNode = false;
        return super.leave(varNode);
    }

//--------------------------------End of visit methods--------------------------------------

    private List<Identifier> getName(PropertyNode propertyNode) {
        List<Identifier> name = new ArrayList(1);
        if (propertyNode.getGetter() != null || propertyNode.getSetter() != null) {
            // check whether this is not defining getter or setter of a property.
            Node previousNode = getPreviousFromPath(1);
            if (previousNode instanceof FunctionNode) {
                FunctionNode fNode = (FunctionNode)previousNode;
                String fName = fNode.getIdent().getName();
                if (fName.startsWith("get ") || fName.startsWith("set ")) { //NOI18N
                    name.add(new IdentifierImpl(fName,
                        ModelUtils.documentOffsetRange(parserResult, fNode.getIdent().getStart(), fNode.getIdent().getFinish())));
                    return name;
                }
            }
        }
        if (propertyNode.getKey() instanceof IdentNode) {
            IdentNode ident = (IdentNode) propertyNode.getKey();
            name.add(new IdentifierImpl(ident.getName(),
                    ModelUtils.documentOffsetRange(parserResult, ident.getStart(), ident.getFinish())));
        } else if (propertyNode.getKey() instanceof LiteralNode){
            LiteralNode lNode = (LiteralNode)propertyNode.getKey();
            name.add(new IdentifierImpl(lNode.getString(),
                    ModelUtils.documentOffsetRange(parserResult, lNode.getStart(), lNode.getFinish())));
        }
        return name;
    }

    private List<Identifier> getName(VarNode varNode) {
        List<Identifier> name = new ArrayList();
        name.add(new IdentifierImpl(varNode.getName().getName(),
                ModelUtils.documentOffsetRange(parserResult, varNode.getName().getStart(), varNode.getName().getFinish())));
        return name;
    }

    private List<Identifier> getName(BinaryNode binaryNode) {
        List<Identifier> name = new ArrayList();
        Node lhs = binaryNode.lhs();
        if (lhs instanceof AccessNode) {
            name = getName((AccessNode)lhs);
        } else if (lhs instanceof IdentNode) {
            IdentNode ident = (IdentNode) lhs;
            name.add(new IdentifierImpl(ident.getName(),
                        ModelUtils.documentOffsetRange(parserResult, ident.getStart(), ident.getFinish())));
        } else if (lhs instanceof IndexNode) {
            IndexNode indexNode = (IndexNode)lhs;
            if (indexNode.getBase() instanceof AccessNode) {
                List<Identifier> aName = getName((AccessNode)indexNode.getBase());
                if (aName != null) {
                    name.addAll(getName((AccessNode)indexNode.getBase()));
                }
                else {
                    return null;
                }
            }
            if (indexNode.getIndex() instanceof LiteralNode) {
                LiteralNode lNode = (LiteralNode)indexNode.getIndex();
                name.add(new IdentifierImpl(lNode.getPropertyName(), 
                        ModelUtils.documentOffsetRange(parserResult, lNode.getStart(), lNode.getFinish())));
            }
        }
        return name;
    }

    private List<Identifier> getName(AccessNode aNode) {
        List<Identifier> name = new ArrayList();
        name.add(new IdentifierImpl(aNode.getProperty().getName(),
                ModelUtils.documentOffsetRange(parserResult, aNode.getProperty().getStart(), aNode.getProperty().getFinish())));
        while (aNode.getBase() instanceof AccessNode) {
            aNode = (AccessNode) aNode.getBase();
            name.add(new IdentifierImpl(aNode.getProperty().getName(),
                    ModelUtils.documentOffsetRange(parserResult, aNode.getProperty().getStart(), aNode.getProperty().getFinish())));
        }
        if (aNode.getBase() instanceof IdentNode) {
            if (name.size() > 0 && aNode.getBase() instanceof IdentNode) {
                IdentNode ident = (IdentNode) aNode.getBase();
                if (!"this".equals(ident.getName())) {
                    name.add(new IdentifierImpl(ident.getName(),
                            ModelUtils.documentOffsetRange(parserResult, ident.getStart(), ident.getFinish())));
                }
            }
            Collections.reverse(name);
            return name;
        } else {
            return null;
        }
    }

    /**
     * Gets the node name if it has any (case of AccessNode, BinaryNode, VarNode, PropertyNode).
     *
     * @param node examined node for getting its name
     * @return name of the node if it supports it
     */
    public List<Identifier> getNodeName(Node node) {
        if (node instanceof AccessNode) {
            return getName((AccessNode) node);
        } else if (node instanceof BinaryNode) {
            return getName((BinaryNode) node);
        } else if (node instanceof VarNode) {
            return getName((VarNode) node);
        } else if (node instanceof PropertyNode) {
            return getName((PropertyNode) node);
        } else if (node instanceof FunctionNode) {
            if (((FunctionNode) node).getKind() == FunctionNode.Kind.SCRIPT) {
                return Collections.<Identifier>emptyList();
            }
            IdentNode ident = ((FunctionNode) node).getIdent();
            return Arrays.<Identifier>asList(new IdentifierImpl(
                    ident.getName(),
                    ModelUtils.documentOffsetRange(parserResult, ident.getStart(), ident.getFinish())));
        } else {
            return Collections.<Identifier>emptyList();
        }
    }

//    private Variable findVarWithName(final Scope scope, final String name) {
//        Variable result = null;
//        Collection<Variable> variables = ScopeImpl.filter(scope.getElements(), new ScopeImpl.ElementFilter() {
//
//            @Override
//            public boolean isAccepted(ModelElement element) {
//                return element.getJSKind().equals(JsElement.Kind.VARIABLE)
//                        && element.getName().equals(name);
//            }
//        });
//
//        if (!variables.isEmpty()) {
//            result = variables.iterator().next();
//        } else {
//            if (!(scope instanceof FileScope)) {
//                result = findVarWithName((Scope)scope.getInElement(), name);
//            }
//        }
//
//        return result;
//    }
//
//    private Field findFieldWithName(FunctionScope function, final String name) {
//        Field result = null;
//        Collection<? extends Field> fields = function.getFields();
//        result = ModelUtils.getFirst(ModelUtils.getFirst(fields, name));
//        if (result == null && function.getInElement() instanceof FunctionScope) {
//            FunctionScope parent = (FunctionScope)function.getInElement();
//            fields = parent.getFields();
//            result = ModelUtils.getFirst(ModelUtils.getFirst(fields, name));
//        }
//        return result;
//    }

    private boolean isInPropertyNode() {
        boolean inFunction = false;
        for (int i = getPath().size() - 1; i > 0 ; i--) {
            final Node node = getPath().get(i);
            if(node instanceof FunctionNode) {
                if (!inFunction) {
                    inFunction = true;
                } else {
                    return false;
                }
            } else if (node instanceof PropertyNode) {
                return true;
            }
        }
        return false;
    }

    private void addOccurence(IdentNode iNode, boolean leftSite) {
        addOccurence(iNode, leftSite, false, 0);
    }

    private void addOccurence(IdentNode iNode, boolean leftSite, boolean isFunction, int countParam) {
        if ("this".equals(iNode.getName())) {
            // don't process this node.
            return;
        }
        DeclarationScope scope = modelBuilder.getCurrentDeclarationScope();
        JsObject property = null;
        JsObject parameter = null;
        while (scope != null && property == null && parameter == null) {
            JsFunction function = (JsFunction)scope;
            property = function.getProperty(iNode.getName());
            parameter = function.getParameter(iNode.getName());
            scope = scope.getInScope();
        }
        if(parameter != null) {
            if (property == null) {
                property = parameter;
            } else {
                if(property.getJSKind() != JsElement.Kind.VARIABLE) {
                    property = parameter;
                }
            }
        }

        if (property != null) {

            // occurence in the doc
            addDocNameOccurence(((JsObjectImpl)property));
            addDocTypesOccurence(((JsObjectImpl)property));

            ((JsObjectImpl)property).addOccurrence(ModelUtils.documentOffsetRange(parserResult, iNode.getStart(), iNode.getFinish()));
        } else {
            // it's a new global variable?
            IdentifierImpl name = ModelElementFactory.create(parserResult, iNode);
            if (name != null) {
                JsObjectImpl newObject;
                if (!isFunction) {
                    newObject = new JsObjectImpl(modelBuilder.getGlobal(), name, name.getOffsetRange(), leftSite);
                } else {
                    FileObject fo = parserResult.getSnapshot().getSource().getFileObject();
                    newObject = new JsFunctionImpl(fo, modelBuilder.getGlobal(), name, Collections.EMPTY_LIST);
                }
                newObject.addOccurrence(name.getOffsetRange());
                modelBuilder.getGlobal().addProperty(name.getName(), newObject);
            }
        }
    }
    
    private void addDocNameOccurence(JsObjectImpl jsObject) {
        JsDocumentationHolder holder = parserResult.getDocumentationHolder();
        JsComment comment = holder.getCommentForOffset(jsObject.getOffset(), holder.getCommentBlocks());
        if (comment != null) {
            for (DocParameter docParameter : comment.getParameters()) {
                DocIdentifier paramName = docParameter.getParamName();
                String name = (docParameter.getParamName() == null) ? "" : docParameter.getParamName().getName(); //NOI18N
                if (name.equals(jsObject.getName())) {
                    jsObject.addOccurrence(DocumentationUtils.getOffsetRange(paramName));
                }
            }
        }
    }

    private void addDocTypesOccurence(JsObjectImpl jsObject) {
        JsDocumentationHolder holder = parserResult.getDocumentationHolder();
        if (holder.getOccurencesMap().containsKey(jsObject.getName())) {
            for (OffsetRange offsetRange : holder.getOccurencesMap().get(jsObject.getName())) {
                ((JsObjectImpl)jsObject).addOccurrence(offsetRange);
            }
        }
    }



    private Node getPreviousFromPath(int back) {
        int size = getPath().size();
        if (size >= back) {
            return getPath().get(size - back);
        }
        return null;
    }    
}