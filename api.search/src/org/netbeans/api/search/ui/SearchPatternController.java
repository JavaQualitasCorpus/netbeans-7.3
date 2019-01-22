/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.api.search.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.search.SearchHistory;
import org.netbeans.api.search.SearchPattern;
import org.netbeans.modules.search.FindDialogMemory;
import org.netbeans.modules.search.ui.PatternChangeListener;
import org.netbeans.modules.search.ui.ShorteningCellRenderer;
import org.netbeans.modules.search.ui.TextFieldFocusListener;
import org.netbeans.modules.search.ui.UiUtils;
import org.openide.util.Parameters;

/**
 * Controller for a combo box for selection and editing of search patterns.
 *
 * @author jhavlin
 * @since api.search/1.1
 */
public final class SearchPatternController
        extends ComponentController<JComboBox> {

    private JTextComponent textToFindEditor;

    /**
     * Options of search patterns.
     */
    public enum Option {
        MATCH_CASE, WHOLE_WORDS, REGULAR_EXPRESSION
    }
    private final Map<Option, AbstractButton> bindings =
            new EnumMap<Option, AbstractButton>(Option.class);
    private final Map<Option, Boolean> options =
            new EnumMap<Option, Boolean>(Option.class);
    private final ItemListener listener;
    private boolean valid;
    private Color defaultTextColor = null;

    SearchPatternController(final JComboBox component) {
        super(component);
        component.setEditable(true);
        Component cboxEditorComp = component.getEditor().getEditorComponent();
        component.setRenderer(new ShorteningCellRenderer());
        textToFindEditor = (JTextComponent) cboxEditorComp;
        textToFindEditor.getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        patternChanged();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        patternChanged();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        patternChanged();
                    }
                });
        initHistory();
        valid = checkValid();
        updateTextPatternColor();
        textToFindEditor.addFocusListener(new TextFieldFocusListener());
        textToFindEditor.getDocument().addDocumentListener(
                new TextToFindChangeListener());

        component.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Object si = component.getSelectedItem();
                if (si instanceof ModelItem) {
                    SearchPattern sp = ((ModelItem) si).sp;
                    for (Map.Entry<Option, AbstractButton> be :
                            bindings.entrySet()) {
                        switch (be.getKey()) {
                            case MATCH_CASE:
                                be.getValue().setSelected(sp.isMatchCase());
                                break;
                            case WHOLE_WORDS:
                                be.getValue().setSelected(sp.isWholeWords());
                                break;
                            case REGULAR_EXPRESSION:
                                be.getValue().setSelected(sp.isRegExp());
                                break;
                        }
                    }
                    options.put(Option.MATCH_CASE, sp.isMatchCase());
                    options.put(Option.WHOLE_WORDS, sp.isWholeWords());
                    options.put(Option.REGULAR_EXPRESSION, sp.isRegExp());
                }
            }
        });

        listener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                for (Map.Entry<Option, AbstractButton> entry :
                        bindings.entrySet()) {
                    if (entry.getValue() == e.getSource()) {
                        setOption(entry.getKey(),
                                e.getStateChange() == ItemEvent.SELECTED);
                        break;
                    }
                }
            }
        };
    }

    private void initHistory() {
        final DefaultComboBoxModel model = new DefaultComboBoxModel();
        final List<SearchPattern> data =
                SearchHistory.getDefault().getSearchPatterns();

        for (SearchPattern sp : data) {
            model.addElement(new ModelItem(sp));
        }

        component.setModel(model);

        if (data.size() > 0) {
            setSearchPattern(data.get(0));
        }
        if (!FindDialogMemory.getDefault().isTextPatternSpecified()) {
            component.setSelectedItem("");                              //NOI18N
        }
    }

    /**
     * Get text currently shown in the combo box editor.
     */
    private @NonNull String getText() {
        String s = textToFindEditor.getText();
        return s == null ? "" : s;                                      //NOI18N
    }

    /**
     * Set text to show in combo box editor
     *
     * @param text Text to show in the component editor.
     */
    private void setText(@NullAllowed String text) {
        component.setSelectedItem(text == null ? "" : text);            //NOI18N
    }

    /**
     * Get current value of an option of the search pattern.
     */
    private boolean getOption(@NonNull Option option) {
        Parameters.notNull("option", option);                           //NOI18N
        Boolean b = options.get(option);
        if (b == null) {
            return false;
        } else {
            return b.booleanValue();
        }
    }

    /**
     * Set value of a search pattern option. Bound abstract button will be
     * selected or deselected accordingly.
     */
    private void setOption(@NonNull Option option, boolean value) {
        Parameters.notNull("option", option);                           //NOI18N
        options.put(option, value);
        AbstractButton button = bindings.get(option);
        if (button != null) {
            button.setSelected(value);
        }
        if (option == Option.REGULAR_EXPRESSION) {
            updateValidity();
        }
        fireChange();
    }

    /**
     * Get search pattern currently represented by this controller.
     */
    public @NonNull SearchPattern getSearchPattern() {
        return SearchPattern.create(getText(),
                getOption(Option.WHOLE_WORDS),
                getOption(Option.MATCH_CASE),
                getOption(Option.REGULAR_EXPRESSION));
    }

    /**
     * Set text and options to represent a search pattern.
     */
    public void setSearchPattern(@NonNull SearchPattern searchPattern) {
        Parameters.notNull("searchPattern", searchPattern);             //NOI18N
        setText(searchPattern.getSearchExpression());
        setOption(Option.WHOLE_WORDS, searchPattern.isWholeWords());
        setOption(Option.MATCH_CASE, searchPattern.isMatchCase());
        setOption(Option.REGULAR_EXPRESSION, searchPattern.isRegExp());
    }

    /**
     * Bind an abstract button (usually checkbox) to a SearchPattern option.
     *
     * @param option Option whose value the button should represent.
     * @param button Button to control and display the option.
     */
    public void bind(@NonNull final Option option,
            @NonNull final AbstractButton button) {
        Parameters.notNull("option", option);                           //NOI18N
        Parameters.notNull("button", button);                           //NOI18N

        if (bindings.containsKey(option)) {
            throw new IllegalStateException(
                    "Already binded with option " + option);           // NOI18N
        }

        bindings.put(option, button);
        button.setSelected(getOption(option));
        button.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                setOption(option, button.isSelected());
            }
        });
    }

    /**
     * Unbind a button from a SearchPattern option.
     */
    public void unbind(@NonNull Option option, @NonNull AbstractButton button) {
        Parameters.notNull("option", option);                           //NOI18N
        Parameters.notNull("button", button);                           //NOI18N
        bindings.remove(option);
        button.removeItemListener(listener);
    }

    private class TextToFindChangeListener extends PatternChangeListener {

        @Override
        public void handleComboBoxChange(String text) {
            patternChanged();
        }
    }

    private void patternChanged() {
        updateValidity();
        fireChange();
    }

    private void updateValidity() {
        boolean wasValid = valid;
        valid = checkValid();
        if (valid != wasValid) {
            updateTextPatternColor();
        }
    }

    private boolean checkValid() {
        String expr = getText();
        if (!getOption(Option.REGULAR_EXPRESSION) || expr == null) {
            return true;
        } else {
            try {
                Pattern p = Pattern.compile(getText());
                return true;
            } catch (PatternSyntaxException p) {
                return false;
            }
        }
    }

    /**
     * Sets proper color of text pattern.
     */
    private void updateTextPatternColor() {
        Color dfltColor = getDefaultTextColor(); // need to be here to init
        textToFindEditor.setForeground(
                valid ? dfltColor : UiUtils.getErrorTextColor());
    }

    private Color getDefaultTextColor() {
        if (defaultTextColor == null) {
            defaultTextColor = component.getForeground();
        }
        return defaultTextColor;
    }

    private static class ModelItem {

        final SearchPattern sp;

        public ModelItem(SearchPattern sp) {
            this.sp = sp;
        }

        @Override
        public String toString() {
            return sp.getSearchExpression();
        }
    }
}