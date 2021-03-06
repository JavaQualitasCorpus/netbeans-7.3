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
package org.netbeans.modules.nativeexecution.api.util;

import com.jcraft.jsch.Channel;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RemoteMeasurements {

    private static final ConcurrentHashMap<Integer, String> stacks = new ConcurrentHashMap<Integer, String>();
    // [stackid-category-args]-to-statistics
    private final ConcurrentHashMap<Integer, Stat> stats = new ConcurrentHashMap<Integer, Stat>();
    private final AtomicLong upTraffic = new AtomicLong();
    private final AtomicLong downTraffic = new AtomicLong();
    private final String name;
    private final long startWallTime;

    RemoteMeasurements(String name) {
        this.name = name;
        startWallTime = System.currentTimeMillis();
    }

    public Object stratChannelActivity(CharSequence category, Channel channel, CharSequence... args) {
        Stat stat = new Stat(category, args);
        int hashCode = stat.hashCode();
        Stat prevStat = stats.putIfAbsent(hashCode, stat);
        if (prevStat != null) {
            stat = prevStat;
        }

        stat.count.incrementAndGet();
        return new StatKey(hashCode, System.currentTimeMillis());
    }

    public void stopChannelActivity(Object activityID) {
        StatKey key = (StatKey) activityID;
        Stat stat = stats.get(key.statID);
        assert stat != null;
        stat.deltaTime.addAndGet(System.currentTimeMillis() - key.startTime);
    }

    void dump(PrintStream out) {
        try {
            dumpTrafficStatistics(out);
            dumpCategoriesStatistics(out);
            dumpCategoriesArgsStatistics(out);
            dumpCategoriesStacksStatistics(out);
        } finally {
            out.println("Total wall time '" + name + "' [ms]: " + (System.currentTimeMillis() - startWallTime)); // NOI18N
        }
    }

    private void dumpTrafficStatistics(PrintStream out) {
        out.println("Upload traffic '" + name + "' [bytes]: " + upTraffic.get()); // NOI18N
        out.println("Download traffic '" + name + "' [bytes]: " + downTraffic.get()); // NOI18N
    }

    private void dumpCategoriesArgsStatistics(PrintStream out) {
        HashMap<String, Counters> data = new HashMap<String, Counters>();

        for (Stat stat : stats.values()) {
            String id = stat.category + "%" + Arrays.toString(stat.args); // NOI18N
            Counters counters = data.get(id);
            if (counters == null) {
                counters = new Counters();
                data.put(id, counters);
            }

            counters.cnt1.addAndGet(stat.count.get());
            counters.cnt2.addAndGet(stat.deltaTime.get());
        }

        out.println("== Arguments statistics start =="); // NOI18N
        out.println("Category|Count|Time|Args"); // NOI18N

        LinkedList<Map.Entry<String, Counters>> dataList = new LinkedList<Map.Entry<String, Counters>>(data.entrySet());
        Collections.sort(dataList, new CategoriesStatComparator());

        for (Map.Entry<String, Counters> entry : dataList) {
            String cat = entry.getKey();
            int idx = cat.indexOf('%');
            Counters cnts = entry.getValue();
            out.println(cat.substring(0, idx) + "|" + cnts.cnt1 + "|" + cnts.cnt2 + "|" + cat.substring(idx + 1)); // NOI18N
        }

        out.println("== Arguments statistics end =="); // NOI18N
    }

    private void dumpCategoriesStacksStatistics(PrintStream out) {
        HashMap<String, Counters> data = new HashMap<String, Counters>();
        
        for (Stat stat : stats.values()) {
            String id = stat.stackID + "%" + stat.category; // NOI18N
            Counters counters = data.get(id);
            if (counters == null) {
                counters = new Counters();
                data.put(id, counters);
            }

            counters.cnt1.addAndGet(stat.count.get());
            counters.cnt2.addAndGet(stat.deltaTime.get());
        }

        out.println("== Categories stacks statistics start =="); // NOI18N
        out.println("Category|Count|Time|Stack"); // NOI18N

        LinkedList<Map.Entry<String, Counters>> dataList = new LinkedList<Map.Entry<String, Counters>>(data.entrySet());
        Collections.sort(dataList, new CategoriesStatComparator());

        for (Map.Entry<String, Counters> entry : dataList) {
            String cat = entry.getKey();
            int idx = cat.indexOf('%');
            Counters cnts = entry.getValue();
            Integer stackID = Integer.valueOf(cat.substring(0, idx));
            String category = cat.substring(idx + 1);
            out.println(category + "|" + cnts.cnt1 + "|" + cnts.cnt2 + "|" + stacks.get(stackID)); // NOI18N
        }

        out.println("== Categories stacks statistics end =="); // NOI18N
    }

    private void dumpCategoriesStatistics(PrintStream out) {
        long totalTime = 0;
        HashMap<String, Counters> map = new HashMap<String, Counters>();
        for (Stat stat : stats.values()) {
            Counters counters = map.get(stat.category);
            if (counters == null) {
                counters = new Counters();
                map.put(stat.category, counters);
            }
            counters.cnt1.addAndGet(stat.count.get());
            counters.cnt2.addAndGet(stat.deltaTime.get());
            counters.cnt3.add(Arrays.deepHashCode(stat.args));
        }

        out.println("== Categories stat begin =="); // NOI18N
        out.println("Category|Count|Time|Unique args"); // NOI18N
        for (Map.Entry<String, Counters> entry : map.entrySet()) {
            long dt = entry.getValue().cnt2.get();
            out.println(entry.getKey() + "|" + entry.getValue().cnt1 + "|" + dt + "|" + entry.getValue().cnt3.size()); // NOI18N
            totalTime += dt;
        }
        out.println("== Categories stat end =="); // NOI18N

        out.println("Total time by all categories [ms]: " + totalTime); // NOI18N
    }

    private static int getStackID() {
        if (!RemoteStatistics.COLLECT_STACKS) {
            return -1;
        }

        final StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace != null) {
            String curFile;
            String prevFile = null;
            for (int i = stackTrace.length - 1; i >= 0; i--) {
                StackTraceElement st = stackTrace[i];
                String filename = st.getFileName();
                curFile = filename.substring(0, filename.lastIndexOf('.'));
                sb.append(curFile.equals(prevFile) ? "" : curFile).append(':').append(st.getLineNumber()).append(';'); // NOI18N
                prevFile = curFile;
                String className = st.getClassName();
                if (className.startsWith("com.jcraft.jsch")) { // NOI18N
                    sb.append("..."); // NOI18N
                    break;
                }
            }
        }
        String stack = sb.toString();
        Integer stackID = stack.hashCode();
        stacks.putIfAbsent(stackID, stack);

        return stackID;
    }

    void bytesUploaded(int bytes) {
        upTraffic.addAndGet(bytes);
    }

    void bytesDownloaded(int bytes) {
        downTraffic.addAndGet(bytes);
    }

    private static final class Stat {

        private final String category;
        private final String[] args;
        private final int stackID;
        private final AtomicLong deltaTime = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);

        public Stat(CharSequence category, CharSequence... args) {
            stackID = getStackID();
            this.category = category.toString();
            if (args == null) {
                this.args = null;
            } else {
                this.args = new String[args.length];
                int i = 0;
                for (CharSequence arg : args) {
                    this.args[i++] = arg.toString();
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Stat other = (Stat) obj;
            if (this.stackID != other.stackID) {
                return false;
            }
            if ((this.category == null) ? (other.category != null) : !this.category.equals(other.category)) {
                return false;
            }
            if (!Arrays.deepEquals(this.args, other.args)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + this.stackID;
            hash = 79 * hash + (this.category != null ? this.category.hashCode() : 0);
            hash = 79 * hash + Arrays.deepHashCode(this.args);
            return hash;
        }
    }

    private static final class Counters {

        private final AtomicLong cnt1 = new AtomicLong();
        private final AtomicLong cnt2 = new AtomicLong();
        private final HashSet<Integer> cnt3 = new HashSet<Integer>();
    }

    private static class CategoriesStatComparator implements Comparator<Map.Entry<String, Counters>> {

        @Override
        public int compare(Map.Entry<String, Counters> o1, Map.Entry<String, Counters> o2) {
            String s1 = o1.getKey();
            String s2 = o2.getKey();
            int idx1 = s1.indexOf('%');
            int idx2 = s2.indexOf('%');
            int cmp = s1.substring(0, idx1).compareTo(s2.substring(0, idx2));
            if (cmp != 0) {
                return cmp;
            }
            long o1Val = o1.getValue().cnt2.get();
            long o2Val = o2.getValue().cnt2.get();
            return (o1Val < o2Val ? 1 : (o1Val == o2Val ? 0 : -1));
        }
    }

    private static final class StatKey {

        private final int statID;
        private final long startTime;

        private StatKey(int statID, long startTime) {
            this.statID = statID;
            this.startTime = startTime;
        }
    }
}
