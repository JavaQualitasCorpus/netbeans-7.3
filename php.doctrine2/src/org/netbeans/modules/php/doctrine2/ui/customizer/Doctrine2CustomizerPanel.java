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
package org.netbeans.modules.php.doctrine2.ui.customizer;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LayoutStyle.ComponentPlacement;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 * Customizer panel for Doctrine2.
 */
public class Doctrine2CustomizerPanel extends JPanel {

    private static final long serialVersionUID = -34644365465463L;

    public Doctrine2CustomizerPanel() {
        initComponents();
    }

    public boolean isSupportEnabled() {
        return enabledCheckBox.isSelected();
    }

    public void setSupportEnabled(boolean enabled) {
        enabledCheckBox.setSelected(enabled);
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
     * Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        enabledCheckBox = new JCheckBox();
        enabledInfoLabel = new JLabel();
        Mnemonics.setLocalizedText(enabledCheckBox, NbBundle.getMessage(Doctrine2CustomizerPanel.class, "Doctrine2CustomizerPanel.enabledCheckBox.text")); // NOI18N
        Mnemonics.setLocalizedText(enabledInfoLabel, NbBundle.getMessage(Doctrine2CustomizerPanel.class, "Doctrine2CustomizerPanel.enabledInfoLabel.text")); // NOI18N

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addComponent(enabledCheckBox)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(enabledInfoLabel))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(enabledCheckBox)
                .addPreferredGap(ComponentPlacement.RELATED)
                .addComponent(enabledInfoLabel))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JCheckBox enabledCheckBox;
    private JLabel enabledInfoLabel;
    // End of variables declaration//GEN-END:variables

}