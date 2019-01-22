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

package org.netbeans.api.java.source;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.tools.JavaFileObject;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullUnknown;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.java.source.parsing.ClassParser;
import org.netbeans.modules.java.source.parsing.ClasspathInfoTask;
import org.netbeans.modules.java.source.parsing.CompilationInfoImpl;
import org.netbeans.modules.java.source.parsing.JavacParser;
import org.netbeans.modules.java.source.parsing.JavacParserFactory;
import org.netbeans.modules.java.source.parsing.MimeTask;
import org.netbeans.modules.java.source.parsing.NewComilerTask;
import org.netbeans.modules.java.source.save.ElementOverlay;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.PositionRef;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/** Class representing Java source file opened in the editor.
 *
 * @author Petr Hrebejk, Tomas Zezula
 */
public final class JavaSource {
    
    
    public static enum Phase {
        MODIFIED,
    
        PARSED,
        
        ELEMENTS_RESOLVED,
    
        RESOLVED,   
    
        UP_TO_DATE;
    
    };
    
    public static enum Priority {
        MAX,
        HIGH,
        ABOVE_NORMAL,
        NORMAL,
        BELOW_NORMAL,
        LOW,
        MIN
    };
    
    
    
    /**
     * This specialization of {@link IOException} signals that a {@link JavaSource#runUserActionTask}
     * or {@link JavaSource#runModificationTask} failed due to lack of memory. The {@link InsufficientMemoryException#getFile}
     * method returns a file which cannot be processed.
     */
    public static final class InsufficientMemoryException extends IOException {        
        
        private FileObject fo;
        
        private InsufficientMemoryException (final String message, final FileObject fo) {
            super (message);
            this.fo = fo;
        }
        
        private InsufficientMemoryException (FileObject fo) {
            this (NbBundle.getMessage(JavaSource.class, "MSG_UnsufficientMemoryException", FileUtil.getFileDisplayName (fo)),fo);
        }
        
        
        /**
         * Returns file which cannot be processed due to lack of memory.
         * @return {@link FileObject}
         */
        public FileObject getFile () {
            return this.fo;
        }        
    }
    
    static {
        JavaSourceAccessor.setINSTANCE (new JavaSourceAccessorImpl ());
    }
    
    //Source files being processed, may be empty
    private final Collection<Source> sources;
    //Files being processed, may be empty
    private final Collection<FileObject> files;
    //Classpath info when explicitely given, may be null
    private final ClasspathInfo classpathInfo;
    //Cached classpath info
    //@GuardedBy(this)
    private ClasspathInfo cachedCpInfo;
    
    private static final Logger LOGGER = Logger.getLogger(JavaSource.class.getName());
                
    /**
     * Returns a {@link JavaSource} instance representing given {@link org.openide.filesystems.FileObject}s
     * and classpath represented by given {@link ClasspathInfo}.
     * @param cpInfo the classpaths to be used.
     * @param files for which the {@link JavaSource} should be created
     * @return a new {@link JavaSource}
     * @throws IllegalArgumentException if fileObject or cpInfo is null        
     */
    public static @NullUnknown JavaSource create(final @NonNull ClasspathInfo cpInfo, final @NonNull FileObject... files) throws IllegalArgumentException {
        if (files == null || cpInfo == null) {
            throw new IllegalArgumentException ();
        }
        return _create(cpInfo, Arrays.asList(files));
    }

    /**
     * Returns a {@link JavaSource} instance representing given {@link org.openide.filesystems.FileObject}s
     * and classpath represented by given {@link ClasspathInfo}.
     * @param cpInfo the classpaths to be used.
     * @param files for which the {@link JavaSource} should be created
     * @return a new {@link JavaSource}
     * @throws IllegalArgumentException if fileObject or cpInfo is null
     */
    public static @NullUnknown JavaSource create(final @NonNull ClasspathInfo cpInfo, final @NonNull Collection<? extends FileObject> files) throws IllegalArgumentException {
        return _create(cpInfo, files);
    }

    //where

    private static @NullUnknown JavaSource _create(final ClasspathInfo cpInfo, final @NonNull Collection<? extends FileObject> files) throws IllegalArgumentException {
        if (files == null) {
            throw new IllegalArgumentException ();
        }    
        try {
            return new JavaSource(cpInfo, files);
        } catch (DataObjectNotFoundException donf) {
            Logger.getLogger("global").warning("Ignoring non existent file: " + FileUtil.getFileDisplayName(donf.getFileObject()));     //NOI18N
        } catch (IOException ex) {            
            Exceptions.printStackTrace(ex);
        }        
        return null;
    }
    
    private static Map<FileObject, Reference<JavaSource>> file2JavaSource = new WeakHashMap<FileObject, Reference<JavaSource>>();
    private static final String[] supportedMIMETypes = new String[] {ClassParser.MIME_TYPE, JavacParser.MIME_TYPE};
    
    /**
     * Returns a {@link JavaSource} instance associated to given {@link org.openide.filesystems.FileObject},
     * it returns null if the {@link Document} is not associated with data type providing the {@link JavaSource}.
     * @param fileObject for which the {@link JavaSource} should be found/created.
     * @return {@link JavaSource} or null
     * @throws IllegalArgumentException if fileObject is null
     */
    public static @CheckForNull JavaSource forFileObject(@NonNull FileObject fileObject) throws IllegalArgumentException {
        if (fileObject == null) {
            throw new IllegalArgumentException ("fileObject == null");  //NOI18N
        }
        if (!fileObject.isValid()) {
            LOGGER.log(Level.FINE, "FileObject ({0}) passed to JavaSource.forFileObject is invalid", fileObject.toURI().toString());
            return null;
        }

        String mimeType = null;

        try {
            if (   fileObject.getFileSystem().isDefault()
                && fileObject.getAttribute("javax.script.ScriptEngine") != null
                && fileObject.getAttribute("template") == Boolean.TRUE) {
                LOGGER.log(Level.FINE, "FileObject ({0}) passed to JavaSource.forFileObject is a template", fileObject.toURI().toString());
                return null;
            }
            DataObject od = DataObject.find(fileObject);
            
            EditorCookie ec = od.getLookup().lookup(EditorCookie.class);
            
            if (!(ec instanceof CloneableEditorSupport)) {
                //allow creation of JavaSource for .class files:
                mimeType = FileUtil.getMIMEType(fileObject, supportedMIMETypes);
                if (!("application/x-class-file".equals(mimeType) || "class".equals(fileObject.getExt()))) {
                    LOGGER.log(Level.FINE, "DataObject ({1}) created for FileObject ({0}) passed to JavaSource.forFileObject does not provide CloneableEditorSupport and is not a classfile", new Object[] {fileObject.toURI().toString(), od.getClass().getName()});
                    return null;
                }
            }
        } catch (FileStateInvalidException ex) {
            LOGGER.log(Level.FINE, null, ex);
            return null;
        } catch (DataObjectNotFoundException ex) {
            LOGGER.log(Level.FINE, null, ex);
            return null;
        }
        
        Reference<JavaSource> ref = file2JavaSource.get(fileObject);
        JavaSource js = ref != null ? ref.get() : null;
        if (js == null) {
            mimeType = mimeType == null ? FileUtil.getMIMEType(fileObject, supportedMIMETypes) : mimeType;
            if ("application/x-class-file".equals(mimeType) || "class".equals(fileObject.getExt())) {   //NOI18N
                ClassPath bootPath = ClassPath.getClassPath(fileObject, ClassPath.BOOT);
                ClassPath compilePath = ClassPath.getClassPath(fileObject, ClassPath.COMPILE);
                if (compilePath == null) {
                    compilePath = ClassPathSupport.createClassPath(new URL[0]);
                }
                ClassPath srcPath = ClassPath.getClassPath(fileObject, ClassPath.SOURCE);
                if (srcPath == null) {
                    srcPath = ClassPathSupport.createClassPath(new URL[0]);
                }
                ClassPath execPath = ClassPath.getClassPath(fileObject, ClassPath.EXECUTE);
                if (execPath != null) {
                    bootPath = ClassPathSupport.createProxyClassPath(execPath, bootPath);
                }
                final ClasspathInfo info = ClasspathInfo.create(bootPath, compilePath, srcPath);
                FileObject root = ClassPathSupport.createProxyClassPath(bootPath,compilePath,srcPath).findOwnerRoot(fileObject);
                if (root == null) {
                    LOGGER.log(Level.FINE, "FileObject ({0}) passed to JavaSource.forFileObject of mimeType classfile does not have a corresponding root", fileObject.toURI().toString());
                    return null;
                }
                try {
                    js = new JavaSource (info,fileObject,root);
                } catch (IOException ioe) {
                    Exceptions.printStackTrace(ioe);
                }
            } 
            else {
                if (!"text/x-java".equals(mimeType) && !"java".equals(fileObject.getExt())) {  //NOI18N
                    LOGGER.log(Level.FINE, "FileObject ({0}) passed to JavaSource.forFileObject is not a Java source file (mimetype: {1})", new Object[] {fileObject.toURI().toString(), mimeType});
                    return null;
                }
                js = _create(null, Collections.singletonList(fileObject));
            }
            file2JavaSource.put(fileObject, new WeakReference<JavaSource>(js));
        }
        return js;
    }
    
    /**
     * Returns a {@link JavaSource} instance associated to the given {@link javax.swing.Document},
     * it returns null if the {@link Document} is not
     * associated with data type providing the {@link JavaSource}.
     * @param doc {@link Document} for which the {@link JavaSource} should be found/created.
     * @return {@link JavaSource} or null
     * @throws IllegalArgumentException if doc is null
     */
    public static @CheckForNull JavaSource forDocument(@NonNull Document doc) throws IllegalArgumentException {
        if (doc == null) {
            throw new IllegalArgumentException ("doc == null");  //NOI18N
        }
        Reference<?> ref = (Reference<?>) doc.getProperty(JavaSource.class);
        JavaSource js = ref != null ? (JavaSource) ref.get() : null;
        if (js == null) {
            Object source = doc.getProperty(Document.StreamDescriptionProperty);
            
            if (source instanceof DataObject) {
                DataObject dObj = (DataObject) source;
                if (dObj != null) {
                    js = forFileObject(dObj.getPrimaryFile());
                }
            }
        }
        return js;
    }
    
    /**
     * Creates a new instance of JavaSource
     * @param files to create JavaSource for
     * @param cpInfo classpath info
     */
    private JavaSource (final ClasspathInfo cpInfo,
                        final @NonNull Collection<? extends FileObject> files) throws IOException {
        boolean multipleSources = files.size() > 1;
        final List<Source> sources = new ArrayList<Source>(files.size());
        final List<FileObject> fl = new ArrayList<FileObject>(files.size());
        for (Iterator<? extends FileObject> it = files.iterator(); it.hasNext();) {
            FileObject file = it.next();
            Logger.getLogger("TIMER").log(Level.FINE, "JavaSource",
                new Object[] {file, this});                
            if (!file.isValid()) {
                if (multipleSources) {
                    LOGGER.warning("Ignoring non existent file: " + FileUtil.getFileDisplayName(file));     //NOI18N
                }
                else {
                    DataObject.find(file);  //throws IOE
                }
            }
            else {
                fl.add (file);
                sources.add(Source.create(file));
            }
        }
        this.files = Collections.unmodifiableList(fl);
        this.sources = Collections.unmodifiableList(sources);
        this.classpathInfo = cpInfo;        
    }

    /**
     * Creates a new instance of JavaSource for an class file
     * @param info the compilation info to be used
     * @param classFileObject the class file
     * @param root owning the class file
     * @throws IOException
     */
    private JavaSource (final @NonNull ClasspathInfo info,
                        final @NonNull FileObject classFileObject,
                        final @NonNull FileObject root) throws IOException {
        assert info != null;
        assert classFileObject != null;
        assert root != null;
        this.files = Collections.<FileObject>singletonList(classFileObject);
        this.sources = Collections.<Source>singletonList(Source.create(classFileObject));
        this.classpathInfo =  info;
    }
       
    /** Runs a task which permits for controlling phases of the parsing process.
     * You probably do not want to call this method unless you are reacting to
     * some user's GUI input which requires immediate action (e.g. code completion popup). 
     * In all other cases use {@link JavaSourceTaskFactory}.<BR>
     * Call to this method will cancel processing of all the phase completion tasks until
     * this task does not finish.<BR>
     * @see org.netbeans.api.java.source.CancellableTask for information about implementation requirements
     * @param task The task which.
     * @param shared if true the java compiler may be reused by other {@link org.netbeans.api.java.source.CancellableTasks},
     * the value false may have negative impact on the IDE performance.     
     * <div class="nonnormative">
     * <p>
     * It's legal to nest the {@link JavaSource#runUserActionTask} into another {@link JavaSource#runUserActionTask}.
     * It's also legal to nest the {@link JavaSource#runModificationTask} into {@link JavaSource#runUserActionTask},
     * the outer {@link JavaSource#runUserActionTask} does not see changes caused by nested {@link JavaSource#runModificationTask},
     * but the following nested task see them. 
     * </p>
     * </div>
     */
    public void runUserActionTask( final @NonNull Task<CompilationController> task, final boolean shared) throws IOException {
        runUserActionTaskImpl(task, shared);
    }
    
    private long runUserActionTaskImpl ( final Task<CompilationController> task, final boolean shared) throws IOException {
        Parameters.notNull("task", task);
        long currentId = -1;
        if (sources.isEmpty()) {
            try {
                ParserManager.parse(JavacParser.MIME_TYPE,new MimeTask(this, task, this.classpathInfo));
            } catch (final ParseException pe) {
                final Throwable rootCase = pe.getCause();
                if (rootCase instanceof CompletionFailure) {
                    IOException ioe = new IOException ();
                    ioe.initCause(rootCase);
                    throw ioe;
                }
                else if (rootCase instanceof RuntimeException) {
                    throw (RuntimeException) rootCase;
                }
                else {
                    IOException ioe = new IOException ();
                    ioe.initCause(rootCase);
                    throw ioe;
                }
            }
        }
        else {
            try {
                    final UserTask _task = new MultiTask(this, task, this.classpathInfo);
                    ParserManager.parse(sources, _task);
                } catch (final ParseException pe) {
                    final Throwable rootCase = pe.getCause();
                    if (rootCase instanceof CompletionFailure) {                        
                        IOException ioe = new IOException ();
                        ioe.initCause(rootCase);
                        throw ioe;
                    }
                    else if (rootCase instanceof RuntimeException) {
                        throw (RuntimeException) rootCase;
                    }
                    else {
                        IOException ioe = new IOException ();
                        ioe.initCause(rootCase);
                        throw ioe;
                    }                                        
                }                
        }
        return currentId;
    }

    //where
    private static class MultiTask extends ClasspathInfoTask {

        private final JavaSource js;
        private final Task<CompilationController> task;

        public MultiTask (final JavaSource js,
                          final  Task<CompilationController> task,
                          final ClasspathInfo cpInfo) {
            super (cpInfo);
            assert js != null;
            assert task != null;
            this.js = js;
            this.task = task;
        }

        @Override
        public void run(ResultIterator resultIterator) throws Exception {
            final Snapshot snapshot = resultIterator.getSnapshot();
            if (JavacParser.MIME_TYPE.equals(snapshot.getMimeType()) ||
                ClassParser.MIME_TYPE.equals(snapshot.getMimeType())) {
                Parser.Result result = resultIterator.getParserResult();
                if (result == null) {
                    //Deleted file of other parser critical issue
                    return;
                }
                final CompilationController cc = CompilationController.get(result);
                assert cc != null;
                cc.setJavaSource(this.js);
                task.run (cc);
                final JavacTaskImpl jt = cc.impl.getJavacTask();
                Log.instance(jt.getContext()).nerrors = 0;
            }
            else {
                Parser.Result result = findEmbeddedJava (resultIterator);
                if (result == null) {
                    //No embedded java
                    return;
                }
                final CompilationController cc = CompilationController.get(result);
                assert cc != null;
                cc.setJavaSource(this.js);
                task.run (cc);
                final JavacTaskImpl jt = cc.impl.getJavacTask();
                Log.instance(jt.getContext()).nerrors = 0;
            }
        }

        @Override
        public String toString () {
            return this.getClass().getName()+"["+task.getClass().getName()+"]";     //NOI18N
        }

        private Parser.Result findEmbeddedJava (final ResultIterator theMess) throws ParseException {
            final Collection<Embedding> todo = new ArrayList<Embedding>();
            //BFS should perform better than DFS in this dark.
            for (Embedding embedding : theMess.getEmbeddings()) {
                if (JavacParser.MIME_TYPE.equals(embedding.getMimeType())) {
                    return theMess.getResultIterator(embedding).getParserResult();
                }
                else {
                    todo.add(embedding);
                }
            }
            for (Embedding embedding : todo) {
                Parser.Result result  = findEmbeddedJava(theMess.getResultIterator(embedding));
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

    }

    long createTaggedController (final long timestamp, final Object[] controller) throws IOException {
        assert controller.length == 1;
        assert controller[0] == null || controller[0] instanceof CompilationController;
        try {
            CompilationController cc = (CompilationController) controller[0];
            final NewComilerTask _task = new NewComilerTask(this.classpathInfo, cc, timestamp);
            ParserManager.parse(sources, _task);
            controller[0] = _task.getCompilationController();
            return _task.getTimeStamp();
        } catch (final ParseException pe) {
            final Throwable rootCase = pe.getCause();
            if (rootCase instanceof CompletionFailure) {
                IOException ioe = new IOException ();
                ioe.initCause(rootCase);
                throw ioe;
            }
            else if (rootCase instanceof RuntimeException) {
                throw (RuntimeException) rootCase;
            }
            else {
                IOException ioe = new IOException ();
                ioe.initCause(rootCase);
                throw ioe;
            }
        }
    }
        
    /**
     * Performs the given task when the scan finished. When no background scan is running
     * it performs the given task synchronously. When the background scan is active it queues
     * the given task and returns, the task is performed when the background scan completes by
     * the thread doing the background scan.
     * @param task to be performed
     * @param shared if true the java compiler may be reused by other {@link org.netbeans.api.java.source.CancellableTasks},
     * the value false may have negative impact on the IDE performance.
     * @return {@link Future} which can be used to find out the sate of the task {@link Future#isDone} or {@link Future#isCancelled}.
     * The caller may cancel the task using {@link Future#cancel} or wait until the task is performed {@link Future#get}.
     * @throws IOException encapsulating the exception thrown by {@link CancellableTasks#run}
     * @since 0.12
     */
    public @NonNull Future<Void> runWhenScanFinished (final @NonNull Task<CompilationController> task, final boolean shared) throws IOException {
        Parameters.notNull("task", task);
        if (sources.isEmpty()) {
            try {
                return ParserManager.parseWhenScanFinished(JavacParser.MIME_TYPE,new MimeTask(this, task, this.classpathInfo));
            } catch (final ParseException pe) {
                final Throwable rootCase = pe.getCause();
                if (rootCase instanceof CompletionFailure) {
                    IOException ioe = new IOException ();
                    ioe.initCause(rootCase);
                    throw ioe;
                }
                else if (rootCase instanceof RuntimeException) {
                    throw (RuntimeException) rootCase;
                }
                else {
                    IOException ioe = new IOException ();
                    ioe.initCause(rootCase);
                    throw ioe;
                }
            }
        }
        else {
            try {
                    final UserTask _task = new MultiTask(this, task, this.classpathInfo);
                    return ParserManager.parseWhenScanFinished(sources, _task);
                } catch (final ParseException pe) {
                    final Throwable rootCase = pe.getCause();
                    if (rootCase instanceof CompletionFailure) {
                        IOException ioe = new IOException ();
                        ioe.initCause(rootCase);
                        throw ioe;
                    }
                    else if (rootCase instanceof RuntimeException) {
                        throw (RuntimeException) rootCase;
                    }
                    else {
                        IOException ioe = new IOException ();
                        ioe.initCause(rootCase);
                        throw ioe;
                    }
                }
        }
    }

       
    /** Runs a task which permits for modifying the sources.
     * Call to this method will cancel processing of all the phase completion tasks until
     * this task does not finish.<BR>
     * @see Task for information about implementation requirements
     * @param task The task which.
     */    
    public @NonNull ModificationResult runModificationTask(final @NonNull Task<WorkingCopy> task) throws IOException {
        if (task == null) {
            throw new IllegalArgumentException ("Task cannot be null");     //NOI18N
        }        
        final ModificationResult result = new ModificationResult(this);
        final ElementOverlay overlay = ElementOverlay.getOrCreateOverlay();
        long start = System.currentTimeMillis();

        Task<CompilationController> inner = new Task<CompilationController>() {
            @Override
            public void run(CompilationController cc) throws Exception {
                final WorkingCopy copy = new WorkingCopy(cc.impl, overlay);
                copy.setJavaSource(JavaSource.this);
                if (sources.isEmpty()) {
                    //runUserActionTask for source-less JavaSources does not require toPhase
                    //so automatically initializing also for runModificationTask:
                    copy.toPhase(Phase.PARSED);
                }
                task.run(copy);
                final JavacTaskImpl jt = copy.impl.getJavacTask();
                Log.instance(jt.getContext()).nerrors = 0;
                final List<ModificationResult.Difference> diffs = copy.getChanges(result.tag2Span);
                if (diffs != null && diffs.size() > 0) {
                    final FileObject file = copy.getFileObject();
                    result.diffs.put(file != null ? file : FileUtil.createMemoryFileSystem().getRoot().createData("temp", "java"), diffs);
                }
            }
        };
        
        runUserActionTask(inner, true);
        
        if (sources.size() == 1) {
            Logger.getLogger("TIMER").log(Level.FINE, "Modification Task",  //NOI18N
                new Object[] {sources.iterator().next().getFileObject(), System.currentTimeMillis() - start});
        }
        return result;
    }

    /**
     * Returns the classpaths ({@link ClasspathInfo}) used by this
     * {@link JavaSource}
     * @return {@link ClasspathInfo}, never returns null.
     */
    public @NonNull ClasspathInfo getClasspathInfo() {
        synchronized (this) {
            if (this.cachedCpInfo != null) {
                return this.cachedCpInfo;
            }
        }
        ClasspathInfo _tmp = this.classpathInfo;
        if (_tmp == null) {
            assert ! this.files.isEmpty();
            _tmp = ClasspathInfo.create(this.files.iterator().next());
        }
        synchronized (this) {
            if (this.cachedCpInfo == null) {
                this.cachedCpInfo = _tmp;
            }
            return this.cachedCpInfo;
        }	
    }
    
    /**
     * Returns unmodifiable {@link Collection} of {@link FileObject}s used by this {@link JavaSource}
     * @return the {@link FileObject}s
     */
    public @NonNull Collection<FileObject> getFileObjects() {
        return files;
    }
    private static class JavaSourceAccessorImpl extends JavaSourceAccessor {
                                                
        @Override
        public JavacTaskImpl getJavacTask (final CompilationInfo compilationInfo) {
            assert compilationInfo != null;
            return compilationInfo.impl.getJavacTask();
        }
        public JavaSource create(ClasspathInfo cpInfo, PositionConverter binding, Collection<? extends FileObject> files) throws IllegalArgumentException {
            return JavaSource.create(cpInfo, files);
        }

        public CompilationController createCompilationController (final Source s) throws IOException, ParseException {
            Parameters.notNull("s", s);
            JavacParserFactory factory = JavacParserFactory.getDefault();
            final Snapshot snapshot = s.createSnapshot();
            final JavacParser parser = factory.createPrivateParser(snapshot);
            if (parser == null)
                return null;
            final UserTask dummy = new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {                    
                }
            };
            parser.parse(snapshot,dummy, null);            
            return CompilationController.get(parser.getResult(dummy));
        }
        
        public long createTaggedCompilationController (JavaSource js, long currentTag, Object[] out) throws IOException {
            return js.createTaggedController(currentTag, out);
        }
                
        public CompilationInfo createCompilationInfo (final CompilationInfoImpl impl) {
            return new CompilationInfo(impl);
        }
        
        public CompilationController createCompilationController (final CompilationInfoImpl impl) {
            return new CompilationController(impl);
        }
        
        public void invalidate (final CompilationInfo info) {
            assert info != null;
            info.invalidate();
        }

        @Override
        public Collection<Source> getSources(JavaSource js) {
            assert js != null;
            return js.sources;
        }

        @Override
        public void setJavaSource(final CompilationInfo info, final JavaSource js) {
            assert info != null;
            assert js != null;
            info.setJavaSource(js);
        }

        @Override
        public void invalidateCachedClasspathInfo(FileObject file) {
            assert file != null;
            final Reference<JavaSource> ref = file2JavaSource.get(file);
            JavaSource js;
            if (ref != null && (js=ref.get())!=null) {
                synchronized (js) {
                    js.cachedCpInfo = null;
                }
            }
        }

        @Override
        public CompilationInfoImpl getCompilationInfoImpl(CompilationInfo info) {
            assert info != null;
            return info.impl;
        }

        @Override
        public @NonNull String generateReadableParameterName (@NonNull String typeName, @NonNull Set<String> used) {
            return SourceUtils.generateReadableParameterName(typeName, used);
        }

        @Override
        public ModificationResult.Difference createDifference(ModificationResult.Difference.Kind kind, PositionRef startPos, PositionRef endPos, String oldText, String newText, String description) {
            return new ModificationResult.Difference(kind, startPos, endPos, oldText, newText, description);
        }

        @Override
        public ModificationResult.Difference createNewFileDifference(JavaFileObject fileObject, String text) {
            return new ModificationResult.CreateChange(fileObject, text);
        }

        @Override
        public ModificationResult createModificationResult(Map<FileObject, List<ModificationResult.Difference>> diffs, Map<?, int[]> tag2Span) {
            ModificationResult result = new ModificationResult(null);

            result.diffs = diffs;
            result.tag2Span = tag2Span;

            return result;
        }

        @Override
        public ElementUtilities createElementUtilities(@NonNull JavacTaskImpl jt) {
            return new ElementUtilities(jt);
        }

        @Override
        public Map<FileObject, List<ModificationResult.Difference>> getDiffsFromModificationResult(ModificationResult mr) {
            return mr.diffs;
        }

        @Override
        public Map<?, int[]> getTagsFromModificationResult(ModificationResult mr) {
            return mr.tag2Span;
        }

        @Override
        public ClassIndex createClassIndex(ClassPath bootPath, ClassPath classPath, ClassPath sourcePath, boolean supportsChanges) {
            return new ClassIndex(bootPath, classPath, sourcePath, supportsChanges);
        }
    }                                                
}