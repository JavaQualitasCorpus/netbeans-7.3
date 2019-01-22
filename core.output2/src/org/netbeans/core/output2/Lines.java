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
package org.netbeans.core.output2;

import java.awt.Color;
import org.openide.windows.OutputListener;

import javax.swing.event.ChangeListener;
import java.io.IOException;
import java.util.Collection;
import org.openide.windows.IOColors;

/**
 * An interface representing the data written to an OutWriter, in terms of lines of text, with
 * methods for handling line wrapping.
 */
public interface Lines {
    /**
     * Get an array of all line numbers which have associated OutputListeners
     * @return an array of line numbers
     */
    int[] getLinesWithListeners();

    /**
     * Get the length, in characters, of a given line index
     *
     * @param idx the line number
     * @return the length
     */
    int length (int idx);

    int lengthWithTabs (int idx);

    /**
     * Get the character position corresponding to the start of a line
     *
     * @param line A line number
     * @return The character in the total text at which this line starts
     */
    int getLineStart (int line);

    /**
     * Get the line index of the nearest line start to this character position in the
     * entire stored text
     * @param position
     * @return The line on which this character position occurs
     */
    int getLineAt (int position);

    /**
     * Get the number of lines in the stored text
     * @return A line count
     */
    int getLineCount();

    /**
     * Get the output listeners associated with this line
     * @param line A line number
     * @return Collection of OutputListeners associated with this line
     */
    Collection<OutputListener> getListenersForLine(int line);

    /**
     * Get informations for line
     * @param line A line number
     * @return LineInfo for specified line
     */
    LineInfo getLineInfo(int line);

    /**
     * Sets default colors
     * @param type line type
     * @param color color
     */
    void setDefColor(IOColors.OutputType type, Color color);

    /**
     * Gets default color
     * @param type output type
     * @return corresponding Color
     */
    Color getDefColor(IOColors.OutputType type);

    /**
     * Get the index of the first line which has a listener
     * @return A line number, or -1 if there are no listeners
     */
    int firstListenerLine ();
    
    /**
     * Get the index of the first line which has an important listener
     * @return A line number, or -1 if there are no important listeners
     */
    int firstImportantListenerLine();

    /**
     * Check whether specified line contains important listener
     * @param line line to be checked
     * @return true if this line contains important listener
     */
    boolean isImportantLine(int line);

    /**
     *
     * @param startSearchPos starting position for searching
     * @param backward direction of searching
     * @param range [startPos, endPosition] of listener
     * @return nearest listener to specified startSearchPosition
     */
    OutputListener nearestListener(int startSearchPos, boolean backward, int[] range);

    /**
     * Get the length of the longest line in the storage
     * @return The longest line's length
     */
    int getLongestLineLength();

    /**
     * Get the count of logical (wrapped) lines above the passed index.  The passed index should be a line
     * index in a physical coordinate space in which lines are wrapped at charCount.  It will return the
     * number of logical (wrapped) lines above this line.
     *
     * @param logicalLine A line index in wrapped, physical space
     * @param charCount The number of characters at which line wrapping happens
     * @return The number of  logical lines above this one.
     */
    int getLogicalLineCountAbove (int logicalLine, int charCount);

    /**
     * Get the total number of logical lines required to display the stored text if wrapped at the specified
     * character count.
     * @param charCount The number of characters at which line wrapping happens
     * @return The number of logical lines needed to fit all of the text
     */
    int getLogicalLineCountIfWrappedAt (int charCount);

    /**
     * Get number of physical characters for the given logical (expanded TABs) length.
     * @param offset
     * @param logicalLength
     * @param tabShiftPtr
     * @return
     */
    public int getNumPhysicalChars(int offset, int logicalLength, int[] tabShiftPtr);

    public int getNumLogicalChars(int offset, int physicalLength);
    
    /**
     * Determine if a character position indicates the first character of a line.
     *
     * @param chpos A character index in the stored text
     * @return Whether or not it's the first character of a line
     */
    boolean isLineStart (int chpos);

    /**
     * Get a line of text
     *
     * @param idx A line number
     * @return The text
     * @throws IOException If an error occurs and the text cannot be read
     */
    String getLine (int idx) throws IOException;

    /**
     * Determine if there are any lines with associated output listeners
     * @return True if there are any listeners
     */
    boolean hasListeners();

    /**
     * Get listener on specified position
     * @param pos position where look for listener
     * @param range if hyperlink exists on specified position [start position,
     * end position] is returned in this array (may be null)
     * @return listener (hyperlink) on specified position or null
     */
     OutputListener getListener(int pos, int[] range);

     /**
      * Check if listener exists on specified position
      * @param start start position of listener
      * @param end end position of listener
      * @return true if listener exists on specified position
      */
     boolean isListener(int start, int end);

    /**
     * Count the total number of characters in the stored text
     *
     * @return The number of characters that have been written
     */
    int getCharCount();

    /**
     * Fetch a getText of the entire text
     * @param start A character position < end
     * @param end A character position > start
     * @return A String representation of the text between these points, including newlines
     */
    String getText (int start, int end);

    /**
     * Fetch a getText of the entire text into a character array
     * @param start A character position < end
     * @param end A character position >  start
     * @param chars A character array at least as large as end-start, or null
     * @return A character array containing the range of text specified
     */
    char[] getText (int start, int end, char[] chars);

    /**
      * Get a physical (real) line index for logical (wrapped) line index.
      * This is to accomodate line wrapping using fixed width fonts. This
      * method answers the question "Which physical (real) line corresponds
      * to certain logical (wrapped) line if we wrap at <code>charsPerLine</code>.
      * It will also return number of wrapped lines for found physical line and
      * the position (index) on this wrapped line.
      *
      * @param info A 3 entry array.  Element 0 should be the logical (wrapped)
      *        line when called;
      *        the other two elements are ignored.  On return,
      *        it contains: <ul>
      *         <li>[0] The physical (real) line index for the passed line</li>
      *         <li>[1] Index of logical line within this physical line</li>
      *         <li>[2] The total number of line wraps for the physical line</li>
      *         </ul>
      */
    void toPhysicalLineIndex (int[] info, int charsPerLine);

    /**
     * Save the contents of the buffer to a file, in platform default encoding.
     *
     * @param path The file to save to
     * @throws IOException If there is a problem writing or encoding the data, or if overwrite is false and the
     *    specified file exists
     */
    void saveAs(String path) throws IOException;

    /**
     *
     * @param start start position for search
     * @param pattern pattern to search
     * @param regExp pattern is regexp (false to escape meta chars)
     * @param matchCase true to match case
     * @return [start, end] position of match
     */
    public int[] find(int start, String pattern, boolean regExp, boolean matchCase);

    /**
     *
     * @param start start position for reverse search
     * @param pattern pattern to search
     * @param regExp pattern is regexp (false to escape meta chars)
     * @param matchCase true to match case
     * @return [start, end] position of match
     */
    public int[] rfind(int start, String pattern, boolean regExp, boolean matchCase);

    /**
     * Acquire a read lock - while held, other threads cannot modify this Lines object.
     *
     * @return
     */
    Object readLock();

    /**
     * Add a change listener, which will detect when lines are written. <strong>Changes are
     * <u>not</u> fired for every write; they should be fired when an initial line is written,
     * when the writer is flushed, or when it is closed.</strong>  Clients which respond to ongoing
     * writes should use a timer and poll via <code>checkDirty()</code> to see if new data has
     * been written.
     *
     * @param cl A change listener
     */
    void addChangeListener (ChangeListener cl);

    /**
     * Remove a change listener.
     *
     * @param cl
     */
    void removeChangeListener (ChangeListener cl);

    /**
     * Allows clients that wish to poll to see if there is new output to do
     * so.  When any thread writes to the output, the dirty flag is set.
     * Calling this method returns its current value and clears it.  If it
     * returns true, a view of the data may need to repaint itself or something
     * such.  This mechanism can be used in preference to listener based
     * notification, by running a timer to poll as long as the output is
     * open, for cases where otherwise the event queue would be flooded with
     * notifications for small writes.
     *
     * @param clear Whether or not to clear the dirty flag
     */
    boolean checkDirty(boolean clear);

    /**
     * Determine whether or not the storage backing this Lines object is being actively written to.
     * @return True if there is still an open stream which may write to the backing storage and no error has occured
     */
    boolean isGrowing();
}