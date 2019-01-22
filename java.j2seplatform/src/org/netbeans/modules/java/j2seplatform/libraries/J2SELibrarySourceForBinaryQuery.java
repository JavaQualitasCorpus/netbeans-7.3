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

package org.netbeans.modules.java.j2seplatform.libraries;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.api.project.libraries.LibraryManager;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.URLMapper;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.WeakListeners;

/**
 * Finds the locations of sources for various libraries.
 * @author Tomas Zezula
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation.class, position=150)
public class J2SELibrarySourceForBinaryQuery implements SourceForBinaryQueryImplementation2 {

    private static final Logger LOG = Logger.getLogger (J2SELibrarySourceForBinaryQuery.class.getName ());
    private final Map<URL,SourceForBinaryQueryImplementation2.Result> cache = new ConcurrentHashMap<URL,SourceForBinaryQueryImplementation2.Result>();
    private final Map<URL,URL> normalizedURLCache = new ConcurrentHashMap<URL,URL>();

    /** Default constructor for lookup. */
    public J2SELibrarySourceForBinaryQuery() {}

    @Override
    public SourceForBinaryQueryImplementation2.Result findSourceRoots2 (URL binaryRoot) {
        SourceForBinaryQueryImplementation2.Result res = cache.get(binaryRoot);
        if (res != null) {
            return res;
        }
        try {
            boolean isNormalizedURL = isNormalizedURL (binaryRoot);
            for (LibraryManager mgr : LibraryManager.getOpenManagers()) {
                for (Library lib : mgr.getLibraries()) {
                    if (lib.getType().equals(J2SELibraryTypeProvider.LIBRARY_TYPE)) {
                        for (URL entry : lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_CLASSPATH)) {
                            URL normalizedEntry = entry;
                            if (isNormalizedURL) {
                                try {
                                    normalizedEntry = getNormalizedURL (normalizedEntry);
                                } catch (MalformedURLException ex) {
                                    LOG.log (Level.INFO, "Invalid URL: " + normalizedEntry, ex);
                                    return null;
                                }
                            }
                            if (binaryRoot.equals(normalizedEntry)) {
                                res = new Result(entry, lib);
                                cache.put(binaryRoot, res);
                                return res;
                            }
                        }
                    }
                }
            }
        } catch (MalformedURLException ex) {
            LOG.log (Level.INFO, "Invalid URL: " + binaryRoot, ex);
            //cache.put (binaryRoot, null);
        }
        return null;
    }
    
    
    @Override
    public SourceForBinaryQuery.Result findSourceRoots (final URL binaryRoot) {
        return this.findSourceRoots2(binaryRoot);
    }
    
    public void preInit() {
        for (final LibraryManager lm : LibraryManager.getOpenManagers()) {
            for (final Library lib : lm.getLibraries()) {
                if (J2SELibraryTypeProvider.LIBRARY_TYPE.equals(lib.getType())) {
                    for (final URL url : lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_CLASSPATH)) {
                        try {
                            getNormalizedURL (url);
                        } catch (MalformedURLException ex) {
                            LOG.log (Level.INFO, "Invalid URL: " + url, ex);
                        }
                    }
                }
            }
        }
    }
    
    private URL getNormalizedURL (URL url) throws MalformedURLException {
        //URL is already nornalized, return it
        if (isNormalizedURL(url)) {
            return url;
        }
        //Todo: Should listen on the LibrariesManager and cleanup cache        
        // in this case the search can use the cache onle and can be faster 
        // from O(n) to O(ln(n))
        URL normalizedURL = normalizedURLCache.get(url);
        if (normalizedURL == null) {
            FileObject fo = URLMapper.findFileObject(url);
            if (fo != null) {
                try {
                    normalizedURL = fo.getURL();
                    this.normalizedURLCache.put (url, normalizedURL);
                } catch (FileStateInvalidException e) {
                    Exceptions.printStackTrace(e);
                }
            }
        }
        return normalizedURL;
    }
    
    /**
     * Returns true if the given URL is file based, it is already
     * resolved either into file URL or jar URL with file path.
     * @param URL url
     * @return true if  the URL is normal
     */
    private static boolean isNormalizedURL (URL url) throws MalformedURLException {
        if ("jar".equals(url.getProtocol())) { //NOI18N
            String path = url.getPath();
            int index = path.indexOf("!/"); //NOI18N
            if (index < 0)
                throw new MalformedURLException ();
            String jarPath = path.substring (0, index);
            if (
                jarPath.indexOf ("file://") > -1 &&         //NOI18N
                jarPath.indexOf("file:////") == -1          //NOI18N
            ) {
                /* Replace because JDK application classloader wrongly recognizes UNC paths. */
                jarPath = jarPath.replaceFirst ("file://", "file:////");  //NOI18N
            }
            url = new   URL(jarPath);
        }
        return "file".equals(url.getProtocol());    //NOI18N
    }
    
    
    private static class Result implements SourceForBinaryQueryImplementation2.Result, PropertyChangeListener {
        
        private Library lib;
        private URL entry;
        private final ChangeSupport cs = new ChangeSupport(this);
        private FileObject[] cache;
        
        @SuppressWarnings("LeakingThisInConstructor")
        public Result (URL queryFor, Library lib) {
            this.entry = queryFor;
            this.lib = lib;
            this.lib.addPropertyChangeListener(WeakListeners.propertyChange(this, this.lib));
        }
        
        @Override
        public synchronized FileObject[] getRoots () {
            if (this.cache == null) {
                // entry is not resolved so directly volume content can be searched for it:
                if (this.lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_CLASSPATH).contains(entry)) {
                    List<FileObject> result = new ArrayList<FileObject>();
                    for (URL u : lib.getContent(J2SELibraryTypeProvider.VOLUME_TYPE_SRC)) {
                        FileObject sourceRoot = URLMapper.findFileObject(u);
                        if (sourceRoot!=null) {
                            result.add (sourceRoot);
                        }
                    }
                    this.cache = result.toArray(new FileObject[result.size()]);
                }
                else {
                    this.cache = new FileObject[0];
                }
            }
            return this.cache;
        }
        
        @Override
        public synchronized void addChangeListener (ChangeListener l) {
            assert l != null : "Listener cannot be null"; // NOI18N
            cs.addChangeListener(l);
        }
        
        @Override
        public synchronized void removeChangeListener (ChangeListener l) {
            assert l != null : "Listener cannot be null"; // NOI18N
            cs.removeChangeListener(l);
        }
        
        @Override
        public void propertyChange (PropertyChangeEvent event) {
            if (Library.PROP_CONTENT.equals(event.getPropertyName())) {
                synchronized (this) {                    
                    this.cache = null;
                }
                cs.fireChange();
            }
        }

        @Override
        public boolean preferSources() {
            return false;
        }
        
    }
    
}