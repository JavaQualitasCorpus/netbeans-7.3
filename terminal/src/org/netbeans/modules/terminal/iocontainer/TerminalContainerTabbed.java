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
package org.netbeans.modules.terminal.iocontainer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.terminal.ioprovider.Terminal;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.InputLine;
import org.openide.awt.MouseUtils;

import org.openide.awt.TabbedPaneFactory;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 * Corresponds to core.io.ui...IOWindow.
 * @author ivan
 */
final public class TerminalContainerTabbed extends TerminalContainerCommon {

    private JTabbedPane tabbedPane;
    private JComponent soleComponent;
    private JPopupMenu pop;
    private PopupListener popL;
    
    public TerminalContainerTabbed(TopComponent owner, String originalName) {
        super(owner, originalName);
        initComponents();
    }

    @Override
    protected void initComponents() {
	super.initComponents();

        tabbedPane = TabbedPaneFactory.createCloseButtonTabbedPane();
        tabbedPane.addPropertyChangeListener(new PropertyChangeListener() {

	    @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(TabbedPaneFactory.PROP_CLOSE)) {
                    JComponent comp = (JComponent) evt.getNewValue();
		    remove(comp);
                }
            }
        });
        tabbedPane.addChangeListener(new ChangeListener() {

	    @Override
            public void stateChanged(ChangeEvent e) {
		checkSelectionChange();
            }
        });
	
	pop = new JPopupMenu();
	pop.add(new Close());
	pop.add(new CloseAll());
	pop.add(new CloseAllButCurrent());
	pop.add(new JSeparator());
	pop.add(new SetTitleAction());
	popL = new PopupListener();

	tabbedPane.addMouseListener(popL);
    }

    @Override
    protected boolean contains (JComponent comp) {
	return soleComponent == comp ||
	       tabbedPane.indexOfComponent(comp) != -1;
    }

    /**
     * Restore attributes that are maintained by the tabbedPane.
     *
     * Called when a component is added to a tabbed pane.
     * No need to do anything (i.e. save) when we remove a component.
     *
     * Also called on individual attribute settings like
     * setIcon(JComponent, Icon). Note that this method is overkill
     * for this purpose. I.e. it will set title etc as well.
     * If this ever becomes an issue we can pass a mask to control
     * what exactly gets restored.
     * @param comp
     */
    @Override
    protected void restoreAttrsFor(JComponent comp) {
	int index = tabbedPane.indexOfComponent(comp);
	if (index == -1)
	    return;

	Attributes attrs = attributesFor(comp);

	tabbedPane.setTitleAt(index, attrs.title);

	tabbedPane.setIconAt(index, attrs.icon);
	tabbedPane.setDisabledIconAt(index, attrs.icon);

	// output2 "stores" toolTipText as the components
	// attribute
	tabbedPane.setToolTipTextAt(index, attrs.toolTipText);
    }

    @Override
    final protected void addTabWork(JComponent comp) {
	if (soleComponent != null) {
	    // only single tab, remove it from TopComp. and add it to tabbed pane
	    assert tabbedPane.getParent() == null;
	    assert tabbedPane.getTabCount() == 0;
	    componentRemove(soleComponent);
	    super.add(tabbedPane);
	    tabbedPane.add(soleComponent);
	    restoreAttrsFor(soleComponent);
	    soleComponent = null;
	    updateWindowName(null);

	    // Add the window we're adding
	    tabbedPane.add(comp);
	    restoreAttrsFor(comp);


	} else if (tabbedPane.getTabCount() > 0) {
	    // already several tabs
	    assert tabbedPane.getParent() != null;
	    assert soleComponent == null;
	    tabbedPane.add(comp);
	    restoreAttrsFor(comp);

	} else {
	    // nothing yet
	    assert tabbedPane.getParent() == null;
	    assert soleComponent == null;
	    setFocusable(false);
	    soleComponent = comp;
	    super.add(comp);
	    updateWindowName(soleComponent.getName());
	    // for first component we act as if select was called
	    checkSelectionChange();
	}

	revalidate();
    }

    @Override
    final protected void removeTabWork(final JComponent comp) {
	if (soleComponent != null) {
	    // removing the last one
	    assert soleComponent == comp;
	    componentRemove(soleComponent);
	    soleComponent = null;
	    updateWindowName(null);
	    checkSelectionChange();
	    setFocusable(true);
	    revalidate();
	    repaint();	// otherwise term will still stay in view

	} else if (tabbedPane.getParent() == this) {
	    assert tabbedPane.getTabCount() > 1;
	    tabbedPane.remove(comp);
	    if (tabbedPane.getTabCount() == 1) {
		//  switch to no tabbed pane
		soleComponent  = (JComponent) tabbedPane.getComponentAt(0);
		tabbedPane.remove(soleComponent);
		componentRemove(tabbedPane);
		super.add(soleComponent);
		updateWindowName(soleComponent.getName());
	    }
	    checkSelectionChange();
	    revalidate();
	}
    }

    //
    // Overrides of TerminalContainer
    //

    @Override
    protected void selectLite(JComponent comp) {
        if (soleComponent == null) {
	    // will call checkSelectionChange() via stateChanged()
            tabbedPane.setSelectedComponent(comp);
	} else {
	    checkSelectionChange();
	}
    }

    @Override
    public JComponent getSelected() {
        if (soleComponent != null)
            return soleComponent;
        else
            return (JComponent) tabbedPane.getSelectedComponent();
    }

    @Override
    public void setTitleWork(JComponent comp, String title) {
	// pass-through for currently visible component
	// SHOULD see if the following logic can be applied generically
	// after addTab() or removeTab()
        if (soleComponent != null) {
	    assert soleComponent == comp;
	    updateWindowName(title);
        } else {
	    assert tabbedPane.getParent() == this;
	    updateWindowName(null);
	    // write thru
	    restoreAttrsFor(comp);
        }
    }
    
    private void closeAll(boolean butCurrent) {
	if (soleComponent == null) {
	    Component current = tabbedPane.getSelectedComponent();
	    Component[] c = tabbedPane.getComponents();
	    for (int i = 0; i < c.length; i++) {
		if (butCurrent && c[i] == current) {
		    continue;
		}
		// remove only terminals, not scroll panes and viewports
		if (c[i] instanceof Terminal) {
		    removeTab((JComponent)c[i]);
		}
	    }
	}
    }

    private class PopupListener extends MouseUtils.PopupMouseAdapter {

	@Override
	protected void showPopup(MouseEvent e) {
	    pop.show(TerminalContainerTabbed.this, e.getX(), e.getY());
	}
    }

    private class Close extends AbstractAction {

	public Close() {
	    super(NbBundle.getMessage(TerminalContainerTabbed.class, "LBL_CloseWindow"));  //NOI18N
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    removeTab(TerminalContainerTabbed.this.getSelected());
	}
    }

    private final class CloseAll extends AbstractAction {

	public CloseAll() {
	    super(NbBundle.getMessage(TerminalContainerTabbed.class, "LBL_CloseAll"));  //NOI18N
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    closeAll(false);
	}
    }

    private class CloseAllButCurrent extends AbstractAction {

	public CloseAllButCurrent() {
	    super(NbBundle.getMessage(TerminalContainerTabbed.class, "LBL_CloseAllButCurrent"));  //NOI18N
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    closeAll(true);
	}
    }
    
    private final class SetTitleAction extends AbstractAction {

	public SetTitleAction() {
	    super(NbBundle.getMessage(TerminalContainerTabbed.class, "CTL_SetTitle"));	// NOI18N
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	    InputLine inputLine = new NotifyDescriptor.InputLine(NbBundle.getMessage(TerminalContainerTabbed.class, "LBL_Title"), NbBundle.getMessage(TerminalContainerTabbed.class, "LBL_SetTitle"));// NOI18N
	    JComponent selected = getSelected();
	    String title = selected.getName();
	    if (selected instanceof Terminal) {
		title = ((Terminal)selected).getTitle();
	    }
	    inputLine.setInputText(title);
	    if (DialogDisplayer.getDefault().notify(inputLine) == NotifyDescriptor.OK_OPTION) {
		String newTitle = inputLine.getInputText().trim();
		if (!newTitle.equals(title)) {
		    if (selected instanceof Terminal) {
			((Terminal)selected).setTitle(newTitle);
		    } else {
			setTitle(selected, newTitle);
		    }
		}
	    }

	}
    }
}