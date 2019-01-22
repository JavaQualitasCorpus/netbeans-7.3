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
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.dwarfdiscovery.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.cnd.api.toolchain.CompilerFlavor;
import org.netbeans.modules.cnd.api.toolchain.PlatformTypes;
import org.netbeans.modules.cnd.discovery.api.ItemProperties;
import org.netbeans.modules.cnd.discovery.api.ProjectProxy;
import org.netbeans.modules.cnd.dwarfdiscovery.provider.BaseDwarfProvider.GrepEntry;
import org.netbeans.modules.cnd.dwarfdump.CompilationUnitInterface;
import org.netbeans.modules.cnd.dwarfdump.CompileLineService;
import org.netbeans.modules.cnd.dwarfdump.CompileLineService.SourceFile;
import org.netbeans.modules.cnd.dwarfdump.Dwarf;
import org.netbeans.modules.cnd.dwarfdump.Dwarf.CompilationUnitIterator;
import org.netbeans.modules.cnd.dwarfdump.exception.WrongFileFormatException;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author Alexander Simon
 */
public class DwarfSourceReaderTest extends NbTestCase {

    public DwarfSourceReaderTest() {
        super("DwarfSourceReaderTest");
        Logger.getLogger("cnd.logger").setLevel(Level.SEVERE);
    }

    @Override
    protected int timeOut() {
        return 500000;
    }

    public void testDllReader(){
        File dataDir = getDataDir();
        String objFileName = dataDir.getAbsolutePath()+"/org/netbeans/modules/cnd/dwarfdiscovery/provider/echo";
        Dwarf dump = null;
        try {
            dump = new Dwarf(objFileName);
            for(String dll : dump.readPubNames().getDlls()) {
                assertEquals(dll, "libc.so.1"); // NOI18N
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (WrongFileFormatException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (dump != null) {
                dump.dispose();
            }
        }
    }

    public void testSunStudioCompiler(){
        TreeMap<String, String> golden = new TreeMap<String, String>();
        golden.put("TEXT_DOMAIN", "\"SUNW_OST_OSCMD\"");
        golden.put("_TS_ERRNO", "null");
        golden.put("_iBCS2", "null");
        TreeMap<String, String> ignore = new TreeMap<String, String>();
        List<String> system = new ArrayList<String>();
        Map<String,GrepEntry> grepBase = new HashMap<String, GrepEntry>();
        DwarfSource source = getDwarfSource("/org/netbeans/modules/cnd/dwarfdiscovery/provider/echo", system, ignore, grepBase, false, null);
        assertNotNull(source);
        TreeMap<String, String> map = new TreeMap<String, String>(source.getUserMacros());
        assertTrue(compareMap(map, golden));
        assertTrue(source.getUserInludePaths().size()==1);
        assertEquals(source.getUserInludePaths().get(0), "/export1/sside/pomona/java_cp/wsb131/proto/root_i386/usr/include");
        printInclidePaths(source);
        List<SourceFile> list = CompileLineService.getSourceFileProperties(getDataDir().getAbsolutePath()+"/org/netbeans/modules/cnd/dwarfdiscovery/provider/echo");
        assertEquals(1, list.size());
        SourceFile sf = list.get(0);
        assertEquals(sf.getCompileDir(), "/export1/sside/pomona/java_cp/wsb131/usr/src/cmd/echo");
        assertEquals(sf.getSource(), "echo.c");
//      /export/opt/sunstudio/12ml/SUNWspro/prod/bin/cc -O -xspace -Xa -xildoff -errtags=yes -errwarn=%all -erroff=E_EMPTY_TRANSLATION_UNIT -erroff=E_STATEMENT_NOT_REACHED -xc99=%none -W0,-xglobalstatic -v -D_iBCS2 -DTEXT_DOMAIN='"SUNW_OST_OSCMD"' -D_TS_ERRNO -I/export1/sside/pomona/java_cp/wsb131/proto/root_i386/usr/include -Bdirect -M/export1/sside/pomona/java_cp/wsb131/usr/src/common/mapfiles/common/map.noexstk -M/export1/sside/pomona/java_cp/wsb131/usr/src/common/mapfiles/i386/map.pagealign -M/export1/sside/pomona/java_cp/wsb131/usr/src/common/mapfiles/i386/map.noexdata -L/export1/sside/pomona/java_cp/wsb131/proto/root_i386/lib -L/export1/sside/pomona/java_cp/wsb131/proto/root_i386/usr/lib -c  echo.c
        //System.err.println(sf.getCompileLine());
        map = new TreeMap<String, String>(sf.getUserMacros());
        assertTrue(compareMap(map, golden));
        assertEquals(sf.getUserPaths().get(0), "/export1/sside/pomona/java_cp/wsb131/proto/root_i386/usr/include");
        list = CompileLineService.getSourceFolderProperties(getDataDir().getAbsolutePath());
        //assertEquals(8, list.size());
        int i = 0;
        for(SourceFile file : list){
            if (!file.getCompileLine().isEmpty()) {
                i++;
            }
        }
        assertEquals(2, i);
    }

    public void testLeopard(){
        TreeMap<String, String> golden = new TreeMap<String, String>();
        golden.put("OBJC_NEW_PROPERTIES", "1");
        TreeMap<String, String> ignore = new TreeMap<String, String>();
        ignore.put("__APPLE_CC__", "5465");
        ignore.put("__APPLE__", "1");
        ignore.put("__CHAR_BIT__", "8");
        ignore.put("__CONSTANT_CFSTRINGS__", "1");
        ignore.put("__DBL_DENORM_MIN__", "4.9406564584124654e-324");
        ignore.put("__DBL_DIG__", "15");
        ignore.put("__DBL_EPSILON__", "2.2204460492503131e-16");
        ignore.put("__DBL_HAS_INFINITY__", "1");
        ignore.put("__DBL_HAS_QUIET_NAN__", "1");
        ignore.put("__DBL_MANT_DIG__", "53");
        ignore.put("__DBL_MAX_10_EXP__", "308");
        ignore.put("__DBL_MAX_EXP__", "1024");
        ignore.put("__DBL_MAX__", "1.7976931348623157e+308");
        ignore.put("__DBL_MIN_10_EXP__", "(-307)");
        ignore.put("__DBL_MIN_EXP__", "(-1021)");
        ignore.put("__DBL_MIN__", "2.2250738585072014e-308");
        ignore.put("__DECIMAL_DIG__", "21");
        ignore.put("__DEPRECATED", "1");
        ignore.put("__DYNAMIC__", "1");
        ignore.put("__ENVIRONMENT_MAC_OS_X_VERSION_MIN_REQUIRED__", "1050");
        ignore.put("__EXCEPTIONS", "1");
        ignore.put("__FINITE_MATH_ONLY__", "0");
        ignore.put("__FLT_DENORM_MIN__", "1.40129846e-45F");
        ignore.put("__FLT_DIG__", "6");
        ignore.put("__FLT_EPSILON__", "1.19209290e-7F");
        ignore.put("__FLT_EVAL_METHOD__", "0");
        ignore.put("__FLT_HAS_INFINITY__", "1");
        ignore.put("__FLT_HAS_QUIET_NAN__", "1");
        ignore.put("__FLT_MANT_DIG__", "24");
        ignore.put("__FLT_MAX_10_EXP__", "38");
        ignore.put("__FLT_MAX_EXP__", "128");
        ignore.put("__FLT_MAX__", "3.40282347e+38F");
        ignore.put("__FLT_MIN_10_EXP__", "(-37)");
        ignore.put("__FLT_MIN_EXP__", "(-125)");
        ignore.put("__FLT_MIN__", "1.17549435e-38F");
        ignore.put("__FLT_RADIX__", "2");
        ignore.put("__GNUC_MINOR__", "0");
        ignore.put("__GNUC_PATCHLEVEL__", "1");
        ignore.put("__GNUC__", "4");
        ignore.put("__GNUG__", "4");
        ignore.put("__GXX_ABI_VERSION", "1002");
        ignore.put("__GXX_WEAK__", "1");
        ignore.put("__INTMAX_MAX__", "9223372036854775807LL");
        ignore.put("__INTMAX_TYPE__", "long long int");
        ignore.put("__INT_MAX__", "2147483647");
        ignore.put("__LDBL_DENORM_MIN__", "3.64519953188247460253e-4951L");
        ignore.put("__LDBL_DIG__", "18");
        ignore.put("__LDBL_EPSILON__", "1.08420217248550443401e-19L");
        ignore.put("__LDBL_HAS_INFINITY__", "1");
        ignore.put("__LDBL_HAS_QUIET_NAN__", "1");
        ignore.put("__LDBL_MANT_DIG__", "64");
        ignore.put("__LDBL_MAX_10_EXP__", "4932");
        ignore.put("__LDBL_MAX_EXP__", "16384");
        ignore.put("__LDBL_MAX__", "1.18973149535723176502e+4932L");
        ignore.put("__LDBL_MIN_10_EXP__", "(-4931)");
        ignore.put("__LDBL_MIN_EXP__", "(-16381)");
        ignore.put("__LDBL_MIN__", "3.36210314311209350626e-4932L");
        ignore.put("__LITTLE_ENDIAN__", "1");
        ignore.put("__LONG_LONG_MAX__", "9223372036854775807LL");
        ignore.put("__LONG_MAX__", "2147483647L");
        ignore.put("__MACH__", "1");
        ignore.put("__MMX__", "1");
        ignore.put("__NO_INLINE__", "1");
        ignore.put("__PIC__", "1");
        ignore.put("__PTRDIFF_TYPE__", "int");
        ignore.put("__REGISTER_PREFIX__", "");
        ignore.put("__SCHAR_MAX__", "127");
        ignore.put("__SHRT_MAX__", "32767");
        ignore.put("__SIZE_TYPE__", "long unsigned int");
        ignore.put("__SSE2_MATH__", "1");
        ignore.put("__SSE2__", "1");
        ignore.put("__SSE_MATH__", "1");
        ignore.put("__SSE__", "1");
        ignore.put("__STDC_HOSTED__", "1");
        ignore.put("__UINTMAX_TYPE__", "long long unsigned int");
        ignore.put("__USER_LABEL_PREFIX__", "_");
        ignore.put("__VERSION__", "\"4.0.1 (Apple Inc. build 5465)\"");
        ignore.put("__WCHAR_MAX__", "2147483647");
        ignore.put("__WCHAR_TYPE__", "int");
        ignore.put("__WINT_TYPE__", "int");
        ignore.put("__cplusplus", "1");
        ignore.put("__i386", "1");
        ignore.put("__i386__", "1");
        ignore.put("__private_extern__", "extern");
        ignore.put("__strong", "");
        ignore.put("__weak", "");
        ignore.put("i386", "1");
        List<String> system = new ArrayList<String>();
        String prefix = "";
        system.add(prefix+"/usr/include");
        system.add(prefix+"/usr/include/c++/4.0.0");
        system.add(prefix+"/usr/include/c++/4.0.0/i686-apple-darwin9/bits");
        system.add(prefix+"/usr/include/libkern");
        system.add(prefix+"/usr/include/libkern/i386");
        system.add(prefix+"/usr/include/mach/i386");
        system.add(prefix+"/usr/lib/gcc/i686-apple-darwin9/4.0.1/include");
        Map<String,GrepEntry> grepBase = new HashMap<String, GrepEntry>();
        GrepEntry entry = new GrepEntry();
        grepBase.put(prefix+"/usr/include", entry);
        entry.includes.add("/sys");
        entry.includes.add("/machine");
        entry.includes.add("/i386");
        entry = new GrepEntry();
        grepBase.put(prefix+"/usr/include/c++/4.0.0", entry);
        entry.includes.add("/bits");
        entry.includes.add("/debug");
        entry.includes.add("/ext");
        DwarfSource source = getDwarfSource("/org/netbeans/modules/cnd/dwarfdiscovery/provider/cpu-g3-gdwarf-2.leopard.o", system, ignore, grepBase, false, null);
        assertNotNull(source);
        TreeMap<String, String> map = new TreeMap<String, String>(source.getUserMacros());
        assertTrue(compareMap(map, golden));
        assertTrue(source.getUserInludePaths().size()==1);
        assertEquals(source.getUserInludePaths().get(0), prefix+"/Users/guest/Quote_10");
        printInclidePaths(source);
    }

    public void testCygwin(){
        TreeMap<String, String> golden = new TreeMap<String, String>();
        golden.put("__CYGWIN32__", "1");
        golden.put("__CYGWIN__", "1");
        golden.put("unix", "1");
        golden.put("__unix__", "1");
        golden.put("__unix", "1");
        golden.put("AAA", "1");
        golden.put("BBB", "11");
        TreeMap<String, String> ignore = new TreeMap<String, String>();
        List<String> system = new ArrayList<String>();
        String prefix = "C:/cygwin";
        system.add(prefix+"/usr/include");
        system.add(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include/c++/i686-pc-cygwin/bits");
        system.add(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include");
        system.add(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include/c++");
        Map<String,GrepEntry> grepBase = new HashMap<String, GrepEntry>();
        GrepEntry entry = new GrepEntry();
        grepBase.put(prefix+"/usr/include", entry);
        entry.includes.add("/sys");
        entry.includes.add("/machine");
        entry.includes.add("/cygwin");
        entry = new GrepEntry();
        grepBase.put(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include/c++", entry);
        entry.includes.add("/bits");
        entry.includes.add("/debug");
        entry.includes.add("/ext");
        DwarfSource source = getDwarfSource("/org/netbeans/modules/cnd/dwarfdiscovery/provider/quote.cygwin.o", system, ignore, grepBase, true, prefix);
        TreeMap<String, String> map = new TreeMap<String, String>(source.getUserMacros());
        assertTrue(compareMap(map, golden));
        assertTrue(source.getUserInludePaths().size()==1);
        assertEquals(source.getUserInludePaths().get(0), "C:/Documents and Settings/tester/My Documents/NetBeansProjects/Quote_1");
        printInclidePaths(source);
    }

    public void testCygwin2(){
        TreeMap<String, String> golden = new TreeMap<String, String>();
        golden.put("__CYGWIN32__", "1");
        golden.put("__CYGWIN__", "1");
        golden.put("unix", "1");
        golden.put("__unix__", "1");
        golden.put("__unix", "1");
        golden.put("HAVE_CONFIG_H", "1");
        TreeMap<String, String> ignore = new TreeMap<String, String>();
        List<String> system = new ArrayList<String>();
        String prefix = "D:/cygwin";
        system.add(prefix+"/usr/include");
        system.add(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include/c++/i686-pc-cygwin/bits");
        system.add(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include");
        system.add(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include/c++");
        Map<String,GrepEntry> grepBase = new HashMap<String, GrepEntry>();
        GrepEntry entry = new GrepEntry();
        grepBase.put(prefix+"/usr/include", entry);
        entry.includes.add("/sys");
        entry.includes.add("/machine");
        entry.includes.add("/cygwin");
        entry = new GrepEntry();
        grepBase.put(prefix+"/lib/gcc/i686-pc-cygwin/3.4.4/include/c++", entry);
        entry.includes.add("/bits");
        entry.includes.add("/debug");
        entry.includes.add("/ext");
        DwarfSource source = getDwarfSource("/org/netbeans/modules/cnd/dwarfdiscovery/provider/string.cygwin.o", system, ignore, grepBase, true, prefix);
        assertNotNull(source);
        TreeMap<String, String> map = new TreeMap<String, String>(source.getUserMacros());
        assertTrue(compareMap(map, golden));
        prefix = "D:";
        assertTrue(compareLists(source.getUserInludePaths(), new String[]{
                "../..",
                "./../../include/litesql",
                prefix+"/usr_/masha/projects/litesql_latest/litesql-0.3.2/src/library",
                prefix+"/usr_/masha/projects/litesql_latest/litesql-0.3.2",
                prefix+"/usr_/masha/projects/litesql_latest/litesql-0.3.2/include/litesql"
                }));
        printInclidePaths(source);
    }

    public void testGentoo43(){
        TreeMap<String, String> golden = new TreeMap<String, String>();
        golden.put("COMMAND_LINE_MACROS_1", "1");
        golden.put("COMMAND_LINE_MACROS_2", "1");
        golden.put("SOURCE_CODE_MACROS_1", "1");
        TreeMap<String, String> ignore = new TreeMap<String, String>();
        ignore.put("_GNU_SOURCE", "1");
        ignore.put("_LP64", "1");
        ignore.put("__CHAR_BIT__", "8");
        ignore.put("__DBL_DENORM_MIN__", "4.9406564584124654e-324");
        ignore.put("__DBL_DIG__", "15");
        ignore.put("__DBL_EPSILON__", "2.2204460492503131e-16");
        ignore.put("__DBL_HAS_DENORM__", "1");
        ignore.put("__DBL_HAS_INFINITY__", "1");
        ignore.put("__DBL_HAS_QUIET_NAN__", "1");
        ignore.put("__DBL_MANT_DIG__", "53");
        ignore.put("__DBL_MAX_10_EXP__", "308");
        ignore.put("__DBL_MAX_EXP__", "1024");
        ignore.put("__DBL_MAX__", "1.7976931348623157e+308");
        ignore.put("__DBL_MIN_10_EXP__", "(-307)");
        ignore.put("__DBL_MIN_EXP__", "(-1021)");
        ignore.put("__DBL_MIN__", "2.2250738585072014e-308");
        ignore.put("__DEC128_DEN__", "0.000000000000000000000000000000001E-6143DL");
        ignore.put("__DEC128_EPSILON__", "1E-33DL");
        ignore.put("__DEC128_MANT_DIG__", "34");
        ignore.put("__DEC128_MAX_EXP__", "6144");
        ignore.put("__DEC128_MAX__", "9.999999999999999999999999999999999E6144DL");
        ignore.put("__DEC128_MIN_EXP__", "(-6143)");
        ignore.put("__DEC128_MIN__", "1E-6143DL");
        ignore.put("__DEC32_DEN__", "0.000001E-95DF");
        ignore.put("__DEC32_EPSILON__", "1E-6DF");
        ignore.put("__DEC32_MANT_DIG__", "7");
        ignore.put("__DEC32_MAX_EXP__", "96");
        ignore.put("__DEC32_MAX__", "9.999999E96DF");
        ignore.put("__DEC32_MIN_EXP__", "(-95)");
        ignore.put("__DEC32_MIN__", "1E-95DF");
        ignore.put("__DEC64_DEN__", "0.000000000000001E-383DD");
        ignore.put("__DEC64_EPSILON__", "1E-15DD");
        ignore.put("__DEC64_MANT_DIG__", "16");
        ignore.put("__DEC64_MAX_EXP__", "384");
        ignore.put("__DEC64_MAX__", "9.999999999999999E384DD");
        ignore.put("__DEC64_MIN_EXP__", "(-383)");
        ignore.put("__DEC64_MIN__", "1E-383DD");
        ignore.put("__DECIMAL_BID_FORMAT__", "1");
        ignore.put("__DECIMAL_DIG__", "21");
        ignore.put("__DEC_EVAL_METHOD__", "2");
        ignore.put("__DEPRECATED", "1");
        ignore.put("__ELF__", "1");
        ignore.put("__EXCEPTIONS", "1");
        ignore.put("__FINITE_MATH_ONLY__", "0");
        ignore.put("__FLT_DENORM_MIN__", "1.40129846e-45F");
        ignore.put("__FLT_DIG__", "6");
        ignore.put("__FLT_EPSILON__", "1.19209290e-7F");
        ignore.put("__FLT_EVAL_METHOD__", "0");
        ignore.put("__FLT_HAS_DENORM__", "1");
        ignore.put("__FLT_HAS_INFINITY__", "1");
        ignore.put("__FLT_HAS_QUIET_NAN__", "1");
        ignore.put("__FLT_MANT_DIG__", "24");
        ignore.put("__FLT_MAX_10_EXP__", "38");
        ignore.put("__FLT_MAX_EXP__", "128");
        ignore.put("__FLT_MAX__", "3.40282347e+38F");
        ignore.put("__FLT_MIN_10_EXP__", "(-37)");
        ignore.put("__FLT_MIN_EXP__", "(-125)");
        ignore.put("__FLT_MIN__", "1.17549435e-38F");
        ignore.put("__FLT_RADIX__", "2");
        ignore.put("__GCC_HAVE_SYNC_COMPARE_AND_SWAP_1", "1");
        ignore.put("__GCC_HAVE_SYNC_COMPARE_AND_SWAP_2", "1");
        ignore.put("__GCC_HAVE_SYNC_COMPARE_AND_SWAP_4", "1");
        ignore.put("__GCC_HAVE_SYNC_COMPARE_AND_SWAP_8", "1");
        ignore.put("__GNUC_GNU_INLINE__", "1");
        ignore.put("__GNUC_MINOR__", "3");
        ignore.put("__GNUC_PATCHLEVEL__", "2");
        ignore.put("__GNUC__", "4");
        ignore.put("__GNUG__", "4");
        ignore.put("__GXX_ABI_VERSION", "1002");
        ignore.put("__GXX_RTTI", "1");
        ignore.put("__GXX_WEAK__", "1");
        ignore.put("__INTMAX_MAX__", "9223372036854775807L");
        ignore.put("__INTMAX_TYPE__", "long int");
        ignore.put("__INT_MAX__", "2147483647");
        ignore.put("__LDBL_DENORM_MIN__", "3.64519953188247460253e-4951L");
        ignore.put("__LDBL_DIG__", "18");
        ignore.put("__LDBL_EPSILON__", "1.08420217248550443401e-19L");
        ignore.put("__LDBL_HAS_DENORM__", "1");
        ignore.put("__LDBL_HAS_INFINITY__", "1");
        ignore.put("__LDBL_HAS_QUIET_NAN__", "1");
        ignore.put("__LDBL_MANT_DIG__", "64");
        ignore.put("__LDBL_MAX_10_EXP__", "4932");
        ignore.put("__LDBL_MAX_EXP__", "16384");
        ignore.put("__LDBL_MAX__", "1.18973149535723176502e+4932L");
        ignore.put("__LDBL_MIN_10_EXP__", "(-4931)");
        ignore.put("__LDBL_MIN_EXP__", "(-16381)");
        ignore.put("__LDBL_MIN__", "3.36210314311209350626e-4932L");
        ignore.put("__LONG_LONG_MAX__", "9223372036854775807LL");
        ignore.put("__LONG_MAX__", "9223372036854775807L");
        ignore.put("__LP64__", "1");
        ignore.put("__MMX__", "1");
        ignore.put("__NO_INLINE__", "1");
        ignore.put("__PTRDIFF_TYPE__", "long int");
        ignore.put("__REGISTER_PREFIX__", "");
        ignore.put("__SCHAR_MAX__", "127");
        ignore.put("__SHRT_MAX__", "32767");
        ignore.put("__SIZEOF_DOUBLE__", "8");
        ignore.put("__SIZEOF_FLOAT__", "4");
        ignore.put("__SIZEOF_INT__", "4");
        ignore.put("__SIZEOF_LONG_DOUBLE__", "16");
        ignore.put("__SIZEOF_LONG_LONG__", "8");
        ignore.put("__SIZEOF_LONG__", "8");
        ignore.put("__SIZEOF_POINTER__", "8");
        ignore.put("__SIZEOF_PTRDIFF_T__", "8");
        ignore.put("__SIZEOF_SHORT__", "2");
        ignore.put("__SIZEOF_SIZE_T__", "8");
        ignore.put("__SIZEOF_WCHAR_T__", "4");
        ignore.put("__SIZEOF_WINT_T__", "4");
        ignore.put("__SIZE_TYPE__", "long unsigned int");
        ignore.put("__SSE2_MATH__", "1");
        ignore.put("__SSE2__", "1");
        ignore.put("__SSE_MATH__", "1");
        ignore.put("__SSE__", "1");
        ignore.put("__STDC_HOSTED__", "1");
        ignore.put("__STDC__", "1");
        ignore.put("__UINTMAX_TYPE__", "long unsigned int");
        ignore.put("__USER_LABEL_PREFIX__", "");
        ignore.put("__VERSION__", "\"4.3.2\"");
        ignore.put("__WCHAR_MAX__", "2147483647");
        ignore.put("__WCHAR_TYPE__", "int");
        ignore.put("__WINT_TYPE__", "unsigned int");
        ignore.put("__amd64", "1");
        ignore.put("__amd64__", "1");
        ignore.put("__cplusplus", "1");
        ignore.put("__gnu_linux__", "1");
        ignore.put("__k8", "1");
        ignore.put("__k8__", "1");
        ignore.put("__linux", "1");
        ignore.put("__linux__", "1");
        ignore.put("__unix", "1");
        ignore.put("__unix__", "1");
        ignore.put("__x86_64", "1");
        ignore.put("__x86_64__", "1");
        ignore.put("linux", "1");
        ignore.put("unix", "1");
        List<String> system = new ArrayList<String>();
        String prefix = "";
        system.add(prefix+"/usr/include");
        system.add(prefix+"/usr/lib/gcc/x86_64-pc-linux-gnu/4.3.2/include");
        system.add(prefix+"/usr/lib/gcc/x86_64-pc-linux-gnu/4.3.2/include/g++-v4");
        system.add(prefix+"/usr/lib/gcc/x86_64-pc-linux-gnu/4.3.2/include/g++-v4/x86_64-pc-linux-gnu/bits");
        Map<String,GrepEntry> grepBase = new HashMap<String, GrepEntry>();
        GrepEntry entry = new GrepEntry();
        grepBase.put(prefix+"/usr/include", entry);
        entry.includes.add("/sys");
        entry.includes.add("/bits");
        entry.includes.add("/gnu");
        entry = new GrepEntry();
        grepBase.put(prefix+"/usr/lib/gcc/x86_64-pc-linux-gnu/4.3.2/include/g++-v4", entry);
        entry.includes.add("/bits");
        entry.includes.add("/debug");
        entry.includes.add("/ext");
        entry.includes.add("/backward");
        DwarfSource source = getDwarfSource("/org/netbeans/modules/cnd/dwarfdiscovery/provider/cpu.gentoo.4.3.o", system, ignore, grepBase, false, null);
        assertNotNull(source);
        TreeMap<String, String> map = new TreeMap<String, String>(source.getUserMacros());
        assertTrue(compareMap(map, golden));
        assertTrue(source.getUserInludePaths().size()==1);
        assertEquals(source.getUserInludePaths().get(0), prefix+"/export/home/av202691/NetBeansProjects/Quote_1");
        printInclidePaths(source);
    }

    public void testRedhat(){
        TreeMap<String, String> golden = new TreeMap<String, String>();
        golden.put("HAVE_CONFIG_H", "1");
        golden.put("HTIOP_BUILD_DLL", "1");
        golden.put("PIC", "1");
        TreeMap<String, String> ignore = new TreeMap<String, String>();
        ignore.put("_GNU_SOURCE", "1");
        ignore.put("_LP64", "1");
        ignore.put("_REENTRANT", "1");
        ignore.put("__CHAR_BIT__", "8");
        ignore.put("__DBL_DENORM_MIN__", "4.9406564584124654e-324");
        ignore.put("__DBL_DIG__", "15");
        ignore.put("__DBL_EPSILON__", "2.2204460492503131e-16");
        ignore.put("__DBL_HAS_INFINITY__", "1");
        ignore.put("__DBL_HAS_QUIET_NAN__", "1");
        ignore.put("__DBL_MANT_DIG__", "53");
        ignore.put("__DBL_MAX_10_EXP__", "308");
        ignore.put("__DBL_MAX_EXP__", "1024");
        ignore.put("__DBL_MAX__", "1.7976931348623157e+308");
        ignore.put("__DBL_MIN_10_EXP__", "(-307)");
        ignore.put("__DBL_MIN_EXP__", "(-1021)");
        ignore.put("__DBL_MIN__", "2.2250738585072014e-308");
        ignore.put("__DECIMAL_DIG__", "21");
        ignore.put("__DEPRECATED", "1");
        ignore.put("__ELF__", "1");
        ignore.put("__EXCEPTIONS", "1");
        ignore.put("__FINITE_MATH_ONLY__", "0");
        ignore.put("__FLT_DENORM_MIN__", "1.40129846e-45F");
        ignore.put("__FLT_DIG__", "6");
        ignore.put("__FLT_EPSILON__", "1.19209290e-7F");
        ignore.put("__FLT_EVAL_METHOD__", "0");
        ignore.put("__FLT_HAS_INFINITY__", "1");
        ignore.put("__FLT_HAS_QUIET_NAN__", "1");
        ignore.put("__FLT_MANT_DIG__", "24");
        ignore.put("__FLT_MAX_10_EXP__", "38");
        ignore.put("__FLT_MAX_EXP__", "128");
        ignore.put("__FLT_MAX__", "3.40282347e+38F");
        ignore.put("__FLT_MIN_10_EXP__", "(-37)");
        ignore.put("__FLT_MIN_EXP__", "(-125)");
        ignore.put("__FLT_MIN__", "1.17549435e-38F");
        ignore.put("__FLT_RADIX__", "2");
        ignore.put("__GNUC_GNU_INLINE__", "1");
        ignore.put("__GNUC_MINOR__", "1");
        ignore.put("__GNUC_PATCHLEVEL__", "2");
        ignore.put("__GNUC_RH_RELEASE__", "42");
        ignore.put("__GNUC__", "4");
        ignore.put("__GNUG__", "4");
        ignore.put("__GXX_ABI_VERSION", "1002");
        ignore.put("__GXX_WEAK__", "1");
        ignore.put("__INTMAX_MAX__", "9223372036854775807L");
        ignore.put("__INTMAX_TYPE__", "long int");
        ignore.put("__INT_MAX__", "2147483647");
        ignore.put("__LDBL_DENORM_MIN__", "3.64519953188247460253e-4951L");
        ignore.put("__LDBL_DIG__", "18");
        ignore.put("__LDBL_EPSILON__", "1.08420217248550443401e-19L");
        ignore.put("__LDBL_HAS_INFINITY__", "1");
        ignore.put("__LDBL_HAS_QUIET_NAN__", "1");
        ignore.put("__LDBL_MANT_DIG__", "64");
        ignore.put("__LDBL_MAX_10_EXP__", "4932");
        ignore.put("__LDBL_MAX_EXP__", "16384");
        ignore.put("__LDBL_MAX__", "1.18973149535723176502e+4932L");
        ignore.put("__LDBL_MIN_10_EXP__", "(-4931)");
        ignore.put("__LDBL_MIN_EXP__", "(-16381)");
        ignore.put("__LDBL_MIN__", "3.36210314311209350626e-4932L");
        ignore.put("__LONG_LONG_MAX__", "9223372036854775807LL");
        ignore.put("__LONG_MAX__", "9223372036854775807L");
        ignore.put("__LP64__", "1");
        ignore.put("__MMX__", "1");
        ignore.put("__NO_INLINE__", "1");
        ignore.put("__PIC__", "1");
        ignore.put("__PTRDIFF_TYPE__", "long int");
        ignore.put("__REGISTER_PREFIX__", "");
        ignore.put("__SCHAR_MAX__", "127");
        ignore.put("__SHRT_MAX__", "32767");
        ignore.put("__SIZE_TYPE__", "long unsigned int");
        ignore.put("__SSE2_MATH__", "1");
        ignore.put("__SSE2__", "1");
        ignore.put("__SSE_MATH__", "1");
        ignore.put("__SSE__", "1");
        ignore.put("__STDC_HOSTED__", "1");
        ignore.put("__STDC__", "1");
        ignore.put("__UINTMAX_TYPE__", "long unsigned int");
        ignore.put("__USER_LABEL_PREFIX__", "");
        ignore.put("__VERSION__", "\"4.1.2 20071124 (Red Hat 4.1.2-42)\"");
        ignore.put("__WCHAR_MAX__", "2147483647");
        ignore.put("__WCHAR_TYPE__", "int");
        ignore.put("__WINT_TYPE__", "unsigned int");
        ignore.put("__amd64", "1");
        ignore.put("__amd64__", "1");
        ignore.put("__cplusplus", "1");
        ignore.put("__gnu_linux__", "1");
        ignore.put("__k8", "1");
        ignore.put("__k8__", "1");
        ignore.put("__linux", "1");
        ignore.put("__linux__", "1");
        ignore.put("__pic__", "1");
        ignore.put("__unix", "1");
        ignore.put("__unix__", "1");
        ignore.put("__x86_64", "1");
        ignore.put("__x86_64__", "1");
        ignore.put("linux", "1");
        ignore.put("unix", "1");
        List<String> system = new ArrayList<String>();
        String prefix = "";
        system.add(prefix+"/usr/include");
        system.add(prefix+"/usr/include/c++/4.1.2");
        system.add(prefix+"/usr/lib/gcc/x86_64-redhat-linux/4.1.2/include");
        system.add(prefix+"/usr/include/c++/4.1.2/x86_64-redhat-linux/bits");
        Map<String,GrepEntry> grepBase = new HashMap<String, GrepEntry>();
        GrepEntry entry = new GrepEntry();
        grepBase.put(prefix+"/usr/include", entry);
        entry.includes.add("/sys");
        entry.includes.add("/bits");
        entry.includes.add("/gnu");
        entry.includes.add("/linux");
        entry.includes.add("/asm");
        entry.includes.add("/asm-x86_64");
        entry.includes.add("/asm-generic");
        entry = new GrepEntry();
        grepBase.put(prefix+"/usr/include/c++/4.1.2", entry);
        entry.includes.add("/bits");
        entry.includes.add("/debug");
        entry.includes.add("/ext");
        GrepEntry grep =new GrepEntry();
        grep.firstMacro="HTIOP_ACCEPTOR_IMPL_CPP";
        grep.firstMacroLine=4;
        String name = "/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/TAO/orbsvcs/orbsvcs/HTIOP/HTIOP_Acceptor_Impl.cpp";
        if (Utilities.isWindows()) {
            name = "C:\\net\\dxespb04x127x81\\export\\devarea\\osprojects\\ACE_TAO\\ACE_wrappers\\TAO\\orbsvcs\\orbsvcs\\HTIOP\\HTIOP_Acceptor_Impl.cpp";
        }
        grepBase.put(name, grep);
        if (Utilities.isWindows()) {
            name = "D:\\net\\dxespb04x127x81\\export\\devarea\\osprojects\\ACE_TAO\\ACE_wrappers\\TAO\\orbsvcs\\orbsvcs\\HTIOP\\HTIOP_Acceptor_Impl.cpp";
            grepBase.put(name, grep);
        }
        DwarfSource source = getDwarfSource("/org/netbeans/modules/cnd/dwarfdiscovery/provider/x86_64-redhat-4.1.2.o", system, ignore, grepBase, false, null);
        assertNotNull(source);
        TreeMap<String, String> map = new TreeMap<String, String>(source.getUserMacros());
        assertTrue(compareMap(map, golden));
        assertTrue(compareLists(source.getUserInludePaths(), new String[]{
        "../../../../TAO/orbsvcs/orbsvcs/HTIOP",
        "../../../../TAO/../ace",
        "../../../ace",
        "../../../../TAO/../ace/os_include/sys",
        "../../../../TAO/../ace/os_include",
        "../../../../TAO/../ace/os_include/netinet",
        prefix+"/usr/include/netinet",
        "../../../../TAO/../ace/os_include/net",
        prefix+"/usr/include/net",
        "../../../../TAO/../ace/os_include/arpa",
        prefix+"/usr/include/arpa",
        "../../../../TAO/tao",
        "../../tao",
        "../../orbsvcs/orbsvcs",
        "../../../../TAO/tao/AnyTypeCode",
        "../../../../TAO/../protocols/ace/HTBP",
        prefix+"/usr/include/rpc",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/TAO/orbsvcs/orbsvcs/HTIOP",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/ace",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/build/ace",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/ace/os_include/sys",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/ace/os_include",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/ace/os_include/netinet",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/ace/os_include/net",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/ace/os_include/arpa",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/TAO/tao",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/build/TAO/tao",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/build/TAO/orbsvcs/orbsvcs",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/TAO/tao/AnyTypeCode",
        prefix+"/net/dxespb04x127x81/export/devarea/osprojects/ACE_TAO/ACE_wrappers/protocols/ace/HTBP"
        // next dirs are detected if source file is available
        //"../../../../TAO/orbsvcs",
        //"../../../../TAO/../protocols",
        //"../../../../TAO",
        //"../../../../TAO/..",
        //"../../..",
        //"../..",
        //"../../orbsvcs"
                }));
        printInclidePaths(source);
    }

    private boolean compareMap(TreeMap<String, String> result, TreeMap<String, String> golden) {
        boolean res = true;
        for (Map.Entry<String,String> entry : result.entrySet()) {
            if (golden.containsKey(entry.getKey())){
                continue;
            }
            //printError("Redundant entry ", entry);
            System.err.println("ignore.put(\""+entry.getKey()+"\", \""+entry.getValue()+"\");");
            res = false;
        }
        for (Map.Entry<String,String> entry : golden.entrySet()) {
            if (result.containsKey(entry.getKey())){
                continue;
            }
            //printError("Not found entry ", entry);
            System.err.println("golden.put(\""+entry.getKey()+"\", \""+entry.getValue()+"\");");
            res = false;
        }
        return res;
    }

    private boolean compareLists(List<String> result, String[] golden){
        Loop:for(String x : result){
            for(String g : golden) {
                if (x.equals(g)) {
                    continue Loop;
                }
            }
            System.err.println("Result:"+x+" not found in golden");
            return false;
        }
        Loop:for(String g : golden) {
            for(String x : result){
                if (x.equals(g)) {
                    continue Loop;
                }
            }
            System.err.println("Golden:"+g+" not found in results");
            return false;
        }
        if (result.size() != golden.length) {
            System.err.println("Result size:"+result.size()+" not equals golden size:"+golden.length);
            return false;
        }
        return true;
    }

    private void printError(String message, Map.Entry<String,String> entry){
        if (true) {
            if (entry.getValue() == null) {
                System.err.println(message+" "+entry.getKey());
            } else {
                System.err.println(message+" "+entry.getKey()+"="+entry.getValue());
            }
        }
    }

    private void printInclidePaths(DwarfSource source){
        if (false) {
            System.err.println("User include paths for file "+source.getItemPath());
            for(String p : source.getUserInludePaths()){
                System.err.println("\t"+p);
            }
            System.err.println("System include paths for file "+source.getItemPath());
            for(String p : source.getSystemInludePaths()){
                System.err.println("\t"+p);
            }
        }
    }

    private DwarfSource getDwarfSource(String resource, final List<String> systemPath,
            final Map<String, String> ignore, final Map<String,GrepEntry> grepBase,
            final boolean isWindows, final String cygwinPath){
        File dataDir = getDataDir();
        String objFileName = dataDir.getAbsolutePath()+resource;
        Dwarf dump = null;
        try {
            dump = new Dwarf(objFileName);
            CompilationUnitIterator units = dump.iteratorCompilationUnits();
            if (units != null && units.hasNext()) {
                while (units.hasNext()) {
                    CompilationUnitInterface cu = units.next();
                    CompilerSettings settings = new CompilerSettings(new ProjectProxy() {
                        @Override
                        public boolean createSubProjects() { return false; }
                        @Override
                        public Project getProject() { return null; }
                        @Override
                        public String getMakefile() { return null; }
                        @Override
                        public String getSourceRoot() { return null; }
                        @Override
                        public String getExecutable() { return null; }
                        @Override
                        public String getWorkingFolder() { return null; }
                        @Override
                        public boolean mergeProjectProperties() { return false;}
                    }){
                        @Override
                        public Map<String, String> getSystemMacroDefinitions(ItemProperties.LanguageKind lang) {
                            return ignore;
                        }

                        @Override
                        public List<String> getSystemIncludePaths(ItemProperties.LanguageKind lang) {
                            return systemPath;
                        }

                        @Override
                        public CompilerFlavor getCompileFlavor() {
                            return CompilerFlavor.toFlavor("Cygwin", PlatformTypes.PLATFORM_WINDOWS);
                        }

                        @Override
                        public String getCygwinDrive() {
                            if (cygwinPath != null) {
                                return cygwinPath;
                            }
                            return super.getCygwinDrive();
                        }

                        @Override
                        public boolean isWindows() {
                            return isWindows;
                        }

                        @Override
                        protected String normalizePath(String path) {
                            //if (isWindows == Utilities.isWindows()) {
                            //    return super.normalizePath(path);
                            //}
                            path = path.replace('\\', '/');
                            while (path.indexOf("/..") > 0) {
                                int i = path.indexOf("/..");
                                String beg = path.substring(0,i);
                                String rest = path.substring(i+3);
                                if (beg.endsWith(".")) {
                                    break;
                                }
                                int j = beg.lastIndexOf('/');
                                if (j < 0) {
                                    break;
                                }
                                path = beg.substring(0,j)+rest;
                            }
                            return path;
                        }

                    };
                    DwarfSource source = new DwarfSource(cu, ItemProperties.LanguageKind.C, ItemProperties.LanguageStandard.C, settings, grepBase, null);
                    source.process(cu);
                    return source;
                }
            }
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (WrongFileFormatException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (dump != null) {
                dump.dispose();
            }
        }
        return null;
    }
}