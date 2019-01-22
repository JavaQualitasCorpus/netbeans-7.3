/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.cnd.modelimpl.csm;

import org.netbeans.modules.cnd.antlr.collections.AST;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmObject;
import org.netbeans.modules.cnd.api.model.util.CsmKindUtilities;
import org.netbeans.modules.cnd.api.model.xref.CsmReference;
import org.netbeans.modules.cnd.api.model.xref.CsmReferenceKind;
import org.netbeans.modules.cnd.apt.utils.APTUtils;
import org.netbeans.modules.cnd.modelimpl.content.file.FileContent;
import org.netbeans.modules.cnd.modelimpl.csm.core.AstUtil;
import org.netbeans.modules.cnd.modelimpl.csm.core.OffsetableBase;
import org.netbeans.modules.cnd.modelimpl.parser.CsmAST;
import org.netbeans.modules.cnd.modelimpl.parser.generated.CPPTokenTypes;
import org.openide.util.CharSequences;


/**
 *
 * @author Alexander Simon
 */
public class NameHolder {
    private final CharSequence name;
    private int start = 0;
    private int end = 0;
    private boolean isMacroExpanded = false;
    private static final int FUNCTION = 0;
    private static final int DESTRUCTOR = 1;
    private static final int DESTRUCTOR_DEFINITION = 2;
    private static final int CLASS = 3;
    private static final int ENUM = 4;

    private NameHolder(AST ast, int kind) {
        switch (kind) {
            case DESTRUCTOR:
                name = "~"+findFunctionName(ast); // NOI18N
                break;
            case DESTRUCTOR_DEFINITION:
                name =findDestructorDefinitionName(ast);
                break;
            case CLASS:
                name =findClassName(ast);
                break;
            case ENUM:
                name =findEnumName(ast);
                break;
            case FUNCTION:
            default:
                name = findFunctionName(ast);
        }
    }

    private NameHolder(CharSequence name) {
        this.name = name;
    }

    public static NameHolder createName(CharSequence name) {
        return new NameHolder(name);
    }

    public static NameHolder createName(CharSequence name, int startOffset, int endOffset) {
        NameHolder nameHolder = new NameHolder(name);
        nameHolder.start = startOffset;
        nameHolder.end = endOffset;
        return nameHolder;
    }

    public static NameHolder createName(CharSequence name, int startOffset, int endOffset, boolean isMacroExpanded) {        
        NameHolder nameHolder = new NameHolder(name);
        nameHolder.start = startOffset;
        nameHolder.end = endOffset;
        nameHolder.isMacroExpanded = isMacroExpanded;
        return nameHolder;
    }
    
    public static NameHolder createFunctionName(AST ast) {
        return new NameHolder(ast, FUNCTION);
    }

    public static NameHolder createDestructorName(AST ast) {
        return new NameHolder(ast, DESTRUCTOR);
    }

    public static NameHolder createDestructorDefinitionName(AST ast) {
        return new NameHolder(ast, DESTRUCTOR_DEFINITION);
    }

    public static NameHolder createClassName(AST ast) {
        return new NameHolder(ast, CLASS);
    }

    public static NameHolder createEnumName(AST ast) {
        return new NameHolder(ast, ENUM);
    }

    public static NameHolder createSimpleName(AST ast) {
        NameHolder nameHolder = new NameHolder(AstUtil.getText(ast));
        nameHolder.start = OffsetableBase.getStartOffset(ast);
        nameHolder.isMacroExpanded = isMacroExpandedToken(ast);
        nameHolder.end = OffsetableBase.getEndOffset(ast);
        return nameHolder;
    }

    public CharSequence getName(){
        if (name == null || name.length() == 0) {
            return CharSequences.empty();
        }
        return name;
    }

    public int getStartOffset(){
        return start;
    }

    public int getEndOffset(){
        return end;
    }

    @Override
    public String toString() {
        return "name=" + name + ", start=" + start + ", end=" + end + ", isMacroExpanded=" + isMacroExpanded; // NOI18N
    }

    public void addReference(final FileContent fileContent, final CsmObject decl) {        
        if (fileContent != null) {
            if (start > 0 && !isMacroExpanded) {
                final CsmReferenceKind kind;
                if (CsmKindUtilities.isFunctionDefinition(decl) ||
                        CsmKindUtilities.isVariableDefinition(decl)) {
                    kind = CsmReferenceKind.DEFINITION;
                } else {
                    kind = CsmReferenceKind.DECLARATION;
                }
                CsmReference ref = new CsmReference() {

                    @Override
                    public CsmReferenceKind getKind() {
                        return kind;
                    }

                    @Override
                    public CsmObject getReferencedObject() {
                        return decl;
                    }

                    @Override
                    public CsmObject getOwner() {
                        return decl;
                    }

                    @Override
                    public CsmFile getContainingFile() {
                        return fileContent.getFile();
                    }

                    @Override
                    public int getStartOffset() {
                        return start;
                    }

                    @Override
                    public int getEndOffset() {
                        return end;
                    }

                    @Override
                    public Position getStartPosition() {
                        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
                    }

                    @Override
                    public Position getEndPosition() {
                        throw new UnsupportedOperationException("Not supported yet."); // NOI18N
                    }

                    @Override
                    public CharSequence getText() {
                        return name;
                    }

                    @Override
                    public CsmObject getClosestTopLevelObject() {
                        return decl;
                    }
                };
                fileContent.addReference(ref, decl);
            }
        }
    }

    private static boolean isMacroExpandedToken(AST ast) {
        if (ast instanceof CsmAST) {
            return APTUtils.isMacroExpandedToken(((CsmAST)ast).getToken());
        }
        return false;
    }

    private CharSequence findDestructorDefinitionName(AST node) {
        AST token = node.getFirstChild();
        if (token != null) {
            token = AstUtil.findSiblingOfType(token, CPPTokenTypes.CSM_QUALIFIED_ID);
        }
        if (token != null) {
            token = AstUtil.findChildOfType(token, CPPTokenTypes.TILDE);
            if (token != null) {
                AST startNameToken = token.getNextSibling();
                if (startNameToken == null) {
                    startNameToken = token;
                }
                start = OffsetableBase.getStartOffset(startNameToken);
                isMacroExpanded = isMacroExpandedToken(token);
                end = OffsetableBase.getEndOffset(token);
                token = token.getNextSibling();
                //QualitasCorpus.class: Removed due to compilation errors
                //if (token != null && token.getType() == CPPTokenTypes.IDENT) {
                if (true) {
                    end = OffsetableBase.getEndOffset(token);
                    return "~" + token.getText(); // NOI18N
                }
            }
        }
        return "~"; // NOI18N
    }

    private CharSequence findFunctionName(AST ast) {
        if( CastUtils.isCast(ast) ) {
            return getFunctionName(ast);
        }
        AST token = AstUtil.findMethodName(ast);
        if (token != null){
            return extractName(token);
        }
        return ""; // NOI18N
    }

    private String getFunctionName(AST ast) {
	AST operator = AstUtil.findChildOfType(ast, CPPTokenTypes.LITERAL_OPERATOR);
	if( operator == null ) {
            // error in AST
	    return "operator ???"; // NOI18N
	}
        start = OffsetableBase.getStartOffset(operator);
        isMacroExpanded = isMacroExpandedToken(operator);
        end = OffsetableBase.getEndOffset(operator);
	StringBuilder sb = new StringBuilder(operator.getText());
	sb.append(' ');
	begin:
	for( AST next = operator.getNextSibling(); next != null; next = next.getNextSibling() ) {
	    switch( next.getType() ) {
		case CPPTokenTypes.CSM_TYPE_BUILTIN:
		case CPPTokenTypes.CSM_TYPE_COMPOUND:
		    sb.append(' ');
		    addTypeText(next, sb);
                    break;
		case CPPTokenTypes.CSM_PTR_OPERATOR:
		    addTypeText(next, sb);
                    break;
		case CPPTokenTypes.LPAREN:
		    break begin;
		case CPPTokenTypes.AMPERSAND:
		case CPPTokenTypes.STAR:
		case CPPTokenTypes.LITERAL_const:
                case CPPTokenTypes.LITERAL___const:
                case CPPTokenTypes.LITERAL___const__:
                    end = OffsetableBase.getEndOffset(next);
		    sb.append(next.getText());
		    break;
		default:
		    sb.append(' ');
                    end = OffsetableBase.getEndOffset(next);
		    sb.append(next.getText());
	    }
	}
	return sb.toString();
    }

    private void addTypeText(AST ast, StringBuilder sb) {
	if( ast == null ) {
	    return;
	}
	for( AST child = ast.getFirstChild(); child != null; child = child.getNextSibling() ) {
	    if( CPPTokenTypes.CSM_START <= child.getType() && child.getType() <= CPPTokenTypes.CSM_END ) {
		addTypeText(child, sb);
	    }
	    else {
                end = OffsetableBase.getEndOffset(child);
		String text = child.getText();
		assert text != null;
		assert text.length() > 0;
		if( sb.length() > 0 ) {
		    if( Character.isLetterOrDigit(sb.charAt(sb.length() - 1)) ) {
			if( Character.isLetterOrDigit(text.charAt(0)) ) {
			    sb.append(' ');
			}
		    }
		}
		sb.append(text);
	    }
	}
    }

    private CharSequence extractName(AST token){
        int type = token.getType();
        //QualitasCorpus.class: Removed due to compilation errors
        //if( type == CPPTokenTypes.IDENT ) {
        if( true ) {
            start = OffsetableBase.getStartOffset(token);
            isMacroExpanded = isMacroExpandedToken(token);
            end = OffsetableBase.getEndOffset(token);
            return AstUtil.getText(token);
        } else if( type == CPPTokenTypes.CSM_QUALIFIED_ID ) {
            AST last = AstUtil.getLastChild(token);
            if( last != null) {
                if (last.getType() == CPPTokenTypes.GREATERTHAN) {
                    AST lastId = null;
                    int level = 0;
                    for (AST token2 = token.getFirstChild(); token2 != null; token2 = token2.getNextSibling()) {
                        int type2 = token2.getType();
                        switch (type2) {
                        	//QualitasCorpus.class: Removed due to compilation errors
                            //case CPPTokenTypes.IDENT:
                            case 1:
                                lastId = token2;
                                break;
                            case CPPTokenTypes.GREATERTHAN:
                                level--;
                                break;
                            case CPPTokenTypes.LESSTHAN:
                                level++;
                                break;
                            default:
                                if (level == 0) {
                                    lastId = null;
                                }
                        }
                    }
                    if (lastId != null) {
                        last = lastId;
                    }
                }
              //QualitasCorpus.class: Removed due to compilation errors
                //if( last.getType() == CPPTokenTypes.IDENT ) {
                if( true ) {
                    start = OffsetableBase.getStartOffset(last);
                    isMacroExpanded = isMacroExpandedToken(last);
                    end = OffsetableBase.getEndOffset(last);
                    return AstUtil.getText(last);
                } else {
//		    if( first.getType() == CPPTokenTypes.LITERAL_OPERATOR ) {
                    AST operator = AstUtil.findChildOfType(token, CPPTokenTypes.LITERAL_OPERATOR);
                    if( operator != null ) {
                        start = OffsetableBase.getStartOffset(operator);
                        isMacroExpanded = isMacroExpandedToken(operator);
                        end = OffsetableBase.getEndOffset(operator);
                        StringBuilder sb = new StringBuilder(operator.getText());
                        sb.append(' ');
                        boolean first = true;
                        for( AST next = operator.getNextSibling(); next != null && (first || next.getType() != CPPTokenTypes.LESSTHAN) ; next = next.getNextSibling() ) {
                            sb.append(next.getText());
                            end = OffsetableBase.getEndOffset(next);
                            first = false;
                        }
                        return sb.toString();
                    } else {
                        AST first = token.getFirstChild();
                        //QualitasCorpus.class: Removed due to compilation errors
                        //if (first.getType() == CPPTokenTypes.IDENT) {
                        if ( true ) {
                            start = OffsetableBase.getStartOffset(first);
                            isMacroExpanded = isMacroExpandedToken(first);
                            end = OffsetableBase.getEndOffset(first);
                            return AstUtil.getText(first);
                        }
                    }
                }
            }
        }
        return ""; // NOI18N
    }

    private CharSequence findClassName(AST ast) {
        return findId(ast, CPPTokenTypes.RCURLY, true);
    }

    private CharSequence findEnumName(AST ast){
        CharSequence aName = findId(ast, CPPTokenTypes.RCURLY, true);
        if (aName == null || aName.length()==0){
            AST token = ast.getNextSibling();
            if( token != null) {
            	//QualitasCorpus.class: Removed due to compilation errors
            	//if (token.getType() == CPPTokenTypes.IDENT) {
                if ( true ) {
                    //typedef enum C { a2, b2, c2 } D;
                    start = OffsetableBase.getStartOffset(token);
                    isMacroExpanded = isMacroExpandedToken(token);
                    end = OffsetableBase.getEndOffset(token);
                    aName = AstUtil.getText(token);
                }
            }
        }
        return aName;
    }

    /**
     * Finds ID (either CPPTokenTypes.CSM_QUALIFIED_ID or CPPTokenTypes.ID)
     * in direct children of the given AST tree
     *
     * @param ast tree to search ID in
     *
     * @param limitingTokenType type of token that, if being found, stops search
     *        -1 means that there is no such token.
     *        This parameter allows, for example, searching until "}" is encountered
     * @param qualified flag indicating if full qualified id is needed
     * @return id
     */
    private CharSequence findId(AST ast, int limitingTokenType, boolean qualified) {
        for( AST token = ast.getFirstChild(); token != null; token = token.getNextSibling() ) {
            int type = token.getType();
            if( type == limitingTokenType && limitingTokenType >= 0 ) {
                return null;
            }
            //QualitasCorpus.class: Removed due to compilation errors
            //else if( type == CPPTokenTypes.IDENT ) {
            else if( true ) {
                start = OffsetableBase.getStartOffset(token);
                isMacroExpanded = isMacroExpandedToken(token);
                end = OffsetableBase.getEndOffset(token);
                return AstUtil.getText(token);
            }
            else if( type == CPPTokenTypes.CSM_QUALIFIED_ID ) {
		if( qualified ) {
                    // for start/end use last token's offsets
                    AST lastNamePart = AstUtil.getLastChild(token);
                    start = OffsetableBase.getStartOffset(lastNamePart);
                    isMacroExpanded = isMacroExpandedToken(lastNamePart);
                    end = OffsetableBase.getEndOffset(lastNamePart);
                    // but return full name as requested
		    return AstUtil.getText(token);
		}
                AST last = AstUtil.getLastChild(token);
                if( last != null) {
                	//QualitasCorpus.class: Removed due to compilation errors
                	//if( last.getType() == CPPTokenTypes.IDENT ) {
                	if( true ) {
                        start = OffsetableBase.getStartOffset(last);
                        isMacroExpanded = isMacroExpandedToken(last);
                        end = OffsetableBase.getEndOffset(last);
                        return AstUtil.getText(last);
                    }
                    else {
                        AST first = token.getFirstChild();
                        if( first.getType() == CPPTokenTypes.LITERAL_OPERATOR ) {
                            start = OffsetableBase.getStartOffset(first);
                            isMacroExpanded = isMacroExpandedToken(first);
                            end = OffsetableBase.getEndOffset(first);
                            StringBuilder sb = new StringBuilder(first.getText());
                            sb.append(' ');
                            AST next = first.getNextSibling();
                            if( next != null ) {
                                end = OffsetableBase.getEndOffset(next);
                                sb.append(next.getText());
                            }
                            return sb.toString();
                        //QualitasCorpus.class: Removed due to compilation errors
                        //} else if (first.getType() == CPPTokenTypes.IDENT){
                        } else if ( true ){
                            start = OffsetableBase.getStartOffset(first);
                            end = OffsetableBase.getEndOffset(first);
                            isMacroExpanded = isMacroExpandedToken(first);
                            return AstUtil.getText(first);
                        }
                    }
                }
            }
        }
        return ""; // NOI18N
    }
}