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

package org.netbeans.lib.profiler.results.cpu.cct;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainerFree;
import org.netbeans.lib.profiler.results.cpu.TimingAdjusterOld;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.TimedCPUCCTNode;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.RuntimeCCTNodeProcessor;


/**
 *
 * @author Jaroslav Bachorik
 */
public class CCTFlattener extends RuntimeCCTNodeProcessor.PluginAdapter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(CCTFlattener.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object containerGuard = new Object();

    // @GuardedBy containerGuard
    private FlatProfileContainer container;
    private ProfilerClient client;
    private Stack parentStack;
    private int[] invDiff;
    private int[] invPM;
    private int[] nCalleeInvocations;
    private long[] timePM0;
    private long[] timePM1;
    private int nMethods;

    private CCTResultsFilter currentFilter = null;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CCTFlattener(ProfilerClient client, CCTResultsFilter filter) {
        this.client = client;
        parentStack = new Stack();
        this.currentFilter = filter;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public FlatProfileContainer getFlatProfile() {
        synchronized (containerGuard) {
            return container;
        }
    }

    @Override
    public void onStart() {
        ProfilingSessionStatus status = client.getStatus();
        nMethods = status.getNInstrMethods();
        timePM0 = new long[nMethods];
        timePM1 = new long[status.collectingTwoTimeStamps() ? nMethods : 0];
        invPM = new int[nMethods];
        invDiff = new int[nMethods];
        nCalleeInvocations = new int[nMethods];
        parentStack.clear();

//        currentFilter = (CCTResultsFilter)Lookup.getDefault().lookup(CCTResultsFilter.class);
        
        synchronized (containerGuard) {
            container = null;
        }
        
        // uncomment the following piece of code when trying to reproduce #205482
//        try {
//            Thread.sleep(120);
//        } catch (InterruptedException interruptedException) {
//        }
    }

    @Override
    public void onStop() {
        ProfilingSessionStatus status = client.getStatus();

        // Now convert the data into microseconds
        long wholeGraphTime0 = 0;

        // Now convert the data into microseconds
        long wholeGraphTime1 = 0;
        long totalNInv = 0;

        for (int i = 0; i < nMethods; i++) {
            double time = TimingAdjusterOld.getInstance(status)
                                           .adjustTime(timePM0[i], (invPM[i] + invDiff[i]), (nCalleeInvocations[i] + invDiff[i]),
                                                       false);

            if (time < 0) {
                // in some cases the combination of cleansing the time by calibration and subtracting wait/sleep
                // times can lead to <0 time
                // see http://profiler.netbeans.org/issues/show_bug.cgi?id=64416
                time = 0;
            }

            timePM0[i] = (long) time;

            // don't include the Thread time into wholegraphtime
            if (i > 0) {
                wholeGraphTime0 += time;
            }

            if (status.collectingTwoTimeStamps()) {
                time = TimingAdjusterOld.getInstance(status)
                                        .adjustTime(timePM1[i], (invPM[i] + invDiff[i]), (nCalleeInvocations[i] + invDiff[i]),
                                                    true);
                timePM1[i] = (long) time;

                // don't include the Thread time into wholegraphtime
                if (i > 0) {
                    wholeGraphTime1 += time;
                }
            }

            totalNInv += invPM[i];
        }

        synchronized (containerGuard) {
            container = new FlatProfileContainerFree(status, timePM0, timePM1, invPM, new char[0], wholeGraphTime0,
                                                     wholeGraphTime1, invPM.length);
        }

        timePM0 = timePM1 = null;
        invPM = invDiff = nCalleeInvocations = null;
        parentStack.clear();
//        currentFilter = null;
    }
    
    @Override
    public void onNode(MethodCPUCCTNode node) {
        final int nodeMethodId = node.getMethodId();
                
        ProfilingSessionStatus status = client.getStatus();
        InstrumentationFilter filter = client.getSettings().getInstrumentationFilter();

        MethodCPUCCTNode currentParent = parentStack.isEmpty() ? null : (MethodCPUCCTNode) parentStack.peek();
        boolean filteredOut = (node.getFilteredStatus() == TimedCPUCCTNode.FILTERED_YES); // filtered out by rootmethod/markermethod rules

        if (!filteredOut && (client.getSettings().getCPUProfilingType() == CommonConstants.CPU_SAMPLED || node.getFilteredStatus() == TimedCPUCCTNode.FILTERED_MAYBE)) { // filter out all methods not complying to instrumentation filter & secure to remove

            String jvmClassName = status.getInstrMethodClasses()[nodeMethodId].replace('.', '/');

            if (!filter.passesFilter(jvmClassName)) {
                filteredOut = true;
            }
        }

        if (!filteredOut && (currentFilter != null)) {
            filteredOut = !currentFilter.passesFilter(); // finally use the mark filter
        }
        final int parentMethodId = currentParent != null ? currentParent.getMethodId() : -1;
        
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Processing runtime node: {0}.{1}; filtered={2}, time={3}, CPU time={4}", // NOI18N
                       new Object[]{status.getInstrMethodClasses()[nodeMethodId], status.getInstrMethodNames()[nodeMethodId], 
                       filteredOut, node.getNetTime0(), node.getNetTime1()});

            String parentInfo = (currentParent != null)
                                ? (status.getInstrMethodClasses()[parentMethodId] + "."
                                + status.getInstrMethodNames()[parentMethodId]) : "none"; // NOI18N
            LOGGER.log(Level.FINEST, "Currently used parent: {0}", parentInfo); // NOI18N
        }

        if (filteredOut) {
            if ((currentParent != null) && !currentParent.isRoot()) {
                invDiff[parentMethodId] += node.getNCalls();

                timePM0[parentMethodId] += node.getNetTime0();

                if (status.collectingTwoTimeStamps()) {
                    timePM1[parentMethodId] += node.getNetTime1();
                }
            }
        } else {
            timePM0[nodeMethodId] += node.getNetTime0();

            if (status.collectingTwoTimeStamps()) {
                timePM1[nodeMethodId] += node.getNetTime1();
            }

            invPM[nodeMethodId] += node.getNCalls();

            if ((currentParent != null) && !currentParent.isRoot()) {
                nCalleeInvocations[parentMethodId] += node.getNCalls();
            }

            currentParent = node;
        }
        parentStack.push(node);
    }

    @Override
    public void onBackout(MethodCPUCCTNode node) {
        parentStack.pop();
    }
}