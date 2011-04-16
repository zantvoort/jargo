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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentEventException;
import org.jargo.ComponentEventNotExecutableException;
import org.jargo.ComponentReference;
import org.jargo.ComponentReferenceLifecycle;
import org.jargo.ComponentException;
import org.jargo.ComponentMetaData;
import org.jargo.ComponentObjectFactory;
import org.jargo.Event;
import org.jargo.EventExecutor;
import org.jargo.InvocationFactory;

/**
 * @author Leon van Zantvoort
 */
final class WeakComponentReference<T> implements ComponentReference<T> {
    
    private final ManagedComponentContext<T> ctx;
    private final ComponentRegistry registry;
    private final Object info;
    private final ComponentConfiguration<T> configuration;
    private final ComponentObjectFactory<T> objectFactory;
    private final AtomicBoolean removed;
    private final AtomicBoolean remove;
    private final InvocationFactory invocationFactory;
    private final List<ComponentReferenceLifecycle<T>> lifecycles;
    private final Lock lock;
    private final AtomicBoolean initialized;
    private final Map<Class<? extends Event>, EventExecutor<T>>
            eventExecutors;
    private final ConcurrentMap<Class<? extends Event>, EventExecutor<T>> 
            cachedEventExecutors;
    private final Destroyer destroyer;

    private Object component;
    
    public WeakComponentReference(ManagedComponentContext<T> ctx, 
            ComponentConfiguration<T> configuration, ComponentRegistry registry, 
            Object info, Destroyer destroyer) {
        this.ctx = ctx;
        this.registry = registry;
        this.info = info;
        this.destroyer = destroyer;
        this.configuration = configuration;
        this.removed = new AtomicBoolean();
        this.remove = new AtomicBoolean();
        this.invocationFactory = registry.getInvocationFactory(configuration);
        this.objectFactory = registry.getComponentObjectFactory(configuration);
        this.lifecycles = new ArrayList<ComponentReferenceLifecycle<T>>();
        this.eventExecutors = new HashMap<Class<? extends Event>, EventExecutor<T>>();
        this.cachedEventExecutors = 
                new ConcurrentHashMap<Class<? extends Event>, EventExecutor<T>>();
        Set<Class<? extends Event>> events = 
                registry.getEventFactory(configuration).getEventTypes();
        for (Class<? extends Event> type : events) {
            EventExecutor<T> executor = registry.getProviders().
                    getEventExecutorProvider().
                    getEventExecutor(configuration, type,
                    registry.getExecutorHandle(configuration).
                    getExecutor());
            if (executor == null) {
                throw new ComponentException(getName(), 
                        "no event executor specified for " + type.getName());
            }
            eventExecutors.put(type, executor);
        }
        this.lock = new ReentrantLock(true);
        this.initialized = new AtomicBoolean();
    }

    @Override
    protected void finalize() {
        // The finalize method is not guaranteed to be called.
        invalidate();
    }
    
    public void setComponent(Object component) {
        this.component = component;
    }
    
    public void init(ComponentReference<T> ref) {
        try {
            for(ComponentReferenceLifecycle<T> lifecycle : registry.getProviders().
                    getComponentReferenceLifecycleProvider().
                    getComponentReferenceLifecycles(configuration, 
                    registry.getExecutorHandle(configuration).getExecutor())) {
                lifecycle.onCreate(ref);
                lifecycles.add(0, lifecycle);
            }
        } finally {
            final boolean doRemove;
            lock.lock();
            try {
                initialized.set(true);
                doRemove = remove.get();
            } finally {
                lock.unlock();
            }
            if (doRemove) {
                doRemove();
            }
        }
    }

    private String getName() {
        return getComponentMetaData().getComponentName();
    }
    
    public ComponentMetaData<T> getComponentMetaData() {
        return ctx.getComponentMetaData();
    }
    
    public Object getInfo() {
        return info;
    }
    
    public boolean isWeak() {
        return weakReference() == this;
    }
    
    public ComponentReference<T> weakReference() {
        return this;
    }
    
    public Object getComponent() {
        return component;
    }

    public boolean isExecutable(Event event) {
        EventExecutor executor = getEventExecutor(event);
        return !isRemoved() && executor != null && 
                invocationFactory.getInvocation(event) != null;
    }

    public Object execute(Event event) throws ComponentEventException {
        return doExecute(event, this);
    }
    
    Object doExecute(Event event, ComponentReference<T> reference) throws 
            ComponentEventException {
        if (isRemoved()) {
            throw new ComponentEventException(getName(), event,
                    "Reference is removed.");
        }
        EventExecutor<T> executor = getEventExecutor(event);
        if (executor == null) {
            throw new ComponentEventNotExecutableException(getName(), event);
        }
        ctx.attach(reference);
        try {
            return executor.execute(event, objectFactory);
        } finally {
            ctx.detach();
        }
    }
    
    private void doRemove() {
        try {
            for (ComponentReferenceLifecycle<T> lifecycle : lifecycles) {
                lifecycle.onDestroy(this);
            }
        }  finally {
            try {
                if (!objectFactory.isStatic()) {
                    ctx.attach(this);
                    try {
                        objectFactory.remove();
                    } finally {
                        ctx.detach();
                    }
                }
            } finally {
                try {
                    destroyer.destroy();
                } finally {
                    removed.set(true);
                }
            }
        }
    }
    
    public void invalidate() {
        final boolean doRemove;
        lock.lock();
        try {
            doRemove = !remove.getAndSet(true) && initialized.get();
        } finally {
            lock.unlock();
        }
        if (doRemove) {
            doRemove();
        }
    }

    public boolean isValid() {
        return !remove.get();
    }
    
    public void remove() {
        removed.set(true);
    }
    
    public boolean isRemoved() {
        return removed.get();
    }

    private EventExecutor<T> getEventExecutor(Event event) {
        Class<? extends Event> cls = event.getClass();
        EventExecutor<T> executor = cachedEventExecutors.get(cls);
        if (executor == null) {
            for (Map.Entry<Class<? extends Event>, EventExecutor<T>> entry :
                    eventExecutors.entrySet()) {
                if (entry.getKey().isAssignableFrom(cls)) {
                    executor = entry.getValue();
                    break;
                }
            }
            if (executor != null) {
                cachedEventExecutors.put(cls, executor);
            }
        }
        return executor;
    }

    public void addDestroyHook(Runnable hook) {
        destroyer.addDestroyHook(hook);
    }
    
    public boolean removeDestroyHook(Runnable hook) {
        return destroyer.removeDestroyHook(hook);
    }

    public String toString() {
        return "ComponentReference{name=" + getName() + ", valid=" + isValid() + 
                ", removed=" + isRemoved() + "}@" + 
                Integer.toHexString(hashCode());
    }
}    
