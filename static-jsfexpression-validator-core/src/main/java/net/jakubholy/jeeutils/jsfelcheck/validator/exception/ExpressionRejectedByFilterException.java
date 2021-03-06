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

package net.jakubholy.jeeutils.jsfelcheck.validator.exception;

import net.jakubholy.jeeutils.jsfelcheck.validator.ElExpressionFilter;

/**
 * Used to indicate an EL expression that hasn't been checked because a filter
 * has rejected it.
 *
 * @see ElExpressionFilter
 */
@SuppressWarnings("serial")
public class ExpressionRejectedByFilterException extends BaseEvaluationException {

    private final String expression;
    private final ElExpressionFilter filter;

    /**
     * New.
     * @param expression (required) the EL expression
     * @param filter (required) the filter that rejected it
     */
    public ExpressionRejectedByFilterException(String expression, ElExpressionFilter filter) {
        super(expression + " rejected by " + filter);
        this.expression = expression;
        this.filter = filter;
    }

    public String getExpression() {
        return expression;
    }

    public ElExpressionFilter getFilter() {
        return filter;
    }

}