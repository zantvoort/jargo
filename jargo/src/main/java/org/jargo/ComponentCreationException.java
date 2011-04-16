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
 * Thrown if an instance for the specified component could not be created, or
 * obtained for some reason.
 * 
 * @author Leon van Zantvoort
 */
public class ComponentCreationException extends ComponentException {

    private static final long serialVersionUID = -2089974143219973006L;
    
    public ComponentCreationException(String componentName) {
        super(componentName);
    }
    
    public ComponentCreationException(String componentName, String message) {
        super(componentName, message);
    }    

    public ComponentCreationException(String componentName, Throwable cause) {
        super(componentName, cause);
    }    

    public ComponentCreationException(String componentName, String message,
            Throwable cause) {
        super(componentName, message, cause);
    }    
}
