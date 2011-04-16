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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jargo.Event;
import org.jargo.Invocation;
import org.jargo.InvocationFactory;
import org.jargo.spi.InvocationFactoryProvider;
import org.jargo.ComponentConfiguration;

/**
 * @author Leon van Zantvoort
 */
final class InvocationFactories implements InvocationFactory {
    
    private final ComponentConfiguration configuration;
    private final InvocationFactoryProvider provider;
    private final ConcurrentMap<Class<? extends Event>, InvocationFactory> 
            factories;
    
    private final Set<Method> methods;
    private final Set<Class<? extends Event>> eventTypes;
    
    public InvocationFactories(ComponentConfiguration configuration, Providers providers) {
        this.configuration = configuration;
        this.provider = providers.getInvocationFactoryProvider();
        this.factories = new ConcurrentHashMap<Class<? extends Event>, 
                InvocationFactory>();
        
        Set<Method> m = new LinkedHashSet<Method>();
        for (InvocationFactory factory : provider.getInvocationFactories(configuration)) {
            m.addAll(factory.getMethods());
        }
        this.methods = Collections.unmodifiableSet(m);
        
        Set<Class<? extends Event>> s = 
                new LinkedHashSet<Class<? extends Event>>();
        for (InvocationFactory factory : provider.getInvocationFactories(configuration)) {
            s.addAll(factory.getEventTypes());
        }
        this.eventTypes = Collections.unmodifiableSet(s);
    }

    public Set<Method> getMethods() {
        return methods;
    }

    public Set<Class<? extends Event>> getEventTypes() {
        return eventTypes;
    }
    
    public Invocation getInvocation(Event event) {
        InvocationFactory factory = factories.get(event.getClass());
        if (factory == null) {
            for (InvocationFactory f : provider.getInvocationFactories(configuration)) {
                for (Class<? extends Event> type : f.getEventTypes()) {
                    if (type.isAssignableFrom(event.getClass())) {
                        factories.put(event.getClass(), f);
                        return f.getInvocation(event);
                    }
                }
            }
            return null;
        } else {
            return factory.getInvocation(event);
        }
    }
}
