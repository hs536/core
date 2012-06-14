/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.tests.unit.deployment.structure.resolution;

import static org.jboss.weld.manager.Enabled.EMPTY_ENABLED;

import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.jboss.weld.Container;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bean.attributes.BeanAttributesFactory;
import org.jboss.weld.bootstrap.BeanDeployerEnvironment;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.ejb.EjbDescriptors;
import org.jboss.weld.event.GlobalObserverNotifierService;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.metadata.TypeStore;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.resources.SharedObjectCache;
import org.jboss.weld.serialization.ContextualStoreImpl;
import org.jboss.weld.serialization.spi.ContextualStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AccessibleManagerResolutionTest {

    private ClassTransformer classTransformer;
    private ServiceRegistry services;

    @BeforeMethod
    public void beforeMethod() {
        this.classTransformer = new ClassTransformer(new TypeStore(), new SharedObjectCache());
        this.services = new SimpleServiceRegistry();
        this.services.add(MetaAnnotationStore.class, new MetaAnnotationStore(classTransformer));
        this.services.add(ContextualStore.class, new ContextualStoreImpl());
        this.services.add(ClassTransformer.class, classTransformer);
        this.services.add(SharedObjectCache.class, new SharedObjectCache());
        this.services.add(GlobalObserverNotifierService.class, new GlobalObserverNotifierService(services));
        this.services.add(InjectionTargetService.class, new InjectionTargetService(BeanManagerImpl.newRootManager("foo", services, EMPTY_ENABLED)));
    }

    private <T> void addBean(BeanManagerImpl manager, Class<T> c) {
        EnhancedAnnotatedType<T> clazz = classTransformer.getEnhancedAnnotatedType(c);
        RIBean<?> bean = ManagedBean.of(BeanAttributesFactory.forBean(clazz, manager), clazz, manager);
        manager.addBean(bean);
        manager.getBeanResolver().clear();
        BeanDeployerEnvironment environment = BeanDeployerEnvironment.newEnvironment(new EjbDescriptors(), manager);
        bean.initialize(environment);
    }

    @Test
    public void testAccessibleDynamicallySingleLevel() {
        BeanManagerImpl root = BeanManagerImpl.newRootManager("root", services, EMPTY_ENABLED);
        Container.initialize(root, services);
        BeanManagerImpl child = BeanManagerImpl.newRootManager("child", services, EMPTY_ENABLED);
        addBean(root, Cow.class);
        Assert.assertEquals(1, root.getBeans(Cow.class).size());
        Assert.assertEquals(0, child.getBeans(Cow.class).size());
        child.addAccessibleBeanManager(root);
        Set<Bean<?>> beans = child.getBeans(Cow.class);
        Assert.assertEquals(1, child.getBeans(Cow.class).size());
        addBean(child, Chicken.class);
        Assert.assertEquals(1, child.getBeans(Chicken.class).size());
        Assert.assertEquals(0, root.getBeans(Chicken.class).size());
    }

    @Test
    public void testAccessibleThreeLevelsWithMultiple() {
        BeanManagerImpl root = BeanManagerImpl.newRootManager("root", services, EMPTY_ENABLED);
        Container.initialize(root, services);
        BeanManagerImpl child = BeanManagerImpl.newRootManager("child", services, EMPTY_ENABLED);
        BeanManagerImpl child1 = BeanManagerImpl.newRootManager("child1", services, EMPTY_ENABLED);
        BeanManagerImpl grandchild = BeanManagerImpl.newRootManager("grandchild", services, EMPTY_ENABLED);
        BeanManagerImpl greatGrandchild = BeanManagerImpl.newRootManager("greatGrandchild", services, EMPTY_ENABLED);
        child.addAccessibleBeanManager(root);
        grandchild.addAccessibleBeanManager(child1);
        grandchild.addAccessibleBeanManager(child);
        addBean(greatGrandchild, Cat.class);
        greatGrandchild.addAccessibleBeanManager(grandchild);
        addBean(root, Cow.class);
        addBean(child, Chicken.class);
        addBean(grandchild, Pig.class);
        addBean(child1, Horse.class);
        Assert.assertEquals(0, root.getBeans(Pig.class).size());
        Assert.assertEquals(0, root.getBeans(Chicken.class).size());
        Assert.assertEquals(1, root.getBeans(Cow.class).size());
        Assert.assertEquals(0, root.getBeans(Horse.class).size());
        Assert.assertEquals(0, root.getBeans(Cat.class).size());
        Assert.assertEquals(0, child.getBeans(Pig.class).size());
        Assert.assertEquals(1, child.getBeans(Chicken.class).size());
        Assert.assertEquals(1, child.getBeans(Cow.class).size());
        Assert.assertEquals(0, child.getBeans(Horse.class).size());
        Assert.assertEquals(0, child.getBeans(Cat.class).size());
        Assert.assertEquals(0, child1.getBeans(Cow.class).size());
        Assert.assertEquals(1, child1.getBeans(Horse.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Pig.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Chicken.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Cow.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Horse.class).size());
        Assert.assertEquals(0, grandchild.getBeans(Cat.class).size());
        Assert.assertEquals(1, greatGrandchild.getBeans(Pig.class).size());
        Assert.assertEquals(1, greatGrandchild.getBeans(Chicken.class).size());
        Assert.assertEquals(1, greatGrandchild.getBeans(Cow.class).size());
        Assert.assertEquals(1, greatGrandchild.getBeans(Horse.class).size());
        Assert.assertEquals(1, greatGrandchild.getBeans(Cat.class).size());
    }

    @Test
    public void testSameManagerAddedTwice() {
        BeanManagerImpl root = BeanManagerImpl.newRootManager("root", services, EMPTY_ENABLED);
        Container.initialize(root, services);
        BeanManagerImpl child = BeanManagerImpl.newRootManager("child", services, EMPTY_ENABLED);
        BeanManagerImpl grandchild = BeanManagerImpl.newRootManager("grandchild", services, EMPTY_ENABLED);
        grandchild.addAccessibleBeanManager(child);
        child.addAccessibleBeanManager(root);
        grandchild.addAccessibleBeanManager(root);
        addBean(root, Cow.class);
        addBean(child, Chicken.class);
        addBean(grandchild, Pig.class);
        Assert.assertEquals(0, root.getBeans(Pig.class).size());
        Assert.assertEquals(0, root.getBeans(Chicken.class).size());
        Assert.assertEquals(1, root.getBeans(Cow.class).size());
        Assert.assertEquals(0, child.getBeans(Pig.class).size());
        Assert.assertEquals(1, child.getBeans(Chicken.class).size());
        Assert.assertEquals(1, child.getBeans(Cow.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Pig.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Chicken.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Cow.class).size());
    }

    @Test
    public void testCircular() {
        BeanManagerImpl root = BeanManagerImpl.newRootManager("root", services, EMPTY_ENABLED);
        Container.initialize(root, services);
        BeanManagerImpl child = BeanManagerImpl.newRootManager("child", services, EMPTY_ENABLED);
        BeanManagerImpl grandchild = BeanManagerImpl.newRootManager("grandchild", services, EMPTY_ENABLED);
        grandchild.addAccessibleBeanManager(child);
        child.addAccessibleBeanManager(root);
        grandchild.addAccessibleBeanManager(root);
        root.addAccessibleBeanManager(grandchild);
        addBean(root, Cow.class);
        addBean(child, Chicken.class);
        addBean(grandchild, Pig.class);
        Assert.assertEquals(1, root.getBeans(Pig.class).size());
        Assert.assertEquals(1, root.getBeans(Chicken.class).size());
        Assert.assertEquals(1, root.getBeans(Cow.class).size());
        Assert.assertEquals(1, child.getBeans(Pig.class).size());
        Assert.assertEquals(1, child.getBeans(Chicken.class).size());
        Assert.assertEquals(1, child.getBeans(Cow.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Pig.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Chicken.class).size());
        Assert.assertEquals(1, grandchild.getBeans(Cow.class).size());
    }

}
