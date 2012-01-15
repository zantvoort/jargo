/*
 * GNU Lesser General Public License
 *
 * Jargo - JSE Container Toolkit.
 * Copyright (C) 2006  Leon van Zantvoort
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 * 
 * Leon van Zantvoort
 * 243 Acalanes Drive #11
 * Sunnyvale, CA 94086
 * USA
 *
 * zantvoort@users.sourceforge.net
 * http://jargo.org
 */
package org.jargo.container;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import org.jargo.ComponentAlias;
import org.jargo.ComponentApplicationContext;
import org.jargo.ComponentApplicationException;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentFactory;
import org.jargo.ComponentNotFoundException;
import org.jargo.ComponentReference;
import org.jargo.ComponentUnit;
import org.jargo.deploy.Deployer;
import org.jargo.deploy.Deployable;

/**
 * @author Leon van Zantvoort
 */
public final class Container extends ComponentApplicationContext {
    
    private final Providers providers;
    private final Deployer rootDeployer;
    private final ComponentRegistry registry;
    
    private final Logger logger;
    private final AtomicBoolean init;
    
    private final List<Deployable> deployables = new ArrayList<Deployable>();

    // Doesn't need to be final, as only one instance exists.
    private final ThreadLocal<LinkedList<ComponentReference<?>>> callStack = new ThreadLocal<LinkedList<ComponentReference<?>>>() {
        @Override
        protected LinkedList<ComponentReference<?>> initialValue() {
            return new LinkedList<ComponentReference<?>>();
        }
    };

    private final ContainerLocalURLRegistration registration = new ContainerLocalURLRegistration();

    public Container() {
        this.providers = new ProvidersImpl();
        this.rootDeployer = new RootDeployer();
        this.registry = new ComponentRegistryImpl(providers);
        this.logger = Logger.getLogger(getClass().getName());
        this.init = new AtomicBoolean(false);

        if (!VanillaProxyGenerator.isCGLibSupported()) {
            logger.info("CGLib is not available. Vanilla proxies are not supported.");
        }
        
        Lock lock = new ReentrantLock(true);
        try {
            deploy(providers);
            deploy(new URLDeployer(registry));
            deploy(new ComponentConfigurationDeployer(registry, lock));
            deploy(new ComponentAliasDeployer(registry, lock));
            deploy(new ClassLoaderProviders());
            deploy(new DependencyInspectorProviders());
            deploy(new EventExecutorProviders());
            deploy(new EventFactoryProviders());
            deploy(new EventInterceptorFactoryProviders());
            deploy(new InjectionFactoryProviders());
            deploy(new InvocationFactoryProviders());
            deploy(new InvocationInterceptorFactoryProviders());
            deploy(new ComponentConfigurationProviders());
            deploy(new ComponentAliasProviders());
            deploy(new ComponentExceptionHandlerProviders());
            deploy(new ComponentObjectFactoryProviders());
            deploy(new ComponentLifecycleProviders());
            deploy(new ComponentReferenceLifecycleProviders());
            deploy(new ExecutorHandleProviders());
            deploy(new LifecycleProviders());
            deploy(new MetaDataProviders());
            deploy(new ObjectFactoryProviders());
        } catch (Exception e) {
            AssertionError ae = new AssertionError();
            ae.initCause(e);
            throw ae;
        }
    }
    
    /**
     * Subclasses can override this method to have control over which instance
     * is to be returned by {@link #instance}.
     */
    protected ComponentApplicationContext resolveInstance() {
        if (!init.getAndSet(true)) {
            try {
                deployables.addAll(getDeployables(registration.getClassLoader()));
                for (Deployable deployable : deployables) {
                    logger.finest("Deploying " + deployable.getClass() + ".");
                    deploy(deployable);
                }
                deploy(registration);
            } catch (ComponentApplicationException e) {
                throw e;
            } catch (Exception e) {
                throw new ComponentApplicationException(e);
            }
        }
        return this;
    }

    @SuppressWarnings("finally")
    public void shutdownDelegate() {
        try {
            try {
                try {
                    try {
                        undeploy(registration);
                    } finally {
                        Collections.reverse(deployables);
                        for (Iterator<Deployable> i = deployables.iterator(); i.hasNext();) {
                            Deployable deployable = i.next();
                            logger.finest("Undeploying " + deployable.getClass() + ".");
                            try {
                                undeploy(deployable);
                            } finally {
                                i.remove();
                                continue;
                            }
                        }
                    }
                } finally {
                    registry.shutdown();
                }
            } catch (ComponentApplicationException e) {
                throw e;
            } catch (Exception e) {
                throw new ComponentApplicationException(e);
            } finally {
                deployables.clear();
            }
        } finally {
            JargoThreadFactory.clear();
        }
    }

    static LinkedList<ComponentReference<?>> getCallStack() {
        Container c = (Container) ComponentApplicationContext.instance();
        return c.callStack.get();
    }

    static void attach(ComponentReference<?> reference) {
        assert reference != null;
        getCallStack().addFirst(reference);
    }

    static void detach() {
        if (getCallStack().removeFirst() == null) {
            assert false;
        }
    }

    public List<ComponentReference<?>> referenceStack() {
        return Collections.unmodifiableList(getCallStack());
    }
    
    public void setParent(Deployer parent) {
        // Do nothing.
    }
    
    public ComponentFactory<?> getComponentFactory(String componentName) throws
            ComponentNotFoundException {
        return registry.lookup(componentName);
    }
    
    public boolean exists(String componentName) {
        return registry.exists(componentName, true);
    }

    public List<ComponentFactory<?>> getComponentFactories() {
        return registry.list();
    }
    
    public <T> List<ComponentFactory<? extends T>> getComponentFactoriesForType(Class<T> type) {
        return registry.list(type);
    }
    
    public void deploy(Deployable deployable) throws Exception {
        rootDeployer.deploy(deployable);
    }
    
    public void undeploy(Deployable deployable) throws Exception {
        rootDeployer.undeploy(deployable);
    }
    
    private List<Deployable> getDeployables(final ClassLoader loader) throws Exception {
        LinkedList<Deployable> deployables = new LinkedList<Deployable>();
        String path = "META-INF/services/" + Deployable.class.getName();
        Enumeration<URL> urls = getClass().getClassLoader().
                getResources(path);
        Set<String> dupes = new HashSet<String>();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            try {
                String className = null;
                while ((className = reader.readLine()) != null) {
                    final String name = className.trim();
                    if (name.length() != 0 && 
                            !name.startsWith("#") && 
                            !name.startsWith(";") && 
                            !name.startsWith("//")) {
                        try {
                            if (!dupes.add(name)) {
                                logger.warning("Duplicate listing of deployable " +
                                        "class: '" + name + "'. " +
                                        "Discarding second listing.");
                                continue;
                            }
                            Constructor constructor = AccessController.
                                    doPrivileged(new PrivilegedExceptionAction<Constructor>() {
                                public Constructor run() throws Exception {
                                    Class<?> cls = Class.forName(name, true, loader);
                                    int m = cls.getModifiers();
                                    if (Deployable.class.isAssignableFrom(cls) &&
                                            !Modifier.isAbstract(m) &&
                                            !Modifier.isInterface(m)) {
                                        // PERMISSION: java.lang.RuntimePermission accessDeclaredMembers
                                        Constructor constructor = cls.getDeclaredConstructor();
                                        if (!Modifier.isPublic(constructor.getModifiers())) {
                                            // PERMISSION: java.lang.reflect.ReflectPermission suppressAccessChecks
                                            constructor.setAccessible(true);
                                        }
                                        return constructor;
                                    } else {
                                        throw new ClassCastException(cls.getName());
                                    }
                                }
                            });
                            Deployable deployable = (Deployable) constructor.newInstance();
                            if (Deployer.class.isInstance(deployable)) {
                                deployables.addFirst(deployable);
                            } else {
                                deployables.addLast(deployable);
                            }
                        } catch (PrivilegedActionException e) {
                            throw e.getException();
                        }
                    }
                }
            } finally {
                reader.close();
            }
        }
        return deployables;
    }
}
