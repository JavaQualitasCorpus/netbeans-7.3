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

package org.netbeans.modules.web.clientproject;

/**
 *
 */
public class ClientSideProjectConstants {

    // misc persistent project properties:
    public static final String PROJECT_ENCODING = "files.encoding"; //NOI18N
    public static final String PROJECT_SITE_ROOT_FOLDER = "site.root.folder"; //NOI18N
    public static final String PROJECT_TEST_FOLDER = "test.folder"; //NOI18N
    public static final String PROJECT_CONFIG_FOLDER = "config.folder"; //NOI18N
    public static final String PROJECT_SERVER = "server"; //NOI18N
    public static final String PROJECT_START_FILE = "start.file"; //NOI18N
    public static final String PROJECT_WEB_ROOT = "web.context.root"; //NOI18N
    public static final String PROJECT_PROJECT_URL = "external.project.url"; //NOI18N
    public static final String PROJECT_AUTO_REFRESH = "browser.autorefresh"; //NOI18N
    public static final String PROJECT_HIGHLIGHT_SELECTION = "browser.highlightselection"; //NOI18N

    // default paths:
    public static final String DEFAULT_SITE_ROOT_FOLDER = "public_html"; //NOI18N
    public static final String DEFAULT_TEST_FOLDER = "test"; //NOI18N
    public static final String DEFAULT_CONFIG_FOLDER = "config"; //NOI18N
    
    // template's descriptor file
    public static final String TEMPLATE_DESCRIPTOR = "netbeans-project.properties"; //NOI18N
}
