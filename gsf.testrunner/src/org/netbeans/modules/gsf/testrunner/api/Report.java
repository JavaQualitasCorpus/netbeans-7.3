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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.gsf.testrunner.api;

import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.netbeans.api.extexecution.print.LineConvertors.FileLocator;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;

/**
 * Data structure (model) of results of test results.
 * The data are built by the {@link TestRecognizer}.
 * 
 * <i>This is a modified version for <code>o.n.m.junit.output.Report</code>.</i>
 *
 * @author  Marian Petras, Erno Mononen
 */
public final class Report {

    private String suiteClassName;
    /**
     * number of recognized (by the parser) passed test reports
     */
    private int totalTests;
    private int passed;
    private int passedWithErrors;
    private int failures;
    private int errors;
    private int pending;
    private int skippedNum;
    private long elapsedTimeMillis;
    private int detectedPassedTests;
    private Collection<Testcase> tests;
    private FileLocator fileLocator;
    private WeakReference<Project> project;
    private final URI projectURI;
    private boolean aborted;
    private boolean skipped;

    protected boolean completed;

    /**
     */
    public Report(String suiteClassName, Project project) {
        this.suiteClassName = suiteClassName;
        this.project = new WeakReference<Project>(project);
        this.projectURI = project.getProjectDirectory().toURI();
        this.fileLocator = project.getLookup().lookup(FileLocator.class);
        this.tests = new ArrayList<Testcase>(10);
        this.completed = true;
        this.aborted = false;
        this.skipped = false;
    }

    public FileLocator getFileLocator() {
        return fileLocator;
    }

    public Project getProject() {
        Project prj = project.get();
        if (prj == null) {
            prj = FileOwnerQuery.getOwner(projectURI);
	    assert prj != null : "Project was null for projectURI: " + projectURI; //NOI18N
            project = new WeakReference<Project>(prj);
        }
        return prj;
    }

    /**
     */
    public void reportTest(Testcase test) {
        
        //PENDING - should be synchronized
        tests.add(test);
        
        if (!Status.isFailureOrError(test.getStatus()) && !Status.isSkipped(test.getStatus())) {
            detectedPassedTests++;
        }
    }
    
    /**
     */
    public void update(Report report) {
        synchronized(this){
            this.suiteClassName = report.suiteClassName;
            this.totalTests = report.totalTests;
            this.passed = report.passed;
            this.passedWithErrors = report.passedWithErrors;
            this.failures = report.failures;
            this.errors = report.errors;
            this.pending = report.pending;
            this.elapsedTimeMillis = report.elapsedTimeMillis;
            this.detectedPassedTests = report.detectedPassedTests;
            this.tests = report.tests;
            this.completed = report.completed;
            this.skipped = report.skipped;
            this.skippedNum = report.skippedNum;
        }
    }
    
    public Status getStatus() {
        if (aborted){
            return Status.ABORTED;
        } else if (skippedNum > 0) {
            return Status.SKIPPED;
        } else if (errors > 0) {
            return Status.ERROR;
        } else if (failures > 0) {
            return Status.FAILED;
        } else if (pending > 0) {
            return Status.PENDING;
        } else if (passedWithErrors > 0) {
            return Status.PASSEDWITHERRORS;
        }
        return Status.PASSED;
    }
    
    /**
     */
    public Collection<Testcase> getTests() {
        
        /*
         * May be called both from the EventDispatch thread and
         * from other threads!
         *
         * TestSuiteNodeChildren.setFiltered() ... EventDispatch thread
         * TestSuiteNodeChildren.addNotify() ... EventDispatch thread or else
         */
        
        //PENDING - should be synchronized
        if (tests.isEmpty()) {
            final Collection<Testcase> emptyList = Collections.emptyList();
            return emptyList;
        } else {
            return new ArrayList<Testcase>(tests);
        }
    }
    
    /**
     */
    public boolean containsFailed() {
        assert EventQueue.isDispatchThread();

        /* Called from the EventDispatch thread */

        return (failures + errors) != 0;
    }

    /**
     * @return the suiteClassName
     */
    public String getSuiteClassName() {
        return suiteClassName;
    }

    /**
     * @param suiteClassName the suiteClassName to set
     */
    public void setSuiteClassName(String suiteClassName) {
        this.suiteClassName = suiteClassName;
    }

    /**
     * @return the totalTests
     */
    public int getTotalTests() {
        return totalTests;
    }

    /**
     * @param totalTests the totalTests to set
     */
    public void setTotalTests(int totalTests) {
        this.totalTests = totalTests;
    }

    /**
     * @return the passed
     */
    public int getPassed() {
        return passed;
    }

    /**
     * @param passed the passed to set
     */
    public void setPassed(int passed) {
        this.passed = passed;
    }

    /**
     * @return the passedWithErrors
     */
    public int getPassedWithErrors() {
        return passedWithErrors;
    }

    /**
     * @param passedWithErrors the passedWithErrors to set
     */
    public void setPassedWithErrors(int passedWithErrors) {
        this.passedWithErrors = passedWithErrors;
    }

    /**
     * @return the the number of skipped tests
     */
    public int getSkipped() {
        return skippedNum;
    }

    /**
     * @param skipped the number of skipped tests to set
     */
    public void setSkipped(int skipped) {
        this.skippedNum = skipped;
    }

    /**
     * @return the failures
     */
    public int getFailures() {
        return failures;
    }

    /**
     * @param failures the failures to set
     */
    public void setFailures(int failures) {
        this.failures = failures;
    }

    /**
     * @return the errors
     */
    public int getErrors() {
        return errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(int errors) {
        this.errors = errors;
    }

    /**
     * @return the pending
     */
    public int getPending() {
        return pending;
    }

    /**
     * @param pending the pending to set
     */
    public void setPending(int pending) {
        this.pending = pending;
    }

    /**
     * @return the elapsedTimeMillis
     */
    public long getElapsedTimeMillis() {
        return elapsedTimeMillis;
    }

    /**
     * @param elapsedTimeMillis the elapsedTimeMillis to set
     */
    public void setElapsedTimeMillis(long elapsedTimeMillis) {
        this.elapsedTimeMillis = elapsedTimeMillis;
    }

    /**
     * @return the detectedPassedTests
     */
    public int getDetectedPassedTests() {
        return detectedPassedTests;
    }

    /**
     * @param detectedPassedTests the detectedPassedTests to set
     */
    public void setDetectedPassedTests(int detectedPassedTests) {
        this.detectedPassedTests = detectedPassedTests;
    }

    /**
     * @param tests the tests to set
     */
    public void setTests(Collection<Testcase> tests) {
        this.tests = tests;
    }

    /**
     * @param fileLocator the fileLocator to set
     */
    public void setFileLocator(FileLocator fileLocator) {
        this.fileLocator = fileLocator;
    }

    /**
     * @param project the project to set
     */
    public void setProject(Project project) {
        this.project = new WeakReference<Project>(project);
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    int getStatusMask(){
        int statusMask = 0;
        statusMask |= getPassed() > 0 ? Status.PASSED.getBitMask() : 0;
        statusMask |= getPassedWithErrors() > 0 ? Status.PASSEDWITHERRORS.getBitMask() : 0;
        statusMask |= getFailures() > 0 ? Status.FAILED.getBitMask() : 0;
        statusMask |= getErrors() > 0 ? Status.ERROR.getBitMask() : 0;
        statusMask |= getSkipped() > 0 ? Status.SKIPPED.getBitMask() : 0;
        return statusMask;
    }
}