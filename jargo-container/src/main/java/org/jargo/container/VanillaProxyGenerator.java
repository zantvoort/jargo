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
import net.sf.cglib.core.CodeGenerationException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentException;
import org.jargo.ComponentReference;
import org.jargo.EventFactory;
import org.jargo.ComponentExceptionHandler;
import org.jargo.ComponentMetaData;
import org.jargo.Event;
import org.jargo.ProxyController;
import org.jargo.ProxyGenerator;

/**
 * @author Leon van Zantvoort
 */
final class VanillaProxyGenerator<T> implements ProxyGenerator<T> {

    private static final Method	HASH_CODE;
    private static final Method	EQUALS;
    private static final Method	TO_STRING;
    private static final boolean cglib;
    
    static {
        try {
            Class<?> type = Object.class;
            HASH_CODE = type.getMethod("hashCode");
            EQUALS = type.getMethod("equals", new Class[]{type});
            TO_STRING = type.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        boolean supported = false;
        try {
            Proxy.isSupported();
            supported = true;
        } catch (Error e) {
            // Ignore.
        }
        cglib = supported;
    }
    
    public static boolean isCGLibSupported() {
        return cglib;
    }
    
    private final ComponentReference<T> reference;
    private final EventFactory eventFactory;
    private final ComponentExceptionHandler exceptionHandler;
    private final ProxyController controller;
    
    public VanillaProxyGenerator(ComponentReference<T> reference, 
            ComponentConfiguration<T> configuration, ComponentRegistry registry) {
        if (!isCGLibSupported()) {
            throw new ComponentException(configuration.getComponentName(), 
                    "CGLib not supported.");
        }
        this.reference = reference;
        this.eventFactory = registry.getEventFactory(configuration);
        this.exceptionHandler = registry.getComponentExceptionHandler(configuration);
        this.controller = new VanillaProxyController();
    }
    
    public ProxyController getProxyController() {
        return controller;
    }

    public T generateProxy(Class<?>[] parameterTypes, Object... args) {
        ComponentMetaData<T> metaData = reference.getComponentMetaData();
        return Proxy.newProxyInstance(
                metaData.getComponentName(), 
                metaData.getComponentUnit().getClassLoader(), 
                metaData.getType(), metaData.getInterfaces().toArray(new Class[0]), 
                parameterTypes, args, 
                new ProxyHandler(reference, eventFactory, exceptionHandler, controller));
    }
    
    private static class ProxyHandler {
        
        private final ComponentReference<?> reference;
        private final EventFactory eventFactory;
        private final ComponentExceptionHandler exceptionHandler;
        private final ProxyController controller;

        public ProxyHandler(ComponentReference<?> reference, 
                EventFactory eventFactory,
                ComponentExceptionHandler exceptionHandler,
                ProxyController controller) {
            this.reference = reference;
            this.eventFactory = eventFactory;
            this.exceptionHandler = exceptionHandler;
            this.controller = controller;
        }
        
        public Object intercept(Object proxy, Method method, Object[] args, 
                MethodProxy methodProxy) throws Throwable {
            if (controller.isNoOp()) {
                return methodProxy.invokeSuper(proxy, args);
            } else {
                if (method.equals(HASH_CODE)) {
                    return methodProxy.invokeSuper(proxy, args);
                } else if (method.equals(EQUALS)) {
                    return methodProxy.invokeSuper(proxy, args);
                } else if (method.equals(TO_STRING)) {
                    return methodProxy.invokeSuper(proxy, args);
                }
                try {
                    Event event = eventFactory.getEvent(method, args);
                    return reference.execute(event);
                } catch (ComponentException e) {
                    exceptionHandler.onException(method, e);
                    return null;
                }
            }
        }
    }
    
    private static class Proxy {

        public static void isSupported() throws Error {
            new Enhancer();
        }

        public static <P> P newProxyInstance(String componentName, 
                ClassLoader loader, Class<P> type, Class<?>[] interfaces, 
                Class<?>[] parameterTypes, Object[] args, 
                final ProxyHandler handler) {
            MethodInterceptor interceptor = new MethodInterceptor() {
                public Object intercept(Object object, Method method, Object[] object0, MethodProxy methodProxy) throws Throwable {
                    return handler.intercept(object, method, object0, methodProxy);
                }
            };
            try {
                handler.controller.attach(true);
                Enhancer enhancer = new Enhancer();
                if (loader != null) {
                    enhancer.setClassLoader(loader);
                }
                enhancer.setSuperclass(type);
                enhancer.setInterfaces(interfaces);
                enhancer.setUseCache(true);
                enhancer.setUseFactory(false);
                enhancer.setCallback(interceptor);
                final P proxy;
                if (parameterTypes == null) {
                    @SuppressWarnings("unchecked")
                    P tmp = (P) enhancer.create();
                    proxy = tmp;
                } else {
                    @SuppressWarnings("unchecked")
                    P tmp = (P) enhancer.create(parameterTypes, args);
                    proxy = tmp;
                }
                return proxy;
            } catch (CodeGenerationException e) {
                if (type != null) {
                    throw new ComponentException(componentName,
                            "Failed to create CGLib proxy of " + type + ".", e);
                } else {
                    throw new ComponentException(componentName,
                            "Failed to create CGLib proxy.", e);
                }
            } catch (IllegalArgumentException e) {
                if (type != null) {
                    throw new ComponentException(componentName,
                            "Failed to create CGLib proxy of " + type + ".", e);
                } else {
                    throw new ComponentException(componentName,
                            "Failed to create CGLib proxy.", e);
                }
            } catch (Exception e) {
                throw new ComponentException(componentName, 
                        "Unexpected exception.", e);
            } finally {
                handler.controller.detach();
            }
        }
    }
    
    private static class VanillaProxyController implements ProxyController {

        private final ThreadLocal<JargoStack<Boolean>> local;
        
        public VanillaProxyController() {
            this.local = new ThreadLocal<JargoStack<Boolean>>() {
                @Override
                protected JargoStack<Boolean> initialValue() {
                    return new JargoStack<Boolean>();
                }
            };
        }

        public void attach(boolean noOp) {
            local.get().push(noOp);
        }

        public void detach() {
            local.get().pop();
        }
        
        public boolean isNoOp() {
            Boolean value = local.get().peek();
            return value == null ? Boolean.FALSE : value;
        }
    }
}
