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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager.CancellationException;
import org.netbeans.modules.remote.api.RemoteBinaryService;
import org.netbeans.modules.cnd.remote.mapper.RemotePathMap;
import org.netbeans.modules.cnd.remote.support.RemoteCommandSupport;
import org.netbeans.modules.cnd.utils.CndPathUtilitities;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.HostInfo;
import org.netbeans.modules.nativeexecution.api.NativeProcessBuilder;
import org.netbeans.modules.nativeexecution.api.util.CommonTasksSupport;
import org.netbeans.modules.nativeexecution.api.util.HostInfoUtils;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 * RemoteBinaryService implementation
 * @author Vladimir Kvashin
 */
@ServiceProvider(service = RemoteBinaryService.class)
public class RemoteBinaryServiceImpl extends RemoteBinaryService {
    
    private static final RequestProcessor RP = new RequestProcessor("RemoteBinaryServiceImpl", 1); // NOI18N

    private final Map<ExecutionEnvironment, Delegate> impls = new HashMap<ExecutionEnvironment, Delegate>();
    private static int downloadCount = 0;

    @Override
    protected RemoteBinaryResult getRemoteBinaryImpl(ExecutionEnvironment execEnv, String remotePath) {
        CndUtils.assertNonUiThread();
        Delegate delegate;
        synchronized (this) {
            delegate = impls.get(execEnv);
            if (delegate == null) {
                delegate = new Delegate(execEnv);
                impls.put(execEnv, delegate);
            }
        }
        try {
            return delegate.getRemoteBinaryImpl(remotePath);
        } catch (InterruptedException ex) {
            // don't log InterruptedException
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } catch (ExecutionException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /*package-local for test purposes*/ static int getDownloadCount() {
        return downloadCount;
    }

    /*package-local for test purposes*/ static void resetDownloadCount() {
        downloadCount = 0;
    }

    /**
     * Corresoinds to the particular execution environment.
     * An instance of impl is created for each execution environment
     */
    private static class Delegate {

        private final ExecutionEnvironment execEnv;
        private final Map<String, Entry> cache = new HashMap<String, Entry>();

        public Delegate(ExecutionEnvironment execEnv) {
            this.execEnv = execEnv;
        }

        public RemoteBinaryResult getRemoteBinaryImpl(String remotePath) throws InterruptedException, IOException, ExecutionException {
            Entry entry;
            synchronized (this) {
                entry = cache.get(remotePath);
                if (entry == null) {
                    entry = new Entry(execEnv, remotePath);
                    cache.put(remotePath, entry);
                }
            }
            return entry.ensureSync();
        }
    }

    /**
     * Corresponds to a particular file
     */
    private static class Entry {

        private final String remotePath;
        private final ExecutionEnvironment execEnv;
        private RemoteBinaryResult lastResult;
        private String timeStamp;

        public Entry(ExecutionEnvironment execEnv, String remotePath) {
            this.remotePath = remotePath;
            this.execEnv = execEnv;
        }

        public RemoteBinaryResult ensureSync() throws InterruptedException, IOException, ExecutionException {
            String localPath = RemotePathMap.getPathMap(execEnv).getLocalPath(remotePath, false);

            RemoteBinaryResult result = null;

            if (localPath != null &&
                    RemotePathMap.isTheSame(execEnv,
                    //new File(remotePath).getParentFile().getAbsolutePath(),
                    CndPathUtilitities.getDirName(remotePath),
                    CndFileUtils.createLocalFile(localPath).getParentFile())) {
                if (lastResult == null) {
                    lastResult = new RemoteBinaryResult(localPath, new FutureTask<Boolean>(new Callable<Boolean>() {

                        @Override
                        public Boolean call() throws Exception {
                            return true;
                        }
                    }));
                    File file = new File(localPath);
                    lastResult.setTimeStamp(""+file.lastModified()); // NOI18N
                }
            } else {
                result = syncImpl();
                if (result != null) {
                    lastResult = result;
                }
            }

            return result == null ? lastResult : result;
        }

        private String getFullTimeLsCommand() throws IOException, CancellationException {
            HostInfo hostInfo = HostInfoUtils.getHostInfo(execEnv);
            switch (hostInfo.getOSFamily()) {
                case LINUX:
                    return "/bin/ls --full-time"; // NOI18N
                case MACOSX:
                    return "/bin/ls -lT"; // NOI18N
                case SUNOS:
                    return "/bin/ls -lE"; // NOI18N
                case WINDOWS:
                    throw new IllegalStateException("Windows in unsupported"); //NOI18N
                case UNKNOWN:
                default:
                    return "/bin/ls -l"; // NOI18N
            }
        }

        /**
         *
         * @return
         * @throws IOException
         * @throws InterruptedException
         * @throws ExecutionException
         */
        private synchronized RemoteBinaryResult syncImpl() throws IOException, InterruptedException, ExecutionException {
            final String newTimestamp = getTimestamp();

            if (timeStamp != null && timeStamp.equals(newTimestamp)) {
                if (CndFileUtils.isValidLocalFile(lastResult.localFName)) {
                    return lastResult;
                }
            }

            final File localFile = File.createTempFile("cnd-remote-binary-", ".bin"); // NOI18N
            localFile.deleteOnExit();

            FutureTask<Boolean> task = new FutureTask<Boolean>(new Callable<Boolean>() {

                @Override
                public Boolean call() throws Exception {

                    String remoteCopyPath = null;

                    try {
                        HostInfo hinfo = HostInfoUtils.getHostInfo(execEnv);
                        String tmpDir = hinfo.getTempDir();
                        // TODO: add utility method to do mktemp ...
                        remoteCopyPath = tmpDir + "/binary." + newTimestamp.hashCode(); // NOI18N

                        NativeProcessBuilder npb = NativeProcessBuilder.newProcessBuilder(execEnv);
                        npb.setExecutable("cp").setArguments(remotePath, remoteCopyPath); // NOI18N
                        Process copyProcess = npb.call();
                        int copyProcessResult = copyProcess.waitFor();

                        if (copyProcessResult != 0) {
                            return false;
                        }

                        Future<Integer> task = CommonTasksSupport.downloadFile(remoteCopyPath, execEnv, localFile.getAbsolutePath(), null);

                        if (task.get() != 0) {
                            return false;
                        }

                        timeStamp = newTimestamp;
                        downloadCount++;
                    } finally {
                        if (remoteCopyPath != null) {
                            CommonTasksSupport.rmFile(execEnv, remoteCopyPath, null);
                        }
                    }

                    return true;
                }
            });

            RP.post(task);
            RemoteBinaryResult remoteBinaryResult = new RemoteBinaryResult(localFile.getAbsolutePath(), task);
            remoteBinaryResult.setTimeStamp(newTimestamp);

            return remoteBinaryResult;
        }

        private String getTimestamp() {
            try {
                String command = getFullTimeLsCommand() + " \"" + remotePath + "\""; // NOI18N
                RemoteCommandSupport rcs = new RemoteCommandSupport(execEnv, command);

                if (rcs.run() == 0) {
                    return rcs.getOutput();
                } else {
                    throw new IOException("Cannot run #"+command); // NOI18N
                }
            } catch (CancellationException ex) {
                Exceptions.printStackTrace(ex); // TODO:CancellationException error processing
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            return null;
        }
    }
}