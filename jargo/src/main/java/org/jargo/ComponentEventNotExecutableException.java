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
 * Thrown if specified event cannot be executed. Whether an event is executable
 * is based on the event type, event properties and component reference, target 
 * for event execution.
 * 
 * @author Leon van Zantvoort
 */
public class ComponentEventNotExecutableException extends ComponentEventException {
    
    private static final long serialVersionUID = -442352315235235566L;
    
    public ComponentEventNotExecutableException(String component, Event event) {
        super(component, event);
    }

    public ComponentEventNotExecutableException(String component, Event event, 
            Throwable cause) {
        super(component, event, cause);
    }
}
