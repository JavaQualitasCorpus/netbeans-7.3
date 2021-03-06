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
package org.netbeans.modules.sampler;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.actions.Openable;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.modules.Places;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import static org.netbeans.modules.sampler.Bundle.*;

/**
 *
 * @author Tomas Hurka
 */
final class InternalSampler extends Sampler {
    private static final String SAMPLER_NAME = "selfsampler";  // NOI18N
    private static final String FILE_NAME = SAMPLER_NAME+SamplesOutputStream.FILE_EXT;
    private static final String UNKNOW_MIME_TYPE = "content/unknown"; // NOI18N
    private static final String DEBUG_ARG = "-Xdebug"; // NOI18N
    private static final Logger LOGGER = Logger.getLogger(InternalSampler.class.getName());
    private static Boolean debugMode;
    private static String lastReason;

    private ProgressHandle progress;

    static InternalSampler createInternalSampler(String key) {
        if (SamplesOutputStream.isSupported() && isRunMode()) {
            return new InternalSampler(key);
        }
        return null;
    }

    private static synchronized boolean isDebugged() {
        if (debugMode == null) {
            debugMode = Boolean.FALSE;

            // check if we are debugged
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            List<String> args = runtime.getInputArguments();
            if (args.contains(DEBUG_ARG)) {
                debugMode = Boolean.TRUE;
            }
        }
        return debugMode.booleanValue();
    }

    private static boolean isRunMode() {
        boolean runMode = true;
        String reason = null;

        if (isDebugged()) {
            reason = "running in debug mode";   // NOI18N
            runMode = false;
        }
        if (runMode) {
            // check if netbeans is profiled
            try {
                Class.forName("org.netbeans.lib.profiler.server.ProfilerServer", false, ClassLoader.getSystemClassLoader()); // NO18N
                reason = "running under profiler";   // NOI18N
                runMode = false;
            } catch (ClassNotFoundException ex) {
            }
        }
        if (!runMode && !reason.equals(lastReason)) {
            LOGGER.log(Level.INFO, "Slowness detector disabled - {0}", reason); // NOI18N
        }
        lastReason = reason;
        return runMode;
    }
    
    InternalSampler(String thread) {
        super(thread);
    }

    @Override
    protected void printStackTrace(Throwable ex) {
        Exceptions.printStackTrace(ex);
    }

    @Override
    @Messages("SelfSamplerAction_SavedFile=Snapshot was saved to {0}")
    protected void saveSnapshot(byte[] arr) throws IOException { // save snapshot
        File outFile = File.createTempFile(SAMPLER_NAME, SamplesOutputStream.FILE_EXT);
        File userDir = Places.getUserDirectory();
        File gestures = null;
        SelfSampleVFS fs;
        
        outFile = FileUtil.normalizeFile(outFile);
        writeToFile(outFile, arr);
        if (userDir != null) {
            gestures = new File(new File(new File(userDir, "var"), "log"), "uigestures"); // NOI18N
        }
        if (gestures != null && gestures.exists()) {
            fs = new SelfSampleVFS(new String[]{FILE_NAME, SAMPLER_NAME+".log"}, new File[]{outFile, gestures});  // NOI18N
        } else {
            fs = new SelfSampleVFS(new String[]{FILE_NAME}, new File[]{outFile});
        }
        // open snapshot
        FileObject fo = fs.findResource(FILE_NAME);
        // test for DefaultDataObject
        if (UNKNOW_MIME_TYPE.equals(fo.getMIMEType())) {
            String msg = SelfSamplerAction_SavedFile(outFile.getAbsolutePath());
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(msg));
        } else {
            DataObject dobj = DataObject.find(fo);
            dobj.getLookup().lookup(Openable.class).open();
        }
    }

    private void writeToFile(File file, byte[] arr) {
        try {
            FileOutputStream fstream = new FileOutputStream(file);
            fstream.write(arr);
            fstream.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    ThreadMXBean getThreadMXBean() {
        return ManagementFactory.getThreadMXBean();
    }

    @Override
    @Messages("Save_Progress=Saving snapshot")
    void openProgress(final int steps) {
        if (EventQueue.isDispatchThread()) {
            // log warnining
            return;
        }
        progress = ProgressHandleFactory.createHandle(Save_Progress());
        progress.start(steps);
    }

    @Override
    void closeProgress() {
        if (EventQueue.isDispatchThread()) {
            return;
        }
        progress.finish();
        progress = null;
    }

    @Override
    void progress(int i) {
        if (EventQueue.isDispatchThread()) {
            return;
        }
        if (progress != null) {
            progress.progress(i);
        }
    }
    
}
