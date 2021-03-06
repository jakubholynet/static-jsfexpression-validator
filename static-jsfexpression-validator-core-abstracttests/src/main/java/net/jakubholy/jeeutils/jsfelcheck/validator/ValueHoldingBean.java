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

/** Dummy class for creating Java Beans to be used in EL expressions. */
public class ValueHoldingBean {
    private Boolean value;
    private String[] stringArray;
    private int[] intArray;
    private ValueHoldingBean[] beanArray;
    private ValueHoldingBean beanProperty;

    /** Empty holder. */
    public ValueHoldingBean() { }

    /** Holder for the given boolean.
     * @param beanValue (required)
     */
    public ValueHoldingBean(final Boolean beanValue) {
        this.value = beanValue;
    }

    public void setValue(final Boolean beanValue) {
        this.value = beanValue;
    }

    public Boolean getValue() {
        return value;
    }

    public void setStringArray(String[] strngArray) {
        this.stringArray = strngArray;
    }

    public String[] getStringArray() {
        return stringArray;
    }

    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    public int[] getIntArray() {
        return intArray;
    }

    public void setBeanArray(ValueHoldingBean[] beanArray) {
        this.beanArray = beanArray;
    }

    public ValueHoldingBean[] getBeanArray() {
        return beanArray;
    }

    public void setBeanProperty(ValueHoldingBean beanProperty) {
        this.beanProperty = beanProperty;
    }

    public ValueHoldingBean getBeanProperty() {
        return beanProperty;
    }


}