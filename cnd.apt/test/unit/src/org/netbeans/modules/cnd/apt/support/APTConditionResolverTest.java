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
package org.netbeans.modules.cnd.apt.support;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;
import org.netbeans.modules.cnd.antlr.TokenStream;
import org.netbeans.modules.cnd.apt.impl.support.APTFileMacroMap;
import org.netbeans.modules.cnd.apt.structure.APTFile;
import org.netbeans.modules.cnd.apt.structure.APTInclude;
import org.netbeans.modules.cnd.apt.support.lang.APTLanguageSupport;
import org.netbeans.modules.cnd.apt.utils.APTUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.util.CharSequences;

/**
 *
 * @author Vladimir Voskresensky
 */
public class APTConditionResolverTest {
    private static final String MAX_INT_CHECK_CODE = "\n"
            + "#define INT_MAX __INT_MAX__\n"
            + "#define UINT_MAX (INT_MAX * 2U + 1U)\n"
            + "\n"
            + "# if (UINT_MAX) == 65535U\n"
            + "#     define SIZEOF_INT 2\n"
            + "#   elif ((UINT_MAX) == 4294967295U)\n"
            + "#     define SIZEOF_INT 4\n"
            + "#   else\n"
            + "#     error: unexpected int size, must be updated for this platform!\n"
            + "#   endif /* UINT_MAX */\n";
    
    private static final String MAX_LONG_CHECK_CODE ="\n"
            + "#define LONG_MAX __LONG_MAX__\n"
            + "#define ULONG_MAX (LONG_MAX * 2UL + 1UL)\n"
            + "\n"
            + "# if (ULONG_MAX) == 65535UL\n"
            + "#     define SIZEOF_LONG 2\n"
            + "#   elif ((ULONG_MAX) == 4294967295UL)\n"
            + "#     define SIZEOF_LONG 4\n"
            + "#   elif ((ULONG_MAX) == 18446744073709551615UL)\n"
            + "#     define SIZEOF_LONG 8\n"
            + "#   else\n"
            + "#     error: unsupported long size, must be updated for this platform!\n"
            + "#   endif /* ULONG_MAX */\n";
    

    @Test
    public void test2BytesInt() {
        doTestSizeof(MAX_INT_CHECK_CODE, "__INT_MAX__", Integer.toString((1<<15)-1), "SIZEOF_INT", 2);
    }

    @Test
    public void test4BytesInt() {
        doTestSizeof(MAX_INT_CHECK_CODE, "__INT_MAX__", Integer.toString((1<<31)-1), "SIZEOF_INT", 4);
    }

    @Test
    public void test2BytesLong() {
        doTestSizeof(MAX_LONG_CHECK_CODE, "__LONG_MAX__", Long.toString((1L << 15) - 1L), "SIZEOF_LONG", 2);
    }

    @Test
    public void test4BytesLong() {
        doTestSizeof(MAX_LONG_CHECK_CODE, "__LONG_MAX__", Long.toString((1L << 31) - 1L), "SIZEOF_LONG", 4);
    }

    @Test
    public void test8BytesLong() {
        doTestSizeof(MAX_LONG_CHECK_CODE, "__LONG_MAX__", Long.toString((1L << 63) - 1L), "SIZEOF_LONG", 8);
    }

    private void doTestSizeof(String src, String macroName, String maxValue, String testMacro, int sizeof) {
        APTFileMacroMap mmap = new APTFileMacroMap(null, Arrays.asList(macroName+"="+maxValue));
        APTMacro macro__INT_MAX__ = mmap.getMacro(APTUtils.createIDENT(CharSequences.create(macroName)));
        assertNotNull(macro__INT_MAX__);
        TokenStream lexer = APTTokenStreamBuilder.buildTokenStream(src, APTLanguageSupport.GNU_CPP);
        APTFile apt = APTBuilder.buildAPT(new DummyFileSystem(), "SIZEOF_CHECKER", lexer);
        APTWalker walker = new TestWalker(apt, mmap);
        walker.visit();
        assertFalse("failed to evaluate " + testMacro + " for " + macroName + "=" + maxValue, walker.isStopped());
        APTMacro macroSIZEOF_INT = mmap.getMacro(APTUtils.createIDENT(CharSequences.create(testMacro)));
        assertNotNull(macroSIZEOF_INT);
        String value = APTUtils.stringize(macroSIZEOF_INT.getBody(), false);
        assertEquals(testMacro, Integer.toString(sizeof), value);
    }

    private static final class TestWalker extends APTAbstractWalker {

        public TestWalker(APTFile apt, APTMacroMap macros) {
            super(apt, APTHandlersSupport.createPreprocHandler(macros, null, true), null);
        }

        @Override
        protected boolean include(ResolvedPath resolvedPath, APTInclude aptInclude, PostIncludeData postIncludeState) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean hasIncludeActionSideEffects() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class DummyFileSystem extends FileSystem {

        public DummyFileSystem() {
        }

        @Override
        public String getDisplayName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReadOnly() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject getRoot() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject findResource(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.openide.util.actions.SystemAction[] getActions() {
            throw new UnsupportedOperationException();
        }
    }
}