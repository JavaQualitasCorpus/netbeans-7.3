/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.git.ui.repository.remote;

/**
 *
 * @author ondra
 */
class SSHPanel extends javax.swing.JPanel {

    /**
     * Creates new form UserPasswordPanel
     */
    public SSHPanel () {
        initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();

        lblPassword.setLabelFor(userPasswordField);
        org.openide.awt.Mnemonics.setLocalizedText(lblPassword, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblPassword.text")); // NOI18N
        lblPassword.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblPassword.toolTipText")); // NOI18N

        lblUser.setLabelFor(userTextField);
        org.openide.awt.Mnemonics.setLocalizedText(lblUser, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblUser.text")); // NOI18N
        lblUser.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblUser.toolTipText")); // NOI18N

        userTextField.setColumns(8);
        userTextField.setMinimumSize(new java.awt.Dimension(11, 22));

        userPasswordField.setColumns(8);
        userPasswordField.setMinimumSize(new java.awt.Dimension(11, 22));

        savePasswordCheckBox.setMnemonic('v');
        org.openide.awt.Mnemonics.setLocalizedText(savePasswordCheckBox, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.savePasswordCheckBox.text")); // NOI18N
        savePasswordCheckBox.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.savePasswordCheckBox.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(lblLeaveBlank, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblLeaveBlank.text")); // NOI18N

        buttonGroup1.add(rbUsernamePassword);
        org.openide.awt.Mnemonics.setLocalizedText(rbUsernamePassword, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.rbUsernamePassword.text")); // NOI18N
        rbUsernamePassword.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.rbUsernamePassword.TTtext")); // NOI18N

        buttonGroup1.add(rbPrivateKey);
        org.openide.awt.Mnemonics.setLocalizedText(rbPrivateKey, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.rbPrivateKey.text")); // NOI18N

        lblPassphrase.setLabelFor(txtPassphrase);
        org.openide.awt.Mnemonics.setLocalizedText(lblPassphrase, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblPassphrase.text")); // NOI18N
        lblPassphrase.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblPassphrase.toolTipText")); // NOI18N

        txtIdentityFile.setColumns(8);
        txtIdentityFile.setMinimumSize(new java.awt.Dimension(11, 22));

        txtPassphrase.setColumns(8);
        txtPassphrase.setMinimumSize(new java.awt.Dimension(11, 22));

        savePassphrase.setMnemonic('v');
        org.openide.awt.Mnemonics.setLocalizedText(savePassphrase, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.savePassphrase.text")); // NOI18N
        savePassphrase.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.savePassphrase.toolTipText")); // NOI18N

        lblIdentityFile.setLabelFor(txtIdentityFile);
        org.openide.awt.Mnemonics.setLocalizedText(lblIdentityFile, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblIdentityFile.text")); // NOI18N
        lblIdentityFile.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.lblIdentityFile.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(btnBrowse, org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.btnBrowse.text")); // NOI18N
        btnBrowse.setToolTipText(org.openide.util.NbBundle.getMessage(SSHPanel.class, "SSHPanel.btnBrowse.TTtext")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rbUsernamePassword)
                    .addComponent(rbPrivateKey)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lblPassphrase)
                                    .addComponent(lblIdentityFile))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(58, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(lblUser)
                                    .addComponent(lblPassword))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(userTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblLeaveBlank))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(userPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(8, 8, 8)
                                .addComponent(savePasswordCheckBox))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(txtIdentityFile, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnBrowse))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(txtPassphrase, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(savePassphrase)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(userTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblLeaveBlank, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUser))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbUsernamePassword)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(savePasswordCheckBox)
                    .addComponent(lblPassword)
                    .addComponent(userPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(rbPrivateKey)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblIdentityFile)
                    .addComponent(txtIdentityFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowse))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPassphrase, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(savePassphrase)
                    .addComponent(lblPassphrase))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    final javax.swing.JButton btnBrowse = new javax.swing.JButton();
    private javax.swing.ButtonGroup buttonGroup1;
    final javax.swing.JLabel lblIdentityFile = new javax.swing.JLabel();
    final javax.swing.JLabel lblLeaveBlank = new javax.swing.JLabel();
    final javax.swing.JLabel lblPassphrase = new javax.swing.JLabel();
    final javax.swing.JLabel lblPassword = new javax.swing.JLabel();
    final javax.swing.JLabel lblUser = new javax.swing.JLabel();
    final javax.swing.JRadioButton rbPrivateKey = new javax.swing.JRadioButton();
    final javax.swing.JRadioButton rbUsernamePassword = new javax.swing.JRadioButton();
    final javax.swing.JCheckBox savePassphrase = new javax.swing.JCheckBox();
    final javax.swing.JCheckBox savePasswordCheckBox = new javax.swing.JCheckBox();
    final javax.swing.JTextField txtIdentityFile = new javax.swing.JTextField();
    final javax.swing.JPasswordField txtPassphrase = new javax.swing.JPasswordField();
    final javax.swing.JPasswordField userPasswordField = new javax.swing.JPasswordField();
    final javax.swing.JTextField userTextField = new javax.swing.JTextField();
    // End of variables declaration//GEN-END:variables
}