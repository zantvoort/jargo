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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jargo.Invocation;
import org.jargo.Lifecycle;

/**
 *
 * @author Leon van Zantvoort
 */
final class Lifecycles implements Lifecycle {

    private final Lifecycle lifecycle;
    private final Map<Class, List<Invocation>> createCache;
    private final Map<Class, List<Invocation>> createInterceptorCache;
    private final Map<Class, List<Invocation>> destroyCache;
    private final Map<Class, List<Invocation>> destroyInterceptorCache;
    
    public Lifecycles(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        
        // No need for weak references / clearing caches.
        createCache = new ConcurrentHashMap<Class, List<Invocation>>();
        createInterceptorCache = new ConcurrentHashMap<Class, List<Invocation>>();
        destroyCache = new ConcurrentHashMap<Class, List<Invocation>>();
        destroyInterceptorCache = new ConcurrentHashMap<Class, List<Invocation>>();
    }

    public List<Invocation> onCreate(Class<?> cls, boolean interceptor) {
        final Map<Class, List<Invocation>> cache;
        if (interceptor) {
            cache = createInterceptorCache;
        } else {
            cache = createCache;
        }
        List<Invocation> list = cache.get(cls);
        if (list == null) {
            list = lifecycle.onCreate(cls, interceptor);
            cache.put(cls, list);
        }
        return list;
    }

    public List<Invocation> onDestroy(Class<?> cls, boolean interceptor) {
        final Map<Class, List<Invocation>> cache;
        if (interceptor) {
            cache = destroyInterceptorCache;
        } else {
            cache = destroyCache;
        }
        List<Invocation> list = cache.get(cls);
        if (list == null) {
            list = lifecycle.onDestroy(cls, interceptor);
            cache.put(cls, list);
        }
        return list;
    }
}
