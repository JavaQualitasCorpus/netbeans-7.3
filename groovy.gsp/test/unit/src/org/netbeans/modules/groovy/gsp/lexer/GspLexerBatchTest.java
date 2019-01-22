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
package org.netbeans.modules.groovy.gsp.lexer;

import junit.framework.TestCase;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import static org.netbeans.lib.lexer.test.LexerTestUtilities.assertTokenEquals;

/**
 * Test GSP lexer
 *
 * @author Martin Adamek
 */
public class GspLexerBatchTest extends TestCase {

    public GspLexerBatchTest(String testName) {
        super(testName);
    }

    public void test1() {
        String text =
                "<html>"
                + "<g:if>"
                + "</g:if>"
                + "</html>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<html>");
        checkNext(sequence, GspTokenId.GTAG, "<g:if>");
        checkNext(sequence, GspTokenId.GTAG, "</g:if>");
        checkNext(sequence, GspTokenId.HTML, "</html>");
    }

    public void test2() {
        String text =
                "<html>"
                + "<g:if test=\"${t}\">"
                + "<div class=\"e\">"
                + "<g:renderErrors bean=\"${f.u}\" />"
                + "</div>"
                + "</g:if>"
                + "<div class=\"s\">${e.s}</div>"
                + "</html>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<html>");
        checkNext(sequence, GspTokenId.GTAG, "<g:if test=\"");
        checkNext(sequence, GspTokenId.DELIMITER, "${");
        checkNext(sequence, GspTokenId.GROOVY_EXPR, "t");
        checkNext(sequence, GspTokenId.DELIMITER, "}");
        checkNext(sequence, GspTokenId.GTAG, "\">");
        checkNext(sequence, GspTokenId.HTML, "<div class=\"e\">");
        checkNext(sequence, GspTokenId.GTAG, "<g:renderErrors bean=\"");
        checkNext(sequence, GspTokenId.DELIMITER, "${");
        checkNext(sequence, GspTokenId.GROOVY_EXPR, "f.u");
        checkNext(sequence, GspTokenId.DELIMITER, "}");
        checkNext(sequence, GspTokenId.GTAG, "\" />");
        checkNext(sequence, GspTokenId.HTML, "</div>");
        checkNext(sequence, GspTokenId.GTAG, "</g:if>");
        checkNext(sequence, GspTokenId.HTML, "<div class=\"s\">");
        checkNext(sequence, GspTokenId.DELIMITER, "${");
        checkNext(sequence, GspTokenId.GROOVY, "e.s");
        checkNext(sequence, GspTokenId.DELIMITER, "}");
        checkNext(sequence, GspTokenId.HTML, "</div>");
        checkNext(sequence, GspTokenId.HTML, "</html>");
    }

    public void test3() {
        String text = 
                "<html>"
                + "<body>"
                + "<g:if test=\"${something different}\">"
                + "<h1>Sample line</h1>"
                + "</g:if>"
                + "</body>"
                + "</html>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<html>");
        checkNext(sequence, GspTokenId.HTML, "<body>");
        checkNext(sequence, GspTokenId.GTAG, "<g:if test=\"");
        checkNext(sequence, GspTokenId.DELIMITER, "${");
        checkNext(sequence, GspTokenId.GROOVY_EXPR, "something different");
        checkNext(sequence, GspTokenId.DELIMITER, "}");
        checkNext(sequence, GspTokenId.GTAG, "\">");
        checkNext(sequence, GspTokenId.HTML, "<h1>");
        checkNext(sequence, GspTokenId.HTML, "Sample line</h1>");
        checkNext(sequence, GspTokenId.GTAG, "</g:if>");
        checkNext(sequence, GspTokenId.HTML, "</body>");
        checkNext(sequence, GspTokenId.HTML, "</html>");
    }

    public void test4() {
        String text = "<html>"
                + "<body>"
                + "<h1>Sample line</h1>"
                + "</body>"
                + "</html>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<html>");
        checkNext(sequence, GspTokenId.HTML, "<body>");
        checkNext(sequence, GspTokenId.HTML, "<h1>");
        checkNext(sequence, GspTokenId.HTML, "Sample line</h1>");
        checkNext(sequence, GspTokenId.HTML, "</body>");
        checkNext(sequence, GspTokenId.HTML, "</html>");
    }

    public void test5() {
        String text = "<html>"
                + "<g:if test=\"\\${}\">"
                + "</g:if>"
                + "</html>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<html>");
        checkNext(sequence, GspTokenId.GTAG, "<g:if test=\"");
        checkNext(sequence, GspTokenId.DELIMITER, "\\${");
        checkNext(sequence, GspTokenId.DELIMITER, "}");
        checkNext(sequence, GspTokenId.GTAG, "\">");
        checkNext(sequence, GspTokenId.GTAG, "</g:if>");
        checkNext(sequence, GspTokenId.HTML, "</html>");
    }

    public void test6() {
        String text = "<tr>"
                + "<td colspan=\"4\">"
                + "<richui:portletView id=\"1\" slotStyle=\"width: 66%; height: 100%;\" playerStyle=\"width: 66%; height: 100%;\" >"
                + "<h1>Tree View Here</h1>"
                + "</richui:portletView>"
                + "</td>"
                + "</tr>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<tr>");
        checkNext(sequence, GspTokenId.HTML, "<td colspan=\"4\">");
        checkNext(sequence, GspTokenId.HTML, "<richui:portletView id=\"1\" slotStyle=\"width: 66%; height: 100%;\" playerStyle=\"width: 66%; height: 100%;\" >");
        checkNext(sequence, GspTokenId.HTML, "<h1>");
        checkNext(sequence, GspTokenId.HTML, "Tree View Here</h1>");
        checkNext(sequence, GspTokenId.HTML, "</richui:portletView>");
        checkNext(sequence, GspTokenId.HTML, "</td>");
        checkNext(sequence, GspTokenId.HTML, "</tr>");
    }

    public void testExclamation() {
        String text = "<p>a!</p>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<p>");
        checkNext(sequence, GspTokenId.HTML, "a!</p>");
    }

    public void testPercent() {
        String text = "<a class=\"home\" href=\"${createLinkTo(dir:'')}\">Home</a>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.HTML, "<a class=\"home\" href=\"");
        checkNext(sequence, GspTokenId.DELIMITER, "${");
        checkNext(sequence, GspTokenId.GROOVY, "createLinkTo(dir:'')");
        checkNext(sequence, GspTokenId.DELIMITER, "}");
        checkNext(sequence, GspTokenId.HTML, "\">");
        checkNext(sequence, GspTokenId.HTML, "Home</a>");
    }

    public void testExpressionInValue() {
        String text =
                "<%@ page import=\"org.grails.bookmarks.*\" %>"
                + "<style type=\"text/css\">.searchbar {width:97%;}</style>";
        TokenSequence<?> sequence = createTokenSequence(text);

        checkNext(sequence, GspTokenId.DELIMITER, "<%@");
        checkNext(sequence, GspTokenId.GROOVY, " page import=\"org.grails.bookmarks.*\" ");
        checkNext(sequence, GspTokenId.DELIMITER, "%>");
        checkNext(sequence, GspTokenId.HTML, "<style type=\"text/css\">");
        checkNext(sequence, GspTokenId.HTML, ".searchbar {width:97%;}</style>");
    }

    private TokenSequence createTokenSequence(String text) {
        TokenHierarchy<?> hierarchy = TokenHierarchy.create(text, GspTokenId.language());
        return hierarchy.tokenSequence();
    }

    private void checkNext(TokenSequence<?> sequence, GspTokenId gspTokenId, String expectedContent) {
        assertTrue(sequence.moveNext());
        assertTokenEquals(sequence, gspTokenId, expectedContent, -1);
    }
}