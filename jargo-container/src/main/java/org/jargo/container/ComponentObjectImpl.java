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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.jargo.ComponentExecutionException;
import org.jargo.Event;
import org.jargo.ComponentReference;
import org.jargo.EventFactory;
import org.jargo.EventInterceptor;
import org.jargo.EventInterceptorAdapter;
import org.jargo.EventContext;
import org.jargo.EventInterceptorFactory;
import org.jargo.InjectionFactory;
import org.jargo.Invocation;
import org.jargo.InvocationFactory;
import org.jargo.InvocationInterceptor;
import org.jargo.InvocationInterceptorAdapter;
import org.jargo.InvocationContext;
import org.jargo.InvocationInterceptorFactory;
import org.jargo.Lifecycle;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentContext;
import org.jargo.ComponentCreationException;
import org.jargo.ComponentEventException;
import org.jargo.ComponentEventNotExecutableException;
import org.jargo.ComponentObject;
import org.jargo.ComponentException;
import org.jargo.ComponentMetaData;
import org.jargo.ConstructorInjection;
import org.jargo.ProxyController;
import org.jargo.SetterInjection;

/**
 * @author Leon van Zantvoort
 */
final class ComponentObjectImpl<T> implements ComponentObject<T> {
    
    private static final Object DUMMY = new Object();
    
    private final Logger logger;
    private final ManagedComponentContext<T> ctx;
    private final ComponentConfiguration<T> configuration;
    private final ComponentRegistry registry;
    private final Providers providers;
    private final AtomicBoolean destroyed;
    
    private final List<InjectionFactory<T>> injectionFactories;
    
    private final EventFactory eventFactory;
    private final ConcurrentEventContext concurrentEvents;
    private final Map<Class<? extends Event>, EventContext> events;
    private final ConcurrentMap<Class<? extends Event>, EventContext> cachedEvents;
    
    private final InvocationFactory invocationFactory;
    private final ConcurrentInvocationContext concurrentInvocations;
    private final Map<Method, InvocationContext> invocations;
    
    private final List<EventInterceptor> eventInterceptors;
    private final List<InvocationInterceptor> invocationInterceptors;
    
    private final List<Lifecycle> lifecycles;
    private final List<InvocationInterceptor> onCreateInterceptors;
    private final List<InvocationInterceptor> onDestroyInterceptors;
    
    private final ThreadLocal<JargoStack<AtomicReference<Object>>> contextStack;
    
    private final boolean vanillaProxy;
    private final ProxyController proxyController;
    private final Class<T> type;
    private final T instance;
    private final Reference<T> proxyInstance;
    
    public ComponentObjectImpl(ManagedComponentContext<T> ctx, 
            ComponentConfiguration<T> configuration, 
            ComponentRegistry registry) throws ComponentCreationException {
        String componentName = ctx.getComponentMetaData().getComponentName();
        assert configuration.getComponentName().equals(componentName);
        
        this.logger = Logger.getLogger(getClass().getName());
        this.ctx = ctx;
        this.configuration = configuration;
        this.registry = registry;
        this.providers = registry.getProviders();
        this.destroyed = new AtomicBoolean();
        
        this.injectionFactories = providers.getInjectionFactoryProvider().
                getInjectionFactories(configuration);
        this.eventFactory = registry.getEventFactory(configuration);
        this.events = new HashMap<Class<? extends Event>, EventContext>();
        this.cachedEvents = new ConcurrentHashMap<Class<? extends Event>, EventContext>();
        
        this.eventInterceptors = new ArrayList<EventInterceptor>();
        this.invocationFactory = registry.getInvocationFactory(configuration);
        this.invocations = new HashMap<Method, InvocationContext>();
        
        ComponentMetaData<T> metaData = ctx.getComponentMetaData();
        this.type = metaData.getType();
        
        this.invocationInterceptors = new ArrayList<InvocationInterceptor>();
        this.lifecycles = new ArrayList<Lifecycle>();
        this.onCreateInterceptors = new ArrayList<InvocationInterceptor>();
        this.onDestroyInterceptors = new ArrayList<InvocationInterceptor>();
        
        this.vanillaProxy = metaData.isVanilla() && metaData.isProxy();
        
        ConstructorInjection<T> constructorInjection = null;
        for (InjectionFactory<T> injectionFactory : injectionFactories) {
            constructorInjection = injectionFactory.getConstructorInjection(
                    type, ctx);
            if (constructorInjection != null) {
                break;
            }
        }
        
        final Object uncheckedInstance;
        if (constructorInjection != null) {
            if (isVanillaProxy()) {
                // ComponentObjectImpl may never maintain a strong
                // reference to the ComponentReference instance.
                ComponentReference<T> reference = ctx.reference();
                assert reference != null;
                VanillaProxyGenerator<T> proxyGenerator = 
                        new VanillaProxyGenerator<T>(
                        reference, configuration, registry);
                proxyController = proxyGenerator.getProxyController();
                uncheckedInstance = constructorInjection.inject(proxyGenerator);
            } else {
                proxyController = null;
                uncheckedInstance = constructorInjection.inject();
            }
        } else {
            if (isVanillaProxy()) {
                // ComponentObjectImpl may never maintain a strong
                // reference to the ComponentReference instance.
                ComponentReference<T> reference = ctx.reference();
                assert reference != null;
                VanillaProxyGenerator<T> proxyGenerator = 
                        new VanillaProxyGenerator<T>(
                        reference, configuration, registry);
                proxyController = proxyGenerator.getProxyController();
                uncheckedInstance = providers.getObjectFactoryProvider().
                        getObjectFactory(configuration).newInstance(proxyGenerator);
            } else {
                proxyController = null;
                uncheckedInstance = providers.getObjectFactoryProvider().
                        getObjectFactory(configuration).newInstance();
            }
        }
        if (uncheckedInstance == null) {
            throw new ComponentCreationException(componentName,
                    "Instance is null.");
        }
        if (!type.isAssignableFrom(uncheckedInstance.getClass())) {
            throw new ComponentCreationException(componentName,
                    "Instance cannot be assigned to component type. " +
                    "Instance type: " + uncheckedInstance.getClass() +
                    ". Component type: " + type + ".");
        }
        
        @SuppressWarnings("unchecked")
        T tmp = (T) uncheckedInstance;
        if (isVanillaProxy()) {
            this.instance = null;
            this.proxyInstance = new WeakReference<T>(tmp);
        } else {
            this.instance = tmp;
            this.proxyInstance = null;
        }
        this.concurrentEvents = new ConcurrentEventInterceptorContextImpl();
        this.concurrentInvocations = new ConcurrentInvocationInterceptorContextImpl();
        
        if (metaData.isVanilla()) {
            ComponentReference<T> reference = ctx.reference().weakReference();
            assert reference instanceof WeakComponentReference;
            ((WeakComponentReference<T>) reference).setComponent(getInstance());
        }
        
        create();
        if (invocationInterceptors.isEmpty()) {
            contextStack = null;
        } else {
            this.contextStack = new ThreadLocal<JargoStack<AtomicReference<Object>>>() {
                @Override
                protected JargoStack<AtomicReference<Object>> initialValue() {
                    return new JargoStack<AtomicReference<Object>>();
                }
            };
        }
    }
    
    private void create() throws ComponentCreationException {
        try {
            if (isVanillaProxy()) {
                proxyController.attach(true);
            }
            ComponentMetaData<T> metaData = ctx.getComponentMetaData();
            String componentName = metaData.getComponentName();
            IdentityHashMap<EventInterceptor, Object> tmpEventInterceptors =
                    new IdentityHashMap<EventInterceptor, Object>();
            for (Class<? extends Event> tmp : eventFactory.getEventTypes()) {
                for (Class<?> cls : events.keySet()) {
                    if (cls.isAssignableFrom(tmp) || tmp.isAssignableFrom(cls)) {
                        throw new ComponentCreationException(componentName,
                                "Ambiguous event types: " +
                                tmp + ", " + cls + ".");
                    }
                }
                List<EventInterceptor> interceptors =
                        new ArrayList<EventInterceptor>();
                if (metaData.isProxy()) {
                    // Interceptors only supported for proxy components.
                    for (EventInterceptorFactory factory : providers.
                            getEventInterceptorFactoryProvider().
                            getEventInterceptorFactories(configuration, tmp)) {
                        ConstructorInjection<T> injection = null;
                        for (InjectionFactory<T> f : injectionFactories) {
                            injection = f.getConstructorInjection(
                                    factory.getType(), ctx);
                            if (injection != null) {
                                break;
                            }
                        }
                        interceptors.addAll(factory.getEventInterceptors(
                                getInstance(), injection, proxyController));
                    }
                    for (EventInterceptor interceptor : interceptors) {
                        if (tmpEventInterceptors.put(interceptor, DUMMY) == null) {
                            eventInterceptors.add(interceptor);
                        }
                    }
                }
                events.put(tmp, EventInterceptorChain.
                        instance(interceptors, concurrentEvents, ctx));
            }
            
            IdentityHashMap<InvocationInterceptor, Object> tmpInvocationInterceptors =
                    new IdentityHashMap<InvocationInterceptor, Object>();
            for (Method method : invocationFactory.getMethods()) {
                List<InvocationInterceptor> interceptors =
                        new ArrayList<InvocationInterceptor>();
                if (metaData.isProxy()) {
                    // Interceptors only supported for proxy components.
                    for (InvocationInterceptorFactory factory : providers.
                            getInvocationInterceptorFactoryProvider().
                            getInvocationInterceptorFactories(configuration,
                            method)) {
                        ConstructorInjection<T> injection = null;
                        for (InjectionFactory<T> f : injectionFactories) {
                            injection = f.getConstructorInjection(
                                    factory.getType(), ctx);
                            if (injection != null) {
                                break;
                            }
                        }
                        interceptors.addAll(factory.getInvocationInterceptors(
                                getInstance(), injection, proxyController));
                    }
                    for (InvocationInterceptor interceptor : interceptors) {
                        if (tmpInvocationInterceptors.put(interceptor, DUMMY) == null) {
                            invocationInterceptors.add(interceptor);
                        }
                    }
                }
                invocations.put(method, InvocationInterceptorChain.
                        instance(interceptors, concurrentInvocations, ctx));
            }
            invocations.put(null, concurrentInvocations);
            lifecycles.addAll(providers.getLifecycleProvider().
                    getLifecycles(configuration, 
                    registry.getExecutorHandle(configuration).
                    getExecutor()));
            onCreateInterceptors.addAll(getOnCreateInterceptors());
            onDestroyInterceptors.addAll(getOnDestroyInterceptors());
            
            List<Object> instances = new ArrayList<Object>();
            IdentityHashMap<Object, Object> identities =
                    new IdentityHashMap<Object, Object>();
            identities.put(getInstance(), DUMMY);
            for (InvocationInterceptor i : onCreateInterceptors) {
                Object o = getInstanceFromInterceptor(i);
                if (identities.put(o, DUMMY) == null) {
                    instances.add(o);
                }
            }
            for (InvocationInterceptor i : onDestroyInterceptors) {
                Object o = getInstanceFromInterceptor(i);
                if (identities.put(o, DUMMY) == null) {
                    instances.add(o);
                }
            }
            for (EventInterceptor i : getEventInterceptors()) {
                Object o = getInstanceFromInterceptor(i);
                if (identities.put(o, DUMMY) == null) {
                    instances.add(o);
                }
            }
            for (InvocationInterceptor i : getInvocationInterceptors()) {
                Object o = getInstanceFromInterceptor(i);
                if (identities.put(o, DUMMY) == null) {
                    instances.add(o);
                }
            }
            
            for (Object o : instances) {
                init(o);
            }
            
            init(getInstance());
            
            final String name;
            if (metaData.isStatic()) {
                name = "'" + ctx.getComponentMetaData().getComponentName() + "'";
            } else {
                name = "'" + ctx.getComponentMetaData().getComponentName() +
                        "' @" + Integer.toHexString(ctx.reference().hashCode());
            }

            logger.finest(name + " onCreate(" + onCreateInterceptors.size() + ").");
            if (!onCreateInterceptors.isEmpty()) {
                InvocationInterceptorChain.instance(onCreateInterceptors, 
                        new LifecycleTerminator(), ctx).proceed();
            }
        } catch (ComponentCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentCreationException(
                    ctx.getComponentMetaData().getComponentName(), e);
        } finally {
            if (isVanillaProxy()) {
                proxyController.detach();
            }
        }
    }
    
    public T getInstance() {
        if (isVanillaProxy()) {
            T tmp = proxyInstance.get();
            assert tmp != null;
            return tmp;
        } else {
            return instance;
        }
    }
    
    private Object getInstanceFromInterceptor(EventInterceptor interceptor) {
        final Object o;
        if (interceptor instanceof EventInterceptorAdapter) {
            o = ((EventInterceptorAdapter) interceptor).getInstance();
        } else {
            o = interceptor;
        }
        return o;
    }
    
    private Object getInstanceFromInterceptor(InvocationInterceptor interceptor) {
        final Object o;
        if (interceptor instanceof InvocationInterceptorAdapter) {
            o = ((InvocationInterceptorAdapter) interceptor).getInstance();
        } else {
            o = interceptor;
        }
        return o;
    }
    
    private List<EventInterceptor> getEventInterceptors() {
        return eventInterceptors;
    }
    
    private List<InvocationInterceptor> getInvocationInterceptors() {
        return invocationInterceptors;
    }
    
    private EventContext getEventInterceptorContext(Event event) {
        Class<? extends Event> cls = event.getClass();
        EventContext tmp = cachedEvents.get(cls);
        if (tmp == null) {
            for (Map.Entry<Class<? extends Event>, EventContext> entry :
                events.entrySet()) {
                if (entry.getKey().isAssignableFrom(event.getClass())) {
                    tmp = entry.getValue();
                    break;
                }
            }
            cachedEvents.put(cls, tmp);
        }
        assert tmp != null;
        return tmp;
    }
    
    private InvocationContext getInvocationInterceptorContext(Method method) {
        return invocations.get(method);
    }
    
    public Object execute(Event event) throws
            ComponentEventException {
        EventContext ectx = getEventInterceptorContext(event);
        concurrentEvents.attach(event);
        try {
            try {
                if (isVanillaProxy()) {
                    proxyController.attach(true);
                }
                return ectx.proceed();
            } catch (ComponentEventException e) {
                throw e;
            } catch (Throwable t) {
                throw new ComponentExecutionException(
                        ctx.getComponentMetaData().getComponentName(), event, t);
            } finally {
                if (isVanillaProxy()) {
                    proxyController.detach();
                }
            }
        } finally {
            concurrentEvents.detach();
        }
    }
    
    public void destroy() {
        if (!destroyed.getAndSet(true)) {
            ComponentMetaData<T> metaData = ctx.getComponentMetaData();
            String componentName = metaData.getComponentName();
            try {
                final String name;
                if (metaData.isStatic()) {
                    name = "'" + componentName + "'";
                } else {
                    name = "'" + componentName + "' @" +
                            Integer.toHexString(ctx.reference().hashCode());
                }
                logger.finest(name + " onDestroy(" + onDestroyInterceptors.size() + ").");
                if (!onDestroyInterceptors.isEmpty()) {
                    InvocationInterceptorChain.instance(onDestroyInterceptors, 
                            new LifecycleTerminator(), ctx).proceed();
                }
            } catch (ComponentException e) {
                throw e;
            } catch (Exception e) {
                throw new ComponentException(componentName, e);
            }
            
            IdentityHashMap<Object, Object> identities =
                    new IdentityHashMap<Object, Object>();
            identities.put(getInstance(), DUMMY);
            for (InvocationInterceptor interceptor : getInvocationInterceptors()) {
                Object o = getInstanceFromInterceptor(interceptor);
                
                if (identities.put(o, DUMMY) == null) {
                    final String name;
                    if (metaData.isStatic()) {
                        name = componentName + " (" + o.getClass() + ")";
                    } else {
                        name = "'" + componentName + "' @" + Integer.toHexString(
                                ctx.reference().hashCode()) + " (" +
                                o.getClass() + ")";
                    }
                    for (Lifecycle lifecycle : lifecycles) {
                        List<Invocation> tmp = lifecycle.onDestroy(
                                o.getClass(), false);
                        if (!tmp.isEmpty()) {
                            logger.finest(name + " onDestroy(" + tmp.size() + ").");
                            for (Invocation invocation : tmp) {
                                try {
                                    invocation.invoke(o);
                                } catch (ComponentException e) {
                                    throw e;
                                } catch (Exception e) {
                                    throw new ComponentException(componentName, e);
                                }
                            }
                        }
                    }
                }
            }
            
            for (EventInterceptor interceptor : getEventInterceptors()) {
                Object o = getInstanceFromInterceptor(interceptor);
                if (identities.put(getInstance(), DUMMY) == null) {
                    final String name;
                    if (metaData.isStatic()) {
                        name = "'" + componentName + "' (" + o.getClass() + ")";
                    } else {
                        name = "'" + componentName + "' @" + Integer.toHexString(
                                ctx.reference().hashCode()) + " (" +
                                o.getClass() + ")";
                    }
                    for (Lifecycle lifecycle : lifecycles) {
                        List<Invocation> tmp = lifecycle.onDestroy(
                                type, false);
                        logger.finest(name + " onDestroy(" + tmp.size() + ").");
                        for (Invocation invocation : tmp) {
                            try {
                                invocation.invoke(getInstance());
                            } catch (ComponentException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new ComponentException(componentName, e);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void init(Object o) {
        ComponentMetaData<T> metaData = ctx.getComponentMetaData();
        String componentName = metaData.getComponentName();
        final String name;
        if (metaData.isStatic()) {
            name = "'" + componentName + "' (" + o.getClass() + ")";
        } else {
            name = "'" + componentName + "' @" + Integer.toHexString(
                    ctx.reference().hashCode()) + " (" + o.getClass() + ")";
        }
        logger.finest(name + " initializing.");
        
        final Class<?> initType;
        if (o == getInstance()) {
            // Could be a CGLib proxy; We don't want to pass the object's subtype.
            initType = type;
        } else {
            initType = o.getClass();
        }
        for (InjectionFactory<T> injectionFactory : injectionFactories) {
            for (SetterInjection injection : injectionFactory.getSetterInjections(
                    initType, ctx)) {
                final String cname;
                if (metaData.isStatic()) {
                    cname = "'" + componentName + "'";
                } else {
                    cname = "'" + componentName + "' @" + Integer.toHexString(
                            ctx.reference().hashCode());
                }
                logger.finest(cname + " injecting " + injection + ".");
                injection.inject(o);
            }
        }
        
        if (o != getInstance()) {
            for (Lifecycle lifecycle : lifecycles) {
                List<Invocation> tmp = lifecycle.onCreate(
                        o.getClass(), false);
                if (!tmp.isEmpty()) {
                    logger.finest(name + " onCreate(" + tmp.size() + ").");
                    for (Invocation invocation : tmp) {
                        try {
                            invocation.invoke(o);
                        } catch (ComponentException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new ComponentException(componentName, e);
                        }
                    }
                }
            }
        }
    }
    
    private List<InvocationInterceptor> getOnCreateInterceptors() {
        return new LifecycleInterceptorCreator() {
            public List<Invocation> getInvocations(Class<?> cls, Lifecycle lifecycle, boolean interceptor) {
                return lifecycle.onCreate(cls, interceptor);
            }
        }.create();
    }
    
    private List<InvocationInterceptor> getOnDestroyInterceptors() {
        return new LifecycleInterceptorCreator() {
            public List<Invocation> getInvocations(Class<?> cls, Lifecycle lifecycle, boolean interceptor) {
                return lifecycle.onDestroy(cls, interceptor);
            }
        }.create();
    }
    
    @Override
    public String toString() {
        return "ComponentObject{name=" + 
                ctx.getComponentMetaData().getComponentName() +
                ", instance=" + getInstance() + "}@" +
                Integer.toHexString(System.identityHashCode(this));
    }
    
    private final class ConcurrentEventInterceptorContextImpl extends
            ConcurrentEventContext {
        
        public ComponentContext getComponentContext() {
            return ctx;
        }
        
        public Object proceed() throws Exception {
            Invocation invocation = invocationFactory.getInvocation(getEvent());
            if (invocation == null) {
                throw new ComponentEventNotExecutableException(
                        ctx.getComponentMetaData().getComponentName(),
                        getEvent());
            }
            // Method can be null.
            Method method = invocation.getMethod();
            InvocationContext ictx = getInvocationInterceptorContext(method);
            if (ictx == null) {
                assert !invocationFactory.getMethods().contains(method) :
                    "No InvocationContext found for method in InvocationFactory.getMethods().";
                throw new ComponentEventException(
                        ctx.getComponentMetaData().getComponentName(), getEvent(),
                        "Method returned by InvocationFactory.getInvocation() is not" +
                        " in InvocationFactory.getMethods(): " + method + ".");
            }
            concurrentInvocations.attach(invocation);
            try {
                return ictx.proceed();
            } finally {
                concurrentInvocations.detach();
            }
        }
        
        @Override
        public void attach(Event event) {
            super.attach(event);
            if (contextStack != null) {
                contextStack.get().push(new AtomicReference<Object>());
            }
        }
        
        @Override
        public void detach() {
            super.detach();
            if (contextStack != null) {
                contextStack.get().pop();
            }
        }
        
        public Object get() {
            final AtomicReference<Object> ref;
            if (contextStack != null) {
                ref = contextStack.get().peek();
            } else {
                ref = null;
            }
            if (ref == null) {
                throw new IllegalStateException();
            }
            return ref.get();
        }
        
        public void set(Object o) {
            final AtomicReference<Object> ref;
            if (contextStack != null) {
                ref = contextStack.get().peek();
            } else {
                ref = null;
            }
            if (ref == null) {
                throw new IllegalStateException();
            }
            ref.set(o);
        }

        public Object getTarget() {
            return getInstance();
        }
    }
    
    private final class ConcurrentInvocationInterceptorContextImpl extends
            ConcurrentInvocationContext {
        
        public ComponentContext getComponentContext() {
            return ctx;
        }
        
        public Object proceed() throws Exception {
            return getInvocation().invoke(getInstance());
        }
        
        public Object get() {
            final AtomicReference<Object> ref;
            if (contextStack != null) {
                ref = contextStack.get().peek();
            } else {
                ref = null;
            }
            if (ref == null) {
                throw new IllegalStateException();
            }
            return ref.get();
        }
        
        public void set(Object o) {
            final AtomicReference<Object> ref;
            if (contextStack != null) {
                ref = contextStack.get().peek();
            } else {
                ref = null;
            }
            if (ref == null) {
                throw new IllegalStateException();
            }
            ref.set(o);
        }

        public Object getTarget() {
            return getInstance();
        }
    }
    
    private class LifecycleTerminator implements InvocationContext {
        private Object value;
        
        public Object proceed() throws Exception {
            return null;
        }
        
        public Invocation getInvocation() {
            return null;
        }
        
        public ComponentContext getComponentContext() {
            return ctx;
        }
        
        public Object get() {
            return value;
        }
        
        public void set(Object o) {
            this.value = o;
        }
        
        public Object getTarget() {
            return getInstance();
        }
    }
    
    private abstract class LifecycleInterceptorCreator {
        
        public abstract List<Invocation> getInvocations(Class<?> cls, Lifecycle lifecycle, boolean interceptor);
        
        public List<InvocationInterceptor> create() {
            List<InvocationInterceptor> lifecycleInterceptors =
                    new ArrayList<InvocationInterceptor>();
            for (Lifecycle lifecycle : lifecycles) {
                IdentityHashMap<Object, Object> identities =
                        new IdentityHashMap<Object, Object>();
                
                for (InvocationInterceptor interceptor : getInvocationInterceptors()) {
                    if (interceptor.isLifecycleInterceptor()) {
                        final Object interceptorInstance;
                        if (interceptor instanceof InvocationInterceptorAdapter) {
                            interceptorInstance = ((InvocationInterceptorAdapter) interceptor).
                                    getInstance();
                        } else {
                            interceptorInstance = interceptor;
                        }
                        if (interceptorInstance != getInstance()) {
                            if (identities.put(interceptorInstance, DUMMY) == null) {
                                for (Invocation invocation : getInvocations(
                                        interceptorInstance.getClass(), lifecycle, true)) {
                                    lifecycleInterceptors.add(
                                            transformInterceptor(invocation, 
                                            interceptorInstance));
                                }
                            }
                        }
                    }
                }
                for (Invocation invocation : getInvocations(type, lifecycle, false)) {
                    lifecycleInterceptors.add(transform(invocation));
                }
            }
            return lifecycleInterceptors;
        }
        
        private InvocationInterceptor transform(final Invocation invocation) {
            return new InvocationInterceptorAdapter() {
                public Object getInstance() {
                    return ComponentObjectImpl.this.getInstance();
                }
                public boolean isLifecycleInterceptor() {
                    // Method does not apply to these wrapped interceptors.
                    return false;
                }
                public Object intercept(InvocationContext ctx) throws Exception {
                    invocation.invoke(getInstance());
                    ctx.proceed();
                    return null;
                }
                @Override
                public String toString() {
                    return "InvocationInterceptorAdapter{instance=" + 
                            getInstance() + ", invocation=" + invocation + "}";
                }
            };
        }
        private InvocationInterceptor transformInterceptor(
                final Invocation invocation, final Object instance) {
            return new InvocationInterceptorAdapter() {
                public Object getInstance() {
                    return instance;
                }
                public boolean isLifecycleInterceptor() {
                    // Method does not apply to these wrapped interceptors.
                    return false;
                }
                public Object intercept(InvocationContext ctx) throws Exception {
                    invocation.setParameters(new Object[]{ctx});
                    return invocation.invoke(getInstance());
                }
                @Override
                public String toString() {
                    return "InvocationInterceptorAdapter{instance=" + getInstance() + ", invocation=" + invocation + "}";
                }
            };
        }
    }
    
    public boolean isVanillaProxy() {
        return vanillaProxy;
    }
}
