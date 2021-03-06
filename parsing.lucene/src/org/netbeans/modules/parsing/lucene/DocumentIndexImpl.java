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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.parsing.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.search.Query;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.parsing.lucene.support.Convertor;
import org.netbeans.modules.parsing.lucene.support.DocumentIndex;
import org.netbeans.modules.parsing.lucene.support.DocumentIndexCache;
import org.netbeans.modules.parsing.lucene.support.Index;
import org.netbeans.modules.parsing.lucene.support.IndexDocument;
import org.netbeans.modules.parsing.lucene.support.Queries;
import org.netbeans.modules.parsing.lucene.support.Queries.QueryKind;

/**
 *
 * @author Tomas Zezula
 */
public class DocumentIndexImpl implements DocumentIndex, Runnable {
    
    private final Index luceneIndex;
    //@GuardedBy (this)
    private final DocumentIndexCache cache;
    /**
     * Transactional extension to the index
     */
    final Index.Transactional txLuceneIndex;
            
    private static final Convertor<IndexDocument,Document> ADD_CONVERTOR = Convertors.newIndexDocumentToDocumentConvertor();
    private static final Convertor<String,Query> REMOVE_CONVERTOR = Convertors.newSourceNameToQueryConvertor();
    private static final Convertor<Document,IndexDocumentImpl> QUERY_CONVERTOR = Convertors.newDocumentToIndexDocumentConvertor();
    private static final Logger LOGGER = Logger.getLogger(DocumentIndexImpl.class.getName());
    
    private final Set</*@GuardedBy("this")*/String> dirtyKeys = new HashSet<String>();
    final AtomicBoolean requiresRollBack = new AtomicBoolean();

    private DocumentIndexImpl (
            @NonNull final Index index,
            @NonNull final DocumentIndexCache cache) {
        assert index != null;
        assert cache != null;
        this.luceneIndex = index;
        this.cache = cache;
        if (index instanceof Index.Transactional) {
            this.txLuceneIndex = (Index.Transactional)index;
        } else {
            this.txLuceneIndex = null;
        }
    }
    
    /**
     * Adds document
     * @param document
     */
    @Override
    public void addDocument(IndexDocument document) {
        final boolean forceFlush;
        synchronized (this) {
            forceFlush = cache.addDocument(document);
        }
        if (forceFlush) {
            try {
                LOGGER.fine("Extra flush forced"); //NOI18N
                store(false, true);
                System.gc();
            } catch (IOException ioe) {
                //Reindexed in RU.storeChanges
                LOGGER.log(Level.WARNING, ioe.getMessage());
                requiresRollBack.set(true);
            }
        }
    }

    /**
     * Removes all documents for given path
     * @param relativePath
     */
    @Override
    public void removeDocument(String primaryKey) {
        final boolean forceFlush;
        synchronized (this) {
            forceFlush = cache.removeDocument(primaryKey);
        }
        if (forceFlush) {
            try {
                LOGGER.fine("Extra flush forced"); //NOI18N
                store(false, true);
            } catch (IOException ioe) {
                //Reindexed in RU.storeChanges
                LOGGER.log(Level.WARNING, ioe.getMessage());
                requiresRollBack.set(true);
            }
        }
    }

    
    /**
     * Checks if the Lucene index is valid.
     * @return {@link Status#INVALID} when the index is broken, {@link Status#EMPTY}
     * when the index does not exist or {@link  Status#VALID} if the index is valid
     * @throws IOException when index is already closed
     */
    @Override
    public Index.Status getStatus() throws IOException {
        return luceneIndex.getStatus(true);
    }
    
    @Override
    public void close() throws IOException {
        luceneIndex.close();
    }
    
    @Override
    public void store(boolean optimize) throws IOException {
        checkRollBackNeeded();
        store(optimize, false);
    }

    @Override
    public void run() {
        if (luceneIndex instanceof Runnable) {
            ((Runnable)luceneIndex).run();
        }
    }
    
    private void store(boolean optimize, boolean flushOnly) throws IOException {
        final  boolean change = storeImpl(optimize, flushOnly);
        if (!change && !flushOnly && txLuceneIndex != null) {
            commitImpl();
        }
    }

    private boolean storeImpl(
            final boolean optimize,
            final boolean flushOnly) throws IOException {
        final Collection<? extends IndexDocument> _toAdd;
        final Collection<? extends String> _toRemove;
        synchronized (this) {
            _toAdd = cache.getAddedDocuments();
            _toRemove = cache.getRemovedKeys();
            cache.clear();
            if (!dirtyKeys.isEmpty()) {
                for(IndexDocument ldoc : _toAdd) {
                    this.dirtyKeys.remove(ldoc.getPrimaryKey());
                }
                this.dirtyKeys.removeAll(_toRemove);
            }
        }
        if (!_toAdd.isEmpty() || !_toRemove.isEmpty()) {
            LOGGER.log(Level.FINE, "Flushing: {0}", luceneIndex.toString()); //NOI18N
            if (flushOnly && txLuceneIndex != null) {
                txLuceneIndex.txStore(
                        _toAdd,
                        _toRemove,
                        ADD_CONVERTOR,
                        REMOVE_CONVERTOR
                );
            } else {
                luceneIndex.store(
                        _toAdd,
                        _toRemove,
                        ADD_CONVERTOR,
                        REMOVE_CONVERTOR,
                        optimize);
            }
            return true;
        }
        return false;
    }

    private void commitImpl() throws IOException {
        checkRollBackNeeded();
        txLuceneIndex.commit();
    }

    private void checkRollBackNeeded() throws IOException {
        if (requiresRollBack.get()) {
            throw new IOException("Index requires rollback.");   //NOI18N
        }
    }

    @Override
    public Collection<? extends IndexDocument> query(String fieldName, String value, QueryKind kind, String... fieldsToLoad) throws IOException, InterruptedException {
        assert fieldName != null;
        assert value != null;
        assert kind != null;
        final List<IndexDocumentImpl> result = new LinkedList<IndexDocumentImpl>();
        final Query query = Queries.createQuery(fieldName, fieldName, value, kind);
        FieldSelector selector = null;
        if (fieldsToLoad != null && fieldsToLoad.length > 0) {
            final String[] fieldsWithSource = new String[fieldsToLoad.length+1];
            System.arraycopy(fieldsToLoad, 0, fieldsWithSource, 0, fieldsToLoad.length);
            fieldsWithSource[fieldsToLoad.length] = IndexDocumentImpl.FIELD_PRIMARY_KEY;
            selector = Queries.createFieldSelector(fieldsWithSource);
        }        
        luceneIndex.query(result, QUERY_CONVERTOR, selector, null, query);
        return result;
    }
    
    @Override
    public Collection<? extends IndexDocument> findByPrimaryKey (
            final String primaryKeyValue,
            final Queries.QueryKind kind,
            final String... fieldsToLoad) throws IOException, InterruptedException {
                return query(IndexDocumentImpl.FIELD_PRIMARY_KEY, primaryKeyValue, kind, fieldsToLoad);
    }

    @Override
    public void markKeyDirty(final String primaryKey) {
        synchronized (this) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "{0}, adding dirty key: {1}", new Object[]{this, primaryKey}); //NOI18N
            }
            dirtyKeys.add(primaryKey);
        }
    }

    @Override
    public void removeDirtyKeys(final Collection<? extends String> keysToRemove) {
        synchronized (this) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "{0}, Removing dirty keys: {1}", new Object[]{this, keysToRemove}); //NOI18N
            }
            dirtyKeys.removeAll(keysToRemove);
        }
    }

    @Override
    public Collection<? extends String> getDirtyKeys() {
        synchronized (this) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "{0}, dirty keys: {1}", new Object[]{this, dirtyKeys}); //NOI18N
            }
            return new ArrayList<String>(dirtyKeys);
        }
    }
    
    
    @Override
    public String toString () {
        return "DocumentIndex["+luceneIndex.toString()+"]";  //NOI18N
    }

    @NonNull
    public static DocumentIndex create(
            @NonNull final Index index,
            @NonNull final DocumentIndexCache cache) {
        return new DocumentIndexImpl(index, cache);
    }

    @NonNull
    public static DocumentIndex.Transactional createTransactional(
            @NonNull final Index.Transactional index,
            @NonNull final DocumentIndexCache cache) {
        return new DocumentIndexImpl.Transactional(index, cache);
    }

    private final static class Transactional extends DocumentIndexImpl implements DocumentIndex.Transactional {

        private Transactional(
            @NonNull final Index.Transactional index,
            @NonNull final DocumentIndexCache cache) {
            super(index, cache);
        }

        @Override
        public void txStore() throws IOException {
            super.storeImpl(false, true);
        }

        @Override
        public void commit() throws IOException {
            super.commitImpl();
        }

        @Override
        public void rollback() throws IOException {
            this.requiresRollBack.set(false);
            this.txLuceneIndex.rollback();
        }

        @Override
        public void clear() throws IOException {
            this.requiresRollBack.set(false);
            this.txLuceneIndex.clear();
        }

        @Override
        public String toString () {
            return "DocumentIndex.Transactional ["+super.luceneIndex.toString()+"]";  //NOI18N
        }

    }
                    
}
