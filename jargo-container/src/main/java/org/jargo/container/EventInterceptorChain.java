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
import java.util.ListIterator;
import org.jargo.ComponentContext;
import org.jargo.Event;
import org.jargo.EventInterceptor;
import org.jargo.EventContext;

/**
 * @author Leon van Zantvoort
 */
final class EventInterceptorChain implements EventContext {
    
    public static EventContext instance(List<EventInterceptor> interceptors,
            ComponentContext ctx) {
        return instance(interceptors, 
                new EventInterceptorChain(null, null, ctx), ctx);
    }
    
    public static EventContext instance(
            List<EventInterceptor> interceptors,
            EventContext terminator, ComponentContext ctx) {
        EventContext ectx = terminator;
        for (ListIterator<EventInterceptor> i = interceptors.
                listIterator(interceptors.size()); i.hasPrevious();) {
            ectx = new EventInterceptorChain(i.previous(), ectx, ctx);
        }
        return ectx;
    }
    
    private final EventInterceptor interceptor;
    private final EventContext next;
    private final ComponentContext ctx;
    
    private EventInterceptorChain(EventInterceptor interceptor, 
            EventContext next, ComponentContext ctx) {
        if (next == null && interceptor != null) {
            throw new IllegalStateException();
        }
        this.interceptor = interceptor;
        this.next = next;
        this.ctx = ctx;
    }
    
    public Object get() {
        if (next == null) {
            throw new IllegalStateException();
        }
        return next.get();
    }
    
    public void set(Object o) {
        if (next == null) {
            throw new IllegalStateException();
        }
        next.set(o);
    }

    public Event getEvent() {
        return next == null ? null : next.getEvent();
    }
    
    public Object proceed() throws Exception {
        return interceptor == null ? null : interceptor.intercept(next);
    }

    public Object getTarget() {
        return next == null ? null : next.getTarget();
    }
    
    public ComponentContext getComponentContext() {
        return ctx;
    }
}
