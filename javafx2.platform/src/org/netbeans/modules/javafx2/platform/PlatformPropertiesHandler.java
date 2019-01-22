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
package org.netbeans.modules.javafx2.platform;

import java.util.Iterator;
import java.util.Map;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.javafx2.platform.api.JavaFXPlatformUtils;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.ErrorManager;
import org.openide.util.Mutex;
import org.openide.util.MutexException;

/**
 * Utility class for platform properties manipulation
 * 
 * @author Anton Chechel
 */
public final class PlatformPropertiesHandler {

    private PlatformPropertiesHandler() {
    }

    /**
     * Load global properties defined by the IDE in the user directory.
     * Currently loads ${netbeans.user}/build.properties if it exists.
     * <p>
     * Acquires read access.
     * <p>
     * @return user properties (empty if missing or malformed)
     */
    @NonNull
    public static EditableProperties getGlobalProperties() {
        return PropertyUtils.getGlobalProperties();
    }
    
    /**
     * Saves given properties to IDE global properties file located in userdir.
     * <p>
     * Acquires write access.
     * <p>
     * @param map of properties
     */
    public static void saveGlobalProperties(@NonNull final Map<String, String> propMap) {
        try {
            ProjectManager.mutex().writeAccess(
                    new Mutex.ExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            final EditableProperties props = PropertyUtils.getGlobalProperties();
                            for (Map.Entry<String, String> entry : propMap.entrySet()) {
                                props.setProperty(entry.getKey(), entry.getValue());
                            }
                            PropertyUtils.putGlobalProperties(props);
                            return null;
                        }
                    });
        } catch (MutexException me) {
            ErrorManager.getDefault().notify(me.getException());
        }
    }

    /**
     * Removes all properties for given platform from IDE global properties file located in userdir.
     * <p>
     * Acquires write access.
     * <p>
     * @param java platform instance
     */
    public static void clearGlobalPropertiesForPlatform(@NonNull final JavaPlatform platform) {
        try {
            ProjectManager.mutex().writeAccess(
                    new Mutex.ExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            String platformName = platform.getProperties().get(JavaFXPlatformUtils.PLATFORM_ANT_NAME);
                            String propPrefix = "platforms." + platformName + ".javafx."; // NOI18N
                            boolean changed = false;
                            
                            final EditableProperties props = PropertyUtils.getGlobalProperties();
                            for (Iterator<String> it = props.keySet().iterator(); it.hasNext();) {
                                String key = it.next();
                                if (key.startsWith(propPrefix)) {
                                    it.remove();
                                    changed = true;
                                }
                            }
                            
                            if (changed) {
                                PropertyUtils.putGlobalProperties(props);
                            }
                            return null;
                        }
                    });
        } catch (MutexException me) {
            ErrorManager.getDefault().notify(me.getException());
        }
    }

}