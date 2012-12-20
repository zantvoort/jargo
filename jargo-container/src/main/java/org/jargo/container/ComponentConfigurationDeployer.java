/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
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

import org.jargo.*;
import org.jargo.deploy.Deployable;
import org.jargo.deploy.Deployer;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;

/**
 * @author Leon van Zantvoort
 */
final class ComponentConfigurationDeployer implements Deployer {
    
    private final Logger logger;
    private final ComponentRegistry registry;
    private final Lock lock;
    private final Providers providers;
    private final Map<String, Set<String>> reverseDependencyMap;
    
    public ComponentConfigurationDeployer(ComponentRegistry registry, Lock lock) {
        this.logger = Logger.getLogger(getClass().getName());
        this.registry = registry;
        this.lock = lock;
        this.providers = registry.getProviders();
        this.reverseDependencyMap = new HashMap<String, Set<String>>();
    }
    
    public void setParent(Deployer parent) {
    }
    
    @SuppressWarnings("finally")
    public void deploy(Deployable deployable) throws Exception {
        if (deployable instanceof ComponentRegistration) {
            List<ComponentConfiguration<?>> configurations =
                    new ArrayList<ComponentConfiguration<?>>(
                    ((ComponentRegistration) deployable).
                    getComponentConfigurations());
            
            final List<ManagedComponentContext<Object>> createdCtx =
                    Collections.synchronizedList(
                    new ArrayList<ManagedComponentContext<Object>>());
            boolean commit = false;
            lock.lock();
            try {
                for (Iterator<ComponentConfiguration<?>> i = configurations.iterator();
                        i.hasNext();) {
                    ComponentConfiguration configuration = i.next();
                    String name = configuration.getComponentName();
                    if (name == null) {
                        throw new ComponentApplicationException("Null " +
                                "component name not supported.");
                    }
                    if (name.equals("")) {
                        throw new ComponentApplicationException("Zero-length " +
                                "component name not supported.");
                    }
                    if (registry.exists(name, false)) {
                        // A component has already been registered with the
                        // same name.
                        ComponentConfiguration org = registry.
                                getComponentConfiguration(name);
                        if (configuration.equals(org)) {
                            // Remove duplicate registration.
                            i.remove();
                        } else {
                            // Another component with the same name is already exists.
                            throw new ComponentException(name,
                                    "Component name already exists.");
                        }
                    }
                    registry.create(configuration);
                }
                
                Map<String, Set<ManagedComponentContext<?>>> map =
                        getOrderedContextList(configurations, true);
                List<Callable<Object>> callables = new ArrayList<Callable<Object>>();
                for (final Set<ManagedComponentContext<?>> set : map.values()) {
                    Callable<Object> callable = new Callable<Object>() {
                        public Object call() throws Exception {
                            for (ManagedComponentContext tmp : set) {
                                @SuppressWarnings("unchecked")
                                ManagedComponentContext<Object> ctx = 
                                        (ManagedComponentContext<Object>) tmp;
                                String componentName = ctx.getComponentMetaData().getComponentName();
                                logger.info("Deploying component '" + componentName + "'.");

                                // Construct static components and perform dependency injection.
                                registry.activate(ctx);
                            }

                            for (ManagedComponentContext tmp : set) {
                                @SuppressWarnings("unchecked")
                                ManagedComponentContext<Object> ctx =
                                        (ManagedComponentContext<Object>) tmp;
                                String componentName = ctx.getComponentMetaData().getComponentName();
                                @SuppressWarnings("unchecked")
                                ComponentConfiguration<Object> configuration =
                                        (ComponentConfiguration)
                                        registry.getComponentConfiguration(componentName);
                                
                                List<ComponentLifecycle<Object>> lifecycles =
                                        registry.getComponentLifecycles(configuration);
                                
                                // List of lifecycles that have run successfully.
                                List<ComponentLifecycle<Object>> commitLifecycles =
                                        new ArrayList<ComponentLifecycle<Object>>();
                                try {
                                    for (ComponentLifecycle<Object> lifecycle : lifecycles) {
                                        // Execute lifecycle methods at component level.
                                        lifecycle.onCreate(new ComponentFactoryImpl<Object>(
                                                configuration, registry));
                                        commitLifecycles.add(lifecycle);
                                    }
                                    
                                    // Only add ctx if all lifecycles have been called
                                    // successfully.
                                    createdCtx.add(ctx);
                                } finally {
                                    if (commitLifecycles.size() != lifecycles.size()) {
                                        // Only rollback lifecycle calls that have been called.
                                        for (ListIterator<ComponentLifecycle<Object>> i =  commitLifecycles.
                                                listIterator(commitLifecycles.size()); i.hasPrevious();) {
                                            try {
                                                ComponentLifecycle<Object> lifecycle = i.previous();
                                                lifecycle.onDestroy(
                                                        new ComponentFactoryImpl<Object>(
                                                        configuration, registry));
                                            } finally {
                                                continue;
                                            }
                                        }
                                    }
                                }
                            }
                            return null;
                        }
                    };
                    callables.add(callable);
                }
                
                int threads = Math.min(callables.size(), AccessController.doPrivileged(
                        new PrivilegedAction<Integer>() {
                    public Integer run() {
                        // PERMISSION: java.util.PropertyPermission org.jargo.threads read
                        return Integer.parseInt(System.getProperty("org.jargo.threads", 
                                String.valueOf(Runtime.getRuntime().availableProcessors())));
                    }
                }));
                if (threads <= 1) {
                    for (Callable callable : callables) {
                        callable.call();
                    }
                } else if (!callables.isEmpty()) {
                    logger.info("The deployment of " + configurations.size() +
                            " components is parallelized by " + threads + 
                            " concurrently running threads.");
                    if (logger.isLoggable(FINEST)) {
                        int i = 0;
                        for (Set<ManagedComponentContext<?>> set : map.values()) {
                            Set<String> names = new LinkedHashSet<String>();
                            for (ManagedComponentContext<?> ctx : set) {
                                names.add(ctx.getComponentMetaData().getComponentName());
                            }
                            logger.finest("Deployment set " + (++i) + ": " + 
                                    names + ".");
                        }
                    }
                    ExecutorService executor = Executors.newFixedThreadPool(
                            threads, JargoThreadFactory.instance("Jargo-Deployer"));
                    try {
                        Throwable throwable = null;
                        for (Future<?> future : executor.invokeAll(callables)) {
                            try {
                                future.get();
                            } catch (ExecutionException e) {
                                throwable = e.getCause();
                            } catch (Exception e) {
                                throwable = e;
                            } finally {
                                continue;
                            }
                        }
                        if (throwable != null) {
                            throw throwable;
                        }
                    } finally {
                        List<Runnable> runnables = executor.shutdownNow();
                        assert runnables.isEmpty();
                    }
                }
                
                // Update reverse dependencies.
                for (ComponentConfiguration configuration : configurations) {
                    Set<String> dependencies = getDependencies(configuration, false);
                    if (dependencies != null) {
                        for (String dependency : dependencies) {
                            Set<String> rev = reverseDependencyMap.get(dependency);
                            if (rev == null) {
                                rev = new LinkedHashSet<String>();
                                reverseDependencyMap.put(dependency, rev);
                            }
                            rev.add(configuration.getComponentName());
                        }
                    }
                }
                commit = true;
            } catch (Throwable t) {
                logger.log(WARNING,
                        "The following exception is caught during deployment. " +
                        "Deployment of this unit is rolled back. " +
                        "This exception is re-thrown and logged again " +
                        "once roll back has finished.", t);
                try {
                    throw t;
                } catch (Exception e) {
                    throw e;
                } catch (Error e) {
                    throw e;
                } catch (Throwable e) {
                    assert false : e;
                    throw new RuntimeException(e);
                }
            } finally {
                try {
                    if (!commit) {
                        try {
                            for (ListIterator<ManagedComponentContext<Object>> i =
                                    createdCtx.listIterator(
                                    createdCtx.size()); i.hasPrevious();) {
                                try {
                                    ManagedComponentContext<Object> ctx = i.previous();
                                    @SuppressWarnings("unchecked")
                                    ComponentConfiguration<Object> configuration =
                                            (ComponentConfiguration)
                                            registry.getComponentConfiguration(
                                            ctx.getComponentMetaData().getComponentName());
                                    List<ComponentLifecycle<Object>> lifecycles =
                                            registry.getComponentLifecycles(configuration);
                                    for (ListIterator<ComponentLifecycle<Object>> i2 = lifecycles.
                                            listIterator(lifecycles.size()); i2.hasPrevious();) {
                                        try {
                                            ComponentLifecycle<Object> lifecycle = i2.previous();
                                            lifecycle.onDestroy(
                                                    new ComponentFactoryImpl<Object>(
                                                    configuration, registry));
                                        } finally {
                                            continue;
                                        }
                                    }
                                } finally {
                                    continue;
                                }
                            }
                        } finally {
                            for (ComponentConfiguration configuration : configurations) {
                                try {
                                    logger.info("Undeploying component '" +
                                            configuration.getComponentName() + "'.");
                                    registry.destroy(configuration.getComponentName());
                                } finally {
                                    continue;
                                }
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
    
    @SuppressWarnings("finally")
    public void undeploy(Deployable deployable)  throws Exception {
        if (deployable instanceof ComponentRegistration){
            List<ComponentConfiguration<?>> configurations =
                    ((ComponentRegistration) deployable).
                    getComponentConfigurations();
            
            lock.lock();
            try {
                Map<String, Set<ManagedComponentContext<?>>> map =
                        getOrderedContextList(configurations, false);
                
                // Clean up reverse dependencies.
                for (ComponentConfiguration<?> configuration : configurations) {
                    Set<String> dependencies = getDependencies(configuration, false);
                    if (dependencies != null) {
                        for (String dependency : dependencies) {
                            Set<String> rev = reverseDependencyMap.get(dependency);
                            if (rev != null) {
                                rev.remove(configuration.getComponentName());
                                if (rev.isEmpty()) {
                                    reverseDependencyMap.remove(dependency);
                                }
                            }
                        }
                    }
                }
                
                // Check for broken dependencies as a result of undeploying.
                for (ComponentConfiguration<?> configuration : configurations) {
                    String componentName = configuration.getComponentName();
                    Set<String> rev = reverseDependencyMap.get(componentName);
                    if (rev != null) {
                        assert !rev.isEmpty();
                        logger.warning("Undeploying component '" + componentName + "' " +
                                "while '" + rev + "' has a dependency on it.");
                    }
                }
                
                List<Callable<Object>> callables = new ArrayList<Callable<Object>>();
                for (final Set<ManagedComponentContext<?>> set : map.values()) {
                    Callable<Object> callable = new Callable<Object>() {
                        public Object call() throws Exception {
                            List<ComponentContext<?>> list = 
                                    new ArrayList<ComponentContext<?>>(set);
                            for (ListIterator<ComponentContext<?>> i =
                                    list.listIterator(list.size()); i.hasPrevious();) {
                                @SuppressWarnings("unchecked")
                                ManagedComponentContext<Object> ctx = 
                                        (ManagedComponentContext<Object>) i.previous();
                                String componentName = ctx.getComponentMetaData().getComponentName();
                                @SuppressWarnings("unchecked")
                                ComponentConfiguration<Object> configuration =
                                        (ComponentConfiguration)
                                        registry.getComponentConfiguration(
                                        componentName);
                                logger.info("Undeploying component '" + componentName + "'.");
                                List<ComponentLifecycle<Object>> lifecycles =
                                        registry.getComponentLifecycles(configuration);
                                for (ListIterator<ComponentLifecycle<Object>> i2 =  lifecycles.
                                        listIterator(lifecycles.size()); i2.hasPrevious();) {
                                    try {
                                        ComponentLifecycle<Object> lifecycle = i2.previous();
                                        lifecycle.onDestroy(
                                                new ComponentFactoryImpl<Object>(
                                                configuration, registry));
                                    } finally {
                                        continue;
                                    }
                                }
                            }

                            for (ListIterator<ComponentContext<?>> i =
                                    list.listIterator(list.size()); i.hasPrevious();) {
                                try {
                                    ComponentContext<?> ctx = i.previous();
                                    registry.destroy(ctx.getComponentMetaData().getComponentName());
                                } finally {
                                    continue;
                                }
                            }
                            return null;
                        }
                    };
                    callables.add(callable);
                }

                int threads = Math.min(callables.size(), AccessController.doPrivileged(
                        new PrivilegedAction<Integer>() {
                    public Integer run() {
                        // PERMISSION: java.util.PropertyPermission org.jargo.threads read
                        return Integer.parseInt(System.getProperty("org.jargo.threads", 
                                String.valueOf(Runtime.getRuntime().availableProcessors())));
                    }
                }));
                if (threads <= 1) {
                    for (Callable callable : callables) {
                        callable.call();
                    }
                } else if (!callables.isEmpty()) {
                    if (logger.isLoggable(FINEST)) {
                        logger.finest("Undeployment of components is performed " +
                                "by " + threads + " threads.");
                        int i = 0;
                        for (Set<ManagedComponentContext<?>> set : map.values()) {
                            Set<String> names = new LinkedHashSet<String>();
                            for (ComponentContext<?> ctx : set) {
                                names.add(ctx.getComponentMetaData().getComponentName());
                            }
                            logger.finest("Deployment set " + (++i) + ": " + 
                                    names + ".");
                        }
                    }
                    ExecutorService executor = Executors.newFixedThreadPool(
                            threads, JargoThreadFactory.instance("Jargo-Undeployer"));
                    try {
                        Throwable throwable = null;
                        for (Future<?> future : executor.invokeAll(callables)) {
                            try {
                                future.get();
                            } catch (ExecutionException e) {
                                throwable = e.getCause();
                            } catch (Exception e) {
                                throwable = e;
                            } finally {
                                continue;
                            }
                        }
                        if (throwable != null) {
                            try {
                                throw throwable;
                            } catch (Exception e) {
                                throw e;
                            } catch (Error e) {
                                throw e;
                            } catch (Throwable t) {
                                assert false : t;
                                throw new RuntimeException(t);
                            }
                        }
                    } finally {
                        List<Runnable> runnables = executor.shutdownNow();
                        assert runnables.isEmpty();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
    
    private Set<String> getDependencies(ComponentConfiguration configuration,
            boolean log) {
        final Set<String> dependencies;
        String componentName = configuration.getComponentName();
        if (registry.exists(componentName, true)) {
            DependencyInspectors dependencyInspectors =
                    new DependencyInspectors(configuration, providers);
            
            Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
            classes.addAll(getEventInterceptorClasses(configuration));
            classes.addAll(getInvocationInterceptorClasses(configuration));
            
            if (log) {
                logger.finest("Component '" + configuration.getComponentName() +
                        "' specifies the following interceptors: " + classes + ".");
            }
            
            classes.add(configuration.getType());
            
            dependencies = new LinkedHashSet<String>();
            for (Class c : classes) {
                for (String dependency : dependencyInspectors.
                        getDependencies(c)) {
                    dependencies.add(registry.getComponentName(dependency));
                }
            }
        } else {
            dependencies = null;
        }
        return dependencies;
    }
    
    private Map<String, Set<ManagedComponentContext<?>>> getOrderedContextList(
            List<ComponentConfiguration<?>> configurations, boolean log) {
        Map<String, Set<ManagedComponentContext<?>>> map =
                new LinkedHashMap<String, Set<ManagedComponentContext<?>>>();
        Map<String, Set<String>> dependencyMap =
                new HashMap<String, Set<String>>();
        Map<String, Set<String>> localDependencyMap =
                new HashMap<String, Set<String>>();
        Map<String, Set<String>> localReverseDependencyMap =
                new HashMap<String, Set<String>>();
        
        Queue<ManagedComponentContext<?>> queue = new
                LinkedList<ManagedComponentContext<?>>();

        Set<String> localComponents = new HashSet<String>();
        for (ComponentConfiguration<?> tmp : configurations) {
            localComponents.add(tmp.getComponentName());
        }        
        for (ComponentConfiguration<?> tmp : configurations) {
            @SuppressWarnings("unchecked")
            ComponentConfiguration<Object> configuration =
                    (ComponentConfiguration<Object>) tmp;
            Set<String> dependencies = getDependencies(configuration, log);
            if (dependencies != null) {
                String componentName = configuration.getComponentName();
                if (log && !dependencies.isEmpty()) {
                    logger.info("Component '" +
                            componentName + "' depends on: " +
                            dependencies + ".");
                }
                for (String dependency : dependencies) {
                    if (localComponents.contains(dependency)) {
                        // Only use components that are actually being registered.
                        Set<String> rev = localReverseDependencyMap.get(dependency);
                        if (rev == null) {
                            rev = new LinkedHashSet<String>();
                            localReverseDependencyMap.put(dependency, rev);
                        }
                        rev.add(configuration.getComponentName());
                    }
                }
                ManagedComponentContext<?> ctx = 
                        new ManagedComponentContextImpl<Object>(
                        registry.getComponentMetaData(configuration));
                dependencyMap.put(componentName, dependencies);
                Set<String> localDependencies = new HashSet<String>(dependencies);
                localDependencies.retainAll(localComponents);
                localDependencyMap.put(componentName, localDependencies);
                queue.add(ctx);
            }
        }
        checkDependencies(dependencyMap);
        
        Set<String> components = new HashSet<String>();
        ManagedComponentContext<?> ctx = null;
        while ((ctx = queue.poll()) != null) {
            boolean checked = true;
            String componentName = ctx.getComponentMetaData().getComponentName();
            
            for (String dependency : dependencyMap.get(componentName)) {
                if (!registry.exists(dependency, true)) {
                    // Check if dependency is available.
                    throw new ComponentException(componentName,
                            "Dependency not found: '" + dependency + "'.");
                } else if (!dependencyMap.containsKey(dependency)) {
                    // Dependency is not part of this unit of deployment.
                    continue;
                } else if (!components.contains(dependency)) {
                    queue.add(ctx);
                    checked = false;
                    break;
                }
            }
            if (checked) {
                Set<ManagedComponentContext<?>> set = getRootSet(componentName, 
                        map, localDependencyMap, localReverseDependencyMap);
                set.add(ctx);
                components.add(componentName);
            }
        }
        return map;
    }
    
    private Set<ManagedComponentContext<?>> getRootSet(String componentName, 
            Map<String, Set<ManagedComponentContext<?>>> map,
            Map<String, Set<String>> dependencyMap,
            Map<String, Set<String>> reverseDependencyMap) {
        Set<String> allRoots = new HashSet<String>();
        getAllRoots(componentName, dependencyMap, reverseDependencyMap, 
                allRoots);
        for (String root : allRoots) {
            Set<ManagedComponentContext<?>> set = map.get(root);
            if (set != null) {
                return set;
            }
        }
        Set<ManagedComponentContext<?>> set = 
                new LinkedHashSet<ManagedComponentContext<?>>();
        Object check = map.put(componentName, set);
        assert check == null;
        return set;
    }
    
    /**
     * Looks up all roots for the specified {@code componentName} in both 
     * directions. This method is different from {@code getRelatedRoots} in that
     * it also adds the roots of all related roots.
     */
    private void getAllRoots(String componentName, 
            Map<String, Set<String>> dependencyMap,
            Map<String, Set<String>> reverseDependencyMap, Set<String> roots) {
        Set<String> relatedRoots = getRelatedRoots(componentName, dependencyMap, 
                reverseDependencyMap);
        for (String root : relatedRoots) {
            if (!roots.contains(root)) {
                roots.add(root);
                getAllRoots(root, dependencyMap, reverseDependencyMap, roots);
            } else {
                roots.add(root);
            }
        }
    }
    
    /**
     * Returns all roots for the specified {@code componentName} in both 
     * directions.
     */
    private Set<String> getRelatedRoots(String componentName, 
            Map<String, Set<String>> dependencyMap,
            Map<String, Set<String>> reverseDependencyMap) {
        Set<String> roots = new LinkedHashSet<String>();
        getRoots(componentName, dependencyMap, roots);
        Set<String> reverseRoots = new LinkedHashSet<String>();
        for (String root : roots) {
            getRoots(root, reverseDependencyMap, reverseRoots);
        }
        Set<String> allRoots = new LinkedHashSet<String>();
        for (String reverseRoot : reverseRoots) {
            getRoots(reverseRoot, dependencyMap, allRoots);
        }
        return allRoots;
    }
    
    /**
     * Looks up all roots for the specified {@code componentName} in 
     * {@code dependencyMap} one direction and adds these to the specified 
     * {@code roots} set.
     */
    private void getRoots(String componentName, 
            Map<String, Set<String>> dependencyMap, Set<String> roots) {
        Set<String> deps = dependencyMap.get(componentName);
        if (deps == null || deps.isEmpty()) {
            roots.add(componentName);
        } else {
            for (String dep : deps) {
                getRoots(dep, dependencyMap, roots);
            }
        }
    }
    
    private Set<Class<?>> getEventInterceptorClasses(
            ComponentConfiguration<?> configuration) {
        Set<Class<?>> interceptorClasses = new LinkedHashSet<Class<?>>();
        EventFactory eventFactory = registry.getEventFactory(configuration);
        for (Class<? extends Event> type : eventFactory.getEventTypes()) {
            for (EventInterceptorFactory factory : providers.
                    getEventInterceptorFactoryProvider().
                    getEventInterceptorFactories(configuration, type)) {
                interceptorClasses.add(factory.getType());
            }
        }
        return interceptorClasses;
    }
    
    private Set<Class<?>> getInvocationInterceptorClasses(
            ComponentConfiguration<?> configuration) {
        Set<Class<?>> interceptorClasses = new LinkedHashSet<Class<?>>();
        InvocationFactory invocationFactory = registry.getInvocationFactory(
                configuration);
        for (Method method: invocationFactory.getMethods()) {
            for (InvocationInterceptorFactory factory : providers.
                    getInvocationInterceptorFactoryProvider().
                    getInvocationInterceptorFactories(configuration, method)) {
                interceptorClasses.add(factory.getType());
            }
        }
        return interceptorClasses;
    }
    
    private static void checkDependencies(Map<String, Set<String>> dependencyMap) {
        LinkedList<String> tmp = new LinkedList<String>();
        for (String dependency : dependencyMap.keySet()) {
            LinkedList<String> path = checkDependencies(dependency, dependencyMap,
                    tmp);
            if (path != null) {
                String last = path.getLast();
                int index = path.indexOf(last);
                assert index != -1;
                for (int i = 0; i < index; i++) {
                    path.removeFirst();
                }
                assert path.size() >= 2;
                final String msg;
                if (path.size() == 2) {
                    msg = "Recursive dependency detected in graph: ";
                } else if (path.size() == 3) {
                    msg = "Cyclic dependency detected in graph: ";
                } else {
                    msg = "Cyclic transitive dependency detected in graph: ";
                }
                StringBuilder builder = new StringBuilder(msg);
                for (Iterator<String> i = path.iterator(); i.hasNext();) {
                    builder.append("'" + i.next() + "'");
                    if (i.hasNext()) {
                        builder.append(" -> ");
                    }
                }
                builder.append(".");
                throw new ComponentException(path.getFirst(), builder.toString());
            }
            tmp.clear();
        }
    }
    
    private static LinkedList<String> checkDependencies(String dependency,
            Map<String, Set<String>> dependencyMap, LinkedList<String> path) {
        path.addLast(dependency);
        if (Collections.frequency(path, dependency) > 1) {
            return path;
        } else {
            Set<String> dependencies = dependencyMap.get(dependency);
            if (dependencies != null) {
                for (String dep : dependencies) {
                    LinkedList<String> tmp = checkDependencies(dep, dependencyMap,
                            path);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
        }
        path.removeLast();
        return null;
    }
}
