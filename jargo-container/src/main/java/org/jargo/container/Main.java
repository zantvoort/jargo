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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jargo.ComponentApplicationContext;

/**
 * @author Leon van Zantvoort
 */
public final class Main {
    
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    /**
     * Starts the container. The first command line argument are the paths to 
     * the deployment unit files or directories.
     * 
     * @param args the command line arguments.
     */
    public static void main(String... args) throws Throwable {
        setSystemProperties();
        Main main = new Main(args);
        main.start();
        main.waitForDeath();
    }
    
    /**
     * Sets some system properties to force the JargoLogManager to be used.
     */
    private static void setSystemProperties() {
        // PERMISSION: java.util.PropertyPermission "java.util.logging.manager" "read,write"
        if (System.getProperty("java.util.logging.manager") == null) {
            System.setProperty("java.util.logging.manager", 
                    "org.jargo.container.JargoLogManager");
        }
    }
    
    private final Map<File, Date> componentsDeployed;
    
    private final Thread thread;
    private AtomicBoolean start;
    private AtomicBoolean deploying;
    private AtomicBoolean stop;
    
    private final long startTimestamp;
    private final List<File> paths;

    private final Lock lock;
    private final Condition stoppedCondition;
    private boolean stopped;
    
    private ComponentApplicationContext ctx;
    
    public Main(String... args) throws Throwable {
        this.startTimestamp = System.currentTimeMillis();
        this.lock = new ReentrantLock();
        this.componentsDeployed = new HashMap<File, Date>();
        this.start = new AtomicBoolean();
        this.deploying = new AtomicBoolean();
        this.stop = new AtomicBoolean();
        this.stoppedCondition = lock.newCondition();
        try {
            Thread t = JargoThreadFactory.
                instance("Jargo-ShutdownHook").newThread(new Runnable() {
                public void run() {
                    stop();
                }
            });
            // Disable to prevent dead-lock.
            t.setUncaughtExceptionHandler(null);
            // PERMISSION: java.io.RuntimePermission shutdownHooks
            Runtime.getRuntime().addShutdownHook(t);
            JargoLogManager.allowReset(false);
        } catch (IllegalStateException e) {
            // Ignore.
        }
        paths = new ArrayList<File>();
        if (args.length == 0) {
            throw new IllegalArgumentException("Arguments: [components-path-1] [components-path-n].");
        } else {
            for (String path : args) {
                paths.add(new File(path));
            }
        }
        final ThreadFactory threadFactory = JargoThreadFactory.
                instance("Jargo-DeploymentScanner");
        thread = threadFactory.newThread(new Runnable() {
            public void run() {
                try {
                    lock.lock();
                    try {
                        while (!stop.get()) {
                            if (!lock.newCondition().await(1, TimeUnit.SECONDS)) {
                                scan(paths);
                            }
                        }
                    } catch (InterruptedException e) {
                    } finally {
                        lock.unlock();
                    }
                } finally {
                    if (!stop.get()) {
                        // PERMISSION: java.io.RuntimePermission modifyThread
                        threadFactory.newThread(this).start();
                    }
                }
            }
        });
    }
    
    /**
     * Starts the container. This method is idempotent.
     */
    public void start() {
        if (!start.getAndSet(true)) {
            lock.lock();
            try {
                logStartMessage(false);
                ctx = ComponentApplicationContext.instance();
                // PERMISSION: java.io.RuntimePermission modifyThread
                scan(paths);
                thread.start();
            } finally {
                try {
                    logStartMessage(true);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Stops the container. This method is idempotent.
     */
    public void stop() {
        if (!stop.getAndSet(true)) {
            if (deploying.get()) {
                logger.info("Jargo Container runtime is currently (un)deploying. Shutdown is initiated after completing (un)deployment.");
            }
            lock.lock();
            try {
                logShutdownMessage(false);
                List<URL> undeploy = new ArrayList<URL>();
                for (Iterator<File> i = componentsDeployed.keySet().iterator();
                        i.hasNext();) {
                    URL url = getURL(i.next(), false);
                    if (url != null) {
                        undeploy.add(url);
                    }
                    i.remove();
                }
                if (ctx != null) {
                    try {
                        ctx.undeploy(new URLRegistrationImpl(undeploy));
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "THROW", e);
                    } finally {
                        ctx.shutdown();
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "THROW", e);
            } finally {
                try {
                    stopped = true;
                    stoppedCondition.signalAll();
                    logShutdownMessage(true);
                } finally {
                    try {
                        JargoLogManager.allowReset(true);
                    } finally {
                        lock.unlock();
                    }
                }
            }
        }
    }
    
    /**
     * Logs a message on startup.
     * 
     * @param started {@code false} on startup, {@code true} if startup is
     * completed.
     */
    public void logStartMessage(boolean started) {
        if (!started) {
            logger.info("Jargo Container runtime starting.");
        } else {
            long now = System.currentTimeMillis();
            long time = now - startTimestamp;

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.setTimeInMillis(time);
            int year = c.get(Calendar.YEAR) - 1970;
            int day = c.get(Calendar.DAY_OF_YEAR) - 1;

            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HH'h':mm'm':ss's'");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            logger.info("Jargo Container runtime started in " +
                    (year > 0 ? (year + " year" + (year == 1 ? "" : "s") + ", ") : "")
                    + day + " day" + (day == 1 ? ", " : "s, ") + 
                    format.format(c.getTime()) + ".");
        }
    }
    
    /**
     * Logs a message on shutdown.
     * 
     * @param shutdown {@code false} if shutdown is initiated, {@code true} if
     * shutdown is completed.
     */
    public void logShutdownMessage(boolean shutdown) {
        long now = System.currentTimeMillis();
        long time = now - startTimestamp;
        
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(time);
        int year = c.get(Calendar.YEAR) - 1970;
        int day = c.get(Calendar.DAY_OF_YEAR) - 1;
        
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HH'h':mm'm':ss's'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        logger.info("Jargo Container runtime " + 
                (shutdown ? "shut down" : "shutting down") + " after " +
                (year > 0 ? (year + " year" + (year == 1 ? "" : "s") + ", ") : "")
                + day + " day" + (day == 1 ? ", " : "s, ") + 
                format.format(c.getTime()) + ".");
    }
    
    /**
     * Blocks until the {@code stop} method is called.
     */
    private void waitForDeath() {
        lock.lock();
        try {
            while (!stopped) {
                stoppedCondition.awaitUninterruptibly();
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void scan(List<File> paths) {
        if (!stop.get()) {
            try {
                deploying.set(true);
                Set<File> removed = new HashSet<File>();
                removed.addAll(componentsDeployed.keySet());

                List<URL> deploy = new ArrayList<URL>();
                List<URL> undeploy = new ArrayList<URL>();

                List<File> files = new ArrayList<File>();
                for (File path : paths) {
                    // PERMISSION: java.io.FilePermission <dir> read
                    if (path.exists()) {
                        if (path.isDirectory()) {
                            files.addAll(Arrays.asList(path.listFiles(new FileFilter() {
                                public boolean accept(File pathname) {
                                    return pathname.getName().endsWith(".jar");
                                }
                            })));
                        } else {
                            files.add(path);
                        }
                    }
                }

                for (File file : files) {
                    // PERMISSION: java.io.FilePermission <file> read
                    Date lastModified = new Date(file.lastModified());

                    Date prevDate = componentsDeployed.get(file);
                    if (prevDate == null) {
                        URL url = getURL(file, true);
                        if (url != null) {
                            componentsDeployed.put(file, lastModified);
                            deploy.add(url);
                        }
                    } else if (!prevDate.equals(lastModified)) {
                        URL url = getURL(file, true);
                        if (url != null) {
                            componentsDeployed.put(file, lastModified);
                            undeploy.add(url);
                            deploy.add(url);
                            removed.remove(file);
                        }
                    } else {
                        removed.remove(file);
                    }
                }

                for (File file : removed) {
                    URL url = getURL(file, false);
                    if (url != null) {
                        undeploy.add(url);
                    }
                    componentsDeployed.remove(file);
                }

                if (!undeploy.isEmpty()) {
                    ctx.undeploy(new URLRegistrationImpl(undeploy));
                }
                if (!deploy.isEmpty()) {
                    ctx.deploy(new URLRegistrationImpl(deploy));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "THROW", e);        
            } finally {
                deploying.set(false);
            }
        }
    }
    
    /**
     * Translates the specified {@code file} to an {@code URL}.
     * 
     * @param file file to be converted.
     * @param validate {@code true} to validate file.
     */
    private URL getURL(File file, boolean validate) {
        URL url;
        try {
            if (validate) {
                // PERMISSION: java.io.FilePermission <dir> read
                if (file.canRead()) {
                    JarFile jar = new JarFile(file, true);
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        entries.nextElement();
                    }
                } else {
                    throw new IOException("Can not read file: " + file + ".");
                }
            }
            // PERMISSION: java.util.PropertyPermission user.dir read
            url = file.toURI().toURL();
        } catch (IOException e) {
            logger.log(Level.WARNING, "THROW", e);
            url = null;
        } catch (InternalError e) {
            logger.log(Level.WARNING, "THROW", e);
            url = null;
        }
        return url;
    }
}
