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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.cnd.remote.sync;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.cnd.api.remote.PathMap;
import org.netbeans.modules.cnd.api.remote.RemoteSyncWorker;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.cnd.remote.mapper.RemotePathMap;
import org.netbeans.modules.cnd.remote.support.RemoteUtil;
import org.netbeans.modules.cnd.remote.sync.FileData.FileInfo;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.cnd.utils.FSPath;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.util.CommonTasksSupport;
import org.netbeans.modules.nativeexecution.api.util.CommonTasksSupport.UploadStatus;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils;
import org.netbeans.modules.nativeexecution.api.util.ProcessUtils.ExitStatus;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;

/**
 *
 * @author Vladimir Kvashin
 */
/*package-local*/ final class FtpSyncWorker extends BaseSyncWorker implements RemoteSyncWorker, Cancellable {

    private TimestampAndSharabilityFilter filter;

    private int preliminaryCount;
    private int uploadCount;
    private long uploadSize;
    private volatile Thread thread;
    private boolean cancelled;
    private final PathMap mapper;
    private ProgressHandle progressHandle;

    // TODO: eliminate copy-paste with ZipSyncWorker.TimestampAndSharabilityFilters
    private static class TimestampAndSharabilityFilter implements FileFilter {

        private final FileData fileData;
        private final File fileDataStorageFile;
        private final SharabilityFilter delegate;

        public TimestampAndSharabilityFilter(FileObject privProjectStorageDir, ExecutionEnvironment executionEnvironment) throws IOException {
            fileData = FileData.get(privProjectStorageDir, executionEnvironment);
            fileDataStorageFile = FileUtil.toFile(fileData.getDataFile());
            delegate = new SharabilityFilter();
        }

        @Override
        public boolean accept(File file) {
            if (file.equals(fileDataStorageFile)) {
                return false; // SharabilityFilter now includes nbproject/private. Exclude our storage
            }
            boolean accepted = delegate.accept(file);
            if (accepted && ! file.isDirectory()) {
                accepted = needsCopying(file);
            }            
            return accepted;
        }

        private boolean needsCopying(File file) {
            FileInfo info = fileData.getFileInfo(file);
            FileState state = (info == null) ? FileState.INITIAL : info.state;
            switch (state) {
                case INITIAL:       return true;
                case TOUCHED:       return true;
                case COPIED:        return info.timestamp != file.lastModified();
                case ERROR:         return true;
                case UNCONTROLLED:  return false;
                default:
                    CndUtils.assertTrue(false, "Unexpected state: " + state); //NOI18N
                    return false;
            }
        }

        public void setState(File file, FileState state) {
            fileData.setState(file, state);
        }

        public void flush() {
            fileData.store();
        }

        private void clear() {
            fileData.clear();
        }
    }

    public FtpSyncWorker(ExecutionEnvironment executionEnvironment, PrintWriter out, PrintWriter err, 
            FileObject privProjectStorageDir, FSPath... paths) {
        super(executionEnvironment, out, err, privProjectStorageDir, paths);
        mapper = RemotePathMap.getPathMap(executionEnvironment);
    }

    /** for trace/debug purposes */
    private StringBuilder getLocalFilesString() {
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(f.getAbsolutePath());
        }
        return sb;
    }

    @SuppressWarnings("CallToThreadDumpStack")
    private void synchronizeImpl(String remoteRoot) throws InterruptedException, ExecutionException, IOException {
        
        uploadCount = 0;
        uploadSize = 0;
        long time = 0;
        
        if (RemoteUtil.LOGGER.isLoggable(Level.FINE)) {
            System.out.printf("Uploading %s to %s ...\n", getLocalFilesString(), executionEnvironment); // NOI18N
            time = System.currentTimeMillis();
        }
        filter = new TimestampAndSharabilityFilter(privProjectStorageDir, executionEnvironment);
        preliminaryCount = 0;
        List<String> dirsToCreate = new LinkedList<String>();
        for (int i = 0; i < files.length; i++) {
            final File name = files[i];
            String remoteFile = mapper.getRemotePath(name.getAbsolutePath(), true);
            if (name.isFile()) {
                File parent = name.getParentFile();
                if (parent != null) {
                    String remoteParent = mapper.getRemotePath(parent.getAbsolutePath(), true);
                    dirsToCreate.add(remoteParent);
                }
            }
            preprocessFile(name, dirsToCreate, remoteFile);
        }
        
        if (!dirsToCreate.isEmpty()) {
            dirsToCreate.add(0, "-p"); // NOI18N
            ExitStatus status = ProcessUtils.execute(executionEnvironment, "mkdir", dirsToCreate.toArray(new String[dirsToCreate.size()])); // NOI18N
            if (!status.isOK()) {
                throw new IOException("Can not check remote directories: " + status.toString()); // NOI18N
            }
        }

        progressHandle.switchToDeterminate(preliminaryCount);
        // success flag is for tracing only. TODO: should we drop it?
        boolean success = false;
        try  {
            for (File file : files) {
                String remoteFile = mapper.getRemotePath(file.getAbsolutePath(), false);
                if (remoteFile == null) { // this never happens since mapper is fixed
                    throw new IOException("Can not find remote path for " + file.getAbsolutePath()); //NOI18N
                }
                upload(file, remoteFile);
            }
            success = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            err.println(ex.getMessage());
        } finally {
        }

        if (RemoteUtil.LOGGER.isLoggable(Level.FINE)) {
            time = System.currentTimeMillis() - time;
            long bps = uploadSize * 1000L / time;
            String speed = (bps < 1024*8) ? (bps + " b/s") : ((bps/1024) + " Kb/s"); // NOI18N

            String strUploadSize = (uploadSize < 1024 ? (uploadSize + " bytes") : ((uploadSize/1024) + " K")); // NOI18N
            System.out.printf("\n\nCopied to %s:%s: %s in %d files. Time: %d ms. %s. Avg. speed: %s\n\n", // NOI18N
                    executionEnvironment, remoteRoot,
                    strUploadSize, uploadCount, time, success ? "OK" : "FAILURE", speed); // NOI18N
        }
    }

    private void preprocessFile(File file, List<String> dirsToCreate, String remoteFile) {
        if (file.isDirectory()) {
            dirsToCreate.add(remoteFile); // create all directories // NOI18N
            File[] children = file.listFiles(filter);
            for (File child : children) {
                preprocessFile(child, dirsToCreate, remoteFile + '/' + child.getName());
            }
        } else {
            preliminaryCount++;
        }
    }

    private void upload(File srcFile, String remotePath) throws InterruptedException, ExecutionException, IOException {
        if (cancelled) {
            return;
        }
        if (srcFile.isDirectory()) {
            File[] srcFiles = srcFile.listFiles(filter);
            for (File file : srcFiles) {
                upload(file, remotePath + '/' + file.getName()); //NOI18N
            }
        } else {
            Future<UploadStatus> fileTask = CommonTasksSupport.uploadFile(srcFile.getAbsolutePath(), executionEnvironment, remotePath, 0700);
            UploadStatus uploadStatus = fileTask.get();
            if (uploadStatus.isOK()) {
                filter.setState(srcFile, FileState.COPIED);
                progressHandle.progress(srcFile.getAbsolutePath(), uploadCount++);
                uploadSize += srcFile.length();
            } else {
                if (err != null) {
                    err.println(uploadStatus.getError());
                }
                throw new IOException("uploading " + srcFile + " to " + executionEnvironment + ':' + remotePath + // NOI18N
                        " finished with error code " + uploadStatus.getExitCode()); // NOI18N
            }
        }
    }

    @Override
    public boolean startup(Map<String, String> env2add) {

        if (SyncUtils.isDoubleRemote(executionEnvironment, fileSystem)) {
            SyncUtils.warnDoubleRemote(executionEnvironment, fileSystem);
            return false;
        }

        // Later we'll allow user to specify where to copy project files to
        String remoteRoot = RemotePathMap.getRemoteSyncRoot(executionEnvironment);
        if (remoteRoot == null) {
            if (err != null) {
                err.printf("%s\n", NbBundle.getMessage(getClass(), "MSG_Cant_find_sync_root", ServerList.get(executionEnvironment).toString()));
            }
            return false; // TODO: error processing
        }

        boolean success = false;
        thread = Thread.currentThread();
        cancelled = false;
        //String title = NbBundle.getMessage(getClass(), "PROGRESS_UPLOADING", ServerList.get(executionEnvironment).getDisplayName());
        String title = "Uploading to " + ServerList.get(executionEnvironment).getDisplayName(); //NOI18N FIXUP
        progressHandle = ProgressHandleFactory.createHandle(title, this);
        progressHandle.start();
        try {
            if (out != null) {
                out.printf("%s\n", NbBundle.getMessage(getClass(), "MSG_Copying",
                        remoteRoot, ServerList.get(executionEnvironment).toString()));
            }
            synchronizeImpl(remoteRoot);
            success = ! cancelled;
            if (success) {
                filter.flush();
            }
        } catch (InterruptedException ex) {
            // reporting does not make sense, just return false
            RemoteUtil.LOGGER.finest(ex.getMessage());
        } catch (InterruptedIOException ex) {
            // reporting does not make sense, just return false
            RemoteUtil.LOGGER.finest(ex.getMessage());
        } catch (ExecutionException ex) {
            RemoteUtil.LOGGER.log(Level.FINE, null, ex);
            if (err != null) {
                err.printf("%s\n", NbBundle.getMessage(getClass(), "MSG_Error_Copying",
                        remoteRoot, ServerList.get(executionEnvironment).toString(), ex.getLocalizedMessage()));
            }
        } catch (IOException ex) {
            RemoteUtil.LOGGER.log(Level.FINE, null, ex);
            if (err != null) {
                err.printf("%s\n", NbBundle.getMessage(getClass(), "MSG_Error_Copying",
                        remoteRoot, ServerList.get(executionEnvironment).toString(), ex.getLocalizedMessage()));
            }
        } finally {
            cancelled = false;
            thread = null;
            progressHandle.finish();
        }
        return success;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public boolean cancel() {
        cancelled = true;
        Thread t = thread;
        if (t != null) {
            t.interrupt();
        }
        return true;
    }
}