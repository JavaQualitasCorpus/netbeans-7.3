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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.jsf.editor.hints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.lib.editor.util.CharSequenceUtilities;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.el.lexer.api.ELTokenId;
import org.netbeans.modules.html.editor.api.gsf.HtmlParserResult;
import org.netbeans.modules.html.editor.lib.api.elements.Attribute;
import org.netbeans.modules.html.editor.lib.api.elements.AttributeFilter;
import org.netbeans.modules.html.editor.lib.api.elements.CloseTag;
import org.netbeans.modules.html.editor.lib.api.elements.Element;
import org.netbeans.modules.html.editor.lib.api.elements.ElementType;
import org.netbeans.modules.html.editor.lib.api.elements.ElementUtils;
import org.netbeans.modules.html.editor.lib.api.elements.ElementVisitor;
import org.netbeans.modules.html.editor.lib.api.elements.Node;
import org.netbeans.modules.html.editor.lib.api.elements.OpenTag;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.web.common.api.LexerUtils;
import org.netbeans.modules.web.jsf.editor.JsfSupportImpl;
import org.netbeans.modules.web.jsf.editor.JsfUtils;
import org.netbeans.modules.web.jsf.editor.facelets.AbstractFaceletsLibrary;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author marekfukala
 */
public class LibraryDeclarationChecker extends HintsProvider {

    @Override
    public List<Hint> compute(RuleContext context) {
        List<Hint> hints = new ArrayList<Hint>();

        checkLibraryDeclarations(hints, context);

        return hints;
    }

    //check the namespaces declaration:
    //1. if the declared library is available
    //2. if the declared library is used + remove unused declaration hint
    //3. if there are usages of undeclared library 
    //    + hint to add the declaration (if library available)
    //        - by default prefix
    //        - or search all the libraries for such component and offer the match/es
    //
    private void checkLibraryDeclarations(final List<Hint> hints, final RuleContext context) {
        final HtmlParserResult result = (HtmlParserResult) context.parserResult;
        final Snapshot snapshot = result.getSnapshot();

        //find all usages of composite components tags for this page
        Collection<String> declaredNamespaces = result.getNamespaces().keySet();
        final Collection<AbstractFaceletsLibrary> declaredLibraries = new ArrayList<AbstractFaceletsLibrary>();
        JsfSupportImpl jsfSupport = JsfSupportImpl.findFor(context.parserResult.getSnapshot().getSource());
        Map<String, AbstractFaceletsLibrary> libs = Collections.emptyMap();
        if (jsfSupport != null) {
            libs = jsfSupport.getLibraries();
        }

        //Find the namespaces declarations itself
        //a.take the html AST & the AST for undeclared components
        //b.search for nodes with xmlns attribute
        //ugly, grr, the whole namespace support needs to be fixed
        final Map<String, Attribute> namespace2Attribute = new HashMap<String, Attribute>();
        Node root = result.root();
        final CharSequence docText = getSourceText(snapshot.getSource());
        
        ElementVisitor namespacesCollector = new ElementVisitor() {

            @Override
            public void visit(Element node) {
                OpenTag openTag = (OpenTag)node;
                //put all NS attributes to the namespace2Attribute map for #1.
                Collection<Attribute> nsAttrs = openTag.attributes(new AttributeFilter() {

                    @Override
                    public boolean accepts(Attribute attribute) {
                        if(attribute.unquotedValue() == null) {
                            return false;
                        }
                        CharSequence nsPrefix = attribute.namespacePrefix();
                        if(nsPrefix == null) {
                            return false;
                        }
                        
                        return LexerUtils.equals("xmlns", nsPrefix, true, true); //NOI18N
                    }
                });
                
                for (Attribute attr : nsAttrs) {
                    namespace2Attribute.put(attr.unquotedValue().toString(), attr);
                }
            }

        };

        ElementUtils.visitChildren(root, namespacesCollector, ElementType.OPEN_TAG);
        Node undeclaredComponentsTreeRoot = result.rootOfUndeclaredTagsParseTree();
        if(undeclaredComponentsTreeRoot != null) {
            ElementUtils.visitChildren(undeclaredComponentsTreeRoot, namespacesCollector, ElementType.OPEN_TAG);

            //check for undeclared tags
            ElementUtils.visitChildren(undeclaredComponentsTreeRoot, new ElementVisitor() {

                @Override
                public void visit(Element node) {
                    OpenTag openTag = (OpenTag)node;
                    String namespacePrefix = openTag.namespacePrefix().toString();
                    if (namespacePrefix != null) {
                        //3. check for undeclared components

                        List<HintFix> fixes = new ArrayList<HintFix>();
                        List<AbstractFaceletsLibrary> libs = getLibsByPrefix(context, namespacePrefix);

                        for (AbstractFaceletsLibrary lib : libs){
                            FixLibDeclaration fix = new FixLibDeclaration(context.doc, namespacePrefix, lib);
                            fixes.add(fix);
                        }

                        //this itself means that the node is undeclared since
                        //otherwise it wouldn't appear in the pure html parse tree
                        hints.add(new Hint(ERROR_RULE_BADGING,
                                NbBundle.getMessage(HintsProvider.class, "MSG_UNDECLARED_COMPONENT", openTag.name().toString()), //NOI18N
                                context.parserResult.getSnapshot().getSource().getFileObject(),
                                JsfUtils.createOffsetRange(snapshot, docText, node.from(), node.from() + openTag.name().length() + 1 /* "<".length */),
                                fixes, DEFAULT_ERROR_HINT_PRIORITY));
                        
                        //put the hint to the close tag as well
                        CloseTag matchingCloseTag = openTag.matchingCloseTag();
                        if(matchingCloseTag != null) {
                            hints.add(new Hint(ERROR_RULE_BADGING,
                                    NbBundle.getMessage(HintsProvider.class, "MSG_UNDECLARED_COMPONENT", openTag.name().toString()), //NOI18N
                                    context.parserResult.getSnapshot().getSource().getFileObject(),
                                    JsfUtils.createOffsetRange(snapshot, docText, matchingCloseTag.from(), matchingCloseTag.to()),
                                    fixes, DEFAULT_ERROR_HINT_PRIORITY));
                        }
                        
                    }
                }
            }, ElementType.OPEN_TAG);
        }

        for (String namespace : declaredNamespaces) {
            AbstractFaceletsLibrary lib = libs.get(namespace);
            if (lib != null) {
                declaredLibraries.add(lib);
            } else {
                //1. report error - missing library for the declaration
                Attribute attr = namespace2Attribute.get(namespace);
                if (attr != null) {
                    //found the declaration, mark as error
                    Hint hint = new Hint(ERROR_RULE_BADGING,
                            NbBundle.getMessage(HintsProvider.class, "MSG_MISSING_LIBRARY", namespace), //NOI18N
                            context.parserResult.getSnapshot().getSource().getFileObject(),
                            JsfUtils.createOffsetRange(snapshot, docText, attr.nameOffset(), attr.valueOffset() + attr.value().length()),
                            Collections.<HintFix>emptyList(), DEFAULT_ERROR_HINT_PRIORITY);
                    hints.add(hint);
                }
            }
        }

        //2. find for unused declarations
        final Collection<OffsetRange> ranges = new ArrayList<OffsetRange>();
        for (AbstractFaceletsLibrary lib : declaredLibraries) {
            Node rootNode = result.root(lib.getNamespace());
            if (rootNode == null) {
                continue; //no parse result for this namespace, the namespace is not declared
            }
            final int[] usages = new int[1];
            ElementUtils.visitChildren(rootNode, new ElementVisitor() {
                @Override
                public void visit(Element node) {
                    usages[0]++;
                }
            }, ElementType.OPEN_TAG);

            usages[0] += isFunctionLibraryPrefixUsedInEL(context, lib, docText) ? 1 : 0;

            if (usages[0] == 0) {
                //unused declaration
                Attribute declAttr = namespace2Attribute.get(lib.getNamespace());
                if (declAttr != null) {
                    int from = declAttr.nameOffset();
                    int to = declAttr.valueOffset() + declAttr.value().length();
                    
                    OffsetRange documentRange = JsfUtils.createOffsetRange(snapshot, docText, from, to);
                    ranges.add(documentRange);
                }

            }
        }
        
        //generate remove all unused declarations
        for (OffsetRange range : ranges) {
            List<HintFix> fixes;
            try {
                //do not create any fixes if there's no document
                fixes = context.doc != null
                        ? Arrays.asList(new HintFix[]{
                            new RemoveUnusedLibraryDeclarationHintFix(context.doc, createPositionRange(context, range)), //the only occurance
                            new RemoveUnusedLibrariesDeclarationHintFix(context.doc, createPositionRanges(context, ranges))
                        }) //remove all
                        : Collections.<HintFix>emptyList();
            } catch (BadLocationException ex) {
                //ignore
                fixes = Collections.emptyList();
            }
            
            Hint hint = new Hint(DEFAULT_WARNING_RULE,
                    NbBundle.getMessage(HintsProvider.class, "MSG_UNUSED_LIBRARY_DECLARATION", docText.subSequence(range.getStart(), range.getEnd())), //NOI18N
                    context.parserResult.getSnapshot().getSource().getFileObject(),
                    range,
                    fixes, DEFAULT_ERROR_HINT_PRIORITY);

            hints.add(hint);
        }

    }
    
    private static PositionRange createPositionRange(RuleContext context, OffsetRange offsetRange) throws BadLocationException {
        return new PositionRange(context, offsetRange.getStart(), offsetRange.getEnd());
    }
    
    private static Collection<PositionRange> createPositionRanges(RuleContext context, Collection<OffsetRange> offsetRanges) throws BadLocationException {
        Collection<PositionRange> ranges = new ArrayList<PositionRange>();
        for(OffsetRange offsetRange : offsetRanges) {
            ranges.add(createPositionRange(context, offsetRange));
        }
        return ranges;
    }
    
    private static List<AbstractFaceletsLibrary> getLibsByPrefix(RuleContext context, String prefix){
        List<AbstractFaceletsLibrary> libs = new ArrayList<AbstractFaceletsLibrary>();
        JsfSupportImpl sup = JsfSupportImpl.findFor(context.parserResult.getSnapshot().getSource());

        if (sup != null){
            //eliminate the library duplicities - see the sup.getLibraries() doc
            for (AbstractFaceletsLibrary lib : new HashSet<AbstractFaceletsLibrary>(sup.getLibraries().values())){
                if (prefix.equals(lib.getDefaultPrefix())){
                    libs.add(lib);
                }
            }
        }

        return libs;
    }

    //find all embedded EL token sequences in the source code and check if the
    //prefix is used in any of them
    private boolean isFunctionLibraryPrefixUsedInEL(RuleContext context, AbstractFaceletsLibrary lib, CharSequence sourceText) {
        String libraryPrefix = ((HtmlParserResult)context.parserResult).getNamespaces().get(lib.getNamespace());

        TokenHierarchy<CharSequence> th = TokenHierarchy.create(sourceText, Language.find("text/xhtml"));
        TokenSequence<?> ts = th.tokenSequence();
        ts.moveStart();
        while(ts.moveNext()) {
            TokenSequence<ELTokenId> elts = ts.embeddedJoined(ELTokenId.language());
            if(elts != null) {
                //check the EL expression for the function library prefix usages
                elts.moveStart();
                while(elts.moveNext()) {
                    if(elts.token().id() == ELTokenId.TAG_LIB_PREFIX && CharSequenceUtilities.equals(libraryPrefix, elts.token().text())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static class RemoveUnusedLibraryDeclarationHintFix extends RemoveUnusedLibrariesDeclarationHintFix {

        public RemoveUnusedLibraryDeclarationHintFix(BaseDocument document, PositionRange range) {
            super(document, Collections.<PositionRange>singletonList(range));
        }

        @Override
        public String getDescription() {
            return NbBundle.getMessage(HintsProvider.class, "MSG_HINTFIX_REMOVE_UNUSED_LIBRARY_DECLARATION");
        }

    }

    private static class RemoveUnusedLibrariesDeclarationHintFix implements HintFix {

        protected Collection<PositionRange> ranges = new ArrayList<PositionRange>();
        protected BaseDocument document;

        public RemoveUnusedLibrariesDeclarationHintFix(BaseDocument document, Collection<PositionRange> ranges) {
            this.document = document;
            this.ranges = ranges;
        }

        @Override
        public String getDescription() {
            return NbBundle.getMessage(HintsProvider.class, "MSG_HINTFIX_REMOVE_ALL_UNUSED_LIBRARIES_DECLARATION");
        }

        @Override
        public void implement() throws Exception {
            document.runAtomic(new Runnable() {

                @Override
                public void run() {
                    try {
                        for (PositionRange range : ranges) {
                            int from = range.getFrom();
                            int to = range.getTo();
                            //check if the line before the area is white
                            int lineBeginning = Utilities.getRowStart(document, from);
                            int firstNonWhite = Utilities.getFirstNonWhiteBwd(document, from);
                            if (lineBeginning > firstNonWhite) {
                                //delete the white content before the area inclusing the newline
                                from = lineBeginning - 1; // (-1 => includes the line end)
                            }
                            document.remove(from, to - from);
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }

                }
            });
        }

        @Override
        public boolean isSafe() {
            return true;
        }

        @Override
        public boolean isInteractive() {
            return false;
        }
    }

    private static final class PositionRange {

        private Position from, to;

        public PositionRange(RuleContext context, int from, int to) throws BadLocationException {
            //the constructor will simply throw BLE when the embedded to source offset conversion fails (returns -1)
            this.from = context.doc.createPosition(from);
            this.to = context.doc.createPosition(to);
        }

        public int getFrom() {
            return from.getOffset();
        }

        public int getTo() {
            return to.getOffset();
        }
    }
}