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

package org.netbeans.modules.xml.multiview;

import java.awt.Toolkit;
import org.openide.text.NbDocument;
import org.openide.actions.SaveAction;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.ProxyLookup;
import javax.swing.*;
import javax.swing.text.Document;
import java.awt.event.KeyEvent;

/**
 * XmlMultiviewElement.java
 *
 * Created on October 5, 2004, 1:35 PM
 * @author  mkuchtiak
 */
public class XmlMultiViewElement extends AbstractMultiViewElement implements java.io.Serializable {
    static final long serialVersionUID = -326467724916080580L;

    private transient XmlMultiViewEditorSupport.XmlCloneableEditor xmlEditor;
    private transient javax.swing.JComponent toolbar;

    /** Creates a new instance of XmlMultiviewElement */
    public XmlMultiViewElement() {
    }

    /** Creates a new instance of XmlMultiviewElement */
    public XmlMultiViewElement(XmlMultiViewDataObject dObj) {
        super(dObj);
    }

    @Override
    public void componentDeactivated() {
    }

    @Override
    public void componentHidden() {
    }
    
    @Override
    public void componentActivated() {
        getXmlEditor().componentActivated();
    }
    
    @Override
    public void componentShowing() {
        getXmlEditor().componentShowing();
    }
    
    @Override
    public void componentOpened() {
        getXmlEditor().componentOpened();
    }
    
    @Override
    public void componentClosed() {
        getXmlEditor().componentClosed();
    }
    
    @Override
    public org.openide.util.Lookup getLookup() {
        return new ProxyLookup(new org.openide.util.Lookup[] {
            dObj.getNodeDelegate().getLookup()
        });
    }

    @Override
    public javax.swing.JComponent getToolbarRepresentation() {
        if (toolbar == null) {
            final JEditorPane editorPane = getXmlEditor().getEditorPane();
            if (editorPane!= null) {
                final Document doc = editorPane.getDocument();
                if (doc instanceof NbDocument.CustomToolbar) {
                    toolbar = ((NbDocument.CustomToolbar) doc).createToolbar(editorPane);
                }
            }
            if (toolbar == null) {
                // attempt to create own toolbar??
                toolbar = new javax.swing.JPanel();
            }
        }
        return toolbar;
    }

    @Override
    public javax.swing.JComponent getVisualRepresentation() {
        return getXmlEditor();
    }

    private XmlMultiViewEditorSupport.XmlCloneableEditor getXmlEditor() {
        if (xmlEditor == null) {
            xmlEditor = (XmlMultiViewEditorSupport.XmlCloneableEditor) dObj.getEditorSupport().createCloneableEditor();
            final ActionMap map = xmlEditor.getActionMap();
            SaveAction act = (SaveAction) SystemAction.get(SaveAction.class);
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
            xmlEditor.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(stroke, "save"); //NOI18N
            map.put("save", act); //NOI18N
        }
        return xmlEditor;
    }

}