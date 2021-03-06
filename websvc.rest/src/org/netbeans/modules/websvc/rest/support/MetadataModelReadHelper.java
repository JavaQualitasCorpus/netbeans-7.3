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

package org.netbeans.modules.websvc.rest.support;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.j2ee.metadata.model.api.MetadataModel;
import org.netbeans.modules.j2ee.metadata.model.api.MetadataModelAction;
import org.openide.util.ChangeSupport;
import org.openide.util.Parameters;
import org.openide.util.RequestProcessor;

/**
 * An utility class for working with metadata models in UI. Since model operations
 * might take a long time, the model access should not take place in
 * the event dispatching thread. Also, there should be status messages in the UI
 * informing the user about the current state ("Classpath scanning in progress",
 * "Reading entity classes", etc.). Writing code that does this
 * is a difficult task and the code tends to be prone to errors. This class attempts to
 * simplify this task.
 *
 * <p>This class can be used to execute an action while ensuring that it
 * is executed outside the AWT thread and that all states (classpath scanning,
 * reading the model) are handled. The state is encapsulated by the {@link #State}
 * enumeration, which can be one of:</p>
 *
 * <ul>
 * <li><code>IDLE</code> before the {@link #start} method has been called.</li>
 * <li><code>WAITING_READY</code> waiting for the model to become {@link MetadataModel#isReady ready}.
 * This e.g corresponds to the classpath scan being in progress for a metadata model
 * based on Java annotations.</li>
 * <li><code>READING_MODEL</code> reading the model.</li>
 * <li><code>FINISHED</code> the action has finished executing. The result of the
 * action can be {@link #getResult retrieved}.
 * </ul>
 *
 * <p>For example, this is how the class would be used to display the entity
 * classes in a wizard:</p>
 *
 * <pre>
 * // the model to read from
 * private MetadataModel<EntityMappingsMetadata> model;
 * // the read helper
 * private MetadataModelReadHelper<EntityMappingsMetadata, List<String>> readHelper;
 * // the result of the model action (what will be read from the model)
 * private List<String> entityNames;
 *
 * // ...
 *
 * public void readSettings(Object settings) {
 *     wizardDescriptor = (WizardDescriptor)settings;
 *     // ensure the read only takes place the first time readSettings() is called
 *     if (model == null) {
 *         // get the model (from a wizard property or in whatever way)
 *         model = (MetadataModel<EntityMappingsMetadata>)wizardDescriptor.getProperty("model"); // NOI18N
 *         // pass a MetadataModelAction to the read helper -- the same action
 *         // that would be passed directly to MetadataModel.runReadActionWhenReady()
 *         readHelper = MetadataModelReadHelper.create(model, new MetadataModelAction<EntityMappingsMetadata, List<String>>() {
 *             public List<String> run(EntityMappingsMetadata metadata) throws Exception {
 *                 List<String> result = new ArrayList<String>();
 *                 for (Entity entity : metadata.getRoot().getEntity()) {
 *                     result.add(entity.getName());
 *                 }
 *                 return result;
 *             }
 *         });
 *         // need to listen on the read helper for state changes
 *         // and forward them as wizard state changes, which will cause
 *         // the isValid() method to be called
 *         readHelper.addChangeListener(new ChangeListener() {
 *             public void stateChanged(ChangeEvent e) {
 *                 fireChange();
 *             }
 *         });
 *         // everything is set up, so start the read helper
 *         // the helper now starts executing the action in another thread
 *         readHelper.start();
 *     }
 * }
 *
 * // ...
 *
 * public boolean isValid() {
 *     // test the read helper state
 *     switch (readHelper.getState()) {
 *         case WAITING_READY:
 *                setErrorMessage("Scanning classpath.");
 *                return false;
 *
 *            case READING_MODEL:
 *                setErrorMessage("Searching entity classes.");
 *                return false;
 *
 *            case FINISHED:
 *                // when we got here the model action has been executed
 *                if (entityNames == null) {
 *                    try {
 *                        entityNames = readHelper.getResult();
 *                        // pass the entity names to the wizard's visual panel
 *                        getComponent().setEntityNames(entityNames);
 *                    } catch (ExecutionException e) {
 *                        Exceptions.printStackTrace(e);
 *                    }
 *                }
 *     }
 *     if (entityNames != null) {
 *        if (entityNames.size() == 0) {
 *            setErrorMessage("No entities");
 *            return false;
 *        }
 *     }
 *     setErrorMessage(" ");
 *     return true;
 * }
 * </pre>
 *
 * @since 1.17
 * @author Andrei Badea
 */
public class MetadataModelReadHelper<T, R> {

    public enum State { IDLE, WAITING_READY, READING_MODEL, FINISHED };

    // needs to have a throughput of 1 to ensure events are queued
    private final RequestProcessor eventRP = new RequestProcessor("MetadataModelReadHelper", 1); // NOI18N
    private final ChangeSupport changeSupport = new ChangeSupport(this);

    private final MetadataModel<T> model;
    private final MetadataModelAction<T, R> action;

    private Reader reader;
    private volatile State state = State.IDLE;
    private volatile R result;
    private volatile ExecutionException executionException;

    /**
     * Creates a new instance of <code>MetadataModelReadHelper</code>.
     *
     * @param  model a metadata model; cannot be null.
     * @param  action an action to be executed by <code>model</code>.
     */
    public static <T, R> MetadataModelReadHelper<T, R> create(MetadataModel<T> model, MetadataModelAction<T, R> action) {
        Parameters.notNull("model", model); // NOI18N
        Parameters.notNull("action", action); // NOI18N
        return new MetadataModelReadHelper<T, R>(model, action);
    }

    private MetadataModelReadHelper(MetadataModel<T> model, MetadataModelAction<T, R> action) {
        this.model = model;
        this.action = action;
    }

    /**
     * Adds a change listener. Whenever the state changes a change event is
     * fired to the registered listeners.
     */
    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    /**
     * Removes a change listener.
     */
    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    /**
     * Starts the helper. This method may only be called once.
     */
    public void start() {
        synchronized (this) {
            if (reader != null) {
                throw new IllegalStateException("The action has already been started"); // NOI18N
            }
            reader = new Reader();
        }
        // ensure that the client sees the WAITING_READY state
        // even if the eventRP task is queued
        state = State.WAITING_READY;
        changeSupport.fireChange();
        // ensure the model is not accessed in the calling thread in case
        // the calling thread is the AWT thread
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                reader.run();
            }
        });
    }

    /**
     * Returns the state of the helper; never null.
     */
    public State getState() {
        return state;
    }

    /**
     * Returns the result of the action. This method may only be called
     * when the state is {@link State#FINISHED}.
     */
    public R getResult() throws ExecutionException {
        if (state != State.FINISHED) {
            throw new IllegalStateException("The action has not finished yet"); // NOI18N
        }
        if (executionException != null) {
            throw executionException;
        }
        return result;
    }
    
    public MetadataModel<T> getModel(){
        return model;
    }

    /**
     * Fires change events asynchronously to avoid calling listeners under any
     * locks that the model may take while executing actions (e.g., the JavaSource
     * javac lock).
     */
    private void fireChange() {
        eventRP.post(new Runnable() {
            public void run() {
                changeSupport.fireChange();
            }
        });
    }

    private final class Reader {

        public void run() {
            try {
                Future<Void> future = model.runReadActionWhenReady(new MetadataModelAction<T, Void>() {
                    public Void run(T metadata) throws Exception {
                        state = State.READING_MODEL;
                        fireChange();
                        result = action.run(metadata);
                        state = State.FINISHED;
                        fireChange();
                        return null;
                    }
                });
                // get any exceptions thrown if the action was run asynchronously
                future.get();
            } catch (Exception e) {
                state = State.FINISHED;
                executionException = new ExecutionException(e.getMessage(), e);
                fireChange();
            }
        }
    }

    public boolean isReady() {
        return getState() == State.FINISHED && model.isReady();
    }
}
