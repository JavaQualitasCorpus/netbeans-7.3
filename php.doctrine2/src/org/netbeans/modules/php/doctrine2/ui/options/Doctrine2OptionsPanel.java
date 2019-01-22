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
package org.netbeans.modules.php.doctrine2.ui.options;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.php.api.util.FileUtils;
import org.netbeans.modules.php.api.util.UiUtils;
import org.netbeans.modules.php.doctrine2.commands.Doctrine2Script;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * Panel for Doctrine2 options.
 */
@OptionsPanelController.Keywords(keywords={"php", "doctrine", "doctrine2", "orm", "odm"}, location=UiUtils.OPTIONS_PATH, tabTitle= "#LBL_PHPDoctrineOptionsName")
public class Doctrine2OptionsPanel extends JPanel {

    private static final long serialVersionUID = 67643468774654L;
    private static final String SCRIPT_LAST_FOLDER_SUFFIX = ".script"; // NOI18N

    private final ChangeSupport changeSupport = new ChangeSupport(this);


    public Doctrine2OptionsPanel() {
        initComponents();
        errorLabel.setText(" "); // NOI18N

        init();
    }

    @NbBundle.Messages({
        "# {0} - short script name",
        "# {1} - long script name",
        "Doctrine2OptionsPanel.script.hint=Full path of Doctrine2 script (typically {0} or {1})."
    })
    private void init() {
        scriptInfoLabel.setText(Bundle.Doctrine2OptionsPanel_script_hint(Doctrine2Script.SCRIPT_NAME, Doctrine2Script.SCRIPT_NAME_LONG));
        errorLabel.setText(" "); // NOI18N


        initListeners();
    }

    private void initListeners() {
        scriptTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                processUpdate();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                processUpdate();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                processUpdate();
            }
            private void processUpdate() {
                fireChange();
            }
        });
    }

    public String getScript() {
        return scriptTextField.getText();
    }

    public void setScript(String script) {
        scriptTextField.setText(script);
    }

    public void setError(String message) {
        errorLabel.setText(" "); // NOI18N
        errorLabel.setForeground(UIManager.getColor("nb.errorForeground")); // NOI18N
        errorLabel.setText(message);
    }

    public void setWarning(String message) {
        errorLabel.setText(" "); // NOI18N
        errorLabel.setForeground(UIManager.getColor("nb.warningForeground")); // NOI18N
        errorLabel.setText(message);
    }

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scriptLabel = new JLabel();
        scriptTextField = new JTextField();
        scriptBrowseButton = new JButton();
        scriptSearchButton = new JButton();
        scriptInfoLabel = new JLabel();
        noteLabel = new JLabel();
        errorLabel = new JLabel();
        installationInstructionsLabel = new JLabel();
        learnMoreLabel = new JLabel();

        scriptLabel.setLabelFor(scriptTextField);
        Mnemonics.setLocalizedText(scriptLabel, NbBundle.getMessage(Doctrine2OptionsPanel.class, "Doctrine2OptionsPanel.scriptLabel.text"));
        Mnemonics.setLocalizedText(scriptBrowseButton, NbBundle.getMessage(Doctrine2OptionsPanel.class, "Doctrine2OptionsPanel.scriptBrowseButton.text"));
        scriptBrowseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                scriptBrowseButtonActionPerformed(evt);
            }
        });
        Mnemonics.setLocalizedText(scriptSearchButton, NbBundle.getMessage(Doctrine2OptionsPanel.class, "Doctrine2OptionsPanel.scriptSearchButton.text"));
        scriptSearchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                scriptSearchButtonActionPerformed(evt);
            }
        });
        Mnemonics.setLocalizedText(scriptInfoLabel, "HINT");
        Mnemonics.setLocalizedText(noteLabel, NbBundle.getMessage(Doctrine2OptionsPanel.class, "Doctrine2OptionsPanel.noteLabel.text"));
        Mnemonics.setLocalizedText(errorLabel, "ERROR");
        Mnemonics.setLocalizedText(installationInstructionsLabel, NbBundle.getMessage(Doctrine2OptionsPanel.class, "Doctrine2OptionsPanel.installationInstructionsLabel.text"));
        Mnemonics.setLocalizedText(learnMoreLabel, NbBundle.getMessage(Doctrine2OptionsPanel.class, "Doctrine2OptionsPanel.learnMoreLabel.text"));
        learnMoreLabel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                learnMoreLabelMouseEntered(evt);
            }
            public void mousePressed(MouseEvent evt) {
                learnMoreLabelMousePressed(evt);
            }
        });

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup()
                .addComponent(scriptLabel)

                .addPreferredGap(ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup()
                        .addComponent(scriptInfoLabel)

                        .addGap(0, 0, Short.MAX_VALUE)).addGroup(layout.createSequentialGroup()
                        .addComponent(scriptTextField)

                        .addPreferredGap(ComponentPlacement.RELATED).addComponent(scriptBrowseButton).addPreferredGap(ComponentPlacement.RELATED).addComponent(scriptSearchButton)))).addGroup(layout.createSequentialGroup()

                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(errorLabel).addComponent(noteLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addGap(0, 0, Short.MAX_VALUE)).addGroup(layout.createSequentialGroup()
                .addContainerGap()

                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(installationInstructionsLabel).addComponent(learnMoreLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING).addGroup(layout.createSequentialGroup()

                .addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(scriptLabel).addComponent(scriptTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addComponent(scriptBrowseButton).addComponent(scriptSearchButton)).addPreferredGap(ComponentPlacement.RELATED).addComponent(scriptInfoLabel).addGap(18, 18, 18).addComponent(noteLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED).addComponent(installationInstructionsLabel).addPreferredGap(ComponentPlacement.RELATED).addComponent(learnMoreLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(errorLabel))
        );
    }// </editor-fold>//GEN-END:initComponents

    @NbBundle.Messages("LBL_SelectScript=Select Doctrine2 script")
    private void scriptBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_scriptBrowseButtonActionPerformed
        File script = new FileChooserBuilder(Doctrine2OptionsPanel.class.getName() + SCRIPT_LAST_FOLDER_SUFFIX)
                .setTitle(Bundle.LBL_SelectScript())
                .setFilesOnly(true)
                .showOpenDialog();
        if (script != null) {
            script = FileUtil.normalizeFile(script);
            scriptTextField.setText(script.getAbsolutePath());
        }
    }//GEN-LAST:event_scriptBrowseButtonActionPerformed

    private void learnMoreLabelMouseEntered(MouseEvent evt) {//GEN-FIRST:event_learnMoreLabelMouseEntered
        evt.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }//GEN-LAST:event_learnMoreLabelMouseEntered

    private void learnMoreLabelMousePressed(MouseEvent evt) {//GEN-FIRST:event_learnMoreLabelMousePressed
        try {
            HtmlBrowser.URLDisplayer.getDefault().showURL(new URL("http://www.doctrine-project.org/")); // NOI18N
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_learnMoreLabelMousePressed

    @NbBundle.Messages({
        "Doctrine2OptionsPanel.search.scripts.title=Doctrine2 scripts",
        "Doctrine2OptionsPanel.search.scripts=&Doctrine2 scripts:",
        "Doctrine2OptionsPanel.search.scripts.pleaseWaitPart=Doctrine2 scripts",
        "Doctrine2OptionsPanel.search.scripts.notFound=No Doctrine2 scripts found."
    })
    private void scriptSearchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_scriptSearchButtonActionPerformed
        String script = UiUtils.SearchWindow.search(new UiUtils.SearchWindow.SearchWindowSupport() {
            @Override
            public List<String> detect() {
                return FileUtils.findFileOnUsersPath(Doctrine2Script.SCRIPT_NAME, Doctrine2Script.SCRIPT_NAME_LONG);
            }
            @Override
            public String getWindowTitle() {
                return Bundle.Doctrine2OptionsPanel_search_scripts_title();
            }
            @Override
            public String getListTitle() {
                return Bundle.Doctrine2OptionsPanel_search_scripts();
            }
            @Override
            public String getPleaseWaitPart() {
                return Bundle.Doctrine2OptionsPanel_search_scripts_pleaseWaitPart();
            }
            @Override
            public String getNoItemsFound() {
                return Bundle.Doctrine2OptionsPanel_search_scripts_notFound();
            }
        });
        if (script != null) {
            scriptTextField.setText(script);
        }
    }//GEN-LAST:event_scriptSearchButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel errorLabel;
    private JLabel installationInstructionsLabel;
    private JLabel learnMoreLabel;
    private JLabel noteLabel;
    private JButton scriptBrowseButton;
    private JLabel scriptInfoLabel;
    private JLabel scriptLabel;
    private JButton scriptSearchButton;
    private JTextField scriptTextField;
    // End of variables declaration//GEN-END:variables
}