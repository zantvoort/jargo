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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import org.jargo.ComponentAlias;
import org.jargo.ComponentApplicationException;
import org.jargo.ComponentExceptionHandler;
import org.jargo.ComponentMetaData;
import org.jargo.ComponentNotActiveException;
import org.jargo.ComponentNotFoundException;
import org.jargo.ComponentReference;
import org.jargo.EventFactory;
import org.jargo.ExecutorHandle;
import org.jargo.InvocationFactory;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentCreationException;
import org.jargo.ComponentLifecycle;
import org.jargo.ComponentObjectPool;
import org.jargo.ComponentException;
import org.jargo.ComponentFactory;
import org.jargo.ComponentObject;
import org.jargo.ComponentObjectFactory;
import org.jargo.Event;
import org.jargo.EventInterceptorFactory;
import org.jargo.InvocationInterceptorFactory;

/**
 * @author Leon van Zantvoort
 */
final class ComponentRegistryImpl implements ComponentRegistry {

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final BlockingQueue<Set<Thread>> threadQueue = new ArrayBlockingQueue<Set<Thread>>(1, false);

    private final Logger logger;
    
    private final ReadWriteLock lock;
    
    private final Providers providers;
    
    private final Map<String, ComponentConfiguration> componentConfigurations;
    
    /**
     * Holds the component context of activated components.
     */
    private final Map<ComponentConfiguration, ManagedComponentContext> componentContexts;
    
    private final Map<ComponentConfiguration, ComponentObjectFactory> componentObjectFactories;
    private final Map<ComponentConfiguration, EventFactory> eventFactories;
    private final Map<ComponentConfiguration, InvocationFactory> invocationFactories;
    private final Map<ComponentConfiguration, List<ComponentLifecycle>> componentLifecycles;
    private final Map<ComponentConfiguration, ComponentMetaData> componentMetaData;
    private final Map<ComponentConfiguration, ExecutorHandle> executorHandles;
    private final Map<ComponentConfiguration, ComponentExceptionHandler> componentExceptionHandlers;
    private final Map<String, Set<WeakComponentReference>> references;

    private final Map<WeakReference<StrongComponentReference>, WeakComponentReference> weakReferences;
    private final ReferenceQueue<StrongComponentReference> queue;

    private final Map<String, String> aliases;
    private final Map<String, String> overrideAliases;
    
    public ComponentRegistryImpl(Providers providers) {
        this.providers = providers;
        this.logger = Logger.getLogger(getClass().getName());
        
        // JCC-4: Fairness policy cannot be used with JSE5, it will result in a deadlock.
        this.lock = new ReentrantReadWriteLock(false);
        this.componentConfigurations = new HashMap<String, ComponentConfiguration>();
        this.componentContexts = new HashMap<ComponentConfiguration, ManagedComponentContext>();
        this.componentObjectFactories = new HashMap<ComponentConfiguration, ComponentObjectFactory>();
        this.eventFactories = new HashMap<ComponentConfiguration, EventFactory>();
        this.invocationFactories = new HashMap<ComponentConfiguration, InvocationFactory>();
        this.componentLifecycles = new HashMap<ComponentConfiguration, List<ComponentLifecycle>>();
        this.componentMetaData = new HashMap<ComponentConfiguration, ComponentMetaData>();
        this.executorHandles = new HashMap<ComponentConfiguration, ExecutorHandle>();
        this.componentExceptionHandlers = new HashMap<ComponentConfiguration, ComponentExceptionHandler>();
        this.references = new HashMap<String, Set<WeakComponentReference>>();

        this.weakReferences = new HashMap<WeakReference<StrongComponentReference>, WeakComponentReference>();
        this.queue = new ReferenceQueue<StrongComponentReference>();
        
        this.aliases = new HashMap<String, String>();
        this.overrideAliases = new HashMap<String, String>();

        Set<Thread> backgroundThreads = new HashSet<Thread>();
        final ThreadFactory detectorThreadFactory = JargoThreadFactory.
                instance("Jargo-StallingReaperDetector");
        Runnable detector = new Runnable() {
            public void run() {
                try {
                    Object ref = null;
                    while (!shutdown.get()) {
                        Object tmp = queue.poll();
                        if (ref != null) {
                            if (ref == tmp) {
                                // If this warning is printed, make a thread 
                                // dump to check what the reaper threads are 
                                // doing.
                                logger.warning("Reference reaper is stalling. " +
                                        "Cleaning up references takes more " +
                                        "than 60 seconds.");
                            }
                            ref = tmp;
                        }
                        Thread.sleep(60000);
                    }
                } catch (InterruptedException e) {
                    if (!shutdown.get()) {
                        throw new ComponentApplicationException(e);
                    }
                } finally {
                    if (!shutdown.get()) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            // Ignore.
                        } finally {
                            Set<Thread> backgroundThreads = threadQueue.remove();
                            try {
                                if (!shutdown.get()) {
                                    Thread t = detectorThreadFactory.newThread(this);
                                    backgroundThreads.add(t);
                                    t.start();
                                }
                            } finally {
                                threadQueue.offer(backgroundThreads);
                            }
                        }
                    }
                }
            }
        };
        Thread detectorThread = detectorThreadFactory.newThread(detector);
        backgroundThreads.add(detectorThread);
        detectorThread.start();

        int processors = Runtime.getRuntime().availableProcessors();
        final ThreadFactory reaperThreadFactory = JargoThreadFactory.
                instance(processors == 1 ? "Jargo-ReferenceReaper" :
                "Jargo-ConcurrentReferenceReaper");
        for (int i = 0; i < processors; i++) {
            Runnable reaper = new Runnable() {
                public void run() {
                    try {
                        while (!shutdown.get()) {
                            WeakComponentReference reference = weakReferences.
                                    remove(queue.remove());
                            assert reference != null;

                            Lock writeLock = lock.writeLock();
                            writeLock.lock();
                            try {
                                Set<WeakComponentReference> set = references.get(
                                        reference.getComponentMetaData().getComponentName());
                                if (set != null) {
                                    boolean removed = set.remove(reference);
                                    assert removed;
                                }
                            } finally {
                                writeLock.unlock();
                            }

                            if (!reference.isRemoved()) {
                                // This if-statement could be removed.
                                reference.invalidate();
                            }
                        }
                    } catch (InterruptedException e) {
                        if (!shutdown.get()) {
                            throw new ComponentApplicationException(e);
                        }
                    } finally {
                        if (!shutdown.get()) {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                // Ignore.
                            } finally {
                                Set<Thread> backgroundThreads = threadQueue.remove();
                                try {
                                    if (!shutdown.get()) {
                                        Thread t = reaperThreadFactory.newThread(this);
                                        backgroundThreads.add(t);
                                        t.start();
                                    }
                                } finally {
                                    threadQueue.offer(backgroundThreads);
                                }
                            }
                        }
                    }
                }
            };
            Thread reaperThread = reaperThreadFactory.newThread(reaper);
            backgroundThreads.add(reaperThread);
            reaperThread.start();
            threadQueue.offer(backgroundThreads);
        }
    }

    @SuppressWarnings("finally")
    public void shutdown() {
        if (!shutdown.getAndSet(true)) {
            boolean interrupted = false;
            for (final Thread t : threadQueue.remove()) {
                try {
                    AccessController.doPrivileged(
                            new PrivilegedAction<Object>() {
                                public Object run() {
                                    // PERMISSION: java.lang.RuntimePermission modifyThread
                                    t.interrupt();
                                    return null;
                                }
                            });

                    if (!interrupted) {
                        t.join(5000);
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                } finally {
                    continue;
                }
            }
        }
    }
    
    public void create(final ComponentConfiguration<?> configuration) {
        String componentName = configuration.getComponentName();
        boolean commit = false;
        try {
            Lock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                if (componentConfigurations.containsKey(componentName)) {
                    throw new ComponentException(componentName,
                            "Already exists.");
                }
                if (aliases.containsKey(componentName)) {
                    throw new ComponentException(componentName,
                            "Alias already exists.");
                }
                
                ComponentObjectFactory factory = providers.
                        getComponentObjectFactoryProvider().
                        getComponentObjectFactory(configuration);
                if (factory == null) {
                    throw new ComponentException(componentName,
                            "No ComponentObjectFactory implementation found.");
                }
                EventFactory eventFactory = new EventFactories(configuration, providers);
                InvocationFactory invocationFactory = new InvocationFactories(configuration, providers);

                ComponentExceptionHandler exceptionHandler =
                        providers.getComponentExceptionHandlerProvider().
                        getComponentExceptionHandler(configuration);
                if (exceptionHandler == null) {
                    exceptionHandler = new DefaultComponentExceptionHandlerImpl();
                }
                componentExceptionHandlers.put(configuration, exceptionHandler);

                ExecutorHandle executorHandle = providers.getExecutorHandleProvider().
                        getExecutorHandle(configuration, JargoThreadFactory.
                        instance("Jargo[" + configuration.getComponentName() + "]"));
                if (executorHandle == null) {
                    executorHandle = new DefaultExecutorHandleImpl();
                }
                executorHandles.put(configuration, executorHandle);

                @SuppressWarnings("unchecked")
                List<ComponentLifecycle> lifecycles = 
                        (List<ComponentLifecycle>) (List<?>) providers.
                        getComponentLifecycleProvider().getComponentLifecycles(
                        configuration, executorHandle.getExecutor());

                componentConfigurations.put(componentName, configuration);
                componentObjectFactories.put(configuration, factory);

                eventFactories.put(configuration, eventFactory);
                invocationFactories.put(configuration, invocationFactory);
                componentLifecycles.put(configuration, lifecycles);
                
                List<Class<?>> interfaces = eventFactory.getInterfaces();
                boolean proxy = isProxy(configuration);
                boolean vanilla = !(getComponentObjectFactory(configuration) 
                        instanceof ComponentObjectPool);
                if (!vanilla) {
                    // Don't create reference set for vanilla components, because
                    // storing references for such components would prevent them from
                    // being garbage collected.
                    references.put(componentName, new HashSet<WeakComponentReference>());
                }
                ComponentMetaData metaData = new ComponentMetaDataImpl(
                        configuration, interfaces, vanilla, factory.isStatic(), 
                        proxy, providers.getMetaDataProvider().
                        getMetaData(configuration));
                componentMetaData.put(configuration, metaData);
            } finally {
                writeLock.unlock();
            }
            commit = true;
        } catch (ComponentException e) {
            throw e;
        } catch (Exception e) {
            // PENDING: catch Errors as well?
            throw new ComponentException(configuration.getComponentName(), e);
        } finally {
            if (!commit) {
                destroy(componentName);
            }
        }
    }

    private boolean isProxy(ComponentConfiguration<?> configuration) {
        if (getComponentObjectFactory(configuration) instanceof ComponentObjectPool) {
            return true;
        }
        if (getEventFactory(configuration).isProxy()) {
            return true;
        }
        for (Class<? extends Event> type : getEventFactory(configuration).
                getEventTypes()) {
            List<EventInterceptorFactory> list = providers.
                    getEventInterceptorFactoryProvider().
                    getEventInterceptorFactories(configuration, type);
            if (!list.isEmpty()) {
                return true;
            }
        }
        for (Method method : getInvocationFactory(configuration).getMethods()) {
            List<InvocationInterceptorFactory> list = providers.
                    getInvocationInterceptorFactoryProvider().
                    getInvocationInterceptorFactories(configuration, method);
            if (!list.isEmpty()) {
                return true;
            }
        }
        List<Class<?>> list = getEventFactory(configuration).getInterfaces();
        if (!list.isEmpty()) {
            return true;
        }
        return false;
    }
    
    /**
     * Returns true component name.
     */
    public String getComponentName(String alias) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            String name = overrideAliases.get(alias);
            if (name == null) {
                name = aliases.get(alias);
            }
            return name == null ? alias : name;
        } finally {
            readLock.unlock();
        }
    }
    
    public void addAlias(ComponentAlias alias) {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (alias.override()) {
                if (overrideAliases.containsKey(alias.getComponentAlias())) {
                    throw new ComponentException(alias.getComponentAlias(),
                            "Override alias already exists.");
                }
                overrideAliases.put(alias.getComponentAlias(), alias.getComponentName());
            } else {
                if (componentConfigurations.containsKey(alias.getComponentAlias())) {
                    throw new ComponentException(alias.getComponentAlias(),
                            "Already exists.");
                }
                if (aliases.containsKey(alias.getComponentAlias())) {
                    throw new ComponentException(alias.getComponentAlias(),
                            "Alias already exists.");
                }
                aliases.put(alias.getComponentAlias(), alias.getComponentName());
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void removeAlias(ComponentAlias alias) {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (alias.override()) {
                overrideAliases.remove(alias.getComponentAlias());
            } else {
                aliases.remove(alias.getComponentAlias());
            }
        } finally {
            writeLock.unlock();
        }
    }
    
    public <T> ComponentReference<T> createReference(
            ComponentConfiguration<T> configuration, Object info) throws 
            ComponentCreationException, ComponentNotActiveException {
        try {
            final StrongComponentReference<T> strongRef;
            final WeakComponentReference<T> weakRef;

            Lock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                @SuppressWarnings("unchecked")
                ManagedComponentContext<T> ctx = 
                        (ManagedComponentContext<T>) componentContexts.get(
                        configuration);
                if (ctx == null) {
                    throw new ComponentNotActiveException(
                            configuration.getComponentName());
                }
                weakRef = new WeakComponentReference<T>(ctx, configuration,
                        this, info, new Destroyer());
                ComponentMetaData<T> metaData = ctx.getComponentMetaData();
                try {
                    ctx.attach(weakRef);
                    ComponentObjectFactory<T> factory = 
                            getComponentObjectFactory(configuration);
                    if (metaData.isVanilla()) {
                        assert !(factory instanceof ComponentObjectPool);
                        ComponentObject<T> object = null;
                        if (!factory.isStatic()) {
                            object = factory.create();
                        }
                        if (object == null) {
                            object = factory.getComponentObject();
                            assert object != null;
                            // If a new component was created by the factory, 
                            // this component was automatically set to weakRef.
                            // Since no new component was created, obtain a 
                            // component and set it manually.
                            weakRef.setComponent(object.getInstance());
                        }
                        strongRef = null;
                    } else {
                        assert factory instanceof ComponentObjectPool;
                        weakRef.setComponent(ComponentProxy.getComponentProxy(
                                weakRef, this));
                        if (!factory.isStatic()) {
                            factory.create();
                        }
                        strongRef = new StrongComponentReference<T>(weakRef);
                        WeakReference<StrongComponentReference> weakReference = new WeakReference<StrongComponentReference>(strongRef, queue);
                        Set<WeakComponentReference> set = references.get(metaData.getComponentName());
                        assert set != null;
                        boolean success = set.add(weakRef);
                        assert success;
                        weakReferences.put(weakReference, weakRef);
                    }
                } finally {
                    ctx.detach();
                }
            } finally {
                writeLock.unlock();
            }
            final ComponentReference<T> ref;
            if (strongRef == null) {
                ref = weakRef;
            } else {
                strongRef.setComponent(ComponentProxy.getComponentProxy(strongRef,
                        this));
                ref = strongRef;
            }    
            weakRef.init(ref);
            return ref;
        } catch (ComponentCreationException e) {
            throw e;
        } catch (Exception e) {
            // PENDING: catch Errors as well?
            throw new ComponentCreationException(
                    configuration.getComponentName(), e);
        }
    }
    
    public void destroy(String componentName) {
        Set<WeakComponentReference> refs = null;
        
        ManagedComponentContext<Object> ctx = null;
        WeakComponentReference<Object> reference = null;
        ComponentObjectFactory<Object> factory = null;
        ExecutorHandle executorHandle = null;
        
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (references.containsKey(componentName)) {
                refs = references.get(componentName);
            } else {
                refs = Collections.emptySet();
            }
            references.remove(componentName);
            
            @SuppressWarnings("unchecked")
            ComponentConfiguration<Object> configuration = 
                    (ComponentConfiguration<Object>)
                    componentConfigurations.get(componentName);
            @SuppressWarnings("unchecked")
            ComponentObjectFactory<Object> tmp1 = 
                    (ComponentObjectFactory<Object>) componentObjectFactories.
                    get(configuration);
            @SuppressWarnings("unchecked")
            ManagedComponentContext<Object> tmp2 = 
                    (ManagedComponentContext<Object>) componentContexts.
                    get(configuration);
            factory = tmp1;
            if (tmp2 != null) {
                if (factory.isStatic()) {
                    reference = new WeakComponentReference<Object>(tmp2, 
                            configuration, this, null, new Destroyer());
                    reference.setComponent(
                            ComponentProxy.getComponentProxy(reference, this));
                }
            }
            ctx = tmp2;
            
            executorHandle = executorHandles.remove(configuration);
                       
            eventFactories.remove(configuration);
            invocationFactories.remove(configuration);
            componentLifecycles.remove(configuration);
            componentMetaData.remove(configuration);
            componentExceptionHandlers.remove(configuration);
            
            componentConfigurations.remove(componentName);
            componentObjectFactories.remove(configuration);
            componentContexts.remove(configuration);
        } finally {
            writeLock.unlock();
            try {
                for (ComponentReference ref : refs) {
                    ref.invalidate();
                }
            } finally {
                try {
                    if (ctx != null) {
                        try {
                            if (factory.isStatic()) {
                                ctx.attach(reference);
                            }
                            factory.destroy();
                        } finally {
                            if (factory.isStatic()) {
                                ctx.detach();
                            }
                        }
                    }
                } finally {
                    if (executorHandle != null) {
                        executorHandle.destroy();
                    }
                }
            }
        }
    }
    
    public Providers getProviders() {
        return providers;
    }
    
    public void activate(ManagedComponentContext<?> ctx) {
        @SuppressWarnings("unchecked")
        ManagedComponentContext<Object> tmp = (ManagedComponentContext<Object>)
                ctx;
        // Initialization of the object factory is outside locking scope!
        @SuppressWarnings("unchecked")
        ComponentConfiguration<Object> configuration = 
                (ComponentConfiguration<Object>) getComponentConfiguration(
                ctx.getComponentMetaData().getComponentName());
        ComponentObjectFactory<Object> factory = 
                getComponentObjectFactory(configuration);
        ComponentObjectBuilderImpl<Object> builder = 
                new ComponentObjectBuilderImpl<Object>(tmp, configuration, this);
        try {
            if (factory.isStatic()) {
                WeakComponentReference<Object> reference = 
                        new WeakComponentReference<Object>(
                        tmp, configuration, this, null, new Destroyer());
                reference.setComponent(
                        ComponentProxy.getComponentProxy(reference, this));
                tmp.attach(reference);
            }
            try {
                factory.init(builder);
            } finally {
                if (factory.isStatic()) {
                    tmp.detach();
                }
            }
        } finally {
            Lock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                componentContexts.put(configuration, tmp);
            } finally {
                writeLock.unlock();
            }
        }
    }
    
    public boolean exists(String componentName, boolean useAlias) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            if (useAlias) {
                componentName = getComponentName(componentName);
            }
            return componentConfigurations.containsKey(componentName);
        } finally {
            readLock.unlock();
        }
    }
    
    public List<ComponentFactory<?>> list() {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            List<ComponentFactory<?>> list = new ArrayList<ComponentFactory<?>>();
            for (ComponentConfiguration<?> configuration : componentConfigurations.values()) {
                @SuppressWarnings("unchecked")
                ComponentConfiguration<Object> cfg = (ComponentConfiguration<Object>) configuration;
                ComponentFactory<?> factory = new ComponentFactoryImpl<Object>(
                        cfg, this);
                list.add(factory);
            }
            return Collections.unmodifiableList(list);
        } finally {
            readLock.unlock();
        }
    }
    
    public <T> List<ComponentFactory<? extends T>> list(Class<T> type) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            List<ComponentFactory<? extends T>> list = null;
            for (Map.Entry<ComponentConfiguration, ComponentMetaData> e : 
                    componentMetaData.entrySet()) {
                ComponentConfiguration<?> configuration = e.getKey();
                ComponentMetaData<?> metaData = e.getValue();
                final List<Class<?>> types;
                if (metaData.isVanilla()) {
                    types = new ArrayList<Class<?>>(metaData.getInterfaces());
                    types.add(metaData.getType());
                } else {
                    types = metaData.getInterfaces();
                }
                for (Class<?> t : types) {
                    if (type.isAssignableFrom(t)) {
                        @SuppressWarnings("unchecked")
                        ComponentConfiguration<T> cfg = 
                                (ComponentConfiguration<T>) configuration;
                        ComponentFactory<T> factory = 
                                new ComponentFactoryImpl<T>(cfg, this);
                        if (list == null) {
                            list = new ArrayList<ComponentFactory<? extends T>>();
                        }
                        list.add(factory);
                        break;
                    }
                }
            }
            return list == null ? 
                    Collections.<ComponentFactory<? extends T>>emptyList() :
                    Collections.unmodifiableList(list);
        } finally {
            readLock.unlock();
        }
    }
    
    public ComponentFactory<?> lookup(String componentName) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            String name = getComponentName(componentName);
            @SuppressWarnings("unchecked")
            ComponentConfiguration<Object> configuration = 
                    (ComponentConfiguration<Object>) componentConfigurations.get(name);
            if (configuration == null) {
                throw new ComponentNotFoundException(componentName);
            }
            ComponentFactory<?> factory = new ComponentFactoryImpl<Object>(
                    configuration, this);
            return factory;
        } finally {
            readLock.unlock();
        }
    }
    
    public ComponentConfiguration<?> getComponentConfiguration(String componentName) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            ComponentConfiguration<?> configuration = componentConfigurations.
                    get(componentName);
            if (configuration == null) {
                throw new ComponentNotFoundException(componentName);
            }
            return configuration;
        } finally {
            readLock.unlock();
        }
    }
    
    public <T> List<ComponentLifecycle<T>> getComponentLifecycles(
            ComponentConfiguration<T> configuration) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            List<ComponentLifecycle<T>> lifecycles = 
                    new ArrayList<ComponentLifecycle<T>>();
            List<ComponentLifecycle> tmp = componentLifecycles.get(configuration);
            if (lifecycles == null) {
                throw new ComponentNotFoundException(
                        configuration.getComponentName());
            }
            for (ComponentLifecycle lifecycle : tmp) {
                @SuppressWarnings("unchecked")
                ComponentLifecycle<T> t = (ComponentLifecycle<T>) lifecycle;
                lifecycles.add(t);
            }
            return lifecycles;
        } finally {
            readLock.unlock();
        }
    }
    
    public <T> ComponentObjectFactory<T> getComponentObjectFactory(
            ComponentConfiguration<T> configuration) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            @SuppressWarnings("unchecked")
            ComponentObjectFactory<T> factory = (ComponentObjectFactory<T>)
                    componentObjectFactories.get(configuration);
            if (factory == null) {
                throw new ComponentNotFoundException(
                        configuration.getComponentName());
            }
            return factory;
        } finally {
            readLock.unlock();
        }
    }
    
    public EventFactory getEventFactory(
            ComponentConfiguration<?> configuration) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            EventFactory factory = eventFactories.get(configuration);
            if (factory == null) {
                throw new ComponentNotFoundException(
                        configuration.getComponentName());
            }
            return factory;
        } finally {
            readLock.unlock();
        }
    }
    
    public InvocationFactory getInvocationFactory(
            ComponentConfiguration<?> configuration) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            InvocationFactory factory = invocationFactories.get(configuration);
            if (factory == null) {
                throw new ComponentNotFoundException(
                        configuration.getComponentName());
            }
            return factory;
        } finally {
            readLock.unlock();
        }
    }
    
    public <T> ComponentMetaData<T> getComponentMetaData(
            ComponentConfiguration<T> configuration) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            @SuppressWarnings("unchecked")
            ComponentMetaData<T> metaData = (ComponentMetaData<T>)
                    componentMetaData.get(configuration);
            if (metaData == null) {
                throw new ComponentNotFoundException(
                        configuration.getComponentName());
            }
            return metaData;
        } finally {
            readLock.unlock();
        }
    }
    
    public ExecutorHandle getExecutorHandle(
            ComponentConfiguration<?> configuration) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            ExecutorHandle handle = executorHandles.get(configuration);
            if (handle == null) {
                throw new ComponentNotFoundException(
                        configuration.getComponentName());
            }
            return handle;
        } finally {
            readLock.unlock();
        }
    }
    
    public ComponentExceptionHandler getComponentExceptionHandler(
            ComponentConfiguration<?> configuration) {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            ComponentExceptionHandler exceptionHandler =
                    componentExceptionHandlers.get(configuration);
            if (exceptionHandler == null) {
                throw new ComponentNotFoundException(
                        configuration.getComponentName());
            }
            return exceptionHandler;
        } finally {
            readLock.unlock();
        }
    }
}
