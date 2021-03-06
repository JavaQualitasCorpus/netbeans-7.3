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

package org.netbeans.modules.cnd.modelimpl.impl.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.netbeans.modules.cnd.api.model.CsmClass;
import org.netbeans.modules.cnd.api.model.CsmDeclaration;
import org.netbeans.modules.cnd.api.model.CsmDeclaration.Kind;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmFunction;
import org.netbeans.modules.cnd.api.model.CsmInclude;
import org.netbeans.modules.cnd.api.model.CsmMacro;
import org.netbeans.modules.cnd.api.model.CsmMember;
import org.netbeans.modules.cnd.api.model.CsmNamespace;
import org.netbeans.modules.cnd.api.model.CsmNamespaceDefinition;
import org.netbeans.modules.cnd.api.model.CsmOffsetableDeclaration;
import org.netbeans.modules.cnd.api.model.CsmProject;
import org.netbeans.modules.cnd.api.model.CsmUID;
import org.netbeans.modules.cnd.api.model.CsmVariable;
import org.netbeans.modules.cnd.api.model.services.CsmSelect.CsmFilter;
import org.netbeans.modules.cnd.api.model.services.CsmSelect.CsmFilterBuilder;
import org.netbeans.modules.cnd.api.model.services.CsmSelect.NameAcceptor;
import org.netbeans.modules.cnd.api.model.util.CsmSortUtilities;
import org.netbeans.modules.cnd.modelimpl.csm.NamespaceDefinitionImpl;
import org.netbeans.modules.cnd.modelimpl.csm.NamespaceImpl;
import org.netbeans.modules.cnd.modelimpl.csm.core.FileImpl;
import org.netbeans.modules.cnd.modelimpl.csm.core.OffsetableDeclarationBase;
import org.netbeans.modules.cnd.modelimpl.csm.core.ProjectBase;
import org.netbeans.modules.cnd.modelimpl.csm.core.Utils;
import org.netbeans.modules.cnd.modelimpl.uid.LazyCsmCollection;
import org.netbeans.modules.cnd.modelimpl.uid.UIDCsmConverter;
import org.netbeans.modules.cnd.modelimpl.uid.UIDUtilities;
import org.netbeans.modules.cnd.spi.model.services.CsmSelectProvider;

/**
 *
 * @author Alexander Simon
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.cnd.spi.model.services.CsmSelectProvider.class)
public class SelectImpl implements CsmSelectProvider {
    private static final FilterBuilder builder = new FilterBuilder();

    @Override
    public CsmFilterBuilder getFilterBuilder() {
        return builder;
    }

    @Override
    public Iterator<CsmMacro> getMacros(CsmFile file, CsmFilter filter) {
        if (file instanceof FileImpl){
            Iterator<CsmMacro> res = analyzeFilter((FileImpl)file, filter);
            if (res != null) {
                return res;
            }
            return ((FileImpl)file).getMacros(filter);
        }
        return file.getMacros().iterator();
    }

    private Iterator<CsmMacro> analyzeFilter(FileImpl file, CsmFilter filter){
        if (filter instanceof FilterBuilder.NameFilterImpl) {
            FilterBuilder.NameFilterImpl implName = (FilterBuilder.NameFilterImpl) filter;
            if (implName.caseSensitive && implName.match && !implName.allowEmptyName) {
                // can be optimized
                Collection<CsmUID<CsmMacro>>res = file.findMacroUids(implName.strPrefix);
                return new LazyCsmCollection<CsmMacro,CsmMacro>(res, true).iterator(filter);
            }
        }
        return null;
    }
    
    @Override
    public Iterator<CsmInclude> getIncludes(CsmFile file, CsmFilter filter) {
        if (file instanceof FileImpl){
            return ((FileImpl)file).getIncludes(filter);
        }
        return file.getIncludes().iterator();
    }


    @Override
    public Iterator<CsmOffsetableDeclaration> getDeclarations(CsmNamespace namespace, CsmFilter filter) {
        if (namespace instanceof NamespaceImpl){
            Iterator<CsmOffsetableDeclaration> res = analyzeFilter((NamespaceImpl)namespace, filter);
            if (res != null) {
                return res;
            }
            return ((NamespaceImpl)namespace).getDeclarations(filter);
        }
        return namespace.getDeclarations().iterator();
    }

    private Iterator<CsmOffsetableDeclaration> analyzeFilter(NamespaceImpl namespace, CsmFilter filter){
        if (!namespace.isGlobal() && namespace.getName().length() == 0) {
            return null;
        }
        FilterBuilder.NameFilterImpl implName = null;
        FilterBuilder.KindFilterImpl implKind = null;
        if (filter instanceof FilterBuilder.CompoundFilterImpl) {
            FilterBuilder.CompoundFilterImpl implCompound = (FilterBuilder.CompoundFilterImpl) filter;
            if ((implCompound.first instanceof FilterBuilder.KindFilterImpl) &&
                (implCompound.second instanceof FilterBuilder.NameFilterImpl)) {
                // optimization by unique name
                implName = (FilterBuilder.NameFilterImpl) implCompound.second;
                implKind = (FilterBuilder.KindFilterImpl) implCompound.first;
            } else if ((implCompound.first instanceof FilterBuilder.NameFilterImpl) &&
                       (implCompound.second instanceof FilterBuilder.KindFilterImpl)) {
                // optimization by unique name
                implName = (FilterBuilder.NameFilterImpl) implCompound.first;
                implKind = (FilterBuilder.KindFilterImpl) implCompound.second;
            }
        } else if (filter instanceof FilterBuilder.KindFilterImpl) {
            implKind = (FilterBuilder.KindFilterImpl) filter;
        }
        List<CsmUID<CsmOffsetableDeclaration>> res = null;
        if (implName != null && implKind != null) {
            if (implName.caseSensitive && implName.match) {
                // can be optimized
                res = new ArrayList<CsmUID<CsmOffsetableDeclaration>>();
                StringBuilder from = new StringBuilder();
                for(int i = 0; i < implKind.kinds.length; i++){
                    from.setLength(0);
                    if (namespace.isGlobal()) {
                        if (implKind.kinds[i] == CsmDeclaration.Kind.VARIABLE || 
                            implKind.kinds[i] == CsmDeclaration.Kind.VARIABLE_DEFINITION) {
                            from.append(Utils.getCsmDeclarationKindkey(implKind.kinds[i]))
                                .append(OffsetableDeclarationBase.UNIQUE_NAME_SEPARATOR)
                                .append("::") // NOI18N
                                .append(implName.strPrefix);
                        } else {
                            from.append(Utils.getCsmDeclarationKindkey(implKind.kinds[i]))
                                .append(OffsetableDeclarationBase.UNIQUE_NAME_SEPARATOR)
                                .append(implName.strPrefix);
                        }
                    } else {
                        from.append(Utils.getCsmDeclarationKindkey(implKind.kinds[i]))
                            .append(OffsetableDeclarationBase.UNIQUE_NAME_SEPARATOR)
                            .append(namespace.getQualifiedName())
                            .append("::") // NOI18N
                            .append(implName.strPrefix);
                    }
                    // possible suffix after name is ' ', '(', '<'.
                    // all chars are contained in the Portable Character Set.
                    // using next char for segment works for any encodins.
                    res.addAll(namespace.findUidsRange(from.toString(), from.append('>').toString())); // NOI18N
                    //res.addAll(namespace.findUidsByPrefix(from));
                }
                if (implName.allowEmptyName) {
                    res.addAll(namespace.getUnnamedUids());
                }
            } else {
                res = new ArrayList<CsmUID<CsmOffsetableDeclaration>>();
                for(int i = 0; i < implKind.kinds.length; i++){
                    String from = Utils.getCsmDeclarationKindkey(implKind.kinds[i]);
                    res.addAll(namespace.findUidsByPrefix(from));
                }
                if (implName.allowEmptyName) {
                    res.addAll(namespace.getUnnamedUids());
                }
            }
        } else if (implKind != null) {
            res = new ArrayList<CsmUID<CsmOffsetableDeclaration>>();
            for(int i = 0; i < implKind.kinds.length; i++){
                String from = Utils.getCsmDeclarationKindkey(implKind.kinds[i]);
                res.addAll(namespace.findUidsByPrefix(from));
            }
        }
        if (res != null) {
            Iterator<CsmOffsetableDeclaration> iter = UIDCsmConverter.UIDsToDeclarations(res).iterator();
            return iter;
        }
        return null;
    }

    @Override
    public Iterator<CsmOffsetableDeclaration> getDeclarations(CsmNamespaceDefinition namespace, CsmFilter filter) {
        if (namespace instanceof NamespaceDefinitionImpl){
            return ((NamespaceDefinitionImpl)namespace).getDeclarations(filter);
        }
        return namespace.getDeclarations().iterator();
    }

    @Override
    public Iterator<CsmOffsetableDeclaration> getDeclarations(CsmFile file, CsmFilter filter) {
        if (file instanceof FileImpl){
            return analyzeFileFilter((FileImpl)file,filter);
        }
        return file.getDeclarations().iterator();
    }

    private Iterator<CsmOffsetableDeclaration> analyzeFileFilter(FileImpl file, CsmFilter filter){
        if (filter instanceof FilterBuilder.InnerOffsetFilterImpl) {
            FilterBuilder.InnerOffsetFilterImpl implOffset = (FilterBuilder.InnerOffsetFilterImpl) filter;
            return file.getDeclarations(implOffset.innerOffset);
        }
        if (file.getDeclarationsSize() < 50) {
            // no optimization
            return file.getDeclarations(filter);
        }
        if (filter instanceof FilterBuilder.OffsetFilterImpl) {
            FilterBuilder.OffsetFilterImpl implOffset = (FilterBuilder.OffsetFilterImpl) filter;
            return UIDCsmConverter.UIDsToDeclarations(file.getDeclarations(implOffset.startOffset, implOffset.endOffset)).iterator();
        } else {
            FilterBuilder.NameFilterImpl implName = null;
            FilterBuilder.KindFilterImpl implKind = null;
            if (filter instanceof FilterBuilder.CompoundFilterImpl) {
                FilterBuilder.CompoundFilterImpl implCompound = (FilterBuilder.CompoundFilterImpl) filter;
                if ((implCompound.first instanceof FilterBuilder.KindFilterImpl) &&
                    (implCompound.second instanceof FilterBuilder.NameFilterImpl)) {
                    implName = (FilterBuilder.NameFilterImpl) implCompound.second;
                    implKind = (FilterBuilder.KindFilterImpl) implCompound.first;
                } else if ((implCompound.first instanceof FilterBuilder.NameFilterImpl) &&
                           (implCompound.second instanceof FilterBuilder.KindFilterImpl)) {
                    implName = (FilterBuilder.NameFilterImpl) implCompound.first;
                    implKind = (FilterBuilder.KindFilterImpl) implCompound.second;
                }
            } else if (filter instanceof FilterBuilder.KindFilterImpl) {
                implKind = (FilterBuilder.KindFilterImpl) filter;
            }
            Collection<CsmUID<CsmOffsetableDeclaration>> res = null;
            if (implName != null && implKind != null) {
                if (implName.caseSensitive && implName.match) {
                    res = file.getDeclarations(implKind.kinds, implName.strPrefix);
                    if (implName.allowEmptyName){
                        res.addAll(file.getDeclarations(implKind.kinds, ""));
                    }
                } else {
                    res = file.getDeclarations(implKind.kinds, null);
                }
            } else if (implKind != null) {
                res = file.getDeclarations(implKind.kinds, null);
            }
            if (res != null) {
                return UIDCsmConverter.UIDsToDeclarations(res, filter);
            }
        }
        return file.getDeclarations(filter);
    }

    @Override
    public Iterator<CsmVariable> getStaticVariables(CsmFile file, CsmFilter filter) {
        if (file instanceof FileImpl){
            return ((FileImpl)file).getStaticVariableDeclarations(filter);
        }
        return Collections.<CsmVariable>emptyList().iterator();
    }

    @Override
    public Iterator<CsmFunction> getStaticFunctions(CsmFile file, CsmFilter filter) {
        if (file instanceof FileImpl){
            return ((FileImpl)file).getStaticFunctionDeclarations(filter);
        }
        return Collections.<CsmFunction>emptyList().iterator();
    }


    @Override
    public Iterator<CsmMember> getClassMembers(CsmClass cls, CsmFilter filter) {
        if (cls instanceof FilterableMembers){
            return ((FilterableMembers)cls).getMembers(filter);
        }
        return cls.getMembers().iterator();
    }

    @Override
    public boolean hasDeclarations(CsmFile file) {
        if (file instanceof FileImpl) {
            return ((FileImpl)file).hasDeclarations();
        }
        return file.getDeclarations().isEmpty();
    }

    @Override
    public Iterator<CsmUID<CsmFile>> getFileUIDs(CsmProject csmProject, NameAcceptor nameFilter) {
        if (csmProject instanceof ProjectBase) {
            return ((ProjectBase)csmProject).getFilteredFileUIDs(nameFilter);
        }
        return UIDCsmConverter.objectsToUIDs(csmProject.getAllFiles()).iterator();
    }
    
    private static interface Filter extends CsmFilter, UIDFilter {
    }
    
    public static interface FilterableMembers {
        Iterator<CsmMember> getMembers(CsmFilter filter);
    }
    
    @SuppressWarnings("unchecked")
    static class FilterBuilder implements CsmFilterBuilder {
        @Override
        public CsmFilter createKindFilter(final CsmDeclaration.Kind ... kinds) {
            return new KindFilterImpl(kinds);
        }

        @SuppressWarnings("unchecked")
        @Override
        public CsmFilter createNameFilter(final CharSequence strPrefix, final boolean match, final boolean caseSensitive, final boolean allowEmptyName) {
            return new NameFilterImpl(allowEmptyName, strPrefix, match, caseSensitive);
        }

        @Override
        public CsmFilter createOffsetFilter(final int startOffset, final int endOffset) {
            return new OffsetFilterImpl(startOffset, endOffset);
        }

        @Override
        public CsmFilter createOffsetFilter(int innerOffset) {
            return new InnerOffsetFilterImpl(innerOffset);
        }

        @Override
        public CsmFilter createCompoundFilter(final CsmFilter first, final CsmFilter second) {
            return new CompoundFilterImpl(first, second);
        }

        @SuppressWarnings("unchecked")
        @Override
        public CsmFilter createNameFilter(final NameAcceptor nameAcceptor) {
            return new NameAcceptorFilterImpl(nameAcceptor);
        }

        private static class KindFilterImpl implements Filter {
            private final Kind[] kinds;

            public KindFilterImpl(Kind[] kinds) {
                this.kinds = kinds;
            }

            @Override
            public boolean accept(CsmUID<?> uid) {
                CsmDeclaration.Kind kind = UIDUtilities.getKind(uid);
                if (kind != null) {
                    for (CsmDeclaration.Kind k : kinds) {
                        if (k == kind) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return Arrays.asList(kinds).toString();
            }
        }

        private static class NameFilterImpl implements Filter {
            private final boolean allowEmptyName;
            private final CharSequence strPrefix;
            private final boolean match;
            private final boolean caseSensitive;
            
            public NameFilterImpl(boolean allowEmptyName, CharSequence strPrefix, boolean match, boolean caseSensitive) {
                this.allowEmptyName = allowEmptyName;
                this.strPrefix = strPrefix;
                this.match = match;
                this.caseSensitive = caseSensitive;
            }

            @Override
            public boolean accept(CsmUID<?> uid) {
                CharSequence name = UIDUtilities.getName(uid);
                if (name != null) {
                    if (allowEmptyName && name.length() == 0) {
                        return true;
                    }
                    return CsmSortUtilities.matchName(name, strPrefix, match, caseSensitive);
                }
                return false;
            }

            @Override
            public String toString() {
                return "pref=" + strPrefix + "; match=" + match + "; cs=" + caseSensitive + "; allowEmpty=" + allowEmptyName; // NOI18N
            }
        }

        private static class OffsetFilterImpl implements Filter {
            private final int startOffset;
            private final int endOffset;

            public OffsetFilterImpl(int startOffset, int endOffset) {
                this.startOffset = startOffset;
                this.endOffset = endOffset;
            }

            @Override
            public boolean accept(CsmUID<?> uid) {
                int start = UIDUtilities.getStartOffset(uid);
                int end = UIDUtilities.getEndOffset(uid);
                if (start < 0) {
                    return true;
                }
                if (end < startOffset || start >= endOffset) {
                    return false;
                }
                return true;
            }

            @Override
            public String toString() {
                return "start offset=" + startOffset + "; endOffset=" + endOffset; // NOI18N
            }
        }

        private static class InnerOffsetFilterImpl implements Filter {
            private final int innerOffset;

            public InnerOffsetFilterImpl(int innerOffset) {
                this.innerOffset = innerOffset;
            }

            @Override
            public boolean accept(CsmUID<?> uid) {
                int start = UIDUtilities.getStartOffset(uid);
                int end = UIDUtilities.getEndOffset(uid);
                if (start < 0) {
                    return true;
                }
                if (start <= innerOffset && innerOffset <= end) {
                    return true;
                }
                return false;
            }

            @Override
            public String toString() {
                return "inner offset=" + innerOffset; // NOI18N
            }
        }

        private static class CompoundFilterImpl implements Filter {
            private final CsmFilter first;
            private final CsmFilter second;

            public CompoundFilterImpl(CsmFilter first, CsmFilter second) {
                this.first = first;
                this.second = second;
            }

            @Override
            public boolean accept(CsmUID<?> uid) {
                return ((UIDFilter) first).accept(uid) && ((UIDFilter) second).accept(uid);
            }

            @Override
            public String toString() {
                return "filter [" + first + "][" + second + "]"; // NOI18N
            }
        }

        private static class NameAcceptorFilterImpl implements Filter {
            private final NameAcceptor nameAcceptor;

            public NameAcceptorFilterImpl(NameAcceptor nameAcceptor) {
                this.nameAcceptor = nameAcceptor;
            }

            @Override
            public boolean accept(CsmUID<?> uid) {
                CharSequence name = UIDUtilities.getName(uid);
                return nameAcceptor.accept(name);
            }
        }
    }

}
