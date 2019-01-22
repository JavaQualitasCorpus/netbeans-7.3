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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.toolchain.compilerset;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.cnd.api.toolchain.Tool;
import org.netbeans.modules.cnd.api.toolchain.CompilerFlavor;
import org.netbeans.modules.cnd.spi.toolchain.CompilerProvider;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.ToolKind;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.AlternativePath;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.BaseFolder;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.CMakeDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.CompilerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.DebuggerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.LinkerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.MakeDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.PredefinedMacro;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.QMakeDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.ScannerDescriptor;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.ScannerPattern;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.ToolchainDescriptor;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.openide.util.NbBundle;

/**
 * A container for information about a set of related compilers, typically from a vendor or
 * re-distributor.
 */
public final class CompilerSetImpl extends CompilerSet {

    @Override
    public boolean isAutoGenerated() {
        return autoGenerated;
    }

    public void setAutoGenerated(boolean autoGenerated) {
        this.autoGenerated = autoGenerated;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isUrlPointer(){
        if (getDirectory() == null || getDirectory().length() == 0){
            return flavor.getToolchainDescriptor().getUpdateCenterUrl() != null && flavor.getToolchainDescriptor().getModuleID() != null;
        }
        return false;
    }

    void setAsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void unsetDefault() {
        this.isDefault = false;  // to set to true use CompilerSetManager.setDefault()
    }

    private final CompilerFlavor flavor;
    private String name;
    private final String displayName;
    private boolean autoGenerated;
    private boolean isDefault;
    private final String directory;
    private final ArrayList<Tool> tools = new ArrayList<Tool>();
    private CompilerProvider compilerProvider;
    private Map<ToolKind,String> pathSearch;
    private boolean isSunStudioDefault;
    private Charset charset;

    /** Creates a new instance of CompilerSet */
    public CompilerSetImpl(CompilerFlavor flavor, String directory, String name) {
        this.directory = directory == null ? "" : directory; // NOI18N

        compilerProvider = CompilerProvider.getInstance();

        if (name != null && !name.isEmpty()) {
            this.name = name;
        } else {
            this.name = flavor.toString();
        }
        //displayName = mapNameToDisplayName(flavor);
        displayName = flavor.getToolchainDescriptor().getDisplayName();
        this.flavor = flavor;
        this.autoGenerated = true;
        this.isDefault = false;
    }

    public CompilerSetImpl(int platform) {
        this.directory = ""; // NOI18N
        this.name = None;
        this.flavor = CompilerFlavorImpl.getUnknown(platform);
        this.displayName = NbBundle.getMessage(CompilerSetImpl.class, "LBL_EmptyCompilerSetDisplayName"); // NOI18N

        compilerProvider = CompilerProvider.getInstance();
        this.autoGenerated = true;
        this.isDefault = false;
    }

    public CompilerSetImpl createCopy() {
        return createCopy(this.flavor, getDirectory(), name, isAutoGenerated(), charset, true);
    }

    /**
     * if null is passed as param value => "this" object value is used instead
     * @param flavor
     * @param directory
     * @param name
     * @param autoGenerated
     * @param def
     * @return
     */
    public CompilerSetImpl createCopy(CompilerFlavor flavor, String directory, String name, Boolean autoGenerated, Charset charset, boolean keepToolFlavor) {
        flavor = (flavor == null) ? this.flavor : flavor;
        directory = (directory == null) ? getDirectory() : directory;
        name = (name == null) ? this.name : name;
        CompilerSetImpl copy = new CompilerSetImpl(flavor, directory, name);
        autoGenerated = (autoGenerated == null) ? isAutoGenerated() : autoGenerated;
        copy.setAutoGenerated(autoGenerated);
        copy.setEncoding(charset);

        for (Tool tool : getTools()) {
            CompilerFlavor toolFlavor;
            if (keepToolFlavor) {
                toolFlavor = tool.getFlavor();
            } else {
                toolFlavor = flavor;
            }
            copy.addTool(tool.createCopy(toolFlavor));
        }

        return copy;
    }

    /**
     * If no compilers are found an empty compiler set is created so we don't have an empty list.
     * Too many places in CND expect a non-empty list and throw NPEs if it is empty!
     */
    protected static CompilerSet createEmptyCompilerSet(int platform) {
        return new CompilerSetImpl(platform);
    }

    @Override
    public CompilerFlavor getCompilerFlavor() {
        return flavor;
    }

    @Override
    public String getDirectory() {
        return directory;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayName() {
        // TODO: this thing is never used although it's being set to informative values by personality
        return displayName;
    }

    /*package-local*/ Tool addTool(ExecutionEnvironment env, String name, String path, ToolKind kind, CompilerFlavor aFlavor) {
        if (findTool(kind) != null) {
            return null;
        }
        if (aFlavor == null) {
            aFlavor = getCompilerFlavor();
        }
        Tool tool = compilerProvider.createCompiler(env, aFlavor, kind, name, kind.getDisplayName(), path);
        if (!tools.contains(tool)) {
            tools.add(tool);
        }
        APIAccessor.get().setCompilerSet(tool, this);
        return tool;
    }

    /*package-local*/ void addTool(Tool tool) {
        tools.add(tool);
        APIAccessor.get().setCompilerSet(tool, this);
    }

    /*package-local*/ Tool addNewTool(ExecutionEnvironment env, String name, String path, ToolKind kind, CompilerFlavor aFlavor) {
        if (aFlavor == null) {
            aFlavor = getCompilerFlavor();
        }
        Tool tool = compilerProvider.createCompiler(env, aFlavor, kind, name, kind.getDisplayName(), path);
        tools.add(tool);
        APIAccessor.get().setCompilerSet(tool, this);
        return tool;
    }

    /**
     * Get the first tool of its kind.
     *
     * @param kind The type of tool to get
     * @return The Tool or null
     */
    @Override
    public Tool getTool(ToolKind kind) {
        for (Tool tool : tools) {
            if (tool.getKind() == kind) {
                return tool;
            }
        }
        CndUtils.assertFalse(true, "Should not be here, cuz we should create empty tools in CompilerSetManager"); //NOI18N
        //TODO: remove this code, empty tools should be created in CompilerSetManager
        Tool t;
        // Fixup: all tools should go here ....
        t = compilerProvider.createCompiler(ExecutionEnvironmentFactory.getLocal(),
                getCompilerFlavor(), kind, "", kind.getDisplayName(), ""); // NOI18N
        APIAccessor.get().setCompilerSet(t, this);
        synchronized( tools ) { // synchronize this only unpredictable tools modification
            tools.add(t);
        }
        return t;
    }


    /**
     * Get the first tool of its kind.
     *
     * @param kind The type of tool to get
     * @return The Tool or null
     */
    @Override
    public Tool findTool(ToolKind kind) {
        for (Tool tool : tools) {
            if (tool.getKind() == kind) {
                return tool;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tool> getTools() {
        synchronized (tools) {
            return (List<Tool>)tools.clone();
        }
    }

    /*package-local*/ void addPathCandidate(ToolKind tool, String path) {
        if (pathSearch == null){
            pathSearch = new HashMap<ToolKind, String>();
        }
        pathSearch.put(tool, path);
    }

    /*package-local*/String getPathCandidate(ToolKind tool){
        if (pathSearch == null){
            return null;
        }
        return pathSearch.get(tool);
    }

    /*package-local*/void setSunStudioDefault(boolean isSunStudioDefault){
        this.isSunStudioDefault = isSunStudioDefault;
    }

    /*package-local*/boolean isSunStudioDefault(){
        return isSunStudioDefault;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Charset getEncoding() {
        if (charset == null) {
            if (flavor != null && flavor.isSunStudioCompiler()) {
                charset = Charset.defaultCharset();
            }
            if (charset == null) {
                charset = Charset.forName("UTF-8"); //NOI18N
                if (charset == null) {
                    charset = Charset.defaultCharset();
                }
            }
        }
        return charset;
    }

    public void setEncoding(Charset charset) {
        this.charset = charset;
    }
    
    private static class UnknownCompilerDescriptor implements CompilerDescriptor {

        @Override
        public String getPathPattern() {
            return "";//NOI18N
        }

        @Override
        public String getExistFolder() {
            return "";//NOI18N
        }

        @Override
        public String getIncludeFlags() {
            return "";//NOI18N
        }

        @Override
        public String getUserIncludeFlag() {
            return "";//NOI18N
        }

        @Override
        public String getIncludeParser() {
            return "";//NOI18N
        }

        @Override
        public String getRemoveIncludePathPrefix() {
            return "";//NOI18N
        }

        @Override
        public String getRemoveIncludeOutputPrefix() {
            return "";//NOI18N
        }

        @Override
        public String getMacroFlags() {
            return "";//NOI18N
        }

        @Override
        public String getMacroParser() {
            return "";//NOI18N
        }

        @Override
        public List<PredefinedMacro> getPredefinedMacros() {
            return Collections.emptyList();
        }

        @Override
        public String getUserMacroFlag() {
            return "";//NOI18N
        }

        @Override
        public String[] getDevelopmentModeFlags() {
            return new String[0];
        }

        @Override
        public String[] getWarningLevelFlags() {
            return new String[0];
        }

        @Override
        public String[] getArchitectureFlags() {
            return new String[0];
        }

        @Override
        public String getStripFlag() {
            return "";//NOI18N
        }

        @Override
        public String[] getMultithreadingFlags() {
            return new String[0];
        }

        @Override
        public String[] getStandardFlags() {
            return new String[0];
        }

        @Override
        public String[] getLanguageExtensionFlags() {
            return new String[0];
        }

        @Override
        public String[] getCppStandardFlags() {
            return new String[0];
        }

        @Override
        public String[] getCStandardFlags() {
            return new String[0];
        }

        @Override
        public String[] getLibraryFlags() {
            return new String[0];
        }

        @Override
        public String getOutputObjectFileFlags() {
            return "";//NOI18N
        }

        @Override
        public String getDependencyGenerationFlags() {
            return "";//NOI18N
        }

        @Override
        public String getPrecompiledHeaderFlags() {
            return "";//NOI18N
        }

        @Override
        public String getPrecompiledHeaderSuffix() {
            return "";//NOI18N
        }

        @Override
        public boolean getPrecompiledHeaderSuffixAppend() {
            return false;
        }

        @Override
        public String[] getNames() {
            return new String[0];
        }

        @Override
        public String getVersionFlags() {
            return "";//NOI18N
        }

        @Override
        public String getVersionPattern() {
            return "";//NOI18N
        }

        @Override
        public String getFingerPrintFlags() {
            return "";//NOI18N
        }

        @Override
        public String getFingerPrintPattern() {
            return "";//NOI18N
        }

        @Override
        public boolean skipSearch() {
            return true;
        }

        @Override
        public AlternativePath[] getAlternativePath() {
            return new AlternativePath[0];
        }

    }

    public static class UnknownToolchainDescriptor implements ToolchainDescriptor {
        CompilerDescriptor unknowDescriptor = new UnknownCompilerDescriptor();

        @Override
        public String getFileName() {
            return ""; // NOI18N
        }

        @Override
        public String getName() {
            return ""; // NOI18N
        }

        @Override
        public String getDisplayName() {
            return ""; // NOI18N
        }

        @Override
        public String[] getFamily() {
            return new String[]{};
        }

        @Override
        public String[] getPlatforms() {
            return new String[]{};
        }

        @Override
        public String getUpdateCenterUrl() {
            return null;
        }

        @Override
        public String getUpdateCenterDisplayName() {
            return null;
        }

        @Override
        public String getUpgradeUrl() {
            return null;
        }

        @Override
        public String getModuleID() {
            return null;
        }

        @Override
        public boolean isAbstract() {
            return true;
        }

        @Override
        public boolean isAutoDetected() {
            return true;
        }

        @Override
        public String[] getAliases() {
            return new String[]{};
        }

        @Override
        public String getSubsitute() {
            return null;
        }

        @Override
        public String getDriveLetterPrefix() {
            return "/"; // NOI18N
        }

        @Override
        public List<BaseFolder> getBaseFolders() {
            return Collections.<BaseFolder>emptyList();
        }

        @Override
        public List<BaseFolder> getCommandFolders() {
            return Collections.<BaseFolder>emptyList();
        }

        @Override
        public String getQmakeSpec() {
            return ""; // NOI18N
        }

        @Override
        public CompilerDescriptor getC() {
            return unknowDescriptor;
        }

        @Override
        public CompilerDescriptor getCpp() {
            return unknowDescriptor;
        }

        @Override
        public CompilerDescriptor getFortran() {
            return unknowDescriptor;
        }

        @Override
        public CompilerDescriptor getAssembler() {
            return unknowDescriptor;
        }

        @Override
        public ScannerDescriptor getScanner() {
            return new ScannerDescriptor() {

                @Override
                public String getID() {
                    return ""; // NOI18N
                }

                @Override
                public List<ScannerPattern> getPatterns() {
                    return Collections.emptyList();
                }

                @Override
                public String getChangeDirectoryPattern() {
                    return ""; // NOI18N
                }

                @Override
                public String getEnterDirectoryPattern() {
                    return ""; // NOI18N
                }

                @Override
                public String getLeaveDirectoryPattern() {
                    return ""; // NOI18N
                }

                @Override
                public String getMakeAllInDirectoryPattern() {
                    return ""; // NOI18N
                }

                @Override
                public String getStackHeaderPattern() {
                    return ""; // NOI18N
                }

                @Override
                public String getStackNextPattern() {
                    return ""; // NOI18N
                }

                @Override
                public List<String> getFilterOutPatterns() {
                    return Collections.emptyList();
                }
            };
        }

        @Override
        public LinkerDescriptor getLinker() {
            return new LinkerDescriptor(){

                @Override
                public String getLibraryPrefix() {
                    return ""; // NOI18N
                }

                @Override
                public String getLibrarySearchFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getDynamicLibrarySearchFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getLibraryFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getPICFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getStaticLibraryFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getDynamicLibraryFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getDynamicLibraryBasicFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getOutputFileFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getStripFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getPreferredCompiler() {
                    return ""; // NOI18N
                }
            };
        }

        @Override
        public MakeDescriptor getMake() {
            return null;            
        }

        @Override
        public Map<String, List<String>> getDefaultLocations() {
            return Collections.<String, List<String>>emptyMap();
        }

        @Override
        public DebuggerDescriptor getDebugger() {
            return null;
        }

        @Override
        public String getMakefileWriter() {
            return null;
        }

        @Override
        public QMakeDescriptor getQMake() {
            return new QMakeDescriptor() {

                @Override
                public String[] getNames() {
                    return new String[0];
                }

                @Override
                public String getVersionFlags() {
                    return ""; //NOI18N
                }

                @Override
                public String getVersionPattern() {
                    return ""; //NOI18N
                }
                
                @Override
                public String getFingerPrintFlags() {
                    return ""; //NOI18N
                }

                @Override
                public String getFingerPrintPattern() {
                    return ""; //NOI18N
                }

                @Override
                public boolean skipSearch() {
                    return true;
                }

                @Override
                public AlternativePath[] getAlternativePath() {
                    return new AlternativePath[0];
                }

            };
        }

        @Override
        public CMakeDescriptor getCMake() {
            return new CMakeDescriptor() {

                @Override
                public String[] getNames() {
                    return new String[0];
                }

                @Override
                public String getVersionFlags() {
                    return ""; //NOI18N
                }

                @Override
                public String getVersionPattern() {
                    return ""; //NOI18N
                }

                @Override
                public String getFingerPrintFlags() {
                    return ""; //NOI18N
                }

                @Override
                public String getFingerPrintPattern() {
                    return ""; //NOI18N
                }

                @Override
                public boolean skipSearch() {
                    return true;
                }

                @Override
                public AlternativePath[] getAlternativePath() {
                    return new AlternativePath[0];
                }
            };
        }
    }
}