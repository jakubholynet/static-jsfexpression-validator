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

package net.jakubholy.jeeutils.jsfelcheck.validator;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.VariableResolver;

import net.jakubholy.jeeutils.jsfelcheck.validator.exception.VariableNotFoundException;

/**
 * Resolve variables from a pre-defined list initialized via {@link PredefinedVariableResolver#declareVariable(String, Object)}.
 * Throws {@link VariableNotFoundException} if it encounteres a variable not present on the list.
 */
public final class PredefinedVariableResolver extends VariableResolver {

    private boolean includeKnownVariablesInException = true;

    public static interface NewVariableEncounteredListener {
        public void handleNewVariableEncountered(String variableName);
    }

    private final Map<String, Object> knownVariables = new HashMap<String, Object>();
    private final PredefinedVariableResolver.NewVariableEncounteredListener newVariableEncounteredListener;
    private final ElVariableResolver unknownVariableResolver;

    public PredefinedVariableResolver(
            final PredefinedVariableResolver.NewVariableEncounteredListener newVariableEncounteredListener, ElVariableResolver unknownVariableResolver) {

        if (unknownVariableResolver == null) {
            throw new IllegalArgumentException("ElVariableResolver unknownVariableResolver may not be null");
        }

        this.newVariableEncounteredListener = newVariableEncounteredListener;
        this.unknownVariableResolver = unknownVariableResolver;
    }

    @Override
    public Object resolveVariable(FacesContext fc, String variableName)
            throws EvaluationException {

        if (newVariableEncounteredListener != null) {
            newVariableEncounteredListener.handleNewVariableEncountered(variableName);
        }

        if (knownVariables.containsKey(variableName)) {
            return knownVariables.get(variableName);
        }

        Class<?> contextLocalVarType = unknownVariableResolver.resolveVariable(variableName);
        if (contextLocalVarType != null) {
            return FakeValueFactory.fakeValueOfType(contextLocalVarType, variableName);
        }

        throw new VariableNotFoundException("No variable '" + variableName + "' among the predefined ones"
                + (isIncludeKnownVariablesInException()? ": " + knownVariables.keySet() : "."));
    }

    public void declareVariable(final String name, final Object value) {
        Object currentOverride = knownVariables.get(name);
        if (currentOverride != null) {
            throw new IllegalArgumentException("The variable '"
                    + name + "' is already defined; current value: " +
                    currentOverride + ", new: " + value);
        }
        knownVariables.put(name, value);
    }

    public void setIncludeKnownVariablesInException(
            boolean includeKnownVariablesInException) {
        this.includeKnownVariablesInException = includeKnownVariablesInException;
    }

    public boolean isIncludeKnownVariablesInException() {
        return includeKnownVariablesInException;
    }

}