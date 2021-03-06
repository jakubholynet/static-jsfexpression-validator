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

package net.jakubholy.jeeutils.jsfelcheck.config;

import net.jakubholy.jeeutils.jsfelcheck.expressionfinder.variables.ContextVariableRegistry;
import net.jakubholy.jeeutils.jsfelcheck.expressionfinder.variables.DataTableVariableResolver;
import net.jakubholy.jeeutils.jsfelcheck.expressionfinder.variables.TagJsfVariableResolver;

/**
 * Declaration of local variables in JSF pages and optionally
 * resolvers for tags that can produce local variables.
 * <p>
 *     A "local variable" is a variable produced by a tag, such as <code>h:dataTable</code>'s <code>var</code>.
 *     It is often impossible to guess their type because they contain elements from collections or maps that
 *     are untyped in Java (at least in the runtime).
 *     Therefore you need to declare of what type they are so that it can be checked that EL expressions
 *     containing them are correct.
 * </p>
 * <p>
 *     You will typically want to static import the static methods to make configurations more readable.
 * </p>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * // JSP:
 * <h:dataTable value="#{myCollectionBean.list}" var="myCollItem">
 *     <h:column>
 *         <h:outputText value="Item value: #{myCollItem.value}" />
 *     </h:column>
 * </h:dataTable>
 *
 * // JAVA:
 * import static net.jakubholy.jeeutils.jsfelcheck.config.LocalVariableConfiguration.*;
 *
 * declareLocalVariable("myCollectionBean.list", MyListElementType.class)
 *      .withLocalVariable("myBean.someArray", String.class)
 * }</pre>
 */
public class LocalVariableConfiguration {

    private final DataTableVariableResolver dataTableVariableResolver;
    private final ContextVariableRegistry contextVariableRegistry = new ContextVariableRegistry();;

    /**
     * Create a new configuration and immediately also declare the type of a local variable identified by the given
     * <code>sourceElExpression</code> - see {@link #withLocalVariable(String, Class)} for details.
     */
    public static LocalVariableConfiguration declareLocalVariable(String sourceElExpression, Class<?> type) {
        return new LocalVariableConfiguration().withLocalVariable(sourceElExpression, type);
    }

    /**
     * Create empty configuration; prefer to use {@link #declareLocalVariable(String, Class)}.
     */
    public LocalVariableConfiguration() {
        this(new DataTableVariableResolver());
    }

    /** *For testing* */
    public LocalVariableConfiguration(DataTableVariableResolver dataTableVariableResolver) {
        this.dataTableVariableResolver = dataTableVariableResolver;
    }

    /**
     * Declare the type of a local variable identified by the given <code>sourceElExpression</code>
     * (for <code>h:dataTable</code>-like tags this is the EL expression that produces the collection whose elements are used
     * consecutively as values of this variable)
     * @param sourceElExpression (required) dataTable-like tags: source EL expression producing a collection
     * whose elements will become values of the local variable defined by dataTable
     * @param type (required) the type of the local variable
     * @return this
     */
    public LocalVariableConfiguration withLocalVariable(String sourceElExpression, Class<?> type) {
        // TODO Store local variables outside of dataTable resolver so that also custom resolvers can read them
	    dataTableVariableResolver.declareTypeFor(sourceElExpression, type);
        return this;
    }

    /**
     * Declare the type of a local variable identified by the given <code>sourceElExpression</code> - see
     * {@link #withLocalVariable(String, Class)} for details.
     */
    public LocalVariableConfiguration and(String sourceElExpression, Class<?> type) {
        return withLocalVariable(sourceElExpression, type);
    }

    /**
     * Register the default {@link DataTableVariableResolver} for another yet compatible JSF tag,
     * namely a tag that declares its local variable in the <code>var</code> attribute and takes values for it
     * from a <code>value</code> attribute. Mostly useful for use with JSF libraries that have their own extensions of
     * the default <code>h:dataTable</code> compatible with how it works.
     * <p>
     *     Also necessary if you use different than the default prefix of 'h:'.
     * </p>
     *
     * @param tagQName (required)  fully qualified name of the JSF tag that can define new local variables,
     * e.g. <code>t:dataTable</code> for MyFaces.
     *
     * @see #withResolverForVariableProducingTag(String, net.jakubholy.jeeutils.jsfelcheck.expressionfinder.variables.TagJsfVariableResolver)
     */
    public LocalVariableConfiguration withCustomDataTableTagAlias(String tagQName) {
        assertQNameValid(tagQName);
        contextVariableRegistry.registerResolverForTag(tagQName, dataTableVariableResolver);
        return this;
    }

    /**
     * Register a resolver that can extract local variables defined in a JSF tag.
     * This is also used internally to register the {@link DataTableVariableResolver} for the tag <code>h:dataTable</code>.
     *
     * @param tagQName (required) fully qualified name of the JSF tag that can define new local variables; ex: <code>ui:repeat</code>
     * @param customResolver (required) the resolver that can find out what local variable the tag declares
     * @return this
     */
    public LocalVariableConfiguration withResolverForVariableProducingTag(String tagQName, TagJsfVariableResolver customResolver) {
        assertQNameValid(tagQName);
        if (customResolver == null) {
            throw new IllegalArgumentException("customResolver: TagJsfVariableResolver must not be null");
        }
        contextVariableRegistry.registerResolverForTag(tagQName, customResolver);
        return this;
    }

    private void assertQNameValid(String tagQName) {
        if (tagQName == null || tagQName.trim().length() == 0) {
            throw new IllegalArgumentException("tagQName: String must be fully qualified JSF tag name " +
                    "such as 'h:dataTable'");
        }
    }

    /** *For internal use only* */
    public ContextVariableRegistry toRegistry() {
        contextVariableRegistry.registerResolverForTag("h:dataTable", dataTableVariableResolver);
        return contextVariableRegistry;
    }
}
