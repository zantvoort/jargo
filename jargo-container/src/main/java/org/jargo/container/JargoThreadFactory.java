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

import java.security.AccessController;
import java.security.PrivilegedAction;
import static java.util.logging.Level.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Leon van Zantvoort
 */
final class JargoThreadFactory implements ThreadFactory {
    
    private static final boolean exitOnException;
    private static final boolean exitOnOOME;
    private static final boolean exitOnError;
    
    static {
        final boolean b[] = new boolean[3];
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                // PERMISSION: java.util.PropertyPermission "org.jargo.*" "read"
                b[0] = Boolean.getBoolean("org.jargo.exitOnException");
                b[1] = Boolean.getBoolean("org.jargo.exitOnOOME");
                b[2] = Boolean.getBoolean("org.jargo.exitOnError");
                return null;
            }
        });
        exitOnException = b[0];
        exitOnOOME = b[1];
        exitOnError = b[2];
    }
    
    private static final Map<String,ThreadFactory> factories = new
            HashMap<String,ThreadFactory>();
    
    private final String name;
    private final AtomicInteger counter;
    private final Logger logger;
    
    private JargoThreadFactory(String name) {
        this.name = (name == null ? "Jargo" : name);
        this.counter = new AtomicInteger(0);
        this.logger = Logger.getLogger(getClass().getName());
    }
    
    private Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof ThreadDeath) {
                    // thread died gracefully
                } else if (e instanceof RuntimeException) {
                    try {
                        logger.log(WARNING, "Unchaught exception at thread: " + 
                                t + ".", e);
                    } finally {
                        if (exitOnException) {
                            try {
                                synchronized(System.err) {
                                    System.err.println("Unchaught exception " +
                                            "at thread: " + t + "; shutting down.");
                                    e.printStackTrace(System.err);
                                }
                            } finally {
                                Runtime.getRuntime().exit(-1);
                            }
                        }
                    }
                } else if (e instanceof Error) {
                    try {
                        logger.log(WARNING, "Unchaught error at thread: " + t + 
                                ".", e);
                    } finally {
                        if (exitOnOOME && e instanceof OutOfMemoryError) {
                            synchronized(System.err) {
                                System.err.println("Unchaught error " +
                                        "at thread: " + t + "; shutting down.");
                                e.printStackTrace(System.err);
                            }
                            Runtime.getRuntime().exit(-1);
                        } else if (exitOnError) {
                            synchronized(System.err) {
                                    System.err.println("Unchaught error " +
                                            "at thread: " + t + "; shutting down.");
                                e.printStackTrace(System.err);
                            }
                            Runtime.getRuntime().exit(-1);
                        }
                    }
                }
            }
        };
    }
    
    /**
     * Returns a instance to the FixSystem thread factory. This
     * factory creates daemon threads.
     */
    public static ThreadFactory instance() {
        return instance(null);
    }
    
    /**
     * Returns a named instance to the FixSystem thread factory. This
     * factory creates daemon threads.
     */
    public static synchronized ThreadFactory instance(String name) {
        ThreadFactory factory = factories.get(name);
        if (factory == null) {
            factory = new JargoThreadFactory(name);
            factories.put(name, factory);
        }
        return factory;
    }
    
    public Thread newThread(final Runnable runnable) {
        Thread thread = new JargoThread(runnable, name + "-" +
                counter.getAndIncrement());
        if (!thread.isDaemon()) {
            thread.setDaemon(true);
        }
        thread.setUncaughtExceptionHandler(getUncaughtExceptionHandler());
        return thread;
    }

    public static synchronized void clear() {
        factories.clear();
    }
}
