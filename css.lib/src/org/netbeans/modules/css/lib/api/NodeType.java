/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only (GPL) or the Common
 * Development and Distribution License(CDDL) (collectively, the
 * License). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the Classpath exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted [year] [name of copyright owner]
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
package org.netbeans.modules.css.lib.api;

/**
 * Node types for all the CSS3 ANTLR grammar elements.
 * 
 * This enum must keep in sync with the ANTLR grammar set of rules!!!
 *
 * @author mfukala@netbeans.org
 */
public enum NodeType {

    /**
     * Identification of an @-rule.
     * 
     * For example represents the 'bounce' text in following code: 
     * <pre>
     * @-webkit-keyframes 'bounce' { ...
     * </pre>
     * 
     * IDENT | STRING 
     */
    atRuleId,
    
    /**
     * A list of bodyItem-s
     */
    body,
    
    /**
     * rule | media | page | counterStyle | fontFace | moz_document
     */
    bodyItem,
    
    combinator,
    /**
     * @counter-style <counter-name> { ... }
     */
    counterStyle,
    cssId,
    cssClass,
    declaration,
    declarations,
    elementSubsequent,
    elementName,
    esPred,
    expression,
    /**
     * @font-face { ... }
     */
    fontFace,
    /**
     * Attribute name - value pair in a css function: h1 { -moz-xxx:
     * draw(shape="rect") }
     */
    fnAttribute,
    /**
     * Attribute name in a css function: h1 { -moz-xxx: draw(shape="rect") }
     */
    fnAttributeName,
    /**
     * Attribute value in a css function: h1 { -moz-xxx: draw(shape="rect") }
     */
    fnAttributeValue,
    function,
    functionName,
     /**
         * any @xxxx { ... } generic at-rule
         */
     generic_at_rule,

    /**
     * @charset "..." rule
     */
    charSet,
    /**
     * The charset string in the @charset rule
     */
    charSetValue,
    invalidRule,
    hexColor,
    margin,
    margin_sym,
    media,
    mediaExpression,
    mediaFeature,
    mediaQueryList,
    mediaQuery,
    
    /**
     * NOT | ONLY prefix of the media query
     */
    mediaQueryOperator,
    
    
    mediaType,
    
    /**
     * Mozilla specific at rule @-moz-document See
     * https://developer.mozilla.org/en/CSS/@-moz-document
     */
    moz_document,
    /**
     * Mozilla @-moz-document at rule argument See
     * https://developer.mozilla.org/en/CSS/@-moz-document
     */
    moz_document_function,
    /**
     * Namespace declaration: @namespace prefix "http://myns";
     */
    namespace,
    
    /**
     * List of namespace-s
     */
    namespaces,
    /**
     * Namespace prefix or wildcard prefix of the css qualified name: myns|E {}
     * || *|E {}
     */
    namespacePrefix,
    /**
     * The namespace prefix
     */
    namespacePrefixName,
    /**
     * syntactic predicate
     */
    nsPred,
    operator,
    page,
    pseudoPage,
    property,
    prio,
    pseudo,
    /**
     * String or URI, used in namespace and import rules
     */
    resourceIdentifier,
    /**
     * @import ... rule
     */
    importItem,
    
    /**
     * list of importItem-s
     */
    imports,
    
    /**
     * Value of the css property - color : "red";
     */
    propertyValue,
    
    rule,
    /**
     * a CSS rule: div { ... }
     */
    
    selector,
    simpleSelectorSequence,
    /**
     * Attribute name - value pair in selectors h1[attr=value] {}
     */
    slAttribute,
    /**
     * Attribute name in selectors h1[attr=value] {}
     */
    slAttributeName,
    /**
     * Attribute value in selectors h1[attr=value] {}
     */
    slAttributeValue,
    /**
     * error recovery rule
     */
    syncToFollow,
    /**
     * error recovery rule
     */
    syncToDeclarationsRule,
    /**
     * error recovery rule
     */
    syncTo_IDENT_RBRACKET_LBRACE,
    
    syncTo_RBRACE,
    
    synpred2_Css3,
    selectorsGroup,
    styleSheet,
    /**
     * syntactic predicate
     */
    synpred1_Css3,
    /**
     * syntactic predicate
     */
    synpred3_Css3,
    term,
    typeSelector,

    /**
     * an artificial root node of each parse tree
     * 
     * The node types doesn't correspond to any of the grammar rules (do not remove it!)
     */
    root,
    /**
     * an error node
     * 
     * The node types doesn't correspond to any of the grammar rules (do not remove it!)
     */
    error,
    /**
     * an error node, but for errors recovered - skipped (resynced) content by
     * syncToBitSet(...)
     * 
     * The node types doesn't correspond to any of the grammar rules (do not remove it!)
     */
    recovery,
    /**
     * a token node (each lexer token has its node in the parse tree)
     * 
     * The node types doesn't correspond to any of the grammar rules (do not remove it!)
     */
    token,
    unaryOperator,
    
    /**
     * A vendor specific @-rule.
     * 
     * Example: @-moz-document rule
     */
    vendorAtRule,
    
    /**
     * @-webkit-keyframes vendor specific rule.
     * 
     * Example:
     * <pre>
     * @-webkit-keyframes 'bounce' {
     * 
     *    from, 20% {
     *      top: 100px;
     *      -webkit-animation-timing-function: ease-out;
     *    }
     * 
     * }
     * </pre>
     * 
     */
    webkitKeyframes,
    
    /**
     * @-webkit-keyframes content block
     * 
     * Example:
     * <pre>
     *    from, 20% {
     *      top: 100px;
     *      -webkit-animation-timing-function: ease-out;
     *    }
     * </pre>
     */
    webkitKeyframesBlock,
    
    /**
     * @-webkit-keyframes content block selectors
     * 
     * Example:
     * <pre>
     * from, 20%
     * </pre>
     */
    webkitKeyframeSelectors,
    
    /**
     * whitespace, new line or comment tokens
     */
    ws;
}
