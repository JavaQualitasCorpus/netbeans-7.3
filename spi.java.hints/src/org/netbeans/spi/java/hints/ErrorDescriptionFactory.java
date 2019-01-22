/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008-2010 Sun Microsystems, Inc.
 */

package org.netbeans.spi.java.hints;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.swing.SwingUtilities;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.modules.analysis.api.CodeAnalysis;
import org.netbeans.modules.analysis.spi.Analyzer.WarningDescription;
import org.netbeans.modules.java.hints.providers.spi.HintMetadata;
import org.netbeans.modules.java.hints.providers.spi.HintMetadata.Options;
import org.netbeans.modules.java.hints.spiimpl.Hacks.InspectAndTransformOpener;
import org.netbeans.modules.java.hints.spiimpl.SPIAccessor;
import org.netbeans.modules.java.hints.spiimpl.SyntheticFix;
import org.netbeans.modules.java.hints.spiimpl.options.HintsSettings;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.EnhancedFix;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.LazyFixList;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Parameters;

/**
 *
 * @author Jan Lahoda
 */
public class ErrorDescriptionFactory {

    private ErrorDescriptionFactory() {
    }

//    public static ErrorDescription forTree(HintContext context, String text, Fix... fixes) {
//        return forTree(context, context.getContext(), text, fixes);
//    }

    public static ErrorDescription forTree(HintContext context, TreePath tree, String text, Fix... fixes) {
        return forTree(context, tree.getLeaf(), text, fixes);
    }
    
    public static ErrorDescription forTree(HintContext context, Tree tree, String text, Fix... fixes) {
        int start;
        int end;

        if (context.getHintMetadata().kind == Hint.Kind.INSPECTION) {
            start = (int) context.getInfo().getTrees().getSourcePositions().getStartPosition(context.getInfo().getCompilationUnit(), tree);
            end = (int) context.getInfo().getTrees().getSourcePositions().getEndPosition(context.getInfo().getCompilationUnit(), tree);
        } else {
            start = end = context.getCaretLocation();
        }

        if (start != (-1) && end != (-1)) {
            LazyFixList fixesForED = org.netbeans.spi.editor.hints.ErrorDescriptionFactory.lazyListForFixes(resolveDefaultFixes(context, fixes));
            return org.netbeans.spi.editor.hints.ErrorDescriptionFactory.createErrorDescription("text/x-java:" + context.getHintMetadata().id, context.getSeverity(), text, context.getHintMetadata().description, fixesForED, context.getInfo().getFileObject(), start, end);
        }

        return null;
    }
    
    /**Create a new {@link ErrorDescription}. Severity is automatically inferred from the {@link HintContext},
     * and the {@link ErrorDescription} is created to be consistent with {@link ErrorDescription}s created
     * by the other factory methods in this class.
     * 
     * @param context from which the {@link Severity} and other properties are inferred.
     * @param start start of the warning
     * @param end end of the warning
     * @param text the warning text
     * @param fixes one or more {@link Fix}es to show shown to the user.
     * @return a standard {@link ErrorDescription} for use in Java source
     * @since 1.9
     */
    public static ErrorDescription forSpan(HintContext context, int start, int end, String text, Fix... fixes) {
        if (context.getHintMetadata().kind != Hint.Kind.INSPECTION) {
            start = end = context.getCaretLocation();
        }

        if (start != (-1) && end != (-1)) {
            LazyFixList fixesForED = org.netbeans.spi.editor.hints.ErrorDescriptionFactory.lazyListForFixes(resolveDefaultFixes(context, fixes));
            return org.netbeans.spi.editor.hints.ErrorDescriptionFactory.createErrorDescription("text/x-java:" + context.getHintMetadata().id, context.getSeverity(), text, context.getHintMetadata().description, fixesForED, context.getInfo().getFileObject(), start, end);
        }

        return null;
    }
    
    public static ErrorDescription forName(HintContext context, TreePath tree, String text, Fix... fixes) {
        return forName(context, tree.getLeaf(), text, fixes);
    }

    public static ErrorDescription forName(HintContext context, Tree tree, String text, Fix... fixes) {
        int[] span;
        
        if (context.getHintMetadata().kind == Hint.Kind.INSPECTION) {
            span = computeNameSpan(tree, context);
        } else {
            span = new int[] {context.getCaretLocation(), context.getCaretLocation()};
        }
        
        if (span != null && span[0] != (-1) && span[1] != (-1)) {
            LazyFixList fixesForED = org.netbeans.spi.editor.hints.ErrorDescriptionFactory.lazyListForFixes(resolveDefaultFixes(context, fixes));
            return org.netbeans.spi.editor.hints.ErrorDescriptionFactory.createErrorDescription("text/x-java:" + context.getHintMetadata().id, context.getSeverity(), text, context.getHintMetadata().description, fixesForED, context.getInfo().getFileObject(), span[0], span[1]);
        }

        return null;
    }

    private static int[] computeNameSpan(Tree tree, HintContext context) {
        switch (tree.getKind()) {
            case METHOD:
                return context.getInfo().getTreeUtilities().findNameSpan((MethodTree) tree);
            case ANNOTATION_TYPE:
            case CLASS:
            case ENUM:
            case INTERFACE:
                return context.getInfo().getTreeUtilities().findNameSpan((ClassTree) tree);
            case VARIABLE:
                return context.getInfo().getTreeUtilities().findNameSpan((VariableTree) tree);
            case MEMBER_SELECT:
                //XXX:
                MemberSelectTree mst = (MemberSelectTree) tree;
                int[] span = context.getInfo().getTreeUtilities().findNameSpan(mst);

                if (span == null) {
                    int end = (int) context.getInfo().getTrees().getSourcePositions().getEndPosition(context.getInfo().getCompilationUnit(), tree);
                    span = new int[] {end - mst.getIdentifier().length(), end};
                }
                return span;
            case METHOD_INVOCATION:
                return computeNameSpan(((MethodInvocationTree) tree).getMethodSelect(), context);
            default:
                int start = (int) context.getInfo().getTrees().getSourcePositions().getStartPosition(context.getInfo().getCompilationUnit(), tree);
                if (    StatementTree.class.isAssignableFrom(tree.getKind().asInterface())
                    && tree.getKind() != Kind.EXPRESSION_STATEMENT
                    && tree.getKind() != Kind.BLOCK) {
                    TokenSequence<?> ts = context.getInfo().getTokenHierarchy().tokenSequence();
                    ts.move(start);
                    if (ts.moveNext()) {
                        return new int[] {ts.offset(), ts.offset() + ts.token().length()};
                    }
                }
                return new int[] {
                    start,
                    (int) context.getInfo().getTrees().getSourcePositions().getEndPosition(context.getInfo().getCompilationUnit(), tree),
                };
        }
    }

    static List<Fix> resolveDefaultFixes(HintContext ctx, Fix... provided) {
        List<Fix> auxiliaryFixes = new LinkedList<Fix>();
        HintMetadata hm = SPIAccessor.getINSTANCE().getHintMetadata(ctx);

        if (hm != null) {
            Set<String> suppressWarningsKeys = new LinkedHashSet<String>();

            for (String key : hm.suppressWarnings) {
                if (key == null || key.length() == 0) {
                    break;
                }

                suppressWarningsKeys.add(key);
            }


            auxiliaryFixes.add(new DisableConfigure(hm, true));
            auxiliaryFixes.add(new DisableConfigure(hm, false));

            if (hm.kind == Hint.Kind.INSPECTION) {
                auxiliaryFixes.add(new InspectFix(hm, false));
                if (!hm.options.contains(Options.QUERY)) {
                    auxiliaryFixes.add(new InspectFix(hm, true));
                }
            }
            
            if (!suppressWarningsKeys.isEmpty()) {
                auxiliaryFixes.addAll(createSuppressWarnings(ctx.getInfo(), ctx.getPath(), suppressWarningsKeys.toArray(new String[0])));
            }

            List<Fix> result = new LinkedList<Fix>();

            for (Fix f : provided != null ? provided : new Fix[0]) {
                if (f == null) continue;
                
                result.add(org.netbeans.spi.editor.hints.ErrorDescriptionFactory.attachSubfixes(f, auxiliaryFixes));
            }

            if (result.isEmpty()) {
                result.add(org.netbeans.spi.editor.hints.ErrorDescriptionFactory.attachSubfixes(new TopLevelConfigureFix(hm), auxiliaryFixes));
            }

            return result;
        }

        return Arrays.asList(provided);
    }

    private static class DisableConfigure implements Fix, SyntheticFix {
        private final @NonNull HintMetadata metadata;
        private final boolean disable;

        DisableConfigure(@NonNull HintMetadata metadata, boolean disable) {
            this.metadata = metadata;
            this.disable = disable;
        }

        @Override
        public String getText() {
            String displayName = metadata.displayName;
            String key;
            switch (metadata.kind) {
                case INSPECTION:
                    key = disable ? "FIX_DisableHint" : "FIX_ConfigureHint";
                    break;
                case ACTION:
                    key = disable ? "FIX_DisableSuggestion" : "FIX_ConfigureSuggestion";
                    break;
                default:
                    throw new IllegalStateException();
            }

            return NbBundle.getMessage(ErrorDescriptionFactory.class, key, displayName);
        }

        @Override
        public ChangeInfo implement() throws Exception {
            if (disable) {
                HintsSettings.setEnabled(metadata, false);
                //XXX: re-run hints task
            } else {
                OptionsDisplayer.getDefault().open("Editor/Hints/text/x-java/" + metadata.id);
            }

            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            final DisableConfigure other = (DisableConfigure) obj;
            if (this.metadata != other.metadata && (this.metadata == null || !this.metadata.equals(other.metadata))) {
                return false;
            }
            if (this.disable != other.disable) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + (this.metadata != null ? this.metadata.hashCode() : 0);
            hash = 43 * hash + (this.disable ? 1 : 0);
            return hash;
        }


    }

    private static final class TopLevelConfigureFix extends DisableConfigure implements EnhancedFix {

        public TopLevelConfigureFix(@NonNull HintMetadata metadata) {
            super(metadata, false);
        }

        @Override
        public CharSequence getSortText() {
            return "\uFFFFzz";
        }
        
    }

    private static class InspectFix implements Fix, SyntheticFix {
        private final @NonNull HintMetadata metadata;
        private final boolean transform;

        InspectFix(@NonNull HintMetadata metadata, boolean transform) {
            this.metadata = metadata;
            this.transform = transform;
        }

        @Override
        @Messages({
            "DN_InspectAndTransform=Run Inspect&Transform on...",
            "DN_Inspect=Run Inspect on..."
        })
        public String getText() {
            return transform ? Bundle.DN_InspectAndTransform() : Bundle.DN_Inspect();
        }

        @Override
        public ChangeInfo implement() throws Exception {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (transform) {
                        final InspectAndTransformOpener o = Lookup.getDefault().lookup(InspectAndTransformOpener.class);

                        if (o != null) {
                            o.openIAT(metadata);
                        } else {
                            //warn
                        }
                    } else {
                        CodeAnalysis.open(WarningDescription.create("text/x-java:" + metadata.id, null, null, null));
                    }
                }
            });
            
            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            final InspectFix other = (InspectFix) obj;
            if (this.metadata != other.metadata && (this.metadata == null || !this.metadata.equals(other.metadata))) {
                return false;
            }
            if (this.transform != other.transform) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + (this.metadata != null ? this.metadata.hashCode() : 0);
            hash = 43 * hash + (this.transform ? 1 : 0);
            return hash;
        }


    }
    
    /** Creates a fix, which when invoked adds @SuppresWarnings(keys) to
     * nearest declaration.
     * @param compilationInfo CompilationInfo to work on
     * @param treePath TreePath to a tree. The method will find nearest outer
     *        declaration. (type, method, field or local variable)
     * @param keys keys to be contained in the SuppresWarnings annotation. E.g.
     *        @SuppresWarnings( "key" ) or @SuppresWarnings( {"key1", "key2", ..., "keyN" } ).
     * @throws IllegalArgumentException if keys are null or empty or id no suitable element
     *         to put the annotation on is found (e.g. if TreePath to CompilationUnit is given")
     */
    static Fix createSuppressWarningsFix(CompilationInfo compilationInfo, TreePath treePath, String... keys ) {
        Parameters.notNull("compilationInfo", compilationInfo);
        Parameters.notNull("treePath", treePath);
        Parameters.notNull("keys", keys);

        if (keys.length == 0) {
            throw new IllegalArgumentException("key must not be empty"); // NOI18N
        }

        if (!isSuppressWarningsSupported(compilationInfo)) {
            return null;
        }

        while (treePath.getLeaf().getKind() != Kind.COMPILATION_UNIT && !DECLARATION.contains(treePath.getLeaf().getKind())) {
            treePath = treePath.getParentPath();
        }

        if (treePath.getLeaf().getKind() != Kind.COMPILATION_UNIT) {
            return new FixImpl(TreePathHandle.create(treePath, compilationInfo), compilationInfo.getFileObject(), keys);
        } else {
            return null;
        }
    }

    /** Creates a fix, which when invoked adds @SuppresWarnings(keys) to
     * nearest declaration.
     * @param compilationInfo CompilationInfo to work on
     * @param treePath TreePath to a tree. The method will find nearest outer
     *        declaration. (type, method, field or local variable)
     * @param keys keys to be contained in the SuppresWarnings annotation. E.g.
     *        @SuppresWarnings( "key" ) or @SuppresWarnings( {"key1", "key2", ..., "keyN" } ).
     * @throws IllegalArgumentException if keys are null or empty or id no suitable element
     *         to put the annotation on is found (e.g. if TreePath to CompilationUnit is given")
     */
    static List<Fix> createSuppressWarnings(CompilationInfo compilationInfo, TreePath treePath, String... keys ) {
        Parameters.notNull("compilationInfo", compilationInfo);
        Parameters.notNull("treePath", treePath);
        Parameters.notNull("keys", keys);

        if (keys.length == 0) {
            throw new IllegalArgumentException("key must not be empty"); // NOI18N
        }

        Fix f = createSuppressWarningsFix(compilationInfo, treePath, keys);

        if (f != null) {
            return Collections.<Fix>singletonList(f);
        } else {
            return Collections.emptyList();
        }
    }

    private static boolean isSuppressWarningsSupported(CompilationInfo info) {
        //cannot suppress if there is no SuppressWarnings annotation in the platform:
        if (info.getElements().getTypeElement("java.lang.SuppressWarnings") == null)
            return false;

        return info.getSourceVersion().compareTo(SourceVersion.RELEASE_5) >= 0;
    }

    private static final Set<Kind> DECLARATION = EnumSet.of(Kind.ANNOTATION_TYPE, Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.METHOD, Kind.VARIABLE);

    private static final class FixImpl implements Fix, SyntheticFix {

        private String keys[];
        private TreePathHandle handle;
        private FileObject file;

        public FixImpl(TreePathHandle handle, FileObject file, String... keys) {
            this.keys = keys;
            this.handle = handle;
            this.file = file;
        }

        public String getText() {
            StringBuilder keyNames = new StringBuilder();
            for (int i = 0; i < keys.length; i++) {
                String string = keys[i];
                keyNames.append(string);
                if ( i < keys.length - 1) {
                    keyNames.append(", "); // NOI18N
                }
            }

            return NbBundle.getMessage(ErrorDescriptionFactory.class, "LBL_FIX_Suppress_Waning",  keyNames.toString() );  // NOI18N
        }

        public ChangeInfo implement() throws IOException {
            JavaSource js = JavaSource.forFileObject(file);

            js.runModificationTask(new Task<WorkingCopy>() {
                public void run(WorkingCopy copy) throws IOException {
                    copy.toPhase(Phase.RESOLVED); //XXX: performance
                    TreePath path = handle.resolve(copy);

                    while (path != null && path.getLeaf().getKind() != Kind.COMPILATION_UNIT && !DECLARATION.contains(path.getLeaf().getKind())) {
                        path = path.getParentPath();
                    }

                    if (path.getLeaf().getKind() == Kind.COMPILATION_UNIT) {
                        return ;
                    }

                    Tree top = path.getLeaf();
                    ModifiersTree modifiers = null;

                    switch (top.getKind()) {
                        case ANNOTATION_TYPE:
                        case CLASS:
                        case ENUM:
                        case INTERFACE:
                            modifiers = ((ClassTree) top).getModifiers();
                            break;
                        case METHOD:
                            modifiers = ((MethodTree) top).getModifiers();
                            break;
                        case VARIABLE:
                            modifiers = ((VariableTree) top).getModifiers();
                            break;
                        default: assert false : "Unhandled Tree.Kind";  // NOI18N
                    }

                    if (modifiers == null) {
                        return ;
                    }

                    TypeElement el = copy.getElements().getTypeElement("java.lang.SuppressWarnings");  // NOI18N

                    if (el == null) {
                        return ;
                    }

                    LiteralTree[] keyLiterals = new LiteralTree[keys.length];

                    for (int i = 0; i < keys.length; i++) {
                        keyLiterals[i] = copy.getTreeMaker().
                                Literal(keys[i]);
                    }

                    ModifiersTree nueMods = GeneratorUtilities.get(copy).appendToAnnotationValue(modifiers, el, "value", keyLiterals);

                    copy.rewrite(modifiers, nueMods);
                }
            }).commit();

            return null;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FixImpl other = (FixImpl) obj;
            if (!Arrays.deepEquals(this.keys, other.keys)) {
                return false;
            }
            if (this.handle != other.handle && (this.handle == null || !this.handle.equals(other.handle))) {
                return false;
            }
            if (this.file != other.file && (this.file == null || !this.file.equals(other.file))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 79 * hash + Arrays.deepHashCode(this.keys);
            hash = 79 * hash + (this.handle != null ? this.handle.hashCode() : 0);
            hash = 79 * hash + (this.file != null ? this.file.hashCode() : 0);
            return hash;
        }
    }
}