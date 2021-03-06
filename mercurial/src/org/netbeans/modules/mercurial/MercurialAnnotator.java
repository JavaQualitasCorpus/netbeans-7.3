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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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
package org.netbeans.modules.mercurial;

import org.netbeans.modules.mercurial.ui.menu.ShowMenu;
import org.netbeans.modules.mercurial.ui.menu.MergeMenu;
import org.netbeans.modules.mercurial.ui.menu.ShareMenu;
import org.netbeans.modules.mercurial.ui.menu.RecoverMenu;
import org.netbeans.modules.mercurial.ui.clone.CloneAction;
import org.netbeans.modules.versioning.spi.VCSAnnotator;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.netbeans.api.project.Project;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.netbeans.modules.versioning.util.Utils;
import javax.swing.*;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.netbeans.modules.mercurial.options.AnnotationColorProvider;
import org.netbeans.modules.mercurial.ui.add.AddAction;
import org.netbeans.modules.mercurial.ui.annotate.AnnotateAction;
import org.netbeans.modules.mercurial.ui.branch.HgBranch;
import org.netbeans.modules.mercurial.ui.commit.CommitAction;
import org.netbeans.modules.mercurial.ui.commit.ExcludeFromCommitAction;
import org.netbeans.modules.mercurial.ui.diff.DiffAction;
import org.netbeans.modules.mercurial.ui.menu.ExportMenu;
import org.netbeans.modules.mercurial.ui.diff.ImportDiffAction;
import org.netbeans.modules.mercurial.ui.ignore.IgnoreAction;
import org.netbeans.modules.mercurial.ui.log.HgLogMessage;
import org.netbeans.modules.mercurial.ui.log.LogAction;
import org.netbeans.modules.mercurial.ui.menu.BranchMenu;
import org.netbeans.modules.mercurial.ui.menu.QueuesMenu;
import org.netbeans.modules.mercurial.ui.menu.TagMenu;
import org.netbeans.modules.mercurial.ui.properties.PropertiesAction;
import org.netbeans.modules.mercurial.ui.pull.FetchAction;
import org.netbeans.modules.mercurial.ui.update.RevertModificationsAction;
import org.netbeans.modules.mercurial.ui.status.StatusAction;
import org.netbeans.modules.mercurial.ui.update.ConflictResolvedAction;
import org.netbeans.modules.mercurial.ui.update.ResolveConflictsAction;
import org.netbeans.modules.mercurial.ui.update.UpdateAction;
import org.netbeans.modules.mercurial.util.HgUtils;
import org.netbeans.modules.versioning.util.SystemActionBridge;
import org.openide.awt.Actions;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;

/**
 * Responsible for coloring file labels and file icons in the IDE and providing IDE with menu items.
 *
 * @author Maros Sandor
 */
public class MercurialAnnotator extends VCSAnnotator implements PropertyChangeListener {
    
    private static final int INITIAL_ACTION_ARRAY_LENGTH = 25;
    
    private static final int STATUS_TEXT_ANNOTABLE = 
            FileInformation.STATUS_NOTVERSIONED_EXCLUDED |
            FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY | 
            FileInformation.STATUS_VERSIONED_UPTODATE |
            FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY | 
            FileInformation.STATUS_VERSIONED_CONFLICT |
            FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY | 
            FileInformation.STATUS_VERSIONED_DELETEDLOCALLY |
            FileInformation.STATUS_VERSIONED_ADDEDLOCALLY;

    private static final Pattern lessThan = Pattern.compile("<");  // NOI18N
    
    private static final int STATUS_BADGEABLE = 
            FileInformation.STATUS_VERSIONED_UPTODATE |
            FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY |
            FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY;

    private static int STATUS_IS_IMPORTANT =
                        FileInformation.STATUS_VERSIONED_UPTODATE |
                        FileInformation.STATUS_LOCAL_CHANGE |
                        FileInformation.STATUS_NOTVERSIONED_EXCLUDED;

    public static final String ANNOTATION_STATUS      = "status";       //NOI18N
    public static final String ANNOTATION_FOLDER      = "folder";       //NOI18N

    public static String[] LABELS = new String[] {ANNOTATION_STATUS, ANNOTATION_FOLDER};
    public static final String ACTIONS_PATH_PREFIX = "Actions/Mercurial/";

    private FileStatusCache cache;
    private MessageFormat format;
    private String emptyFormat;

    private static final String badgeModified = "org/netbeans/modules/mercurial/resources/icons/modified-badge.png";
    private static final String badgeConflicts = "org/netbeans/modules/mercurial/resources/icons/conflicts-badge.png";
    private static final String toolTipModified = "<img src=\"" + MercurialAnnotator.class.getClassLoader().getResource(badgeModified) + "\">&nbsp;"
            + NbBundle.getMessage(MercurialAnnotator.class, "MSG_Contains_Modified_Locally");
    private static final String toolTipConflict = "<img src=\"" + MercurialAnnotator.class.getClassLoader().getResource(badgeConflicts) + "\">&nbsp;"
            + NbBundle.getMessage(MercurialAnnotator.class, "MSG_Contains_Conflicts");
    private static final Logger LOG = Logger.getLogger(MercurialAnnotator.class.getName());

    MercurialAnnotator(FileStatusCache cache) {
        this.cache = cache;
        initDefaults();
    }
    
    private void initDefaults() {
        refresh();
    }

    public void refresh() {
        String string = HgModuleConfig.getDefault().getAnnotationFormat();
        if (string != null && !string.trim().equals("")) { // NOI18N
            string = HgUtils.createAnnotationFormat(string);
            if (!HgUtils.isAnnotationFormatValid(string))   {
                // see #136440
                Mercurial.LOG.log(Level.WARNING, "Bad annotation format, switching to defaults");
                string = org.openide.util.NbBundle.getMessage(MercurialAnnotator.class, "MercurialAnnotator.defaultFormat"); // NOI18N
            }
            format = new MessageFormat(string);
            emptyFormat = format.format(new String[] {"", "", ""} , new StringBuffer(), null).toString().trim(); // NOI18N
        }
    }

    private static MessageFormat getFormat(String key) {
        String format = NbBundle.getMessage(MercurialAnnotator.class, key);
        return new MessageFormat(format);
    }
    
    @Override
    public String annotateName(String name, VCSContext context) {
        FileInformation mostImportantInfo = null;
        File mostImportantFile = null;
        boolean folderAnnotation = false;
                
        for (final File file : context.getRootFiles()) {
            FileInformation info = cache.getCachedStatus(file);
            int status = info.getStatus();
            if ((status & STATUS_IS_IMPORTANT) == 0) continue;
            
            if (isMoreImportant(info, mostImportantInfo)) {
                mostImportantInfo = info;
                mostImportantFile = file;
                folderAnnotation = file.isDirectory();
            }
        }
        
        if (folderAnnotation == false && context.getRootFiles().size() > 1) {
            folderAnnotation = !Utils.isFromMultiFileDataObject(context);
        }
        
        if (mostImportantInfo == null) return null;
        return folderAnnotation ?
            annotateFolderNameHtml(name, context, mostImportantInfo, mostImportantFile) :
            annotateNameHtml(name, mostImportantInfo, mostImportantFile);
    }
                
    @Override
    public Image annotateIcon(Image icon, VCSContext context) {
        boolean folderAnnotation = false;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "annotateIcon(): for {0}", new Object[] { context.getRootFiles() });
        }
        for (File file : context.getRootFiles()) {
            if (file.isDirectory()) {
                folderAnnotation = true;
                Utils.addFolderToLog(file);
                break;
            }
        }

        if (folderAnnotation == false && context.getRootFiles().size() > 1) {
            folderAnnotation = !Utils.isFromMultiFileDataObject(context);
        }
        
        if (folderAnnotation == false) {
            icon = annotateFileIcon(context, icon);
        } else {
            icon = annotateFolderIcon(context, icon);
        }
        return addToolTip(icon, context);
    }

    private Image annotateFileIcon(VCSContext context, Image icon) throws IllegalArgumentException {
        FileInformation mostImportantInfo = null;
        File mostImportantFile = null;
        for (final File file : context.getRootFiles()) {
            FileInformation info = cache.getCachedStatus(file);
            int status = info.getStatus();
            if ((status & STATUS_IS_IMPORTANT) == 0) {
                continue;
            }
            if (isMoreImportant(info, mostImportantInfo)) {
                mostImportantInfo = info;
                mostImportantFile = file;
            }
        }
        if(mostImportantInfo == null) return icon; 
        String statusText = null;
        int status = mostImportantInfo.getStatus();
        if (0 != (status & FileInformation.STATUS_NOTVERSIONED_EXCLUDED)) {
            statusText = getAnnotationProvider().EXCLUDED_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_DELETEDLOCALLY)) {
            statusText = getAnnotationProvider().DELETED_LOCALLY_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY)) {
            statusText = getAnnotationProvider().REMOVED_LOCALLY_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY)) {
            statusText = getAnnotationProvider().NEW_LOCALLY_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
            FileStatus fileStatus = mostImportantInfo.getStatus(mostImportantFile);
            if (fileStatus != null && fileStatus.isCopied()) {
                statusText = getAnnotationProvider().COPIED_LOCALLY_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
            } else {
                statusText = getAnnotationProvider().ADDED_LOCALLY_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
            }
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY)) {
            statusText = getAnnotationProvider().MODIFIED_LOCALLY_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_UPTODATE)) {
            statusText = null;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_CONFLICT)) {
            statusText = getAnnotationProvider().CONFLICT_FILE_TOOLTIP.getFormat().format(new Object[]{mostImportantInfo.getStatusText()});
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NOTMANAGED)) {
            statusText = null;
        } else if (status == FileInformation.STATUS_UNKNOWN) {
            statusText = null;
        } else {
            throw new IllegalArgumentException("Uncomparable status: " + status); // NOI18N
        }
        return statusText != null ? ImageUtilities.addToolTipToImage(icon, statusText) : icon;
    }

    private Image annotateFolderIcon(VCSContext context, Image icon) {
        boolean isVersioned = false;
        for (Iterator i = context.getRootFiles().iterator(); i.hasNext();) {
            File file = (File) i.next();
            // There is an assumption here that annotateName was already
            // called and FileStatusCache.getStatus was scheduled if
            // FileStatusCache.getCachedStatus returned null.
            FileInformation info = cache.getCachedStatus(file);
            if (info != null && (info.getStatus() & STATUS_BADGEABLE) != 0) {
                isVersioned = true;
                break;
            }
        }
        if (!isVersioned) {
            return icon;
        }
        Image badge = null;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "annotateFolderIcon(): for {0}", new Object[] { context.getRootFiles() });
        }
        if (cache.containsFileOfStatus(context, FileInformation.STATUS_VERSIONED_CONFLICT, true)) {
            badge = ImageUtilities.assignToolTipToImage(
                    ImageUtilities.loadImage(badgeConflicts, true), toolTipConflict);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "annotateFolderIcon(): contains conflict");
            }
        } else if (cache.containsFileOfStatus(context, FileInformation.STATUS_LOCAL_CHANGE, true)) {
            badge = ImageUtilities.assignToolTipToImage(
                    ImageUtilities.loadImage(badgeModified, true), toolTipModified);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "annotateFolderIcon(): contains local change");
            }
        }
        if (badge != null) {
            return ImageUtilities.mergeImages(icon, badge, 16, 9);
        } else {
            return icon;
        }
    }

    @Override
    public Action[] getActions(VCSContext ctx, VCSAnnotator.ActionDestination destination) {
        // TODO: get resource strings for all actions:
        ResourceBundle loc = NbBundle.getBundle(MercurialAnnotator.class);
        Node [] nodes = ctx.getElements().lookupAll(Node.class).toArray(new Node[0]);
        File [] files = ctx.getRootFiles().toArray(new File[ctx.getRootFiles().size()]);
        Set<File> roots = HgUtils.getRepositoryRoots(ctx);
        boolean noneVersioned = (roots == null || roots.isEmpty());
        boolean onlyFolders = onlyFolders(files);
        boolean onlyProjects = onlyProjects(nodes);

        List<Action> actions = new ArrayList<Action>(INITIAL_ACTION_ARRAY_LENGTH);
        if (destination == VCSAnnotator.ActionDestination.MainMenu) {
            // XXX use Actions.forID
            Action a = Utils.getAcceleratedAction("Actions/Mercurial/org-netbeans-modules-mercurial-ui-create-CreateAction.instance");
            if(a instanceof ContextAwareAction) {
                a = ((ContextAwareAction)a).createContextAwareInstance(Lookups.singleton(ctx));
            }            
            if(a != null) actions.add(a);
            if (noneVersioned) {
                a = (Action) Utils.getAcceleratedAction("Actions/Mercurial/org-netbeans-modules-mercurial-ui-clone-CloneExternalAction.instance");
                if(a != null) actions.add(a);
            } else {
                actions.add(null);
                actions.add(SystemAction.get(StatusAction.class));
                actions.add(SystemAction.get(DiffAction.class));
                actions.add(SystemAction.get(UpdateAction.class));
                actions.add(SystemAction.get(CommitAction.class));
                actions.add(SystemAction.get(AddAction.class));
                actions.add(null);
                actions.add(new ExportMenu());
                actions.add(SystemAction.get(ImportDiffAction.class));

                actions.add(null);
                actions.add(SystemAction.get(CloneAction.class));
                a = (Action) Utils.getAcceleratedAction("Actions/Mercurial/org-netbeans-modules-mercurial-ui-clone-CloneExternalAction.instance");
                if(a != null) actions.add(a);

                actions.add(SystemAction.get(FetchAction.class));
                actions.add(new ShareMenu());
                actions.add(new MergeMenu(false));
                actions.add(null);
                actions.add(SystemAction.get(LogAction.class));
                if (!onlyProjects  && !onlyFolders) {
                    AnnotateAction tempA = SystemAction.get(AnnotateAction.class);
                    if (tempA.visible(nodes)) {
                        actions.add(new ShowMenu(true, true));
                    } else {
                        actions.add(new ShowMenu(true, false));
                    }
                }else{
                    actions.add(new ShowMenu(false, false));
                }
                actions.add(null);
                actions.add(SystemAction.get(RevertModificationsAction.class));
                actions.add(new RecoverMenu());
                if (!onlyProjects) {
                    actions.add(SystemAction.get(IgnoreAction.class));
                }
                actions.add(null);
                actions.add(new BranchMenu(null));
                actions.add(new TagMenu(null));
                actions.add(new QueuesMenu(null));
                actions.add(null);
                actions.add(SystemAction.get(PropertiesAction.class));
            }
            Utils.setAcceleratorBindings(ACTIONS_PATH_PREFIX, actions.toArray(new Action[actions.size()]));
        } else {
            Lookup context = ctx.getElements();
            if (noneVersioned){
                Action a = Actions.forID("Mercurial", "org.netbeans.modules.mercurial.ui.create.CreateAction");
                if(a instanceof ContextAwareAction) {
                    a = ((ContextAwareAction)a).createContextAwareInstance(Lookups.singleton(ctx));
                }            
                if(a != null) actions.add(a);
            }else{
                actions.add(SystemActionBridge.createAction(SystemAction.get(StatusAction.class), loc.getString("CTL_PopupMenuItem_Status"), context)); //NOI18N
                actions.add(SystemActionBridge.createAction(SystemAction.get(DiffAction.class), loc.getString("CTL_PopupMenuItem_Diff"), context)); //NOI18N
                actions.add(SystemActionBridge.createAction(SystemAction.get(CommitAction.class), loc.getString("CTL_PopupMenuItem_Commit"), context)); //NOI18N
                actions.add(SystemActionBridge.createAction(SystemAction.get(AddAction.class), NbBundle.getMessage(AddAction.class, "CTL_PopupMenuItem_Add"), context)); //NOI18N
                actions.add(null);
                actions.add(SystemActionBridge.createAction(SystemAction.get(ResolveConflictsAction.class), loc.getString("CTL_PopupMenuItem_Resolve"), context)); //NOI18N
                if (!onlyProjects  && !onlyFolders) {
                    actions.add(SystemActionBridge.createAction(SystemAction.get(ConflictResolvedAction.class), loc.getString("CTL_PopupMenuItem_MarkResolved"), context)); // NOI18N
                }
                actions.add(null);

                if (!onlyProjects  && !onlyFolders) {
                    actions.add(SystemActionBridge.createAction(SystemAction.get(AnnotateAction.class),
                                                                ((AnnotateAction)SystemAction.get(AnnotateAction.class)).visible(nodes) ?
                                                                        loc.getString("CTL_PopupMenuItem_HideAnnotations") : //NOI18N
                                                                        loc.getString("CTL_PopupMenuItem_ShowAnnotations"), context)); //NOI18N
                }
                actions.add(SystemActionBridge.createAction(SystemAction.get(LogAction.class), loc.getString("CTL_PopupMenuItem_Log"), context)); //NOI18N
                actions.add(null);
                actions.add(SystemActionBridge.createAction(SystemAction.get(RevertModificationsAction.class), loc.getString("CTL_PopupMenuItem_Revert"), context)); //NOI18N
                if (!onlyProjects) {
                    actions.add(SystemActionBridge.createAction(SystemAction.get(IgnoreAction.class), loc.getString("CTL_PopupMenuItem_Ignore"), context)); //NOI18N
                }
                if (!onlyProjects && !onlyFolders) {
                    ExcludeFromCommitAction exclude = (ExcludeFromCommitAction) SystemAction.get(ExcludeFromCommitAction.class);
                    actions.add(SystemActionBridge.createAction(exclude, exclude.getActionStatus(null) == ExcludeFromCommitAction.INCLUDING
                            ? loc.getString("CTL_PopupMenuItem_IncludeInCommit") //NOI18N
                            : loc.getString("CTL_PopupMenuItem_ExcludeFromCommit"), context)); //NOI18N
                }
                actions.add(null);
                actions.add(new ShareMenu(context));
                actions.add(new BranchMenu(context, ctx));
                actions.add(new TagMenu(context));
                actions.add(new QueuesMenu(context));
                actions.add(null);
                actions.add(SystemActionBridge.createAction(SystemAction.get(PropertiesAction.class), loc.getString("CTL_PopupMenuItem_Properties"), context)); //NOI18N
            }
        }
        return actions.toArray(new Action[actions.size()]);
    }
    
    /**
     * Applies custom format.
     */
    private String formatAnnotation(FileInformation info, File file) {
        String statusString = "";  // NOI18N
        int status = info.getStatus();
        if (status != FileInformation.STATUS_VERSIONED_UPTODATE) {
            statusString = info.getShortStatusText();
        }

        //String stickyString = SvnUtils.getCopy(file);
        String stickyString = null;
        if (stickyString == null) {
            stickyString = ""; // NOI18N
        }

        Object[] arguments = new Object[] {
            statusString,
            stickyString,
        };

        String annotation = format.format(arguments, new StringBuffer(), null).toString().trim();
        if(annotation.equals(emptyFormat)) {
            return ""; // NOI18N
        } else {
            return " " + annotation; // NOI18N
        }
    }

    public String annotateNameHtml(File file, FileInformation info) {
        return annotateNameHtml(file.getName(), info, file);
    }

    public String annotateNameHtml(String name, FileInformation mostImportantInfo, File mostImportantFile) {
        // Hg: The codes used to show the status of files are:
        // M = modified
        // A = added
        // R = removed
        // C = clean
        // ! = deleted, but still tracked
        // ? = not tracked
        // I = ignored (not shown by default)
        
        name = htmlEncode(name);
        
        String textAnnotation;
        boolean annotationsVisible = VersioningSupport.getPreferences().getBoolean(VersioningSupport.PREF_BOOLEAN_TEXT_ANNOTATIONS_VISIBLE, false);
        int status = mostImportantInfo.getStatus();
        
        if (annotationsVisible && mostImportantFile != null && (status & STATUS_TEXT_ANNOTABLE) != 0) {
            if (format != null) {
                textAnnotation = formatAnnotation(mostImportantInfo, mostImportantFile);
            } else {
                //String sticky = SvnUtils.getCopy(mostImportantFile);
                String sticky = null;
                if (status == FileInformation.STATUS_VERSIONED_UPTODATE && sticky == null) {
                    textAnnotation = "";  // NOI18N
                } else if (status == FileInformation.STATUS_VERSIONED_UPTODATE) {
                    textAnnotation = " [" + sticky + "]"; // NOI18N
                } else if (sticky == null) {
                    String statusText = mostImportantInfo.getShortStatusText();
                    if(!statusText.equals("")) { // NOI18N
                        textAnnotation = " [" + mostImportantInfo.getShortStatusText() + "]"; // NOI18N
                    } else {
                        textAnnotation = ""; // NOI18N
                    }
                } else {
                    textAnnotation = " [" + mostImportantInfo.getShortStatusText() + "; " + sticky + "]"; // NOI18N
                }
            }
        } else {
            textAnnotation = ""; // NOI18N
        }

        if (textAnnotation.length() > 0) {
            textAnnotation = NbBundle.getMessage(MercurialAnnotator.class, "textAnnotation", textAnnotation); // NOI18N
        }

        if (0 != (status & FileInformation.STATUS_NOTVERSIONED_EXCLUDED)) {
            return getAnnotationProvider().EXCLUDED_FILE.getFormat().format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_DELETEDLOCALLY)) {
            return getAnnotationProvider().DELETED_LOCALLY_FILE.getFormat().format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY)) {
            return getAnnotationProvider().REMOVED_LOCALLY_FILE.getFormat().format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY)) {
            return getAnnotationProvider().NEW_LOCALLY_FILE.getFormat().format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
            FileStatus fileStatus = mostImportantInfo.getStatus(mostImportantFile);
            if (fileStatus != null && fileStatus.isCopied()) {
                return getAnnotationProvider().COPIED_LOCALLY_FILE.getFormat().format(new Object [] { name, textAnnotation });
            } else {
                return getAnnotationProvider().ADDED_LOCALLY_FILE.getFormat().format(new Object [] { name, textAnnotation });
            }
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY)) {
            return getAnnotationProvider().MODIFIED_LOCALLY_FILE.getFormat().format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_UPTODATE)) {
            return getAnnotationProvider().UP_TO_DATE_FILE.getFormat().format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_CONFLICT)) {
            return getAnnotationProvider().CONFLICT_FILE.getFormat().format(new Object [] { name, textAnnotation });
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NOTMANAGED)) {
            return name;
        } else if (status == FileInformation.STATUS_UNKNOWN) {
            return name;
        } else {
            throw new IllegalArgumentException("Uncomparable status: " + status); // NOI18N
        }
    }
    
    private String htmlEncode(String name) {
        if (name.indexOf('<') == -1) return name;
        return lessThan.matcher(name).replaceAll("&lt;"); // NOI18N
    }
    
    private String annotateFolderNameHtml(String name, VCSContext context, FileInformation mostImportantInfo, File mostImportantFile) {
        String nameHtml = htmlEncode(name);
        if (mostImportantInfo.getStatus() == FileInformation.STATUS_NOTVERSIONED_EXCLUDED){
            return getAnnotationProvider().EXCLUDED_FILE.getFormat().format(new Object [] { nameHtml, ""}); // NOI18N
        }
        boolean annotationsVisible = VersioningSupport.getPreferences().getBoolean(VersioningSupport.PREF_BOOLEAN_TEXT_ANNOTATIONS_VISIBLE, false);
        MessageFormat uptodateFormat = getAnnotationProvider().UP_TO_DATE_FILE.getFormat();

        final Set<File> rootFiles = context.getRootFiles();
        File repo = null;        
        String folderAnotation = ""; //NOI18N
        File root = null; 
        boolean isRepoRoot = false;
        if(rootFiles.size() == 1) {
            File file = rootFiles.iterator().next();
            repo = Mercurial.getInstance().getRepositoryRoot(file);
            if(repo == null) {
                Mercurial.LOG.log(Level.WARNING, "Couldn''t find repository root for file {0}", file);
            } else {
                root = file;
                isRepoRoot = repo.equals(root);
            }
            // repo = null iff the file' status is actually unversioned, but cache has not yet have the right value
            if (repo == null || !isRepoRoot) {
                // not from repo root => do not annnotate with folder name 
                return uptodateFormat.format(new Object [] { nameHtml, ""});
            }             
        } else {
            // Label top level repository nodes with a repository name label when:
            // Display Name (name) is different from its repo name (repo.getName())        
            File parentFile = null;
            for (File file : rootFiles) {            
                if(parentFile == null) {
                    parentFile = file.getParentFile();
                } else {
                    File p = file.getParentFile();
                    if(p == null || !parentFile.equals(p)) {
                        // not comming from the same parent => do not annnotate with folder name
                        return uptodateFormat.format(new Object [] { nameHtml, ""});
                    }
                }
            }
            for (File file : rootFiles) {            
                repo = Mercurial.getInstance().getRepositoryRoot(file);
                if(repo == null) {
                    Mercurial.LOG.log(Level.WARNING, "Couldn''t find repository root for file {0}", file);
                } else {
                    root = file;
                    break;
                }
            }
            isRepoRoot = parentFile != null && parentFile.equals(repo);
        }

        if (annotationsVisible && repo != null) {
            if (isRepoRoot && !repo.getName().equals(name)) {
                folderAnotation = repo.getName();
            }
            WorkingCopyInfo info = WorkingCopyInfo.getInstance(repo);
            addFileWithRepositoryAnnotation(info, root);
            HgLogMessage[] parents = info.getWorkingCopyParents();
            StringBuilder label = new StringBuilder();
            if (parents.length == 1) {
                HgLogMessage parent = parents[0];
                for (String b : parent.getBranches()) {
                    label.append(b);
                }
                if (label.length() == 0) {
                    label.append(HgBranch.DEFAULT_NAME);
                }
                if (parent.getTags().length == 0) {
                    label.append(' ').append(parent.getCSetShortID().substring(0, Math.min(7, parent.getCSetShortID().length())));
                } else {
                    for (String b : parent.getTags()) {
                        label.append(' ').append(b);
                    }
                }
            } else if (parents.length > 1) {
                String b1 = parents[0].getBranches().length == 0 ? HgBranch.DEFAULT_NAME : parents[0].getBranches()[0];
                String b2 = parents[1].getBranches().length == 0 ? HgBranch.DEFAULT_NAME : parents[1].getBranches()[0];
                if (b1.equals(b2)) {
                    label.append(NbBundle.getMessage(MercurialAnnotator.class, "LBL_Annotator.label.merged.oneBranch", new Object[] { //NOI18N
                        parents[0].getCSetShortID().substring(0, Math.min(7, parents[0].getCSetShortID().length())), 
                        parents[1].getCSetShortID().substring(0, Math.min(7, parents[1].getCSetShortID().length())), 
                        b1
                    }));
                } else {
                    label.append(NbBundle.getMessage(MercurialAnnotator.class, "LBL_Annotator.label.merged.twoBranches", new Object[] { b1, b2 })); //NOI18N
                }
            }
            if (!folderAnotation.isEmpty() && label.length() > 0) {
                label.insert(0, ", "); //NOI18N
            }
            label.insert(0, folderAnotation);
            folderAnotation = label.toString();
        }

        return uptodateFormat.format(new Object [] { nameHtml, folderAnotation.isEmpty() ? "" : " [" + folderAnotation + "]" }); //NOI18N
    }
    
    private boolean isMoreImportant(FileInformation a, FileInformation b) {
        if (b == null) return true;
        if (a == null) return false;
        return getComparableStatus(a.getStatus()) < getComparableStatus(b.getStatus());
    }
    
    /**
     * Gets integer status that can be used in comparators. The more important the status is for the user,
     * the lower value it has. Conflict is 0, unknown status is 100.
     *
     * @return status constant suitable for 'by importance' comparators
     */
    public static int getComparableStatus(int status) {
        if (0 != (status & FileInformation.STATUS_VERSIONED_CONFLICT)) {
            return 0;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MERGE)) {
            return 1;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_DELETEDLOCALLY)) {
            return 10;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY)) {
            return 11;
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY)) {
            return 12;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
            return 13;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MODIFIEDLOCALLY)) {
            return 14;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_REMOVEDINREPOSITORY)) {
            return 30;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_NEWINREPOSITORY)) {
            return 31;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_MODIFIEDINREPOSITORY)) {
            return 32;
        } else if (0 != (status & FileInformation.STATUS_VERSIONED_UPTODATE)) {
            return 50;
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_EXCLUDED)) {
            return 100;
        } else if (0 != (status & FileInformation.STATUS_NOTVERSIONED_NOTMANAGED)) {
            return 101;
        } else if (status == FileInformation.STATUS_UNKNOWN) {
            return 102;
        } else {
            throw new IllegalArgumentException("Uncomparable status: " + status); // NOI18N
        }
    }
    
    private static boolean onlyProjects(Node[] nodes) {
        if (nodes == null) return false;
        for (Node node : nodes) {
            if (node.getLookup().lookup(Project.class) == null) return false;
        }
        return true;
    }
    
    private boolean onlyFolders(File[] files) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) return false;
            FileInformation status = cache.getCachedStatus(files[i]);
            if (status == null // be optimistic, this can be a file
                    || (!files[i].exists() && !status.isDirectory())) {
                return false;
            }
        }
        return true;
    }

    private AnnotationColorProvider getAnnotationProvider() {
        return AnnotationColorProvider.getInstance();
    }
    
    private final Map<WorkingCopyInfo, Set<File>> filesWithRepositoryAnnotations = new HashMap<WorkingCopyInfo, Set<File>>(3);
    
    private void addFileWithRepositoryAnnotation (WorkingCopyInfo info, File file) {
        info.removePropertyChangeListener(this);
        info.addPropertyChangeListener(this);
        synchronized (filesWithRepositoryAnnotations) {
            Set<File> files = filesWithRepositoryAnnotations.get(info);
            if (files == null) {
                filesWithRepositoryAnnotations.put(info, files = new HashSet<File>());
            }
            files.add(file);
        }
    }

    @Override
    public void propertyChange (final PropertyChangeEvent evt) {
        if (WorkingCopyInfo.PROPERTY_WORKING_COPY_PARENT.equals(evt.getPropertyName())) {
            Utils.post(new Runnable() {
                @Override
                public void run() {
                    WorkingCopyInfo info = (WorkingCopyInfo) evt.getSource();
                    Set<File> filesToRefresh;
                    synchronized (filesWithRepositoryAnnotations) {
                        filesToRefresh = filesWithRepositoryAnnotations.remove(info);
                    }
                    if (filesToRefresh != null && !filesToRefresh.isEmpty()) {
                        Mercurial.getInstance().refreshAnnotations(filesToRefresh);
                    }
                }
            }, 400);
        }
    }

    private Image addToolTip (Image icon, VCSContext context) {
        if (!VersioningSupport.getPreferences().getBoolean(VersioningSupport.PREF_BOOLEAN_TEXT_ANNOTATIONS_VISIBLE, false)) {
            return icon;
        }
        File root = null;
        File repository = null;
        for (File f : context.getRootFiles()) {
            File repo = Mercurial.getInstance().getRepositoryRoot(f);
            if (repo != null) {
                if (repository == null) {
                    repository = repo;
                    root = f;
                } else if (!repository.equals(repo)) {
                    // root files are from different repositories, do not annotate icon
                    return icon;
                }
            }
        }
        if (repository != null) {
            WorkingCopyInfo info = WorkingCopyInfo.getInstance(repository);
            addFileWithRepositoryAnnotation(info, root);
            HgLogMessage[] parents = info.getWorkingCopyParents();
            String label = null;
            if (parents.length == 1) {
                HgLogMessage parent = parents[0];
                String branchName = null;
                for (String b : parent.getBranches()) {
                    branchName = b;
                }
                if (branchName != null) {
                    label = NbBundle.getMessage(MercurialAnnotator.class, "LBL_Annotator.currentBranch.toolTip", branchName); //NOI18N
                }
            } else if (parents.length > 1) {
                String b1 = parents[0].getBranches().length == 0 ? HgBranch.DEFAULT_NAME : parents[0].getBranches()[0];
                String b2 = parents[1].getBranches().length == 0 ? HgBranch.DEFAULT_NAME : parents[1].getBranches()[0];
                if (b1.equals(b2)) {
                    label = NbBundle.getMessage(MercurialAnnotator.class, "LBL_Annotator.mergeNeeded.oneBranch.toolTip", new Object[] { //NOI18N
                        parents[0].getCSetShortID().substring(0, Math.min(7, parents[0].getCSetShortID().length())), 
                        parents[1].getCSetShortID().substring(0, Math.min(7, parents[1].getCSetShortID().length())), 
                        b1
                    });
                } else {
                    label = NbBundle.getMessage(MercurialAnnotator.class, "LBL_Annotator.mergeNeeded.twoBranches.toolTip", new Object[] { b1, b2 }); //NOI18N
                }
            }
            if (label != null) {
                icon = ImageUtilities.addToolTipToImage(icon, label.toString());
            }
        }
        return icon;
    }
}
