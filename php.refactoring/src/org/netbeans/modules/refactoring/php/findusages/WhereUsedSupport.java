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
package org.netbeans.modules.refactoring.php.findusages;

import java.util.*;
import javax.swing.Icon;
import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.UiUtils;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.php.editor.api.ElementQuery;
import org.netbeans.modules.php.editor.api.ElementQuery.Index;
import org.netbeans.modules.php.editor.api.ElementQueryFactory;
import org.netbeans.modules.php.editor.api.PhpElementKind;
import org.netbeans.modules.php.editor.api.QuerySupportFactory;
import org.netbeans.modules.php.editor.api.elements.MethodElement;
import org.netbeans.modules.php.editor.api.elements.PhpElement;
import org.netbeans.modules.php.editor.api.elements.TypeElement;
import org.netbeans.modules.php.editor.model.*;
import org.netbeans.modules.php.editor.parser.PHPParseResult;
import org.netbeans.modules.php.editor.parser.astnodes.ASTNode;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Radek Matous
 */

@ActionReferences({
    @ActionReference(id = @ActionID(category = "Refactoring", id = "org.netbeans.modules.refactoring.api.ui.WhereUsedAction"), path = "Loaders/text/x-php5/Actions", position = 1700)})
public final class WhereUsedSupport {

    private ASTNode node;
    private FileObject fo;
    private int offset;
    private PhpElementKind kind;
    private final Set<ModelElement> declarations;
    private ModelElement modelElement;
    private Results results;
    private Set<FileObject> unopenedFiles;
    private Set<Modifier> modifier;
    private FindUsageSupport usageSupport;
    private ElementQuery.Index idx;

    private WhereUsedSupport(ElementQuery.Index idx,Set<ModelElement> declarations, ASTNode node, FileObject fo) {
        this(idx, declarations, node.getStartOffset(), fo);
        this.node = node;
    }

    private WhereUsedSupport(ElementQuery.Index idx, Set<ModelElement> declarations, int offset, FileObject fo) {
        this.fo = fo;
        this.declarations = declarations;
        this.offset = offset;
        this.idx = idx;
        setModelElement(ModelUtils.getFirst(declarations));
        kind = this.modelElement.getPhpElementKind();
        this.results = new Results();
    }

    void setModelElement(ModelElement modelElement) {
        this.modelElement = modelElement;
        this.usageSupport = FindUsageSupport.getInstance(idx, this.modelElement);
    }

    public String getName() {
        return modelElement.getName();
    }

    public ASTNode getASTNode() {
        return node;
    }

    /*public FileObject getFileObject() {
        return fo;
    }*/

    public int getOffset() {
        return offset;
    }

    public void clearResults() {
        if (results != null) {
            results.clear();
        }
    }

    public PhpElementKind getKind() {
        return kind;
    }

    public ElementKind getElementKind() {
        return modelElement.getPHPElement().getKind();
    }

    public Set<Modifier> getModifiers() {
        ModelElement attributeElement = getModelElement();
        return getModifiers(attributeElement);
    }

    public WhereUsedSupport.Results getResults() {
        return results;
    }

    void overridingMethods() {
        Collection<MethodElement> methods = usageSupport.overridingMethods();
        for (MethodElement meth : methods) {
            results.addEntry(meth);
        }
    }

    void collectSubclasses() {
        Collection<TypeElement> subclasses = usageSupport.subclasses();
        for (TypeElement typeElement : subclasses) {
            results.addEntry(typeElement);
        }
    }

    void collectDirectSubclasses() {
        Collection<TypeElement> subclasses = usageSupport.directSubclasses();
        for (TypeElement typeElement : subclasses) {
            results.addEntry(typeElement);
        }
    }

    void collectUsages(FileObject fileObject) {
        Collection<Occurence> occurences = usageSupport.occurences(fileObject);
        if (occurences != null) {
            for (Occurence occurence : occurences) {
                results.addEntry(fileObject, occurence);
            }
        }
    }

    private static Occurence findOccurence(final Model model, final int offset) {
        Occurence result = model.getOccurencesSupport(offset).getOccurence();
        if (result == null) {
            result = model.getOccurencesSupport(offset + "$".length()).getOccurence(); //NOI18N
        }
        return result;
    }

    public static WhereUsedSupport getInstance(final PHPParseResult info, final int offset) {
        Model model = ModelFactory.getModel(info);
        final Occurence occurence = findOccurence(model, offset);
        final Set<ModelElement> declarations = new HashSet<ModelElement>();
        final Collection<? extends PhpElement> allDeclarations = occurence != null ? occurence.getAllDeclarations() : Collections.<PhpElement>emptyList();
        boolean canContinue = occurence != null && allDeclarations.size() > 0 && allDeclarations.size() < 5;
        if (canContinue && EnumSet.of(Occurence.Accuracy.EXACT, Occurence.Accuracy.MORE, Occurence.Accuracy.UNIQUE).contains(occurence.degreeOfAccuracy())) {
            for (final PhpElement declarationElement : allDeclarations) {
                try {
                    final FileObject fileObject = declarationElement.getFileObject();
                    FileObject parserFo = info.getSnapshot().getSource().getFileObject();
                    if (fileObject != null && parserFo != fileObject) {
                        ParserManager.parse(Collections.singleton(Source.create(fileObject)), new UserTask() {

                            @Override
                            public void run(ResultIterator resultIterator) throws Exception {
                                Result parserResult = resultIterator.getParserResult();
                                if (parserResult != null && parserResult instanceof PHPParseResult) {
                                    Model modelForDeclaration = ModelFactory.getModel((PHPParseResult)parserResult);
                                    declarations.add(modelForDeclaration.findDeclaration(declarationElement));
                                }
                            }
                        });
                    } else {
                        declarations.add(model.findDeclaration(declarationElement));
                    }
                } catch (ParseException ex) {
                    Exceptions.printStackTrace(ex);
                    return null;
                }
            }
            final Index indexQuery = ElementQueryFactory.createIndexQuery(QuerySupportFactory.get(info));
            FileObject fileObject = info.getSnapshot().getSource().getFileObject();
            return getInstance(declarations, indexQuery, fileObject, offset);
        }
        return null;
    }

    public static WhereUsedSupport getInstance(final Set<ModelElement> declarations, final Index indexQuery, final FileObject fileObject, final int offset) {
        final ModelElement declaration = ModelUtils.getFirst(declarations);
        return (declaration != null && declarations.size() > 0) ? new WhereUsedSupport(indexQuery, declarations, offset, fileObject) : null;
    }

    public ModelElement getModelElement() {
        return modelElement;
    }

    public List<ModelElement> getModelElements() {
        return new ArrayList<ModelElement>(declarations);
    }

    Set<FileObject> getRelevantFiles() {
        ModelElement mElement = getModelElement();
        if (mElement instanceof VariableName) {
            VariableName variable = (VariableName) mElement;
            if (!variable.isGloballyVisible()) {
                return Collections.singleton(mElement.getFileObject());
            }
        } else if (mElement != null && mElement.getPhpModifiers().isPrivate()) {
            return Collections.singleton(mElement.getFileObject());
        }

        return usageSupport.inFiles();
    }

    private Set<Modifier> getModifiers(ModelElement mElement) {
        if (modifier == null) {
            Set<Modifier> retval = Collections.emptySet();
            if (mElement != null && mElement.getInScope() instanceof TypeScope) {
                retval = EnumSet.noneOf(Modifier.class);
                if (mElement.getPhpModifiers().isPrivate()) {
                    retval.add(Modifier.PRIVATE);
                } else if (mElement.getPhpModifiers().isProtected()) {
                    retval.add(Modifier.PROTECTED);
                }
                if (mElement.getPhpModifiers().isPublic()) {
                    retval.add(Modifier.PUBLIC);
                }
                if (mElement.getPhpModifiers().isStatic()) {
                    retval.add(Modifier.STATIC);
                }
            }
           modifier = retval;
        }
        return modifier;
    }

    public static boolean isAlreadyInResults(ASTNode node, Set<ASTNode> results) {
        OffsetRange newOne = new OffsetRange(node.getStartOffset(), node.getEndOffset());
        for (Iterator<ASTNode> it = results.iterator(); it.hasNext();) {
            ASTNode aSTNode = it.next();
            OffsetRange oldOne = new OffsetRange(aSTNode.getStartOffset(), aSTNode.getEndOffset());
            if (newOne.containsInclusive(oldOne.getStart()) || oldOne.containsInclusive(newOne.getStart())) {
                return true;
            }
        }
        return false;
    }


    public class Results {

        Collection<WhereUsedElement> elements = new TreeSet<WhereUsedElement>(new Comparator<WhereUsedElement>() {

            @Override
            public int compare(WhereUsedElement o1, WhereUsedElement o2) {
                String path1 = o1.getFile() != null ? o1.getFile().getPath() : ""; //NOI18N
                String path2 = o2.getFile() != null ? o2.getFile().getPath() : ""; //NOI18N
                int retval = path1.compareTo(path2);
                if (retval == 0) {
                    int offset1 = o1.getPosition().getBegin().getOffset();
                    int offset2 = o2.getPosition().getBegin().getOffset();
                    retval = offset1 < offset2 ? -1 : 1;
                }
                return retval;
            }
        });

        Map<FileObject, WarningFileElement> warningElements = new HashMap<FileObject, WarningFileElement>();

        private Results() {
        }

        private void clear() {
            elements.clear();
            warningElements.clear();
        }

        private void addEntry(PhpElement decl) {
            Icon icon = UiUtils.getElementIcon(WhereUsedSupport.this.getElementKind(), decl.getModifiers());
            WhereUsedElement whereUsedElement = WhereUsedElement.create(decl.getName(), decl.getFileObject(), new OffsetRange(decl.getOffset(), decl.getOffset() + decl.getName().length()), icon);
            if (whereUsedElement != null) {
                elements.add(whereUsedElement);
            }
        }

        private void addEntry(FileObject fo, Occurence occurence) {
            Collection<? extends PhpElement> allDeclarations = occurence.getAllDeclarations();
            if (allDeclarations.size() > 0) {
                PhpElement decl = allDeclarations.iterator().next();
                Icon icon = UiUtils.getElementIcon(WhereUsedSupport.this.getElementKind(), decl.getModifiers());
                WhereUsedElement wue = WhereUsedElement.create(decl.getName(), fo, occurence.getOccurenceRange(), icon);
                if (wue != null) {
                    elements.add(wue);
                } else if(!warningElements.containsKey(fo)) {
                    warningElements.put(fo, new WarningFileElement(fo));
                }
            }
        }

        public Collection<WhereUsedElement> getResultElements() {
            return Collections.unmodifiableCollection(elements);
        }

        public Collection<WarningFileElement> getWarningElements() {
            return warningElements.values();
        }
    }
}
