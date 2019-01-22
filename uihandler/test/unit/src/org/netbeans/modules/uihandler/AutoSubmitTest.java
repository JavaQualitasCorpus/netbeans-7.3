/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.uihandler;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;
import javax.swing.SwingUtilities;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.uihandler.api.Activated;
import org.netbeans.modules.uihandler.api.Deactivated;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Martin Entlicher
 */
public class AutoSubmitTest extends NbTestCase {
    
    private Installer installer;
    
    static {
        MemoryURL.initialize();
    }
    
    public AutoSubmitTest(String testName) {
        super(testName);
    }
    
    @Override
    protected boolean runInEQ() {
        return false;
    }

    @Override
    protected void setUp() throws Exception {
        UIHandler.flushImmediatelly();
        MetricsHandler.flushImmediatelly();
        System.setProperty("netbeans.user", getWorkDirPath());
        clearWorkDir();
        Locale.setDefault(new Locale("ts", "AU"));
        NbPreferences.root().node("org/netbeans/core").putBoolean("usageStatisticsEnabled", true);
        
        installer = Installer.findObject(Installer.class, true);
        assertNotNull(installer);
        MockServices.setServices(A.class, D.class);
        MemoryURL.initialize();
    }
    
    /**
     * Tests auto-submission of UI logs.
     * 
     * @throws Exception 
     */
    public void testAutoSubmitUILogs() throws Exception {
        // Needs to be call in the thread's context class loader, due to mockup services
        Installer.logDeactivated();
        // setup the listing
        //installer.restored();
        File logs = File.createTempFile("autoSubmitLogs", ".xml");
        logs.deleteOnExit();
        String utf8 = 
            "<html><head>" +
            "<meta http-equiv='Content-Type' content='text/html; charset=utf-8'></meta>" +
            "</head>" +
            "<body>" +
            "<form action='file://"+logs.getAbsolutePath()+"' method='post'>" +
            "  <input name='submit' value='auto' type='hidden'> </input>" +
            "</form>" +
            "</body></html>";
        //ByteArrayInputStream is = new ByteArrayInputStream(utf8.getBytes("UTF-8"));
        
        
        Preferences prefs = NbPreferences.forModule(Installer.class);
        prefs.putBoolean("autoSubmitWhenFull", true);
        //LogRecord r = new LogRecord(Level.INFO, "MSG_SOMETHING");
        //r.setLoggerName("org.netbeans.ui.anything");
        /*
        {
            Logger logger = Logger.getLogger("org.netbeans.ui.anything");
            while (logger != null) {
                Handler[] handlers = logger.getHandlers();
                System.err.println("Logger "+logger.getName()+" handlers = "+Arrays.asList(handlers));
                if (!logger.getUseParentHandlers()) {
                    break;
                }
                logger = logger.getParent();
            }
        }
        */
        int n1 = Installer.getLogsSizeTest();
        int n = UIHandler.MAX_LOGS + 1 - n1;
        for (int i = 0; i < n; i++) {
            //Installer.writeOut(r);
            MemoryURL.registerURL("memory://auto.html", utf8);
            LogRecord r = new LogRecord(Level.INFO, "MSG_SOMETHING"+i);
            r.setLoggerName("org.netbeans.ui.anything");
            Logger.getLogger("org.netbeans.ui.anything").log(r);
        }
        UIHandler.waitFlushed();
        
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                // Empty just to get over the last scheduled task invoked in AWT.
            }
        });
        waitTillTasksInRPFinish(Installer.RP_SUBMIT);
        
        checkMessagesIn(logs, n);
        logs.delete();
    }
    
    private static void waitTillTasksInRPFinish(RequestProcessor rp) throws Exception {
        // Perhaps there is a better way to wait untill all scheduled tasks are processed...?
        do {
            Field runningFiled = RequestProcessor.class.getDeclaredField("running");
            runningFiled.setAccessible(true);
            Integer running = (Integer) runningFiled.get(rp);
            if (running.intValue() == 0) {
                break;
            } else {
                Thread.sleep(500);
            }
        } while (true);
    }
    
    private static void checkMessagesIn(File logs, int n) throws Exception {
        InputStream in = new FileInputStream(logs);
        byte[] bytes = new byte[(int) logs.length()];
        int from = 0;
        int l;
        while ((l = in.read(bytes, from, bytes.length - from)) > 0) {
            from += l;
        }
        in.close();
        l = from;
        //System.err.println("Read file length = "+l);
        //System.err.println("File size = "+logs.length());
        // Skip the header:
        // GZIP header magic number.
        byte b0 = GZIPInputStream.GZIP_MAGIC & 255;
        byte b1 = (byte) (GZIPInputStream.GZIP_MAGIC >> 8);
        
        int begin = 0;
        for (int i = 0; i < (l - 1); i++) {
            if (bytes[i] == b0 && bytes[i + 1] == b1) {
                begin = i;
                break;
            }
        }
        
        //System.err.println("Begin = "+begin);
        
        GZIPInputStream gin = new GZIPInputStream(new ByteArrayInputStream(bytes, begin, l - begin));
        BufferedReader br = new BufferedReader(new InputStreamReader(gin));
        //StringBuilder sb = new StringBuilder();
        String line;
        String lastMessage = "<message>MSG_SOMETHING"+(n - 1)+"</message>";
        String afterLastMessage = "<message>MSG_SOMETHING"+n+"</message>";
        boolean isLastMessage = false;
        boolean isAfterLastMessage = false;
        while ((line = br.readLine()) != null) {
            //sb.append(line);
            //sb.append('\n');
            line = line.trim();
            if (lastMessage.equals(line)) {
                isLastMessage = true;
            }
            if (afterLastMessage.equals(line)) {
                isAfterLastMessage = true;
            }
        }
        br.close();
        //System.err.println("AUTO FILE CONTENT = "+sb);
        //System.err.println("isLastMessage("+lastMessage+") = "+isLastMessage);
        //System.err.println("isAfterLastMessage("+afterLastMessage+") = "+isAfterLastMessage);
        
        assertTrue("Last message '"+lastMessage+"' not found in the log.", isLastMessage);
        assertFalse("Messages following the last message '"+lastMessage+"' are present in the log.", isAfterLastMessage);
    }

    public static final class A implements Activated {
        @Override
        public void activated(Logger uiLogger) {
            //uiLogger.config("A started");
        }
    }
    public static final class D implements Deactivated {
        @Override
        public void deactivated(Logger uiLogger) {
            //uiLogger.config("D stopped");
        }
    }
}