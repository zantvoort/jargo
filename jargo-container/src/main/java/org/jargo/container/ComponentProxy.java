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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentException;
import org.jargo.ComponentReference;
import org.jargo.Event;
import org.jargo.EventFactory;
import org.jargo.ComponentExceptionHandler;
import org.jargo.ComponentMetaData;

/**
 * @author Leon van Zantvoort
 */
final class ComponentProxy<T> implements InvocationHandler {

    private static final Method	HASH_CODE;
    private static final Method	EQUALS;
    private static final Method	TO_STRING;
    
    static {
        try {
            Class<?> type = Object.class;
            HASH_CODE = type.getMethod("hashCode");
            EQUALS = type.getMethod("equals", new Class[]{type});
            TO_STRING = type.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    public static <T> T getComponentProxy(ComponentReference<T> reference,
            ComponentRegistry registry) {
        ComponentMetaData<T> metaData = reference.getComponentMetaData();
        
        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newProxyInstance(metaData.getComponentUnit().
                getClassLoader(), metaData.getInterfaces().toArray(new Class[0]), 
                new ComponentProxy(reference, registry));
        return proxy;
    }
    
    private final ComponentReference<T> reference;
    private final EventFactory eventFactory;
    private final ComponentExceptionHandler exceptionHandler;
    
    private ComponentProxy(ComponentReference<T> reference, 
            ComponentRegistry registry) {
        String name = reference.getComponentMetaData().getComponentName();
        this.reference = reference;
        ComponentConfiguration configuration = registry.
                getComponentConfiguration(name);
        this.eventFactory = registry.getEventFactory(configuration);
        this.exceptionHandler = registry.getComponentExceptionHandler(configuration);
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws
            Throwable {
        if (method.equals(HASH_CODE)) {
            return reference.hashCode();
        } else if (method.equals(EQUALS)) {
            Object o = args[0];
            if (o != null) {
                if (Proxy.isProxyClass(o.getClass())) {
                    InvocationHandler h = Proxy.getInvocationHandler(o);
                    if (h instanceof ComponentProxy) {
                        return reference.equals(((ComponentProxy) h).reference);
                    }
                }
            }
            return false;
        } else if (method.equals(TO_STRING)) {
            return toString();
        }
        
        try {
            Event event = eventFactory.getEvent(method, args);
            return reference.execute(event);
        } catch (ComponentException e) {
            exceptionHandler.onException(method, e);
            return null;
        }
    }
    
    public String toString() {
        return "ComponentProxy{name=" + reference.getComponentMetaData().getComponentName() + 
                "}@" + Integer.toHexString(reference.hashCode());
    }
}
