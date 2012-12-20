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
package org.jargo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.List;

import org.jargo.deploy.Deployer;

/**
 * Provides access to jargo components.
 * 
 * @author Leon van Zantvoort
 */
public abstract class ComponentApplicationContext implements Deployer {
    
    private static class LazyHolder {
        static ComponentApplicationContext ctx;
        static {
            try {
                try {
                    Constructor constructor = AccessController.doPrivileged(
                            new PrivilegedExceptionAction<Constructor>() {
                        public Constructor run() throws Exception {
                            String path = "META-INF/services/" + 
                                    ComponentApplicationContext.class.getName();

                            // PERMISSION: java.lang.RuntimePermission getClassLoader
                            ClassLoader loader = Thread.currentThread().
                                    getContextClassLoader();
                            final Enumeration<URL> urls;
                            if (loader == null) {
                                urls = ComponentApplicationContext.class.
                                        getClassLoader().getResources(path);
                            } else {
                                urls = loader.getResources(path);
                            }
                            while (urls.hasMoreElements()) {
                                URL url = urls.nextElement();
                                BufferedReader reader = new BufferedReader(new InputStreamReader(
                                        url.openStream()));
                                try {
                                    String className = null;
                                    while ((className = reader.readLine()) != null) {
                                        final String name = className.trim();
                                        if (!name.startsWith("#") && !name.startsWith(";") &&
                                                !name.startsWith("//")) {
                                            final Class<?> cls;
                                            if (loader == null) {
                                                cls = Class.forName(name);
                                            } else {
                                                cls = Class.forName(name, true, loader);
                                            }
                                            int m = cls.getModifiers();
                                            if (ComponentApplicationContext.class.isAssignableFrom(cls) &&
                                                    !Modifier.isAbstract(m) &&
                                                    !Modifier.isInterface(m)) {
                                                // PERMISSION: java.lang.RuntimePermission accessDeclaredMembers
                                                Constructor constructor = cls.getDeclaredConstructor();
                                                // PERMISSION: java.lang.reflect.ReflectPermission suppressAccessChecks
                                                if (!Modifier.isPublic(constructor.getModifiers())) {
                                                    constructor.setAccessible(true);
                                                }
                                                return constructor;
                                            } else {
                                                throw new ClassCastException(cls.getName());
                                            }
                                        }
                                    }
                                } finally {
                                    reader.close();
                                }
                            }
                            throw new ComponentApplicationException("No " +
                                    "ComponentApplicationContext implementation " +
                                    "found.");
                        }
                    });
                    ctx = (ComponentApplicationContext) constructor.newInstance();
                } catch (PrivilegedActionException e) {
                    throw e.getException();
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            } catch (ComponentApplicationException e) {
                throw e;
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                throw new ComponentApplicationException(t);
            }
        }
    }
    
    /**
     * Factory method for obtaining a {@code ComponentApplicationContext} 
     * instance.
     */
    public static ComponentApplicationContext instance() throws 
            ComponentApplicationException {
        try {
            try {
                ComponentApplicationContext ctx = LazyHolder.ctx;
                if (ctx == null) {
                    throw new ComponentApplicationException("Container shut down.");
                }
                return ctx.resolveInstance();
            } catch (ExceptionInInitializerError e) {
                try {
                    throw e.getException();
                } catch (ComponentApplicationException e2) {
                    throw e2;
                } catch (Error e2) {
                    throw e2;
                } catch (Throwable t) {
                    throw new ComponentApplicationException(t);
                }
            }
        } catch (ComponentApplicationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        }
    }
    
    /**
     * Subclasses can override this method to have control over which instance
     * is to be returned by {@link #instance}.
     */
    protected ComponentApplicationContext resolveInstance() throws 
            ComponentApplicationException {
        return this;
    }

    /**
     * Undeploys all components and stops all internal container threads.
     */
    public void shutdown() throws ComponentApplicationException {
        try {
            shutdownDelegate();
        } finally {
            LazyHolder.ctx = null;
            Runtime.getRuntime().gc();
        }
    }

    protected abstract void shutdownDelegate() throws ComponentApplicationException;

    /**
     * Returns an immutable list of {@code ComponentFactory} objects for all 
     * registered components.
     */
    public abstract List<ComponentFactory<?>> getComponentFactories();
    
    /**
     * Looks up the {@code ComponentFactory} for the specified
     * {@code componentName}.
     *
     * @throws ComponentNotFoundException if no component is registered for the
     * specified {@code componentName}.
     */
    public abstract List<ComponentFactory<?>> getComponentFactoriesForName(
            String componentName) throws ComponentNotFoundException;

    /**
     * Returns an immutable list of {@code ComponentFactory} objects for 
     * component instances that match the specified {@code type}.
     */
    public abstract <T> List<ComponentFactory<? extends T>>
            getComponentFactoriesForType(Class<T> type);
    
    /**
     * Returns an immutable list of {@code ComponentFactory} objects for component instances that match both the
     * specified {@code componentName} and {@code type}.
     */
    public abstract <T> List<ComponentFactory<? extends T>>
            getComponentFactoriesForNameAndType(String componentName, Class<T> type);

    /**
     * Returns the call stack of all references being invoked by the this 
     * thread.
     */
    public abstract List<ComponentReference<?>> referenceStack();
}

