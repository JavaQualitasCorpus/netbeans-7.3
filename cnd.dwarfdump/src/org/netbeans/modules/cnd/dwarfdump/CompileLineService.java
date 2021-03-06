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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.dwarfdump;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.netbeans.modules.cnd.dwarfdump.dwarfconsts.LANG;
import org.netbeans.modules.cnd.dwarfdump.exception.WrongFileFormatException;

/**
 * Light weight service that gets command line from binary file in case Sun Studio compiler
 *
 * @author Alexander Simon
 */
public class CompileLineService {

    private CompileLineService() {
    }

    public static void main(String[] args){
        if (args.length < 2) {
            System.err.println("Not enough parameters."); // NOI18N
            System.err.println("Usage:"); // NOI18N
            System.err.println("java -cp org-netbeans-modules-cnd-dwarfdump.jar org.netbeans.modules.cnd.dwarfdump.CompileLineService -file binaryFileName"); // NOI18N
            System.err.println("java -cp org-netbeans-modules-cnd-dwarfdump.jar org.netbeans.modules.cnd.dwarfdump.CompileLineService -folder folderName"); // NOI18N
            return;
        }
        try {
            dump(args[0], args[1], System.out);
        } catch (Throwable ex) {
            Dwarf.LOG.log(Level.INFO, "File "+args[1], ex);
        }
    }

    private static void dump(String kind, String objFileName, PrintStream out) throws IOException, Exception {
        List<SourceFile> res = null;
        if ("-file".equals(kind)){ // NOI18N
            res = getSourceFileProperties(objFileName);
        } else if ("-folder".equals(kind)){ // NOI18N
            res = getSourceFolderProperties(objFileName);
        } else {
            throw new Exception("Wrong arguments: "+kind+" "+objFileName); // NOI18N
        }
        for(SourceFile entry : res) {
            out.println(entry.compileDir);
            out.println(entry.sourceFile);
            out.println(entry.compileLine);
        }
    }

    public static List<SourceFile> getSourceProperties(BufferedReader out) throws IOException {
        return readSourceProperties(out);
    }

    private static List<SourceFile> readSourceProperties(BufferedReader out) throws IOException {
        List<SourceFile> list = new ArrayList<SourceFile>();
        String line;
        String compileDir = null;
        String sourceFile = null;
        int i = 0;
        while ((line=out.readLine())!= null){
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            switch (i%3) {
                case 0:
                    compileDir = line;
                    break;
                case 1:
                    sourceFile = line;
                    break;
                case 2:
                    list.add(createSourceFile(compileDir, sourceFile, line));
                    break;
            }
            i++;
        }
        return list;
    }

    // valid on Solaris or Linux
    public static List<SourceFile> getSourceFolderProperties(String objFolderName) {
        List<SourceFile> list = new ArrayList<SourceFile>();
        for(String objFileName : getObjectFiles(objFolderName)) {
            list.addAll(getSourceFileProperties(objFileName));
        }
        return list;
    }

    public static List<SourceFile> getSourceFileProperties(String objFileName) {
        List<SourceFile> list = new ArrayList<SourceFile>();
        Dwarf dump = null;
        try {
            dump = new Dwarf(objFileName);
            Dwarf.CompilationUnitIterator iterator = dump.iteratorCompilationUnits();
            while (iterator.hasNext()) {
                CompilationUnitInterface cu = iterator.next();
                if (cu != null) {
                    if (cu.getRoot() == null || cu.getSourceFileName() == null) {
                        if (Dwarf.LOG.isLoggable(Level.FINE)) {
                            Dwarf.LOG.log(Level.FINE, "Compilation unit has broken name in file {0}", objFileName);  // NOI18N
                        }
                        continue;
                    }
                    String lang = cu.getSourceLanguage();
                    if (lang == null) {
                        if (Dwarf.LOG.isLoggable(Level.FINE)) {
                            Dwarf.LOG.log(Level.FINE, "Compilation unit has unresolved language in file {0}for {1}", new Object[]{objFileName, cu.getSourceFileName()});  // NOI18N
                        }
                        continue;
                    }
                    if (LANG.DW_LANG_C.toString().equals(lang)
                            || LANG.DW_LANG_C89.toString().equals(lang)
                            || LANG.DW_LANG_C99.toString().equals(lang)
                            || LANG.DW_LANG_C_plus_plus.toString().equals(lang)) {
                        try {
                            list.add(createSourceFile(cu));
                        } catch (IOException ex){
                            throw ex;
                        } catch (Exception ex){
                            if (Dwarf.LOG.isLoggable(Level.FINE)) {
                                Dwarf.LOG.log(Level.FINE, "Compilation unit {0} {1}", new Object[]{cu.getSourceFileName(), ex.getMessage()});  // NOI18N
                            }
                            continue;
                        }
                    } else {
                        if (Dwarf.LOG.isLoggable(Level.FINE)) {
                            Dwarf.LOG.log(Level.FINE, "Unknown language: {0}", lang);  // NOI18N
                        }
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            // Skip Exception
            if (Dwarf.LOG.isLoggable(Level.FINE)) {
                Dwarf.LOG.log(Level.FINE, "File not found {0}: {1}", new Object[]{objFileName, ex.getMessage()});  // NOI18N
            }
        } catch (WrongFileFormatException ex) {
            if (Dwarf.LOG.isLoggable(Level.FINE)) {
                Dwarf.LOG.log(Level.FINE, "Unsuported format of file {0}: {1}", new Object[]{objFileName, ex.getMessage()});  // NOI18N
            }
        } catch (IOException ex) {
            Dwarf.LOG.log(Level.INFO, "Exception in file " + objFileName, ex);  // NOI18N
        } catch (Exception ex) {
            Dwarf.LOG.log(Level.INFO, "Exception in file " + objFileName, ex);  // NOI18N
        } finally {
            if (dump != null) {
                dump.dispose();
            }
        }
        return list;
    }

    public static SourceFile createSourceFile(String compileDir, String sourceFile, String compileLine) {
        return new SourceFile(compileDir, sourceFile, compileLine);
    }

    public static SourceFile createSourceFile(CompilationUnitInterface cu) throws IOException, Exception {
        return new SourceFile(cu);
    }

    public static final class SourceFile {

        private final String compileLine;
        private final String compileDir;
        private final String sourceFile;
        private Map<String,String> userMacros;
        private List<String> userPaths;

        private SourceFile(CompilationUnitInterface cu) throws IOException, Exception {
            String s = cu.getCommandLine();
            if (s == null) {
                // client may be interested in compilation units also
                s = "";  // NOI18N
                //throw new Exception("Dwarf information does not contain compile line");  // NOI18N
            } 
            compileLine = s.trim();
            compileDir = cu.getCompilationDir();
            if (compileDir == null) {
                throw new Exception("Dwarf information does not contain compile dir");  // NOI18N
            }
            sourceFile = cu.getSourceFileName();
            if (sourceFile == null) {
                throw new Exception("Dwarf information does not contain source file name");  // NOI18N
            }
        }

        private SourceFile( String compileDir, String sourceFile, String compileLine) {
            this.compileLine = compileLine;
            this.compileDir = compileDir;
            this.sourceFile = sourceFile;
        }

        public final String getCompileDir() {
            return compileDir;
        }

        public final String getSource() {
            return sourceFile;
        }

        public final String getCompileLine() {
            return compileLine;
        }

        public final Map<String,String> getUserMacros() {
            if (userMacros == null) {
                initMacrosAndPaths();
            }
            return userMacros;
        }

        public final List<String> getUserPaths() {
            if (userPaths == null) {
                initMacrosAndPaths();
            }
            return userPaths;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SourceFile other = (SourceFile) obj;
            if ((this.compileLine == null) ? (other.compileLine != null) : !this.compileLine.equals(other.compileLine)) {
                return false;
            }
            if ((this.compileDir == null) ? (other.compileDir != null) : !this.compileDir.equals(other.compileDir)) {
                return false;
            }
            if ((this.sourceFile == null) ? (other.sourceFile != null) : !this.sourceFile.equals(other.sourceFile)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + (this.compileLine != null ? this.compileLine.hashCode() : 0);
            hash = 97 * hash + (this.compileDir != null ? this.compileDir.hashCode() : 0);
            hash = 97 * hash + (this.sourceFile != null ? this.sourceFile.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return "SourceFile{" + "compileLine=" + compileLine + ", compileDir=" + compileDir + ", sourceFile=" + sourceFile + '}'; // NOI18N
        }
        
        
        private void initMacrosAndPaths(){
            userPaths = new ArrayList<String>();
            userMacros = new LinkedHashMap<String, String>();
            Iterator<String> st = splitCommandLine(compileLine).iterator();
            while(st.hasNext()){
                String option = st.next();
                if (option.startsWith("-D")){ // NOI18N
                    String macro = option.substring(2);
                    int i = macro.indexOf('=');
                    if (i>0){
                        String value = macro.substring(i+1).trim();
                        if (value.length() >= 2 &&
                           (value.charAt(0) == '\'' && value.charAt(value.length()-1) == '\'' || // NOI18N
                            value.charAt(0) == '"' && value.charAt(value.length()-1) == '"' )) { // NOI18N
                            value = value.substring(1,value.length()-1);
                        }
                        userMacros.put(macro.substring(0,i), value);
                    } else {
                        userMacros.put(macro, null);
                    }
                } else if (option.startsWith("-I")){ // NOI18N
                    String path = option.substring(2);
                    if (path.length()==0 && st.hasNext()){
                        path = st.next();
                    }
                    userPaths.add(path);
                } else if (option.startsWith("-Y")){ // NOI18N
                    String defaultSearchPath = option.substring(2);
                    if (defaultSearchPath.length()==0 && st.hasNext()){
                        defaultSearchPath = st.next();
                    }
                    if (defaultSearchPath.startsWith("I,")){ // NOI18N
                        defaultSearchPath = defaultSearchPath.substring(2);
                        userPaths.add(defaultSearchPath);
                    }
                } else if (option.startsWith("-isystem")){ // NOI18N
                    String path = option.substring(8);
                    if (path.length()==0 && st.hasNext()){
                        path = st.next();
                    }
                    userPaths.add(path);
                } else if (option.startsWith("-include")){ // NOI18N
                    String path = option.substring(8);
                    if (path.length()==0 && st.hasNext()){
                        path = st.next();
                    }
                    userPaths.add(path);
                } else if (option.startsWith("-imacros")){ // NOI18N
                    String path = option.substring(8);
                    if (path.length()==0 && st.hasNext()){
                        path = st.next();
                    }
                    userPaths.add(path);
                }
            }
        }

        private List<String> splitCommandLine(String line) {
            List<String> res = new ArrayList<String>();
            int i = 0;
            StringBuilder current = new StringBuilder();
            boolean isSingleQuoteMode = false;
            boolean isDoubleQuoteMode = false;
            while (i < line.length()) {
                char c = line.charAt(i);
                i++;
                switch (c) {
                    case '\'': // NOI18N
                        if (isSingleQuoteMode) {
                            isSingleQuoteMode = false;
                        } else if (!isDoubleQuoteMode) {
                            isSingleQuoteMode = true;
                        }
                        current.append(c);
                        break;
                    case '\"': // NOI18N
                        if (isDoubleQuoteMode) {
                            isDoubleQuoteMode = false;
                        } else if (!isSingleQuoteMode) {
                            isDoubleQuoteMode = true;
                        }
                        current.append(c);
                        break;
                    case ' ': // NOI18N
                    case '\t': // NOI18N
                    case '\n': // NOI18N
                    case '\r': // NOI18N
                        if (isSingleQuoteMode || isDoubleQuoteMode) {
                            current.append(c);
                            break;
                        } else {
                            if (current.length() > 0) {
                                res.add(current.toString());
                                current.setLength(0);
                            }
                        }
                        break;
                    default:
                        current.append(c);
                        break;
                }
            }
            if (current.length() > 0) {
                res.add(current.toString());
            }
            return res;
        }
    }

    private static Set<String> getObjectFiles(String root){
        HashSet<String> set = new HashSet<String>();
        gatherSubFolders(new File(root), set, new HashSet<String>());
        HashSet<String> map = new HashSet<String>();
        for (Iterator<String> it = set.iterator(); it.hasNext();){
            File d = new File(it.next());
            if (d.exists() && d.isDirectory() && d.canRead()){
                File[] ff = d.listFiles();
                if (ff != null) {
                    for (int i = 0; i < ff.length; i++) {
                        if (ff[i].isFile()) {
                            String name = ff[i].getName();
                            if (name.endsWith(".o") ||  // NOI18N
                                name.endsWith(".so") || // NOI18N
                                name.endsWith(".a") ||  // NOI18N
                                isExecutable(ff[i])){
                                String path = ff[i].getAbsolutePath();
                                map.add(path);
                            }
                        }
                    }
                }
            }
        }
        return map;
    }

    private static boolean isExecutable(File file){
        String name = file.getName();
        return name.indexOf('.') < 0;
    }

    private static void gatherSubFolders(File d, HashSet<String> set, HashSet<String> antiLoop){
        if (d.exists() && d.isDirectory() && d.canRead()){
            if (ignoreFolder(d)){
                return;
            }
            String canPath;
            try {
                canPath = d.getCanonicalPath();
            } catch (IOException ex) {
                Dwarf.LOG.log(Level.INFO, "File "+d.getAbsolutePath(), ex);
                return;
            }
            if (!antiLoop.contains(canPath)){
                antiLoop.add(canPath);
                set.add(d.getAbsolutePath());
                File[] ff = d.listFiles();
                if (ff != null) {
                    for (int i = 0; i < ff.length; i++) {
                        if (ff[i].isDirectory()) {
                            gatherSubFolders(ff[i], set, antiLoop);
                        }
                    }
                }
            }
        }
    }

    private static boolean ignoreFolder(File file){
        if (file.isDirectory()) {
            String name = file.getName();
            return name.equals("SCCS") || name.equals("CVS") || name.equals(".hg") || name.equals("SunWS_cache") || name.equals(".svn"); // NOI18N
        }
        return false;
    }

}
