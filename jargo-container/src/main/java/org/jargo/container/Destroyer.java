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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jargo.Destroyable;

/**
 *
 * @author Leon van Zantvoort
 */
final class Destroyer implements Destroyable {
    
    private static final Logger logger = Logger.getLogger(Destroyer.class.getName());
    private Map<Runnable, Runnable> hooks;
    
    /** Creates a new instance of Destroyer */
    public Destroyer() {
        this.hooks = new IdentityHashMap<Runnable, Runnable>();
    }
    
    /**
     * Although this method should not be called twice, since it never throws
     * any exceptions, it is made idempotent.
     */
    public void destroy() {
        final Set<Runnable> runnables;
        synchronized (this) {
            if (hooks != null) {
                runnables = hooks.keySet();
                hooks = null;
            } else {
                runnables = Collections.emptySet();
            }
        }
        destroy(runnables.iterator());
    }
    
    private void destroy(Iterator<Runnable> i) {
        if (i.hasNext()) {
            try {
                i.next().run();
            } catch (Throwable t) {
                logger.log(WARNING, "Unexpected exception at destroy.", t);
            } finally {
                destroy(i);
            }
        }
    }

    public synchronized void addDestroyHook(Runnable hook) {
        if(hooks == null) {
            throw new IllegalStateException("Destroy in progress.");
        }
	if (hook == null) {
	    throw new NullPointerException();
        }
        if (hooks.containsKey(hook)) {
            throw new IllegalArgumentException("Hook previously registered.");
        }
        hooks.put(hook, hook);
    }
    
    public synchronized boolean removeDestroyHook(Runnable hook) {
	if (hooks == null) {
	    throw new IllegalStateException("Destroy in progress.");
        }
	if (hook == null) {
	    throw new NullPointerException();
        }
	return hooks.remove(hook) != null;
    }
}
