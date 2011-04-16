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

// Created by container

/**
 * Factory for creating component references.
 *
 * @author Leon van Zantvoort
 */
public interface ComponentFactory<T> {
    
    /**
     * Returns the meta data for the underlying component.
     */
    ComponentMetaData<T> getComponentMetaData();
    
    /**
     * Creates a new component reference for the underlying component.
     * 
     * @return a new component reference for the underlying component.
     * @throws ComponentCreationException if component could not be created for
     * any reason.
     * @throws ComponentNotActiveException if component has not been activated 
     * yet.
     */
    ComponentReference<T> create() throws ComponentCreationException,
            ComponentNotActiveException;

    /**
     * Creates a new component reference for the underlying component.
     * 
     * @param info object that can be used to pass information to the newly
     * created component reference.
     * @return a new component reference for the underlying component.
     * @throws ComponentCreationException if component could not be created for
     * any reason.
     * @throws ComponentNotActiveException if component has not been activated 
     * yet.
     */
    ComponentReference<T> create(Object info) throws ComponentCreationException,
            ComponentNotActiveException;
}
