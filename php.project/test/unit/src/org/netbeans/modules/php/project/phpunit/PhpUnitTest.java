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

package org.netbeans.modules.php.project.phpunit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import org.netbeans.modules.php.project.util.PhpTestCase;
import org.openide.util.Utilities;

/**
 * @author Tomas Mysik
 */
public class PhpUnitTest extends PhpTestCase {

    public PhpUnitTest(String name) {
        super(name);
    }

    public void testLinePatternTestRunner() {
        Matcher matcher = PhpUnit.LINE_PATTERN.matcher("/home/gapon/test/Calculator.php:635");
        assertTrue(matcher.matches());
        assertEquals("/home/gapon/test/Calculator.php", matcher.group(1));
        assertEquals("635", matcher.group(2));
        assertTrue(PhpUnit.LINE_PATTERN.matcher("/h o m e/gapon/test/Calculator.php:635").matches());
        assertTrue(PhpUnit.LINE_PATTERN.matcher("C:\\home\\gapon\\test\\Calculator.php:635").matches());

        assertFalse(PhpUnit.LINE_PATTERN.matcher("").matches());
    }

    public void testLinePatternOutput() {
        assertTrue(PhpUnit.LINE_PATTERN.matcher("/home/gapon/test/Calculator.php:635").matches());

        Matcher matcher = PhpUnit.LINE_PATTERN.matcher("0.1077    6609264   6. PHPUnit_Util_Fileloader::checkAndLoad() /usr/share/php/PHPUnit/Framework/TestSuite.php:385");
        assertTrue(matcher.matches());
        assertEquals("/usr/share/php/PHPUnit/Framework/TestSuite.php", matcher.group(1));
        assertEquals("385", matcher.group(2));
    }

    public void testRelPath() {
        final File testFile = new File("/tmp/a.php");
        final File sourceFile = new File("/home/b.php");
        final String abs = "ABS/";
        final String rel = "REL/";
        final String suff = "/SUFF";

        String relPath = PhpUnit.getRelPath(testFile, sourceFile, abs, rel, suff, false);
        assertEquals(rel + ".." + sourceFile.getAbsolutePath() + suff, relPath);

        relPath = PhpUnit.getRelPath(testFile, sourceFile, abs, rel, suff, true);
        assertEquals(abs + sourceFile.getAbsolutePath() + suff, relPath);
    }

    public void testRequireOnceUnix() throws Exception {
        if (!Utilities.isUnix()) {
            return;
        }
        List<Crate> crates = new LinkedList<Crate>();
        crates.add(new Crate(
                "/project1/src/MyClass.php",
                "/project1/test/MyClassTest.php",
                PhpUnit.REQUIRE_ONCE_REL_PART + "../src/MyClass.php"));
        crates.add(new Crate(
                "/project1/src/a/MyClass.php",
                "/project1/test/a/MyClassTest.php",
                PhpUnit.REQUIRE_ONCE_REL_PART + "../../src/a/MyClass.php"));
        crates.add(new Crate(
                "/project1/src/a/b/c/MyClass.php",
                "/project1/test/a/b/c/MyClassTest.php",
                PhpUnit.REQUIRE_ONCE_REL_PART + "../../../../src/a/b/c/MyClass.php"));

        verifyCrates(crates);
    }

    public void testRequireOnceWindows() throws Exception {
        if (!Utilities.isWindows()) {
            return;
        }
        List<Crate> crates = new LinkedList<Crate>();
        crates.add(new Crate(
                "C:\\project1\\src\\MyClass.php",
                "C:\\project1\\test\\MyClassTest.php",
                PhpUnit.REQUIRE_ONCE_REL_PART + "../src/MyClass.php"));
        crates.add(new Crate(
                "C:\\project1\\src\\MyClass.php",
                "D:\\project1\\test\\MyClassTest.php",
                "C:/project1/src/MyClass.php"));

        verifyCrates(crates);
    }

    private void verifyCrates(List<Crate> creates) {
        for (Crate crate : creates) {
            String requireOnce = PhpUnit.getRequireOnce(crate.testFile, crate.sourceFile);
            assertNotNull(requireOnce);
            assertEquals(crate.requireOnce, requireOnce);
        }
    }

    // Inner classes

    private static final class Crate {

        public final File sourceFile;
        public final File testFile;
        public final String requireOnce;


        public Crate(String sourceFile, String testFile, String requireOnce) {
            this.sourceFile = new File(sourceFile);
            this.testFile = new File(testFile);
            this.requireOnce = requireOnce;
        }
    }

}
