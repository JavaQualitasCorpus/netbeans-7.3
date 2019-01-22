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

package org.netbeans.modules.java.hints.perf;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.spi.java.hints.ConstraintVariableType;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerPatterns;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.JavaFix;
import org.netbeans.spi.java.hints.MatcherUtilities;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.JavaFixUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
@Hint(displayName = "#DN_org.netbeans.modules.java.hints.perf.ManualArrayCopy", description = "#DESC_org.netbeans.modules.java.hints.perf.ManualArrayCopy", category="performance", suppressWarnings={"ManualArrayToCollectionCopy", "", "ManualArrayToCollectionCopy"})
public class ManualArrayCopy {


    @TriggerPatterns({
        @TriggerPattern(value="for (int $i = $s; $i < $len; $i++) {\n"+
                              "    $tarr[$i] = $arr[$i];\n" +
                              "}"),
        @TriggerPattern(value="for (int $i = $s; $i < $len; $i++) {\n"+
                              "    $tarr[$o1 + $i] = $arr[$i];\n"+
                              "}"),
        @TriggerPattern(value="for (int $i = $s; $i < $len; $i++) {\n"+
                              "    $tarr[$i + $o2] = $arr[$i];\n"+
                              "}")
    })
    public static ErrorDescription arrayCopy(HintContext ctx) {
        TypeMirror sourceType = ctx.getInfo().getTrees().getTypeMirror(ctx.getVariables().get("$arr"));
        TypeMirror targetType = ctx.getInfo().getTrees().getTypeMirror(ctx.getVariables().get("$tarr"));

        if (   !ctx.getInfo().getTypes().isSubtype(sourceType, targetType)
            || sourceType.getKind() == TypeKind.ERROR)
            return null;

        String startSource;
        String startTarget;
        String length;
        boolean isSZero = MatcherUtilities.matches(ctx, ctx.getVariables().get("$s"), "0");

        if (ctx.getVariables().containsKey("$o1")) {
            if (isSZero) {
                startSource = "0"; startTarget = "$o1"; length = "$len";
            } else {
                startSource = "$s"; startTarget = "$o1 + $s"; length = "$len - $s";
            }
        } else if (ctx.getVariables().containsKey("$o2")) {
            if (isSZero) {
                startSource = "0"; startTarget = "$o2"; length = "$len";
            } else {
                startSource = "$s"; startTarget = "$s + $o2"; length = "$len - $s";
            }
        } else {
            if (isSZero) {
                startSource = "0"; startTarget = "0"; length = "$len";
            } else {
                startSource = "$s"; startTarget = "$s"; length = "$len - $s";
            }
        }

        String fix = String.format("java.lang.System.arraycopy($arr, %s, $tarr, %s, %s);", startSource, startTarget, length);

        return compute(ctx, "manual-array-copy", fix);
    }

    @TriggerPatterns({
        @TriggerPattern(value="for (int $i = 0; $i < $arr.length; $i++) {\n"+
                              "    $coll.add($arr[$i]);\n"+
                              "}\n",
                        constraints={
                            @ConstraintVariableType(variable="$arr", type="java.lang.Object[]"),
                            @ConstraintVariableType(variable="$coll", type="java.util.Collection")
                        }),
        @TriggerPattern(value="for ($type $var : $arr) {\n"+
                              "    $coll.add($var);\n"+
                              "}\n",
                        constraints={
                            @ConstraintVariableType(variable="$arr", type="java.lang.Object[]"),
                            @ConstraintVariableType(variable="$coll", type="java.util.Collection")
                        })
    })
    public static ErrorDescription collection(HintContext ctx) {
        return compute(ctx, "manual-array-copy-coll", "$coll.addAll(java.util.Arrays.asList($arr));");
    }
    
    private static ErrorDescription compute(HintContext ctx, String key, String to) {
        String fixDisplayName = NbBundle.getMessage(ManualArrayCopy.class, "FIX_" + key);
        Fix fix = JavaFixUtilities.rewriteFix(ctx, fixDisplayName, ctx.getPath(), to);
        String displayName = NbBundle.getMessage(ManualArrayCopy.class, "ERR_" + key);

        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), displayName, fix);
    }

}