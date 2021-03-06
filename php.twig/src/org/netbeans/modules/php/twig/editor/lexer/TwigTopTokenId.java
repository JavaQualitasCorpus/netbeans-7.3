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
 * Contributor(s): Sebastian Hörl
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.php.twig.editor.lexer;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.html.lexer.HTMLTokenId;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.modules.php.twig.editor.gsf.TwigLanguage;
import org.netbeans.spi.lexer.LanguageEmbedding;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

public enum TwigTopTokenId implements TokenId {

    T_HTML(null, "twig_html"), //NOI18N
    T_ERROR(null, "twig_error"), //NOI18N
    T_TWIG(null, "twig"), //NOI18N
    T_TWIG_RAW(null, "twig_raw"); //NOI18N
    
    private String fixedText;
    private String primaryCategory;

    TwigTopTokenId(String fixedText, String primaryCategory) {
        this.fixedText = fixedText;
        this.primaryCategory = primaryCategory;
    }

    public String fixedText() {
        return fixedText;
    }

    @Override
    public String primaryCategory() {
        return primaryCategory;
    }
    private static final Language<TwigTopTokenId> language =
            new LanguageHierarchy<TwigTopTokenId>() {
                @Override
                protected Collection<TwigTopTokenId> createTokenIds() {
                    return EnumSet.allOf(TwigTopTokenId.class);
                }

                @Override
                protected Map<String, Collection<TwigTopTokenId>> createTokenCategories() {
                    Map<String, Collection<TwigTopTokenId>> cats = new HashMap<String, Collection<TwigTopTokenId>>();
                    return cats;
                }

                @Override
                protected Lexer<TwigTopTokenId> createLexer(LexerRestartInfo<TwigTopTokenId> info) {
                    return TwigTopLexer.create(info);
                }

                @Override
                protected String mimeType() {
                    return TwigLanguage.TWIG_MIME_TYPE;
                }

                @Override
                protected LanguageEmbedding<?> embedding(Token<TwigTopTokenId> token,
                        LanguagePath languagePath, InputAttributes inputAttributes) {

                    TwigTopTokenId id = token.id();
                    if (id == T_HTML || id == T_TWIG_RAW) {
                        return LanguageEmbedding.create(HTMLTokenId.language(), 0, 0, true);
                    } else if (id == T_TWIG) {
                        return LanguageEmbedding.create(TwigTokenId.language(), 0, 0);
                    }

                    return null;

                }
            }.language();

    public static Language<TwigTopTokenId> language() {
        return language;
    }
}