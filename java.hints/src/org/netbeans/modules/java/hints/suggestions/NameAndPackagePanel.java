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
package org.netbeans.modules.java.hints.suggestions;

import java.util.StringTokenizer;

import org.openide.util.Utilities;

/**
 *
 * @author balek
 */
public class NameAndPackagePanel extends javax.swing.JPanel {

    static final String IS_VALID = "NameAndPackagePanel.isValidData"; //NOI18N

    /**
     * Creates new form NameAndPackagePanel
     */
    public NameAndPackagePanel(String className, String packageName) {
        initComponents();
        classNameTextField.setText(className);
        packageNameTextField.setText(packageName);
    }

    public String getClassName() {
        return classNameTextField.getText();
    }

    public String getPackageName() {
        return packageNameTextField.getText();
    }

    public boolean isValidData() {
        return isValidTypeIdentifier(getClassName()) && isValidPackageName(getPackageName());
    }
    
    private boolean isValidPackageName(String str) {
        if (str.length() > 0 && str.charAt(0) == '.') {
            return false;
        }
        StringTokenizer tukac = new StringTokenizer(str, ".");
        while (tukac.hasMoreTokens()) {
            String token = tukac.nextToken();
            if ("".equals(token)) {
                return false;
            }
            if (!Utilities.isJavaIdentifier(token)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidTypeIdentifier(String ident) {
        if (ident == null || "".equals(ident) || !Utilities.isJavaIdentifier(ident)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        classNameLabel = new javax.swing.JLabel();
        classNameTextField = new javax.swing.JTextField();
        packageNameLabel = new javax.swing.JLabel();
        packageNameTextField = new javax.swing.JTextField();

        classNameLabel.setLabelFor(classNameTextField);
        classNameLabel.setText(org.openide.util.NbBundle.getMessage(NameAndPackagePanel.class, "LBL_ClassName")); // NOI18N

        classNameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                classNameTextFieldKeyTyped(evt);
            }
        });

        packageNameLabel.setLabelFor(packageNameTextField);
        packageNameLabel.setText(org.openide.util.NbBundle.getMessage(NameAndPackagePanel.class, "LBL_PackageName")); // NOI18N

        packageNameTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                packageNameTextFieldKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(packageNameLabel)
                    .addComponent(classNameLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(classNameTextField)
                    .addComponent(packageNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(classNameLabel)
                    .addComponent(classNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(packageNameLabel)
                    .addComponent(packageNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void classNameTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_classNameTextFieldKeyTyped
        firePropertyChange(IS_VALID, null, null);
    }//GEN-LAST:event_classNameTextFieldKeyTyped

    private void packageNameTextFieldKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_packageNameTextFieldKeyTyped
        firePropertyChange(IS_VALID, null, null);
    }//GEN-LAST:event_packageNameTextFieldKeyTyped

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel classNameLabel;
    private javax.swing.JTextField classNameTextField;
    private javax.swing.JLabel packageNameLabel;
    private javax.swing.JTextField packageNameTextField;
    // End of variables declaration//GEN-END:variables
}