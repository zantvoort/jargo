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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.jargo.ComponentRegistration;
import org.jargo.Event;
import org.jargo.EventExecutor;
import org.jargo.spi.EventExecutorProvider;
import org.jargo.ComponentConfiguration;
import org.jargo.deploy.Deployable;

/**
 *
 * @author Leon van Zantvoort
 */
final class EventExecutorProviders extends 
        AbstractProviders<EventExecutorProvider> implements 
        EventExecutorProvider {
    
    private final Map<ComponentConfiguration, Map<Class, EventExecutor>> cache;
    
    public EventExecutorProviders() {
        cache = Collections.synchronizedMap(new HashMap<ComponentConfiguration, Map<Class, EventExecutor>>());
    }
    
    public <T> EventExecutor<T> getEventExecutor(
            ComponentConfiguration<T> configuration, 
            Class<? extends Event> type, Executor executor) {
        EventExecutor<T> eventExecutor = null;
        Map<Class, EventExecutor> map = cache.get(configuration);
        if (map == null) {
            map = Collections.synchronizedMap(new HashMap<Class, EventExecutor>());
            cache.put(configuration, map);
        }
        if (map.containsKey(type)) {
            @SuppressWarnings("unchecked")
            EventExecutor<T> tmp = (EventExecutor<T>) map.get(type);
            eventExecutor = tmp;
        } else {
            for (EventExecutorProvider provider : getProviders()) {
                eventExecutor = provider.getEventExecutor(configuration, type, executor);
                if (eventExecutor != null) {
                    break;
                }
            }
            map.put(type, eventExecutor);
        }
        return eventExecutor;
    }

    @Override
    public void undeploy(Deployable deployable) throws Exception {
        super.undeploy(deployable);
        if (deployable instanceof ComponentRegistration) {
            List<ComponentConfiguration<?>> configurations = 
                    ((ComponentRegistration) deployable).getComponentConfigurations();
            cache.keySet().removeAll(configurations);
        }
    }
}
