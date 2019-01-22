/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing the
 * software, include this License Header Notice in each file and include the
 * License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by Oracle
 * in the GPL Version 2 section of the License file that accompanied this code.
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or only
 * the GPL Version 2, indicate your decision by adding "[Contributor] elects to
 * include this software in this distribution under the [CDDL or GPL Version 2]
 * license." If you do not indicate a single choice of license, a recipient has
 * the option to distribute your version of this file under either the CDDL, the
 * GPL Version 2 or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.localhistory;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.netbeans.editor.ActionFactory;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.localhistory.store.LocalHistoryStore;
import org.netbeans.modules.localhistory.store.LocalHistoryStoreFactory;
import org.netbeans.modules.localhistory.store.StoreEntry;
import org.netbeans.modules.versioning.core.VersioningManager;
import org.netbeans.modules.versioning.core.spi.VCSInterceptor;
import org.netbeans.modules.versioning.masterfs.VersioningAnnotationProvider;
import org.netbeans.modules.versioning.util.VersioningListener;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author tomas
 */
public class InterceptorTest extends NbTestCase {

    public InterceptorTest(String name) {
        super(name);
    }

    @Override
    protected Level logLevel() {
        return Level.FINER;
    }
    
    @Override
    protected void setUp() throws Exception {
        System.setProperty("netbeans.localhistory.historypath", getWorkDir().getParentFile().getAbsolutePath());
        System.setProperty("netbeans.user", getWorkDirPath() + "/userdir");
        VersioningManager.getInstance().versionedRootsChanged(); // flush file owner caches
        MockServices.setServices(new Class[] {
            VersioningAnnotationProvider.class,
            LocalHistoryVCS.class});
        super.setUp();
    }
    
    public void testBeforeEdit() throws IOException, InterruptedException, TimeoutException {
        File f = new File(getWorkDir(), "file");
        f.createNewFile();
        LogHandler lh = new LogHandler("finnished copy file " + f, LogHandler.Compare.STARTS_WITH);
        write(f, "1");
        long ts = f.lastModified();
        
        FileObject fo = FileUtil.toFileObject(f);
        // lock will invoke async storing
        fo.lock();
        // block until we know file was stored
        lh.waitUntilDone();

        // check store
        StoreEntry entry = LocalHistory.getInstance().getLocalHistoryStore().getStoreEntry(f, ts);
        assertNotNull(entry);
        assertEntry(entry, "1");
    }
    
    public void testEdit() throws IOException {
        File f = new File(getWorkDir(), "file");
        f.createNewFile();
        
        write(f, "1");
        long ts = f.lastModified();
        
        FileObject fo = FileUtil.toFileObject(f);
        // change file. it should be ensured that, after write returns,
        // the file is already stored
        write(fo, "2");
        
        StoreEntry entry = LocalHistory.getInstance().getLocalHistoryStore().getStoreEntry(f, ts);
        assertNotNull(entry);
        assertEntry(entry, "1");
        assertNotSame(entry.getTimestamp(), f.lastModified());
        assertNotSame(ts, f.lastModified());
        
        ts = f.lastModified();
        
        write(fo, "3");
        entry = LocalHistory.getInstance().getLocalHistoryStore().getStoreEntry(f, ts);
        
        assertNotNull(entry);
        assertEntry(entry, "2");
        
    }
    
    public void testDelete() throws IOException {
        File f = new File(getWorkDir(), "file");
        f.createNewFile();
        
        write(f, "1");
        long ts = f.lastModified();
        
        FileObject fo = FileUtil.toFileObject(f);
        // delete file. it should be ensured that, after delete returns,
        // the file is already stored
        fo.delete();
        
        StoreEntry entry = LocalHistory.getInstance().getLocalHistoryStore().getStoreEntry(f, ts);
        assertNotNull(entry);
        assertEntry(entry, "1");
        assertNotSame(entry.getTimestamp(), f.lastModified());
        assertNotSame(ts, f.lastModified());
        
    }
    
    public void testBeforeEditBlocksFileChange() throws IOException {
        File f = new File(getWorkDir(), "file");
        f.createNewFile();
        
        write(f, "1");
        long ts = f.lastModified();

        // change file and block the storing for some time right after the msg gets intercepted
        FileObject fo = FileUtil.toFileObject(f);
        LogHandler lhBocked = new LogHandler("beforeChange for file " + f + " was blocked", LogHandler.Compare.STARTS_WITH);
        
        long BLOCK_TIME = 5000;
        LogHandler lh = new LogHandler("finnished copy file " + f, LogHandler.Compare.STARTS_WITH);
        lh.block(BLOCK_TIME); 
        
        long t = System.currentTimeMillis();
        write(fo, "2");
        assertTrue(lh.isDone()); // was blocked while storing 
        if(System.currentTimeMillis() - t < BLOCK_TIME) {
            fail("should have been blocked for at least " + (BLOCK_TIME / 10000) + " seconds");
        }
        assertTrue(lhBocked.isDone()); // was blocked in beforeEdit as well 
        
        StoreEntry entry = LocalHistory.getInstance().getLocalHistoryStore().getStoreEntry(f, ts);
        assertNotNull(entry);
        assertEntry(entry, "1");
    }
    
    public void testBeforeEditBlocksFileDelete() throws IOException {
        File f = new File(getWorkDir(), "file");
        f.createNewFile();
        
        write(f, "1");
        long ts = f.lastModified();

        // change file and block the storing for some time right after the msg gets intercepted
        FileObject fo = FileUtil.toFileObject(f);
        LogHandler lhBocked = new LogHandler("beforeDelete for file " + f + " was blocked", LogHandler.Compare.STARTS_WITH);
        
        long BLOCK_TIME = 5000;
        LogHandler lh = new LogHandler("finnished copy file " + f, LogHandler.Compare.STARTS_WITH);
        lh.block(BLOCK_TIME); 
        
        long t = System.currentTimeMillis();
        fo.delete();
        assertTrue(lh.isDone()); // was blocked while storing 
        if(System.currentTimeMillis() - t < BLOCK_TIME) {
            fail("should have been blocked for at least " + (BLOCK_TIME / 10000) + " seconds");
        }
        assertTrue(lhBocked.isDone()); // was blocked in beforeEdit as well 
        
        StoreEntry entry = LocalHistory.getInstance().getLocalHistoryStore().getStoreEntry(f, ts);
        assertNotNull(entry);
        assertEntry(entry, "1");
    }
    
    public void testLockTimeout() throws IOException, InterruptedException, TimeoutException {

        File f = new File(getWorkDir(), "file");
        f.createNewFile();
        
        write(f, "1");
        long ts = f.lastModified();

        // change file and block the storing for some time right after the msg gets intercepted
        FileObject fo = FileUtil.toFileObject(f);

        long BLOCK_TIME = 15000;
        long LOCK_TIME = 3;
        System.setProperty("netbeans.t9y.localhistory.release-lock.timeout", "" + LOCK_TIME);
        LogHandler fileStoreBlock = new LogHandler("finnished copy file " + f, LogHandler.Compare.STARTS_WITH);
        fileStoreBlock.block(BLOCK_TIME); 
        LogHandler beforeDeleteBlock = new LogHandler("beforeDelete for file " + f + " was blocked", LogHandler.Compare.STARTS_WITH);
        
        long t = System.currentTimeMillis();
        fo.delete();
        assertTrue(beforeDeleteBlock.isDone()); // beforeDelete is done
        long d = System.currentTimeMillis() - t;
        if(d < LOCK_TIME * 1000) {
            fail("should have been locked for at least " + LOCK_TIME + " seconds but was " + d);
        } else if(System.currentTimeMillis() - t >= BLOCK_TIME) {
            fail("was blocked longer that expected: " + (BLOCK_TIME / 1000) + " seconds");
        }
        // great, the lock was released, now lets wait until
        // is realy stored
        fileStoreBlock.waitUntilDone();
        
        StoreEntry entry = LocalHistory.getInstance().getLocalHistoryStore().getStoreEntry(f, ts);
        assertNotNull(entry);
        assertEntry(entry, "1");
    }

    /**
     * methods that aren't implemented in {@link LocalHistoryVCSInterceptor}
     */
    private static final Set<String> INTERCEPTOR_COMPLETE_WHITELIST = new HashSet<String>(Arrays.asList(
        "isMutable",
        "getAttribute",
        "doDelete",
        "doMove",
        "doCopy",
        "doCreate",
        "afterCopy",
        "refreshRecursively"));
    
    /**
     * files that are implemented in LocalHistoryVCSInterceptor,
     * but do not have to call {@link LocalHistoryStore.waitForProcessedStoring}
     */
    private static final Set<String> WAIT_FOR_STORING_WHITELIST = new HashSet<String>(Arrays.asList(
        "beforeEdit",
        "afterChange",
        "afterDelete",
        "afterMove",
        "afterCreate"));
    
    /**
     * Test whether all important methods are overriden and 
     * if they block in case of an currently running async copying into storage.
     * 
     * @throws IOException
     */
    public void testTestInterceptorComplete() throws IOException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        setupTestStore();
        
        Set<String> testInterceptorMethods = new HashSet<String>();
        Method[]  methods = LocalHistoryVCSInterceptor.class.getDeclaredMethods();
        for (Method method : methods) {
            if((method.getModifiers() & Modifier.PUBLIC) != 0) {
                
                System.out.println(" tested interceptor method: " + method.getName());
                
                if(INTERCEPTOR_COMPLETE_WHITELIST.contains(method.getName())) {
                    fail("method " + method.getName() + " is on whitelist but also iplemented in LocalHistoryVCSInterceptor");
                }
                        
                testInterceptorMethods.add(method.getName());
                
                LocalHistoryVCSInterceptor i = new LocalHistoryVCSInterceptor();
                Class<?>[] params = method.getParameterTypes();
                if(params.length == 1 && params[0] == File.class) {
                    method.invoke(i, new File(method.getName()));
                } else if(params.length == 2 && params[0] == File.class && params[1] == File.class) {
                    method.invoke(i, new File(method.getName()), new File(method.getName() + ".2"));
                } else if(params.length == 2 && params[0] == File.class && params[1] == String.class) {
                    method.invoke(i, new File(method.getName()), "");
                } else if(params.length == 2 && params[0] == File.class && params[1] == boolean.class) {
                    method.invoke(i, new File(method.getName()), false);
                } else if(params.length == 3 && params[0] == File.class && params[1] == long.class && params[2] == List.class) {
                    method.invoke(i, new File(method.getName()), -1, Collections.EMPTY_LIST);
                } else {
                    fail("not yet handled method " + method.getName() + " with parameters: " + Arrays.asList(params));
                }
            }
        }
        
        methods = VCSInterceptor.class.getDeclaredMethods();
        for (Method method : methods) {
            if((method.getModifiers() & Modifier.PUBLIC) != 0) {
                System.out.println(" vcsinterceptor method: " + method.getName());
                if(!INTERCEPTOR_COMPLETE_WHITELIST.contains(method.getName()) && 
                   !testInterceptorMethods.contains(method.getName())) 
                {
                    fail("" + LocalHistoryVCSInterceptor.class.getName() + " should override method " + method.getName());
                }
                
                if(!INTERCEPTOR_COMPLETE_WHITELIST.contains(method.getName()) &&
                   !WAIT_FOR_STORING_WHITELIST.contains(method.getName()) && 
                   !TestLocalHistoryStore.instance.didWaitFor.contains(method.getName())) 
                {
                    fail(" interceptor method " + method.getName() + " DID NOT call waitForProcessedStoring !!! Either implement it or add it to the whitelist.");
                }
            }
        }
        
        for(String m : WAIT_FOR_STORING_WHITELIST) {
            if(TestLocalHistoryStore.instance.didWaitFor.contains(m)) {
                fail("method " + m + " is in whitelist but it blocked anyway. Fix either the whitelist or the method implementation.");
            }
        }
    }  
    
    private void write(File f, String txt) throws IOException {
        FileWriter fw = new FileWriter(f);
        try {
            fw.append(txt);
            fw.flush();
        } finally {
            fw.close();
        }
    }
    
    private void write(FileObject fo, String txt) throws IOException {
        // delay the write until we are sure the timestamp 
        // is going to be diferent to the current one
        long ts = fo.lastModified().getTime();
        File f = new File(getWorkDir(), "toucher");
        if(!f.exists()) f.createNewFile();
        while(f.lastModified() <= ts) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            f.setLastModified(System.currentTimeMillis());
        }
        
        OutputStream os = fo.getOutputStream();
        try {
            os.write(txt.getBytes());
            os.flush();
        } finally {
            os.close();
        }
    }

    private void assertEntry(StoreEntry entry, String txt) throws IOException {
        DataInputStream is = new DataInputStream(entry.getStoreFileInputStream());
        StringBuilder sb = new StringBuilder();
        byte[] b = new byte[1024];
        while(true) {
            int i = is.read(b);
            byte[] s = new byte[i];
            System.arraycopy(b, 0, s, 0, i);
            sb.append(new String(s));
            if(i <= b.length) {
                break;
            } 
        }
        assertEquals(txt, sb.toString());
    }

    private void setupTestStore() throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Class<LocalHistory> c = LocalHistory.class;
        Field f = c.getDeclaredField("store");
        f.setAccessible(true);
        f.set(LocalHistory.getInstance(), new TestLocalHistoryStore());
    }
    
    private static class TestLocalHistoryStore implements LocalHistoryStore {
        static TestLocalHistoryStore instance;

        public TestLocalHistoryStore() {
            instance = this;
        }
        
        List<String> didWaitFor = new LinkedList<String>();
        
        @Override
        public void waitForProcessedStoring(File file, String caller) {
            didWaitFor.add(file.getName());
        }
        @Override
        public void fileCreate(File file, long ts) { }
        @Override
        public void fileDelete(File file, long ts) { }
        @Override
        public void fileCreateFromMove(File fromFile, File toFile, long ts) { }
        @Override
        public void fileDeleteFromMove(File fromFile, File toFile, long ts) { }
        @Override
        public void fileChange(File file, long ts) { }
        @Override
        public StoreEntry setLabel(File file, long ts, String label) { return null; }
        @Override
        public void addVersioningListener(VersioningListener l) { }
        @Override
        public void removeVersioningListener(VersioningListener l) { }
        @Override
        public StoreEntry[] getStoreEntries(File file) { return new StoreEntry[0]; }
        @Override
        public StoreEntry getStoreEntry(File file, long ts) { return null; }
        @Override
        public StoreEntry[] getFolderState(File root, File[] files, long ts) { return new StoreEntry[0]; }
        @Override
        public StoreEntry[] getDeletedFiles(File root) { return new StoreEntry[0]; }
        @Override
        public void deleteEntry(File file, long ts) { }
        @Override
        public void cleanUp(long ttl) { }
    }
}