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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.actions;

import org.netbeans.modules.profiler.api.EditorSupport;
import org.netbeans.modules.profiler.api.java.SourceClassInfo;
import org.netbeans.modules.profiler.api.java.JavaProfilerSource;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Action enabled on Java sources allowing to select root method(s) for Profiling of Part of Application.
 * @author Jaroslav Bachorik <jaroslav.bachorik@sun.com>
 */
@NbBundle.Messages({
    "LBL_SelectRootMethodsAction1=Select Profiling Root Methods..."
})
@ActionID(id = "org.netbeans.modules.profiler.actions.SelectJavaRootMethodsAction", category = "Profile")
@ActionRegistration(displayName = "#LBL_SelectRootMethodsAction1")
@ActionReference(path = "Editors/text/x-java/Popup/Profiling", name = "org-netbeans-modules-profiler-actions-SelectRootMethodsAction", position = 200)
final public class SelectJavaRootMethodsAction extends BaseSelectRootMethodsAction {

    @Override
    protected String getFileClassName(JavaProfilerSource source) {
        if (source == null)  return null;
        
        String className = null;
        // Read current offset in editor
        int currentOffsetInEditor = EditorSupport.getCurrentOffset();

        if (currentOffsetInEditor == -1) {
            return null;
        }

        // Try to get class at cursor or type of field at cursor
        SourceClassInfo resolvedClass = source.resolveClassAtPosition(currentOffsetInEditor, true);

        if ((resolvedClass != null)) {
            className = resolvedClass.getVMName();

        //      NetBeansProfiler.getDefaultNB().displayInfo("<html><br><b>Will open root method selector for class at cursor:</b><br><br><code>" + resolvedClass.getVMClassName() + "</code></html>");
        }

        if (className == null) {
            // Try to get method enclosing cursor position
            className = source.getEnclosingClass(currentOffsetInEditor).getVMName();
        }

        if (className == null) {
            // Get toplevel class
            className = source.getTopLevelClass().getVMName();
        }
        return className;
    }
}
