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

import static java.util.logging.Level.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import org.jargo.ComponentAlias;
import org.jargo.deploy.Deployer;
import org.jargo.deploy.Deployable;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentUnit;
import org.jargo.URLRegistration;
import org.jargo.spi.ComponentAliasProvider;
import org.jargo.spi.ComponentConfigurationProvider;

/**
 * Deploys component components.
 *
 * @author Leon van Zantvoort
 */
final class URLDeployer implements Deployer {
    
    // PENDING: document classloader specifics
    
    private final Logger logger;
    private final Lock lock;
    private final Map<URL, List<ComponentConfiguration<?>>> configurationMap;
    private final Map<URL, List<ComponentAlias>> aliasMap;
    private final Map<URL, ComponentUnit> unitMap;
    private final Map<URL, Destroyer> unitDestroyerMap;
    private final Providers providers;
    
    private Deployer parent;
    
    public URLDeployer(ComponentRegistry registry) {
        logger = Logger.getLogger(getClass().getName());
        lock = new ReentrantLock(true);
        configurationMap = new HashMap<URL, List<ComponentConfiguration<?>>>();
        aliasMap = new HashMap<URL, List<ComponentAlias>>();
        unitMap = new HashMap<URL, ComponentUnit>();
        unitDestroyerMap = new HashMap<URL, Destroyer>();
        
        providers = registry.getProviders();
    }
    
    public void setParent(Deployer parent) {
        this.parent = parent;
    }

    /**
     * <p>Deploys <tt>URLDeployable</tt> objects.</p>
     * 
     * <p>The specified URL is checked for classes implementing the 
     * <tt>Deployer</tt> interface. These classes are instantiated and submitted
     * to the parent deployer. Next, the URL <tt>ComponentConfigurationProvider</tt>
     * is called, resulting in a list <tt>ComponentConfiguration</tt>s. If 
     * non-empty, this list is wrapped in a 
     * <tt>ComponentConfigurationDeployable</tt> and submitted to the parent
     * deployer.
     * 
     * @param deployable object to be deployed. This deployer only supports 
     * <tt>URLDeployable</tt> objects, other objects are ignored.
     * @throws Exception if underlying URL cannot be read. Failure of this 
     * method occurs atomically.
     */
    @SuppressWarnings("finally")
    public void deploy(Deployable deployable) throws Exception {
        if (deployable instanceof URLRegistration) {
            final ClassLoader org = AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    // PERMISSION: java.lang.RuntimePermission getClassLoader
                    return Thread.currentThread().getContextClassLoader();
                }
            });
            lock.lock();
            try {
                List<URL> urls = ((URLRegistration) deployable).getURLs();

                logger.info("Deploying urls: " + urls + ".");

                Map<URL, List<Deployable>> deployableMap = 
                        new HashMap<URL, List<Deployable>>();
                for (final URL url : urls) {
                    final ComponentUnit unit;
                    if (unitMap.containsKey(url)) {
                        unit = unitMap.get(url);
                    } else {
                        ClassLoader loader = providers.getClassLoaderProvider().
                                getClassLoader(url);
                        Destroyer destroyer = new Destroyer();
                        unit = new ComponentUnitImpl(url, loader, destroyer);
                        unitMap.put(url, unit);
                        unitDestroyerMap.put(url, destroyer);
                    }
                    AccessController.doPrivileged(
                            new PrivilegedAction<Object>() {
                        public Object run() {
                            // PERMISSION: java.lang.RuntimePermission setContextClassLoader
                            Thread.currentThread().setContextClassLoader(unit.getClassLoader());
                            return null;
                        }
                    });
                    // Make sure that deployers are deployed first.
                    // PENDING: add sequencing / dependencies
                    List<Deployable> deployableList = getDeployables(unit);
                    List<Deployable> deployedList = new ArrayList<Deployable>();
                    for (Iterator<Deployable> i = deployableList.iterator(); i.hasNext();) {
                        Deployable d = i.next();
                        if (d instanceof Deployer) {
                            boolean commit = false;
                            try {
                                parent.deploy(d);
                                deployedList.add(d);
                                commit = true;
                            } finally {
                                if (!commit) {
                                    for (Deployable deployed : deployedList) {
                                        try {
                                            undeploy(deployed);
                                        } finally {
                                            continue;
                                        }
                                    }
                                }
                            }
                            i.remove();
                        }
                    }
                    deployableMap.put(url, deployableList);
                }
                List<ComponentConfiguration<?>> configurationList = 
                        new ArrayList<ComponentConfiguration<?>>();
                List<ComponentAlias> aliasList = 
                        new ArrayList<ComponentAlias>();
                for (URL url : urls) {
                    try {
                        List<Deployable> deployedList = new ArrayList<Deployable>();
                        for (Deployable d : deployableMap.get(url)) {
                            boolean commit = false;
                            try {
                                parent.deploy(d);
                                deployedList.add(d);
                                commit = true;
                            } finally {
                                if (!commit) {
                                    for (Deployable deployed : deployedList) {
                                        try {
                                            undeploy(deployed);
                                        } finally {
                                            continue;
                                        }
                                    }
                                }
                            }
                        }

                        ComponentUnit unit = unitMap.get(url);
                        assert unit != null;

                        ComponentAliasProvider aliasProvider = providers.
                                getComponentAliasProvider();
                        List<ComponentAlias> tmpAliases = 
                                aliasProvider.getComponentAliases(unit);
                        aliasList.addAll(tmpAliases);
                        aliasMap.put(url, tmpAliases);

                        ComponentConfigurationProvider configurationProvider = providers.
                                getComponentConfigurationProvider();
                        List<ComponentConfiguration<?>> tmpConfigurations = 
                                new ArrayList<ComponentConfiguration<?>>(configurationProvider.
                                getComponentConfigurations(unit));
                        for (Iterator i = tmpConfigurations.iterator(); i.hasNext();) {
                            if (configurationList.contains(i.next())) {
                                // Remove duplicates.
                                i.remove();
                            }
                        }
                        configurationList.addAll(tmpConfigurations);
                        configurationMap.put(url, tmpConfigurations);
                    } catch (Exception e) {
                        logger.warning("An exception is caught during " +
                                "deployment of the following url: '" + url + 
                                "'. Deployment of this unit is rolled back" +
                                (urls.size() > 1 ? ": " + urls : "") + ".");
                        throw e;
                    }
                }
                if (!aliasList.isEmpty()) {
                    boolean commit = false;
                    try {
                        parent.deploy(new ComponentAliasDeployable(
                                aliasList));
                        commit = true;
                    } finally {
                        if (!commit) {
                            aliasMap.keySet().remove(urls);
                        }
                    }
                }
                if (!configurationList.isEmpty()) {
                    boolean commit = false;
                    try {
                        parent.deploy(new ComponentRegistrationImpl(
                                configurationList));
                        commit = true;
                    } finally {
                        if (!commit) {
                            configurationMap.keySet().remove(urls);
                        }
                    }
                }
                logger.info("Successfully deployed urls: " + urls + ".");
            } finally {
                lock.unlock();
                AccessController.doPrivileged(
                        new PrivilegedAction<Object>() {
                    public Object run() {
                        // PERMISSION: java.lang.RuntimePermission setContextClassLoader
                        Thread.currentThread().setContextClassLoader(org);
                        return null;
                    }
                });
            }
        }
    }
    
    /**
     * <p>Undeploys <tt>URLDeployable</tt> objects.</p>
     * 
     * <p>All objects that were previously deployed for the underlying URL are
     * undeployed through this method.
     *
     * @param deployable object to be undeployed. This deployer only supports 
     * <tt>URLDeployable</tt> objects, other objects are ignored.
     */
    @SuppressWarnings("finally")
    public void undeploy(Deployable deployable) throws Exception {
        if (deployable instanceof URLRegistration) {
            final ClassLoader org = AccessController.doPrivileged(
                    new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    // PERMISSION: java.lang.RuntimePermission getClassLoader
                    return Thread.currentThread().getContextClassLoader();
                }
            });
            lock.lock();
            try {
                List<URL> urls = ((URLRegistration) deployable).getURLs();
                logger.info("Undeploying urls: " + urls + ".");
                List<ComponentConfiguration<?>> configurationList = 
                        new ArrayList<ComponentConfiguration<?>>();
                List<ComponentAlias> aliasList = 
                        new ArrayList<ComponentAlias>();
                Set<Destroyer> destroyerSet = new LinkedHashSet<Destroyer>(); 
                for (URL url : urls) {
                    final ComponentUnit unit = unitMap.remove(url);
                    final Destroyer destroyer = unitDestroyerMap.remove(url);
                    assert unit != null;
                    assert destroyer != null;
                    AccessController.doPrivileged(
                            new PrivilegedAction<Object>() {
                        public Object run() {
                            // PERMISSION: java.lang.RuntimePermission setContextClassLoader
                            Thread.currentThread().setContextClassLoader(unit.getClassLoader());
                            return null;
                        }
                    });
                    List<ComponentAlias> tmpAliases = aliasMap.remove(url);
                    if (tmpAliases != null) {
                        aliasList.addAll(tmpAliases);
                    }
                    List<ComponentConfiguration<?>> tmpConfigurations = 
                            configurationMap.remove(url);
                    if (tmpConfigurations != null) {
                        configurationList.addAll(tmpConfigurations);
                    }
                    destroyerSet.add(destroyer);
                }
                try {
                    parent.undeploy(new ComponentRegistrationImpl(
                            configurationList));
                } finally {
                    try {
                        parent.undeploy(new ComponentAliasDeployable(
                                aliasList));
                    } finally {
                        for (Destroyer destroyer : destroyerSet) {
                            try {
                                destroyer.destroy();
                            } finally {
                                continue;
                            }
                        }
                    }
                }
                logger.info("Successfully undeployed urls: " + urls + ".");
            } finally {
                lock.unlock();
                AccessController.doPrivileged(
                        new PrivilegedAction<Object>() {
                    public Object run() {
                        // PERMISSION: java.lang.RuntimePermission setContextClassLoader
                        Thread.currentThread().setContextClassLoader(org);
                        return null;
                    }
                });
            }
        }
    }
    
    /**
     * Opens a jar URL connection to the given URL and inspects all underlying
     * classes. Classes that implement the Deployable interface are constructed
     * through a constructor that takes no arguments.
     *
     * @param url URL to extract deployable objects from.
     * @return list of deployable instances.     
     * @throws Exception on read and class instantiation errors.
     */
    private List<Deployable> getDeployables(ComponentUnit unit) throws
            Exception {
        // PENDING: Use jar-file service discovery instead?
        List<Deployable> deployables = new ArrayList<Deployable>();
        
        List<Class> classes = getClasses(unit);
        for (final Class<?> cls : classes) {
            int m = cls.getModifiers();
            if (Deployable.class.isAssignableFrom(cls) && 
                    !Modifier.isAbstract(m) && 
                    !Modifier.isInterface(m)) {
                Deployable deployable = null;
                try {
                    try {
                        Constructor constructor = AccessController.doPrivileged(
                                new PrivilegedExceptionAction<Constructor>() {
                            public Constructor run() throws Exception {
                                // PERMISSION: java.lang.RuntimePermission accessDeclaredMembers
                                Constructor constructor = cls.getDeclaredConstructor();
                                if (!Modifier.isPublic(constructor.getModifiers())) {
                                    // PERMISSION: java.lang.reflect.ReflectPermission suppressAccessChecks
                                    constructor.setAccessible(true);
                                }
                                return constructor;
                            }
                        });
                        deployable = (Deployable) constructor.newInstance();
                    } catch (PrivilegedActionException e) {
                        throw e.getException();
                    }
                } catch (IllegalAccessException e) {
                    logger.throwing(getClass().getName(), "getDeployables", e);
                } catch (InvocationTargetException e) {
                    logger.throwing(getClass().getName(), "getDeployables", e);
                } catch (InstantiationException e) {
                    logger.throwing(getClass().getName(), "getDeployables", e);
                } catch (NoSuchMethodException e) {
                    logger.throwing(getClass().getName(), "getDeployables", e);
                }

                if (deployable != null) {
                    deployables.add(deployable);
                }
            }
        }
        return deployables;
    }
    
    /**
     * Extracts all classes from the specified url.
     *
     * @param url url to extract classes from.
     * @return all classes found in the specified url.
     * @throws IOException on read failures.
     */
    private static List<Class> getClasses(ComponentUnit unit) throws 
            IOException {
        try {
            URLConnection connection = unit.getURL().openConnection();
            connection.setUseCaches(false);
            JarInputStream jis = new JarInputStream(new BufferedInputStream(
                    connection.getInputStream()));
            try {
                List<Class> classes = new ArrayList<Class>();
                JarEntry entry = null;
                while ((entry = jis.getNextJarEntry()) != null) {
                    String name = entry.getName();
                    if (name.endsWith(".class")) {
                        String className = name.substring(0, name.length() -
                                ".class".length());
                        className = className.replace('/', '.');
                        try {
                            final Class cls;
                            if (unit.getClassLoader() != null) {
                                cls = Class.forName(className, true, unit.getClassLoader());
                            } else {
                                cls = Class.forName(className);
                            }
                            classes.add(cls);
                        } catch (ClassNotFoundException e) {
                            throw e;
                        }
                    }
                }
                return classes;
            } finally {
                jis.close();
            }
        } catch (IOException e) {
            throw e;
        } catch (Throwable t) {
            IOException io = new IOException();
            io.initCause(t);
            throw io;
        }
    }
}
