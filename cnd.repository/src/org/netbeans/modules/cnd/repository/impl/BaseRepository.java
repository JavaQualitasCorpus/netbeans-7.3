/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.repository.impl;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.modules.cnd.repository.api.CacheLocation;
import org.netbeans.modules.cnd.repository.api.Repository;
import org.netbeans.modules.cnd.repository.disk.FilesAccessStrategy;
import org.netbeans.modules.cnd.repository.disk.FilesAccessStrategyImpl;
import org.netbeans.modules.cnd.repository.disk.StorageAllocator;
import org.netbeans.modules.cnd.repository.translator.RepositoryTranslatorImpl;
import org.netbeans.modules.cnd.repository.relocate.api.UnitCodec;

/**
 *
 * @author Vladimir Kvashin
 */
public abstract class BaseRepository implements Repository, UnitCodec {
    
    public static final int REPO_DENOM = 100000;
    
    private final UnitCodecImpl codec;
    private final CacheLocation cacheLocation;
    private final RepositoryTranslatorImpl translator;
    private final StorageAllocator storageAllocator;
    private final FilesAccessStrategy filesAccessStrategy;

    private final Map<Integer, Object> unitLocks = new HashMap<Integer, Object>();    
    private static final class UnitLock {}
    private final Object mainUnitLock = new UnitLock();

    protected BaseRepository(int id, CacheLocation cacheLocation) {
        this.codec = new UnitCodecImpl(id);
        this.cacheLocation = cacheLocation;
        this.storageAllocator = new StorageAllocator(cacheLocation);
        this.filesAccessStrategy = new FilesAccessStrategyImpl(storageAllocator, this);
        this.translator = new RepositoryTranslatorImpl(storageAllocator, this);
    }

    public final Object getUnitLock(int unitId) {
        synchronized (mainUnitLock) {
            Object lock = unitLocks.get(unitId);
            if (lock == null) {
                lock = new NamedLock("unitId=" + unitId); // NOI18N
                unitLocks.put(unitId, lock);
            }
            return lock;
        }
    }
   
    UnitCodec getUnitCodec() {
        return codec;
    }
    
    @Override
    public int unmaskRepositoryID(int unitId) {
        return codec.unmaskRepositoryID(unitId);
    }

    @Override
    public int maskByRepositoryID(int unitId) {
        return codec.maskByRepositoryID(unitId);
    }

    public final RepositoryTranslatorImpl getTranslation() {
        return translator;
    }

    public StorageAllocator getStorageAllocator() {
        return storageAllocator;
    }

    public FilesAccessStrategy getFilesAccessStrategy() {
        return filesAccessStrategy;
    }

    public CacheLocation getCacheLocation() {
        return cacheLocation;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ' ' + codec.id + ' ' + cacheLocation;
    }
    
    private static final class NamedLock {

        private final String name;

        public NamedLock(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NamedLock other = (NamedLock) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }
    }   
    
    private static final class UnitCodecImpl implements UnitCodec {
        private final int id;

        public UnitCodecImpl(int repositoryID) {
            this.id = repositoryID;            
        }

        @Override
        public int unmaskRepositoryID(int clientUnitId) {
            return clientUnitId % REPO_DENOM; // write it *without* repository ID
        }

        @Override
        public int maskByRepositoryID(int internalUnitId) {
            return id * REPO_DENOM + (internalUnitId % REPO_DENOM); // add repository ID
        }
        
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + this.id;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final UnitCodecImpl other = (UnitCodecImpl) obj;
            return this.id == other.id;
        }

        @Override
        public String toString() {
            return "UnitCodecImpl{" + "id=" + id + '}'; // NOI18N
        }
    }
}