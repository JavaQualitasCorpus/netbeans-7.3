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
package org.netbeans.modules.ws.qaf;

import java.io.IOException;
import junit.framework.Test;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.OutputTabOperator;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.modules.ws.qaf.WebServicesTestBase.ProjectType;

/**
 *
 * @author jp154641
 */
public class JavaSEWsValidation extends WsValidation {

    /** Default constructor.
     * @param testName name of particular test case
     */
    public JavaSEWsValidation(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        assertServerRunning();
    }

    @Override
    protected ProjectType getProjectType() {
        return ProjectType.JAVASE_APPLICATION;
    }

    @Override
    protected String getWsClientProjectName() {
        return "WsClientInJavaSE"; //NOI18N
    }

    public static Test suite() {
        return NbModuleSuite.create(addServerTests(NbModuleSuite.createConfiguration(JavaSEWsValidation.class),
                "testCreateWsClient",
                "testCallWsOperationInJavaMainClass",
                "testWsClientHandlers",
                //"testRefreshClient",
                "testRunWsClientProject"
                ).enableModules(".*").clusters(".*"));
    }

    public void testCallWsOperationInJavaMainClass() {
        final EditorOperator eo = new EditorOperator("Main.java"); //NOI18N
        eo.select("// TODO code application logic here"); //NOI18N
        callWsOperation(eo, "myIntMethod", 18); //NOI18N
        assertTrue("Web service lookup class has not been found", eo.contains(getWsClientLookupCall())); //NOI18N
        assertFalse("@WebServiceRef present", eo.contains("@WebServiceRef")); //NOI18N
    }

    /**
     * Since there's not Deploy action for Java Projects, it's Run insteda and output checked
     * @throws java.io.IOException
     */
    public void testRunWsClientProject() throws IOException {
        runProject(getProjectName());
        OutputTabOperator oto = new OutputTabOperator(getProjectName());
        assertTrue(oto.getText().indexOf("Result = []") > -1); //NOI18N
        assertTrue(oto.getText().indexOf("BUILD SUCCESSFUL") > -1); //NOI18N
    }
}