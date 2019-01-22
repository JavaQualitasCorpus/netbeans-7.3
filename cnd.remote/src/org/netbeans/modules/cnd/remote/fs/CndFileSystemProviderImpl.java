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

package org.netbeans.modules.cnd.remote.fs;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.cnd.spi.utils.CndFileSystemProvider;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.cnd.utils.FSPath;
import org.netbeans.modules.cnd.utils.cache.CharSequenceUtils;
import org.netbeans.modules.cnd.utils.cache.CndFileUtils;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.util.EnvUtils;
import org.netbeans.modules.remote.spi.FileSystemCacheProvider;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.netbeans.modules.remote.spi.FileSystemProvider.FileSystemProblemListener;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Vladimir Kvashin
 */
@ServiceProvider(service=CndFileSystemProvider.class)
public class CndFileSystemProviderImpl extends CndFileSystemProvider {

   /** just to speed it up, since Utilities.isWindows will get string property, test equals, etc */
   private static final boolean isWindows = Utilities.isWindows();
   private String cachePrefix;

    public CndFileSystemProviderImpl() {
    }

    @Override
    protected FileObject toFileObjectImpl(CharSequence absPath) {
        FileSystemAndString p = getFileSystemAndRemotePath(absPath);
        if (p == null) {
            return FileSystemProvider.urlToFileObject(absPath.toString());
        } else {
            return p.getFileObject();
        }
    }

    @Override
    protected FileObject toFileObjectImpl(File file) {
        return FileSystemProvider.fileToFileObject(file);
    }

    @Override
    protected CharSequence fileObjectToUrlImpl(FileObject fileObject) {
        return FileSystemProvider.fileObjectToUrl(fileObject);
    }

    @Override
    protected CharSequence toUrlImpl(FSPath fsPath) {
        return FileSystemProvider.toUrl(fsPath.getFileSystem(), fsPath.getPath());
    }

    @Override
    protected CharSequence toUrlImpl(FileSystem fileSystem, CharSequence absPath) {
        return FileSystemProvider.toUrl(fileSystem, absPath.toString());
    }

    @Override
    protected CharSequence getCanonicalPathImpl(FileSystem fileSystem, CharSequence absPath) throws IOException {
        return FileSystemProvider.getCanonicalPath(fileSystem, absPath.toString());
    }

    @Override
    protected FileObject getCanonicalFileObjectImpl(FileObject fo) throws IOException {
        return FileSystemProvider.getCanonicalFileObject(fo);
    }

    @Override
    protected String getCanonicalPathImpl(FileObject fo) throws IOException {
        return FileSystemProvider.getCanonicalPath(fo);
    }

    @Override
    protected String normalizeAbsolutePathImpl(FileSystem fs, String absPath) {
        return FileSystemProvider.normalizeAbsolutePath(absPath, fs);
    }

    @Override
    protected FileObject urlToFileObjectImpl(CharSequence url) {
        // That's legacy: an url can be a path to RFS cache file.
        FileSystemAndString p = getFileSystemAndRemotePath(url);
        if (p == null) {
            return FileSystemProvider.urlToFileObject(url.toString());
        } else {
            return p.getFileObject();
        }
    }

    @Override
    protected FileInfo[] getChildInfoImpl(CharSequence path) {
        FileSystemAndString p = getFileSystemAndRemotePath(path);
        if (p != null) {
            CndUtils.assertNotNull(p.fileSystem, "null file system"); //NOI18N
            CndUtils.assertNotNull(p.remotePath, "null remote path"); //NOI18N
            FileObject dirFO = p.getFileObject();
            if (dirFO == null) {
                return new FileInfo[0];
            }
            FileObject[] children = dirFO.getChildren();
            FileInfo[] result = new FileInfo[children.length];
            for (int i = 0; i < children.length; i++) {
                result[i] = new FileInfo(path.toString() + '/' + children[i].getNameExt(), children[i].isFolder(), children[i].isData());
            }
            return result;
        }
        return null;
    }

    @Override
    protected Boolean canReadImpl(CharSequence path) {
        FileSystemAndString p = getFileSystemAndRemotePath(path);
        if (p != null) {
            CndUtils.assertNotNull(p.fileSystem, "null file system"); //NOI18N
            CndUtils.assertNotNull(p.remotePath, "null remote path"); //NOI18N
            FileObject fo = p.getFileObject();
            return (fo != null && fo.isValid() && fo.canRead());
        }
        return null;
    }

    @Override
    protected Boolean existsImpl(CharSequence path) {
        FileSystemAndString p = getFileSystemAndRemotePath(path);
        if (p != null) {
            CndUtils.assertNotNull(p.fileSystem, "null file system"); //NOI18N
            CndUtils.assertNotNull(p.remotePath, "null remote path"); //NOI18N
            FileObject fo = p.getFileObject();
            return (fo != null && fo.isValid());
        }
        return null;
    }

    @Override
    protected boolean addFileChangeListenerImpl(FileChangeListener listener, FileSystem fileSystem, String path) {
       FileSystemProvider.addFileChangeListener(listener, fileSystem, path);
       return true;
    }

    @Override
    protected boolean removeFileChangeListenerImpl(FileChangeListener listener, FileSystem fileSystem, String path) {
        FileSystemProvider.removeRecursiveListener(listener, fileSystem, path);
        return true;
    }

    @Override
    protected boolean addFileChangeListenerImpl(FileChangeListener listener) {
        FileSystemProvider.addFileChangeListener(listener);
        return true;
    }

    @Override
    protected boolean removeFileChangeListenerImpl(FileChangeListener listener) {
        FileSystemProvider.removeFileChangeListener(listener);
        return true;
    }
    
    

    private FileSystemAndString getFileSystemAndRemotePath(CharSequence path) {
        String prefix = getPrefix();
        if (prefix != null) {
            if (isWindows) {
                path = path.toString().replace('\\', '/');
            }
            if (pathStartsWith(path, prefix)) {
                CharSequence rest = path.subSequence(prefix.length(), path.length());
                int slashPos = CharSequenceUtils.indexOf(rest, "/"); // NOI18N
                if (slashPos >= 0) {
                    String envID = rest.subSequence(0, slashPos).toString();
                    CharSequence remotePath = rest.subSequence(slashPos + 1, rest.length());
                    ExecutionEnvironment env = getExecutionEnvironmentByEnvID(envID);
                    if (env != null) {
                        FileSystem fs = FileSystemProvider.getFileSystem(env);
                        return new FileSystemAndString(fs, remotePath);
                    }
                }
            }
        }
        return null;
    }

    private synchronized String getPrefix() {
        if (cachePrefix == null) {
            String cacheRoot = FileSystemCacheProvider.getCacheRoot(ExecutionEnvironmentFactory.getLocal());
            if (cacheRoot != null) {
                String prefix = new File(cacheRoot).getParent();
                if (prefix != null) {
                    prefix= prefix.replace("\\", "/"); //NOI18N
                    if (!prefix.endsWith("/")) { //NOI18N
                        prefix += '/';
                    }
                    cachePrefix = prefix;
                }
            }
        }
        return cachePrefix;
    }

    private boolean pathStartsWith(CharSequence path, CharSequence prefix) {
        if (CndFileUtils.isSystemCaseSensitive()) {
            return CharSequenceUtils.startsWith(path, prefix);
        } else {
            return CharSequenceUtils.startsWithIgnoreCase(path, prefix);
        }
    }

    private static ExecutionEnvironment getExecutionEnvironmentByEnvID(String envID) {
        // envID has form: hostId + '_' + userId
        ExecutionEnvironment result = null;
        for(ExecutionEnvironment env : ServerList.getEnvironments()) {
            String currHostID = EnvUtils.toHostID(env);
            if (envID.startsWith(currHostID)) {
                if (envID.length() > currHostID.length() && envID.charAt(currHostID.length()) == '_') {
                    String user = envID.substring(currHostID.length() + 1);
                    if (user.equals(env.getUser())) {
                        return env;
                    }
                }
            }
        }
        return result;
    }
    
    private final List<ProblemListenerAdapter> adapters = new ArrayList<ProblemListenerAdapter>();
    
    @Override
    protected void addFileSystemProblemListenerImpl(CndFileSystemProblemListener listener, FileSystem fileSystem) {
        ProblemListenerAdapter newAdapter = new ProblemListenerAdapter(listener);
        synchronized (adapters) {
            for (Iterator<ProblemListenerAdapter> it = adapters.iterator(); it.hasNext(); ) {
                ProblemListenerAdapter adapter = it.next();
                if (adapter.listenerRef.get() == null) {
                    it.remove();
                }
            }
            adapters.add(newAdapter);
        }
        FileSystemProvider.addFileSystemProblemListener(newAdapter, fileSystem);
    }

    @Override
    protected void removeFileSystemProblemListenerImpl(CndFileSystemProblemListener listener, FileSystem fileSystem) {
        synchronized (adapters) {
            for (Iterator<ProblemListenerAdapter> it = adapters.iterator(); it.hasNext(); ) {
                ProblemListenerAdapter adapter = it.next();
                CndFileSystemProblemListener l = adapter.listenerRef.get();
                if (l == null) {
                    it.remove();
                } else if (l == listener) {
                    FileSystemProvider.removeFileSystemProblemListener(adapter, fileSystem);
                    it.remove();
                }                
            }
        }
    }

    private static class ProblemListenerAdapter implements FileSystemProblemListener {
        
        private final WeakReference<CndFileSystemProblemListener> listenerRef;

        public ProblemListenerAdapter(CndFileSystemProblemListener listener) {
            listenerRef = new WeakReference<CndFileSystemProblemListener>(listener);
        }

        @Override
        public void problemOccurred(FileSystem fileSystem, String path) {
            CndFileSystemProblemListener listener = listenerRef.get();
            if (listener != null) {
                listener.problemOccurred(new FSPath(fileSystem, path));
            }
        }

        @Override
        public void recovered(FileSystem fileSystem) {
            CndFileSystemProblemListener listener = listenerRef.get();
            if (listener != null) {
                listener.recovered(fileSystem);
            }
        }        
    }

    private static class FileSystemAndString {

        public final FileSystem fileSystem;
        public final CharSequence remotePath;

        public FileSystemAndString(FileSystem fs, CharSequence path) {
            this.fileSystem = fs;
            this.remotePath = path;
        }

        public FileObject getFileObject() {
            return fileSystem.findResource(remotePath.toString());
        }
    }
}