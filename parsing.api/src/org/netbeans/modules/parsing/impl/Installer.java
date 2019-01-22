/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 */
package org.netbeans.modules.parsing.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.modules.parsing.impl.indexing.LogContext;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.modules.parsing.impl.indexing.lucene.DocumentBasedIndexManager;
import org.netbeans.modules.parsing.impl.indexing.lucene.LuceneIndexFactory;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    private static volatile boolean closed;

    public static boolean isClosed() {
        return closed;
    }

    @Override
    public void restored () {
        super.restored();
        RepositoryUpdater.getDefault().start(false);

        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run () {
                RequestProcessor.getDefault().post(new Runnable() {
                    @Override
                    public void run() {
                        Schedulers.init();
                    }
                });
            }
        });
    }

    @Override
    public boolean closing() {
        LogContext.notifyClosing();
        return super.closing();
    }

    @Override
    public void close() {
        super.close();
        closed = true;
        final CountDownLatch done = new CountDownLatch(1);
        final Runnable postTask = new Runnable() {
            private AtomicBoolean started = new AtomicBoolean();
            @Override
            public void run() {
                if (started.compareAndSet(false, true)) {                    
                    try {
                        LuceneIndexFactory.getDefault().close();
                        DocumentBasedIndexManager.getDefault().close();
                    } finally {
                        done.countDown();
                    }
                }
            }
        };
        try {
            RepositoryUpdater.getDefault().stop(postTask);
        } catch (TimeoutException timeout) {
            postTask.run();
        } finally {
            try {
                done.await();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}