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

import org.jargo.Event;
import org.jargo.EventContext;

/**
 * @author Leon van Zantvoort
 */
abstract class ConcurrentEventContext implements EventContext {
    
    private final ThreadLocal<JargoStack<Event>> events;
    
    public ConcurrentEventContext() {
        this.events = new ThreadLocal<JargoStack<Event>>() {
            @Override
            protected JargoStack<Event> initialValue() {
                return new JargoStack<Event>();
            }
        };
    }
    
    public void attach(Event e) {
        events.get().push(e);
    }
    
    public void detach() {
        if (events.get().poll() == null) {
            throw new AssertionError();
        }
    }
    
    public Event getEvent() {
        return events.get().peek();
    }
}
