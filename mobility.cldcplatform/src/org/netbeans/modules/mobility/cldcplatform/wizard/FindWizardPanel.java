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

package org.netbeans.modules.mobility.cldcplatform.wizard;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Adam Sotona
 */
public class FindWizardPanel implements WizardDescriptor.FinishablePanel {
    
    ArrayList<ChangeListener> listeners = new ArrayList<ChangeListener>();
    FindPanel component = new FindPanel(this);
    
    public boolean isFinishPanel() {
        return false;
    }
    
    public Component getComponent() {
        return component;
    }
    
    public HelpCtx getHelp() {
        return new HelpCtx(DetectWizardPanel.class);
    }
    
    public void readSettings(final Object object) {
        component.readSettings((WizardDescriptor) object);
    }
    
    public void storeSettings(final Object object) {
        component.storeSettings((WizardDescriptor) object);
    }
    
    public boolean isValid() {
        return component.isStateValid();
    }
    
    public void addChangeListener(final ChangeListener changeListener) {
        listeners.add(changeListener);
    }
    
    public void removeChangeListener(final ChangeListener changeListener) {
        listeners.remove(changeListener);
    }
    
    public void fireChanged() {
        final ChangeEvent e = new ChangeEvent(this);
        final Object[] la = listeners.toArray();
        for (int i = 0; i < la.length; i++) {
            final ChangeListener l = (ChangeListener) la[i];
            l.stateChanged(e);
        }
    }
    
}