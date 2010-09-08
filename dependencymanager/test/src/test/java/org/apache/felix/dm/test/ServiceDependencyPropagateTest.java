/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Component;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Validates ServiceDependency service properties propagation.
 */
@RunWith(JUnit4TestRunner.class)
public class ServiceDependencyPropagateTest extends Base {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    

    /**
     * Checks that a ServiceDependency propagates the dependency service properties to the provided service properties.
     */
    @Test
    public void testServiceDependencyPropagate(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Component c1 = m.createComponent()
                      .setImplementation(new C1(e))
                      .add(m.createServiceDependency().setService(C2.class).setRequired(true).setCallbacks("bind", null));

        Component c2 = m.createComponent()
                      .setInterface(C2.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
                      .setImplementation(new C2())
                      .add(m.createServiceDependency().setService(C3.class).setRequired(true).setPropagate(true));

        Component c3 = m.createComponent()
                      .setInterface(C3.class.getName(), new Hashtable() {{ put("foo2", "bar2"); }})
                      .setImplementation(new C3());
        
        m.add(c1);
        m.add(c2);
        m.add(c3);

        e.waitForStep(3, 10000);
    }
    
    /**
     * Checks that a ServiceDependency propagates the dependency service properties to the provided service properties,
     * using a callback method.
     */
    @Test
    public void testServiceDependencyPropagateCallback(BundleContext context) {
        DependencyManager m = new DependencyManager(context);
        // helper class that ensures certain steps get executed in sequence
        Ensure e = new Ensure();
        Component c1 = m.createComponent()
                      .setImplementation(new C1(e))
                      .add(m.createServiceDependency().setService(C2.class).setRequired(true).setCallbacks("bind", null));

        C2 c2Impl = new C2();
        Component c2 = m.createComponent()
                      .setInterface(C2.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
                      .setImplementation(c2Impl)
                      .add(m.createServiceDependency().setService(C3.class).setRequired(true).setPropagate(c2Impl, "getServiceProperties"));
        
        Component c3 = m.createComponent()
                      .setInterface(C3.class.getName(), null)
                      .setImplementation(new C3());
        
        m.add(c1);
        m.add(c2);
        m.add(c3);

        e.waitForStep(3, 10000);
    }
    
    public static class C1 {
        private Map m_props;
        private Ensure m_ensure;
        
        C1(Ensure ensure) {
            m_ensure = ensure;
        }

        void bind(Map props, C2 c2) {
            m_props = props;
        }
        
        void start() {
            m_ensure.step(1);
            if ("bar".equals(m_props.get("foo"))) {
                m_ensure.step(2);
            }
            if ("bar2".equals(m_props.get("foo2"))) {
                m_ensure.step(3);
            }
        }
    }
    
    public static class C2 {
      C3 m_c3;
      
      public Dictionary getServiceProperties(ServiceReference ref) {
          return new Hashtable() {{ put("foo2", "bar2"); }};
      }
    }
    
    public static class C3 {
    }
}
