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
import java.util.Map;
import java.util.WeakHashMap;
import net.sf.cglib.core.CodeGenerationException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.InvocationHandler;
import org.jargo.ComponentApplicationException;

final class CGLibProxy {

    // PENDING: Check if these weak reference will be cleared.
    private static Map<Class, Object> generatedClasses = 
            Collections.synchronizedMap(new WeakHashMap<Class, Object>());
    
    public static void isSupported() throws Error {
        new Enhancer();
    }
    
    public static Class getProxyClass(ClassLoader loader, Class[] interfaces) {
        return getProxyClass(loader, null, interfaces);
    }

    public static Class getProxyClass(ClassLoader loader, Class type, 
            Class[] interfaces) {
        Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(loader);
        enhancer.setSuperclass(type);
        enhancer.setInterfaces(interfaces);
        enhancer.setUseCache(true);
        enhancer.setUseFactory(true);
        Class cls = enhancer.createClass();
        generatedClasses.put(cls, null);
        return cls;
    }
    
    public static boolean isProxyClass(Class<?> cls) {
        return generatedClasses.containsKey(cls);
    }
    
    public static java.lang.reflect.InvocationHandler getInvocationHandler(
            Object proxy) {
        if (!isProxyClass(proxy.getClass())) {
            throw new ComponentApplicationException("Not a proxy instance: " + 
                    proxy.getClass());
        }
        return ((HandlerAdapter) ((Factory) proxy).getCallback(0)).handler;
    }
    
    public static Object newProxyInstance(ClassLoader loader, 
            Class<?>[] interfaces, java.lang.reflect.InvocationHandler h) {
        return newProxyInstance(loader, null, interfaces, null, null, h);
    }
    
    public static Object newProxyInstance(ClassLoader loader, Class<?> type, 
            Class<?>[] interfaces, java.lang.reflect.InvocationHandler h) {
        return newProxyInstance(loader, type, interfaces, null, null, h);
    }
    
    public static Object newProxyInstance(ClassLoader loader, Class<?> type, 
            Class<?>[] interfaces, Class<?>[] parameterTypes, Object[] args, 
            java.lang.reflect.InvocationHandler h) {
        try {
            Enhancer enhancer = new Enhancer();
            if (loader != null) {
                enhancer.setClassLoader(loader);
            }
            enhancer.setSuperclass(type);
            enhancer.setInterfaces(interfaces);
            enhancer.setUseCache(true);
            enhancer.setUseFactory(true);
            enhancer.setCallback(new HandlerAdapter(h));
            final Object proxy;
            if (parameterTypes == null) {
                proxy = enhancer.create();
            } else {
                proxy = enhancer.create(parameterTypes, args);
            }
            generatedClasses.put(proxy.getClass(), null);
            return proxy;
        } catch (CodeGenerationException e) {
            if (type != null) {
                throw new ComponentApplicationException(
                        "Failed to create CGLib " +
                        "proxy of " + type + ".", e);
            } else {
                throw new ComponentApplicationException(
                        "Failed to create CGLib proxy.", e);
            }
        } catch (IllegalArgumentException e) {
            if (type != null) {
                throw new ComponentApplicationException(
                        "Failed to create CGLib proxy of " + type + ".", e);
            } else {
                throw new ComponentApplicationException(
                        "Failed to create CGLib proxy.", e);
            }
        } catch (Exception e) {
            throw new ComponentApplicationException("Unexpected exception.", e);
        }
    }
    
    private static class HandlerAdapter implements InvocationHandler {
        private java.lang.reflect.InvocationHandler handler;
        
        public HandlerAdapter(java.lang.reflect.InvocationHandler handler) {
            this.handler = handler;
        }
        
        public Object invoke(Object proxy, Method method, Object[] args) throws 
                Throwable {
            return handler.invoke(proxy, method, args);
        }
    }
}