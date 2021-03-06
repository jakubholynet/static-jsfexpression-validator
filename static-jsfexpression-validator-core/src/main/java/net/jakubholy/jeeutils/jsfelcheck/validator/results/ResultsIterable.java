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

package net.jakubholy.jeeutils.jsfelcheck.validator.results;

import java.util.Collection;
import java.util.Iterator;

/**
 * A collection or EL validation results.
 *
 * @param <T> The type of the results
 */
public class ResultsIterable<T> implements Iterable<T> {

    private final Collection<T> results;

    ResultsIterable(Collection<T> results) {
        this.results = results;
    }

    /** {@inheritDoc} */
    public Iterator<T> iterator() {
        return results.iterator();
    }

    /**
     * Number of results in this iterable.
     * @return >= 0
     */
    public int size() {
        return results.size();
    }

    @Override
    public String toString() {
        StringBuilder resultList = new StringBuilder("RESULTS [").append(size()).append("]:\n");
        for (T result : results) {
            resultList.append(result).append('\n');
        }
        return resultList.toString();
    }

}