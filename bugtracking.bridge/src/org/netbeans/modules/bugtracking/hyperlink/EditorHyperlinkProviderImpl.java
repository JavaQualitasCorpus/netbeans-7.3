/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.bugtracking.hyperlink;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.modules.bugtracking.spi.IssueFinder;
import org.netbeans.modules.bugtracking.util.BugtrackingUtil;
import org.netbeans.modules.bugtracking.util.IssueFinderUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import static org.netbeans.modules.bugtracking.util.IssueFinderUtils.HyperlinkSpanInfo;

/**
 * 
 * Provides hyperlink functionality on issue reference in code comments
 * 
 * @author Tomas Stupka
 */
public class EditorHyperlinkProviderImpl implements HyperlinkProviderExt, LookupListener {

    private static final Logger LOG = Logger.getLogger(EditorHyperlinkProviderImpl.class.getName());

    //--------------------------------------------------------------------------

    private IssueFinder[] issueFinders;

    {
        refreshIssueFinders();
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        refreshIssueFinders();
    }

    private void refreshIssueFinders() {
        Collection<IssueFinder> allInstances = IssueFinderUtils.getIssueFinders();
        IssueFinder[] newResult = new IssueFinder[allInstances.size()];
        allInstances.toArray(newResult);
        issueFinders = newResult;
    }

    //--------------------------------------------------------------------------

    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION); 
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        return getIssueSpan(doc, offset, type) != null;
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
        HyperlinkSpanInfo spanInfo = getIssueSpan(doc, offset, type);
        return (spanInfo != null) ? new int[] {spanInfo.startOffset,
                                               spanInfo.endOffset}
                                  : null;
    }

    @Override
    public void performClickAction(final Document doc, final int offset, final HyperlinkType type) {
        final String issueId = getIssueId(doc, offset, type);
        if(issueId == null) {
            return;
        }

        class IssueDisplayer implements Runnable {
            @Override
            public void run() {
                DataObject dobj = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
                File file = null;
                if (dobj != null) {
                    FileObject fileObject = dobj.getPrimaryFile();
                    if(fileObject != null) {
                        file = FileUtil.toFile(fileObject);
                    }
                }
                if(file == null) {
                    Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "EditorHyperlinkProviderImpl - no file found for given document");
                    return;
                }
                BugtrackingUtil.openIssue(file, issueId);
            }
        }
        RequestProcessor.getDefault().post(new IssueDisplayer());
    }

    @Override
    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
        return NbBundle.getMessage(EditorHyperlinkProviderImpl.class, "LBL_OpenIssue", new Object[] { getIssueId(doc, offset, type) });
    }

    // XXX get/unify from/with hyperlink provider
    private String getIssueId(Document doc, int offset, HyperlinkType type) {
        HyperlinkSpanInfo spanInfo = getIssueSpan(doc, offset, type);

        if (spanInfo == null) {
            return null;
        }

        String issueId = null;
        try {
            if ((spanInfo.startOffset <= offset) && (offset <= spanInfo.endOffset)) {
                /* at first, check that it is a valid reference text: */
                int length = spanInfo.endOffset - spanInfo.startOffset;
                String text = doc.getText(spanInfo.startOffset, length);
                int[] spans = spanInfo.issueFinder.getIssueSpans(text);
                if (spans.length == 2) {
                    /* valid - now just retreive the issue id: */
                    issueId = spanInfo.issueFinder.getIssueId(text);
                }
            }
        } catch (BadLocationException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        if(issueId == null) {
            try {
                Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "No issue found for {0}", doc.getText(spanInfo.startOffset, spanInfo.endOffset - spanInfo.startOffset));
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return issueId;
    }

    private HyperlinkSpanInfo getIssueSpan(final Document doc, final int offset, final HyperlinkType type) {
        if (issueFinders.length == 0) {
            return null;
        }

        final HyperlinkSpanInfo hyperlinkSpanInfo[] = new HyperlinkSpanInfo[1];
        doc.render(new Runnable() {
            @Override
            public void run() {
                TokenHierarchy th = TokenHierarchy.get(doc);
                List<TokenSequence> list = th.embeddedTokenSequences(offset, false);

                for (TokenSequence ts : list) {
                    if (ts == null) {
                        return;
                    }
                    ts.move(offset);
                    if (!ts.moveNext()) {
                        return;
                    }
                    Token t = ts.token();
                    TokenId tokenId;
                    String primCategory, name;

                    if (((tokenId = t.id()) == null)
                            || ((primCategory = tokenId.primaryCategory()) == null)
                            || ((name = tokenId.name()) == null)) {
                        continue;
                    }
                    if (primCategory.toUpperCase().indexOf("COMMENT") > -1 || // primaryCategory == commment should be more or less a convention // NOI18N
                            name.toUpperCase().indexOf("COMMENT") > -1) // consider this as a fallback // NOI18N
                    {
                        CharSequence text = t.text();
                        for (IssueFinder issueFinder : issueFinders) {
                            int[] span = issueFinder.getIssueSpans(text);
                            for (int i = 1; i < span.length; i += 2) {
                                if (ts.offset() + span[i - 1] <= offset && offset <= ts.offset() + span[i]) {
                                    hyperlinkSpanInfo[0] = new HyperlinkSpanInfo(issueFinder,
                                            ts.offset() + span[i - 1],
                                            ts.offset() + span[i]);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        });
        return hyperlinkSpanInfo[0];
    }

}