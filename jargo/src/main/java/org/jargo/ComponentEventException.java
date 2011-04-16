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

/**
 * Generic event exception.
 * 
 * @author Leon van Zantvoort
 */
public class ComponentEventException extends ComponentException {
    
    private static final long serialVersionUID = -423523525213523566L;
    
    private final Event event;
    
    public ComponentEventException(String component, Event event) {
        super(component, String.valueOf(event));
        if (event == null) {
            throw new NullPointerException();
        }
        this.event = event;
    }

    public ComponentEventException(String component, Event event, 
            String message) {
        super(component, 
                String.valueOf(event) + (message == null ? "" : ": " + message));
        if (event == null) {
            throw new NullPointerException();
        }
        this.event = event;
    }
    
    public ComponentEventException(String component, Event event, 
            Throwable cause) {
        super(component, String.valueOf(event), cause);
        if (event == null) {
            throw new NullPointerException();
        }
        this.event = event;
    }
    
    public ComponentEventException(String component, Event event, 
            String message, Throwable cause) {
        super(component, 
                String.valueOf(event) + (message == null ? "" : ": " + message), cause);
        if (event == null) {
            throw new NullPointerException();
        }
        this.event = event;
    }
    
    public Event getEvent() {
        return event;
    }
}
