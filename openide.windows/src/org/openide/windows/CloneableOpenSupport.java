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
package org.openide.windows;

import org.openide.awt.StatusDisplayer;
import org.openide.util.*;

import java.beans.*;


/** Simple support for an openable objects.
* Can be used either as an {@link org.openide.cookies.OpenCookie},
* {@link org.openide.cookies.ViewCookie}, or {@link org.openide.cookies.CloseCookie},
* depending on which cookies the subclass implements.
*
* @author Jaroslav Tulach
*/
public abstract class CloneableOpenSupport extends Object {
    private static java.awt.Container container;

    /** the environment that provides connection to outside world */
    protected Env env;

    /** All opened editors on this file.
     * <em>Warning:</em> Treat this field like <code>final</code>.
     * Internally the instance is used as <code>WeakListener</code>
     * on <code>Env</code> validity changes.
     * Changing the instance in subclasses would lead to breaking
     * of that listening, thus to errorneous behaviour. */
    protected CloneableTopComponent.Ref allEditors;

    /** New support for a given environment.
    * @param env environment to take all date from/to
    */
    public CloneableOpenSupport(Env env) {
        this.env = env;

        Listener l = new Listener(env, this);
        this.allEditors = l;

        // attach property change listener to be informed about loosing validity
        env.addPropertyChangeListener(org.openide.util.WeakListeners.propertyChange(l, env));

        // attach vetoable change listener to be cancel loosing validity when modified
        env.addVetoableChangeListener(org.openide.util.WeakListeners.vetoableChange(l, env));
    }

    /** Opens and focuses or just focuses already opened
     * <code>CloneableTopComponent</code>.
     * <p><b>Note: The actual processing of this method is scheduled into AWT thread
     * in case it is called from other than the AWT thread.</b></p>
     * @see org.openide.cookies.OpenCookie#open
     * @see #openCloneableTopComponent
     */
    public void open() {
        //Bugfix #10688 open() is now run in AWT thread
        Mutex.EVENT.writeAccess(
            new Runnable() {
                public void run() {
                    CloneableTopComponent editor = openCloneableTopComponent();
                    editor.requestActive();
                }
            }
        );
    }

    /** Focuses existing component to view, or if none exists creates new.
    * The default implementation simply calls {@link #open}.
    * @see org.openide.cookies.ViewCookie#view
    */
    public void view() {
        open();
    }

    /** Focuses existing component to view, or if none exists creates new.
    * The default implementation simply calls {@link #open}.
    * @see org.openide.cookies.EditCookie#edit
    */
    public void edit() {
        open();
    }

    /** Closes all components.
    * @return <code>true</code> if every component is successfully closed or <code>false</code> if the user cancelled the request
    * @see org.openide.cookies.CloseCookie#close
    */
    public boolean close() {
        return close(true);
    }

    /** Closes all opened windows.
    * @param ask true if we should ask user
    * @return true if sucesfully closed
    */
    protected boolean close(final boolean ask) {
        if (allEditors.isEmpty()) {
            return true;
        }

        //Bugfix #10688 close() is now run in AWT thread
        //also bugfix of 10714 - whole close (boolean) is run in AWT thread
        Boolean ret = Mutex.EVENT.writeAccess(
                new Mutex.Action<Boolean>() {
                    public Boolean run() {
                        //synchronized (allEditors) {
                        synchronized (getLock()) {
                            // user canceled the action
                            if (ask && !canClose()) {
                                return Boolean.FALSE;
                            }

                            java.util.Enumeration en = allEditors.getComponents();

                            while (en.hasMoreElements()) {
                                TopComponent c = (TopComponent) en.nextElement();

                                if (!c.close()) {
                                    return Boolean.FALSE;
                                }
                            }
                        }

                        return Boolean.TRUE;
                    }
                }
            );

        return ret.booleanValue();
    }

    /** Should test whether all data is saved, and if not, prompt the user
    * to save.
    * The default implementation returns <code>true</code>.
    *
    * @return <code>true</code> if everything can be closed
    */
    protected boolean canClose() {
        return true;
    }

    /** Simply open for an editor. */
    protected final CloneableTopComponent openCloneableTopComponent() {
        //synchronized (allEditors) {
        synchronized (getLock()) {
            CloneableTopComponent ret = allEditors.getArbitraryComponent();

            if (ret != null) {
                ret.open();

                return ret;
            } else {
                // no opened editor
                String msg = messageOpening();

                if (msg != null) {
                    StatusDisplayer.getDefault().setStatusText(msg);
                }

                CloneableTopComponent editor = createCloneableTopComponent();
                editor.setReference(allEditors);
                editor.open();

                msg = messageOpened();

                if (msg == null) {
                    msg = ""; // NOI18N
                }

                StatusDisplayer.getDefault().setStatusText(msg);

                return editor;
            }
        }
    }

    /** Creates lock object used in close and openCloneableTopComponent. */
    private Object getLock() {
        if (container == null) {
            container = new java.awt.Container();
        }

        return container.getTreeLock();
    }

    /** A method to create a new component. Must be overridden in subclasses.
    * @return the cloneable top component for this support
    */
    protected abstract CloneableTopComponent createCloneableTopComponent();

    /** Message to display when an object is being opened.
    * @return the message or null if nothing should be displayed
    */
    protected abstract String messageOpening();

    /** Message to display when an object has been opened.
    * @return the message or null if nothing should be displayed
    */
    protected abstract String messageOpened();

    /** Abstract interface that is used by CloneableOpenSupport to
    * talk to outside world.
    */
    public static interface Env extends java.io.Serializable {
        /** that is fired when the objects wants to mark itself as
        * invalid, so all components should be closed.
        */
        public static final String PROP_VALID = "valid"; // NOI18N

        /** that is fired when the objects wants to mark itself modified
        * or not modified.
        */
        public static final String PROP_MODIFIED = "modified"; // NOI18N

        /** Adds property listener.
        */
        public void addPropertyChangeListener(PropertyChangeListener l);

        /** Removes property listener.
        */
        public void removePropertyChangeListener(PropertyChangeListener l);

        /** Adds veto listener.
        */
        public void addVetoableChangeListener(VetoableChangeListener l);

        /** Removes veto listener.
        */
        public void removeVetoableChangeListener(VetoableChangeListener l);

        /** Test whether the support is in valid state or not.
        * It could be invalid after deserialization when the object it
        * referenced to does not exist anymore.
        *
        * @return true or false depending on its state
        */
        public boolean isValid();

        /** Test whether the object is modified or not.
        * @return true if the object is modified
        */
        public boolean isModified();

        /** Support for marking the environement modified.
        * @exception IOException if the environment cannot be marked modified
        *    (for example when the file is readonly), when such exception
        *    is the support should discard all previous changes
        */
        public void markModified() throws java.io.IOException;

        /** Reverse method that can be called to make the environment
        * unmodified.
        */
        public void unmarkModified();

        /** Method that allows environment to find its
        * cloneable open support.
        */
        public CloneableOpenSupport findCloneableOpenSupport();
    }

    /** Property change & veto listener. To react to dispose/delete of
    * the data object.
    */
    private static final class Listener extends CloneableTopComponent.Ref implements PropertyChangeListener,
        VetoableChangeListener, Runnable {
        /** generated Serialized Version UID */
        static final long serialVersionUID = -1934890789745432531L;

        /** environment to use as connection to outside world */
        private final Env env;
        // rerefence to prevent GC of COS created in readResolve() by call to support()
        private final transient CloneableOpenSupport refCOS;

        /** Constructor.
        */
        public Listener(Env env, CloneableOpenSupport cos) {
            this.env = env;
            this.refCOS = cos;
        }

        /** Getter for the associated CloneableOpenSupport
        * @return the support or null if none was found
        */
        private CloneableOpenSupport support() {
            return env.findCloneableOpenSupport();
        }

        public void propertyChange(PropertyChangeEvent ev) {
            if (Env.PROP_VALID.equals(ev.getPropertyName())) {
                // do not check it if old value is not true
                if (Boolean.FALSE.equals(ev.getOldValue())) {
                    return;
                }

                Mutex.EVENT.readAccess(this);
            }
        }

        /** Closes the support in AWT thread.
         */
        public void run() {
            // loosing validity
            CloneableOpenSupport os = support();

            if (os != null) {
                // mark the object as not being modified, so nobody
                // will ask for save
                env.unmarkModified();

                os.close(false);
            }
        }

        /** Forbids setValid (false) on data object when there is an
        * opened editor.
        *
        * @param ev PropertyChangeEvent
        */
        public void vetoableChange(PropertyChangeEvent ev)
        throws PropertyVetoException {
            if (Env.PROP_VALID.equals(ev.getPropertyName())) {
                // do not check it if old value is not true
                if (Boolean.FALSE.equals(ev.getOldValue())) {
                    return;
                }

                if (env.isModified()) {
                    // if the object is modified 
                    CloneableOpenSupport os = support();

                    if ((os != null) && !os.canClose()) {
                        // is modified and has not been sucessfully closed
                        throw new PropertyVetoException(
                        // [PENDING] this is not a very good detail message!
                        "", ev // NOI18N
                        );
                    }
                }
            }
        }

        /** Resolvable to connect to the right data object. This
        * method is used for connectiong CloneableTopComponents via
        * their CloneableTopComponent.Ref
        */
        public Object readResolve() {
            CloneableOpenSupport os = support();

            if (os == null) {
                // problem! no replace!?
                return this;
            }

            // use the editor support's CloneableTopComponent.Ref
            return os.allEditors;
        }
    }
}
