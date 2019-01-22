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

package org.netbeans.modules.web.el;

import org.netbeans.modules.web.el.navigation.ELDeclarationFinder;
import org.netbeans.modules.web.el.completion.ELCodeCompletionHandler;
import org.netbeans.api.lexer.Language;
import org.netbeans.modules.csl.api.CodeCompletionHandler;
import org.netbeans.modules.csl.api.DeclarationFinder;
import org.netbeans.modules.csl.api.HintsProvider;
import org.netbeans.modules.csl.api.OccurrencesFinder;
import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
import org.netbeans.modules.csl.spi.LanguageRegistration;
import org.netbeans.modules.el.lexer.api.ELTokenId;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.PathRecognizerRegistration;
import org.netbeans.modules.web.el.hints.ELHintsProvider;

/**
 * CSL language for Expression Language
 *
 * @author Erno Mononen
 */
@LanguageRegistration(mimeType=ELLanguage.MIME_TYPE)
@PathRecognizerRegistration(mimeTypes=ELLanguage.MIME_TYPE, libraryPathIds={}, binaryLibraryPathIds={})
public class ELLanguage extends DefaultLanguageConfig {

    public static final String MIME_TYPE = "text/x-el"; //NOI18N

    @Override
    public Language getLexerLanguage() {
        return ELTokenId.language();
    }

    @Override
    public String getDisplayName() {
        return "EL";
    }

    @Override
    public Parser getParser() {
        return new ELParser();
    }

    @Override
    public EmbeddingIndexerFactory getIndexerFactory() {
        return new ELIndexer.Factory();
    }

    @Override
    public boolean hasHintsProvider() {
        return true;
    }

    @Override
    public HintsProvider getHintsProvider() {
        return new ELHintsProvider();
    }

    @Override
    public boolean hasOccurrencesFinder() {
        return true;
    }

    @Override
    public OccurrencesFinder getOccurrencesFinder() {
        return new ELOccurrencesFinder();
    }

    @Override
    public CodeCompletionHandler getCompletionHandler() {
        return new ELCodeCompletionHandler();
    }

    @Override
    public DeclarationFinder getDeclarationFinder() {
        return new ELDeclarationFinder();
    }

}