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

package org.netbeans.editor;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.text.AbstractDocument;
import javax.swing.text.View;
import org.netbeans.api.editor.EditorActionNames;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.api.editor.EditorActionRegistrations;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldUtilities;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.lib.editor.util.swing.PositionRegion;
import org.netbeans.modules.editor.indent.api.Indent;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.netbeans.modules.editor.lib.NavigationHistory;
import org.netbeans.modules.editor.lib2.RectangularSelectionUtils;
import org.netbeans.modules.editor.lib2.search.EditorFindSupport;
import org.netbeans.modules.editor.lib2.view.DocumentView;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

/**
* Actions that are not considered basic and therefore
* they are not included directly in BaseKit, but here.
* Their names however are still part of BaseKit.
*
* @author Miloslav Metelka
* @version 1.00
*
* TODO: I18N in RunMacroAction errors
*/

public class ActionFactory {

    // -J-Dorg.netbeans.editor.ActionFactory.level=FINE
    private static final Logger LOG = Logger.getLogger(ActionFactory.class.getName());
    
    private ActionFactory() {
        // no instantiation
    }

    // No registration since shared instance gets created
    //@EditorActionRegistration(name = BaseKit.removeTabAction)
    public static class RemoveTabAction extends LocalBaseAction {

        static final long serialVersionUID =-1537748600593395706L;

        public RemoveTabAction() {
            super(BaseKit.removeTabAction,
                    MAGIC_POSITION_RESET | ABBREV_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                final Caret caret = target.getCaret();
                final BaseDocument doc = (BaseDocument)target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            if (Utilities.isSelectionShowing(caret)) { // block selected
                                try {
                                    BaseKit.changeBlockIndent(
                                        doc,
                                        target.getSelectionStart(),
                                        target.getSelectionEnd(),
                                        -1);
                                } catch (GuardedException e) {
                                    target.getToolkit().beep();
                                } catch (BadLocationException e) {
                                    e.printStackTrace();
                                }
                            } else { // no selected text
                                try {
                                    int dot = caret.getDot();
                                    int lineStartOffset = Utilities.getRowStart(doc, dot);
                                    int firstNW = Utilities.getRowFirstNonWhite(doc, dot);
                                    if (firstNW != -1 && dot <= firstNW) {
                                        // Non-white row and caret inside initial whitespace => decrease text indent
                                        int lineEndOffset = Utilities.getRowEnd(doc, dot);
                                        BaseKit.changeBlockIndent(doc, lineStartOffset, lineEndOffset, -1);
                                    } else {
                                        int endNW = (firstNW == -1)
                                                ? lineStartOffset
                                                : (Utilities.getRowLastNonWhite(doc, dot) + 1);
                                        if (dot > endNW) {
                                            int shiftWidth = doc.getShiftWidth();
                                            if (shiftWidth > 0) {
                                                int dotColumn = Utilities.getVisualColumn(doc, dot);
                                                int targetColumn = Math.max(0,
                                                        (dotColumn - 1) / shiftWidth * shiftWidth);
                                                // There may be '\t' chars so remove char-by-char
                                                // and possibly fill-in spaces to get to targetColumn
                                                while (dotColumn > targetColumn && --dot >= endNW) {
                                                    doc.remove(dot, 1);
                                                    dotColumn = Utilities.getVisualColumn(doc, dot);
                                                }
                                                int insertLen;
                                                if (dot >= endNW && (insertLen = targetColumn - dotColumn) > 0) {
                                                    char[] spaceChars = new char[insertLen];
                                                    Arrays.fill(spaceChars, ' ');
                                                    String spaces = new String(spaceChars);
                                                    doc.insertString(dot, spaces, null);
                                                }
                                            }
                                        }
                                    }
                                } catch (GuardedException e) {
                                    target.getToolkit().beep();
                                } catch (BadLocationException e) {
                                    e.printStackTrace();
                                }
                            }
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }

        }

    }

    /* #47709
    public static class RemoveWordAction extends LocalBaseAction {

        static final long serialVersionUID =9193117196412195554L;

        public RemoveWordAction() {
            super(BaseKit.removeWordAction, MAGIC_POSITION_RESET
                  | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                Caret caret = target.getCaret();
                try {
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    int dotPos = caret.getDot();
                    int [] identBlock = Utilities.getIdentifierBlock(doc,dotPos);
                    if (identBlock != null){
                        doc.remove(identBlock[0], identBlock[1] - identBlock[0]);
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }
     */

    // Disabled annotations due to overriding by camel-case actions in GSF (no concrete mimetype)
//    @EditorActionRegistration(name = BaseKit.removePreviousWordAction)
    public static class RemoveWordPreviousAction extends LocalBaseAction {

        public RemoveWordPreviousAction() {
            super(BaseKit.removePreviousWordAction,
                    MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final Caret caret = target.getCaret();
                final BaseDocument doc = (BaseDocument)target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            int dotPos = caret.getDot();
                            int bolPos = Utilities.getRowStart(doc, dotPos);
                            int wsPos = Utilities.getPreviousWord(target, dotPos);
                            wsPos = (dotPos == bolPos) ? wsPos : Math.max(bolPos, wsPos);
                            doc.remove(wsPos, dotPos - wsPos);
                        } catch (BadLocationException e) {
                            target.getToolkit().beep();
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }

    // Disabled annotations due to overriding by camel-case actions in GSF (no concrete mimetype)
//    @EditorActionRegistration(name = BaseKit.removeNextWordAction)
    public static class RemoveWordNextAction extends LocalBaseAction {

        public RemoveWordNextAction() {
            super(BaseKit.removeNextWordAction,
                    MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final Caret caret = target.getCaret();
                final BaseDocument doc = (BaseDocument)target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            int dotPos = caret.getDot();
                            int eolPos = Utilities.getRowEnd(doc, dotPos);
                            int wsPos = Utilities.getNextWord(target, dotPos);
                            wsPos = (dotPos == eolPos) ? wsPos : Math.min(eolPos, wsPos);
                            doc.remove(dotPos , wsPos - dotPos);
                        } catch (BadLocationException e) {
                            target.getToolkit().beep();
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }

    
    @EditorActionRegistration(name = BaseKit.removeLineBeginAction)
    public static class RemoveLineBeginAction extends LocalBaseAction {

        static final long serialVersionUID =9193117196412195554L;

        public RemoveLineBeginAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final Caret caret = target.getCaret();
                final BaseDocument doc = (BaseDocument)target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            int dotPos = caret.getDot();
                            int bolPos = Utilities.getRowStart(doc, dotPos);
                            if (dotPos == bolPos) { // at begining of the line
                                if (dotPos > 0) {
                                    doc.remove(dotPos - 1, 1); // remove previous new-line
                                }
                            } else { // not at the line begining
                                char[] chars = doc.getChars(bolPos, dotPos - bolPos);
                                if (Analyzer.isWhitespace(chars, 0, chars.length)) {
                                    doc.remove(bolPos, dotPos - bolPos); // remove whitespace
                                } else {
                                    int firstNW = Utilities.getRowFirstNonWhite(doc, bolPos);
                                    if (firstNW >= 0 && firstNW < dotPos) {
                                        doc.remove(firstNW, dotPos - firstNW);
                                    }
                                }
                            }
                        } catch (BadLocationException e) {
                            target.getToolkit().beep();
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }

    @EditorActionRegistration(name = BaseKit.removeLineAction)
    public static class RemoveLineAction extends LocalBaseAction {

        static final long serialVersionUID =-536315497241419877L;

        public RemoveLineAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final Caret caret = target.getCaret();
                final BaseDocument doc = (BaseDocument)target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            int bolPos = Utilities.getRowStart(target, target.getSelectionStart());
                            int eolPos = Utilities.getRowEnd(target, target.getSelectionEnd());
                            if (eolPos == doc.getLength()) {
                                // Ending newline can't be removed so instead remove starting newline if it exist
                                if (bolPos > 0) {
                                    bolPos--;
                                }
                            } else { // Not the last line
                                eolPos++;
                            }
                            doc.remove(bolPos, eolPos - bolPos);
                            // Caret will be at bolPos due to removal
                        } catch (BadLocationException e) {
                            target.getToolkit().beep();
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }
    
    @EditorActionRegistration(name = BaseKit.moveSelectionElseLineUpAction)
    public static class MoveSelectionElseLineUpAction extends LocalBaseAction {

        static final long serialVersionUID = 1L;

        public MoveSelectionElseLineUpAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        @Override
        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                final BaseDocument doc = (BaseDocument) target.getDocument();
                if (doc instanceof GuardedDocument && ((GuardedDocument) doc).isPosGuarded(target.getCaretPosition())) {
                    target.getToolkit().beep();
                    return;
                }
                doc.runAtomicAsUser (new Runnable () {
                    @Override
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            Element rootElement = doc.getDefaultRootElement();

                            Caret caret = target.getCaret();
                            boolean selection = false;
                            boolean backwardSelection = false;
                            int start = target.getCaretPosition();
                            int end = start;

                            // check if there is a selection
                            if (Utilities.isSelectionShowing(caret)) {
                                int selStart = caret.getDot();
                                int selEnd = caret.getMark();
                                start = Math.min(selStart, selEnd);
                                end =   Math.max(selStart, selEnd) - 1;
                                selection = true;
                                backwardSelection = (selStart >= selEnd);
                            }

                            int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                            int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                            if (zeroBaseStartLineNumber == -1) {
                                // could not get line number
                                target.getToolkit().beep();
                            } else if (zeroBaseStartLineNumber == 0) {
                                // already first line
                            } else {
                                try {
                                    // get line text
                                    Element startLineElement = rootElement.getElement(zeroBaseStartLineNumber);
                                    int startLineStartOffset = startLineElement.getStartOffset();

                                    Element endLineElement = rootElement.getElement(zeroBaseEndLineNumber);
                                    int endLineEndOffset = endLineElement.getEndOffset();

                                    String linesText = doc.getText(startLineStartOffset, (endLineEndOffset - startLineStartOffset));

                                    Element previousLineElement = rootElement.getElement(zeroBaseStartLineNumber - 1);
                                    int previousLineStartOffset = previousLineElement.getStartOffset();

                                    int column = start - startLineStartOffset;

                                    // insert the text before the previous line
                                    doc.insertString(previousLineStartOffset, linesText, null);
                                    
                                    // remove the line
                                    if (endLineEndOffset + linesText.length() > doc.getLength()) {
                                        removeLineByLine(doc, startLineStartOffset + linesText.length() - 1, endLineEndOffset - startLineStartOffset);
                                    } else {
                                        removeLineByLine(doc, startLineStartOffset + linesText.length(), endLineEndOffset - startLineStartOffset);
                                    }
                                    
                                    if (selection) {
                                        // select moved lines
                                        if (backwardSelection) {
                                            caret.setDot(previousLineStartOffset + column);
                                            caret.moveDot(previousLineStartOffset + (endLineEndOffset - startLineStartOffset) - (endLineEndOffset - end - 1));
                                        } else {
                                            caret.setDot(previousLineStartOffset + (endLineEndOffset - startLineStartOffset) - (endLineEndOffset - end - 1));
                                            caret.moveDot(previousLineStartOffset + column);
                                        }
                                    } else {
                                        // set caret position
                                        target.setCaretPosition(previousLineStartOffset + column);
                                    }
                                } catch (BadLocationException ex) {
                                    target.getToolkit().beep();
                                }
                            }
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }
    
    @EditorActionRegistration(name = BaseKit.moveSelectionElseLineDownAction)
    public static class MoveSelectionElseLineDownAction extends LocalBaseAction {

        static final long serialVersionUID = 1L;

        public MoveSelectionElseLineDownAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        @Override
        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                final BaseDocument doc = (BaseDocument) target.getDocument();
                if (doc instanceof GuardedDocument && ((GuardedDocument) doc).isPosGuarded(target.getCaretPosition())) {
                    target.getToolkit().beep();
                    return;
                }
                doc.runAtomicAsUser (new Runnable () {
                    @Override
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            Element rootElement = doc.getDefaultRootElement();

                            Caret caret = target.getCaret();
                            boolean selection = false;
                            boolean backwardSelection = false;
                            int start = target.getCaretPosition();
                            int end = start;
                            
                            // check if there is a selection
                            if (Utilities.isSelectionShowing(caret)) {
                                int selStart = caret.getDot();
                                int selEnd = caret.getMark();
                                start = Math.min(selStart, selEnd);
                                end =   Math.max(selStart, selEnd) - 1;
                                selection = true;
                                backwardSelection = (selStart >= selEnd);
                            }

                            int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                            int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                            if (zeroBaseEndLineNumber == -1) {
                                // could not get line number
                                target.getToolkit().beep();
                            } else {
                                try {
                                    // get line text
                                    Element startLineElement = rootElement.getElement(zeroBaseStartLineNumber);
                                    int startLineStartOffset = startLineElement.getStartOffset();
                                    

                                    Element endLineElement = rootElement.getElement(zeroBaseEndLineNumber);
                                    int endLineEndOffset = endLineElement.getEndOffset();
                                    if (endLineEndOffset > doc.getLength()) {
                                        return;
                                    }

                                    String linesText = doc.getText(startLineStartOffset, (endLineEndOffset - startLineStartOffset));

                                    Element nextLineElement = rootElement.getElement(zeroBaseEndLineNumber + 1);
                                    int nextLineEndOffset = nextLineElement.getEndOffset();

                                    int column = start - startLineStartOffset;

                                    // insert it after next line
                                    if (nextLineEndOffset > doc.getLength()) {
                                        doc.insertString(doc.getLength(), "\n" + linesText.substring(0, linesText.length()-1), null);
                                    } else {
                                        doc.insertString(nextLineEndOffset, linesText, null);
                                    }

                                    // remove original line
                                    removeLineByLine(doc, startLineStartOffset, (endLineEndOffset - startLineStartOffset));
                                    if (selection) {
                                        // select moved lines
                                        if (backwardSelection) {
                                            caret.setDot(nextLineEndOffset  - (endLineEndOffset - startLineStartOffset) + column);
                                            caret.moveDot(nextLineEndOffset - (endLineEndOffset - end - 1));
                                        } else {
                                            caret.setDot(nextLineEndOffset - (endLineEndOffset - end - 1));
                                            caret.moveDot(nextLineEndOffset  - (endLineEndOffset - startLineStartOffset) + column);
                                        }
                                    } else {
                                        // set caret position
                                        target.setCaretPosition(Math.min(doc.getLength(), nextLineEndOffset + column - (endLineEndOffset - startLineStartOffset)));
                                    }
                                } catch (BadLocationException ex) {
                                    target.getToolkit().beep();
                                }
                            }
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }
    
    static void removeLineByLine(Document doc, int startPosition, int length) throws BadLocationException {
        String text = doc.getText(startPosition, length);
        BadLocationException ble = null;
        int notDeleted = 0;
        int deleted = 0;
        int line = 0;
        while(true) {
            line = text.indexOf('\n', line+1);
            if (line == -1) {
                break;
            }
            try {
                doc.remove(startPosition + notDeleted, line + 1 - deleted - notDeleted);
                deleted = line + 1 - notDeleted;
            } catch (BadLocationException blee) {
                ble = blee;
                notDeleted = line + 1 - deleted;
            }
        }
        doc.remove(startPosition + notDeleted, length - deleted - notDeleted);
        if (ble != null) {
            throw ble;
        }
    }
    
    @EditorActionRegistration(name = BaseKit.copySelectionElseLineUpAction)
    public static class CopySelectionElseLineUpAction extends LocalBaseAction {

        static final long serialVersionUID = 1L;
        
        public CopySelectionElseLineUpAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                final BaseDocument doc = (BaseDocument) target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            Element rootElement = doc.getDefaultRootElement();

                            Caret caret = target.getCaret();
                            boolean selection = false;
                            boolean backwardSelection = false;
                            int start = target.getCaretPosition();
                            int end = start;

                            // check if there is a selection
                            if (Utilities.isSelectionShowing(caret)) {
                                int selStart = caret.getDot();
                                int selEnd = caret.getMark();
                                start = Math.min(selStart, selEnd);
                                end =   Math.max(selStart, selEnd) - 1;
                                selection = true;
                                backwardSelection = (selStart >= selEnd);
                            }

                            int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                            int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                            if (zeroBaseStartLineNumber == -1) {
                                // could not get line number
                                target.getToolkit().beep();
                                return;
                            } else {
                                try {
                                    // get line text
                                    Element startLineElement = rootElement.getElement(zeroBaseStartLineNumber);
                                    int startLineStartOffset = startLineElement.getStartOffset();

                                    Element endLineElement = rootElement.getElement(zeroBaseEndLineNumber);
                                    int endLineEndOffset = endLineElement.getEndOffset();

                                    String linesText = doc.getText(startLineStartOffset, (endLineEndOffset - startLineStartOffset));

                                    int column = start - startLineStartOffset;

                                    try {
                                        NavigationHistory.getEdits().markWaypoint(target, startLineStartOffset, false, true);
                                    } catch (BadLocationException e) {
                                        LOG.log(Level.WARNING, "Can't add position to the history of edits.", e); //NOI18N
                                    }
                                    // insert it
                                    doc.insertString(startLineStartOffset, linesText, null);

                                    if (selection) {
                                        // select moved lines
                                        if (backwardSelection) {
                                            caret.setDot(startLineStartOffset + column);
                                            caret.moveDot(startLineStartOffset + (endLineEndOffset - startLineStartOffset) - (endLineEndOffset - end - 1));
                                        } else {
                                            caret.setDot(startLineStartOffset + (endLineEndOffset - startLineStartOffset) - (endLineEndOffset - end - 1));
                                            caret.moveDot(startLineStartOffset + column);
                                        }
                                    } else {
                                        // set caret position
                                        target.setCaretPosition(startLineStartOffset + column);
                                    }
                                } catch (BadLocationException ex) {
                                    target.getToolkit().beep();
                                }
                            }
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }
    
    @EditorActionRegistration(name = BaseKit.copySelectionElseLineDownAction)
    public static class CopySelectionElseLineDownAction extends LocalBaseAction {

        static final long serialVersionUID = 1L;

        public CopySelectionElseLineDownAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled() || Boolean.TRUE.equals(target.getClientProperty("AsTextField"))) {
                    target.getToolkit().beep();
                    return;
                }
                final BaseDocument doc = (BaseDocument) target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            Element rootElement = doc.getDefaultRootElement();

                            Caret caret = target.getCaret();
                            boolean selection = false;
                            boolean backwardSelection = false;
                            int start = target.getCaretPosition();
                            int end = start;

                            // check if there is a selection
                            if (Utilities.isSelectionShowing(caret)) {
                                int selStart = caret.getDot();
                                int selEnd = caret.getMark();
                                start = Math.min(selStart, selEnd);
                                end =   Math.max(selStart, selEnd) - 1;
                                selection = true;
                                backwardSelection = (selStart >= selEnd);
                            }

                            int zeroBaseStartLineNumber = rootElement.getElementIndex(start);
                            int zeroBaseEndLineNumber = rootElement.getElementIndex(end);

                            if (zeroBaseEndLineNumber == -1) {
                                // could not get line number
                                target.getToolkit().beep();
                                return;
                            } else {
                                try {
                                    // get line text
                                    Element startLineElement = rootElement.getElement(zeroBaseStartLineNumber);
                                    int startLineStartOffset = startLineElement.getStartOffset();

                                    Element endLineElement = rootElement.getElement(zeroBaseEndLineNumber);
                                    int endLineEndOffset = endLineElement.getEndOffset();

                                    String linesText = doc.getText(startLineStartOffset, (endLineEndOffset - startLineStartOffset));

                                    int column = start - startLineStartOffset;

                                    try {
                                        if (endLineEndOffset == doc.getLength() + 1) {
                                            NavigationHistory.getEdits().markWaypoint(target, endLineEndOffset - 1, false, true);
                                        } else {
                                            NavigationHistory.getEdits().markWaypoint(target, endLineEndOffset, false, true);
                                        }
                                    } catch (BadLocationException e) {
                                        LOG.log(Level.WARNING, "Can't add position to the history of edits.", e); //NOI18N
                                    }
                                    // insert it after next line
                                    if (endLineEndOffset == doc.getLength() + 1) { // extra newline at doc end (not included in doc-len)
                                        assert (linesText.charAt(linesText.length() - 1) == '\n');
                                        doc.insertString(endLineEndOffset - 1, "\n" + linesText.substring(0, linesText.length() - 1), null);
                                    } else { // Regular case
                                        doc.insertString(endLineEndOffset, linesText, null);
                                    }

                                    if (selection) {
                                        // select moved lines
                                        if (backwardSelection) {
                                            caret.setDot(endLineEndOffset + column);
                                            caret.moveDot(endLineEndOffset + (endLineEndOffset - startLineStartOffset) - (endLineEndOffset - end - 1));
                                        } else {
                                            caret.setDot(endLineEndOffset + (endLineEndOffset - startLineStartOffset) - (endLineEndOffset - end - 1));
                                            caret.moveDot(endLineEndOffset + column);
                                        }
                                    } else {
                                        // set caret position
                                        target.setCaretPosition(Math.min(doc.getLength() - 1, endLineEndOffset + column));
                                    }
                                } catch (BadLocationException ex) {
                                    target.getToolkit().beep();
                                }
                            }
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }

    /* Useful for popup menu - remove selected block or do nothing */
    // No annotation registration since shared instance exists in BaseKit
    //@EditorActionRegistration(name = BaseKit.removeSelectionAction)
    public static class RemoveSelectionAction extends LocalBaseAction {

        static final long serialVersionUID =-1419424594746686573L;

        public RemoveSelectionAction() {
            super(BaseKit.removeSelectionAction,
                    MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            //#54893 putValue ("helpID", RemoveSelectionAction.class.getName ()); // NOI18N
        }

        public void actionPerformed(ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final BaseDocument doc = (BaseDocument)target.getDocument();
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            target.replaceSelection(null);
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }

    /** Switch to overwrite mode or back to insert mode */
    @EditorActionRegistration(name = BaseKit.toggleTypingModeAction)
    public static class ToggleTypingModeAction extends LocalBaseAction {

        static final long serialVersionUID =-2431132686507799723L;

        public ToggleTypingModeAction() {
            super();
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI editorUI = Utilities.getEditorUI(target);
                Boolean overwriteMode = (Boolean)editorUI.getProperty(EditorUI.OVERWRITE_MODE_PROPERTY);
                // Now toggle
                overwriteMode = (overwriteMode == null || !overwriteMode.booleanValue())
                                ? Boolean.TRUE : Boolean.FALSE;
                editorUI.putProperty(EditorUI.OVERWRITE_MODE_PROPERTY, overwriteMode);
            }
        }
    }

    /**
     * @deprecated Without any replacement. This action is not used anymore and
     * is no longer functional.
     */
    public static class RunMacroAction extends BaseAction {

        static final long serialVersionUID =1L;

        static HashSet runningActions = new HashSet();
        private String macroName;

        public RunMacroAction( String name ) {
            super( BaseKit.macroActionPrefix + name);
            this.macroName = name;
        }

        protected void error( JTextComponent target, String messageKey ) {
            Utilities.setStatusText( target, LocaleSupport.getString(
                messageKey, "Error in macro: " + messageKey ) // NOI18N
            );
            Toolkit.getDefaultToolkit().beep();
        }
        
        public void actionPerformed(ActionEvent evt, final JTextComponent target) {
            if( !runningActions.add( macroName ) ) { // this macro is already running, beware of loops
                error( target, "loop" ); // NOI18N
                return;
            }

            if( target == null ) return;
           
            final BaseKit kit = Utilities.getKit(target);
            if( kit == null ) return;
            
//            Map macroMap = (Map)Settings.getValue( kit.getClass(), SettingsNames.MACRO_MAP);
//            
//            String commandString = (String)macroMap.get( macroName );
            String commandString = null;
            
            if( commandString == null ) {
                error( target, "macro-not-found" ); // NOI18N
                runningActions.remove( macroName );
                return;
            }

            final StringBuffer actionName = new StringBuffer();
            final char[] command = commandString.toCharArray();
            final int len = command.length;

            final BaseDocument doc = (BaseDocument)target.getDocument();
            doc.runAtomicAsUser (new Runnable () {
                public void run () {
                    try {
                        for( int i = 0; i < len; i++ ) {
                            if( Character.isWhitespace( command[i] ) ) continue;
                            if( command[i] == '"' ) {
                                while( ++i < len && command[i] != '"' ) {
                                    char ch = command[i];
                                    if( ch == '\\' ) {
                                        if( ++i >= len ) { // '\' at the end
                                            error( target, "macro-malformed" ); // NOI18N
                                            return;
                                        }
                                        ch = command[i];
                                        if( ch != '"' && ch != '\\' ) { // neither \\ nor \" // NOI18N
                                            error( target, "macro-malformed" ); // NOI18N
                                            return;
                                        } // else fall through
                                    }
                                    Action a = target.getKeymap().getDefaultAction();

                                    if (a != null) {
                                        ActionEvent newEvt = new ActionEvent( target, 0, new String( new char[] { ch } ) );
                                        if( a instanceof BaseAction ) {
                                            ((BaseAction)a).updateComponent(target);
                                            ((BaseAction)a).actionPerformed( newEvt, target );
                                        } else {
                                            a.actionPerformed( newEvt );
                                        }
                                    }
                                }
                            } else { // parse the action name
                                actionName.setLength( 0 );
                                while( i < len && ! Character.isWhitespace( command[i] ) ) {
                                    char ch = command[i++];
                                    if( ch == '\\' ) {
                                        if( i >= len ) { // macro ending with single '\'
                                            error( target, "macro-malformed" ); // NOI18N
                                            return;
                                        };
                                        ch = command[i++];
                                        if( ch != '\\' && ! Character.isWhitespace( ch ) ) {//
                                            error( target, "macro-malformed" ); // neither "\\" nor "\ " // NOI18N
                                            return;
                                        } // else fall through
                                    }
                                    actionName.append( ch );
                                }
                                // execute the action
                                Action a = kit.getActionByName( actionName.toString() );
                                if (a != null) {
                                    ActionEvent fakeEvt = new ActionEvent( target, 0, "" );
                                    if( a instanceof BaseAction ) {
                                        ((BaseAction)a).updateComponent(target);
                                        ((BaseAction)a).actionPerformed( fakeEvt, target );
                                    } else {
                                        a.actionPerformed( fakeEvt );
                                    }
                                    if(DefaultEditorKit.insertBreakAction.equals(actionName.toString())){
                                        Action def = target.getKeymap().getDefaultAction();
                                        ActionEvent fakeEvt10 = new ActionEvent( target, 0, new String(new byte[]{10}) );
                                        if( def instanceof BaseAction ) {
                                            ((BaseAction)def).updateComponent(target);
                                            ((BaseAction)def).actionPerformed( fakeEvt10, target );
                                        } else {
                                            def.actionPerformed( fakeEvt10 );
                                        }
                                    }
                                } else {
                                    error( target, "macro-unknown-action" ); // NOI18N
                                    return;
                                }
                            }
                        }
                    } finally {
                        runningActions.remove( macroName );
                    }
                }
            });
        }
    } // End of RunMacroAction class
    
    /**
     * @deprecated Without any replacement. This action is not used anymore.
     */
    public static class StartMacroRecordingAction extends LocalBaseAction {
    // Not registered by annotation since it's not actively used

        static final long serialVersionUID =1L;

        public StartMacroRecordingAction() {
            super( BaseKit.startMacroRecordingAction, NO_RECORDING );
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/start_macro_recording.png"); // NOI18N
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if( !startRecording(target) ) target.getToolkit().beep();
            }
        }
    }

    /**
     * @deprecated Without any replacement. This action is not used anymore and
     * is no longer functional.
     */
    public static class StopMacroRecordingAction extends LocalBaseAction {
    // Not registered by annotation since it's not actively used

        static final long serialVersionUID =1L;

        public StopMacroRecordingAction() {
            super( BaseKit.stopMacroRecordingAction, NO_RECORDING );
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/stop_macro_recording.png"); // NOI18N
        }
        
        protected MacroDialogSupport getMacroDialogSupport(Class kitClass){
            return new MacroDialogSupport(kitClass);
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                String macro = stopRecording(target);
                if( macro == null ) { // not recording
                    target.getToolkit().beep();
                } else {
                    // popup a macro dialog
                    BaseKit kit = Utilities.getKit(target);
                    MacroDialogSupport support = getMacroDialogSupport(kit.getClass());
                    support.setBody( macro );
                    support.showMacroDialog();
                }
            }
        }
    }

    /** @deprecated Use Editor Code Templates API instead. */
    public static class AbbrevExpandAction extends LocalBaseAction {

        static final long serialVersionUID =-2124569510083544403L;

        public AbbrevExpandAction() {
            super(BaseKit.abbrevExpandAction,
                  MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            putValue(BaseAction.NO_KEYBINDING, Boolean.TRUE);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                EditorUI editorUI = ((BaseTextUI)target.getUI()).getEditorUI();
                try {
                    editorUI.getAbbrev().checkAndExpand(evt);
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    /** @deprecated Use Editor Code Templates API instead. */
    public static class AbbrevResetAction extends LocalBaseAction {

        static final long serialVersionUID =-2807497346060448395L;

        public AbbrevResetAction() {
            super(BaseKit.abbrevResetAction, ABBREV_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
        }

    }

    public static class ChangeCaseAction extends LocalBaseAction {

        @EditorActionRegistration(name = BaseKit.toUpperCaseAction)
        public static ChangeCaseAction createToUpperCase() {
            return new ChangeCaseAction(Utilities.CASE_UPPER);
        }

        @EditorActionRegistration(name = BaseKit.toLowerCaseAction)
        public static ChangeCaseAction createToLowerCase() {
            return new ChangeCaseAction(Utilities.CASE_LOWER);
        }

        @EditorActionRegistration(name = BaseKit.switchCaseAction)
        public static ChangeCaseAction createSwitchCase() {
            return new ChangeCaseAction(Utilities.CASE_SWITCH);
        }

        int changeCaseMode;

        static final long serialVersionUID =5680212865619897402L;

        private ChangeCaseAction(int changeCaseMode) {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
            this.changeCaseMode = changeCaseMode;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                try {
                    Caret caret = target.getCaret();
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    int dotPos = caret.getDot();
                    if (Utilities.isSelectionShowing(caret)) { // valid selection
                        int startPos = target.getSelectionStart();
                        int endPos = target.getSelectionEnd();
                        Utilities.changeCase(doc, startPos, endPos - startPos, changeCaseMode);
                        caret.setDot(dotPos == startPos ? endPos : startPos);
                        caret.moveDot(dotPos == startPos ? startPos : endPos);
                    } else { // no selection - change current char
                        Utilities.changeCase(doc, dotPos, 1, changeCaseMode);
                        caret.setDot(dotPos + 1);
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    @EditorActionRegistration(name = BaseKit.findNextAction,
            iconResource = "org/netbeans/modules/editor/resources/find_next.png") // NOI18N
    public static class FindNextAction extends LocalBaseAction {

        static final long serialVersionUID =6878814427731642684L;

        public FindNextAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        @Override
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI eui = org.netbeans.editor.Utilities.getEditorUI(target);
                if (eui.getComponent().getClientProperty("AsTextField") == null)  { //NOI18N
                    EditorFindSupport.getInstance().setFocusedTextComponent(eui.getComponent());
                }
                openFindIfNecessary(eui, evt);
                EditorFindSupport.getInstance().find(null, false);
            }
        }
    }
    
    private static void openFindIfNecessary(EditorUI eui, ActionEvent evt) {
        Object findWhat = EditorFindSupport.getInstance().getFindProperty(EditorFindSupport.FIND_WHAT);
        if (findWhat == null || !(findWhat instanceof String) || ((String) findWhat).isEmpty()) {

            Action findAction = ((BaseKit) eui.getComponent().getUI().getEditorKit(
                    eui.getComponent())).getActionByName("find");
            if (findAction != null) {
                findAction.actionPerformed(evt);
            }
        }
    }
    
    @EditorActionRegistration(name = BaseKit.findPreviousAction,
            iconResource = "org/netbeans/modules/editor/resources/find_previous.png") // NOI18N
    public static class FindPreviousAction extends LocalBaseAction {

        static final long serialVersionUID =-43746947902694926L;

        public FindPreviousAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        @Override
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI eui = org.netbeans.editor.Utilities.getEditorUI(target);
                if (eui.getComponent().getClientProperty("AsTextField") == null)  { //NOI18N
                    EditorFindSupport.getInstance().setFocusedTextComponent(eui.getComponent());
                }
                openFindIfNecessary(eui, evt);
                EditorFindSupport.getInstance().find(null, true);
            }
        }
    }

    /** Finds either selection or if there's no selection it finds
    * the word where the cursor is standing.
    */
    @EditorActionRegistration(name = BaseKit.findSelectionAction,
            iconResource = "org/netbeans/modules/editor/resources/find_selection.png") // NOI18N
    public static class FindSelectionAction extends LocalBaseAction {

        static final long serialVersionUID =-5601618936504699565L;

        public FindSelectionAction() {
            super();
        }

        @Override
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorFindSupport findSupport = EditorFindSupport.getInstance();
                Caret caret = target.getCaret();
                int dotPos = caret.getDot();
                HashMap props = new HashMap(findSupport.createDefaultFindProperties());
                String searchWord = null;
                boolean revert = false;
                Boolean originalValue = null;
                Map revertMap = (Map)props.get(EditorFindSupport.REVERT_MAP);
                Boolean revertValue = revertMap != null ? (Boolean)revertMap.get(EditorFindSupport.FIND_WHOLE_WORDS) : null;

                if (Utilities.isSelectionShowing(caret)) { // valid selection
                    searchWord = target.getSelectedText();
                    originalValue = (Boolean)props.put(EditorFindSupport.FIND_WHOLE_WORDS, Boolean.FALSE);
                    if (Boolean.FALSE.equals(revertValue)) {
                        revertMap.remove(EditorFindSupport.FIND_WHOLE_WORDS);
                    } else {
                        revert = !Boolean.FALSE.equals(originalValue);
                    }
                } else { // no selection, get current word
                    try {
                        searchWord = Utilities.getIdentifier((BaseDocument)target.getDocument(),
                                                             dotPos);
                        originalValue = (Boolean)props.put(EditorFindSupport.FIND_WHOLE_WORDS, Boolean.TRUE);
                        if (Boolean.TRUE.equals(revertValue)) {
                            revertMap.remove(EditorFindSupport.FIND_WHOLE_WORDS);
                        } else {
                            revert = !Boolean.TRUE.equals(originalValue);
                        }

                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }

                if (searchWord != null) {
                    int n = searchWord.indexOf( '\n' );
                    if (n >= 0 ) 
                        searchWord = searchWord.substring(0, n);
                    props.put(EditorFindSupport.FIND_WHAT, searchWord);
                
                    if (revert){
                        revertMap = new HashMap();
                        revertMap.put(EditorFindSupport.FIND_WHOLE_WORDS, originalValue != null ? originalValue : Boolean.FALSE);
                        props.put(EditorFindSupport.REVERT_MAP, revertMap);
                    }
                    
                    props.put(EditorFindSupport.FIND_BLOCK_SEARCH, Boolean.FALSE);
                    props.put(EditorFindSupport.FIND_BLOCK_SEARCH_START, null);
                    props.put(EditorFindSupport.FIND_BLOCK_SEARCH_END, null);

                    EditorUI eui = org.netbeans.editor.Utilities.getEditorUI(target);
                    if (eui.getComponent().getClientProperty("AsTextField") == null) { //NOI18N
                        findSupport.setFocusedTextComponent(eui.getComponent());
                    }
                    findSupport.putFindProperties(props);
                    if (findSupport.find(null, false)) {
                        findSupport.addToHistory(new EditorFindSupport.SPW((String) props.get(EditorFindSupport.FIND_WHAT),
                                (Boolean) props.get(EditorFindSupport.FIND_WHOLE_WORDS), (Boolean) props.get(EditorFindSupport.FIND_MATCH_CASE), (Boolean) props.get(EditorFindSupport.FIND_REG_EXP)));
                    }
                }
            }
        }
    }

    // Cannot easily use EditorActionRegistration yet for toggle buttons
    public static class ToggleRectangularSelectionAction extends LocalBaseAction
    implements Presenter.Toolbar, ContextAwareAction, PropertyChangeListener {

        static final long serialVersionUID = 0L;
        
        private JEditorPane pane;
        
        private JToggleButton toggleButton;

        public ToggleRectangularSelectionAction() {
            super(EditorActionNames.toggleRectangularSelection);
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/editor/resources/rect_select_16x16.png", false)); //NOI18N
            putValue("noIconInMenu", Boolean.TRUE); // NOI18N
        }
        
        void setPane(JEditorPane pane) {
            assert (pane != null);
            this.pane = pane;
            pane.addPropertyChangeListener(this);
            updateState();
        }
        
        void updateState() {
            if (pane != null) {
                boolean rectangleSelection = RectangularSelectionUtils.isRectangularSelection(pane);
                if (toggleButton != null) {
                    toggleButton.setSelected(rectangleSelection);
                    toggleButton.setContentAreaFilled(rectangleSelection);
                    toggleButton.setBorderPainted(rectangleSelection);
                }
            }
        }

        @Override
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null && !Boolean.TRUE.equals(target.getClientProperty("AsTextField"))) {
                boolean newRectSel = !RectangularSelectionUtils.isRectangularSelection(target);
                RectangularSelectionUtils.setRectangularSelection(target, newRectSel);
            }
        }

        @Override
        public Component getToolbarPresenter() {
            toggleButton = new JToggleButton();
            toggleButton.putClientProperty("hideActionText", Boolean.TRUE); //NOI18N
            toggleButton.setIcon((Icon) getValue(SMALL_ICON));
            toggleButton.setAction(this); // this will make hard ref to button => check GC
            return toggleButton;
        }

        @Override
        public Action createContextAwareInstance(Lookup actionContext) {
            JEditorPane pane = actionContext.lookup(JEditorPane.class);
            if (pane != null) {
                ToggleRectangularSelectionAction action = new ToggleRectangularSelectionAction();
                action.setPane(pane);
                return action;
            }
            return this;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (pane == evt.getSource()) { // Event from pane
                if (RectangularSelectionUtils.getRectangularSelectionProperty().equals(evt.getPropertyName())) {
                    updateState();
                }
            }
        }
        
        
    }    

// suspending the use of EditorActionRegistration due to #167063
//    @EditorActionRegistration(name = BaseKit.toggleHighlightSearchAction,
//            iconResource = "org/netbeans/modules/editor/resources/toggle_highlight.png")
    public static class ToggleHighlightSearchAction extends LocalBaseAction implements Presenter.Toolbar {

        static final long serialVersionUID =4603809175771743200L;

        public ToggleHighlightSearchAction() {
            super(BaseKit.toggleHighlightSearchAction, CLEAR_STATUS_TEXT);
            putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon("org/netbeans/modules/editor/resources/toggle_highlight.png", false)); //NOI18N
            putValue("noIconInMenu", Boolean.TRUE); // NOI18N
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Boolean cur = (Boolean)EditorFindSupport.getInstance().getFindProperty(
                                  EditorFindSupport.FIND_HIGHLIGHT_SEARCH);
                if (cur == null || cur.booleanValue() == false) {
                    cur = Boolean.TRUE;
                } else {
                    cur = Boolean.FALSE;
                }
                EditorFindSupport.getInstance().putFindProperty(
                    EditorFindSupport.FIND_HIGHLIGHT_SEARCH, cur);
            }
        }

        public Component getToolbarPresenter() {
            JToggleButton b = new MyGaGaButton();
            b.setModel(new HighlightButtonModel());
            b.putClientProperty("hideActionText", Boolean.TRUE); //NOI18N
            b.setAction(this);
            
            return b;
        }
        
        private static final class HighlightButtonModel extends JToggleButton.ToggleButtonModel implements PropertyChangeListener {
            
            public HighlightButtonModel() {
                EditorFindSupport efs = EditorFindSupport.getInstance();
                efs.addPropertyChangeListener(WeakListeners.propertyChange(this, efs));
                propertyChange(null);
            }
        
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt == null || evt.getPropertyName() == null || evt.getPropertyName().equals(EditorFindSupport.FIND_HIGHLIGHT_SEARCH)) {
                    Boolean value = (Boolean) EditorFindSupport.getInstance().getFindProperty(EditorFindSupport.FIND_HIGHLIGHT_SEARCH);
                    setSelected(value == null ? false : value.booleanValue());
                }
            }
        } // End of HighlightButtonModel class
        
        private static final class MyGaGaButton extends JToggleButton implements ChangeListener {
            
            public MyGaGaButton() {
                
            }
            
            @Override
            public void setModel(ButtonModel model) {
                ButtonModel oldModel = getModel();
                if (oldModel != null) {
                    oldModel.removeChangeListener(this);
                }
                
                super.setModel(model);
                
                ButtonModel newModel = getModel();
                if (newModel != null) {
                    newModel.addChangeListener(this);
                }
                
                stateChanged(null);
            }

            public void stateChanged(ChangeEvent evt) {
                boolean selected = isSelected();
                super.setContentAreaFilled(selected);
                super.setBorderPainted(selected);
            }

            @Override
            public void setBorderPainted(boolean arg0) {
                if (!isSelected()) {
                    super.setBorderPainted(arg0);
                }
            }

            @Override
            public void setContentAreaFilled(boolean arg0) {
                if (!isSelected()) {
                    super.setContentAreaFilled(arg0);
                }
            }
        }
    } // End of ToggleHighlightSearchAction class

    public static class UndoAction extends LocalBaseAction {

        static final long serialVersionUID =8628586205035497612L;

        public UndoAction() {
            super(BaseKit.undoAction, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (!target.isEditable() || !target.isEnabled()) {
                target.getToolkit().beep();
                return;
            }

            Document doc = target.getDocument();
            UndoableEdit undoMgr = (UndoableEdit)doc.getProperty(
                                       BaseDocument.UNDO_MANAGER_PROP);
            if (target != null && undoMgr != null) {
                try {
                    undoMgr.undo();
                } catch (CannotUndoException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    public static class RedoAction extends LocalBaseAction {

        static final long serialVersionUID =6048125996333769202L;

        public RedoAction() {
            super(BaseKit.redoAction, ABBREV_RESET
                  | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (!target.isEditable() || !target.isEnabled()) {
                target.getToolkit().beep();
                return;
            }

            Document doc = target.getDocument();
            UndoableEdit undoMgr = (UndoableEdit)doc.getProperty(
                                       BaseDocument.UNDO_MANAGER_PROP);
            if (target != null && undoMgr != null) {
                try {
                    undoMgr.redo();
                } catch (CannotRedoException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    @EditorActionRegistrations({
        @EditorActionRegistration(name = BaseKit.wordMatchNextAction,
            iconResource = "org/netbeans/modules/editor/resources/next_matching.png"),
        @EditorActionRegistration(name = BaseKit.wordMatchPrevAction,
            iconResource = "org/netbeans/modules/editor/resources/previous_matching.png")
    })
    public static class WordMatchAction extends LocalBaseAction {

        private boolean matchNext;

        static final long serialVersionUID =595571114685133170L;

        public WordMatchAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
        }

        @Override
        protected void actionNameUpdate(String actionName) {
            super.actionNameUpdate(actionName);
            this.matchNext = BaseKit.wordMatchNextAction.equals(actionName);
        }

        public void actionPerformed(final ActionEvent evt, final  JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                EditorUI editorUI = Utilities.getEditorUI(target);
                Caret caret = target.getCaret();
                final BaseDocument doc = Utilities.getDocument(target);

                // Possibly remove selection
                if (Utilities.isSelectionShowing(caret)) {
                    target.replaceSelection(null);
                }

                final int caretOffset = caret.getDot();
                final String s = editorUI.getWordMatch().getMatchWord(caretOffset, matchNext);
                final String prevWord = editorUI.getWordMatch().getPreviousWord();
                if (s != null) {
                    doc.runAtomicAsUser (new Runnable () {
                        public void run () {
                            DocumentUtilities.setTypingModification(doc, true);
                            try {
                                int offset = caretOffset;
                                boolean removePrevWord = (prevWord != null && prevWord.length() > 0);
                                if (removePrevWord) {
                                    offset -= prevWord.length();
                                }
                                // Create position due to possible text replication (e.g. for variable renaming)
                                Position pos = doc.createPosition(offset);
                                doc.remove(offset, prevWord.length());
                                doc.insertString(pos.getOffset(), s, null);
                            } catch (BadLocationException e) {
                                target.getToolkit().beep();
                            } finally {
                                DocumentUtilities.setTypingModification(doc, false);
                            }
                        }
                    });
                }
            }
        }

    }


    @EditorActionRegistrations({
        @EditorActionRegistration(name = BaseKit.shiftLineLeftAction,
            iconResource = "org/netbeans/modules/editor/resources/shift_line_left.png"),
        @EditorActionRegistration(name = BaseKit.shiftLineRightAction,
            iconResource = "org/netbeans/modules/editor/resources/shift_line_right.png")
    })
    public static class ShiftLineAction extends LocalBaseAction {

        static final long serialVersionUID =-5124732597493699582L;

        public ShiftLineAction() {
            super(MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
        }

        @Override
        protected void actionNameUpdate(String actionName) {
            super.actionNameUpdate(actionName);
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final Caret caret = target.getCaret();
                final BaseDocument doc = Utilities.getDocument(target);
                doc.runAtomicAsUser (new Runnable () {
                    public void run () {
                        DocumentUtilities.setTypingModification(doc, true);
                        try {
                            boolean right = BaseKit.shiftLineRightAction.equals(getValue(Action.NAME));
                            if (Utilities.isSelectionShowing(caret)) {
                                BaseKit.shiftBlock(
                                    doc,
                                    target.getSelectionStart(), target.getSelectionEnd(),
                                    right);
                            } else {
                                BaseKit.shiftLine(doc, caret.getDot(), right);
                            }
                        } catch (GuardedException e) {
                            target.getToolkit().beep();
                        } catch (BadLocationException e) {
                            e.printStackTrace();
                        } finally {
                            DocumentUtilities.setTypingModification(doc, false);
                        }
                    }
                });
            }
        }
    }

    @EditorActionRegistrations({
        @EditorActionRegistration(name = BaseKit.reindentLineAction),
        @EditorActionRegistration(name = BaseKit.reformatLineAction)
    })        
    public static class ReindentLineAction extends LocalBaseAction {

        private boolean reindent;
        
        static final long serialVersionUID =1L;

        public ReindentLineAction() {
            // TODO: figure out what these flags are all about
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
            //putValue ("helpID", ReindentLineAction.class.getName ());
        }

        @Override
        protected void actionNameUpdate(String actionName) {
            super.actionNameUpdate(actionName);
            this.reindent = BaseKit.reindentLineAction.equals(actionName);
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final Caret caret = target.getCaret();
                final BaseDocument doc = (BaseDocument)target.getDocument();
                final GuardedDocument gdoc = (doc instanceof GuardedDocument)
                                       ? (GuardedDocument)doc : null;

                final Indent indenter = reindent ? Indent.get(doc) : null;
                final Reformat reformat = reindent ? null : Reformat.get(doc);
                if (reindent) {
                    indenter.lock();
                } else {
                    reformat.lock();
                }
                try {
                    doc.runAtomicAsUser (new Runnable () {
                        public void run () {
                            try {
                                int startPos;
                                Position endPosition;

                                if (Utilities.isSelectionShowing(caret)) {
                                    startPos = target.getSelectionStart();
                                    endPosition = doc.createPosition(target.getSelectionEnd());
                                } else {
                                    startPos = Utilities.getRowStart(doc, caret.getDot());
                                    endPosition = doc.createPosition(Utilities.getRowEnd(doc, caret.getDot()));
                                }

                                int pos = startPos;
                                if (gdoc != null) {
                                    pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
                                }

                                while (pos <= endPosition.getOffset()) {
                                    int stopPos = endPosition.getOffset();
                                    if (gdoc != null) { // adjust to start of the next guarded block
                                        stopPos = gdoc.getGuardedBlockChain().adjustToNextBlockStart(pos);
                                        if (stopPos == -1 || stopPos > endPosition.getOffset()) {
                                            stopPos = endPosition.getOffset();
                                        }
                                    }

                                    Position stopPosition = doc.createPosition(stopPos);
                                    if (reindent) {
                                        indenter.reindent(pos, stopPos);
                                    } else {
                                        reformat.reformat(pos, stopPos);
                                    }
                                    pos = stopPosition.getOffset() + 1;

                                    if (gdoc != null) { // adjust to end of current block
                                        pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
                                    }
                                }
                            } catch (GuardedException e) {
                                target.getToolkit().beep();
                            } catch (BadLocationException e) {
                                Utilities.annotateLoggable(e);
                            }
                        }
                    });
                } finally {
                    if (reindent) {
                        indenter.unlock();
                    } else {
                        reformat.unlock();
                    }
                }
            }
        }
    }

    
    public static class AdjustWindowAction extends LocalBaseAction {

        @EditorActionRegistration(name = BaseKit.adjustWindowTopAction)
        public static AdjustWindowAction createAdjustTop() {
            return new AdjustWindowAction(0);
        }

        @EditorActionRegistration(name = BaseKit.adjustWindowCenterAction)
        public static AdjustWindowAction createAdjustCenter() {
            return new AdjustWindowAction(50);
        }

        @EditorActionRegistration(name = BaseKit.adjustWindowBottomAction)
        public static AdjustWindowAction createAdjustBottom() {
            return new AdjustWindowAction(100);
        }

        int percentFromWindowTop;

        static final long serialVersionUID =8864278998999643292L;

        public AdjustWindowAction(int percentFromWindowTop) {
            this.percentFromWindowTop = percentFromWindowTop;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Utilities.getEditorUI(target).adjustWindow(percentFromWindowTop);
            }
        }
    }

    public static class AdjustCaretAction extends LocalBaseAction {

        @EditorActionRegistration(name = BaseKit.adjustCaretTopAction)
        public static AdjustCaretAction createAdjustTop() {
            return new AdjustCaretAction(0);
        }

        @EditorActionRegistration(name = BaseKit.adjustCaretCenterAction)
        public static AdjustCaretAction createAdjustCenter() {
            return new AdjustCaretAction(50);
        }

        @EditorActionRegistration(name = BaseKit.adjustCaretBottomAction)
        public static AdjustCaretAction createAdjustBottom() {
            return new AdjustCaretAction(100);
        }

        int percentFromWindowTop;

        static final long serialVersionUID =3223383913531191066L;

        public AdjustCaretAction(int percentFromWindowTop) {
            this.percentFromWindowTop = percentFromWindowTop;
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Utilities.getEditorUI(target).adjustCaret(percentFromWindowTop);
            }
        }
    }

    @EditorActionRegistration(name = BaseKit.formatAction)
    public static class FormatAction extends LocalBaseAction {

        static final long serialVersionUID =-7666172828961171865L;

        public FormatAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
            //#54893 putValue ("helpID", FormatAction.class.getName ()); // NOI18N
        }

        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }

                final Caret caret = target.getCaret();
                final BaseDocument doc = Utilities.getDocument(target);
                if (doc == null)
                    return;
                // Set hourglass cursor
                final Cursor origCursor = target.getCursor();
                target.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                try {
                final AtomicBoolean canceled = new AtomicBoolean();
                ProgressUtils.runOffEventDispatchThread(new Runnable() {
                    public void run() {
                        if (canceled.get()) return;
                        final Reformat formatter = Reformat.get(doc);
                        formatter.lock();
                        try {
                            if (canceled.get()) return;
                            doc.runAtomicAsUser(new Runnable() {

                                public void run() {
                                    try {
                                        int startPos, endPos;
                                        if (Utilities.isSelectionShowing(caret)) {
                                            startPos = target.getSelectionStart();
                                            endPos = target.getSelectionEnd();
                                        } else {
                                            startPos = 0;
                                            endPos = doc.getLength();
                                        }

                                        reformat(formatter, doc, startPos, endPos, canceled);
                                    } catch (GuardedException e) {
                                        target.getToolkit().beep();
                                    } catch (BadLocationException e) {
                                        Utilities.annotateLoggable(e);
                                    }
                                }
                            });
                        } finally {
                            formatter.unlock();
                        }
                    }
                }, NbBundle.getMessage(FormatAction.class, "Format_in_progress"), canceled, false); //NOI18N
                } catch (Exception e) {
                    // not sure about this, but was getting j.l.Exception that the operation is too slow - wtf?
                    Logger.getLogger(FormatAction.class.getName()).log(Level.FINE, null, e);
                } finally {
                    target.setCursor(origCursor);
                }
            }
        }
    }

    static void reformat(Reformat formatter, Document doc, int startPos, int endPos, AtomicBoolean canceled) throws BadLocationException {
        final GuardedDocument gdoc = (doc instanceof GuardedDocument)
                ? (GuardedDocument) doc : null;

        int pos = startPos;
        if (gdoc != null) {
            pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
        }

        LinkedList<PositionRegion> regions = new LinkedList<PositionRegion>();
        while (pos < endPos) {
            int stopPos = endPos;
            if (gdoc != null) { // adjust to start of the next guarded block
                stopPos = gdoc.getGuardedBlockChain().adjustToNextBlockStart(pos);
                if (stopPos == -1 || stopPos > endPos) {
                    stopPos = endPos;
                }
            }

            if (pos < stopPos) {
                regions.addFirst(new PositionRegion(doc, pos, stopPos));
                pos = stopPos;
            } else {
                pos++; //ensure to make progress
            }

            if (gdoc != null) { // adjust to end of current block
                pos = gdoc.getGuardedBlockChain().adjustToBlockEnd(pos);
            }
        }

        if (canceled.get()) return;
        // Once we start formatting, the task can't be canceled

        for (PositionRegion region : regions) {
            formatter.reformat(region.getStartOffset(), region.getEndOffset());
        }
    }
    
    @EditorActionRegistrations({
        @EditorActionRegistration(name = BaseKit.firstNonWhiteAction),
        @EditorActionRegistration(name = BaseKit.selectionFirstNonWhiteAction)
    })
    public static class FirstNonWhiteAction extends LocalBaseAction {

        static final long serialVersionUID =-5888439539790901158L;

        public FirstNonWhiteAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    int pos = Utilities.getRowFirstNonWhite((BaseDocument)target.getDocument(),
                                                            caret.getDot());
                    if (pos >= 0) {
                        boolean select = BaseKit.selectionFirstNonWhiteAction.equals(getValue(Action.NAME));
                        if (select) {
                            caret.moveDot(pos);
                        } else {
                            caret.setDot(pos);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    @EditorActionRegistrations({
        @EditorActionRegistration(name = BaseKit.lastNonWhiteAction),
        @EditorActionRegistration(name = BaseKit.selectionLastNonWhiteAction)
    })
    public static class LastNonWhiteAction extends LocalBaseAction {

        static final long serialVersionUID =4503533041729712917L;

        public LastNonWhiteAction() {
            super(MAGIC_POSITION_RESET | ABBREV_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    int pos = Utilities.getRowLastNonWhite((BaseDocument)target.getDocument(),
                                                           caret.getDot());
                    if (pos >= 0) {
                        boolean select = BaseKit.selectionLastNonWhiteAction.equals(getValue(Action.NAME));
                        if (select) {
                            caret.moveDot(pos);
                        } else {
                            caret.setDot(pos);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    @EditorActionRegistration(name = BaseKit.selectIdentifierAction)
    public static class SelectIdentifierAction extends LocalBaseAction {

        static final long serialVersionUID =-7288216961333147873L;

        public SelectIdentifierAction() {
            super(MAGIC_POSITION_RESET);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                try {
                    if (Utilities.isSelectionShowing(caret)) {
                        caret.setDot(caret.getDot()); // unselect if anything selected
                    } else { // selection not visible
                        int block[] = Utilities.getIdentifierBlock((BaseDocument)target.getDocument(),
                                      caret.getDot());
                        if (block != null) {
                            caret.setDot(block[0]);
                            caret.moveDot(block[1]);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    @EditorActionRegistration(name = BaseKit.selectNextParameterAction)
    public static class SelectNextParameterAction extends LocalBaseAction {

        static final long serialVersionUID =8045372985336370934L;

        public SelectNextParameterAction() {
            super(BaseKit.selectNextParameterAction, MAGIC_POSITION_RESET | CLEAR_STATUS_TEXT);
            putValue(BaseAction.NO_KEYBINDING, Boolean.TRUE);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                Caret caret = target.getCaret();
                BaseDocument doc = (BaseDocument)target.getDocument();
                int dotPos = caret.getDot();
                int selectStartPos = -1;
                try {
                    if (dotPos > 0) {
                        if (doc.getChars(dotPos - 1, 1)[0] == ',') { // right after the comma
                            selectStartPos = dotPos;
                        }
                    }
                    if (dotPos < doc.getLength()) {
                        char dotChar = doc.getChars(dotPos, 1)[0];
                        if (dotChar == ',') {
                            selectStartPos = dotPos + 1;
                        } else if (dotChar == ')') {
                            caret.setDot(dotPos + 1);
                        }
                    }
                    if (selectStartPos >= 0) {
                        int selectEndPos = doc.find(
                                               new FinderFactory.CharArrayFwdFinder( new char[] { ',', ')' }),
                                               selectStartPos, -1
                                           );
                        if (selectEndPos >= 0) {
                            target.select(selectStartPos, selectEndPos);
                        }
                    }
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }

    /**
     * This implementation is no longer used, see org.netbeans.modules.editor.impl.actions.NavigationHistoryForwardAction
     * in the editor module.
     */
    public static class JumpListNextAction extends LocalBaseAction {
    // Not registered by annotation since it's not actively used

        static final long serialVersionUID =6891721278404990446L;
        PropertyChangeListener pcl;

        public JumpListNextAction() {
            super(BaseKit.jumpListNextAction);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/edit_next.png"); // NOI18N
            JumpList.addPropertyChangeListener(pcl = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    setEnabled(JumpList.hasNext());
                }
            });
            setEnabled(JumpList.hasNext());
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpNext(target);
            }
        }
    }

    /**
     * This implementation is no longer used, see org.netbeans.modules.editor.impl.actions.NavigationHistoryBackAction
     * in the editor module.
     */
    public static class JumpListPrevAction extends LocalBaseAction {
    // Not registered by annotation since it's not actively used

        static final long serialVersionUID =7174907031986424265L;
        PropertyChangeListener pcl;

        public JumpListPrevAction() {
            super(BaseKit.jumpListPrevAction);
            putValue(BaseAction.ICON_RESOURCE_PROPERTY,
                "org/netbeans/modules/editor/resources/edit_previous.png"); // NOI18N
            JumpList.addPropertyChangeListener(pcl = new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    setEnabled(JumpList.hasPrev());
                }
            });
            setEnabled(JumpList.hasPrev());
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpPrev(target);
            }
        }
    }

    /**
     * This implementation is no longer used, see org.netbeans.modules.editor.impl.actions.NavigationHistoryForwardAction
     * in the editor module.
     */
    public static class JumpListNextComponentAction extends LocalBaseAction {
    // Not registered by annotation since it's not actively used

        static final long serialVersionUID =-2059070050865876892L;

        public JumpListNextComponentAction() {
            super(BaseKit.jumpListNextComponentAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpNextComponent(target);
            }
        }
    }

    /**
     * This implementation is no longer used, see org.netbeans.modules.editor.impl.actions.NavigationHistoryBackAction
     * in the editor module.
     */
    public static class JumpListPrevComponentAction extends LocalBaseAction {
    // Not registered by annotation since it's not actively used

        static final long serialVersionUID =2032230534727849525L;

        public JumpListPrevComponentAction() {
            super(BaseKit.jumpListPrevComponentAction);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                JumpList.jumpPrevComponent(target);
            }
        }
    }

    @EditorActionRegistration(name = BaseKit.scrollUpAction)
    public static class ScrollUpAction extends LocalBaseAction {

        public ScrollUpAction() {
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI editorUI = Utilities.getEditorUI(target);
                Rectangle bounds = editorUI.getExtentBounds();
                bounds.y += editorUI.getLineHeight();
                bounds.x += editorUI.getTextMargin().left;
                editorUI.scrollRectToVisible(bounds, EditorUI.SCROLL_SMALLEST);
            }
        }

    }

    @EditorActionRegistration(name = BaseKit.scrollDownAction)
    public static class ScrollDownAction extends LocalBaseAction {

        public ScrollDownAction() {
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                EditorUI editorUI = Utilities.getEditorUI(target);
                Rectangle bounds = editorUI.getExtentBounds();
                bounds.y -= editorUI.getLineHeight();
                bounds.x += editorUI.getTextMargin().left;
                editorUI.scrollRectToVisible(bounds, EditorUI.SCROLL_SMALLEST);
            }
        }

    }

    @EditorActionRegistration(name = BaseKit.insertDateTimeAction)
    public static class InsertDateTimeAction extends LocalBaseAction {
        
        static final long serialVersionUID =2865619897402L;
        
        public InsertDateTimeAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET | WORD_MATCH_RESET);
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                if (!target.isEditable() || !target.isEnabled()) {
                    target.getToolkit().beep();
                    return;
                }
                
                try {
                    Caret caret = target.getCaret();
                    BaseDocument doc = (BaseDocument)target.getDocument();
                    
                    // Format the current time.
                    SimpleDateFormat formatter = new SimpleDateFormat();
                    Date currentTime = new Date();
                    String dateString = formatter.format(currentTime);
                    
                    doc.insertString(caret.getDot(), dateString, null);
                } catch (BadLocationException e) {
                    target.getToolkit().beep();
                }
            }
        }
    }
    
    /** Select text of whole document */
    @EditorActionRegistration(name = BaseKit.generateGutterPopupAction)
    public static class GenerateGutterPopupAction extends LocalBaseAction {

        static final long serialVersionUID =-3502499718130556525L;

        public GenerateGutterPopupAction() {
            putValue(BaseAction.NO_KEYBINDING, Boolean.TRUE);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
        }

        public JMenuItem getPopupMenuItem(JTextComponent target) {
            EditorUI ui = Utilities.getEditorUI(target);
            try {
                return ui.getDocument().getAnnotations().createMenu(Utilities.getKit(target), Utilities.getLineOffset(ui.getDocument(),target.getCaret().getDot()));
            } catch (BadLocationException ex) {
                return null;
            }
        }
    
    }

    /**
     * Switch visibility of line numbers in editor
     * @deprecated this action is no longer used. It is reimplemented in editor.actions module.
     */
    //@EditorActionRegistration(name = BaseKit.toggleLineNumbersAction)
    // Registration in createActions() due to getPopupMenuItem()
    public static class ToggleLineNumbersAction extends LocalBaseAction {

        static final long serialVersionUID =-3502499718130556526L;
        
        private JCheckBoxMenuItem item = null;

        public ToggleLineNumbersAction() {
            super(BaseKit.toggleLineNumbersAction); // Due to creation from MainMenuAction
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            toggleLineNumbers();
        }
        
        public JMenuItem getPopupMenuItem(JTextComponent target) {
            
            item = new JCheckBoxMenuItem(NbBundle.getBundle(BaseKit.class).
                    getString("line-numbers-menuitem"), isLineNumbersVisible());
            item.addItemListener( new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    actionPerformed(null,null);
                }
            });
            return item;
        }
        
        protected boolean isLineNumbersVisible() {
            return false;
        }
        
        protected void toggleLineNumbers() {
        }
        
    }
    
    /** Cycle through annotations on the current line */
    @EditorActionRegistration(name = BaseKit.annotationsCyclingAction)
    public static class AnnotationsCyclingAction extends LocalBaseAction {
        
        public AnnotationsCyclingAction() {
            putValue(BaseAction.NO_KEYBINDING, Boolean.TRUE);
        }

        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            if (target != null) {
                try {
                    Caret caret = target.getCaret();
                    BaseDocument doc = Utilities.getDocument(target);
                    int caretLine = Utilities.getLineOffset(doc, caret.getDot());
                    AnnotationDesc aDesc = doc.getAnnotations().activateNextAnnotation(caretLine);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /** Returns the fold that should be collapsed/expanded in the caret row
     *  @param hierarchy hierarchy under which all folds should be collapsed/expanded.
     *  @param dot caret position offset
     *  @param lineStart offset of the start of line
     *  @param lineEnd offset of the end of line
     *  @return the fold that meet common criteria in accordance with the caret position
     */
    private static Fold getLineFold(FoldHierarchy hierarchy, int dot, int lineStart, int lineEnd){
        Fold caretOffsetFold = FoldUtilities.findOffsetFold(hierarchy, dot);

        // beginning searching from the lineStart
        Fold fold = FoldUtilities.findNearestFold(hierarchy, lineStart);  
        
        while (fold!=null && 
                  (fold.getEndOffset()<=dot || // find next available fold if the 'fold' is one-line
                      // or it has children and the caret is in the fold body
                      // i.e. class A{ |public void method foo(){}}
                      (!fold.isCollapsed() && fold.getFoldCount() > 0  && fold.getStartOffset()+1<dot) 
                   )
               ){

                   // look for next fold in forward direction
                   Fold nextFold = FoldUtilities.findNearestFold(hierarchy,
                       (fold.getFoldCount()>0) ? fold.getStartOffset()+1 : fold.getEndOffset());
                   if (nextFold!=null && nextFold.getStartOffset()<lineEnd){
                       if (nextFold == fold) return fold;
                       fold = nextFold;
                   }else{
                       break;
                   }
        }

        
        // a fold on the next line was found, returning fold at offset (in most cases inner class)
        if (fold == null || fold.getStartOffset()>lineEnd) {

            // in the case:
            // class A{
            // }     |
            // try to find an offset fold on the offset of the line beginning
            if (caretOffsetFold == null){
                caretOffsetFold = FoldUtilities.findOffsetFold(hierarchy, lineStart);
            }
            
            return caretOffsetFold;
        }
        
        // no fold at offset found, in this case return the fold
        if (caretOffsetFold == null) return fold;
        
        // skip possible inner class members validating if the innerclass fold is collapsed
        if (caretOffsetFold.isCollapsed()) return caretOffsetFold;
        
        // in the case:
        // class A{
        // public vo|id foo(){} }
        // 'fold' (in this case fold of the method foo) will be returned
        if ( caretOffsetFold.getEndOffset()>fold.getEndOffset() && 
             fold.getEndOffset()>dot){
            return fold;
        }
        
        // class A{
        // |} public void method foo(){}
        // inner class fold will be returned
        if (fold.getStartOffset()>caretOffsetFold.getEndOffset()) return caretOffsetFold;
        
        // class A{
        // public void foo(){} |}
        // returning innerclass fold
        if (fold.getEndOffset()<dot) return caretOffsetFold;
        
        return fold;
    }
    
    /** Collapse a fold. Depends on the current caret position. */
    @EditorActionRegistration(name = BaseKit.collapseFoldAction,
            menuText = "#" + BaseKit.collapseFoldAction + "_menu_text")
    public static class CollapseFold extends LocalBaseAction {
        public CollapseFold(){
        }
        
        private boolean dotInFoldArea(JTextComponent target, Fold fold, int dot) throws BadLocationException{
            int foldStart = fold.getStartOffset();
            int foldEnd = fold.getEndOffset();
            int foldRowStart = javax.swing.text.Utilities.getRowStart(target, foldStart);
            int foldRowEnd = javax.swing.text.Utilities.getRowEnd(target, foldEnd);
            if (foldRowStart > dot || foldRowEnd < dot) return false; // it's not fold encapsulating dot
            return true;
            }

        
        public void actionPerformed(ActionEvent evt, final JTextComponent target) {
            Document doc = target.getDocument();
            doc.render(new Runnable() {
                @Override
                public void run() {
                    FoldHierarchy hierarchy = FoldHierarchy.get(target);
                    int dot = target.getCaret().getDot();
                    hierarchy.lock();
                    try{
                        try{
                            int rowStart = javax.swing.text.Utilities.getRowStart(target, dot);
                            int rowEnd = javax.swing.text.Utilities.getRowEnd(target, dot);
                            Fold fold = FoldUtilities.findNearestFold(hierarchy, rowStart);
                            fold = getLineFold(hierarchy, dot, rowStart, rowEnd);
                            if (fold==null){
                                return; // no success
                            }
                            // ensure we' got the right fold
                            if (dotInFoldArea(target, fold, dot)){
                                hierarchy.collapse(fold);
                            }
                        }catch(BadLocationException ble){
                            ble.printStackTrace();
                        }
                    }finally {
                        hierarchy.unlock();
                    }
                }
            });
        }
    }
    
    /** Expand a fold. Depends on the current caret position. */
    @EditorActionRegistration(name = BaseKit.expandFoldAction,
            menuText = "#" + BaseKit.expandFoldAction + "_menu_text")
    public static class ExpandFold extends LocalBaseAction {
        public ExpandFold(){
        }
        
        public void actionPerformed(ActionEvent evt, final JTextComponent target) {
            Document doc = target.getDocument();
            doc.render(new Runnable() {
                @Override
                public void run() {
                    FoldHierarchy hierarchy = FoldHierarchy.get(target);
                    int dot = target.getCaret().getDot();
                    hierarchy.lock();
                    try {
                        try {
                            int rowStart = javax.swing.text.Utilities.getRowStart(target, dot);
                            int rowEnd = javax.swing.text.Utilities.getRowEnd(target, dot);
                            Fold fold = getLineFold(hierarchy, dot, rowStart, rowEnd);
                            if (fold != null) {
                                hierarchy.expand(fold);
                            }
                        } catch (BadLocationException ble) {
                            ble.printStackTrace();
                        }
                    } finally {
                        hierarchy.unlock();
                    }
                }
            });
        }
    }
    
    /** Collapse all existing folds in the document. */
    @EditorActionRegistration(name = BaseKit.collapseAllFoldsAction,
            menuText = "#" + BaseKit.collapseAllFoldsAction + "_menu_text")
    public static class CollapseAllFolds extends LocalBaseAction {
        public CollapseAllFolds(){
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            FoldHierarchy hierarchy = FoldHierarchy.get(target);
            // Hierarchy locking done in the utility method
            FoldUtilities.collapseAll(hierarchy);
        }
    }

    /** Expand all existing folds in the document. */
    @EditorActionRegistration(name = BaseKit.expandAllFoldsAction,
            menuText = "#" + BaseKit.expandAllFoldsAction + "_menu_text")
    public static class ExpandAllFolds extends LocalBaseAction {
        public ExpandAllFolds(){
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            FoldHierarchy hierarchy = FoldHierarchy.get(target);
            // Hierarchy locking done in the utility method
            FoldUtilities.expandAll(hierarchy);
        }
    }

    /** Expand all existing folds in the document. */
    @EditorActionRegistration(name = "dump-view-hierarchy")
    public static class DumpViewHierarchyAction extends LocalBaseAction {

        public DumpViewHierarchyAction() {
            putValue(BaseAction.NO_KEYBINDING, Boolean.TRUE);
        }
        
        public void actionPerformed(ActionEvent evt, JTextComponent target) {
            AbstractDocument adoc = (AbstractDocument)target.getDocument();

            // Dump fold hierarchy
            FoldHierarchy hierarchy = FoldHierarchy.get(target);
            adoc.readLock();
            try {
                hierarchy.lock();
                try {
                    /*DEBUG*/System.err.println("FOLD HIERARCHY DUMP:\n" + hierarchy); // NOI18N
                    TokenHierarchy<?> th = TokenHierarchy.get(adoc);
                    /*DEBUG*/System.err.println("TOKEN HIERARCHY DUMP:\n" + (th != null ? th : "<NULL-TH>")); // NOI18N

                } finally {
                    hierarchy.unlock();
                }
            } finally {
                adoc.readUnlock();
            }

            View rootView = null;
            TextUI textUI = target.getUI();
            if (textUI != null) {
                rootView = textUI.getRootView(target); // Root view impl in BasicTextUI
                if (rootView != null && rootView.getViewCount() == 1) {
                    rootView = rootView.getView(0); // Get DocumentView
                }
            }
            if (rootView != null) {
                String rootViewDump = (rootView instanceof DocumentView)
                        ? ((DocumentView)rootView).toStringDetail()
                        : rootView.toString();
                /*DEBUG*/System.err.println("DOCUMENT VIEW: " + System.identityHashCode(rootView) + // NOI18N
                        "\n" + rootViewDump); // NOI18N
                int caretOffset = target.getCaretPosition();
                int caretViewIndex = rootView.getViewIndex(caretOffset, Position.Bias.Forward);
                /*DEBUG*/System.err.println("caretOffset=" + caretOffset + ", caretViewIndex=" + caretViewIndex); // NOI18N
                if (caretViewIndex >= 0 && caretViewIndex < rootView.getViewCount()) {
                    View caretView = rootView.getView(caretViewIndex);
                    /*DEBUG*/System.err.println("caretView: " + caretView); // NOI18N
                }
                /*DEBUG*/System.err.println(FixLineSyntaxState.lineInfosToString(adoc));
                // Check the hierarchy correctness
                //org.netbeans.editor.view.spi.ViewUtilities.checkViewHierarchy(rootView);
            }
            
            if (adoc instanceof BaseDocument) {
                /*DEBUG*/System.err.println("DOCUMENT:\n" + ((BaseDocument)adoc).toStringDetail()); // NOI18N
            }
        }
    }
    
    @EditorActionRegistration(name = BaseKit.startNewLineAction)
    public static class StartNewLine extends LocalBaseAction {

        public StartNewLine() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
        }

        @Override
        public void actionPerformed(final ActionEvent evt, final JTextComponent target) {
            // shift-enter while editing aka startNewLineAction
            if (!target.isEditable() || !target.isEnabled()) {
                target.getToolkit().beep();
                return;
            }

            final BaseDocument doc = (BaseDocument) target.getDocument();
            final Indent indenter = Indent.get(doc);
            indenter.lock();
            doc.runAtomicAsUser(new Runnable() {
                public void run() {
                    try {
                        Caret caret = target.getCaret();

                        // insert new line, caret moves to the new line
                        int eolDot = Utilities.getRowEnd(target, caret.getDot());
                        doc.insertString(eolDot, "\n", null); //NOI18N

                        // reindent the new line
                        Position newDotPos = doc.createPosition(eolDot + 1);
                        indenter.reindent(eolDot + 1);

                        caret.setDot(newDotPos.getOffset());
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    } finally {
                        indenter.unlock();
                    }
                }
            });
        }
    }
    
    /**
     * Cut text from the caret position to either begining or end
     * of the line with the caret.
     */
    @EditorActionRegistrations({
        @EditorActionRegistration(name = BaseKit.cutToLineBeginAction),
        @EditorActionRegistration(name = BaseKit.cutToLineEndAction)
    })
    public static class CutToLineBeginOrEndAction extends LocalBaseAction {
        
        /**
         * Construct new action.
         *
         * @param toLineEnd whether cutting to line end instead of line begin.
         */
        public CutToLineBeginOrEndAction() {
            super(ABBREV_RESET | MAGIC_POSITION_RESET | UNDO_MERGE_RESET);
        }
        
        public void actionPerformed (final ActionEvent evt, final JTextComponent target) {
            // shift-enter while editing aka startNewLineAction
            if (!target.isEditable() || !target.isEnabled()) {
                target.getToolkit().beep();
                return;
            }
            
            final BaseDocument doc = (BaseDocument)target.getDocument();
            
            doc.runAtomicAsUser (new Runnable () {
                public void run () {
                DocumentUtilities.setTypingModification(doc, true);
                try {
                    ActionMap actionMap = target.getActionMap();
                    Action cutAction;
                    if (actionMap != null && (cutAction = actionMap.get(DefaultEditorKit.cutAction)) != null) {
                        Caret caret = target.getCaret();
                        int caretOffset = caret.getDot();
                        boolean toLineEnd = BaseKit.cutToLineEndAction.equals(getValue(Action.NAME));
                        int boundOffset = toLineEnd
                                ? Utilities.getRowEnd(target, caretOffset)
                                : Utilities.getRowStart(target, caretOffset);

                        // Check whether there is only whitespace from caret position
                        // till end of line
                        if (toLineEnd) {
                            String text = target.getText(caretOffset, boundOffset - caretOffset);
                            if (boundOffset < doc.getLength() && text != null && text.matches("^[\\s]*$")) { // NOI18N
                                boundOffset += 1; // Include line separator
                            }
                        }

                        caret.moveDot(boundOffset);

                        // Call the cut action to cut out the selection
                        cutAction.actionPerformed(evt);
                    }
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                } finally{
                    DocumentUtilities.setTypingModification(doc, false);
                }
                }
            });
        }
    }
    
    
}