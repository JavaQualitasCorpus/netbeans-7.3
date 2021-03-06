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

package org.netbeans.libs.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * Provides information about a certain commit, usually is returned by 
 * git commit or log command.
 * 
 * @author Jan Becicka
 */
public final class GitRevisionInfo {

    private RevCommit revCommit;
    private Repository repository;
    private GitFileInfo[] modifiedFiles;
    private static final Logger LOG = Logger.getLogger(GitRevisionInfo.class.getName());

    GitRevisionInfo (RevCommit commit, Repository repository) {
        this.revCommit = commit;
        this.repository = repository;
    }

    /**
     * @return id of the commit
     */
    public String getRevision () {
        return ObjectId.toString(revCommit.getId());
    }

    /**
     * @return the first line of the commit message.
     */
    public String getShortMessage () {
        return revCommit.getShortMessage();
    }

    /**
     * @return full commit message
     */
    public String getFullMessage () {
        return revCommit.getFullMessage();
    }

    /**
     * @return time this commit was created in milliseconds.
     */
    public long getCommitTime () {
        return (long) revCommit.getCommitTime() * 1000;
    }

    /**
     * @return author of the commit
     */
    public GitUser getAuthor () {
        return GitClassFactoryImpl.getInstance().createUser(revCommit.getAuthorIdent());
    }

    /**
     * @return person who actually committed the changes, may or may not be the same as a return value of the <code>getAuthor</code> method.
     */
    public GitUser getCommitter () {
        return GitClassFactoryImpl.getInstance().createUser(revCommit.getCommitterIdent());
    }
    
    /**
     * Returns the information about the files affected (modified, deleted or added) by this commit.
     * <strong>First time call should not be done from the EDT.</strong> When called for the first time the method execution can take a big amount of time
     * because it compares the commit tree with its parents and identifies the modified files. 
     * Any subsequent call to the first <strong>successful</strong> call will return the cached value and will be fast.
     * @return files affected by this change set
     * @throws GitException when an error occurs
     */
    public java.util.Map<java.io.File, GitFileInfo> getModifiedFiles () throws GitException {
        if (modifiedFiles == null) {
            synchronized (this) {
                listFiles();
            }
        }
        Map<File, GitFileInfo> files = new HashMap<File, GitFileInfo>(modifiedFiles.length);
        for (GitFileInfo info : modifiedFiles) {
            files.put(info.getFile(), info);
        }
        return files;
    }
    
    /**
     * @return commit ids of this commit's parents
     */
    public String[] getParents () {
        String[] parents = new String[revCommit.getParentCount()];
        for (int i = 0; i < revCommit.getParentCount(); ++i) {
            parents[i] = ObjectId.toString(revCommit.getParent(i).getId());
        }
        return parents;
    }
    
    private void listFiles() throws GitException {
        RevWalk revWalk = new RevWalk(repository);
        TreeWalk walk = new TreeWalk(repository);
        try {
            ArrayList<GitFileInfo> result = new ArrayList<GitFileInfo>();
            walk.reset();
            walk.setRecursive(true);
            RevCommit parentCommit = null;
            if (revCommit.getParentCount() > 0) {
                for (RevCommit commit : revCommit.getParents()) {
                    revWalk.markStart(revWalk.lookupCommit(commit));
                }
                revWalk.setRevFilter(RevFilter.MERGE_BASE);
                Iterator<RevCommit> it = revWalk.iterator();
                if (it.hasNext()) {
                    parentCommit = it.next();
                }
                if (parentCommit != null) {
                    walk.addTree(parentCommit.getTree().getId());
                }
            }
            walk.addTree(revCommit.getTree().getId());
            walk.setFilter(AndTreeFilter.create(TreeFilter.ANY_DIFF, PathFilter.ANY_DIFF));
            if (parentCommit != null) {
                List<DiffEntry> entries = DiffEntry.scan(walk);
                RenameDetector rd = new RenameDetector(repository);
                rd.addAll(entries);
                entries = rd.compute();
                for (DiffEntry e : entries) {
                    GitFileInfo.Status status;
                    File oldFile = null;
                    String oldPath = null;
                    String path = e.getOldPath();
                    if (path == null) {
                        path = e.getNewPath();
                    }
                    switch (e.getChangeType()) {
                        case ADD:
                            status = GitFileInfo.Status.ADDED;
                            path = e.getNewPath();
                            break;
                        case COPY:
                            status = GitFileInfo.Status.COPIED;
                            oldFile = new File(repository.getWorkTree(), e.getOldPath());
                            oldPath = e.getOldPath();
                            path = e.getNewPath();
                            break;
                        case DELETE:
                            status = GitFileInfo.Status.REMOVED;
                            path = e.getOldPath();
                            break;
                        case MODIFY:
                            status = GitFileInfo.Status.MODIFIED;
                            path = e.getOldPath();
                            break;
                        case RENAME:
                            status = GitFileInfo.Status.RENAMED;
                            oldFile = new File(repository.getWorkTree(), e.getOldPath());
                            oldPath = e.getOldPath();
                            path = e.getNewPath();
                            break;
                        default:
                            status = GitFileInfo.Status.UNKNOWN;
                    }
                    if (status == GitFileInfo.Status.RENAMED) {
                        result.add(new GitFileInfo(new File(repository.getWorkTree(), e.getOldPath()), e.getOldPath(), GitFileInfo.Status.REMOVED, null, null));
                    }
                    result.add(new GitFileInfo(new File(repository.getWorkTree(), path), path, status, oldFile, oldPath));
                }
            } else {
                while (walk.next()) {
                    result.add(new GitFileInfo(new File(repository.getWorkTree(), walk.getPathString()), walk.getPathString(), GitFileInfo.Status.ADDED, null, null));
                }
            }
            this.modifiedFiles = result.toArray(new GitFileInfo[result.size()]);
        } catch (IOException ex) {
            throw new GitException(ex);
        } finally {
            revWalk.release();
            walk.release();
        }
    }
    
    /**
     * Provides information about what happened to a file between two different commits.
     * If the file is copied or renamed between the two commits, you can get the path
     * of the original file.
     */
    public static final class GitFileInfo {

        /**
         * State of the file in the second commit in relevance to the first commit.
         */
        public static enum Status {
            ADDED,
            MODIFIED,
            RENAMED,
            COPIED,
            REMOVED,
            UNKNOWN
        }

        private final String relativePath;
        private final String originalPath;
        private final Status status;
        private final File file;
        private final File originalFile;

        GitFileInfo (File file, String relativePath, Status status, File originalFile, String originalPath) {
            this.relativePath = relativePath;
            this.status = status;
            this.file = file;
            this.originalFile = originalFile;
            this.originalPath = originalPath;
        }

        /**
         * @return relative path of the file to the root of the repository
         */
        public String getRelativePath() {
            return relativePath;
        }

        /**
         * @return the relative path of the original file this file was copied or renamed from.
         *         For other statuses than <code>COPIED</code> or <code>RENAMED</code> it may be <code>null</code> 
         *         or the same as the return value of <code>getPath</code> method
         */
        public String getOriginalPath() {
            return originalPath;
        }

        /**
         * @return state of the file between the two commits
         */
        public Status getStatus() {
            return status;
        }

        /**
         * @return the file this refers to
         */
        public File getFile () {
            return file;
        }

        /**
         * @return the original file this file was copied or renamed from.
         *         For other statuses than <code>COPIED</code> or <code>RENAMED</code> it may be <code>null</code> 
         *         or the same as the return value of <code>getFile</code> method
         */
        public File getOriginalFile () {
            return originalFile;
        }
    }
    
}
