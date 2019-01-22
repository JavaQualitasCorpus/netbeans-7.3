/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.git.ui.fetch;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.libs.git.GitBranch;
import org.netbeans.modules.git.client.GitClient;
import org.netbeans.libs.git.GitException;
import org.netbeans.libs.git.GitRemoteConfig;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.client.GitClientExceptionHandler;
import org.netbeans.modules.git.client.GitProgressSupport;
import org.netbeans.modules.git.ui.repository.RepositoryInfo;
import org.netbeans.modules.git.ui.selectors.ItemSelector;
import org.netbeans.modules.git.ui.wizards.AbstractWizardPanel;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;

/**
 *
 * @author ondra
 */
public class FetchBranchesStep extends AbstractWizardPanel implements WizardDescriptor.FinishablePanel<WizardDescriptor>, ChangeListener {
    private String fetchUri;
    private GitRemoteConfig remote;
    private GitProgressSupport supp;
    private GitProgressSupport validatingSupp;
    private final Mode mode;
    private final File repository;
    private final ItemSelector<BranchMapping> branches;

    public static enum Mode {
        ACCEPT_EMPTY_SELECTION,
        ACCEPT_NON_EMPTY_SELECTION_ONLY
    }

    public FetchBranchesStep (File repository, Mode mode) {
        this.mode = mode;
        this.repository = repository;
        this.branches = new ItemSelector<BranchMapping>(NbBundle.getMessage(FetchBranchesStep.class,"FetchBranchesPanel.jLabel1.text"));
        this.branches.addChangeListener(this);
        Mutex.EVENT.readAccess(new Runnable() {
            @Override
            public void run () {
                validateBeforeNext();
            }
        });
        getJComponent().setName(NbBundle.getMessage(FetchBranchesStep.class, "LBL_FetchBranches.remoteBranches")); //NOI18N
    }
    
    @Override
    protected final void validateBeforeNext () {
        setValid(true, null);
        boolean acceptEmptySelection = mode == Mode.ACCEPT_EMPTY_SELECTION;
        if (!acceptEmptySelection && branches.getSelectedBranches().isEmpty()) {
            setValid(false, new Message(NbBundle.getMessage(FetchBranchesStep.class, "MSG_FetchBranchesPanel.errorNoBranchSelected"), true)); //NOI18N
        } else if (acceptEmptySelection && branches.isEmpty()) {
            setValid(true, new Message(NbBundle.getMessage(FetchBranchesStep.class, "MSG_FetchBranchesPanel.errorNoBranch"), true)); //NOI18N
        } else {
            setValid(true, null);
        }
    }

    @Override
    protected final JComponent getJComponent () {
        return branches.getPanel();
    }
    
    @Override
    public HelpCtx getHelp() {
        return new HelpCtx(FetchBranchesStep.class);
    }

    public void setFetchUri (String fetchUri, boolean loadRemoteBranches) {
        if (fetchUri != null && !fetchUri.equals(this.fetchUri) || fetchUri == null && this.fetchUri != null) {
            this.fetchUri = fetchUri;
            if (loadRemoteBranches) {
                refreshRemoteBranches();
            }
        }
    }

    public void setRemote (GitRemoteConfig remote) {
        if (this.remote != remote && (this.remote == null || remote == null)) {
            this.remote = remote;
        }
        validateBeforeNext();
    }

    public void fillRemoteBranches (final Map<String, GitBranch> branches) {
        if (repository == null) {
            fillRemoteBranches(branches, Collections.<String, GitBranch>emptyMap());
        } else {
            new GitProgressSupport.NoOutputLogging() {
                @Override
                protected void perform () {
                    final Map<String, GitBranch> localBranches = new HashMap<String, GitBranch>();
                    RepositoryInfo info = RepositoryInfo.getInstance(repository);
                    info.refresh();
                    localBranches.putAll(info.getBranches());
                    EventQueue.invokeLater(new Runnable () {
                        @Override
                        public void run () {
                            fillRemoteBranches(branches, localBranches);
                        }
                    });
                }
            }.start(Git.getInstance().getRequestProcessor(repository), repository, NbBundle.getMessage(FetchBranchesStep.class, "MSG_FetchBranchesPanel.loadingLocalBranches")); //NOI18N
        }
    }

    private void fillRemoteBranches (Map<String, GitBranch> branches, Map<String, GitBranch> localBranches) {
        List<BranchMapping> l = new ArrayList<BranchMapping>(branches.size());
        for (GitBranch branch : branches.values()) {
            GitBranch localBranch = localBranches.get(remote.getRemoteName() + "/" + branch.getName());
            boolean preselected = localBranch != null && !localBranch.getId().equals(branch.getId());
            l.add(new BranchMapping(branch.getName(), branch.getId(), localBranch, remote, preselected));
        }
        this.branches.setBranches(l);
        validateBeforeNext();
    }
    
    @Override
    public void stateChanged(ChangeEvent ce) {
        validateBeforeNext();
    }

    private void refreshRemoteBranches () {
        assert EventQueue.isDispatchThread();
        cancelBackgroundTasks();
        DefaultListModel model = new DefaultListModel();
        branches.setBranches(new ArrayList<BranchMapping>(0));
        if (fetchUri != null) {
            final String uri = fetchUri;
            model.addElement(NbBundle.getMessage(FetchBranchesStep.class, "MSG_FetchBranchesPanel.loadingBranches")); //NOI18N
            branches.setEnabled(false);
            Utils.post(new Runnable() {
                @Override
                public void run () {
                    final File tempRepository = Utils.getTempFolder();
                    supp = new GitProgressSupport.NoOutputLogging() {
                        @Override
                        protected void perform () {
                            final Map<String, GitBranch> branches = new HashMap<String, GitBranch>();
                            final Map<String, GitBranch> localBranches = new HashMap<String, GitBranch>();
                            try {
                                GitClient client = getClient();
                                client.init(getProgressMonitor());
                                branches.putAll(client.listRemoteBranches(uri, getProgressMonitor()));
                                if (repository != null) {
                                    RepositoryInfo info = RepositoryInfo.getInstance(repository);
                                    info.refresh();
                                    localBranches.putAll(info.getBranches());
                                }
                            } catch (GitException ex) {
                                GitClientExceptionHandler.notifyException(ex, true);
                            } finally {
                                Utils.deleteRecursively(tempRepository);
                                final GitProgressSupport supp = this;
                                EventQueue.invokeLater(new Runnable () {
                                    @Override
                                    public void run () {
                                        if (!supp.isCanceled()) {
                                            fillRemoteBranches(branches, localBranches);
                                        }
                                    }
                                });
                            }
                        }
                    };
                    supp.start(Git.getInstance().getRequestProcessor(tempRepository), tempRepository, NbBundle.getMessage(FetchBranchesStep.class, "MSG_FetchBranchesPanel.loadingBranches")); //NOI18N
                }
            });
        }
    }

    public void cancelBackgroundTasks () {
        if (supp != null) {
            supp.cancel();
        }
        if (validatingSupp != null) {
            validatingSupp.cancel();
        }
    }

    public List<String> getSelectedRefSpecs () {
        List<String> specs = new LinkedList<String>();
        for (BranchMapping b : branches.getSelectedBranches()) {
            specs.add(b.getRefSpec());
        }
        return specs;
    }

    @Override
    public boolean isFinishPanel () {
        return true;
    }
}