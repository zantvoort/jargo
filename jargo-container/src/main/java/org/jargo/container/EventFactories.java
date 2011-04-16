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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jargo.Event;
import org.jargo.EventFactory;
import org.jargo.ComponentConfiguration;

/**
 * @author Leon van Zantvoort
 */
final class EventFactories implements EventFactory {

    private final List<EventFactory> factories;
    private final ConcurrentMap<Method, EventFactory> cache;
    
    private final List<Class<?>> interfaces;
    private final Set<Class<? extends Event>> eventTypes;
    private final boolean proxy;
    
    public EventFactories(ComponentConfiguration configuration, Providers providers) {
        this.factories = providers.getEventFactoryProvider().
                getEventFactories(configuration);
        this.cache = new ConcurrentHashMap<Method, EventFactory>();

        CopyOnWriteArrayList<Class<?>> l = new CopyOnWriteArrayList<Class<?>>();
        Set<Class<? extends Event>> s = 
                new LinkedHashSet<Class<? extends Event>>();
        boolean tmp = false;
        for (EventFactory factory : factories) {
            l.addAllAbsent(factory.getInterfaces());
            if (factory.isProxy()) {
                tmp = true;
            }
            s.addAll(factory.getEventTypes());
        }
        this.interfaces = Collections.unmodifiableList(l);
        this.proxy = tmp;
        this.eventTypes = Collections.unmodifiableSet(s);
    }

    public List<Class<?>> getInterfaces() {
        return interfaces;
    }

    public boolean isProxy() {
        return proxy;
    }

    public Set<Class<? extends Event>> getEventTypes() {
        return eventTypes;
    }

    public Event getEvent(Method method, Object[] args) {
        final Event event;
        EventFactory factory = cache.get(method);
        if (factory == null) {
            Event tmp = null;
            for (EventFactory f : factories) {
                tmp = f.getEvent(method, args);
                if (tmp != null) {
                    cache.put(method, f);
                    break;
                }
            }
            event = tmp;
        } else {
            event = factory.getEvent(method, args);
        }
        return event;
    }
}
