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

package net.jakubholy.jeeutils.jsfelcheck.beanfinder;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Register all Spring Beans defined in Spring applicationContext file as JSF
 * Managed Beans.
 */
public class SpringContextBeanFinder implements ManagedBeanFinder {

    private final Collection<File> springContextFiles;

    public SpringContextBeanFinder(final Collection<File> springContextFiles) {
        if (springContextFiles == null || springContextFiles.isEmpty()) {
            throw new IllegalArgumentException("springContextFiles: Collection<File> cannot be null/empty, is: " + springContextFiles);
        }
        // TODO verify the files exist
        this.springContextFiles = new LinkedList<File>(springContextFiles);
    }

    @Override
    public Collection<ManagedBeanDescriptor> findDefinedBackingBeans() {
        Collection<ManagedBeanDescriptor> allBeans = new LinkedList<ManagedBeanFinder.ManagedBeanDescriptor>();

        BeanDefinitionRegistry knownBeans = new SimpleBeanDefinitionRegistry();
        BeanDefinitionReader beanParser = new XmlBeanDefinitionReader(knownBeans);

        beanParser.loadBeanDefinitions(toResources(springContextFiles));

        String[] beanNames = knownBeans.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            String beanClassName = knownBeans.getBeanDefinition(beanName).getBeanClassName();
            Class<?> beanClass = loadBeanClass(beanName, beanClassName);
            allBeans.add(new ManagedBeanDescriptor(beanName, beanClass));
        }

        return allBeans;
    }

    private Class<?> loadBeanClass(String beanName, String beanClassName) {
        try {
            return Class.forName(beanClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load class '"
            		+ beanClassName + "' for bean '" + beanName + "'");
        }
    }

    private Resource[] toResources(Collection<File> springContextFiles) {
        Resource[] locations = new Resource[springContextFiles.size()];

        int index = 0;
        for (File configFile : springContextFiles) {
            locations[index++] = new FileSystemResource(configFile);
        }

        return locations;
    }

}