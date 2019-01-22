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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
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
package org.netbeans.modules.masterfs.filebasedfs.fileobjects;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.masterfs.filebasedfs.FileUtilTest.EventType;
import org.netbeans.modules.masterfs.filebasedfs.FileUtilTest.TestFileChangeListener;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 * 
 * @author Jiri Skrivanek
 */
public class FileObjTest extends NbTestCase {

    public FileObjTest(String testName) {
        super(testName);
    }

    @Override
    protected int timeOut() {
        return 60000;
    }

    /** Tests it is not possible to create duplicate FileObject for the same path.
     * - create FO1
     * - create FO2
     * - delete FO1 => FO1 is invalid now
     * - rename FO2 to FO1
     * - rename FO1 to FO1 => FO1 still invalid
     * - try to write to FO1.getOutputStream() => it should not be possible because FO1 is still invalid
     */
    public void testDuplicateFileObject130998() throws IOException {
        clearWorkDir();
        FileObject testFolder = FileUtil.toFileObject(getWorkDir());
        FileObject fileObject1 = testFolder.createData("fileObject1");
        FileObject fileObject2 = testFolder.createData("fileObject2");
        fileObject1.delete();
        assertFalse("fileObject1 should be invalid after delete.", fileObject1.isValid());

        FileLock lock = fileObject2.lock();
        fileObject2.rename(lock, fileObject1.getName(), null);
        lock.releaseLock();
        assertTrue("fileObject2 should be valid.", fileObject2.isValid());

        lock = fileObject1.lock();
        fileObject1.rename(lock, fileObject1.getName(), null);
        lock.releaseLock();
        assertFalse("fileObject1 should remain invalid after rename.", fileObject1.isValid());
        
        OutputStream os = fileObject1.getOutputStream();
        assertTrue("Valid file", FileUtil.toFile(fileObject1).exists());
        assertFalse("Invalid file object", fileObject1.isValid());
        assertNotNull("Since #211483 it is possible to obtain OutputStream for valid file/invalid fo", os);
    }

    /** #165406 - tests that only one event is fired for single change. */
    public void testChangeEvents165406() throws Exception {
        clearWorkDir();
        File workdir = getWorkDir();
        File file = new File(workdir, "testfile");
        file.createNewFile();
        final FileObject fo = FileUtil.toFileObject(file);
        fo.refresh(); // to set lastModified field
        final long beforeModification = fo.lastModified().getTime();
        Thread.sleep(1000);
        Handler handler = new Handler() {

            @Override
            public void publish(LogRecord record) {
                if (record.getMessage().equals("getOutputStream-close")) {
                    // wait for physical change of timestamp after stream was closed
                    while (beforeModification == fo.lastModified().getTime()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    // call concurrent refresh
                    fo.refresh();
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };

        Logger logger = Logger.getLogger(FileObj.class.getName());
        logger.addHandler(handler);
        logger.setLevel(Level.FINEST);

        TestFileChangeListener listener = new TestFileChangeListener();
        fo.addFileChangeListener(listener);
        
        OutputStream os = fo.getOutputStream();
        os.write("Ahoj everyone!\n".getBytes("UTF-8"));
        os.close();

        assertEquals("Only one change event should be fired.", 1, listener.check(EventType.CHANGED));
    }

    public void testReadOnlyFile() throws Exception {
        clearWorkDir();
        File f = new File(getWorkDir(), "read-only.txt");
        f.createNewFile();
        FileObject fo = FileUtil.toFileObject(f);
        f.setWritable(false);
        try {
            OutputStream os = fo.getOutputStream();
            fail("Can't get the output stream for read-only file: " + os);
        } catch (IOException ex) {
            String msg = Exceptions.findLocalizedMessage(ex);
            assertNotNull("The exception comes with a localized message", msg);
        } finally {
            f.setWritable(true);
        }
    }

    public void testFileObjectForBrokenLinkWithGetChildren() throws Exception {
        if (!Utilities.isUnix()) {
            return;
        }
        doFileObjectForBrokenLink(true);
    }
//    public void testFileObjectForBrokenLink() throws Exception {
//        if (!Utilities.isUnix()) {
//            return;
//        }
//        doFileObjectForBrokenLink(false);
//    }
    
    private void doFileObjectForBrokenLink (boolean listFirst) throws Exception {
        clearWorkDir();
        File wd = new File(getWorkDir(), "wd");
        wd.mkdirs();

        File original = new File(wd, "original");
//        original.createNewFile();
        File lockFile = new File(wd, "wlock");
        for (int i = 1; i <= 2; ++i) {
            try {
                lockFile.delete();
                FileUtil.toFileObject(wd).refresh();
                ProcessBuilder pb = new ProcessBuilder().directory(wd).command(
                    new String[] { "ln", "-s", original.getName(), lockFile.getName() }
                );
                pb.start().waitFor();
                final List<String> names = Arrays.asList(lockFile.getParentFile().list());
                assertEquals("One file", 1, names.size());
                // file exists, or at least dir.listFiles lists the file
                assertTrue(names.contains(lockFile.getName()));
                // java.io.File.exists returns false
                assertFalse(lockFile.exists());

                if (listFirst) {
                    FileObject root = FileUtil.toFileObject(wd);
                    root.refresh();
                    List<FileObject> arr = Arrays.asList(root.getChildren());
                    assertEquals("Round " + i + " One files: " + arr, 1, arr.size());
                    assertEquals("Round " + i + "Has the right name", lockFile.getName(), arr.get(0).getName());
                }

                // and no FileObject is reated for such a file
                assertNotNull(FileUtil.toFileObject(lockFile));
            } finally {
                lockFile.delete();
            }
        }
    }
}