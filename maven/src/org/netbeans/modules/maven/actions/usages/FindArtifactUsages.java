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
 * Contributor(s): theanuradha@netbeans.org
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.maven.actions.usages;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.apache.maven.artifact.Artifact;
import static org.netbeans.modules.maven.actions.usages.Bundle.*;
import org.netbeans.modules.maven.actions.usages.ui.UsagesUI;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Anuradha G
 */
public class FindArtifactUsages extends AbstractAction {

    private Artifact artifact;

    @Messages("LBL_FindartifactUsages=Find Usages")
    public FindArtifactUsages(Artifact artifact) {
        this.artifact = artifact;
        putValue(Action.NAME, LBL_FindartifactUsages());

    }

  //QualitasCorpus.class: Modified to Stub due to compilation errors
    private Object LBL_FindartifactUsages() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    @Messages("TIT_FindartifactUsages=Usages")
    public void actionPerformed(ActionEvent event) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<b>"); //NOI18N
        buffer.append(artifact.getArtifactId());
        buffer.append("</b>"); //NOI18N
        buffer.append(":"); //NOI18N
        buffer.append("<b>"); //NOI18N
        buffer.append(artifact.getVersion().toString());
        buffer.append("</b>"); //NOI18N

        UsagesUI uI = new UsagesUI(buffer.toString(), artifact);
        DialogDescriptor dd = new DialogDescriptor(uI, TIT_FindartifactUsages());
        uI.initialize(dd.createNotificationLineSupport());
        dd.setClosingOptions(new Object[]{
            DialogDescriptor.CLOSED_OPTION
        });
        dd.setOptions(new Object[]{
            DialogDescriptor.CLOSED_OPTION
        });
        DialogDisplayer.getDefault().notify(dd);

    }

	//QualitasCorpus.class: Modified to Stub due to compilation errors
	private String TIT_FindartifactUsages() {
		// TODO Auto-generated method stub
		return null;
	}
}