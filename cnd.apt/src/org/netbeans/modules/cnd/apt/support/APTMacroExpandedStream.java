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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.apt.support;

import org.netbeans.modules.cnd.antlr.RecognitionException;
import org.netbeans.modules.cnd.antlr.TokenStream;
import org.netbeans.modules.cnd.antlr.TokenStreamException;
import org.netbeans.modules.cnd.apt.impl.support.APTCommentToken;
import org.netbeans.modules.cnd.apt.impl.support.MacroExpandedToken;
import org.netbeans.modules.cnd.apt.utils.APTUtils;

/**
 *
 * @author Vladimir Voskresensky
 */
public class APTMacroExpandedStream extends APTExpandedStream {
    private final boolean emptyExpansionAsComment;
    
    public APTMacroExpandedStream(TokenStream stream, APTMacroCallback callback, boolean emptyExpansionAsComment) {
        super(stream, callback);
        this.emptyExpansionAsComment = emptyExpansionAsComment;
    }

    @Override
    protected APTTokenStream createMacroBodyWrapper(APTToken token, APTMacro macro) throws TokenStreamException, RecognitionException {
        APTTokenStream origExpansion = super.createMacroBodyWrapper(token, macro);
        APTToken last = getLastExtractedParamRPAREN();
        if (last == null) {
            last = token;
        }
        APTTokenStream expandedMacros = new MacroExpandWrapper(token, origExpansion, last, emptyExpansionAsComment);
        return expandedMacros;
    }   
    
    private static final class MacroExpandWrapper implements TokenStream, APTTokenStream {
        private final APTToken expandedFrom;
        private final APTTokenStream expandedMacros;
        private final APTToken endOffsetToken;
        private boolean firstToken = true;
        private final boolean emptyExpansionAsComment;
        
        public MacroExpandWrapper(APTToken expandedFrom, APTTokenStream expandedMacros, APTToken endOffsetToken, boolean emptyExpansionAsComment) {
            this.expandedFrom = expandedFrom;
            this.expandedMacros = expandedMacros;
            assert endOffsetToken != null : "end offset token must be valid";
            this.endOffsetToken = endOffsetToken;
            this.emptyExpansionAsComment = emptyExpansionAsComment;
        }
        
        @Override
        public APTToken nextToken() {
            APTToken expandedTo = expandedMacros.nextToken();
            if (emptyExpansionAsComment && firstToken && APTUtils.isEOF(expandedTo)) {
                // adding empty comment as expansion of empty macro
                expandedTo = new APTCommentToken();
                expandedTo.setType(APTTokenTypes.COMMENT);
            }
            firstToken = false;
            APTToken outToken = new MacroExpandedToken(expandedFrom, expandedTo, endOffsetToken);
            return outToken;
        }        
    }
}