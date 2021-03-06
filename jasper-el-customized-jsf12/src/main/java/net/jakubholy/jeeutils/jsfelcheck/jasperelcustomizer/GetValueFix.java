/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.jakubholy.jeeutils.jsfelcheck.jasperelcustomizer;

import org.apache.el.lang.EvaluationContext;
import org.apache.el.parser.Node;

import static org.apache.el.lang.ELSupport.coerceToBoolean;

/**
 * Helper class to be injected into jasper-el to re-implement some of its
 * logic so that all parts of an expression are always evaluated.
 */
public class GetValueFix {

    public static Boolean and(EvaluationContext ctx, Node parent) {
        assertCorrectChildrenCount(2, parent);
        // Explicitely evaluate each value before doing AND to make sure they are evaluated
        boolean v1 = coerceToBoolean(parent.jjtGetChild(0).getValue(ctx)).booleanValue();
        boolean v2 = coerceToBoolean(parent.jjtGetChild(1).getValue(ctx)).booleanValue();
        return Boolean.valueOf(v1 && v2);
    }

    public static Boolean or(EvaluationContext ctx, Node parent) {
        assertCorrectChildrenCount(2, parent);
        // Explicitely evaluate each value before doing AND to make sure they are evaluated
        boolean v1 = coerceToBoolean(parent.jjtGetChild(0).getValue(ctx)).booleanValue();
        boolean v2 = coerceToBoolean(parent.jjtGetChild(1).getValue(ctx)).booleanValue();
        return Boolean.valueOf(v1 || v2);
    }

    public static Object choice(EvaluationContext ctx, Node parent) {
        assertCorrectChildrenCount(3, parent);
        // Explicitely evaluate each value before doing AND to make sure they are evaluated
        boolean condition = coerceToBoolean(parent.jjtGetChild(0).getValue(ctx)).booleanValue();
        Object v1 = parent.jjtGetChild(1).getValue(ctx);
        Object v2 = parent.jjtGetChild(2).getValue(ctx);
        return condition ? v1 : v2;
    }

    private static void assertCorrectChildrenCount(int expected, Node parent) {
        if (expected != parent.jjtGetNumChildren()) {
            throw new IllegalStateException("Expected to find " + expected
                + " children on this node but found " + parent.jjtGetNumChildren()
                + ", node: " + parent.toString());
        }
    }

}
