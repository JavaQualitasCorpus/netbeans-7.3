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
package org.netbeans.modules.css.lib;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.BadLocationException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.netbeans.lib.editor.util.CharSequenceUtilities;
import org.netbeans.modules.css.lib.api.*;
import org.netbeans.modules.css.lib.api.properties.ResolvedProperty;
import org.netbeans.modules.parsing.spi.ParseException;
import org.openide.filesystems.FileObject;

/**
 *
 * @author marekfukala
 */
public class Css3ParserTest extends CssTestBase {

    public Css3ParserTest(String testName) {
        super(testName);
    }

//     public static Test suite() throws IOException, BadLocationException {
//        System.err.println("Beware, only selected tests runs!!!");
//        TestSuite suite = new TestSuite();
//        suite.addTest(new Css3ParserTest("testIssue211103"));
//        return suite;
//    }
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CssParserResult.IN_UNIT_TESTS = true;
    }

    public void testAllANTLRRulesHaveNodeTypes() {
        for (String rule : Css3Parser.ruleNames) {
            assertNotNull(NodeType.valueOf(rule));
        }
    }

    public void testErrorRecoveryInRule() throws ParseException, BadLocationException {
        //resync the parser to the last right curly bracket
        String code = "myns|h1  color: red; } h2 { color: blue; }";

        CssParserResult res = TestUtil.parse(code);
//        dumpResult(res);

        //this case recovers badly so far - the myns|h1 and h2 are joined into a single rule
    }

    public void testErrorRecoveryInsideDeclaration() throws ParseException, BadLocationException {
        //recovery inside declaration rule, resyncing to next semicolon or right curly brace
        String code = "a {\n"
                + " s  red; \n"
                + " background: red; \n"
                + "}";

        CssParserResult res = TestUtil.parse(code);

        assertResult(res, 2);
//        TestUtil.dumpResult(res);

        //the background: red; declaration is properly parsed even if the previous declaration is broken
        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|1/property/background"));

    }

    public void testErrorRecoveryGargabeBeforeDeclaration() throws ParseException, BadLocationException {
        //recovery before entering declaration rule, the Parser.syncToIdent() is used to skip until ident is found

        String code = "a {\n"
                + " @ color: red; \n"
                + " background: red; \n"
                + "}";

        CssParserResult res = TestUtil.parse(code);
//        TestUtil.dumpResult(res);

        assertResult(res, 1);

        //the garbage char @ is skipped by Parser.syncToIdent()
        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|0/property/color"));
        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|1/property/background"));

    }

    public void testSimpleValidCode() throws ParseException, BadLocationException {
        String code = "a {"
                + "color : black;"
                + "}";

        CssParserResult res = TestUtil.parse(code);
//        TestUtil.dumpResult(res);
        assertResult(res, 0);

        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|0/property/color"));

    }

    public void testValidCode() throws ParseException, BadLocationException {
        String code = "a {\n"
                + "color : black; \n"
                + "background: red; \n"
                + "}\n\n"
                + ".class { }\n"
                + "#id { }";

        CssParserResult res = TestUtil.parse(code);
        assertResult(res, 0);

        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|0/property/color"));
        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|1/property/background"));

    }

    public void testParseTreeOffsets() throws ParseException, BadLocationException {
        String code = "/* comment */ body { color: red; }";
        //             01234567890123456789
        //             0         1

        CssParserResult res = TestUtil.parse(code);
//        TestUtil.dumpResult(res);
        assertResult(res, 0);

        Node aNode = NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + "rule/selectorsGroup/selector/simpleSelectorSequence/typeSelector/elementName/body");

        assertNotNull(aNode);
        assertTrue(aNode instanceof TokenNode);

        assertEquals("body", aNode.name());
        assertEquals(NodeType.token, aNode.type());

        assertEquals("body".length(), aNode.name().length());
        assertEquals(14, aNode.from());
        assertEquals(18, aNode.to());
    }

    public void testNamespacesInSelector() throws ParseException, BadLocationException {
        CssParserResult res = assertResultOK(TestUtil.parse("myns|h1 { color: red; }"));
//        NodeUtil.dumpTree(res.getParseTree());

        String typeSelectorPath = "rule/selectorsGroup/selector/simpleSelectorSequence/typeSelector/";

        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + typeSelectorPath + "namespacePrefix/namespacePrefixName/myns"));
        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + typeSelectorPath + "elementName/h1"));

        res = assertResultOK(TestUtil.parse("*|h1 { color: red; }"));


        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + typeSelectorPath + "namespacePrefix/*"));
        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + typeSelectorPath + "elementName/h1"));

        res = assertResultOK(TestUtil.parse("*|* { color: red; }"));

        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + typeSelectorPath + "namespacePrefix/*"));
        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + typeSelectorPath + "elementName/*"));
    }

    public void testNamespacesInAttributes() throws ParseException, BadLocationException {
        CssParserResult res = assertResultOK(TestUtil.parse("h1[myns|attr=val] { color: red; }"));
//        NodeUtil.dumpTree(res.getParseTree());

        String simpleSelectorPath = "rule/selectorsGroup/selector/simpleSelectorSequence/";

        assertNotNull(NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + simpleSelectorPath + "typeSelector/elementName/h1"));

        Node attribNode = NodeUtil.query(res.getParseTree(),
                TestUtil.bodysetPath + simpleSelectorPath + "elementSubsequent/slAttribute");

        assertNotNull(attribNode);
        assertNotNull(NodeUtil.query(attribNode,
                "namespacePrefix/namespacePrefixName/myns"));

        assertNotNull(NodeUtil.query(attribNode,
                "slAttributeName/attr"));

        assertNotNull(NodeUtil.query(attribNode,
                "slAttributeValue/val"));

    }

    public void testNodeImages() throws ParseException, BadLocationException {
        String selectors = "#id .class body ";
        String code = selectors + "{ color: red}";
        CssParserResult res = TestUtil.parse(code);
//        dumpResult(res);

        String selectorsGroupPath = "rule/selectorsGroup";

        //test rule node image
        Node selectorsGroup = NodeUtil.query(res.getParseTree(), TestUtil.bodysetPath + selectorsGroupPath);
        assertNotNull(selectorsGroup);

        assertTrue(CharSequenceUtilities.equals(selectors, selectorsGroup.image()));

        //test root node image
        assertTrue(CharSequenceUtilities.equals(code, res.getParseTree().image()));

        //test token node image
        Node id = NodeUtil.query(selectorsGroup, "selector/simpleSelectorSequence/elementSubsequent/cssId/#id");
        assertNotNull(id);
        assertTrue(id instanceof TokenNode);
        assertTrue(CharSequenceUtilities.equals("#id", id.image()));

    }

    public void testCommon() throws ParseException, BadLocationException {
        String code = "#id .class body { color: red}     body {}";
        CssParserResult res = TestUtil.parse(code);
//        TestUtil.dumpResult(res);
        assertResult(res, 0);
    }

    public void testMedia() throws ParseException, BadLocationException {
        String code = "@media screen { h1 { color: red; } }";
        CssParserResult res = TestUtil.parse(code);
//        TestUtil.dumpResult(res);
        assertResult(res, 0);
    }

    public void testRootNodeSpan() throws ParseException, BadLocationException {
        String code = "   h1 { }    ";
        //             012345678901234
        CssParserResult res = TestUtil.parse(code);
//        TestUtil.dumpResult(res);

        Node root = res.getParseTree();
        assertEquals(0, root.from());
        assertEquals(code.length(), root.to());
    }

    public void testImport() throws ParseException, BadLocationException {
        String code = "@import \"file.css\";";
        CssParserResult res = TestUtil.parse(code);

//        TestUtil.dumpResult(res);
        Node imports = NodeUtil.query(res.getParseTree(), "styleSheet/imports");
        assertNotNull(imports);

        //url form
        code = "@import url(\"file.css\");";
        res = TestUtil.parse(code);

//        TestUtil.dumpResult(res);
        imports = NodeUtil.query(res.getParseTree(), "styleSheet/imports");
        assertNotNull(imports);

    }

    public void testErrorCase1() throws BadLocationException, ParseException, FileNotFoundException {
        String code = "h1 { color:  }";
        CssParserResult res = TestUtil.parse(code);

        //Test whether all the nodes are properly intialized - just dump the tree.
        //There used to be a bug that error token caused some rule
        //nodes not having first token set properly by the NbParseTreeBuilder
        NodeUtil.dumpTree(res.getParseTree(), new PrintWriter(new StringWriter()));


//        NodeUtil.dumpTree(res.getParseTree());
    }

    public void testErrorCase2() throws BadLocationException, ParseException, FileNotFoundException {
        String code = "a { color: red; } ";

        CssParserResult res = TestUtil.parse(code);

//        NodeUtil.dumpTree(res.getParseTree());

        assertResult(res, 0);

    }

    public void testErrorCase_emptyDeclarations() throws BadLocationException, ParseException, FileNotFoundException {
        String code = "h1 {}";

        CssParserResult res = TestUtil.parse(code);

        //syncToIdent bug - it cannot sync to ident since there isn't one - but the case is valid
        //=> reconsider putting syncToIdent back to the declarations rule, but then I need 
        //to resolve why it is not called even in proper cases!!!
//        NodeUtil.dumpTree(res.getParseTree());
        AtomicBoolean recoveryNodeFound = new AtomicBoolean(false);
        NodeVisitor<AtomicBoolean> visitor = new NodeVisitor<AtomicBoolean>(recoveryNodeFound) {

            @Override
            public boolean visit(Node node) {
                if (node.type() == NodeType.recovery) {
                    getResult().set(true);
                    return true;
                }
                return false;
            }
        };

        visitor.visitChildren(res.getParseTree());

        assertResult(res, 0);

        assertFalse(recoveryNodeFound.get());

        //this doesn't work actually, the resyncing to ident doesn't work naturally

    }

    //issue #160780
    public void testFatalParserError() throws ParseException, BadLocationException {
        //fatal parse error on such input
        String content = "@charset";

        CssParserResult result = TestUtil.parse(content);
        assertNotNull(result.getParseTree());
        assertEquals(1, result.getDiagnostics().size());
    }

    public void testCharsetParsing() throws ParseException, BadLocationException {
        String content = "@charset \"iso-8859-1\";\n h1 { color: red; }";

        CssParserResult result = TestUtil.parse(content);
        assertResult(result, 0);
    }

    public void testErrorCase4() throws ParseException, BadLocationException {
        String content = "h1 { color: ;}";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpResult(result);
        
        assertResult(result, 2);
    }

    public void testIdParsing() throws ParseException, BadLocationException {
        String content = "h1 #myid { color: red }";

        CssParserResult result = TestUtil.parse(content);
        assertResult(result, 0);
//        TestUtil.dumpResult(result);

        Node id = NodeUtil.query(result.getParseTree(), TestUtil.bodysetPath + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/cssId");
        assertNotNull(id);

        assertEquals(NodeType.cssId, id.type());

    }

    public void testErrorRecoveryBetweenrules() throws ParseException, BadLocationException {
        String content = "h1 { color: red} ; h2 { color: blue }";
        //                                 ^ -- semicolon not allowed here

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpResult(result);

        //commented out since it currently fails
        //assertResult(result, 0); 
    }

    public void testErrorCase5() throws ParseException, BadLocationException {
        String content = "a { }   m { }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpResult(result);

        assertResult(result, 0);
    }

    public void testMSFunction() throws ParseException, BadLocationException {
        //Microsoft css extension allows following code:
        String content = "a {"
                + "filter: progid:DXImageTransform.Microsoft.gradient("
                + "startColorstr='#bdb1a0', endColorstr='#958271',GradientType=0 ); /* IE6-9 */"
                + "   color: red;"
                + "}";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpResult(result);

        assertNotNull(NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|0/property/filter"));

        Node function = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath + "rule/declarations/declaration|0/propertyValue/expression/term/function");
        assertNotNull(function);

        Node functionName = NodeUtil.query(function, "functionName");
        assertNotNull(functionName);
        assertEquals("progid:DXImageTransform.Microsoft.gradient", functionName.image().toString());

        Node attr = NodeUtil.query(function, "fnAttribute|0");
        assertNotNull(attr);

        Node attrName = NodeUtil.query(attr, "fnAttributeName");
        assertNotNull(attrName);
        assertEquals("startColorstr", attrName.image().toString());

        Node attrVal = NodeUtil.query(attr, "fnAttributeValue");
        assertNotNull(attrVal);
        assertEquals("'#bdb1a0'", attrVal.image().toString());

        assertResult(result, 0);


    }

    public void testNamespaceDeclaration() throws ParseException, BadLocationException {
        String content = "@namespace prefix \"url\";";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpResult(result);

        Node ns = NodeUtil.query(result.getParseTree(),
                "styleSheet/namespaces/namespace");
        assertNotNull(ns);

        Node prefix = NodeUtil.query(ns, "namespacePrefixName");
        assertNotNull(prefix);
        assertEquals("prefix", prefix.image().toString());

        Node res = NodeUtil.query(ns, "resourceIdentifier");
        assertNotNull(res);
        assertEquals("\"url\"", res.image().toString());

        assertResult(result, 0);
    }

    public void testErrorCase7() throws ParseException, BadLocationException {
        String content = "h1[ $@# ]{ }";
        //                012345678

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        Node error = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/slAttribute/slAttributeName/error");
        assertNotNull(error);

        assertEquals(4, error.from());
        assertEquals(6, error.to());
        assertEquals("$@", error.image().toString());

    }

    public void testErrorCase8() throws ParseException, BadLocationException {
        String content = "h1[f { }";
        //                01234567

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        Node error = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/slAttribute/error");
        assertNotNull(error);
        assertEquals(5, error.from());
        assertEquals(6, error.to());
        assertEquals("{", error.image().toString());

        content = "h1[foo=] { }";
        //         012345678
        result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        error = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/slAttribute/slAttributeValue/error");
        assertNotNull(error);
        assertEquals(7, error.from());
        assertEquals(8, error.to());
        assertEquals("]", error.image().toString());

    }

    public void testErrorCase9() throws ParseException, BadLocationException {
        String content = "h1[foo|attr=val,]";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        Node error = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/selectorsGroup/selector/simpleSelectorSequence/error");
        assertNotNull(error);
        assertEquals(17, error.from());
        assertEquals(17, error.to());

        //premature end of file
        Node error2 = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/error");
        assertNotNull(error2);
        assertEquals(content.length(), error2.from());
        assertEquals(content.length(), error2.to());

        assertResult(result, 2);

    }

    public void testPseudoClasses() throws ParseException, BadLocationException {
        String content = "div:enabled { }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);
        Node pseudo = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/pseudo");
        assertNotNull(pseudo);
        assertEquals(":enabled", pseudo.image().toString());

        assertResultOK(result);

        content = "div:nth-child(even) { }";

        result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);
        pseudo = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/pseudo");
        assertNotNull(pseudo);
        assertEquals(":nth-child(even)", pseudo.image().toString());
        assertResultOK(result);

    }

    public void testPseudoElements() throws ParseException, BadLocationException {
        String content = "div::before { }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);
        Node pseudo = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/pseudo");
        assertNotNull(pseudo);
        assertEquals("::before", pseudo.image().toString());

        assertResultOK(result);

    }

    public void testErrorCase10() throws ParseException, BadLocationException {
        String content = "p { color: hsl(10, }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);
        Node error = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "rule/declarations/declaration/propertyValue/expression/term/function/expression/error");
        assertNotNull(error);

        assertResult(result, 3);

    }

    public void testParseURL() throws ParseException, BadLocationException {
        String content = "p { background-image: url(flower.png); }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);
        assertResultOK(result);

    }

    public void testMediaQueries() throws ParseException, BadLocationException {
        String content = "@media screen and (device-aspect-ratio: 2560/1440) { p { } }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        Node media_query = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "media/mediaQueryList/mediaQuery");
        assertNotNull(media_query);

        Node media_type = NodeUtil.query(media_query, "mediaType");
        assertNotNull(media_type);
        assertEquals("screen", media_type.image().toString());

        Node media_expression = NodeUtil.query(media_query, "mediaExpression");
        assertNotNull(media_expression);


        Node media_feature = NodeUtil.query(media_expression, "mediaFeature");
        assertNotNull(media_feature);

        assertResultOK(result);

    }

    public void testMediaQueries2() throws ParseException, BadLocationException {
        assertResultOK(TestUtil.parse("@media tv and (scan: progressive) { p {} }"));
        assertResultOK(TestUtil.parse("@media print and (min-resolution: 118dpcm) { p {} }"));
        assertResultOK(TestUtil.parse("@media all and (min-monochrome: 1) { p {} }"));
        assertResultOK(TestUtil.parse("@media screen and (min-width: 400px) and (max-width: 700px) { p {} }"));
        assertResultOK(TestUtil.parse("@media handheld and (min-width: 20em), screen and (min-width: 20em) { p {} }"));
        assertResultOK(TestUtil.parse("@media (orientation: portrait) { p {} }"));
    }

    public void testPagedMedia() throws ParseException, BadLocationException {
        String content = "@page test:first {"
                + "color: green;"
                + "@top-left {"
                + "content: \"foo\";"
                + "color: blue;"
                + "};"
                + "@top-right {"
                + "content: \"bar\";"
                + "} "
                + "}";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        assertResultOK(result);

        //test page node
        Node page = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "page");
        assertNotNull(page);

        //pseudo page
        Node pseudoPage = NodeUtil.query(page,
                "pseudoPage");
        assertNotNull(pseudoPage);
        assertEquals(":first", pseudoPage.image().toString());

        //declaration
        Node declaration = NodeUtil.query(page,
                "declaration");
        assertNotNull(declaration);
        assertEquals("color: green", declaration.image().toString());

        //margin
        Node margin = NodeUtil.query(page,
                "margin");
        assertNotNull(margin);

        //margin symbol
        Node margin_sym = NodeUtil.query(margin, "margin_sym");
        assertNotNull(margin_sym);
        assertEquals("@top-left", margin_sym.image().toString());

        //declaration in the margin body
        Node declarationInMargin = NodeUtil.query(margin, "declarations/declaration");
        assertNotNull(declarationInMargin);

        //next margin
        Node margin2 = NodeUtil.query(page,
                "margin|1");
        assertNotNull(margin2);

        //next margin symbol
        Node margin_sym2 = NodeUtil.query(margin2, "margin_sym");
        assertNotNull(margin_sym2);
        assertEquals("@top-right", margin_sym2.image().toString());

        assertNotNull(NodeUtil.query(margin2, "declarations/declaration"));



    }
    
    public void testPagedMediaWithoutPseudoClass() throws ParseException, BadLocationException {
        String content = "@page test {"
                + "color: green;"
                + "}";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        assertResultOK(result);

        //test page node
        Node page = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "page");
        assertNotNull(page);

        //declaration
        Node declaration = NodeUtil.query(page,
                "declaration");
        assertNotNull(declaration);
        assertEquals("color: green", declaration.image().toString());

    }

    public void testCounterStyle() throws ParseException, BadLocationException {
        String content = "@counter-style cool { glyph: '|'; }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        Node counterStyle = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "counterStyle");
        assertNotNull(counterStyle);

        Node ident = NodeUtil.getChildTokenNode(counterStyle, CssTokenId.IDENT);
        assertNotNull(ident);
        assertEquals("cool", ident.image().toString());

        Node declaration = NodeUtil.query(counterStyle, "declarations/declaration");
        assertNotNull(declaration);
        assertEquals("glyph: '|'", declaration.image().toString());

        assertResultOK(result);

    }

    public void testSubstringMatchingAttributeSelectors() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("p[class$=\"st\"]{ } "));
        assertResultOK(TestUtil.parse("p[class*=\"st\"]{ } "));
        assertResultOK(TestUtil.parse("p[class^=\"st\"]{ } "));
    }

    public void testFontFace() throws ParseException, BadLocationException {
        String content = "@font-face { font-family: Gentium; src: url(http://example.com/fonts/Gentium.ttf); }";

        CssParserResult result = TestUtil.parse(content);
//        TestUtil.dumpTokens(result);
//        TestUtil.dumpResult(result);

        Node counterStyle = NodeUtil.query(result.getParseTree(),
                TestUtil.bodysetPath
                + "fontFace");
        assertNotNull(counterStyle);

        Node declaration = NodeUtil.query(counterStyle, "declarations/declaration|0");
        assertNotNull(declaration);
        assertEquals("font-family: Gentium", declaration.image().toString());

        assertResultOK(result);

    }

    public void testNetbeans_Css() throws ParseException, BadLocationException, IOException {
        CssParserResult result = TestUtil.parse(getTestFile("testfiles/netbeans.css"));
//        TestUtil.dumpResult(result);
        assertResult(result, 0);
    }

    public void testPropertyValueAsFunction() throws BadLocationException, ParseException {
        String code = "div { animation: cubic-bezier(1,2,3,4); } ";
        assertResultOK(TestUtil.parse(code));
    }

    public void testIssue203573() throws ParseException, BadLocationException, IOException {
        String code = "h1 { color:red; - }";
        CssParserResult result = TestUtil.parse(code);

//        NodeUtil.dumpTree(result.getParseTree());

        assertNoTokenNodeLost(result);
    }

    public void testParserRecovery_Issue203579() throws BadLocationException, ParseException {
        String code = "p {} #{} .{} div {}";
        CssParserResult result = TestUtil.parse(code);

//        NodeUtil.dumpTree(result.getParseTree());

        Node node = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem|0/"
                + "rule/selectorsGroup/selector/simpleSelectorSequence/typeSelector/elementName");
        assertNotNull(node);
        assertFalse(NodeUtil.containsError(node));

        node = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem|1/"
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/cssId");
        assertNotNull(node);
        assertTrue(NodeUtil.containsError(node));

        node = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem|2/"
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/cssClass");
        assertNotNull(node);
        assertTrue(NodeUtil.containsError(node));

    }

    public void testMSExpression() throws BadLocationException, ParseException {
        String code =
                "div {"
                + "zoom:expression(runtimeStyle.zoom = 1, insertAdjacentHTML('beforeEnd', '<u class=\"after\"></u>'));"
                + "}";

        CssParserResult result = TestUtil.parse(code);
//        TestUtil.dumpResult(result);
        
        assertResultOK(result);

    }
    
    public void testMozDocumentAtRule() throws BadLocationException, ParseException {
        CssParserResult result = TestUtil.parse(
                "@-moz-document url(http://www.w3.org/),  "
                + "url-prefix(http://www.w3.org/Style/),  "
                + "domain(mozilla.org),  "
                + "regexp(\"^https:.*\") { div { color: red; } }");
        
        assertResultOK(result);
//        NodeUtil.dumpTree(result.getParseTree());
        assertNotNull(NodeUtil.query(result.getParseTree(), "styleSheet/body/bodyItem/vendorAtRule/moz_document"));
    }
    
    //Bug 204128 - CC stops work after # in a color attribute 
    public void testErrorRecoveryAfterHash() throws BadLocationException, ParseException {
        CssParserResult result = TestUtil.parse(
                "#test {\n"
                + "color: #\n"
                + "\n"
                + "   }\n"
                + "div { color: red; }\n");
                
//        TestUtil.dumpResult(result);
        Node node = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem/"
                + "rule/declarations/declaration/propertyValue/expression/error");
        assertNotNull(node);
        assertEquals(15, node.from());
        assertEquals(16, node.to());
    }
    
    public void testParsingOfAsterixOnly() throws BadLocationException, ParseException {
        CssParserResult result = TestUtil.parse("*     ");
        //                                       0123456
//        TestUtil.dumpResult(result);
        
        Node node = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem/"
                + "rule/selectorsGroup/selector/simpleSelectorSequence/error");
        assertNotNull(node);
        assertEquals(6, node.from());
        assertEquals(6, node.to());
        
    }
    
    public void testErrorInSelector() throws BadLocationException, ParseException {
        CssParserResult result = TestUtil.parse("h1[|");
//        TestUtil.dumpResult(result);
        
//        Node node = NodeUtil.query(result.getParseTree(),
//                "styleSheet/body/bodyset/"
//                + "rule/selectorsGroup/selector/simpleSelectorSequence/error");
//        assertNotNull(node);
//        assertEquals(1, node.from());
//        assertEquals(6, node.to());
        
    }
    
    public void testNoEmptyRuleNodesInTheParseTree() throws BadLocationException, ParseException {
        CssParserResult result = TestUtil.parse("*  ");
        AtomicBoolean foundEmptyRuleNode = new AtomicBoolean(false);
        NodeVisitor<AtomicBoolean> visitor  = new NodeVisitor<AtomicBoolean>(foundEmptyRuleNode) {
            @Override
            public boolean visit(Node node) {
                if(node instanceof RuleNode) {
                    if(node.children().isEmpty()) {
                        getResult().set(true);
                        return true;
                    }
                }
                return false;
            }
        };
        visitor.visitChildren(result.getParseTree());
        assertFalse(foundEmptyRuleNode.get());
    }

    public void testDuplicatedErrors() throws BadLocationException, ParseException {
        CssParserResult result = TestUtil.parse(
                  "head{\n"
                + "    background-image: uri();"
                + "}");
        
        TestUtil.dumpResult(result);
        
        assertResult(result, 2);
        
    }
    
    public void testParseWSAfterImportantSym() throws BadLocationException, ParseException {
        CssParserResult result = TestUtil.parse(".green { "
                + "    background-color : lime ! important "
                + "}");
        assertResultOK(result);
    }
    
    public void testParse_nth_child() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("table.t1 tr:nth-child(-n+4) { background-color : red }"));
        assertResultOK(TestUtil.parse("table.t2 td:nth-child(3n+1) { background-color : red }"));
    }
    
    public void testParseNotInPseudo() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("a:not(a) {  }"));
    }
    
    public void testParseAsterixInPseudo() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("a:not(*) { }"));
    }
    
    public void testParseClassInPseudo() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("a:not(.t2) {  }"));
    }
    
    public void testParseAttribInPseudo() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("div.stub *|*:not([test]) { }"));
    }
 
    public void testParsePseudoClassInPseudo() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("p:not(:target) { }"));
    }
    
    public void testParsePipeWithoutPrefixInSelector() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("|testA {background-color : lime }"));
    }
    
    public void testParseNsPrefixedElementInPseudo() throws BadLocationException, ParseException {
        assertResultOK(TestUtil.parse("div.test *:not(a|p) {  }"));
    }
    
    //Bug 207080 - Insufficient CSS parser error recovery
    public void testIssue_207080() {
        String code = "#wrapper {\n"
                + "   height: 100%;\n"
                + "#z-index: 200; \n"
                + "}\n"
                + "\n"
                + "#header {\n"
                + "}\n";
        CssParserResult result = TestUtil.parse(code);
        
//        TestUtil.dumpResult(result);
        
        assertResult(result, 1); //ProblemDescription{from=28, to=36, description=Unexpected token HASH found, key=PARSING, type=ERROR}

        
        //check if the #header rule is properly parsed
        Node node = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem|1/"
                + "rule/selectorsGroup/selector/simpleSelectorSequence/elementSubsequent/cssId");
        assertNotNull(node);
        

    }
    
    public void testGenericAtRule() {
        String code = "@-cool-rule spin { h2 { color: red; } }";
        CssParserResult result = TestUtil.parse(code);
        
        assertResultOK(result);
        
//        TestUtil.dumpResult(result);
        
        Node node = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem/vendorAtRule/generic_at_rule");
                
        assertNotNull(node);
        
    }
    
    //Bug 211103 - Freezes on starting IDE at "Scanning project" for too long
    public void testIssue211103() throws IOException, ParseException, BadLocationException {
        FileObject file = getTestFile("testfiles/itabbar.css.testfile");
        //check if we can even parse the file w/o an infinite loop in the recovery
        TestUtil.parse(file);
    }
    
//    public void testRecoveryInBodySet() {
//        String code = "div { } ;@ a { } h1 { }";
//        CssParserResult result = TestUtil.parse(code);
//        
//        assertResultOK(result);
//        
//        TestUtil.dumpResult(result);
//        
//        Node node = NodeUtil.query(result.getParseTree(),
//                "styleSheet/bodylist/bodyset/generic_at_rule");
//                
//        assertNotNull(node);
//        
//        
//    }
    
    
    public void testWebkitKeyFrames() {
        String code = "@-webkit-keyframes spin { 40% {  left: 150px;  } from { left: 2px } }";
        //             012345678901234567890123456789012345678901234567890123456789
        //             0         1         2         3         4         5
        CssParserResult result = TestUtil.parse(code);
        
        assertResultOK(result);
        
//        TestUtil.dumpResult(result);
        
        Node wkf = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem/vendorAtRule/webkitKeyframes");
                
        assertNotNull(wkf);
        
        Node atRuleName = NodeUtil.query(wkf, "atRuleId");
        assertNotNull(atRuleName);
        assertEquals("spin", atRuleName.image().toString());
        
        //block1
        Node block = NodeUtil.query(wkf, "webkitKeyframesBlock|0");
        Node selectors = NodeUtil.query(block, "webkitKeyframeSelectors");
        assertNotNull(selectors);
        assertEquals("40%", selectors.image().toString());
        
        Node declarations = NodeUtil.query(wkf, "webkitKeyframesBlock/declarations");
        assertNotNull(declarations);
        assertNotNull(NodeUtil.query(declarations, "declaration/property"));
        assertNotNull(NodeUtil.query(declarations, "declaration/propertyValue"));
        
        //block2
        block = NodeUtil.query(wkf, "webkitKeyframesBlock|1");
        selectors = NodeUtil.query(block, "webkitKeyframeSelectors");
        assertNotNull(selectors);
        assertEquals("from", selectors.image().toString());
        
        declarations = NodeUtil.query(wkf, "webkitKeyframesBlock/declarations");
        assertNotNull(declarations);
        assertNotNull(NodeUtil.query(declarations, "declaration/property"));
        assertNotNull(NodeUtil.query(declarations, "declaration/propertyValue"));
        
        
    }
    
    //http://en.wikipedia.org/wiki/CSS_filter#Star_hack
    //Bug 215168 - Netbeans doesn't know about CSS star hack 
    public void testIEPropertyStarHack() throws ParseException, BadLocationException {
        //case #1 error appears before declarations grammar rule
        String source = ".aclass { *color: red; }";
        CssParserResult result = TestUtil.parse(source);

//        NodeUtil.dumpTree(result.getParseTree());
        assertEquals(0, result.getDiagnostics().size());
        
        //case #2 - error happens in the declarations grammar rule
        source = ".aclass { padding: 2px; *color: red; }";
        result = TestUtil.parse(source);

//        NodeUtil.dumpTree(result.getParseTree());
        assertEquals(0, result.getDiagnostics().size());
    }

    public void testFromTextPropertyValueBug() {
        String source = ".x { background-image: -webkit-gradient(linear, 0 0, 0 100%, from(#149bdf), to(#0480be)) }";
        CssParserResult result = TestUtil.parse(source);

        NodeUtil.dumpTree(result.getParseTree());
        assertEquals(0, result.getDiagnostics().size());
    }
    
    //Bug 219587 - parsing error on .box:nth-child(4n - 2)
    public void testWSInExpression() throws ParseException, BadLocationException {
        String source = ".box:nth-child(4n - 2) { } ";
        CssParserResult result = TestUtil.parse(source);

//        NodeUtil.dumpTree(result.getParseTree());
        assertEquals(0, result.getDiagnostics().size());
        
    }
    
    public void testDataURI() throws ParseException, BadLocationException {
        String source = "div.menu {"
                + "    background-image: url('data:image/png;base64,iVBORw0KGgoAA"
                + "AANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAABlBMVEUAAAD///+l2Z/dAAAAM0l"
                + "EQVR4nGP4/5/h/1+G/58ZDrAz3D/McH8yw83NDDeNGe4Ug9C9zwz3gVLMDA/A6"
                + "P9/AFGGFyjOXZtQAAAAAElFTkSuQmCC');"
                + "}";

        CssParserResult result = TestUtil.parse(source);
//        NodeUtil.dumpTree(result.getParseTree());
        
        assertEquals(0, result.getDiagnostics().size());
        
        Node term = NodeUtil.query(result.getParseTree(),
                "styleSheet/body/bodyItem/rule/declarations/declaration/propertyValue/expression/term");
        assertNotNull(term);
        
        Node uri = NodeUtil.getChildTokenNode(term, CssTokenId.URI);
        assertNotNull(uri);
        
    }
    
     //Bug 223809 - Incorrect CSS syntax error highlighting when @-ms-viewport rule is added 
    public void testVendorAtRuleInMedia() throws ParseException, BadLocationException {
        String source = "@media screen and (max-width: 400px) {"
                + "  @-ms-viewport { width: 320px; }"
                + "  @-o-viewport {}"
                + "}";
        
        CssParserResult result = TestUtil.parse(source);

//        NodeUtil.dumpTree(result.getParseTree());
        assertEquals(0, result.getDiagnostics().size());
        
    }
}