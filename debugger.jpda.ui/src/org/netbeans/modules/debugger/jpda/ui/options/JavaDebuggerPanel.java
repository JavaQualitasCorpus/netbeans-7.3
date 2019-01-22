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

package org.netbeans.modules.debugger.jpda.ui.options;

import java.util.Collection;
import org.netbeans.modules.options.java.api.JavaOptions;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

@OptionsPanelController.Keywords(keywords={"java debugger", "#KW_JavaDebugger"}, location=JavaOptions.JAVA, tabTitle= "#LBL_JavaDebugger")
final class JavaDebuggerPanel extends StorablePanel {

    private static final String SHOW_FORMATTERS_PROP_NAME = "org.netbeans.modules.debugger.jpda.ui.options.SHOW_FORMATTERS";
    private static final int FORMATTERS_INDEX = 2;

    private final JavaDebuggerOptionsPanelController controller;

    private StorablePanel[] categoryPanels;

    JavaDebuggerPanel(JavaDebuggerOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        Collection<? extends Provider> panelProviders = Lookups.forPath("debugger/jpda/options").lookupAll(StorablePanel.Provider.class);
        categoryPanels = new StorablePanel[3 + panelProviders.size()];
        final String[] panelNames = new String[3 + panelProviders.size()];
        int i = 0;
        categoryPanels[i] = new CategoryPanelGeneral();
        panelNames[i++] = NbBundle.getMessage(JavaDebuggerPanel.class, "JavaDebuggerPanel.categoryRadioButtonGeneral.text");
        categoryPanels[i] = new CategoryPanelStepFilters();
        panelNames[i++] = NbBundle.getMessage(JavaDebuggerPanel.class, "JavaDebuggerPanel.categoryRadioButtonStepFilters.text");
        categoryPanels[i] = new CategoryPanelFormatters();
        panelNames[i++] = NbBundle.getMessage(JavaDebuggerPanel.class, "JavaDebuggerPanel.categoryRadioButtonFormatters.text");
        for (Provider p : panelProviders) {
            categoryPanels[i] = p.getPanel();
            panelNames[i++] = p.getPanelName();
        }
        categoriesList.setModel(new javax.swing.AbstractListModel() {
            public int getSize() { return panelNames.length; }
            public Object getElementAt(int i) { return panelNames[i]; }
        });
        String value = System.getProperty(SHOW_FORMATTERS_PROP_NAME);
        int index = value != null && "true".equals(value) ? FORMATTERS_INDEX : 0; // NOI18N
        selectCategory(index);
        categoriesList.setSelectedIndex(index);
        if (index == FORMATTERS_INDEX) {
            System.setProperty(SHOW_FORMATTERS_PROP_NAME, "false"); // NOI18N
        }
        // TODO listen to changes in form fields and call controller.changed()
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        categoriesButtonGroup = new javax.swing.ButtonGroup();
        categoryPanel = new javax.swing.JPanel();
        categoriesPanel = new javax.swing.JPanel();
        categoriesList = new javax.swing.JList();
        categoriesLabel = new javax.swing.JLabel();

        categoryPanel.setLayout(new javax.swing.BoxLayout(categoryPanel, javax.swing.BoxLayout.LINE_AXIS));

        categoriesPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        categoriesList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "General", "Step Filters", "Variable Formatters" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        categoriesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        categoriesList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                categoriesListValueChanged(evt);
            }
        });

        javax.swing.GroupLayout categoriesPanelLayout = new javax.swing.GroupLayout(categoriesPanel);
        categoriesPanel.setLayout(categoriesPanelLayout);
        categoriesPanelLayout.setHorizontalGroup(
            categoriesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(categoriesList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        categoriesPanelLayout.setVerticalGroup(
            categoriesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(categoriesList, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
        );

        categoriesLabel.setLabelFor(categoriesList);
        org.openide.awt.Mnemonics.setLocalizedText(categoriesLabel, org.openide.util.NbBundle.getMessage(JavaDebuggerPanel.class, "JavaDebuggerPanel.categoriesLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(categoriesLabel)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(categoriesPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(categoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(categoriesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(categoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                    .addComponent(categoriesPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        categoriesLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(JavaDebuggerPanel.class, "Categories_description")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void categoriesListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_categoriesListValueChanged
        int c = categoriesList.getSelectedIndex();
        if (c >= 0 && c <= 3) {
            selectCategory(c);
        }
}//GEN-LAST:event_categoriesListValueChanged

    private void selectCategory(int c) {
        if (categoryPanel.getComponentCount() > 0) {
            categoryPanel.removeAll();
        }
        categoryPanel.add(categoryPanels[c]);
        categoryPanel.revalidate();
        categoryPanel.repaint();
    }

    public void load() {
        for (StorablePanel p : categoryPanels) {
            p.load();
        }
        String value = System.getProperty(SHOW_FORMATTERS_PROP_NAME);
        if (value != null && "true".equals(value)) { //NOI18N
            selectCategory(FORMATTERS_INDEX);
            categoriesList.setSelectedIndex(FORMATTERS_INDEX);
            System.setProperty(SHOW_FORMATTERS_PROP_NAME, "false"); // NOI18N
        }
        // TODO read settings and initialize GUI
        // Example:        
        // someCheckBox.setSelected(Preferences.userNodeForPackage(JavaDebuggerPanel.class).getBoolean("someFlag", false));
        // or for org.openide.util with API spec. version >= 7.4:
        // someCheckBox.setSelected(NbPreferences.forModule(JavaDebuggerPanel.class).getBoolean("someFlag", false));
        // or:
        // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());
    }

    public void store() {
        for (StorablePanel p : categoryPanels) {
            p.store();
        }
        // TODO store modified settings
        // Example:
        // Preferences.userNodeForPackage(JavaDebuggerPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(JavaDebuggerPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup categoriesButtonGroup;
    private javax.swing.JLabel categoriesLabel;
    private javax.swing.JList categoriesList;
    private javax.swing.JPanel categoriesPanel;
    private javax.swing.JPanel categoryPanel;
    // End of variables declaration//GEN-END:variables

}