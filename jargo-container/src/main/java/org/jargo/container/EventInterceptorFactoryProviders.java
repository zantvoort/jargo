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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jargo.Event;
import org.jargo.spi.EventInterceptorFactoryProvider;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentRegistration;
import org.jargo.EventInterceptorFactory;
import org.jargo.deploy.Deployable;

/**
 *
 * @author Leon van Zantvoort
 */
final class EventInterceptorFactoryProviders extends 
        AbstractProviders<EventInterceptorFactoryProvider> implements 
        EventInterceptorFactoryProvider {
    
    private final Map<ComponentConfiguration, Map<Class, List<EventInterceptorFactory>>> cache;
    
    public EventInterceptorFactoryProviders() {
        cache = Collections.synchronizedMap(new HashMap<ComponentConfiguration, Map<Class, List<EventInterceptorFactory>>>());
    }
    
    public List<EventInterceptorFactory> getEventInterceptorFactories(
            ComponentConfiguration<?> configuration, 
            Class<? extends Event> type) {
        Map<Class, List<EventInterceptorFactory>> map = cache.get(configuration);
        if (map == null) {
            map = Collections.synchronizedMap(new HashMap<Class, List<EventInterceptorFactory>>());
            cache.put(configuration, map);
        }
        List<EventInterceptorFactory> factories = map.get(type);
        if (factories == null) {
            factories = new ArrayList<EventInterceptorFactory>();
            for (EventInterceptorFactoryProvider provider : getProviders()) {
                factories.addAll(provider.getEventInterceptorFactories(configuration, type)); 
            }
            factories = Collections.unmodifiableList(factories);
            map.put(type, factories);
        }
        return factories;
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
