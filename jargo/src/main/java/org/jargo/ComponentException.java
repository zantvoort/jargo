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
 * Indicates that there is an issue with the specified cmoponent. This exception
 * can be subclassed to express more detailed information.
 * 
 * @author Leon van Zantvoort
 */
public class ComponentException extends ComponentApplicationException {
    
    private static final long serialVersionUID = 6881118947022470235L;

    private final String componentName;
    
    public ComponentException(String componentName) {
        super("'" + componentName.toString() + "'");
        this.componentName = componentName;
    }

    public ComponentException(String componentName, String message) {
        super("'" + componentName.toString() + "': " + message.toString());
        this.componentName = componentName;
    }
    
    public ComponentException(String componentName, Throwable cause) {
        super("'" + componentName.toString() + "'", cause);
        this.componentName = componentName;
    }
    
    public ComponentException(String componentName, String message, 
            Throwable cause) {
        super("'" + componentName.toString() + "': " + message.toString(), cause);
        this.componentName = componentName;
    }
    
    public String getComponentName() {
        return componentName;
    }
}
