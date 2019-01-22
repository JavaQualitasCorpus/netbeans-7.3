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

package org.netbeans.modules.groovy.editor.options;

import java.util.prefs.Preferences;
import javax.swing.text.Document;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import org.netbeans.modules.editor.indent.spi.CodeStylePreferences;

/** 
 *  XXX add support for profiles
 *  XXX get the preferences node from somewhere else in odrer to be able not to
 *      use the getters and to be able to write to it.
 * 
 * @author Dusan Balek, Martin Janicek
 */
public final class CodeStyle {

    private static final String continuationIndentSize = "continuationIndentSize"; //NOI18N
    private static final String reformatComments = "reformatComments"; //NOI18N
    private static final String indentHtml = "indentHtml"; //NOI18N
    private static final String rightMargin = SimpleValueNames.TEXT_LIMIT_WIDTH;

    private final Preferences preferences;
    private final Document doc;


    private CodeStyle(Document doc) {
        this.preferences = CodeStylePreferences.get(doc).getPreferences();
        this.doc = doc;
    }

    public static CodeStyle get(Document doc) {
        return new CodeStyle(doc);
    }

    public int getIndentSize() {
        return IndentUtils.indentLevelSize(doc);
    }

    public int getContinuationIndentSize() {
        return preferences.getInt(continuationIndentSize, 4);
    }

    public boolean isReformatComments() {
        return preferences.getBoolean(reformatComments, false);
    }

    public boolean isIndentHtml() {
        return preferences.getBoolean(indentHtml, true);
    }
    
    public int getRightMargin() {
        return preferences.getInt(rightMargin, 80);
    }
}