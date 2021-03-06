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
package org.netbeans.modules.gsf.codecoverage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.spi.GsfUtilities;
import org.netbeans.modules.gsf.codecoverage.api.CoverageType;
import org.netbeans.modules.gsf.codecoverage.api.FileCoverageDetails;
import org.netbeans.spi.editor.highlighting.HighlightsSequence;
import org.netbeans.spi.editor.highlighting.support.AbstractHighlightsContainer;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.WeakListeners;

/**
 * Highlight coverage lines and react to line edits
 *
 * @author Tor Norbye
 */
public class CoverageHighlightsContainer extends AbstractHighlightsContainer implements DocumentListener {
    private AttributeSet covered;
    private AttributeSet uncovered;
    private AttributeSet inferred;
    private AttributeSet partial;
    private List<Position> lastPositions;
    private List<CoverageType> lastTypes;
    private boolean enabled;
    private boolean listening;
    private final BaseDocument doc;
    private final String mimeType;
    private long version = 0;
    private FileObject fileObject;
    private Project project;

    CoverageHighlightsContainer(Document document) {
        if (document instanceof BaseDocument) {
            this.doc = (BaseDocument) document;
        } else {
            this.doc = null;
        }
        this.mimeType = (String) document.getProperty("mimeType");
    }

    public HighlightsSequence getHighlights(int startOffset, int endOffset) {
        enabled = false;
        CoverageManagerImpl manager = CoverageManagerImpl.getInstance();
        if (doc == null || manager == null || !manager.isEnabled(mimeType)) {
            return HighlightsSequence.EMPTY;
        }
        enabled = true;
        synchronized (this) {
            if (fileObject == null) {
                fileObject = GsfUtilities.findFileObject(doc);
                if (fileObject != null) {
                    project = FileOwnerQuery.getOwner(fileObject);
                } else {
                    project = null;
                }

                if (fileObject == null || project == null) {
                    return HighlightsSequence.EMPTY;
                }
            }
        }

        FileCoverageDetails details = manager.getDetails(project, fileObject, doc);
        if (details == null) {
            return HighlightsSequence.EMPTY;
        }

        initColors();
        if (!listening) {
            listening = true;
            doc.addDocumentListener(WeakListeners.document(this, null));
        }

        return new Highlights(0, startOffset, endOffset, details);
    }

    private static Color getColoring(FontColorSettings fcs, String tokenName) {
        AttributeSet as = fcs.getTokenFontColors(tokenName);
        if (as != null) {
            return (Color) as.getAttribute(StyleConstants.Background); //NOI18N
        }
        return null;
    }

    private void initColors() {
        if (covered != null) {
            return;
        }

        Color coveredBc = null;
        Color uncoveredBc = null;
        Color inferredBc = null;
        Color partialBc = null;
        FontColorSettings fcs = MimeLookup.getLookup(mimeType).lookup(FontColorSettings.class);
        if (fcs != null) {
            coveredBc = getColoring(fcs, "covered"); // NOI18N
            uncoveredBc = getColoring(fcs, "uncovered"); // NOI18N
            inferredBc = getColoring(fcs, "inferred"); // NOI18N
            partialBc = getColoring(fcs, "partial"); // NOI18N
        }
        if (coveredBc == null) {
            coveredBc = new Color(0xCC, 0xFF, 0xCC);
        }
        if (uncoveredBc == null) {
            uncoveredBc = new Color(0xFF, 0xCC, 0xCC);
        }
        if (inferredBc == null) {
            inferredBc = new Color(0xE0, 0xFF, 0xE0);
        }
        if (partialBc == null) {
            partialBc = new Color(0xFF, 0xFF, 0xE0);
        }

        covered = coveredBc == null ? SimpleAttributeSet.EMPTY : AttributesUtilities.createImmutable(
                StyleConstants.Background, coveredBc,
                ATTR_EXTENDS_EOL, Boolean.TRUE, ATTR_EXTENDS_EMPTY_LINE, Boolean.TRUE);
        uncovered = uncoveredBc == null ? SimpleAttributeSet.EMPTY : AttributesUtilities.createImmutable(
                StyleConstants.Background, uncoveredBc,
                ATTR_EXTENDS_EOL, Boolean.TRUE, ATTR_EXTENDS_EMPTY_LINE, Boolean.TRUE);
        inferred = inferredBc == null ? SimpleAttributeSet.EMPTY : AttributesUtilities.createImmutable(
                StyleConstants.Background, inferredBc,
                ATTR_EXTENDS_EOL, Boolean.TRUE, ATTR_EXTENDS_EMPTY_LINE, Boolean.TRUE);
        partial = partialBc == null ? SimpleAttributeSet.EMPTY : AttributesUtilities.createImmutable(
                StyleConstants.Background, partialBc,
                ATTR_EXTENDS_EOL, Boolean.TRUE, ATTR_EXTENDS_EMPTY_LINE, Boolean.TRUE);
    }

    void refresh() {
        lastPositions = null;
        lastTypes = null;
        fireHighlightsChange(0, doc.getLength());
    }

    private void handleEdits(int offset, int length, boolean inserted) {
        // If we're editing a document with coverage highlights, we need to
        // do a couple of things:
        // (1) If you're inserting a newline AFTER the text on a line, simply
        //    insert a blank (non-highlighted) line after the previous line
        // (2) If you're inserting a newline at the beginning of a highlighted
        //    line (including in the whitespace prefix of the line), then move
        //    the highlight down, and insert a blank line in the previous position
        // (3) If you're editing somewhere in the middle of a line, clear the
        //    coverage line highlight
        try {
            assert length > 0;
            if (inserted) {
                String s = doc.getText(offset, length);
                // Can't just check for \n, because on a newline some languages also
                // add and subtract spaces to the document to implement
                // smart-indent
                if ((s.trim().length() == 0)) { // whitespace changes only?
                    if (Utilities.isRowEmpty(doc, offset) ||
                            (offset >= Utilities.getRowLastNonWhite(doc, offset) + 1) ||
                            (offset <= Utilities.getRowFirstNonWhite(doc, offset))) {
                        fireHighlightsChange(offset, offset + length);
                        return;
                    }
                }
            }

            int lineStart = Utilities.getRowFirstNonWhite(doc, offset);
            if (lineStart == -1) {
                lineStart = Utilities.getRowStart(doc, offset);
            }
            List<Position> positions = lastPositions;
            if (positions != null) {
                int positionIndex = findPositionIndex(positions, lineStart);
                if (positionIndex >= 0) {
                    // Create a new list to avoid sync problems
                    List<Position> copy = new ArrayList<Position>(positions);
                    copy.remove(positionIndex);
                    lastPositions = copy;
                    fireHighlightsChange(offset, offset + length);
                }
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void insertUpdate(DocumentEvent ev) {
        if (enabled) {
            handleEdits(ev.getOffset(), ev.getLength(), true);
        }
    }

    public void removeUpdate(DocumentEvent ev) {
        //if (enabled) {
        //    handleEdits(ev.getOffset(), ev.getLength(), false);
        //}
    }

    public void changedUpdate(DocumentEvent ev) {
    }

    private int findPositionIndex(List<Position> positions, final int target) {
        return Collections.binarySearch(positions, new Position() {
            public int getOffset() {
                return target;
            }
        }, new Comparator<Position>() {
            public int compare(Position pos1, Position pos2) {
                return pos1.getOffset() - pos2.getOffset();
            }
        });
    }

    private class Highlights implements HighlightsSequence {
        private final List<Position> positions;
        private final List<CoverageType> types;
        private final long version;
        private final int startOffsetBoundary;
        private final int endOffsetBoundary;
        private int startOffset;
        private int endOffset;
        private AttributeSet attributeSet;
        private boolean finished = false;
        private int index;

        private Highlights(long version, int startOffset, int endOffset, FileCoverageDetails details) {
            this.version = version;
            this.startOffsetBoundary = startOffset;
            this.endOffsetBoundary = endOffset;

            if (lastPositions == null) {
                positions = new ArrayList<Position>();
                types = new ArrayList<CoverageType>();
                for (int lineno = 0, maxLines = details.getLineCount(); lineno < maxLines; lineno++) {
                    CoverageType type = details.getType(lineno);
                    if (type == CoverageType.COVERED || type == CoverageType.INFERRED ||
                            type == CoverageType.NOT_COVERED || type == CoverageType.PARTIAL) {
                        try {
                            int offset = Utilities.getRowStartFromLineOffset(doc, lineno);
                            if (offset == -1) {
                                continue;
                            }
                            // Attach the highlight position to the beginning of text, such
                            // that if we insert a new line at the beginning of a line (or in
                            // the whitespace region) the highlight will move down with the
                            // text
                            int rowStart = Utilities.getRowFirstNonWhite(doc, offset);
                            if (rowStart != -1) {
                                offset = rowStart;
                            }
                            Position pos = doc.createPosition(offset, Position.Bias.Forward);
                            positions.add(pos);
                            types.add(type);
                        } catch (BadLocationException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
                lastPositions = positions;
                lastTypes = types;
            } else {
                positions = lastPositions;
                types = lastTypes;
            }

            try {
                int lineStart = Utilities.getRowFirstNonWhite(doc, startOffsetBoundary);
                if (lineStart == -1) {
                    lineStart = Utilities.getRowStart(doc, startOffsetBoundary);
                    index = findPositionIndex(positions, lineStart);
                    if (index < 0) {
                        index = -index;
                    }
                }
            } catch (BadLocationException ble) {
            }
        }

        private boolean _moveNext() {
            for (; index < positions.size(); index++) {
                Position pos = positions.get(index);
                int offset = pos.getOffset();
                try {
                    offset = Utilities.getRowStart(doc, offset);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
                if (offset > endOffsetBoundary) {
                    break;
                }
                if (offset >= startOffsetBoundary) {
                    startOffset = offset;
                    try {
                        endOffset = Utilities.getRowEnd(doc, offset);
                        if (endOffset < doc.getLength()) {
                            endOffset++; // Include end of line
                        }
                        CoverageType type = types.get(index);
                        switch (type) {
                            case COVERED:
                                attributeSet = covered;
                                break;
                            case NOT_COVERED:
                                attributeSet = uncovered;
                                break;
                            case INFERRED:
                                attributeSet = inferred;
                                break;
                            case PARTIAL:
                                attributeSet = partial;
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    index++;
                    return true;
                }
            }

            return false;
        }

        public boolean moveNext() {
            synchronized (CoverageHighlightsContainer.this) {
                if (checkVersion()) {
                    if (_moveNext()) {
                        return true;
                    }
                }
            }

            finished = true;
            return false;
        }

        public int getStartOffset() {
            synchronized (CoverageHighlightsContainer.this) {
                if (finished) {
                    throw new NoSuchElementException();
                } else {
                    return startOffset;
                }
            }
        }

        public int getEndOffset() {
            synchronized (CoverageHighlightsContainer.this) {
                if (finished) {
                    throw new NoSuchElementException();
                } else {
                    return endOffset;
                }
            }
        }

        public AttributeSet getAttributes() {
            synchronized (CoverageHighlightsContainer.this) {
                if (finished) {
                    throw new NoSuchElementException();
                } else {
                    return attributeSet;
                }
            }
        }

        private boolean checkVersion() {
            return this.version == CoverageHighlightsContainer.this.version;
        }
    }
}
