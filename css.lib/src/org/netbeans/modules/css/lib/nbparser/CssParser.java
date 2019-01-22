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
package org.netbeans.modules.css.lib.nbparser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.css.lib.ExtCss3Lexer;
import org.netbeans.modules.css.lib.ExtCss3Parser;
import org.netbeans.modules.css.lib.api.CssParserResult;
import javax.swing.event.ChangeListener;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.css.lib.AbstractParseTreeNode;
import org.netbeans.modules.css.lib.NbParseTreeBuilder;
import org.netbeans.modules.css.lib.api.ProblemDescription;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.netbeans.modules.web.common.api.Lines;
import org.openide.filesystems.FileObject;
import org.openide.util.CharSequences;

/**
 *
 * @author mfukala@netbeans.org
 */
public class CssParser extends Parser {
    
    private static final CharSequence TEMPLATING_MARK = "@@@"; //NOI18N
    
    private static final Logger LOG = Logger.getLogger(CssParser.class.getSimpleName());
    private CssParserResult result;

    public static CssParserResult parse(Snapshot snapshot) throws ParseException {
        if(snapshot == null) {
            return null;
        }
        FileObject fo = snapshot.getSource().getFileObject();
        String fileName = fo == null ? "no file" : fo.getPath(); //NOI18N
        LOG.log(Level.FINE, "Parsing {0} ", fileName); //NOI18N
        long start = System.currentTimeMillis();
        try {
            CharSequence source = snapshot.getText();
            ExtCss3Lexer lexer = new ExtCss3Lexer(source);
            TokenStream tokenstream = new CommonTokenStream(lexer);
            NbParseTreeBuilder builder = new NbParseTreeBuilder(source);
            ExtCss3Parser parser = new ExtCss3Parser(tokenstream, builder);
            parser.styleSheet();

            AbstractParseTreeNode tree = builder.getTree();
            List<ProblemDescription> problems = new ArrayList<ProblemDescription>();
            //add lexer issues
            problems.addAll(lexer.getProblems());
            //add parser issues
            problems.addAll(builder.getProblems());
            
            

            return new CssParserResult(snapshot, tree, filterTemplatingProblems(snapshot, problems));
        } catch (RecognitionException ex) {
            throw new ParseException(String.format("Error parsing %s snapshot.", snapshot), ex); //NOI18N
        } finally {
            long end = System.currentTimeMillis();
            LOG.log(Level.FINE, "Parsing of {0} took {1} ms.", new Object[]{fileName, (end - start)}); //NOI18N
        }
    }
    
    
    //filtering out problems caused by templating languages
    private static List<ProblemDescription> filterTemplatingProblems(Snapshot snapshot, List<ProblemDescription> problems) {
        MimePath mimePath = snapshot.getMimePath();
        CharSequence text = snapshot.getText();
        if(mimePath.size() <= 2 || mimePath.size() == 3 && mimePath.getMimeType(0).equals("text/xhtml")) { //NOI18N
            //text/css
            //or
            //text/html/text/css
            //or
            //hack for the fake text/xhtml language:
            //for .xhtml files the mime is text/xhtml/text/html/text/css
            return problems;
        } else {
            //typically text/php/text/html/text/css
            List<ProblemDescription> filtered = new ArrayList<ProblemDescription>(problems.size());
            for(ProblemDescription p : problems) {
                //XXX Idealy the filtering context should be dependent on the enclosing node
                //sg. like if there's a templating error in an declaration - search the whole
                //declaration for the templating mark. 
                //
                //Using some simplification - line context, though some nodes may span multiple
                //lines and the templating mark may not necessarily be at the line with the error.
                //
                //so find line bounds...
                
                //the "premature end of file" error has position pointing after the last char (=text.length())!
                if(p.getFrom() == text.length()) {
                    continue; //consider this as hidden error
                }
                
                int from, to;
                for(from = p.getFrom(); from > 0;from--) {
                    char c = text.charAt(from);
                    if(c == '\n') {
                        break;
                    }
                }
                for(to = p.getTo(); to < text.length(); to++) {
                    char c = text.charAt(to);
                    if(c == '\n') {
                        break;
                    }
                }
                //check if there's the templating mark (@@@) in the context
                CharSequence img = snapshot.getText().subSequence(from, to);
                if(CharSequences.indexOf(img, TEMPLATING_MARK) == -1) {
                    filtered.add(p);
                }
            }
            return filtered;
        }
    }

    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
        result = parse(snapshot);
    }

    @Override
    public ParserResult getResult(Task task) throws ParseException {
        return result;
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {
        //no-op
    }

    @Override
    public void removeChangeListener(ChangeListener changeListener) {
        //no-op
    }
}