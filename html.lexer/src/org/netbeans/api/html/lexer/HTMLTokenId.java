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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.api.html.lexer;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.lexer.*;
import org.netbeans.lib.html.lexer.HtmlLexer;
import org.netbeans.spi.lexer.LanguageEmbedding;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 * Token ids of HTML language
 *
 * @author Jan Lahoda, Miloslav Metelka, Marek Fukala
 */
public enum HTMLTokenId implements TokenId {

    /** HTML text */
    TEXT("text"),
    /** HTML script e.g. javascript. */
    SCRIPT("script"),
    /** HTML CSS style.*/
    STYLE("style"),
    /** Whitespace in a tag: <code> &lt;BODY" "bgcolor=red&gt;</code>. */
    WS("ws"),
    /** Error token - returned in various erroneous situations. */
    ERROR("error"),
    /** HTML open tag name: <code>&lt;"BODY"/&gt;</code>.*/
    TAG_OPEN("tag"),
    /** HTML close tag name: <code>&lt;/"BODY"&gt;</code>.*/
    TAG_CLOSE("tag"),
    /** HTML tag attribute name: <code> &lt;BODY "bgcolor"=red&gt;</code>.*/
    ARGUMENT("argument"),
    /** Equals sign in HTML tag: <code> &lt;BODY bgcolor"="red&gt;</code>.*/
    OPERATOR("operator"),
    /** Attribute value in HTML tag: <code> &lt;BODY bgcolor="red"&gt;</code>.*/
    VALUE("value"),
    /** HTML javascript attribute value, such as one following onclick etc. */
    VALUE_JAVASCRIPT("value"),
    /** HTML style attribute value */
    VALUE_CSS("value"),
    /** HTML block comment: <code> &lt;!-- xxx --&gt; </code>.*/
    BLOCK_COMMENT("block-comment"),
    /** HTML/SGML comment.*/
    SGML_COMMENT("sgml-comment"),
    /** HTML/SGML declaration: <code> &lt;!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"&gt; </code>.*/
    DECLARATION("sgml-declaration"),
    /** XML processing instruction: <? ... ?>*/
    XML_PI("xml-pi"),
    /** Character reference: <code> &amp;amp; </code>.*/
    CHARACTER("character"),
    /** End of line.*/
    EOL("text"),
    /** HTML open tag symbol: <code> "&lt;"BODY&gt; </code>.*/
    TAG_OPEN_SYMBOL("tag"),
    /** HTML close tag symbol: <code> "&lt;/"BODY&gt; </code>.*/
    TAG_CLOSE_SYMBOL("tag");
    private final String primaryCategory;
    private static final String JAVASCRIPT_MIMETYPE = "text/javascript";//NOI18N
    private static final String STYLE_MIMETYPE = "text/css";//NOI18N
    /**
     * Property key of css value tokens determining the token type in more detail.
     * The value may either be null for common embedded
     * css code (<style>...</style> or <div style="..."/>) or "id" or "class" for
     * css id selectors (<div id="..."/> resp. class selectors (<div class="..."/>).
     * The values are defined in VALUE_CSS_TOKEN_TYPE_ID and VALUE_CSS_TOKEN_TYPE_CLASS
     * constants of this class.
     *
     * Typical usage is:
     * <code>
     *      Token<HTMLTokenId> token = ...;
     *      String val = token.getProperty(VALUE_CSS_TOKEN_TYPE_PROPERTY);
     *      if(VALUE_CSS_TOKEN_TYPE_CLASS.equals(val) {
     *          //the token represents a class selector embedded in html
     *          //class attribute of an html tag token
     *      }
     * </code>
     */
    public static final String VALUE_CSS_TOKEN_TYPE_PROPERTY = "valueCssType"; //NOI18N
    /**
     * Token's property value of VALUE_CSS_TOKEN_TYPE_PROPERTY property key marking
     * this token as an id selector.
     */
    public static final String VALUE_CSS_TOKEN_TYPE_ID = "id"; //NOI18N
    /**
     * Token's property value of VALUE_CSS_TOKEN_TYPE_PROPERTY property key marking
     * this token as a class selector.
     */
    public static final String VALUE_CSS_TOKEN_TYPE_CLASS = "class"; //NOI18N
    
    /**
     * Token property name for the SCRIPT tokens.
     * 
     * Allows to get value of the type attribute of script tag.
     */
    public static final String SCRIPT_TYPE_TOKEN_PROPERTY = "type"; //NOI18N
    
    private static final Logger LOGGER = Logger.getLogger(HtmlLexer.class.getName());
    private static final boolean LOG = LOGGER.isLoggable(Level.FINE);

    HTMLTokenId(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }
    private static final Language<HTMLTokenId> language = new LanguageHierarchy<HTMLTokenId>() {

        @Override
        protected Collection<HTMLTokenId> createTokenIds() {
            return EnumSet.allOf(HTMLTokenId.class);
        }

        @Override
        protected Map<String, Collection<HTMLTokenId>> createTokenCategories() {
            //Map<String,Collection<HTMLTokenId>> cats = new HashMap<String,Collection<HTMLTokenId>>();
            // Additional literals being a lexical error
            //cats.put("error", EnumSet.of());
            return null;
        }

        @Override
        protected Lexer<HTMLTokenId> createLexer(LexerRestartInfo<HTMLTokenId> info) {
            return new HtmlLexer(info);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected LanguageEmbedding embedding(
                Token<HTMLTokenId> token, LanguagePath languagePath, InputAttributes inputAttributes) {
            if (LOG) {
                LOGGER.log(Level.FINE,
                        String.format("HTMLTokenId$Language<HTMLTokenId>.embedding(...) called on removed token with %s token id. LanguagePath: %s", token.id(), languagePath),
                        new IllegalStateException()); //NOI18N
            }

            String mimeType = null;
            switch (token.id()) {
                // BEGIN TOR MODIFICATIONS
                case VALUE_JAVASCRIPT:
                    mimeType = JAVASCRIPT_MIMETYPE;
                    if (mimeType != null) {
                        Language lang = Language.find(mimeType);
                        if (lang == null) {
                            return null; //no language found
                        } else {
                            // TODO:
                            // XXX Don't handle JavaScript for non-quoted attributes
                            // (Or use separate state so I can do 0,0 as offsets there

                            // Marek: AFAIK value of the onSomething methods is always javascript
                            // so having the attribute unqouted doesn't make much sense -  the html spec
                            // allows only a-zA-Z characters in unqouted values so I belive
                            // it is not possible to write reasonable js code - it ususally
                            // contains some whitespaces, brackets, quotations etc.

                            PartType ptype = token.partType();
                            int startSkipLength = ptype == PartType.COMPLETE || ptype == PartType.START ? 1 : 0;
                            int endSkipLength = ptype == PartType.COMPLETE || ptype == PartType.END ? 1 : 0;
                            //do not join css code sections in attribute value between each other,
                            //only token parts inside one value
                            return LanguageEmbedding.create(
                                    lang,
                                    startSkipLength,
                                    endSkipLength,
                                    (ptype == PartType.END || ptype == PartType.COMPLETE) ? false : true);
                        }
                    }
                    break;
                // END TOR MODIFICATIONS
                case VALUE_CSS:
                    mimeType = STYLE_MIMETYPE;
                    if (mimeType != null) {
                        Language lang = Language.find(mimeType);
                        if (lang == null) {
                            return null; //no language found
                        } else {
                            PartType ptype = token.partType();
                            if (token.isRemoved()) {
                                //strange, but sometimes the embedding is requested on removed token which has null image
                                return null;
                            }
                            char firstChar = token.text().charAt(0);
                            boolean quoted = firstChar == '\'' || firstChar == '"';
                            int startSkipLength = quoted && (ptype == PartType.COMPLETE || ptype == PartType.START) ? 1 : 0;
                            int endSkipLength = quoted && (ptype == PartType.COMPLETE || ptype == PartType.END) ? 1 : 0;

                            //do not join css code sections in attribute value between each other,
                            //only token parts inside one value
                            return LanguageEmbedding.create(
                                    lang,
                                    startSkipLength,
                                    endSkipLength,
                                    (ptype == PartType.END || ptype == PartType.COMPLETE) ? false : true);
                        }
                    }
                    break;
                case SCRIPT:
                    String scriptType = (String)token.getProperty(SCRIPT_TYPE_TOKEN_PROPERTY);
                    if(scriptType == null || JAVASCRIPT_MIMETYPE.equals(scriptType)) {
                        mimeType = JAVASCRIPT_MIMETYPE;
                    }
                    break;
                case STYLE:
                    mimeType = STYLE_MIMETYPE;
                    break;
            }
            if (mimeType != null) {
                Language lang = Language.find(mimeType);
                if (lang == null) {
                    return null; //no language found
                } else {
                    return LanguageEmbedding.create(lang, 0, 0, true);
                }
            }
            return null;
        }

        @Override
        protected String mimeType() {
            return "text/html";
        }
    }.language();

    /** Gets a LanguageDescription describing a set of token ids
     * that comprise the given language.
     *
     * @return non-null LanguageDescription
     */
    public static Language<HTMLTokenId> language() {
        return language;
    }

    /**
     * Get name of primary token category into which this token belongs.
     * <br/>
     * Other token categories for this id can be defined in the language hierarchy.
     *
     * @return name of the primary token category into which this token belongs
     *  or null if there is no primary category for this token.
     */
    public String primaryCategory() {
        return primaryCategory;
    }
}