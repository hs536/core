/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.tests.specialization.weld1651;

import static junit.framework.Assert.assertNotNull;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.BeanArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Matus Abaffy
 */
@RunWith(Arquillian.class)
public class AdvancedGenericBeanSpecializationTest {

    @Inject
    SpecializingGenericBean<?, ?> bean;

    @Deployment
    public static Archive<?> createArchive() {
        return ShrinkWrap.create(BeanArchive.class).addClasses(AdvancedGenericBeanSpecializationTest.class, SpecializedGenericBean.class,
                SpecializingGenericBean.class, Bar.class, Foo.class);
    }

    @Test
    public void testGenericBeanSpecialization(){
        assertNotNull(bean);
    }
}