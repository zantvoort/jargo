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

import java.util.logging.LogManager;

/**
 * This log manager allows applications to log messages while the Java VM
 * is shutting down.
 * 
 * The 'cleaner' shutdown hook of the original log manager invokes 
 * {@code reset} to tear down all loggers. The {@code JargoLogManager}
 * disables the {@code reset} method during shutdown.
 * 
 * The {@code JargoLogManager} installs a 'cleaner' shutdown hook as well. 
 * The shutdown hook waits until {@code allowReset} has been called. 
 * Applications should invoke this method once the Java VM is allowed to stop 
 * logging. The Java VM will not exit before this method has been invoked.
 * 
 * Enable this log manager by setting the system property 
 * {@code java.util.logging.manager} to 
 * {@code org.jargo.container.JargoLogManager}.
 * 
 * @author Leon van Zantvoort
 */
public final class JargoLogManager extends LogManager {

    private static boolean allowReset = true;
    
    /**
     * Invoke this method to allow this log manager to clean up during shutdown.
     * The 'cleaner' shutdown hook waits until this method has been invoked, 
     * keeping the Java VM from finishing the shutdown sequence.
     */
    public static void allowReset(boolean flag) {
        synchronized(JargoLogManager.class) {
            allowReset = flag;
            JargoLogManager.class.notifyAll();
        }
    }
    
    public JargoLogManager() {
        // PERMISSION: java.lang.RuntimePermission shutdownHooks
        super();
        try {
            // PERMISSION: java.lang.RuntimePermission shutdownHooks
            Runtime.getRuntime().addShutdownHook(new Cleaner());
        } catch (IllegalStateException e) {
        }
    }
    
    // This private class is used as a shutdown hook.
    // It does a "reset" to close all open handlers.
    private class Cleaner extends Thread {
	public void run() {
            synchronized(JargoLogManager.class) {
                while (!allowReset) {
                    try {
                        JargoLogManager.class.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
            try {
                // Allow log messages to be processed.
                Thread.sleep(666);
            } catch (InterruptedException e) {
            }
            // PERMISSION: java.util.logging.LoggingPermission control
            // Invoke original reset method.
	    JargoLogManager.super.reset();
 	}
    }
    
    /**
     * Returns {@code true} if the Java VM is shutting down, {@code false} 
     * otherwise.
     *
     * @return {@code true} if the Java VM is shutting down, {@code false} 
     * otherwise.
     */
    private boolean isShuttingDown() {
        Thread t = new Thread();
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.addShutdownHook(t);
            try {
                runtime.removeShutdownHook(t);
            } catch (IllegalStateException e) {
                // Caught if VM entered shutdown phase between addShutdownHook 
                // and removeShutdownHook.
            }
            return false;
        } catch (IllegalStateException e) {
            return true;
        }
    }
    
    /**
     * Invokes {@code super.reset} if and only if Java VM is not shutting down.
     */
    public void reset() throws SecurityException {
        // This method is expected to be called infrequently.
        if (!isShuttingDown()) {
            super.reset();
        }
    }
}
