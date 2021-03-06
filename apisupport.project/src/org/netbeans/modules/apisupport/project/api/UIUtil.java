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

package org.netbeans.modules.apisupport.project.api;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Locale;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import org.netbeans.api.templates.TemplateRegistration;
import static org.netbeans.modules.apisupport.project.api.Bundle.*;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.RecommendedTemplates;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;

/**
 * @author Martin Krauskopf
 */
public final class UIUtil {
    
    private UIUtil() {}

    /**
     * Category to use for NBM-related templates.
     * @see TemplateRegistration#category
     * @see RecommendedTemplates
     */
    public static final String TEMPLATE_CATEGORY = "nbm-specific";

    /**
     * Folder to use for NBM-related templates.
     * @see TemplateRegistration#folder
     * @see PrivilegedTemplates
     */
    public static final String TEMPLATE_FOLDER = "NetBeansModuleDevelopment";

    /**
     * ID for a template to create a new action.
     * @see TemplateRegistration#id
     * @see PrivilegedTemplates
     */
    public static final String TEMPLATE_ACTION_ID = "newAction";

    /**
     * ID for a template to create a new action.
     * @see TemplateRegistration#id
     * @see PrivilegedTemplates
     */
    public static final String TEMPLATE_WINDOW_ID = "newWindow";

    /**
     * Convenient class for listening on document changes. Use it if you do not
     * care what exact change really happened. {@link #removeUpdate} and {@link
     * #changedUpdate} just delegate to {@link #insertUpdate}. So everything
     * what is needed in order to be notified about document changes is to
     * override {@link #insertUpdate} method.
     */
    public abstract static class DocumentAdapter implements DocumentListener {
        public void removeUpdate(DocumentEvent e) { insertUpdate(null); }
        public void changedUpdate(DocumentEvent e) { insertUpdate(null); }
    }
    
    private static Reference<JFileChooser> iconChooser;
    
    /**
     * Returns an instance of {@link javax.swing.JFileChooser} permitting
     * selection only a regular <em>icon</em>.
     */
    public static JFileChooser getIconFileChooser() {        
        if (iconChooser != null) {
            JFileChooser choose = iconChooser.get();
            if (choose != null) {
                return choose;
            }
        }
        final JFileChooser chooser = new IconFileChooser();        
        iconChooser = new WeakReference<JFileChooser>(chooser);
        return chooser;
    }

        
    private static final class IconFilter extends FileFilter {
        public boolean accept(File pathname) {
            return pathname.isDirectory() ||
                    pathname.getName().toLowerCase(Locale.ENGLISH).endsWith("gif") || // NOI18N
                    pathname.getName().toLowerCase(Locale.ENGLISH).endsWith("png"); // NOI18N
        }
        public String getDescription() {
            return "*.gif, *.png"; // NOI18N
        }
    }
    
    private static class IconFileChooser extends JFileChooser {
        private final JTextField iconInfo = new JTextField();        
        @Messages("TITLE_IconDialog=Select Icon")
        private  IconFileChooser() {
            JPanel accessoryPanel = getAccesoryPanel(iconInfo);
            setDialogTitle(TITLE_IconDialog());//NOI18N
            setAccessory(accessoryPanel);
            setAcceptAllFileFilterUsed(false);
            setFileSelectionMode(JFileChooser.FILES_ONLY);
            setMultiSelectionEnabled(false);
            addChoosableFileFilter(new IconFilter());
            setFileView(new FileView() {
                public @Override Icon getIcon(File f) {
                    // Show icons right in the chooser, to make it easier to find
                    // the right one.
                    if (f.getName().endsWith(".gif") || f.getName().endsWith(".png")) { // NOI18N
                        Icon icon = new ImageIcon(f.getAbsolutePath());
                        if (icon.getIconWidth() == 16 && icon.getIconHeight() == 16) {
                            return icon;
                        }
                    }
                    return null;
                }
                public @Override String getName(File f) {
                    File f2 = getSelectedFile();
                    if (f2 != null && (f2.getName().endsWith(".gif") || f2.getName().endsWith(".png"))) { // NOI18N
                        Icon icon = new ImageIcon(f2.getAbsolutePath());
                        StringBuffer sb = new StringBuffer();
                        sb.append(f2.getName()).append(" [");//NOI18N
                        sb.append(icon.getIconWidth()).append('x').append(icon.getIconHeight());
                        sb.append(']');
                        setApproveButtonToolTipText(sb.toString());
                        iconInfo.setText(sb.toString());
                    } else {
                        iconInfo.setText("");
                    }
                    return super.getName(f);
                }
                
            });            
        }
        
        @Messages("LBL_IconInfo=Selected icon [size]:")
        private static JPanel getAccesoryPanel(final JTextField iconInfo) {
            iconInfo.setColumns(15);
            iconInfo.setEditable(false);
            
            JPanel accessoryPanel = new JPanel();
            JPanel inner = new JPanel();
            JLabel iconInfoLabel = new JLabel();
            accessoryPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
            
            inner.setLayout(new GridLayout(2, 1, 0, 6));
            
            iconInfoLabel.setLabelFor(iconInfo);
            Mnemonics.setLocalizedText(iconInfoLabel, LBL_IconInfo());
            inner.add(iconInfoLabel);
            
            inner.add(iconInfo);
            
            accessoryPanel.add(inner);
            return accessoryPanel;
        }
    }

    /** Generally usable in conjuction with {@link #createComboWaitModel}. */
    public static final String WAIT_VALUE =
            please_wait();

    /**
     * Use this model in situation when you need to populate combo in the
     * background. The only item in this model is {@link #WAIT_VALUE}.
     */
    @Messages("please_wait=Please wait...")
    public static ComboBoxModel createComboWaitModel() {
        return new DefaultComboBoxModel(new Object[] { WAIT_VALUE });
    }

    public interface WaitingModel extends ListModel {
        boolean isWaiting();
    }

    /**
     * Conveninent method which delegates to {@link #hasOnlyValue} passing a
     * given model and {@link #WAIT_VALUE} as a value.
     * Also checks {@link WaitingModel}.
     */
    public static boolean isWaitModel(final ListModel model) {
        if (model instanceof WaitingModel) {
            return ((WaitingModel) model).isWaiting();
        }
        return hasOnlyValue(model, WAIT_VALUE);
    }

    /**
     * Returns true if the given model is not <code>null</code> and contains
     * only the given value.
     */
    public static boolean hasOnlyValue(final ListModel model, final Object value) {
        return model != null && model.getSize() == 1 && model.getElementAt(0) == value;
    }

    /**
     * Use this model in situation when you need to populate list in the
     * background. The only item in this model is {@link #WAIT_VALUE}.
     *
     * @see #isWaitModel
     */
    public static ListModel createListWaitModel() {
        DefaultListModel listWaitModel = new DefaultListModel();
        listWaitModel.addElement(WAIT_VALUE);
        return listWaitModel;
    }

    public static final /*@StaticResource*/ String LIBRARIES_ICON = "org/netbeans/modules/apisupport/project/api/libraries.gif";

}
