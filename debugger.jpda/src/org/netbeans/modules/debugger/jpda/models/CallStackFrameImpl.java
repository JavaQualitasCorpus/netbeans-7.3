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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

package org.netbeans.modules.debugger.jpda.models;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.NativeMethodException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;

import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.StepRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.jpda.CallStackFrame;
import org.netbeans.api.debugger.jpda.JPDAClassType;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.api.debugger.jpda.LocalVariable;
import org.netbeans.api.debugger.jpda.MonitorInfo;
import org.netbeans.api.debugger.jpda.This;
import org.netbeans.modules.debugger.jpda.EditorContextBridge;
import org.netbeans.modules.debugger.jpda.JPDADebuggerImpl;
import org.netbeans.modules.debugger.jpda.jdi.ClassNotPreparedExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.IllegalThreadStateExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.InternalExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.InvalidRequestStateExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.InvalidStackFrameExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.LocationWrapper;
import org.netbeans.modules.debugger.jpda.jdi.MethodWrapper;
import org.netbeans.modules.debugger.jpda.jdi.MirrorWrapper;
import org.netbeans.modules.debugger.jpda.jdi.NativeMethodExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.ObjectCollectedExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.ReferenceTypeWrapper;
import org.netbeans.modules.debugger.jpda.jdi.StackFrameWrapper;
import org.netbeans.modules.debugger.jpda.jdi.ThreadReferenceWrapper;
import org.netbeans.modules.debugger.jpda.jdi.TypeComponentWrapper;
import org.netbeans.modules.debugger.jpda.jdi.TypeWrapper;
import org.netbeans.modules.debugger.jpda.jdi.VMDisconnectedExceptionWrapper;
import org.netbeans.modules.debugger.jpda.jdi.ValueWrapper;
import org.netbeans.modules.debugger.jpda.jdi.VirtualMachineWrapper;
import org.netbeans.modules.debugger.jpda.jdi.request.EventRequestManagerWrapper;
import org.netbeans.modules.debugger.jpda.jdi.request.EventRequestWrapper;
import org.netbeans.modules.debugger.jpda.util.Executor;
import org.netbeans.modules.debugger.jpda.util.JPDAUtils;
import org.netbeans.modules.debugger.jpda.util.Operator;
import org.netbeans.spi.debugger.jpda.EditorContext.MethodArgument;
import org.netbeans.spi.debugger.jpda.EditorContext.Operation;
import org.openide.ErrorManager;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;


/**
* Class representating one line of callstack.
*/
public class CallStackFrameImpl implements CallStackFrame {
    
    private final JPDAThreadImpl thread;
    private StackFrame          sf;
    private Location            sfLocation;
    private int                 depth;
    private JPDADebuggerImpl    debugger;
    //private AST                 ast;
    private volatile Operation  currentOperation;
    private final EqualsInfo    equalsInfo;
    private boolean             valid;
    
    public CallStackFrameImpl (
        JPDAThreadImpl      thread,
        StackFrame          sf,
        int                 depth,
        JPDADebuggerImpl    debugger
    ) {
        this.thread = thread;
        this.sf = sf;
        this.depth = depth;
        this.debugger = debugger;
        equalsInfo = new EqualsInfo(debugger, sf, depth);
        this.valid = true; // suppose we're valid when we're new
        try {
            sfLocation = StackFrameWrapper.location(sf);
        } catch (InternalExceptionWrapper ex) {
            // Ignored
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // Unfortunate
        } catch (VMDisconnectedExceptionWrapper ex) {}
    }

    // public interface ........................................................
        
    /**
    * Returns line number of this frame in this callstack.
    *
    * @return Returns line number of this frame in this callstack.
    */
    public synchronized int getLineNumber (String struts) {
        if (!valid && sfLocation == null) return 0;
        try {
            Location l = getStackFrameLocation();
            return LocationWrapper.lineNumber0(l, struts);
        } catch (InvalidStackFrameExceptionWrapper isfex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return 0;
        } catch (InternalExceptionWrapper ex) {
            return 0;
        } catch (VMDisconnectedExceptionWrapper ex) {
            return 0;
        }
    }
    
    public Operation getCurrentOperation(String struts) {
        return currentOperation;
    }
    
    public void setCurrentOperation(Operation operation) {
        this.currentOperation = operation;
    }

    /**
    * Returns method name of this frame in this callstack.
    *
    * @return Returns method name of this frame in this callstack.
    */
    public synchronized String getMethodName () {
        if (!valid && sfLocation == null) return "";
        try {
            Location l = getStackFrameLocation();
            return TypeComponentWrapper.name(LocationWrapper.method(l));
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return "";
        } catch (VMDisconnectedExceptionWrapper ex) {
            return "";
        } catch (InternalExceptionWrapper ex) {
            return "";
        }
    }

    /**
    * Returns class name of this frame in this callstack.
    *
    * @return class name of this frame in this callstack
    */
    public synchronized String getClassName () {
        if (!valid && sfLocation == null) return "";
        assert !java.awt.EventQueue.isDispatchThread();
        try {
            Location l = getStackFrameLocation();
            return ReferenceTypeWrapper.name(LocationWrapper.declaringType(l));
        } catch (InternalExceptionWrapper ex) {
            return "";
        } catch (ObjectCollectedExceptionWrapper ex) {
            return "";
        } catch (VMDisconnectedExceptionWrapper ex) {
            return "";
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return "";
        }
    }

    public synchronized JPDAClassType getClassType() {
        if (!valid && sfLocation == null) return null;
        try {
            Location l = getStackFrameLocation();
            return new JPDAClassTypeImpl(debugger, LocationWrapper.declaringType(l));
        } catch (InternalExceptionWrapper ex) {
            return null;
        } catch (VMDisconnectedExceptionWrapper ex) {
            return null;
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return null;
        }
    }

    /**
    * Returns name of default stratumn.
    *
    * @return name of default stratumn
    */
    public synchronized String getDefaultStratum () {
        if (!valid && sfLocation == null) return "";
        try {
            Location l = getStackFrameLocation();
            return ReferenceTypeWrapper.defaultStratum(LocationWrapper.declaringType(l));
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return "";
        } catch (ObjectCollectedExceptionWrapper ex) {
            return "";
        } catch (VMDisconnectedExceptionWrapper ex) {
            return "";
        } catch (InternalExceptionWrapper ex) {
            return "";
        }
    }

    /**
    * Returns name of default stratumn.
    *
    * @return name of default stratumn
    */
    public synchronized List<String> getAvailableStrata () {
        if (!valid && sfLocation == null) return Collections.emptyList();
        try {
            Location l = getStackFrameLocation();
            return ReferenceTypeWrapper.availableStrata(LocationWrapper.declaringType(l));
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return Collections.emptyList();
        } catch (ObjectCollectedExceptionWrapper ex) {
            return Collections.emptyList();
        } catch (VMDisconnectedExceptionWrapper ex) {
            return Collections.emptyList();
        } catch (InternalExceptionWrapper ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Returns the source debug extension.
     * This is usually the SMAP file.
     *
     * @return source debug extension or <code>null</code>.
     */
    public String getSourceDebugExtension() {
        if (!valid && sfLocation == null) return null;
        try {
            Location l = getStackFrameLocation();
            if (VirtualMachineWrapper.canGetSourceDebugExtension(l.virtualMachine())) {
                return ReferenceTypeWrapper.sourceDebugExtension(LocationWrapper.declaringType(l));
            } else {
                return null;
            }
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return null;
        } catch (ObjectCollectedExceptionWrapper ex) {
            return null;
        } catch (VMDisconnectedExceptionWrapper ex) {
            return null;
        } catch (InternalExceptionWrapper ex) {
            return null;
        } catch (AbsentInformationException ex) {
            return null;
        }
    }

    /**
    * Returns name of file of this frame.
    *
    * @return name of file of this frame
    * @throws NoInformationException if informations about source are not included or some other error
    *   occurres.
    */
    public synchronized String getSourceName (String stratum) throws AbsentInformationException {
        if (!valid && sfLocation == null) return "";
        assert !java.awt.EventQueue.isDispatchThread();
        try {
            Location l = getStackFrameLocation();
            return LocationWrapper.sourceName(l, stratum);
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return "";
        } catch (InternalExceptionWrapper ex) {
            return "";
        } catch (VMDisconnectedExceptionWrapper ex) {
            return "";
        }
    }
    
    /**
     * Returns source path of file this frame is stopped in or null.
     *
     * @return source path of file this frame is stopped in or null
     */
    public synchronized String getSourcePath (String stratum) throws AbsentInformationException {
        if (!valid && sfLocation == null) return "";
        assert !java.awt.EventQueue.isDispatchThread();
        try {
            Location l = getStackFrameLocation();
            return LocationWrapper.sourcePath(l, stratum);
        } catch (InvalidStackFrameExceptionWrapper ex) {
            // this stack frame is not available or information in it is not available
            valid = false;
            return "";
        } catch (InternalExceptionWrapper ex) {
            return "";
        } catch (VMDisconnectedExceptionWrapper ex) {
            return "";
        }
    }
    
    /**
     * Returns local variables.
     *
     * @return local variables
     */
    public org.netbeans.api.debugger.jpda.LocalVariable[] getLocalVariables () 
    throws AbsentInformationException {
        try {
            Location location = getStackFrameLocation();
            String className = ReferenceTypeWrapper.name(LocationWrapper.declaringType(location));
            List l = StackFrameWrapper.visibleVariables (getStackFrame());
            int n = l.size();
            LocalVariable[] locals = new LocalVariable [n];
            for (int i = 0; i < n; i++) {
                com.sun.jdi.LocalVariable lv = (com.sun.jdi.LocalVariable) l.get (i);
                Value v = StackFrameWrapper.getValue(getStackFrame(), lv);
                LocalVariable local = (LocalVariable) debugger.getLocalVariable(lv, v);
                if (local instanceof Local) {
                    Local localImpl = (Local) local;
                    localImpl.setFrame(this);
                    //localImpl.setInnerValue(v);
                    localImpl.setClassName(className);
                } else {
                    ObjectLocalVariable localImpl = (ObjectLocalVariable) local;
                    localImpl.setFrame(this);
                    //localImpl.setInnerValue(v);
                    localImpl.setClassName(className);
                }
                locals[i] = local;
            }
            return locals;
        } catch (NativeMethodExceptionWrapper ex) {
            throw new AbsentInformationException ("native method");
        } catch (InvalidStackFrameExceptionWrapper ex) {
            throw new AbsentInformationException ("thread is running");
        } catch (ObjectCollectedExceptionWrapper ex) {
            throw new AbsentInformationException ("frame class is collected");
        } catch (VMDisconnectedExceptionWrapper ex) {
            return new LocalVariable [0];
        } catch (InternalExceptionWrapper ex) {
            throw new AbsentInformationException (ex.getLocalizedMessage());
        }
    }
    
    /**
     * Returns local variable.
     * @param name The name of the variable
     * @return local variable
     */
    org.netbeans.api.debugger.jpda.LocalVariable getLocalVariable(String name) 
    throws AbsentInformationException {
        try {
            Location l = getStackFrameLocation();
            String className = ReferenceTypeWrapper.name(LocationWrapper.declaringType(l));
            com.sun.jdi.LocalVariable lv;
            try {
                lv = StackFrameWrapper.visibleVariableByName(getStackFrame(), name);
            } catch (NativeMethodExceptionWrapper ex) {
                lv = null;
            }
            if (lv == null) {
                return null;
            }
            Value v = StackFrameWrapper.getValue(getStackFrame(), lv);
            LocalVariable local = (LocalVariable) debugger.getLocalVariable(lv, v);
            if (local instanceof Local) {
                Local localImpl = (Local) local;
                localImpl.setFrame(this);
                localImpl.setInnerValue(v);
                localImpl.setClassName(className);
            } else {
                ObjectLocalVariable localImpl = (ObjectLocalVariable) local;
                localImpl.setFrame(this);
                localImpl.setInnerValue(v);
                localImpl.setClassName(className);
            }
            return local;
        } catch (NativeMethodException ex) {
            throw new AbsentInformationException ("native method");
        } catch (InvalidStackFrameExceptionWrapper ex) {
            throw new AbsentInformationException ("thread is running");
        } catch (ObjectCollectedExceptionWrapper ex) {
            throw new AbsentInformationException ("frame class is collected");
        } catch (VMDisconnectedExceptionWrapper ex) {
            return null;
        } catch (InternalExceptionWrapper ex) {
            throw new AbsentInformationException (ex.getLocalizedMessage());
        }
    }
    
    public LocalVariable[] getMethodArguments() {
        try {
            StackFrame sf = getStackFrame();
            String url = debugger.getEngineContext().getURL(sf,
                                                            getDefaultStratum());
            List<Value> argValues = getArgumentValues(sf);
            if (argValues == null) return null;
            Location l = getStackFrameLocation();
            MethodArgument[] argumentNames = EditorContextBridge.getContext().getArguments(url, LocationWrapper.lineNumber(l));
            int n = argValues.size();
            argumentNames = checkArgumentCount(argumentNames, argValues, n);
            //int n = Math.min(argValues.size(), argumentNames.length);
            LocalVariable[] arguments = new LocalVariable[n];
            for (int i = 0; i < n; i++) {
                com.sun.jdi.Value value = argValues.get(i);
                if (value instanceof ObjectReference) {
                    arguments[i] =
                            new ArgumentObjectVariable(debugger,
                                                 (ObjectReference) value,
                                                 argumentNames[i].getName(),
                                                 argumentNames[i].getType());
                } else {
                    arguments[i] =
                            new ArgumentVariable(debugger,
                                                 (PrimitiveValue) value,
                                                 argumentNames[i].getName(),
                                                 argumentNames[i].getType());
                }
            }
            return arguments;
        } catch (InvalidStackFrameExceptionWrapper e) {
            return new LocalVariable[0];
        } catch (InternalExceptionWrapper e) {
            return new LocalVariable[0];
        } catch (ObjectCollectedExceptionWrapper e) {
            return new LocalVariable[0];
        } catch (VMDisconnectedExceptionWrapper e) {
            return new LocalVariable[0];
        }
    }

    static boolean canFindOperationArguments() {
        return JPDAUtils.IS_JDK_160_02;
    }
    
    List<LocalVariable> findOperationArguments(Operation operation) {
        if (!JPDAUtils.IS_JDK_160_02) return null; // Can evaluate methods after pop since JDK 1.6.0_02
        JPDADebuggerImpl debuggerImpl = (JPDADebuggerImpl) debugger;
        thread.accessLock.writeLock().lock();
        try {
            if (thread.isInStep()) {
                // A next step is submitted, give this up
                return null;
            }
            try {
                ThreadReference tr = thread.getThreadReference();
                com.sun.jdi.VirtualMachine vm = MirrorWrapper.virtualMachine(tr);
                EventRequestManager erm = VirtualMachineWrapper.eventRequestManager(vm);
                List<StepRequest> stepRequests = EventRequestManagerWrapper.stepRequests(erm);
                for (StepRequest sr : stepRequests) {
                    if (tr == sr.thread()) {
                        // There's a step request for this thread, we can not submit another...
                        return null;
                    }
                }

                int frameCount = 0;
                try {
                    frameCount = ThreadReferenceWrapper.frameCount0(tr);
                } catch (IncompatibleThreadStateException itsex) {
                    ErrorManager.getDefault().notify(itsex);
                    return null;
                }

                Logger logger = Logger.getLogger(CallStackFrameImpl.class.getName());

                // Find out if method <classType>.<methodName> is native:
                String classType = operation.getMethodClassType();
                String methodName = operation.getMethodName();
                String methodDescriptor = null;
                try {
                    java.lang.reflect.Field methodDescriptorField = Operation.class.getDeclaredField("methodDescriptor");
                    methodDescriptorField.setAccessible(true);
                    methodDescriptor = (String) methodDescriptorField.get(operation);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("findOperationArguments(): Operation method name = '"+operation.getMethodName()+"', method descriptor = '"+methodDescriptor+"', class type = '"+operation.getMethodClassType()+"'");
                }

                List<ReferenceType> classes = VirtualMachineWrapper.classesByName0(vm, classType);
                for (ReferenceType clazz : classes) {
                    try {
                        List<Method> methods;
                        if (methodDescriptor != null) {
                            methods = ReferenceTypeWrapper.methodsByName(clazz, methodName, methodDescriptor);
                        } else {
                            methods = ReferenceTypeWrapper.methodsByName(clazz, methodName);
                        }
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("  Methods = "+methods);
                        }
                        for (Method method : methods) {
                            if (method.isNative()) {
                                List<LocalVariable> noArgsList = new ArrayList<LocalVariable>(1);
                                noArgsList.add(new ArgumentObjectVariable(debuggerImpl,
                                                             null,
                                                             NbBundle.getMessage(CallStackFrameImpl.class, "MSG_NoArgsForNative"),
                                                             ""));
                                return noArgsList;
                            }
                        }
                    } catch (ClassNotPreparedExceptionWrapper ex) {
                    }
                }

                com.sun.jdi.request.StepRequest step = EventRequestManagerWrapper.createStepRequest(
                        VirtualMachineWrapper.eventRequestManager(vm),
                        tr,
                        com.sun.jdi.request.StepRequest.STEP_MIN,
                        com.sun.jdi.request.StepRequest.STEP_INTO);
                EventRequestWrapper.addCountFilter(step, 1);
                EventRequestWrapper.setSuspendPolicy(step, com.sun.jdi.request.StepRequest.SUSPEND_EVENT_THREAD);
                EventRequestWrapper.enable(step);
                EventRequestWrapper.putProperty(step, Operator.SILENT_EVENT_PROPERTY, Boolean.TRUE);
                final Boolean[] stepDone = new Boolean[] { null };
                debugger.getOperator().register(step, new Executor() {
                    public boolean exec(com.sun.jdi.event.Event event) {
                        synchronized (stepDone) {
                            stepDone[0] = true;
                            stepDone.notify();
                        }
                        return false;
                    }

                    public void removed(EventRequest eventRequest) {
                        synchronized (stepDone) {
                            stepDone[0] = false;
                            stepDone.notify();
                        }
                    }
                });
                ThreadReferenceWrapper.resume(tr);
                synchronized (stepDone) {
                    if (stepDone[0] == null) {
                        try {
                            stepDone.wait();
                        } catch (InterruptedException iex) {}
                    }
                    if (Boolean.FALSE.equals(stepDone[0])) {
                        return null; // Step was canceled
                    }
                }
                StackFrame sf = null;
                List<com.sun.jdi.Value> arguments = null;
                try {
                    int newFrameCount = ThreadReferenceWrapper.frameCount0(tr);
                    if (!(newFrameCount > frameCount)) {
                        // Oops, we're not in the desired method...
                        return null;
                    }
                    sf = ThreadReferenceWrapper.frames(tr, 0, 1).get(0);
                    arguments = getArgumentValues(sf);
                } catch (IncompatibleThreadStateException itsex) {
                    ErrorManager.getDefault().notify(itsex);
                    return null;
                } finally {
                    EventRequestManagerWrapper.deleteEventRequest(
                            VirtualMachineWrapper.eventRequestManager(vm),
                            step);
                    debugger.getOperator().unregister(step);
                    try {
                        if (sf != null) {
                            ThreadReferenceWrapper.popFrames(tr, sf);
                        }
                    } catch (IncompatibleThreadStateException itsex) {
                        ErrorManager.getDefault().notify(itsex);
                        return null;
                    } catch (NativeMethodExceptionWrapper nmex) {
                        return null;
                    } catch (InternalExceptionWrapper iex) {
                        if (iex.getCause().errorCode() == 32) {
                            return null;
                        } else {
                            throw iex;
                        }
                    }
                }
                if (arguments != null) {
                    MethodArgument[] argumentNames;
                    try {
                        Session session = debugger.getSession();
                        argumentNames =
                            EditorContextBridge.getContext().getArguments(
                                debuggerImpl.getEngineContext().getURL(ThreadReferenceWrapper.frames(tr, 0, 1).get(0),
                                                                       session.getCurrentLanguage()),
                                operation);
                    } catch (IncompatibleThreadStateException itsex) {
                        ErrorManager.getDefault().notify(itsex);
                        return null;
                    }
                    int n = arguments.size();
                    argumentNames = checkArgumentCount(argumentNames, arguments, n);
                    List<LocalVariable> argumentList = new ArrayList<LocalVariable>(n);
                    for (int i = 0; i < n; i++) {
                        com.sun.jdi.Value value = arguments.get(i);
                        if (value instanceof ObjectReference) {
                            argumentList.add(
                                    new ArgumentObjectVariable(debuggerImpl,
                                                         (ObjectReference) value,
                                                         argumentNames[i].getName(),
                                                         //argumentNames[i].getType()));
                                                         TypeWrapper.name(ValueWrapper.type(value))));
                        } else {
                            argumentList.add(
                                    new ArgumentVariable(debuggerImpl,
                                                         (PrimitiveValue) value,
                                                         argumentNames[i].getName(),
                                                         //argumentNames[i].getType()));
                                                         (value != null) ? TypeWrapper.name(ValueWrapper.type(value)) : null));
                        }
                    }
                    return argumentList;
                }
            } catch (VMDisconnectedExceptionWrapper e) {
                return null;
            } catch (ObjectCollectedExceptionWrapper e) {
                return null;
            } catch (InternalExceptionWrapper e) {
                return null;
            } catch (IllegalThreadStateExceptionWrapper e) {
                return null;
            } catch (InvalidRequestStateExceptionWrapper irse) {
                Exceptions.printStackTrace(irse);
                return null;
            } catch (InvalidStackFrameExceptionWrapper e) {
                Exceptions.printStackTrace(e);
                return null;
            }
        } finally {
            thread.accessLock.writeLock().unlock();
        }
        return null;
    }
    
    private static MethodArgument[] checkArgumentCount(MethodArgument[] argumentNames, List<Value> argValues, int size) {
        if (argumentNames == null || argumentNames.length != size) {
            argumentNames = new MethodArgument[size];
            for (int i = 0; i < argumentNames.length; i++) {
                Value v = argValues.get(i);
                String type;
                if (v != null) {
                    type = v.type().name();
                } else {
                    type = Object.class.getName();
                }
                argumentNames[i] = new MethodArgument(NbBundle.getMessage(CallStackFrameImpl.class, "CTL_MethodArgument", (i+1)), type, null, null);
            }
        }
        return argumentNames;
    }

    private static List<com.sun.jdi.Value> getArgumentValues(StackFrame sf) {
        try {
            com.sun.jdi.Method m = LocationWrapper.method(StackFrameWrapper.location(sf));
            if (MethodWrapper.isNative(m)) {
                throw new NativeMethodException(TypeComponentWrapper.name(m));
            }
            return StackFrameWrapper.getArgumentValues0(sf);
        } catch (InvalidStackFrameExceptionWrapper e) {
            return java.util.Collections.emptyList();
        } catch (InternalExceptionWrapper e) {
            return java.util.Collections.emptyList();
        } catch (VMDisconnectedExceptionWrapper e) {
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Returns object reference this frame is associated with or null (
     * frame is in static method).
     *
     * @return object reference this frame is associated with or null
     */
    public synchronized This getThisVariable () {
        if (!valid) return null;
        ObjectReference thisR;
        try {
            thisR = StackFrameWrapper.thisObject (getStackFrame());
        } catch (InvalidStackFrameExceptionWrapper ex) {
            valid = false;
            return null;
        } catch (InternalExceptionWrapper e) {
            return null;
        } catch (VMDisconnectedExceptionWrapper e) {
            return null;
        }
        if (thisR == null) return null;
        return new ThisVariable (debugger, thisR, "");
    }
    
    /**
     * Sets this frame current.
     *
     * @see org.netbeans.api.debugger.jpda.JPDADebugger#getCurrentCallStackFrame
     */
    public void makeCurrent () {
        debugger.setCurrentCallStackFrame (this);
    }

    public boolean isCurrent() {
        return this.equals(debugger.getCurrentCallStackFrame());
    }
    
    /**
     * Returns <code>true</code> if the method in this frame is obsoleted.
     *
     * @return <code>true</code> if the method in this frame is obsoleted
     * @throws InvalidStackFrameException when this stack frame becomes invalid
     */
    public synchronized boolean isObsolete () {
        try {
            Location l = getStackFrameLocation();
            return MethodWrapper.isObsolete0(LocationWrapper.method(l));
        } catch (InvalidStackFrameExceptionWrapper ex) {
            throw ex.getCause();
        } catch (InternalExceptionWrapper ex) {
            return false;
        } catch (VMDisconnectedExceptionWrapper ex) {
            return false;
        }
    }
    
    public boolean canPop() {
        if (!debugger.canPopFrames()) return false;
        try {
            ThreadReference t = StackFrameWrapper.thread(getStackFrame());
            if (ThreadReferenceWrapper.frameCount(t) <= 1) { // Nowhere to pop
                return false;
            }
            List topFrames = ThreadReferenceWrapper.frames(t, 0, 2);
            if (MethodWrapper.isNative(LocationWrapper.method(StackFrameWrapper.location((StackFrame) topFrames.get(0)))) ||
                MethodWrapper.isNative(LocationWrapper.method(StackFrameWrapper.location((StackFrame) topFrames.get(1))))) {
                // Have native methods on the stack - can not pop
                return false;
            }
        } catch (IncompatibleThreadStateException itsex) {
            return false;
        } catch (IllegalThreadStateExceptionWrapper itsex) {
            return false;
        } catch (InvalidStackFrameExceptionWrapper isex) {
            return false;
        } catch (InternalExceptionWrapper iex) {
            return false;
        } catch (ObjectCollectedExceptionWrapper ocex) {
            return false;
        } catch (VMDisconnectedExceptionWrapper dex) {
            return false;
        }
        // Looks like we should be able to pop...
        return true;
    }
    
    /**
     * Pop stack frames. All frames up to and including the frame 
     * are popped off the stack. The frame previous to the parameter 
     * frame will become the current frame.
     *
     * @throws InvalidStackFrameException when this stack frame becomes invalid
     */
    public void popFrame () {
        try {
            StackFrame frame = getStackFrame();
            debugger.popFrames(StackFrameWrapper.thread(frame), frame);
        } catch (InternalExceptionWrapper ex) {
            // Ignored
        } catch (VMDisconnectedExceptionWrapper ex) {
            // Ignored
        } catch (InvalidStackFrameExceptionWrapper ex) {
            throw ex.getCause();
        }
    }
    
    /**
     * Returns thread.
     *
     * @return thread
     * @throws InvalidStackFrameException when this stack frame becomes invalid
     */
    public JPDAThread getThread () {
        return thread;//debugger.getThread (sf.thread());
    }

    
    // other methods............................................................

    private synchronized Location getStackFrameLocation() throws InternalExceptionWrapper, VMDisconnectedExceptionWrapper, InvalidStackFrameExceptionWrapper {
        if (sfLocation == null) sfLocation = StackFrameWrapper.location(getStackFrame());
        return sfLocation;
    }

    /**
     * Get the JDI stack frame.
     * @throws InvalidStackFrameExceptionWrapper when the associated thread is not suspended.
     */
    public StackFrame getStackFrame () throws InternalExceptionWrapper, VMDisconnectedExceptionWrapper, InvalidStackFrameExceptionWrapper {
        try {
            // Just a validity test
            StackFrameWrapper.thread(sf);
        } catch (InvalidStackFrameExceptionWrapper isfex) {
            // We're invalid! Try to retrieve the new stack frame.
            // We could be invalidated due to http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6700889
            try {
                ThreadReference ref = thread.getThreadReference();
                if (depth >= ThreadReferenceWrapper.frameCount(ref)) {
                    // The execution has moved elsewhere.
                    throw isfex;
                }
                sf = ThreadReferenceWrapper.frame(ref, depth);
                sfLocation = StackFrameWrapper.location(getStackFrame());
            } catch (ObjectCollectedExceptionWrapper ex) {
                throw isfex;
            } catch (IncompatibleThreadStateException ex) {
                // This was not successful. Throw the original exception.
                throw isfex;
            } catch (IllegalThreadStateExceptionWrapper ex) {
                // This was not successful. Throw the original exception.
                throw isfex;
            }
            if (!equalsInfo.equals(new EqualsInfo(debugger, sf, depth))) {
                // The execution has moved elsewhere.
                throw isfex;
            }
        }
        return sf;
    }
    
    /**
     * Get the depth of this stack frame in the thread stack.
     */
    public int getFrameDepth() {
        return depth;
    }

    public boolean equals (Object o) {
        if (!(o instanceof CallStackFrameImpl)) {
            return false;
        }
        CallStackFrameImpl frame = (CallStackFrameImpl) o;
        return equalsInfo.equals(frame.equalsInfo);
    }
    
    public int hashCode () {
        return equalsInfo.hashCode();
    }

    @Override
    public String toString() {
        return "CallStackFrameImpl["+thread+", depth = "+depth+"]";
    }

    public List<MonitorInfo> getOwnedMonitors() {
        List<MonitorInfo> threadMonitors;
        try {
            threadMonitors = getThread().getOwnedMonitorsAndFrames();
        } catch (InvalidStackFrameException itsex) {
            threadMonitors = Collections.emptyList();
        } catch (VMDisconnectedException e) {
            threadMonitors = Collections.emptyList();
        }
        if (threadMonitors.size() == 0) {
            return threadMonitors;
        }
        List<MonitorInfo> frameMonitors = new ArrayList<MonitorInfo>();
        for (MonitorInfo mi : threadMonitors) {
            if (this.equals(mi.getFrame())) {
                frameMonitors.add(mi);
            }
        }
        return Collections.unmodifiableList(frameMonitors);
    }
    
    private final static class EqualsInfo {
        
        private JPDAThread thread;
        private int depth;
        private Location location;
        
        public EqualsInfo(JPDADebuggerImpl debugger, StackFrame sf, int depth) {
            try {
                thread = debugger.getThread(StackFrameWrapper.thread(sf));
                this.depth = depth;
                this.location = StackFrameWrapper.location(sf);
            } catch (VMDisconnectedExceptionWrapper e) {
                thread = null;
            } catch (InternalExceptionWrapper e) {
                thread = null;
            } catch (InvalidStackFrameExceptionWrapper e) {
                thread = null;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EqualsInfo)) {
                return false;
            }
            EqualsInfo ei = (EqualsInfo) obj;
            try {
                return thread == ei.thread &&
                       depth == ei.depth &&
                       (location == ei.location ||
                        location != null && location.equals(ei.location));
            } catch (VMDisconnectedException vmdex) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            if (thread == null) return 0;
            try {
                return (thread.hashCode() << 8 + depth + (location != null ? location.hashCode() << 4 : 0));
            } catch (VMDisconnectedException vmdex) {
                return 0;
            }
        }
        
    }
}
