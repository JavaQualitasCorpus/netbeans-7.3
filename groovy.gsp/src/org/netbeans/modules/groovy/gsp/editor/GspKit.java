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

package org.netbeans.modules.groovy.gsp.editor;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.lib.editor.util.CharSequenceUtilities;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.ext.ExtKit.ExtDefaultKeyTypedAction;
import org.netbeans.modules.csl.api.DeleteToNextCamelCasePosition;
import org.netbeans.modules.csl.api.DeleteToPreviousCamelCasePosition;
import org.netbeans.modules.csl.api.InstantRenameAction;
import org.netbeans.modules.csl.api.NextCamelCasePosition;
import org.netbeans.modules.csl.api.PreviousCamelCasePosition;
import org.netbeans.modules.csl.api.SelectCodeElementAction;
import org.netbeans.modules.csl.api.SelectNextCamelCasePosition;
import org.netbeans.modules.csl.api.SelectPreviousCamelCasePosition;
import org.netbeans.modules.html.editor.api.HtmlKit;
import org.netbeans.modules.groovy.gsp.lexer.GspTokenId;
import org.openide.util.Exceptions;

/**
 * Editor kit implementation for GSP content type
 *
 * @todo rewrite from Ruby to Groovy specifics
 * 
 * @author Marek Fukala
 * @author Tor Norbye
 * @author Martin Adamek
 */

public class GspKit extends HtmlKit {
    @Override
    public org.openide.util.HelpCtx getHelpCtx() {
        return new org.openide.util.HelpCtx(GspKit.class);
    }
    
    static final long serialVersionUID =-1381945567613910297L;
        
    public GspKit(){
        super(GspTokenId.MIME_TYPE);
    }
    
    @Override
    public String getContentType() {
        return GspTokenId.MIME_TYPE;
    }
    
    @Override
    protected DeleteCharAction createDeletePrevAction() {
        return new GspDeleteCharAction(deletePrevCharAction, false, super.createDeletePrevAction());
    }
    
    @Override
    protected ExtDefaultKeyTypedAction createDefaultKeyTypedAction() {
        return new GspDefaultKeyTypedAction(super.createDefaultKeyTypedAction());
    }

    @Override
    protected Action[] createActions() {
        Action[] superActions = super.createActions();
        
        return TextAction.augmentList(superActions, new Action[] {
            // TODO - also register a Tab key action which tabs out of <% %> if the caret is near the end
            // (Shift Enter inserts a line below the current - perhaps that's good enough)
//            new GspToggleCommentAction(),
            new SelectCodeElementAction(SelectCodeElementAction.selectNextElementAction, true),
            new SelectCodeElementAction(SelectCodeElementAction.selectPreviousElementAction, false),
            new NextCamelCasePosition(findAction(superActions, nextWordAction)),
            new PreviousCamelCasePosition(findAction(superActions, previousWordAction)),
            new SelectNextCamelCasePosition(findAction(superActions, selectionNextWordAction)),
            new SelectPreviousCamelCasePosition(findAction(superActions, selectionPreviousWordAction)),
            new DeleteToNextCamelCasePosition(findAction(superActions, removeNextWordAction)),
            new DeleteToPreviousCamelCasePosition(findAction(superActions, removePreviousWordAction)),
            new InstantRenameAction(),
         });
    }

    private static Action findAction(Action [] actions, String name) {
        for(Action a : actions) {
            Object nameObj = a.getValue(Action.NAME);
            if (nameObj instanceof String && name.equals(nameObj)) {
                return a;
            }
        }
        return null;
    }
    private boolean handleDeletion(BaseDocument doc, int dotPos) {
        if (dotPos > 0) {
            try {
                char ch = doc.getText(dotPos-1, 1).charAt(0);
                if (ch == '%') {
                    TokenHierarchy<Document> th = TokenHierarchy.get((Document)doc);
                    TokenSequence<?> ts = th.tokenSequence();
                    ts.move(dotPos);
                    if (ts.movePrevious()) {
                        Token<?> token = ts.token();
                        if (token.id() == GspTokenId.DELIMITER && ts.offset()+token.length() == dotPos && ts.moveNext()) {
                            token = ts.token();
                            if (token.id() == GspTokenId.DELIMITER && ts.offset() == dotPos) {
                                doc.remove(dotPos-1, 1+token.length());
                                return true;
                            }
                        }
                    }
                }
            } catch (BadLocationException ble) {
                Exceptions.printStackTrace(ble);
            }
        }
        
        return false;
    }

    private boolean handleInsertion(BaseDocument doc, Caret caret, char c) {
        int dotPos = caret.getDot();
        // Bracket matching on <% %>
        if (c == ' ' && dotPos >= 2) {
            try {
                String s = doc.getText(dotPos-2, 2);
                if ("%=".equals(s) && dotPos >= 3) { // NOI18N
                    s = doc.getText(dotPos-3, 3);
                }
                if ("<%".equals(s) || "<%=".equals(s)) { // NOI18N
                    doc.insertString(dotPos, "  ", null);
                    caret.setDot(dotPos+1);
                    return true;
                }
            } catch (BadLocationException ble) {
                Exceptions.printStackTrace(ble);
            }
            
            return false;
        }
        
        if ((dotPos > 0) && (c == '%' || c == '>')) {
            TokenHierarchy<Document> th = TokenHierarchy.get((Document)doc);
            TokenSequence<?> ts = th.tokenSequence();
            ts.move(dotPos);
            try {
                if (ts.moveNext() || ts.movePrevious()) {
                    Token<?> token = ts.token();
                    if (token.id() == GspTokenId.HTML && doc.getText(dotPos-1, 1).charAt(0) == '<') {
                        // See if there's anything ahead
                        int first = Utilities.getFirstNonWhiteFwd(doc, dotPos, Utilities.getRowEnd(doc, dotPos));
                        if (first == -1) {
                            doc.insertString(dotPos, "%%>", null); // NOI18N
                            caret.setDot(dotPos+1);
                            return true;
                        }
                    } else if (token.id() == GspTokenId.DELIMITER) {
                        String tokenText = token.text().toString();
                        if (tokenText.endsWith("%>")) { // NOI18N
                            // TODO - check that this offset is right
                            int tokenPos = (c == '%') ? dotPos : dotPos-1;
                            CharSequence suffix = DocumentUtilities.getText(doc, tokenPos, 2);
                            if (CharSequenceUtilities.textEquals(suffix, "%>")) { // NOI18N
                                caret.setDot(dotPos+1);
                                return true;
                            }
                        } else if (tokenText.endsWith("<")) {
                            // See if there's anything ahead
                            int first = Utilities.getFirstNonWhiteFwd(doc, dotPos, Utilities.getRowEnd(doc, dotPos));
                            if (first == -1) {
                                doc.insertString(dotPos, "%%>", null); // NOI18N
                                caret.setDot(dotPos+1);
                                return true;
                            }
                        }
                    } else if (token.id() == GspTokenId.GROOVY || token.id() == GspTokenId.GROOVY_EXPR && dotPos >= 1 && dotPos <= doc.getLength()-3) {
                        // If you type ">" one space away from %> it's likely that you typed
                        // "<% foo %>" without looking at the screen; I had auto inserted %> at the end
                        // and because I also auto insert a space without typing through it, you've now
                        // ended up with "<% foo %> %>". Let's prevent this by interpreting typing a ""
                        // right before %> as a duplicate for %>.   I can't just do this on % since it's
                        // quite plausible you'd have
                        //   <% x = %q(foo) %>  -- if I simply moved the caret to %> when you typed the
                        // % in %q we'd be in trouble.
                        String s = doc.getText(dotPos-1, 4);
                        if ("% %>".equals(s)) { // NOI18N
                            doc.remove(dotPos-1, 2);
                            caret.setDot(dotPos+1);
                            return true;
                        }
                    }
                }
            } catch (BadLocationException ble) {
                Exceptions.printStackTrace(ble);
            }
        }
        
        return false;
    }

    private class GspDefaultKeyTypedAction extends ExtDefaultKeyTypedAction {
        private ExtDefaultKeyTypedAction htmlAction;

        GspDefaultKeyTypedAction(ExtDefaultKeyTypedAction htmlAction) {
            this.htmlAction = htmlAction;
        }
        
        @Override
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            Caret caret = target.getCaret();
            BaseDocument doc = (BaseDocument)target.getDocument();
            String cmd = evt.getActionCommand();
            if (cmd.length() > 0) {
                char c = cmd.charAt(0);
                if (handleInsertion(doc, caret, c)) {
                    return;
                }
            }

            htmlAction.actionPerformed(evt, target);
        }
    }
    
    private class GspDeleteCharAction extends ExtDeleteCharAction {
        private DeleteCharAction htmlAction;

        public GspDeleteCharAction(String nm, boolean nextChar, DeleteCharAction htmlAction) {
            super(nm, nextChar);
            this.htmlAction = htmlAction;
        }

        @Override
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            Caret caret = target.getCaret();
            BaseDocument doc = (BaseDocument)target.getDocument();
            int dotPos = caret.getDot();
            if (handleDeletion(doc, dotPos)) {
                return;
            }

            htmlAction.actionPerformed(evt, target);
        }
    }
    
    @Override
    public Object clone() {
        return new GspKit();
    }
    
    private static Token<?> getToken(BaseDocument doc, int offset, boolean checkEmbedded) {
        TokenHierarchy<Document> th = TokenHierarchy.get((Document)doc);
        TokenSequence<?> ts = th.tokenSequence();
        ts.move(offset);
        if (!ts.moveNext() && !ts.movePrevious()) {
            return null;
        }

        if (checkEmbedded) {
            TokenSequence<?> es = ts.embedded();
            if (es != null) {
                es.move(offset);
                if (es.moveNext() || es.movePrevious()) {
                    return es.token();
                }
            }
        }
        return ts.token();
    }
}
