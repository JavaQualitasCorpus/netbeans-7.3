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
package org.netbeans.modules.html.editor.gsf;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.StructureItem;
import org.netbeans.modules.csl.api.StructureScanner;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.html.editor.api.gsf.HtmlParserResult;
import org.netbeans.modules.html.editor.lib.api.elements.*;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.web.common.api.Pair;
import org.openide.filesystems.FileObject;

/**
 *
 * @author mfukala@netbeans.org
 */
public class HtmlStructureScanner implements StructureScanner {

    private static final Logger LOGGER = Logger.getLogger(HtmlStructureScanner.class.getName());
    private static final boolean LOG = LOGGER.isLoggable(Level.FINE);
    private static final long MAX_SNAPSHOT_SIZE = 4 * 1024 * 1024;
    private Reference<Pair<ParserResult, List<HtmlStructureItem>>> cache;

    private boolean isOfSupportedSize(ParserResult info) {
        Snapshot snapshot = info.getSnapshot();
        int slen = snapshot.getText().length();
        return slen < MAX_SNAPSHOT_SIZE;
    }

    @Override
    public List<? extends StructureItem> scan(final ParserResult info) {
        //temporary workaround for 
        //Bug 211139 - HtmlStructureScanner.scan() called twice with the same ParserResult 
        //so it is easier to debug
        if (cache != null) {
            Pair<ParserResult, List<HtmlStructureItem>> pair = cache.get();
            if (pair != null) {
                if (info == pair.getA()) {
                    return pair.getB();
                }
            }
        }

        if (!isOfSupportedSize(info)) {
            return Collections.emptyList();
        }

        HtmlParserResult presult = (HtmlParserResult) info;
        Node root = ((HtmlParserResult) presult).root();

        if (LOG) {
            LOGGER.log(Level.FINE, "HTML parser tree output:");
            LOGGER.log(Level.FINE, root.toString());
        }

        Snapshot snapshot = info.getSnapshot();
        FileObject file = snapshot.getSource().getFileObject();
        List<StructureItem> elements = new ArrayList<StructureItem>();
        for(OpenTag tag : root.children(OpenTag.class)) {
            HtmlElementHandle handle = new HtmlElementHandle(tag, file);
            StructureItem si = new HtmlStructureItem(tag, handle, snapshot);
            elements.add(si);
        }

        //cache
        Pair<ParserResult, List<HtmlStructureItem>> pair = new Pair(info, elements);
        cache = new WeakReference<Pair<ParserResult, List<HtmlStructureItem>>>(pair);

        return elements;

    }

    @Override
    public Map<String, List<OffsetRange>> folds(final ParserResult info) {
        if (!isOfSupportedSize(info)) {
            return Collections.emptyMap();
        }

        final BaseDocument doc = (BaseDocument) info.getSnapshot().getSource().getDocument(false);
        if (doc == null) {
            return Collections.emptyMap();
        }

        final Map<String, List<OffsetRange>> folds = new HashMap<String, List<OffsetRange>>();
        final List<OffsetRange> tags = new ArrayList<OffsetRange>();
        final List<OffsetRange> comments = new ArrayList<OffsetRange>();

        ElementVisitor foldsSearch = new ElementVisitor() {
            @Override
            public void visit(Element node) {
                if (node.type() == ElementType.OPEN_TAG
                        || node.type() == ElementType.COMMENT) {
                    try {

                        int from = node.from();
                        int to = node.type() == ElementType.OPEN_TAG
                                ? ((OpenTag) node).semanticEnd()
                                : node.to();

                        int so = documentPosition(from, info.getSnapshot());
                        int eo = documentPosition(to, info.getSnapshot());

                        if (so == -1 || eo == -1) {
                            //cannot be mapped back properly
                            return;
                        }

                        if (eo > doc.getLength()) {
                            eo = doc.getLength();
                            if (so > eo) {
                                so = eo;
                            }
                        }

                        if (Utilities.getLineOffset(doc, so) < Utilities.getLineOffset(doc, eo)) {
                            //do not creare one line folds
                            //XXX this logic could possibly seat in the GSF folding impl.
                            if (node.type() == ElementType.OPEN_TAG) {
                                tags.add(new OffsetRange(so, eo));
                            } else {
                                comments.add(new OffsetRange(so, eo));
                            }
                        }
                    } catch (BadLocationException ex) {
                        LOGGER.log(Level.INFO, null, ex);
                    }
                }
            }
        };

        //the document is touched during the ast tree visiting, we need to lock it
        doc.readLock();
        try {
            Collection<Node> roots = ((HtmlParserResult) info).roots().values();
            for (Node root : roots) {
                ElementUtils.visitChildren(root, foldsSearch);
            }
        } finally {
            doc.readUnlock();
        }
        folds.put("tags", tags);
        folds.put("comments", comments);

        return folds;
    }

    private static int documentPosition(int astOffset, Snapshot snapshot) {
        return snapshot.getOriginalOffset(astOffset);
    }

    @Override
    public Configuration getConfiguration() {
        return new Configuration(false, false, 0);
    }

}