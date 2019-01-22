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

package org.netbeans.modules.j2ee.ddloaders.multiview;

import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.xml.cookies.ValidateXMLCookie;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.modules.j2ee.common.Util;
import org.netbeans.modules.j2ee.dd.api.common.RootInterface;
import org.netbeans.modules.j2ee.dd.impl.common.DDUtils;
import org.netbeans.modules.j2ee.dd.impl.ejb.EjbJarProxy;
import org.netbeans.modules.j2ee.ddloaders.ejb.DDChangeEvent;
import org.netbeans.modules.j2ee.ddloaders.ejb.DDChangeListener;
import org.netbeans.modules.j2ee.ddloaders.ejb.EjbJarDataLoader;
import org.netbeans.modules.j2ee.common.DDEditorNavigator;
import org.netbeans.modules.xml.multiview.SectionNode;
import org.netbeans.modules.xml.multiview.ui.SectionNodeInnerPanel;
import org.netbeans.modules.xml.multiview.ui.SectionNodeView;
import org.netbeans.modules.xml.multiview.ToolBarMultiViewElement;
import org.netbeans.spi.xml.cookies.DataObjectAdapters;
import org.netbeans.spi.xml.cookies.ValidateXMLSupport;
import org.openide.cookies.ViewCookie;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.RequestProcessor;
import org.xml.sax.InputSource;
import javax.swing.event.ChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.j2ee.dd.api.ejb.DDProvider;
import org.netbeans.modules.j2ee.dd.api.ejb.Ejb;
import org.netbeans.modules.j2ee.dd.api.ejb.EjbJar;
import org.netbeans.modules.j2ee.dd.api.ejb.EjbRelation;
import org.netbeans.modules.j2ee.dd.api.ejb.EnterpriseBeans;
import org.netbeans.modules.j2ee.dd.api.ejb.Entity;
import org.netbeans.modules.j2ee.dd.api.ejb.EntityAndSession;
import org.netbeans.modules.j2ee.dd.api.ejb.Relationships;
import org.netbeans.modules.j2ee.dd.api.ejb.Session;
import org.netbeans.modules.j2ee.dd.impl.common.ParseUtils;
import org.netbeans.modules.j2ee.ddloaders.catalog.EnterpriseCatalog;
import org.netbeans.modules.xml.multiview.XmlMultiViewDataObject;
import org.netbeans.modules.xml.multiview.XmlMultiViewElement;
import org.openide.awt.HtmlBrowser;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.MultiDataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Represents a DD object in the Repository.
 *
 * @author pfiala
 */
@Messages("CTL_SourceTabCaption=Source")
public class EjbJarMultiViewDataObject extends DDMultiViewDataObject
        implements DDChangeListener, DDEditorNavigator, FileChangeListener, ChangeListener {
    private EjbJarProxy ejbJar;
    private final List<FileObject> srcRoots = new ArrayList<FileObject>();
    private PropertyChangeListener ejbJarChangeListener;
    private Map entityHelperMap = new HashMap();
    private Map sessionHelperMap = new HashMap();
    
    private static final long serialVersionUID = 8857563089355069362L;
    
    private static final Logger LOGGER = Logger.getLogger(EjbJarMultiViewDataObject.class.getName());
    /**
     * Property name for documentDTD property
     */
    public static final String PROP_DOCUMENT_DTD = "documentDTD";   // NOI18N
    
    private static final int HOME = 10;
    private static final int REMOTE = 20;
    private static final int LOCAL_HOME = 30;
    private static final int LOCAL = 40;
    private static final String OVERVIEW = "Overview"; //NOI18N
    private static final String CMP_RELATIONSHIPS = "CmpRelationships"; //NOI18N
    
    private static final RequestProcessor rp = new RequestProcessor(EjbJarMultiViewDataObject.class);
    private final RequestProcessor.Task refreshSourcesTask = rp.create(new Runnable() {
        @Override
        public void run() {
            refreshSourceFolders ();
        }
    });

    public EjbJarMultiViewDataObject(FileObject pf, EjbJarDataLoader loader) throws DataObjectExistsException {
        super(pf, loader);
        
        // added ValidateXMLCookie
        InputSource in = DataObjectAdapters.inputSource(this);
        ValidateXMLCookie validateCookie = new ValidateXMLSupport(in);
        CookieSet set = getCookieSet();
        set.add(validateCookie);
        CookieSet.Factory viewCookieFactory = new ViewCookieFactory();
        set.add(ViewCookie.class, viewCookieFactory);
        
        
        Project project = getProject();
        if (project != null) {
            Sources sources = ProjectUtils.getSources(project);
            sources.addChangeListener(this);
        }
        refreshSourceFolders();
    }
    
    
    private void refreshSourceFolders() {
        SourceGroup[] groups;
        Project project = getProject();
        if (project != null) {
            Sources sources = ProjectUtils.getSources(project);
            groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        } else {
            groups = null;
        }
        if (groups != null) {
            synchronized(srcRoots){
                srcRoots.clear();
                for (int i = 0; i < groups.length; i++) {
                    org.netbeans.modules.j2ee.api.ejbjar.EjbJar ejbModule = org.netbeans.modules.j2ee.api.ejbjar.EjbJar.getEjbJar(groups[i].getRootFolder());
                    if ((ejbModule != null) && (ejbModule.getDeploymentDescriptor() != null)) {
                        try {
                            FileObject fo = groups[i].getRootFolder();
                            srcRoots.add(fo);
                            FileSystem fs = fo.getFileSystem();
                            fs.removeFileChangeListener(this); //avoid being added multiple times
                            fs.addFileChangeListener(this);
                        } catch (FileStateInvalidException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }
        }
    }
    
    
    private Project getProject() {
        return FileOwnerQuery.getOwner(getPrimaryFile());
    }

    public FileObject getProjectDirectory() {
        Project project = getProject();
        return project == null ? null : project.getProjectDirectory();
    }
    
    public SourceGroup[] getSourceGroups() {
        Project project = getProject();
        if (project != null) {
            return ProjectUtils.getSources(project).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        } else {
            return null;
        }
    }
    
    private String getPackageName(FileObject clazz) {
        synchronized(srcRoots){
            for (FileObject fo : srcRoots) {
                String rp = FileUtil.getRelativePath(fo, clazz);
                if (rp != null) {
                    if (clazz.getExt().length() > 0) {
                        rp = rp.substring(0, rp.length() - clazz.getExt().length() - 1);
                    }
                    return rp.replace('/', '.');
                }
            }
        }
        return null;
    }
    
    public EjbJar getEjbJar() {
        if (ejbJar == null) {
            try {
               parseDocument();
            } catch (IOException e) {
               Logger.getLogger("global").log(Level.INFO, null, e);
            }
        }
        return ejbJar;
    }
    
    protected Node createNodeDelegate() {
        return new EjbJarMultiViewDataNode(this);
    }
    
    /**
     * gets the Icon Base for node delegate when parser accepts the xml document as valid
     * <p/>
     * PENDING: move into node
     *
     * @return Icon Base for node delegate
     */
    protected String getIconBaseForValidDocument() {
        return Utils.ICON_BASE_DD_VALID;
    }
    
    /**
     * gets the Icon Base for node delegate when parser finds error(s) in xml document
     *
     * @return Icon Base for node delegate
     *         <p/>
     *         PENDING: move into node
     */
    protected String getIconBaseForInvalidDocument() {
        return Utils.ICON_BASE_DD_INVALID; // NOI18N
    }
    
    protected DataObject handleCopy(DataFolder f) throws IOException {
        DataObject dataObject = super.handleCopy(f);
        try {
            dataObject.setValid(false);
        } catch (PropertyVetoException e) {
            // should not occur
        }
        return dataObject;
    }
    
    /**
     * This methods gets called when servlet is changed
     *
     * @param evt - object that describes the change.
     */
    public void deploymentChange(DDChangeEvent evt) {
    }
    
    
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    private RequestProcessor.Task elementTask;
    private List deletedEjbNames;
    private List newFileNames;
    
    private void elementCreated(final String elementName) {
        synchronized (this) {
            if (newFileNames == null) {
                newFileNames = new ArrayList();
            }
            newFileNames.add(elementName);
        }
        
        if (elementTask == null) {
            elementTask = RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    if (deletedEjbNames != null) {
                        for (int i = 0; i < deletedEjbNames.size(); i++) {
                            String deletedServletName = (String) deletedEjbNames.get(i);
                            String deletedName = deletedServletName;
                            int index = deletedServletName.lastIndexOf("."); //NOI18N
                            if (index > 0) {
                                deletedName = deletedServletName.substring(index + 1);
                            }
                            boolean found = false;
                            for (int j = 0; j < newFileNames.size(); j++) {
                                String newFileName = (String) newFileNames.get(j);
                                String newName = newFileName;
                                int ind = newFileName.lastIndexOf("."); //NOI18N
                                if (ind > 0) {
                                    newName = newFileName.substring(ind + 1);
                                }
                                if (deletedName.equals(newName)) { // servlet was removed
                                    found = true;
                                    DDChangeEvent ddEvent =
                                            new DDChangeEvent(EjbJarMultiViewDataObject.this,
                                            EjbJarMultiViewDataObject.this, deletedServletName, newFileName,
                                            DDChangeEvent.EJB_CHANGED);
                                    deploymentChange(ddEvent);
                                    synchronized (EjbJarMultiViewDataObject.this) {
                                        newFileNames.remove(newFileName);
                                    }
                                    break;
                                }
                            }
                            if (!found) {
                                DDChangeEvent ddEvent =
                                        new DDChangeEvent(EjbJarMultiViewDataObject.this,
                                        EjbJarMultiViewDataObject.this, null, deletedServletName,
                                        DDChangeEvent.EJB_DELETED);
                                deploymentChange(ddEvent);
                            }
                        } //end for
                        synchronized (EjbJarMultiViewDataObject.this) {
                            deletedEjbNames = null;
                        }
                    } // servlets
                    
                    synchronized (EjbJarMultiViewDataObject.this) {
                        newFileNames = null;
                    }
                    
                }///end run
                
            }, 1500, Thread.MIN_PRIORITY);
        } else {
            elementTask.schedule(1500);
        }
    }
    
    public void fileRenamed(FileRenameEvent fileRenameEvent) {
        FileObject fo = fileRenameEvent.getFile();
        String resourceName = getPackageName(fo);
        if (resourceName != null) {
            int index = resourceName.lastIndexOf("."); //NOI18N
            String oldName = fileRenameEvent.getName();
            String oldResourceName = (index >= 0 ? resourceName.substring(0, index + 1) : "") + oldName;
            EjbJar ejbJar = getEjbJar();
            if (ejbJar.getStatus() == EjbJar.STATE_VALID) {
                fireEvent(oldResourceName, resourceName, DDChangeEvent.EJB_CHANGED);
            }
        }
    }
    
    public void fileFolderCreated(FileEvent fileEvent) {
    }
    
    public void fileDeleted(FileEvent fileEvent) {
        FileObject fo = fileEvent.getFile();
        String resourceName = getPackageName(fo);
        if (resourceName != null) {
            if (newFileNames == null) {
                fireEvent(null, resourceName, DDChangeEvent.EJB_DELETED);
            } else {
                Ejb[] ejbs = nullSafeGetEjbs();
                for (int i = 0; i < ejbs.length; i++) {
                    if (resourceName.equals(ejbs[i].getEjbClass())) {
                        synchronized (this) {
                            if (deletedEjbNames == null) {
                                deletedEjbNames = new ArrayList();
                            }
                            deletedEjbNames.add(resourceName);
                        }
                        break;
                    }
                }
            }
        }
    }
    
    private Ejb[] nullSafeGetEjbs(){
        EjbJar ejbJar = getEjbJar();
        if (ejbJar == null){
            return new Ejb[0];
        }
        EnterpriseBeans enterpriseBeans = ejbJar.getEnterpriseBeans();
        if (enterpriseBeans == null){
            return new Ejb[0];
        }
        Ejb[] result = enterpriseBeans.getEjbs();
        return result != null ? result : new Ejb[0];
    }
    
    public void fileDataCreated(FileEvent fileEvent) {
        FileObject fo = fileEvent.getFile();
        String resourceName = getPackageName(fo);
        if (resourceName != null) {
            elementCreated(resourceName);
        }
    }
    
    public void fileChanged(FileEvent fileEvent) {
    }
    
    public void fileAttributeChanged(FileAttributeEvent fileAttributeEvent) {
    }
    
    @Override
    public void stateChanged(javax.swing.event.ChangeEvent e) {
        //#181904 break the thread stack chain
        refreshSourcesTask.schedule(100);
    }
    
    protected void parseDocument() throws IOException {
        DDProvider ddProvider = DDProvider.getDefault();
        if (ejbJar == null || ejbJar.getOriginal() == null) {
            try {
                EjbJarProxy newEjbJar = (EjbJarProxy) ddProvider.getDDRoot(getPrimaryFile());
                if (newEjbJar == null){
                    LOGGER.log(Level.INFO, "Could not resolve EjbJar for " + getPrimaryFile().getPath() + ""); //NO18N
                    newEjbJar = new EjbJarProxy(null, null);
                }
                setEjbJar(newEjbJar);
            } catch (IOException e) {
                if (ejbJar == null) {
                    setEjbJar(new EjbJarProxy(null, null));
                }
            }
        } else {
            DDUtils.merge(ejbJar, createReader());
        }
        validateDocument();
    }
    
    protected void validateDocument() throws IOException {
        try{
            ParseUtils.parseDD(new InputSource(createInputStream()), new EnterpriseCatalog());
            setSaxError(null);
        }catch (SAXException saxe){
            setSaxError(saxe);
            ejbJar.setStatus(EjbJar.STATE_INVALID_UNPARSABLE);
            if (saxe instanceof SAXParseException) {
                ejbJar.setError((SAXParseException) saxe);
            } else if (saxe.getException() instanceof SAXParseException) {
                ejbJar.setError((SAXParseException) saxe.getException());
            }
        }   
    }
    
    private void setEjbJar(EjbJarProxy newEjbJar) {
        if(ejbJar != null) {
            ejbJar.removePropertyChangeListener(ejbJarChangeListener);
        }
        ejbJar = newEjbJar;
            if (ejbJarChangeListener == null) {
                ejbJarChangeListener = new EjbJarPropertyChangeListener();
            }
            ejbJar.addPropertyChangeListener(ejbJarChangeListener);
        }
    
    protected RootInterface getDDModel() {
        if (ejbJar == null) {
            try {
                parseDocument();
            } catch (IOException ex) {
                Logger.getLogger("global").log(Level.INFO, null, ex);
            }
        }
        return ejbJar;
    }
    
    public boolean isDocumentParseable() {
        return EjbJar.STATE_INVALID_UNPARSABLE != getEjbJar().getStatus();
    }
    
    protected String getPrefixMark() {
        return "<ejb-jar";
    }

    @Override
    protected String getEditorMimeType() {
        if (Util.isJavaEE5orHigher(getProject())){
            return "text/x-dd-ejbjar-ee5";
        }
        return "text/x-dd-ejbjar";
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @MultiViewElement.Registration(
        mimeType="text/x-dd-ejbjar",
        iconBase="org/netbeans/modules/j2ee/ddloaders/client/DDDataIcon.gif",
        persistenceType=TopComponent.PERSISTENCE_ONLY_OPENED,
        preferredID="multiview_xml",
        displayName="#CTL_SourceTabCaption",
        position=1
    )
    public static XmlMultiViewElement createXmlMultiViewElement(Lookup lookup) {
        return new XmlMultiViewElement(lookup.lookup(XmlMultiViewDataObject.class));
    }

    @MultiViewElement.Registration(
        mimeType="text/x-dd-ejbjar-ee5",
        iconBase="org/netbeans/modules/j2ee/ddloaders/client/DDDataIcon.gif",
        persistenceType=TopComponent.PERSISTENCE_ONLY_OPENED,
        preferredID="multiview_xml",
        displayName="#CTL_SourceTabCaption",
        position=1
    )
    public static XmlMultiViewElement createXmlMultiViewElementEE5(Lookup lookup) {
        return new XmlMultiViewElement(lookup.lookup(XmlMultiViewDataObject.class));
    }

    @MultiViewElement.Registration(
        displayName="#LBL_" + OVERVIEW,
        iconBase=Utils.ICON_BASE_DD_VALID + ".gif",
        persistenceType=TopComponent.PERSISTENCE_NEVER,
        preferredID="dd_multiview_" + OVERVIEW,
        mimeType="text/x-dd-ejbjar",
        position=500
    )
    public static MultiViewElement getOverview(Lookup context) {
        DataObject dObj = context.lookup(DataObject.class);
        assert dObj != null;
        return new EjbMultiViewElement((EjbJarMultiViewDataObject) dObj);
    }

    @MultiViewElement.Registration(
        displayName="#LBL_" + CMP_RELATIONSHIPS,
        iconBase=Utils.ICON_BASE_DD_VALID + ".gif",
        persistenceType=TopComponent.PERSISTENCE_NEVER,
        preferredID="dd_multiview_" + CMP_RELATIONSHIPS,
        mimeType="text/x-dd-ejbjar",
        position=1000
    )
    public static MultiViewElement getCmpRelationShips(Lookup context) {
        DataObject dObj = context.lookup(DataObject.class);
        assert dObj != null;
        return new CmpRelationshipsMultiViewElement((EjbJarMultiViewDataObject) dObj);
    }

    /** Used to detect if data model has already been created or not.
     * Method is called before switching to the design view from XML view when the document isn't parseable.
     */
    protected boolean isModelCreated() {
        return (ejbJar!=null && ejbJar.getOriginal()!=null);
    }
    
    public void showElement(final Object element) {
        if (element instanceof Relationships || element instanceof EjbRelation) {
            openView(2);
        } else {
            openView(1);
        }
        Utils.runInAwtDispatchThread(new Runnable() {
            public void run() {
                final SectionNodeView sectionView =
                        (SectionNodeView) EjbJarMultiViewDataObject.this .getActiveMVElement().getSectionView();
                final Node root = sectionView.getRoot();
                final SectionNode node = ((SectionNode) root.getChildren().getNodes()[0]).getNodeForElement(element);
                if (node != null) {
                    sectionView.openPanel(node);
                    ((SectionNodeInnerPanel) node.getSectionNodePanel().getInnerPanel()).focusData(element);
                }
            }
        });
    }
    

    /**
     * Factory for creating view cookies. <p>
     */
    private class ViewCookieFactory implements CookieSet.Factory {
        
        public Node.Cookie createCookie(Class klass) {
            if (klass == ViewCookie.class) {
                return new ViewSupport(EjbJarMultiViewDataObject.this.getPrimaryEntry());
            } else {
                return null;
            }
        }
    }
    
    /**
     * An implementation of ViewCookie that opens an HTML browser
     * for viewing the file.<p>
     */
    private static final class ViewSupport implements ViewCookie {
        
        private MultiDataObject.Entry primary;
        
        public ViewSupport(MultiDataObject.Entry primary) {
            this.primary = primary;
        }
        
        public void view() {
            try {
                HtmlBrowser.URLDisplayer.getDefault().showURL(primary.getFile().getURL());
            } catch (FileStateInvalidException e) {
            }
        }
    }
    
    /** Enable to access Active element
     */
    public ToolBarMultiViewElement getActiveMVElement() {
        return (ToolBarMultiViewElement)super.getActiveMultiViewElement();
    }
    
    private Ejb getEjbFromEjbClass(String ejbClassName) {
        EnterpriseBeans enterpriseBeans = getEjbJar().getEnterpriseBeans();
        if(enterpriseBeans != null) {
            Ejb[] ejbs = enterpriseBeans.getEjbs();
            for (int i = 0; i < ejbs.length; i++) {
                if (ejbs[i].getEjbClass() != null && ejbs[i].getEjbClass().equals(ejbClassName)) {
                    return ejbs[i];
                }
            }
        }
        return null;
    }
    
    private int getBeanInterfaceType(String interfaceName) {
        int interfaceType = -1;
        EjbJar ejbJar = getEjbJar();
        if (ejbJar == null) {
            return interfaceType;
        }
        EnterpriseBeans eb = ejbJar.getEnterpriseBeans();
        if (eb == null) {
            return interfaceType;
        }
        EntityAndSession[] beans = eb.getSession();
        for (int i = 0; i < beans.length; i++) {
            if (beans[i].getHome() != null &&
                    beans[i].getHome().equals(interfaceName)) {
                interfaceType = HOME;
                break;
            }
            if (beans[i].getRemote() != null &&
                    beans[i].getRemote().equals(interfaceName)) {
                interfaceType = REMOTE;
                break;
            }
            if (beans[i].getLocalHome() != null &&
                    beans[i].getLocalHome().equals(interfaceName)) {
                interfaceType = LOCAL_HOME;
                break;
            }
            if (beans[i].getLocal() != null &&
                    beans[i].getLocal().equals(interfaceName)) {
                interfaceType = LOCAL;
                break;
            }
        }
        return interfaceType;
    }
    
    private int getSpecificEvent(int eventType, int interfaceType) {
        if (eventType == DDChangeEvent.EJB_CHANGED) {
            switch (interfaceType) {
                case HOME:
                {
                    return DDChangeEvent.EJB_HOME_CHANGED;
                }
                case REMOTE:
                {
                    return DDChangeEvent.EJB_REMOTE_CHANGED;
                }
                case LOCAL_HOME:
                {
                    return DDChangeEvent.EJB_LOCAL_HOME_CHANGED;
                }
                case LOCAL:
                {
                    return DDChangeEvent.EJB_LOCAL_CHANGED;
                }
            }
        }
        if (eventType == DDChangeEvent.EJB_DELETED) {
            switch (interfaceType) {
                case HOME:
                {
                    return DDChangeEvent.EJB_HOME_DELETED;
                }
                case REMOTE:
                {
                    return DDChangeEvent.EJB_REMOTE_DELETED;
                }
                case LOCAL_HOME:
                {
                    return DDChangeEvent.EJB_LOCAL_HOME_DELETED;
                }
                case LOCAL:
                {
                    return DDChangeEvent.EJB_LOCAL_DELETED;
                }
            }
        }
        return -1;
    }
    
    private boolean fireEvent(String oldResourceName, String resourceName, int eventType) {
        boolean elementFound = false;
        String resource;
        int specificEventType = -1;
        if (eventType == DDChangeEvent.EJB_CHANGED) {
            resource = oldResourceName;
        } else {
            resource = resourceName;
        }
        Ejb ejb = getEjbFromEjbClass(resource);
        
        if (ejb != null) {
            if (eventType == DDChangeEvent.EJB_CHANGED) {
                specificEventType = DDChangeEvent.EJB_CLASS_CHANGED;
            } else {
                specificEventType = DDChangeEvent.EJB_CLASS_DELETED;
            }
            elementFound = true;
        }
        
        if (!elementFound) {
            int interfaceType = getBeanInterfaceType(resource);
            
            if (interfaceType > 0) {
                specificEventType =
                        getSpecificEvent(eventType, interfaceType);
                elementFound = true;
            }
        }
        if (elementFound) {
            assert(specificEventType > 0);
            DDChangeEvent ddEvent =
                    new DDChangeEvent(this, this, oldResourceName,
                    resourceName, specificEventType);
            deploymentChange(ddEvent);
        }
        return elementFound;
    }
    
    public EntityHelper getEntityHelper(Entity entity) {
        EntityHelper entityHelper = (EntityHelper) entityHelperMap.get(entity);
        if (entityHelper == null) {
            entityHelper = new EntityHelper(this, entity);
            entityHelperMap.put(entity, entityHelper);
        }
        return entityHelper;
    }
    
    public SessionHelper getSessionHelper(Session session) {
        SessionHelper sessionHelper = (SessionHelper) sessionHelperMap.get(session);
        if (sessionHelper == null) {
            sessionHelper = new SessionHelper(this, session);
            sessionHelperMap.put(session, sessionHelper);
        }
        return sessionHelper;
    }
    
    private class EjbJarPropertyChangeListener implements PropertyChangeListener {
        
        public void propertyChange(PropertyChangeEvent evt) {
            if (EjbJar.PROPERTY_STATUS.equals(evt.getPropertyName())) {
                return;
            }
            Object source = evt.getSource();
            if (source instanceof EnterpriseBeans) {
                Object oldValue = evt.getOldValue();
                Object newValue = evt.getNewValue();
                if ((oldValue instanceof Entity || newValue instanceof Entity)) {
                    entityHelperMap.keySet().retainAll(Arrays.asList(((EnterpriseBeans) source).getEntity()));
                } else if ((oldValue instanceof Session || newValue instanceof Session)) {
                    sessionHelperMap.keySet().retainAll(Arrays.asList(((EnterpriseBeans) source).getSession()));
                }
            }
        }
    }
    
    
}