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

package net.jakubholy.jeeutils.jsfelcheck.expressionfinder.impl.facelets

import org.junit.Test
import org.junit.Before
import net.jakubholy.jeeutils.jsfelcheck.webtest.jsf20.test.MyActionBean
import org.junit.Ignore


class ExperimentalFaceletsElFinderTest {

    private static final Set ALL_EL_EXPRESSIONS = new HashSet(allExpectedEls())

    def private static allExpectedEls() {
        def allMapKeys = MyActionBean
            .allMapKeys() - ["valueForCustomTag", "valueForComposite"]
        allMapKeys = allMapKeys.collect { "myActionBean.map.$it" }
        def beanPropsAndMethods = ["doAction"
                , "doActionListening", "doValueChangeListening"
                , "doValidating", "value", "books"
                , "converter", "validator", "actionsInvokedSummary"
                , "paramFromTemplatedPage", "paramFromIncludingPage"
                , "myTagAttribute.map.valueForCustomTag"
                , "cc.attrs.compositeAttributeValueBean.map.valueForComposite"
                , "derivedVar"
            ].collect { "myActionBean.$it" }
        def localVars = ["book.name"]
        def selfmapKeys = MyActionBean.allSelfmapKeys().collect { "myActionBean.selfmap.$it" }
        return allMapKeys + selfmapKeys + beanPropsAndMethods + localVars
    }

    private ExperimentalFaceletsElFinder finder;

    @Before
    public void setUp() {
        finder = new ExperimentalFaceletsElFinder(new File("src/main/webapp"), "tests/valid_el")

    }

    /**
     * Undetected so far:
     *
     * myActionBean.doValidating,
     * myActionBean.validator, myActionBean.converter,
     * myActionBean.myTagAttribute.map.valueForCustomTag,
     * myActionBean.paramFromTemplatedPage,
     * myActionBean.map.attributeValue,
     * myActionBean.cc.attrs.compositeAttributeValueBean.map.valueForComposite,
     * book.name,
     * myActionBean.map.varFromIncludingPage,
     * myActionBean.map.palValue, myActionBean.map.palTarget
     * myActionBean.actionsInvokedSummary,
     * myActionBean.derivedVar,
     * myActionBean.doActionListening,
     * myActionBean.paramFromIncludingPage,
     * myActionBean.doValueChangeListening
     * ??? myActionBean.selfmap.*
     */
    @Ignore("To be used only during development")
    @Test
    public void compile_to_component_tree_and_walk_it() throws Exception {
        def elsFound = [] //+ finder.verifyExpressionsViaComponentTree("/faceletsParsingFullTest.xhtml") +
            //finder.verifyExpressionsViaComponentTree("/templateTest/pageWithTemplate.xhtml")
        // Can't get Composites working - UnsupportedOperationException in
        // JspViewDeclarationLanguageBase.getComponentMetadata
        elsFound.addAll finder.verifyExpressionsViaComponentTree("/customTagTest/pageWithCustomTagAndComposite.xhtml")

        println "elsFound: $elsFound"
        def undetected = ALL_EL_EXPRESSIONS - elsFound
        assert undetected.isEmpty()
        // Undetected = myActionBean.map.palValue, myActionBean.map.attributeValue, myActionBean.map.palTarget
        assert elsFound == ALL_EL_EXPRESSIONS
    }

    @Ignore("To be used only during development")
    @Test
    public void compile_and_listen_for_compiler_events() throws Exception {
        def elsFound = finder.verifyExpressionsViaCompiler("/faceletsParsingFullTest.xhtml")
        elsFound.addAll(finder.verifyExpressionsViaCompiler("/templateTest/pageWithTemplate.xhtml"))
        elsFound.addAll(finder.verifyExpressionsViaCompiler("/customTagTest/pageWithCustomTagAndComposite.xhtml"))
        assert elsFound == ALL_EL_EXPRESSIONS
    }
}
