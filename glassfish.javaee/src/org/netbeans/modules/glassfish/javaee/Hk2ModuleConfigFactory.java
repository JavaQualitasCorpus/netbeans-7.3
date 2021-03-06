/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.glassfish.javaee;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.j2ee.deployment.common.api.ConfigurationException;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfiguration;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfigurationFactory2;

/**
 *
 * @author vbk
 */
public class Hk2ModuleConfigFactory implements ModuleConfigurationFactory2 {
    
    /** Creates a new instance of Hk2ModuleConfigFactory */
    public Hk2ModuleConfigFactory() {
    }
    
    @Override
    public ModuleConfiguration create(J2eeModule module) throws ConfigurationException {
        ModuleConfiguration retVal = null;
        try {
            retVal = new ModuleConfigurationImpl(module, new Hk2Configuration(module), null);
        } catch (ConfigurationException ce) {
            throw ce;
        } catch (Exception ex) {
            throw new ConfigurationException(module.toString(), ex);
        }
        return retVal;
    }

    @Override
    public ModuleConfiguration create(@NonNull J2eeModule module, @NonNull String instanceUrl) throws ConfigurationException {
        ModuleConfiguration retVal = null;
        try {
            Hk2DeploymentManager hk2Dm =
                    (Hk2DeploymentManager) Hk2DeploymentFactory.createEe6().getDisconnectedDeploymentManager(instanceUrl);
            if (instanceUrl.contains("gfv3ee6wc")) { // NOI18N
                retVal = new ModuleConfigurationImpl(module, new Three1Configuration(module), hk2Dm);
            } else {
                retVal = new ModuleConfigurationImpl(module, new Hk2Configuration(module), hk2Dm);
            }
        } catch (ConfigurationException ce) {
            throw ce;
        } catch (Exception ex) {
            throw new ConfigurationException(module.toString(), ex);
        }
        return retVal;
    }
    
}
