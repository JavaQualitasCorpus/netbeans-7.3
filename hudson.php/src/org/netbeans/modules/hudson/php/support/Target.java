/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.hudson.php.support;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.hudson.php.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Base class for Ant target customizations of <tt>build.xml</tt>
 * generated by <a href="https://github.com/sebastianbergmann/php-project-wizard">ppw</a> (PHP Project Wizard).
 * <p>
 * This class and all successors are thread-safe.
 */
public abstract class Target {

    protected final Logger logger = Logger.getLogger(getClass().getName());

    private volatile boolean selected = true;
    private volatile String selectedOption = null;


    Target() {
    }

    public static List<Target> all() {
        return Arrays.<Target>asList(
                new PhpmdTarget(),
                new PhpcpdTarget(),
                new PhpcsTarget(),
                new PhpdocTarget(),
                new PhplocTarget()
        );
    }

    public abstract String getName();

    public abstract String getTitleWithMnemonic();

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEnabled() {
        return true;
    }

    public List<String> getOptions() {
        return null;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public void apply(Map<String, String> commandParams) {
        // noop
    }

    public boolean apply(Document document) {
        if (isSelected()) {
            // noop
            return true;
        }
        return commentNode(document, "//antcall[@target='" + getName() + "']"); // NOI18N
    }

    protected boolean commentNode(Document document, String xpathExpression) {
        Node node = XmlUtils.query(document, xpathExpression);
        if (node == null) {
            logger.log(Level.WARNING, "Node not found for {0}", xpathExpression);
            return false;
        }
        XmlUtils.commentNode(document, node);
        return true;
    }

}
