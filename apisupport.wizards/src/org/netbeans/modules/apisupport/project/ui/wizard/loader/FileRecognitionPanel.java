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

package org.netbeans.modules.apisupport.project.ui.wizard.loader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.swing.ButtonGroup;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.apisupport.project.api.UIUtil;
import org.netbeans.modules.apisupport.project.ui.wizard.common.BasicWizardIterator;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 * the first panel in loaders wizard
 *
 * @author Milos Kleint
 */
final class FileRecognitionPanel extends BasicWizardIterator.Panel {
    
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("([.]?[a-zA-Z0-9_]+){1}([ ,]+[.]?[a-zA-Z0-9_]+)*[ ]*"); // NOI18N
    private static final Pattern ELEMENT_PATTERN = Pattern.compile("(application/([a-zA-Z0-9_.-])*\\+xml|text/([a-zA-Z0-9_.-])*\\+xml)"); // NOI18N
    private static final Pattern MIME_TYPE_PATTERN = Pattern.compile("[\\w.]+(?:[+-][\\w.]+)?/[\\w.]+(?:[+-][\\w.]+)*"); // NOI18N
    
    private NewLoaderIterator.DataModel data;
    private ButtonGroup group;
    private boolean listenersAttached;
    private DocumentListener docList;
    
    /**
     * Creates new form FileRecognitionPanel
     */
    public FileRecognitionPanel(WizardDescriptor setting, NewLoaderIterator.DataModel data) {
        super(setting);
        this.data = data;
        initComponents();
        initAccessibility();
        group = new ButtonGroup();
        group.add(rbByElement);
        group.add(rbByExtension);
        ActionListener list = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                txtExtension.setEnabled(rbByExtension.isSelected());
                txtNamespace.setEnabled(rbByElement.isSelected());
                checkValidity();
            }
        };
        docList = new UIUtil.DocumentAdapter() {
            public void insertUpdate(DocumentEvent e) {
                checkValidity();
            }
        };
        
        rbByElement.addActionListener(list);
        rbByExtension.addActionListener(list);
        
        putClientProperty("NewFileWizard_Title", getMessage("LBL_LoaderWizardTitle"));
    }

    static String checkValidity(AtomicBoolean error, String mimeType, String namespace, String extension, boolean byElement) {
        if (mimeType.isEmpty()) {
            return getMessage("MSG_EmptyMIMEType");
        } else if (!MIME_TYPE_PATTERN.matcher(mimeType).matches()) {
            error.set(true);
            return getMessage("MSG_NotValidMimeType");
        } else {
            if (byElement) {
                if (namespace.isEmpty()) {
                    return getMessage("MSG_NoNamespace");
                } else {
                    if (!ELEMENT_PATTERN.matcher(mimeType).matches()) {
                        error.set(true);
                        return getMessage("MSG_BadMimeTypeForXML");
                    }
                }
            } else {
                if (extension.isEmpty()) {
                    return getMessage("MSG_NoExtension");
                } else {
                    if (!EXTENSION_PATTERN.matcher(extension).matches()) {
                        error.set(true);
                        return getMessage("MSG_BadExtensionPattern");
                    }
                }
            }
        }
        return null;
    }
    
    private void checkValidity() {
        AtomicBoolean error = new AtomicBoolean();
        String msg = checkValidity(error, txtMimeType.getText().trim(), txtNamespace.getText().trim(),
                txtExtension.getText().trim(), rbByElement.isSelected());
        if (msg == null) {
            markValid();
        } else if (error.get()) {
            setError(msg);
        } else {
            setInfo(msg, false);
        }
    }
    
    public void addNotify() {
        super.addNotify();
        attachDocumentListeners();
        checkValidity();
    }
    
    public void removeNotify() {
        // prevent checking when the panel is not "active"
        removeDocumentListeners();
        super.removeNotify();
    }
    
    private void attachDocumentListeners() {
        if (!listenersAttached) {
            txtNamespace.getDocument().addDocumentListener(docList);
            txtExtension.getDocument().addDocumentListener(docList);
            txtMimeType.getDocument().addDocumentListener(docList);
            listenersAttached = true;
        }
    }
    
    private void removeDocumentListeners() {
        if (listenersAttached) {
            txtNamespace.getDocument().removeDocumentListener(docList);
            txtExtension.getDocument().removeDocumentListener(docList);
            txtMimeType.getDocument().removeDocumentListener(docList);
            listenersAttached = false;
        }
    }
    
    
    protected void storeToDataModel() {
        data.setMimeType(txtMimeType.getText().trim());
        data.setExtensionBased(rbByExtension.isSelected());
        if (data.isExtensionBased()) {
            data.setExtension(txtExtension.getText().trim());
            data.setNamespace(null);
        } else {
            data.setExtension(null);
            data.setNamespace(txtNamespace.getText().trim());
        }
    }
    
    protected void readFromDataModel() {
        String mime = data.getMimeType();
        if (mime == null) {
            mime = "";
        }
        txtMimeType.setText(mime);
        if (data.isExtensionBased()) {
            rbByExtension.setSelected(true);
        } else {
            rbByElement.setSelected(true);
        }
        txtExtension.setEnabled(rbByExtension.isSelected());
        txtNamespace.setEnabled(rbByElement.isSelected());
        txtExtension.setText(data.getExtension());
        txtNamespace.setText(data.getNamespace());
        
        checkValidity();
    }
    
    protected String getPanelName() {
        return getMessage("LBL_FileRecognition_Title");
    }
    
    protected HelpCtx getHelp() {
        return new HelpCtx(FileRecognitionPanel.class);
    }
    
    private static String getMessage(String key) {
        return NbBundle.getMessage(FileRecognitionPanel.class, key);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lblMimeType = new javax.swing.JLabel();
        txtMimeType = new javax.swing.JTextField();
        rbByExtension = new javax.swing.JRadioButton();
        lblExtension = new javax.swing.JLabel();
        txtExtension = new javax.swing.JTextField();
        rbByElement = new javax.swing.JRadioButton();
        lblNamespace = new javax.swing.JLabel();
        txtNamespace = new javax.swing.JTextField();
        mimeTypeHint = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        lblMimeType.setLabelFor(txtMimeType);
        org.openide.awt.Mnemonics.setLocalizedText(lblMimeType, org.openide.util.NbBundle.getMessage(FileRecognitionPanel.class, "LBL_MimeType")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(lblMimeType, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        add(txtMimeType, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(rbByExtension, org.openide.util.NbBundle.getMessage(FileRecognitionPanel.class, "LBL_ByExtension")); // NOI18N
        rbByExtension.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(rbByExtension, gridBagConstraints);

        lblExtension.setLabelFor(txtExtension);
        org.openide.awt.Mnemonics.setLocalizedText(lblExtension, org.openide.util.NbBundle.getMessage(FileRecognitionPanel.class, "LBL_Extension")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(lblExtension, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 0);
        add(txtExtension, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(rbByElement, org.openide.util.NbBundle.getMessage(FileRecognitionPanel.class, "LBL_ByElement")); // NOI18N
        rbByElement.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(rbByElement, gridBagConstraints);

        lblNamespace.setLabelFor(txtNamespace);
        org.openide.awt.Mnemonics.setLocalizedText(lblNamespace, org.openide.util.NbBundle.getMessage(FileRecognitionPanel.class, "LBL_Element")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(lblNamespace, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 0);
        add(txtNamespace, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(mimeTypeHint, "(e.g. \"text/x-myformat\" or \"text/myformat+xml\" for XML)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 0);
        add(mimeTypeHint, gridBagConstraints);
        mimeTypeHint.getAccessibleContext().setAccessibleName("MIME Type Hint");
        mimeTypeHint.getAccessibleContext().setAccessibleDescription("MIME Type Hint");
    }// </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblExtension;
    private javax.swing.JLabel lblMimeType;
    private javax.swing.JLabel lblNamespace;
    private javax.swing.JLabel mimeTypeHint;
    private javax.swing.JRadioButton rbByElement;
    private javax.swing.JRadioButton rbByExtension;
    private javax.swing.JTextField txtExtension;
    private javax.swing.JTextField txtMimeType;
    private javax.swing.JTextField txtNamespace;
    // End of variables declaration//GEN-END:variables
    
    private void initAccessibility() {
        this.getAccessibleContext().setAccessibleDescription(getMessage("ACS_FileRecognitionPanel"));
        rbByElement.getAccessibleContext().setAccessibleDescription(getMessage("ACS_CTL_ByElement"));
        rbByExtension.getAccessibleContext().setAccessibleDescription(getMessage("ACS_CTL_ByExtension"));
        txtExtension.getAccessibleContext().setAccessibleDescription(getMessage("ACS_CTL_Extension"));
        txtNamespace.getAccessibleContext().setAccessibleDescription(getMessage("ACS_CTL_Namespace"));
        txtMimeType.getAccessibleContext().setAccessibleDescription(getMessage("ACS_CTL_Mimetype"));
    }
}
