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

package org.netbeans.modules.web.javascript.debugger.eval.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.Popup;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.netbeans.modules.web.javascript.debugger.MiscEditorUtil;
import org.netbeans.modules.web.javascript.debugger.locals.VariablesModel.ScopedRemoteObject;
import org.netbeans.modules.web.javascript.debugger.locals.VariablesModel.ViewScope;
import org.netbeans.modules.web.webkit.debugging.api.Debugger;
import org.netbeans.modules.web.webkit.debugging.api.debugger.CallFrame;
import org.netbeans.modules.web.webkit.debugging.api.debugger.RemoteObject;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.awt.JPopupMenuUtils;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Daniel Prusa
 */
public class CodeEvaluator extends TopComponent implements HelpCtx.Provider,
    DocumentListener, KeyListener, PropertyChangeListener, Debugger.Listener {

    /** unique ID of <code>TopComponent</code> (singleton) */
    private static final String ID = "debuggerJSevaluator"; //NOI18N
    private static final String PROP_RESULT_CHANGED = "resultChanged"; // NOI18N
    private static final String PREFERENCES_NAME = "variables_view"; // NOI18N

    final private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static WeakReference<CodeEvaluator> instanceRef;

    private JEditorPane codePane;
    // Text of the code pane, which is updated in AWT and can be read in any thread.
    // Solves the problem with calling getText() in non-AWT thread.
    private volatile String codeText = "";
    private History history;
    private Reference<Debugger> debuggerRef = new WeakReference(null);
    private DbgManagerListener dbgManagerListener;
    private TopComponent resultView;
    private Set<String> editItemsSet = new HashSet<String>();
    private ArrayList<String> editItemsList = new ArrayList<String>();
    private JButton dropDownButton;

    private Preferences preferences = NbPreferences.forModule(ContextProvider.class).node(PREFERENCES_NAME);

    private HistoryRecord lastEvaluationRecord = null;
    private ScopedRemoteObject result;
    private static RequestProcessor rp = new RequestProcessor("Debugger Evaluator", 1);  // NOI18N
    private RequestProcessor.Task evalTask = rp.create(new EvaluateTask());


    /** Creates new form CodeEvaluator */
    public CodeEvaluator() {
        initComponents();
        codePane = new JEditorPane();
        codePane.setMinimumSize(new Dimension(0,0));
        history = new History();

        dropDownButton = createDropDownButton();

        javax.swing.GroupLayout rightPanelLayout = new javax.swing.GroupLayout(rightPanel);
        rightPanel.setLayout(rightPanelLayout);
        rightPanelLayout.setHorizontalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(dropDownButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 2, 2))
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 2, 2)
                .addComponent(evaluateButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 2, 2))
        );
        rightPanelLayout.setVerticalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rightPanelLayout.createSequentialGroup()
                .addComponent(dropDownButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(evaluateButton))
        );
        
        //setupContext();
        editorScrollPane.setViewportView(codePane);
        codePane.addKeyListener(this);
        dbgManagerListener = new DbgManagerListener (this);
        DebuggerManager.getDebuggerManager().addDebuggerListener(
                DebuggerManager.PROP_CURRENT_SESSION,
                dbgManagerListener
        );
        checkDebuggerState();
    }

    public static RequestProcessor getRequestProcessor() {
        return rp;
    }

    public void pasteExpression(String expr) {
        codePane.setText(expr);
        codeText = expr;
        if (!isOpened()) {
            open();
        }
        requestActive();
    }

    @NbBundle.Messages("CTL_Expressions_Dropdown_tooltip=Show lastly evaluated code expressions")
    private JButton createDropDownButton() {
        Icon icon = ImageUtilities.loadImageIcon("org/netbeans/modules/web/javascript/debugger/resources/drop_down_arrow.png", false);
        final JButton button = new DropDownButton();
        button.setIcon(icon);
        String tooltipText = Bundle.CTL_Expressions_Dropdown_tooltip();
        button.setToolTipText(tooltipText);
        button.setEnabled(false);
        Dimension size = new Dimension(icon.getIconWidth() + 3, icon.getIconHeight() + 2);
        button.setPreferredSize(size);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusable(false);
        AbstractAction action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ("pressed".equals(e.getActionCommand())) {
                    JComponent jc = (JComponent) e.getSource();
                    createEditItemsPopup().show(jc, 0, jc.getHeight());
                    //Other portion of issue 37487, looks funny if the
                    //button becomes pressed
                    if (jc instanceof AbstractButton) {
                        AbstractButton jb = (AbstractButton) jc;
                        jb.getModel().setPressed(false);
                        jb.getModel().setRollover(false);
                        jb.getModel().setArmed(false);
                        jb.repaint();
                    }
                }
            } // actionPerformed

            @Override
            public boolean isEnabled() {
                return !editItemsList.isEmpty();
            }

        };
        action.putValue(Action.SMALL_ICON, icon);
        action.putValue(Action.SHORT_DESCRIPTION, tooltipText);
        button.setAction(action);
        return button;
    }

    private RequestProcessor.Task setupContextTask;

    private void setupContext() {
        if (setupContextTask == null) {
            setupContextTask = rp.create(new Runnable() {
                @Override
                public void run() {
                    setupContextLazily();
                }
            });
        }
        // Setting up a context takes time.
        setupContextTask.schedule(500);
    }

    private void setupContextLazily() {
        final String text = codeText;
        final Document[] documentPtr = new Document[] { null };

        class ContextUpdated implements ActionListener, Runnable {
            @Override
            public void actionPerformed(ActionEvent e) {
                run();
            }
            @Override
            public void run() {
                if (codePane.getDocument() != documentPtr[0]) {
                    codePane.getDocument().addDocumentListener(CodeEvaluator.this);
                    if (text != null) {
                        codePane.setText(text);
                    }
                }
                documentPtr[0] = codePane.getDocument();
            }
        }

        ContextUpdated contextUpdated = new ContextUpdated();
        MiscEditorUtil.setupContext(codePane, contextUpdated);
        SwingUtilities.invokeLater(contextUpdated);
    }

    private JPopupMenu createEditItemsPopup() {
        JPopupMenu popup = new JPopupMenu();
        ActionListener editItemAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem mi = (JMenuItem) e.getSource();
                String text = mi.getText();
                codePane.setText(text);
                codeText = text;
            }
        };
        for (String item : editItemsList) {
            JMenuItem editItem = new JMenuItem(item);
            editItem.addActionListener(editItemAction);
            popup.add(editItem);
        }
        return popup;
    }
    
    public void recomputeDropDownItems() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for (String str : editItemsList) {
                    StringTokenizer tok = new StringTokenizer(str, "\n"); // NOI18N
                    String dispName = "";
                    while (dispName.trim().length() == 0 && tok.hasMoreTokens()) {
                        dispName = tok.nextToken();
                    }
                }
                dropDownButton.setEnabled(!editItemsList.isEmpty());
            }
        });
    }

    public static synchronized CodeEvaluator getInstance() {
        CodeEvaluator instance = (CodeEvaluator) WindowManager.getDefault().findTopComponent(ID);
        if (instance == null) {
            instance = new CodeEvaluator();
        }
        return instance;
    }

    private static CodeEvaluator getDefaultInstance() {
        CodeEvaluator evaluator = instanceRef != null ? instanceRef.get() : null;
        if (evaluator != null) {
            return evaluator;
        }
        final CodeEvaluator result[] = new CodeEvaluator[1];
        if (SwingUtilities.isEventDispatchThread()) {
            result[0] = getInstance();
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        result[0] = getInstance();
                    }
                });
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        instanceRef = new WeakReference(result[0]);
        return result[0];
    }

    public static ArrayList<History.Item> getHistory() {
        CodeEvaluator defaultInstance = getDefaultInstance();
        return defaultInstance != null ? defaultInstance.history.getItems() : new ArrayList<History.Item>();
    }

    public static ScopedRemoteObject getResult() {
        CodeEvaluator defaultInstance = getDefaultInstance();
        return defaultInstance != null ? defaultInstance.result : null;
    }

    public static String getExpressionText() {
        CodeEvaluator defaultInstance = getDefaultInstance();
        HistoryRecord rec = defaultInstance != null ? defaultInstance.lastEvaluationRecord : null;
        return rec != null ? rec.expr : "";
    }

    public static void addResultListener(final PropertyChangeListener listener) {
        rp.post(new Runnable() {
            @Override
            public void run() {
                CodeEvaluator defaultInstance = getDefaultInstance();
                if (defaultInstance != null) {
                    synchronized(defaultInstance.pcs) {
                        defaultInstance.pcs.addPropertyChangeListener(listener);
                    }
                }
            }
        });
    }

    public static void removeResultListener(final PropertyChangeListener listener) {
        rp.post(new Runnable() {
            @Override
            public void run() {
                CodeEvaluator defaultInstance = getDefaultInstance();
                if (defaultInstance != null) {
                    synchronized(defaultInstance.pcs) {
                        defaultInstance.pcs.removePropertyChangeListener(listener);
                    }
                }
            }
        });
    }

    private static void fireResultChange() {
        rp.post(new Runnable() {
            @Override
            public void run() {
                CodeEvaluator defaultInstance = getDefaultInstance();
                if (defaultInstance != null) {
                    synchronized (defaultInstance.pcs) {
                        defaultInstance.pcs.firePropertyChange(PROP_RESULT_CHANGED, null, null);
                    }
                }
            }
        });
    }

    private synchronized void checkDebuggerState() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DebuggerEngine de = DebuggerManager.getDebuggerManager().getCurrentEngine();
                Debugger debugger = null;
                if (de != null) {
                    debugger = de.lookupFirst(null, Debugger.class);
                }
                Debugger lastDebugger = debuggerRef.get();
                if (debugger != lastDebugger) {
                    setupContext();
                }
                if (lastDebugger != null && debugger != lastDebugger) {
                    lastDebugger.removePropertyChangeListener(CodeEvaluator.this);
                    lastDebugger.removeListener(CodeEvaluator.this);
                    debuggerRef = new WeakReference(null);
                    displayResult(null);
                }
                if (debugger != null) {
                    debuggerRef = new WeakReference(debugger);
                    debugger.addListener(CodeEvaluator.this);
                    debugger.addPropertyChangeListener(CodeEvaluator.this);
                } else {
                    history.clear();
                }
                computeEvaluationButtonState();
            }
        });
    }

    private void computeEvaluationButtonState() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    computeEvaluationButtonState();
                }
            });
            return;
        }
        Debugger debugger = debuggerRef.get();
        boolean isEnabled = debugger != null && debugger.isEnabled() &&
                debugger.isSuspended() && debugger.getCurrentCallFrame() != null &&
                codePane.getDocument().getLength() > 0 &&
                editorScrollPane.getViewport().getView() == codePane;
        evaluateButton.setEnabled(isEnabled);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        DebuggerManager.getDebuggerManager().removeDebuggerListener(
                DebuggerManager.PROP_CURRENT_ENGINE,
                dbgManagerListener);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        evaluateButton = new javax.swing.JButton();
        editorScrollPane = new javax.swing.JScrollPane();
        separatorPanel = new javax.swing.JPanel();
        rightPanel = new javax.swing.JPanel();

        evaluateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/web/javascript/debugger/resources/evaluate.png"))); // NOI18N
        evaluateButton.setToolTipText(org.openide.util.NbBundle.getMessage(CodeEvaluator.class, "HINT_Evaluate_Button")); // NOI18N
        evaluateButton.setEnabled(false);
        evaluateButton.setPreferredSize(new java.awt.Dimension(38, 22));
        evaluateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                evaluateButtonActionPerformed(evt);
            }
        });

        editorScrollPane.setBorder(null);

        separatorPanel.setBackground(javax.swing.UIManager.getDefaults().getColor("Separator.foreground"));
        separatorPanel.setMaximumSize(new java.awt.Dimension(1, 32767));
        separatorPanel.setMinimumSize(new java.awt.Dimension(1, 10));
        separatorPanel.setPreferredSize(new java.awt.Dimension(1, 10));

        rightPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        rightPanel.setPreferredSize(new java.awt.Dimension(48, 0));

        javax.swing.GroupLayout rightPanelLayout = new javax.swing.GroupLayout(rightPanel);
        rightPanel.setLayout(rightPanelLayout);
        rightPanelLayout.setHorizontalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 48, Short.MAX_VALUE)
        );
        rightPanelLayout.setVerticalGroup(
            rightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 22, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(editorScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(separatorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(rightPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editorScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
            .addComponent(separatorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
            .addComponent(rightPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void evaluateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_evaluateButtonActionPerformed
        evaluate();
    }//GEN-LAST:event_evaluateButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane editorScrollPane;
    private javax.swing.JButton evaluateButton;
    private javax.swing.JPanel rightPanel;
    private javax.swing.JPanel separatorPanel;
    // End of variables declaration//GEN-END:variables

    public static void openEvaluator() {
        String selectedText = null;
        JEditorPane editor = EditorContextDispatcher.getDefault().getMostRecentEditor();
        if (editor != null) {
            selectedText = editor.getSelectedText();
        }
        CodeEvaluator evaluator = getInstance();
        evaluator.open ();
        if (selectedText != null) {
            evaluator.codePane.setText(selectedText);
            evaluator.codeText = selectedText;
        }
        evaluator.codePane.selectAll();
        evaluator.requestActive ();
    }

    @Override
    public boolean requestFocusInWindow() {
        codePane.requestFocusInWindow(); // [TODO]
        return super.requestFocusInWindow();
    }

    @Override
    protected String preferredID() {
        return this.getClass().getName();
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_ALWAYS;
    }

    @NbBundle.Messages("CTL_Code_Evaluator_name=Evaluate Code")
    @Override
    public String getName() {
        return Bundle.CTL_Code_Evaluator_name();
    }

    @NbBundle.Messages("CTL_Code_Evaluator_tooltip=Evaluate Code")
    @Override
    public String getToolTipText() {
        return Bundle.CTL_Code_Evaluator_tooltip();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new org.openide.util.HelpCtx("EvaluateCode"); // NOI18N
    }

    // ..........................................................................

    public String getExpression() {
        return codeText;
    }

    public void evaluate() {
        evalTask.schedule(10);
    }

    private void displayResult(ScopedRemoteObject var) {
        this.result = var;
        if (var == null) {
            fireResultChange();
            return ;
        }
        //DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(var.getValue()));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (preferences.getBoolean("show_evaluator_result", true)) {
                    TopComponent view = WindowManager.getDefault().findTopComponent("localsView"); // NOI18N [TODO]
                    view.open();
                    view.requestActive();
                } else {
                    if (resultView == null) {
                        resultView = getResultViewInstance();
                    }
                    if (result != null) {
                        resultView.open();
                        resultView.requestActive();
                    }
                }
                getInstance().requestActive();
                fireResultChange();
            }
        });
    }

    private void addResultToHistory(final String expr, ScopedRemoteObject result) {
        if (lastEvaluationRecord != null) {
            history.addItem(lastEvaluationRecord.expr, lastEvaluationRecord.type,
                    lastEvaluationRecord.value, lastEvaluationRecord.toString);
        }
        if (result != null) { // 'result' can be null if debugger finishes
            String type = result.getRemoteObject().getClassName();
            String value = result.getRemoteObject().getDescription();
            lastEvaluationRecord = new HistoryRecord(expr, type, value, value);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String expr2 = expr.trim();
                if (editItemsSet.contains(expr2)) {
                    editItemsList.remove(expr2);
                    editItemsList.add(0, expr2);
                } else {
                    editItemsList.add(0, expr2);
                    editItemsSet.add(expr2);
                    if (editItemsList.size() > 20) { // [TODO] constant
                        String removed = editItemsList.remove(editItemsList.size() - 1);
                        editItemsSet.remove(removed);
                    }
                }
                recomputeDropDownItems();
            }
        });
    }

    // KeyListener implementation ..........................................

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
            e.consume();
            if (debuggerRef.get() != null) {
                evaluate();
            }
        }
//        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
//            e.consume();
//            close();
//        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    // DocumentListener implementation ..........................................

    @Override
    public void insertUpdate(DocumentEvent e) {
        updateWatch();
        codeText = codePane.getText();
    }

    // DocumentListener
    @Override
    public void removeUpdate(DocumentEvent e) {
        updateWatch();
        codeText = codePane.getText();
    }

    // DocumentListener
    @Override
    public void changedUpdate(DocumentEvent e) {
        updateWatch();
        codeText = codePane.getText();
    }

    private void updateWatch() {
        // Update this LAZILY to prevent from deadlocks!
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                computeEvaluationButtonState();
            }
        });
    }

    // PropertyChangeListener on current thread .................................

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (Debugger.PROP_CURRENT_FRAME.equals(propertyName)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    synchronized (CodeEvaluator.this) {
                        Debugger debugger = debuggerRef.get();
                        if (debugger != null) {
                            computeEvaluationButtonState();
                            setupContext();
                        }
                    }
                }
            });
            /*
        } else if (JPDADebugger.PROP_STATE.equals(propertyName)) {
            synchronized (this) {
                JPDADebugger debugger = debuggerRef.get();
                if (debugger != null && debugger.getState() != JPDADebugger.STATE_STOPPED) {
                    if (result != null) {
                        history.addItem(lastEvaluationRecord.expr, lastEvaluationRecord.type,
                            lastEvaluationRecord.value, lastEvaluationRecord.toString);
                        lastEvaluationRecord = null;
                        result = null;
                        fireResultChange();
                    } // if
                } // if
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JPDADebugger debugger = debuggerRef.get();
                        if (debugger != null && debugger.getState() == JPDADebugger.STATE_STOPPED) {
                            setupContext();
                        }
                        computeEvaluationButtonState();
                    }
                });
            } // synchronized
            */
        }
    }

    @Override
    public void paused(List<CallFrame> callStack, String reason) {
        setupContext();
        computeEvaluationButtonState();
    }

    @Override
    public void resumed() {
        synchronized (this) {
            if (result != null) {
                history.addItem(lastEvaluationRecord.expr, lastEvaluationRecord.type,
                    lastEvaluationRecord.value, lastEvaluationRecord.toString);
                lastEvaluationRecord = null;
                result = null;
                fireResultChange();
            }
        }
        computeEvaluationButtonState();
    }

    @Override
    public void reset() {
        resumed();
    }

    // ..........................................................................

    private synchronized TopComponent getResultViewInstance() {
        /** unique ID of <code>TopComponent</code> (singleton) */
        TopComponent instance = WindowManager.getDefault().findTopComponent("resultsView"); // NOI18N [TODO]
        return instance;
    }

    // History ..................................................................

    public static class History {

        private static final int MAX_ITEMS = 100;

        private ArrayList<Item> historyItems = new ArrayList<Item>();

        private void addItem(String expr, String type, String value, String toString) {
            Item item = new Item(expr, type, value, toString);
            historyItems.add(0, item);
            if (historyItems.size() > MAX_ITEMS) {
                historyItems.remove(MAX_ITEMS);
            }
        }

        public ArrayList<Item> getItems() {
            return historyItems;
        }

        public void clear() {
            historyItems.clear();
        }

        public class Item {
            public String expr;
            public String type;
            public String value;
            public String toString;
            public String tooltip;
            public String exprFormatted;

            Item(String expr, String type, String value, String toString) {
                this.expr = expr;
                this.type = type;
                this.value = value;
                this.toString = toString;
                StringBuilder buf = new StringBuilder();
                buf.append("<html>");
                String text = expr.replaceAll ("&", "&amp;");
                text = text.replaceAll ("<", "&lt;");
                text = text.replaceAll (">", "&gt;");
                text = text.replaceAll ("\n", "<br/>");
                text = text.replaceAll ("\r", "");
                buf.append(text);
                buf.append("</html>");
                this.tooltip = buf.toString();
            }

            @Override
            public String toString() {
                return expr;
            }
        }

    }

    // EvaluateTask .............................................................

    private class EvaluateTask implements Runnable {
        @Override
        public void run() {
            String exp = getExpression();
            if (exp == null || "".equals(exp)) {
                //System.out.println("Can not evaluate '"+exp+"'");
                return ;
            }
            //System.out.println("evaluate: '"+exp+"'");
            //try {
                Debugger debugger = debuggerRef.get();
                if (debugger != null) {
                    CallFrame frame = debugger.getCurrentCallFrame();
                    if (frame != null) {
                        RemoteObject ro = frame.evaluate(exp);
                        ScopedRemoteObject var;
                        if (ro != null) {
                            var = new ScopedRemoteObject(ro, exp, ViewScope.LOCAL);
                        } else {
                            var = null;
                        }
                        addResultToHistory(exp, var);
                        displayResult(var);
                    }
                }
                /*
            } catch (InvalidExpressionException ieex) {
                String message = ieex.getLocalizedMessage();
                Throwable t = ieex.getTargetException();
                if (t != null && t instanceof org.omg.CORBA.portable.ApplicationException) {
                    java.io.StringWriter s = new java.io.StringWriter();
                    java.io.PrintWriter p = new java.io.PrintWriter(s);
                    t.printStackTrace(p);
                    p.close();
                    message += " \n" + s.toString();
                }
                DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message(message));
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        //evalDialog.requestFocus();
                        codePane.requestFocusInWindow();
                    }
                });
            }
                */
        }
    }

    private static class DbgManagerListener extends DebuggerManagerAdapter {

        private Reference<CodeEvaluator> codeEvaluatorRef;

        public DbgManagerListener(CodeEvaluator evaluator) {
            codeEvaluatorRef = new WeakReference<CodeEvaluator>(evaluator);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            CodeEvaluator evaluator = (CodeEvaluator) codeEvaluatorRef.get();
            if (evaluator != null) {
                evaluator.checkDebuggerState();
            }
        }

    }

    private static class DropDownButton extends JButton {

        @Override
        protected void processMouseEvent(MouseEvent me) {
            super.processMouseEvent(me);
            if (isEnabled() && me.getID() == MouseEvent.MOUSE_PRESSED) {
                getAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "pressed"));
            }
        }

        protected String getTabActionCommand(ActionEvent e) {
            return null;
        }

        void performAction( ActionEvent e ) {
        }

    }

    private static class HistoryRecord {
        String expr;
        String type;
        String value;
        String toString;

        HistoryRecord(String expr, String type, String value, String toString) {
            this.expr = expr;
            this.type = type;
            this.value = value;
            this.toString = toString;
        }
    }

}
