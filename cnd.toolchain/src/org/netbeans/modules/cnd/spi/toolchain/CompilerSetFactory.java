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

package org.netbeans.modules.cnd.spi.toolchain;

import org.netbeans.modules.cnd.toolchain.compilerset.CompilerSetImpl;
import org.netbeans.modules.cnd.toolchain.compilerset.CompilerFlavorImpl;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.cnd.api.toolchain.CompilerFlavor;
import org.netbeans.modules.cnd.api.toolchain.CompilerSet;
import org.netbeans.modules.cnd.api.toolchain.CompilerSetManager;
import org.netbeans.modules.cnd.api.toolchain.ToolchainManager.ToolchainDescriptor;
import org.netbeans.modules.cnd.toolchain.compilerset.CompilerSetManagerImpl;
import org.netbeans.modules.cnd.toolchain.compilerset.ToolUtils;
import org.netbeans.modules.cnd.toolchain.compilerset.ToolchainManagerImpl;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;

/**
 *
 * @author Alexander Simon
 */
public final class CompilerSetFactory {

    private CompilerSetFactory() {
    }

    /**
     * Get an existing compiler set. If it doesn't exist, get an empty one based on the requested name.
     *
     * @param name The name of the compiler set we want
     * @returns The best fitting compiler set (may be an empty CompilerSet)
     */
    public static CompilerSet getCompilerSet(ExecutionEnvironment env, CompilerFlavor flavor, String name) {
        final CompilerSetManagerImpl csm = (CompilerSetManagerImpl) CompilerSetManagerImpl.get(env);
        CompilerSet cs = getCompilerSet(csm, flavor, name);
        if (cs == null) {
            cs = new CompilerSetImpl(flavor, "", name); // NOI18N
            ((CompilerSetImpl)cs).setAutoGenerated(false);
            csm.add(cs);
        }
        return cs;
    }

    public static List<CompilerFlavor> getCompilerSetFlavor(String directory, int platform) {
        List<CompilerFlavor> list = new ArrayList<CompilerFlavor>();
        for(ToolchainDescriptor d : ToolchainManagerImpl.getImpl().getToolchains(platform)) {
            if (d.isAbstract()) {
                continue;
            }
            if (ToolUtils.isMyFolder(directory, d, platform, false)){
                CompilerFlavor f = CompilerFlavorImpl.toFlavor(d.getName(), platform);
                if (f != null) {
                    list.add(f);
                }
            }
        }
        return list;
    }

    public static CompilerSet getCustomCompilerSet(String directory, CompilerFlavor flavor, String name) {
        CompilerSetImpl cs = new CompilerSetImpl(flavor, directory, name);
        cs.setAutoGenerated(false);
        return cs;
    }

    private static CompilerSet getCompilerSet(CompilerSetManager csm, CompilerFlavor flavor, String name) {
        CompilerSet flavorCandidate = null;
        CompilerSet nameCandidate = null;
        for (CompilerSet cs : csm.getCompilerSets()) {
            if (cs.getCompilerFlavor().equals(flavor)) {
                if (cs.getName().equals(name)) {
                    return cs;
                }
                if (flavorCandidate == null) {
                    flavorCandidate = cs;
                }
            } else if (cs.getName().equals(name)) {
                if (nameCandidate == null) {
                    nameCandidate = cs;
                }
            }
        }
        if (flavorCandidate != null) {
            return flavorCandidate;
        }
        if (nameCandidate != null) {
            return nameCandidate;
        }
        return null;
    }


}