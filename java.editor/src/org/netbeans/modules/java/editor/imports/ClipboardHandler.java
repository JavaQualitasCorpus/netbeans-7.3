/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.java.editor.imports;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.awt.Dialog;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;

import com.sun.source.tree.ImportTree;
import org.netbeans.api.editor.EditorActionRegistration;
import org.netbeans.api.editor.EditorActionRegistrations;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.ActionFactory.CutToLineBeginOrEndAction;
import org.netbeans.editor.BaseKit;
import org.netbeans.editor.BaseKit.CutAction;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author lahvac
 */
public class ClipboardHandler {

    public static void install(JTextComponent c) {
        c.setTransferHandler(new ImportingTransferHandler(c.getTransferHandler()));
    }

    private static final Logger LOG = Logger.getLogger(ClipboardHandler.class.getName());
    private static final RequestProcessor WORKER = new RequestProcessor(ClipboardHandler.class.getName(), 3, false, false);
    
    private static void doImport(JavaSource js, final Document doc, final int caret, final Map<String, String> simple2ImportFQN, final List<Position[]> inSpans, AtomicBoolean cancel) {
        final Map<Position[], String> putFQNs = new HashMap<Position[], String>();

        try {
            final ModificationResult mr = js.runModificationTask(new Task<WorkingCopy>() {
                @Override public void run(WorkingCopy copy) throws Exception {
                    copy.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                    TreePath context = copy.getTreeUtilities().pathFor(caret);
                    Scope scope = copy.getTrees().getScope(context);
                    Scope cutScope = copy.getTrees().getScope(new TreePath(context.getCompilationUnit()));
                    List<Position[]> spans = new ArrayList<Position[]>(inSpans);

                    Collections.sort(spans, new Comparator<Position[]>() {
                        @Override public int compare(Position[] o1, Position[] o2) {
                            return o1[0].getOffset() - o2[0].getOffset();
                        }
                    });

                    Map<String, String> imported = new HashMap<String, String>();

                    for (Position[] span : spans) {
                        String currentSimpleName = copy.getText().substring(span[0].getOffset(), span[1].getOffset());
                        String handled = imported.get(currentSimpleName);

                        if (handled == null) {
                            String fqn = simple2ImportFQN.get(currentSimpleName);

                            Element e = fqn2element(copy.getElements(), fqn);
                            if (e == null) continue;

                            if (e.getKind().isClass() || e.getKind().isInterface()) {
                                handled = SourceUtils.resolveImport(copy, context, fqn);
                            } else {
                                CompilationUnitTree cut = (CompilationUnitTree) copy.resolveRewriteTarget(copy.getCompilationUnit());
                                if (e.getModifiers().contains(Modifier.STATIC) && copy.getTreeUtilities().isAccessible(cutScope, e, e.getEnclosingElement().asType())
                                        && (scope.getEnclosingClass() == null || copy.getElementUtilities().outermostTypeElement(e) != copy.getElementUtilities().outermostTypeElement(scope.getEnclosingClass()))) {
                                    copy.rewrite(copy.getCompilationUnit(), GeneratorUtilities.get(copy).addImports(cut, Collections.singleton(e)));
                                }
                                handled = e.getSimpleName().toString();
                            }
                            imported.put(currentSimpleName, handled);
                        }

                        putFQNs.put(span, handled);
                    }
                }
            });

            if (cancel.get()) return ;
            
            NbDocument.runAtomicAsUser((StyledDocument) doc, new Runnable() {
                @Override public void run() {
                    try {
                        mr.commit();
                        for (Entry<Position[], String> e : putFQNs.entrySet()) {
                            doc.remove(e.getKey()[0].getOffset(), e.getKey()[1].getOffset() - e.getKey()[0].getOffset());
                            doc.insertString(e.getKey()[0].getOffset(), e.getValue(), null);
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            });
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    static boolean autoImport = false; //tests
    private static void showImportDialog(final JavaSource js, final Document doc, final int caret, final Map<String, String> simple2ImportFQN, Collection<String> toShow, final List<Position[]> inSpans) {
        if (autoImport) {
            doImport(js, doc, caret, simple2ImportFQN, inSpans, new AtomicBoolean());
            return;
        }

        ClipboardImportPanel panel = new ClipboardImportPanel(toShow);
        final AtomicBoolean cancel = new AtomicBoolean();
        final JButton okButton = new JButton(NbBundle.getMessage(ClipboardHandler.class, "BTN_ClipboardImportOK"));
        final JButton cancelButton = new JButton(NbBundle.getMessage(ClipboardHandler.class, "BTN_ClipboardImportCancel"));
        DialogDescriptor dd = new DialogDescriptor(panel, NbBundle.getMessage(ClipboardHandler.class, "MSG_ClipboardImportImportClasses"), true, new Object[] {okButton, cancelButton}, okButton, DialogDescriptor.DEFAULT_ALIGN, null, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { }
        });
        final Dialog[] d = new Dialog[1];

        okButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(false);
                WORKER.post(new Runnable() {
                    @Override public void run() {
                        doImport(js, doc, caret, simple2ImportFQN, inSpans, cancel);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                d[0].setVisible(false);
                                d[0].dispose();
                            }
                        });
                    }
                });
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                cancel.set(true);
                d[0].setVisible(false);
                d[0].dispose();
            }
        });

        d[0] = DialogDisplayer.getDefault().createDialog(dd);
        d[0].setVisible(true);
    }

    private static Collection<? extends String> needsImports(JavaSource js, final int caret, final Map<String, String> simple2FQNs) {
        final List<String> unavailable = new ArrayList<String>();

        boolean finished = runQuickly(js, new Task<CompilationController>() {
            @Override
            public void run(final CompilationController cc) throws Exception {
                cc.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);

                final TreePath tp = cc.getTreeUtilities().pathFor(caret);
                final Scope context = cc.getTrees().getScope(tp);

                SourcePositions[] sps = new SourcePositions[1];

                OUTER: for (Entry<String, String> e : simple2FQNs.entrySet()) {
                    Element el = fqn2element(cc.getElements(), e.getValue());
                    if (el == null) {
                        continue;
                    } else if (el.getKind().isClass() || el.getKind().isInterface()) {
                        ExpressionTree simpleName = cc.getTreeUtilities().parseExpression(e.getKey() + ".class", sps);
                        cc.getTreeUtilities().attributeTree(simpleName, context);
                        Element elm = cc.getTrees().getElement(new TreePath(tp, ((MemberSelectTree) simpleName).getExpression()));
                        if (el.equals(elm)) continue;
                    } else {
                        if (!cc.getTreeUtilities().isAccessible(context, el, el.getEnclosingElement().asType())
                                || (context.getEnclosingClass() != null && cc.getElementUtilities().outermostTypeElement(el) == cc.getElementUtilities().outermostTypeElement(context.getEnclosingClass()))) continue;
                        for (ImportTree importTree : cc.getCompilationUnit().getImports()) {
                            if (importTree.isStatic() && importTree.getQualifiedIdentifier().getKind() == Tree.Kind.MEMBER_SELECT) {
                                MemberSelectTree mst = (MemberSelectTree) importTree.getQualifiedIdentifier();
                                Element elm = cc.getTrees().getElement(TreePath.getPath(cc.getCompilationUnit(), mst.getExpression()));
                                if (el.getEnclosingElement().equals(elm) && ("*".contentEquals(mst.getIdentifier()) || el.getSimpleName().contentEquals(mst.getIdentifier()))) continue OUTER; //NOI18N
                            }
                        }
                    }
                    unavailable.add(e.getValue());
                }
            }
        });

        if (finished) {
            return unavailable;
        } else {
            return null;
        }
    }
    
    private static Element fqn2element(final Elements elements, final String fqn) {
        if (fqn == null) {
            return null;
        }
        TypeElement type = elements.getTypeElement(fqn);
        if (type != null) {
            return type;
        }
        int idx = fqn.lastIndexOf('.');
        if (idx > 0) {
            type = elements.getTypeElement(fqn.substring(0, idx));
            String name = fqn.substring(idx + 1);
            if (type != null && name.length() > 0) {
                for (Element el : type.getEnclosedElements()) {
                    if (el.getModifiers().contains(Modifier.STATIC) && name.contentEquals(el.getSimpleName())) {
                        return el;
                    }
                }
            }
        }
        return null;
    }

    private static boolean runQuickly(final JavaSource js, final Task<CompilationController> task) {
        final CountDownLatch started = new CountDownLatch(1);
        final AtomicBoolean cancel = new AtomicBoolean();

        RequestProcessor.Task t = WORKER.post(new Runnable() {
            @Override public void run() {
                try {
                    js.runUserActionTask(new Task<CompilationController>() {
                        @Override public void run(CompilationController parameter) throws Exception {
                            started.countDown();
                            if (cancel.get()) return ;

                            task.run(parameter);
                        }
                    }, true);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });

        boolean finished;

        try {
            finished = started.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
            finished = false;
        }

        if (finished) {
            try {
                finished = t.waitFinished(1000);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
                finished = false;
            }
        } else {
            cancel.set(true);
        }

        return finished;
    }

    private static final class ImportingTransferHandler extends TransferHandler {
        private final TransferHandler delegate;

        public ImportingTransferHandler(TransferHandler delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return delegate.canImport(support);
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            return delegate.canImport(comp, transferFlavors);
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            try {
                java.lang.reflect.Method method = delegate.getClass().getDeclaredMethod(
                    "createTransferable", // NOI18N
                    new Class[] {javax.swing.JComponent.class});
                method.setAccessible(true);

                return (Transferable)method.invoke(delegate, new Object[] {c});
            } catch (NoSuchMethodException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalAccessException ex) {
                Exceptions.printStackTrace(ex);
            } catch (java.lang.reflect.InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }

        @Override
        public void exportAsDrag(JComponent comp, InputEvent e, int action) {
            delegate.exportAsDrag(comp, e, action);
        }

        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            try {
                java.lang.reflect.Method method = delegate.getClass().getDeclaredMethod(
                    "exportDone",  // NOI18N
                    new Class[] {javax.swing.JComponent.class, Transferable.class, int.class});
                method.setAccessible(true);
                method.invoke(delegate, new Object[] {source, data, new Integer(action)});
            } catch (NoSuchMethodException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalAccessException ex) {
                Exceptions.printStackTrace(ex);
            } catch (java.lang.reflect.InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
            JavaSource js;
            ImportsWrapper iw = null;
            boolean copiedFromString = false;

            if (comp instanceof JTextComponent) {
                copiedFromString = insideToken((JTextComponent)comp, JavaTokenId.STRING_LITERAL, JavaTokenId.CHAR_LITERAL);
                if (comp.getClientProperty(NO_IMPORTS) == null && (js = JavaSource.forDocument(((JTextComponent) comp).getDocument())) != null) {
                    final JTextComponent tc = (JTextComponent) comp;
                    final int start = tc.getSelectionStart();
                    final int end = tc.getSelectionEnd();
                    final Map<String, String> simple2ImportFQN = new HashMap<String, String>();
                    final List<int[]> spans = new ArrayList<int[]>();

                    Task<CompilationController> w = new Task<CompilationController>() {
                        @Override public void run(final CompilationController parameter) throws Exception {
                            parameter.toPhase(JavaSource.Phase.RESOLVED);

                            new TreePathScanner<Void, Void>() {
                                @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                                    int s = (int) parameter.getTrees().getSourcePositions().getStartPosition(parameter.getCompilationUnit(), node);
                                    int e = (int) parameter.getTrees().getSourcePositions().getEndPosition(parameter.getCompilationUnit(), node);
                                    javax.lang.model.element.Element el = parameter.getTrees().getElement(getCurrentPath());

                                    if (s >= start && e >= start && e <= end && el != null) {
                                        if (el.getKind().isClass() || el.getKind().isInterface()) {
                                            TreePath parentPath = getCurrentPath().getParentPath();
                                            if (parentPath == null || parentPath.getLeaf().getKind() != Tree.Kind.NEW_CLASS
                                                    || ((NewClassTree)parentPath.getLeaf()).getEnclosingExpression() == null
                                                    || ((NewClassTree)parentPath.getLeaf()).getIdentifier() != node) {
                                                simple2ImportFQN.put(el.getSimpleName().toString(), ((TypeElement) el).getQualifiedName().toString());
                                                spans.add(new int[] {s - start, e - start});
                                            }
                                        } else if ((el.getKind().isField() || el.getKind() == ElementKind.METHOD)
                                                && el.getModifiers().contains(Modifier.STATIC)
                                                && !el.getModifiers().contains(Modifier.PRIVATE)) {
                                            simple2ImportFQN.put(el.getSimpleName().toString(), ((TypeElement) el.getEnclosingElement()).getQualifiedName().toString() + '.' + el.getSimpleName().toString());
                                            spans.add(new int[] {s - start, e - start});
                                        }
                                    }
                                    return super.visitIdentifier(node, p);
                                }
                                private Tree lastType;
                                @Override
                                public Void visitVariable(VariableTree node, Void p) {
                                    if (lastType == node.getType()) {
                                        scan(node.getInitializer(), null);
                                        return null;
                                    } else {
                                        lastType = node.getType();
                                        return super.visitVariable(node, p);
                                    }
                                }
                                @Override public Void scan(Tree tree, Void p) {
                                    if (tree == null || parameter.getTreeUtilities().isSynthetic(new TreePath(getCurrentPath(), tree))) return null;
                                    return super.scan(tree, p);
                                }
                            }.scan(parameter.getCompilationUnit(), null);
                        }
                    };

                    boolean finished = false;

                    if (comp.getClientProperty(RUN_SYNCHRONOUSLY) != null) {
                        try {
                            js.runUserActionTask(w, true);
                            finished = true;
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    } else {
                        finished = runQuickly(js, w);
                    }


                    if (finished) {
                        iw = new ImportsWrapper(NbEditorUtilities.getFileObject(tc.getDocument()), simple2ImportFQN, spans);
                    }
                }
            }
            
            delegate.exportToClipboard(comp, clip, action);
            if (iw != null || copiedFromString) {
                clip.setContents(new WrappedTransferable(clip.getContents(null), iw, copiedFromString), null);
            }
        }

        @Override
        public int getSourceActions(JComponent c) {
            return delegate.getSourceActions(c);
        }

        @Override
        public Icon getVisualRepresentation(Transferable t) {
            return delegate.getVisualRepresentation(t);
        }

        @Override
        public boolean importData(TransferSupport support) {
            return delegate.importData(support);
        }

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            if (t.isDataFlavorSupported(IMPORT_FLAVOR) && comp instanceof JTextComponent && !insideToken((JTextComponent)comp, JavaTokenId.STRING_LITERAL)) {
                boolean result = false;

                try {
                    final JTextComponent tc = (JTextComponent) comp;
                    final int caret = tc.getSelectionStart();

                    if (result = delegatedImportData(comp, t)) {
                        final ImportsWrapper imports = (ImportsWrapper) t.getTransferData(IMPORT_FLAVOR);
                        final FileObject file = NbEditorUtilities.getFileObject(tc.getDocument());
                        final Document doc = tc.getDocument();
                        final int len = doc.getLength();
                        final List<Position[]> inSpans = new ArrayList<Position[]>();

                        for (int[] span : imports.identifiers) {
                            int start = caret + span[0];
                            int end = caret + span[1];
                            if (0 <= start && start <= end && end <= len)
                                inSpans.add(new Position[] {doc.createPosition(start), doc.createPosition(end)});
                        }

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() {
                                JavaSource js = JavaSource.forDocument(tc.getDocument());

                                if (js == null) return;

                                Collection<? extends String> unavailable = needsImports(js, caret, imports.simple2ImportFQN);

                                if (unavailable == null) {
                                    unavailable = (file == null || !file.equals(imports.sourceFO)) ? imports.simple2ImportFQN.values() : Collections.<String>emptyList();
                                }

                                final Collection<String> toShow = new HashSet<String>(imports.simple2ImportFQN.values());
                                
                                toShow.retainAll(unavailable);

                                if (!unavailable.isEmpty()) {
                                    showImportDialog(js, doc, caret, imports.simple2ImportFQN, toShow, inSpans);
                                }
                            }
                        });
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (UnsupportedFlavorException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }

                return result;
            }

            return delegatedImportData(comp, t);
        }
        
        private boolean delegatedImportData(final JComponent comp, final Transferable t) {
            if (comp instanceof JTextComponent && !t.isDataFlavorSupported(COPY_FROM_STRING_FLAVOR) && insideToken((JTextComponent) comp, JavaTokenId.STRING_LITERAL)) {
                return delegate.importData(comp, new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return t.getTransferDataFlavors();
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return t.isDataFlavorSupported(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                        Object data = t.getTransferData(flavor);
                        if (data instanceof String) {
                            String s = (String) data;
                            s = s.replace("\\","\\\\"); //NOI18N
                            s = s.replace("\"","\\\""); //NOI18N
                            s = s.replace("\n","\\n\" +\n\""); //NOI18N
                            data = s;
                        } else if (data instanceof Reader) {
                            BufferedReader br = new BufferedReader((Reader)data);
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                line = line.replace("\\","\\\\"); //NOI18N
                                line = line.replace("\"","\\\""); //NOI18N
                                if (sb.length() > 0) {
                                    sb.append("\\n\" +\n\""); //NOI18N
                                }
                                sb.append(line);
                            }
                            data = new StringReader(sb.toString());
                        }
                        return data;
                    }
                });
            }
            return delegate.importData(comp, t);
        }
        
        private boolean insideToken(final JTextComponent jtc, final JavaTokenId first, final JavaTokenId... rest) {
            final Document doc = jtc.getDocument();
            final boolean[] result = new boolean[1];
            
            doc.render(new Runnable() {
                @Override public void run() {
                    int offset = jtc.getSelectionStart();
                    TokenSequence<JavaTokenId> ts = SourceUtils.getJavaTokenSequence(TokenHierarchy.get(doc), offset);
                    if (ts == null || !ts.moveNext() && !ts.movePrevious() || offset == ts.offset()) {
                        result[0] = false;
                    } else {
                        EnumSet tokenIds = EnumSet.of(first, rest);
                        result[0] = tokenIds.contains(ts.token().id());
                    }
                }
            });
            
            return result[0];
        }
    }

    private static final Object NO_IMPORTS = new Object();
    private static final Object RUN_SYNCHRONOUSLY = new Object();
    private static final DataFlavor IMPORT_FLAVOR = new DataFlavor(ImportsWrapper.class, NbBundle.getMessage(ClipboardHandler.class, "MSG_ClipboardImportFlavor"));
    private static final DataFlavor COPY_FROM_STRING_FLAVOR = new DataFlavor(Boolean.class, NbBundle.getMessage(ClipboardHandler.class, "MSG_ClipboardCopyFromStringFlavor"));

    private static final class WrappedTransferable implements Transferable {

        private final Transferable delegate;
        private final ImportsWrapper importsData;
        private final boolean copiedFromString;

        public WrappedTransferable(Transferable delegate, ImportsWrapper importsData, boolean copiedFromString) {
            this.delegate = delegate;
            this.importsData = importsData;
            this.copiedFromString = copiedFromString;
        }

        private DataFlavor[] transferDataFlavorsCache;

        @Override
        public synchronized DataFlavor[] getTransferDataFlavors() {
            if (transferDataFlavorsCache != null) return transferDataFlavorsCache;

            DataFlavor[] f = delegate.getTransferDataFlavors();
            DataFlavor[] result = Arrays.copyOf(f, f.length + (importsData != null ? 1 : 0) + (copiedFromString ? 1 : 0));

            if (importsData != null)
                result[f.length] = IMPORT_FLAVOR;
            if (copiedFromString)
                result[result.length - 1] = COPY_FROM_STRING_FLAVOR;

            return transferDataFlavorsCache = result;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return IMPORT_FLAVOR.equals(flavor) && importsData != null
                    || COPY_FROM_STRING_FLAVOR.equals(flavor) && copiedFromString
                    || delegate.isDataFlavorSupported(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (IMPORT_FLAVOR.equals(flavor)) return importsData;
            if (COPY_FROM_STRING_FLAVOR.equals(flavor)) return copiedFromString;
            return delegate.getTransferData(flavor);
        }

    }

    public static final class ImportsWrapper {
        private final FileObject sourceFO;
        private final Map<String, String> simple2ImportFQN;
        private final List<int[]> identifiers;

        public ImportsWrapper(FileObject sourceFO, Map<String, String> simple2ImportFQN, List<int[]> identifiers) {
            this.sourceFO = sourceFO;
            this.simple2ImportFQN = simple2ImportFQN;
            this.identifiers = identifiers;
        }
    }

    public static final class JavaCutAction extends CutAction {
        @Override public void actionPerformed(final ActionEvent evt, final JTextComponent target) {
            Document doc = target.getDocument();
            JavaSource js = JavaSource.forDocument(doc);
            final Object lock = new Object();
            final AtomicBoolean cancel = new AtomicBoolean();
            final AtomicBoolean alreadyRunning = new AtomicBoolean();

            if (js != null) {
                Task<CompilationController> work = new Task<CompilationController>() {
                     @Override public void run(CompilationController parameter) throws Exception {
                         synchronized (lock) {
                             if (cancel.get()) return;
                             alreadyRunning.set(true);
                         }

                         try {
                             target.putClientProperty(RUN_SYNCHRONOUSLY, true);

                             JavaCutAction.super.actionPerformed(evt, target);
                         } finally {
                             target.putClientProperty(RUN_SYNCHRONOUSLY, null);
                         }
                     }
                };

                if (target.getClientProperty(RUN_SYNCHRONOUSLY) == null) {
                    if (!DocumentUtilities.isWriteLocked(doc)) {
                        boolean finished = runQuickly(js, work);

                        if (finished)
                            return;
                    }
                } else {
                    try {
                        js.runUserActionTask(work, true);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }

                    return;
                }
            }

            synchronized (lock) {
                if (alreadyRunning.get()) return;
                cancel.set(true);
            }

            try {
                target.putClientProperty(NO_IMPORTS, true);

                super.actionPerformed(evt, target);
            } finally {
                target.putClientProperty(NO_IMPORTS, null);
            }
        }
    }

    @EditorActionRegistrations({
        @EditorActionRegistration(name = BaseKit.cutToLineBeginAction, mimeType="text/x-java"),
        @EditorActionRegistration(name = BaseKit.cutToLineEndAction, mimeType="text/x-java")
    })
    public static class JavaCutToLineBeginOrEndAction extends CutToLineBeginOrEndAction {
        @Override
        public void actionPerformed(final ActionEvent evt, final JTextComponent target) {
            Document doc = target.getDocument();
            JavaSource js = JavaSource.forDocument(doc);
            final Object lock = new Object();
            final AtomicBoolean cancel = new AtomicBoolean();
            final AtomicBoolean alreadyRunning = new AtomicBoolean();

            if (js != null && !DocumentUtilities.isWriteLocked(doc)) {
                boolean finished = runQuickly(js, new Task<CompilationController>() {

                    @Override
                    public void run(CompilationController parameter) throws Exception {
                        synchronized (lock) {
                            if (cancel.get()) {
                                return;
                            }
                            alreadyRunning.set(true);
                        }

                        try {
                            target.putClientProperty(RUN_SYNCHRONOUSLY, true);

                            JavaCutToLineBeginOrEndAction.super.actionPerformed(evt, target);
                        } finally {
                            target.putClientProperty(RUN_SYNCHRONOUSLY, null);
                        }
                    }
                });

                if (finished) {
                    return;
                }
            }

            synchronized (lock) {
                if (alreadyRunning.get()) {
                    return;
                }
                cancel.set(true);
            }

            try {
                target.putClientProperty(NO_IMPORTS, true);

                super.actionPerformed(evt, target);
            } finally {
                target.putClientProperty(NO_IMPORTS, null);
            }
        }
    }
}