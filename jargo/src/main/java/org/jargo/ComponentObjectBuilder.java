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
 * Builder for {@code ComponentObject} instances.
 * 
 * @author Leon van Zantvoort
 */
public interface ComponentObjectBuilder<T> {
    
    /**
     * Creates a new {@code ComponentObject}.
     */
    ComponentObject<T> newInstance() throws ComponentCreationException;
    
    /**
     * Returns the component reference that is currently bound to the calling 
     * thread.
     * 
     * @throw ComponentStateException in case the underlying component is static.
     */
    ComponentReference<T> reference() throws ComponentStateException;
    
    /**
     * Attaches the specified {@code reference} to the calling thread. This 
     * method should only be used in case of out of bound 
     * {@code ComponentObject} interaction.
     * 
     * @throw ComponentStateException in case the underlying component is static.
     */
    void attach(ComponentReference<T> reference) throws ComponentStateException;

    /**
     * Detaches the last {@code reference} from the calling thread. This 
     * method should only be used in case of out of bound 
     * {@code ComponentObject} interaction.
     * 
     * @throw ComponentStateException in case the underlying component is static.
     */
    void detach() throws ComponentStateException;    
}
