/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.maven.configurations;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.codehaus.plexus.util.StringUtils;
import org.netbeans.modules.maven.NbMavenProjectImpl;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.netbeans.modules.maven.api.ProjectProfileHandler;
import org.netbeans.modules.maven.api.customizer.ModelHandle2;
import static org.netbeans.modules.maven.configurations.ConfigurationPersistenceUtils.*;
import org.netbeans.modules.maven.customizer.CustomizerProviderImpl;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
/**
 * WARNING: this class shall in no way use project.getLookup() as it's called
 * in the critical loop (getOriginalMavenproject
 * @author mkleint
 */
public class M2ConfigProvider implements ProjectConfigurationProvider<M2Configuration> {

    private PropertyChangeSupport support = new PropertyChangeSupport(this);
    private NbMavenProjectImpl project;
    private final M2Configuration DEFAULT;
    // next four guarded by this
    private SortedSet<M2Configuration> profiles = null;
    private SortedSet<M2Configuration> shared = null;
    private SortedSet<M2Configuration> nonshared = null;
    private M2Configuration active;
    private String initialActive;
    private AuxiliaryConfiguration aux;
    private ProjectProfileHandler profileHandler;
    private PropertyChangeListener propertyChange;


    private static final RequestProcessor RP = new RequestProcessor(M2ConfigProvider.class.getName(),10);
    
    public M2ConfigProvider(NbMavenProjectImpl proj, AuxiliaryConfiguration aux, ProjectProfileHandler prof) {
        project = proj;
        this.aux = aux;
        profileHandler = prof;
        DEFAULT = M2Configuration.createDefault(project.getProjectDirectory());
        //read the active one..
        initialActive = readActiveConfigurationName(aux);
        
        active = DEFAULT;
        propertyChange = new PropertyChangeListener() {
            public @Override void propertyChange(PropertyChangeEvent evt) {
                if (NbMavenProjectImpl.PROP_PROJECT.equals(evt.getPropertyName())) {
                    synchronized (M2ConfigProvider.this) {
                        profiles = null;
                    }
                    RP.post(new Runnable() {
                        public @Override void run() {
                            checkActiveAgainstAll(getConfigurations(), false);
                            firePropertyChange();
                        }

                    });
                }
            }
        };
    }

    private void checkActiveAgainstAll(Collection<M2Configuration> confs, boolean async) {
        assert !Thread.holdsLock(this);
        boolean found = false;
        String id;
        synchronized (this) {
            id = active.getId();
        }
        for (M2Configuration conf : confs) {
            if (conf.getId().equals(id)) {
                found = true;
                break;
            }
        }
        if (!found) {
            Runnable dothis = new Runnable() {
                    public @Override void run() {
                        M2Configuration _active;
                        synchronized (M2ConfigProvider.this) {
                            _active = active;
                        }
                        try {
                            doSetActiveConfiguration(DEFAULT, _active);
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                };
            if (async) {
                RP.post(dothis);
            } else {
                dothis.run();
            }
        }
    }
    
    private synchronized Collection<M2Configuration> getConfigurations(boolean skipProfiles) {
        if (profiles == null && !skipProfiles) {
            profiles = createProfilesList();
        }
        if (shared == null) {
            //read from auxconf
            shared = readConfigurations(aux, project.getProjectDirectory(), true);
        }
        if (nonshared == null) {
            //read from auxconf
            nonshared = readConfigurations(aux, project.getProjectDirectory(), false);
        }
        Collection<M2Configuration> toRet = new TreeSet<M2Configuration>();
        toRet.add(DEFAULT);
        toRet.addAll(shared);
        toRet.addAll(nonshared);
        if (!skipProfiles) {
            //prevent duplicates in the list
            Iterator<M2Configuration> it = profiles.iterator();
            while (it.hasNext()) {
                M2Configuration c = it.next();
                if (!toRet.contains(c)) {
                    toRet.add(c);
                } else {
                    it.remove();
                }
            }
        }
        return toRet;
        
    }
    
    public @Override synchronized Collection<M2Configuration> getConfigurations() {
        return getConfigurations(false);
    }

    public M2Configuration getDefaultConfig() {
        return DEFAULT;
    }
    
    public synchronized Collection<M2Configuration> getProfileConfigurations() {
        getConfigurations();
        return profiles;
    }
    
    public synchronized Collection<M2Configuration> getSharedConfigurations() {
        getConfigurations();
        return shared;
    }
    
    public synchronized Collection<M2Configuration> getNonSharedConfigurations() {
        getConfigurations();
        return nonshared;
    }
    
    public @Override boolean hasCustomizer() {
        return true;
    }

    public @Override void customize() {
        CustomizerProviderImpl prv = project.getLookup().lookup(CustomizerProviderImpl.class);
        prv.showCustomizer(ModelHandle2.PANEL_CONFIGURATION);
    }

    public @Override boolean configurationsAffectAction(String action) {
        if (ActionProvider.COMMAND_DELETE.equals(action) || ActionProvider.COMMAND_COPY.equals(action) || ActionProvider.COMMAND_MOVE.equals(action)) {
            return false;
        }
        return true;
    }


    public @Override M2Configuration getActiveConfiguration() {
        M2Configuration _active;
        Collection<M2Configuration> confs;
        synchronized (this) {
            confs = getConfigurations(false);
            if (initialActive != null) {
                for (M2Configuration conf : confs) {
                    if (initialActive.equals(conf.getId())) {
                        active = conf;
                        initialActive = null;
                        break;
                    }
                }
                if (initialActive != null) {
                    RP.post(new Runnable() {
                        public @Override void run() {
                            try {
                                doSetActiveConfiguration(DEFAULT, null);
                            } catch (Exception ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    });
                    initialActive = null;
                }
            }
            _active = active;
        }
        checkActiveAgainstAll(confs, true);
        return _active;
    }
    
    public void setConfigurations(List<M2Configuration> shared, List<M2Configuration> nonshared, boolean includeProfiles) {
        writeAuxiliaryData(aux, true, shared);
        writeAuxiliaryData(aux, false, nonshared);
        synchronized (this) {
            this.shared = new TreeSet<M2Configuration>(shared);
            this.nonshared = new TreeSet<M2Configuration>(nonshared);
            //#174637
            if (active != null) {
                if (shared.contains(active)) {
                    M2Configuration newActive = shared.get(shared.indexOf(active));
                    //can have different content
                    active = newActive;
                }
                if (nonshared.contains(active)) {
                    M2Configuration newActive = nonshared.get(nonshared.indexOf(active));
                    //can have different content
                    active = newActive;
                }
            }
            this.profiles = null;
        }
        firePropertyChange();
    }

    public @Override void setActiveConfiguration(M2Configuration configuration) throws IllegalArgumentException, IOException {
        M2Configuration _active;
        synchronized (this) {
            if (active == configuration || (active != null && active.equals(configuration))) {
                return;
            }
            _active = active;
        }
        doSetActiveConfiguration(configuration, _active);
        NbMavenProject.fireMavenProjectReload(project);
    }

    private void doSetActiveConfiguration(M2Configuration newone, M2Configuration old) throws IllegalArgumentException, IOException {
        M2Configuration _active;
        synchronized (this) {
            active = newone;
            writeAuxiliaryData(
                    aux,
                    ACTIVATED, active.getId());
            _active = active;
        }
        assert !Thread.holdsLock(this);
        support.firePropertyChange(PROP_CONFIGURATION_ACTIVE, old, _active);
    }

    private SortedSet<M2Configuration> createProfilesList() {
        List<String> profs = profileHandler.getAllProfiles();
        SortedSet<M2Configuration> config = new TreeSet<M2Configuration>();
//        config.add(DEFAULT);
        for (String prof : profs) {
            M2Configuration c = new M2Configuration(prof, project.getProjectDirectory());
            c.setActivatedProfiles(Collections.singletonList(prof));
            config.add(c);
        }
        return config;
    }

    public @Override synchronized void addPropertyChangeListener(PropertyChangeListener lst) {
        if (support.getPropertyChangeListeners().length == 0) {
            project.getProjectWatcher().addPropertyChangeListener(propertyChange);
        }
        support.addPropertyChangeListener(lst);

    }

    public @Override synchronized void removePropertyChangeListener(PropertyChangeListener lst) {
        support.removePropertyChangeListener(lst);
        if (support.getPropertyChangeListeners().length == 0) {
            project.getProjectWatcher().removePropertyChangeListener(propertyChange);
        }
    }


    private void firePropertyChange() {
        support.firePropertyChange(ProjectConfigurationProvider.PROP_CONFIGURATIONS, null, null);
    }
    
    

    public static void writeAuxiliaryData(AuxiliaryConfiguration conf, String property, String value) {
        Element el = conf.getConfigurationFragment(ROOT, NAMESPACE, false);
        if (el == null) {
            el = XMLUtil.createDocument(ROOT, NAMESPACE, null, null).getDocumentElement();
        }
        Element enEl;
        NodeList list = el.getElementsByTagNameNS(NAMESPACE, property);
        if (list.getLength() > 0) {
            enEl = (Element)list.item(0);
        } else {
            enEl = el.getOwnerDocument().createElementNS(NAMESPACE, property);
            el.appendChild(enEl);
        }
        enEl.setTextContent(value);
        conf.putConfigurationFragment(el, false);
    }

    private static void writeAuxiliaryData(AuxiliaryConfiguration conf, boolean shared, List<M2Configuration> configs) {
        Element el = conf.getConfigurationFragment(ROOT, NAMESPACE, shared);
        if (el == null) {
            el = XMLUtil.createDocument(ROOT, NAMESPACE, null, null).getDocumentElement();
        }
        Element enEl;
        NodeList list = el.getElementsByTagNameNS(NAMESPACE, CONFIGURATIONS);
        if (list.getLength() > 0) {
            enEl = (Element)list.item(0);
            NodeList nl = enEl.getChildNodes();
            int len = nl.getLength();
            for (int i = 0; i < len; i++) {
                enEl.removeChild(nl.item(0));
            }
        } else {
            enEl = el.getOwnerDocument().createElementNS(NAMESPACE, CONFIGURATIONS);
            el.appendChild(enEl);
        }
        for (M2Configuration config : configs) {
            Element child  = enEl.getOwnerDocument().createElementNS(NAMESPACE, CONFIG);
            child.setAttribute(CONFIG_ID_ATTR, config.getId());
            child.setAttribute(CONFIG_PROFILES_ATTR, StringUtils.join(config.getActivatedProfiles().iterator(), " "));
            for (Map.Entry<String,String> entry : config.getProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    Element prop  = enEl.getOwnerDocument().createElementNS(NAMESPACE, PROPERTY);
                    prop.setAttribute(PROPERTY_NAME_ATTR, key);
                    prop.setTextContent(value);
                    child.appendChild(prop);
                }
            }
            enEl.appendChild(child);
        }
        conf.putConfigurationFragment(el, shared);
    }

}
