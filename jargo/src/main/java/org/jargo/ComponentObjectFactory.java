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
 * Factory for {@code ComponentObject}s.
 * 
 * @see ComponentObjectPool
 * @author Leon van Zantvoort
 */
public interface ComponentObjectFactory<T> {
    
    /**
     * Returns {@code true} if this factory creates component objects from a
     * static context.
     */
    boolean isStatic();
    
    /**
     * <p>Initializes the factory with the specified {@code builder}. It is 
     * allowed for implementations to pre create one or more 
     * {@code ComponentObject}s.</p>
     * 
     * <p>The {@code builder} is used to create new {@code ComponentObject}s and
     * to request which {@code ComponentReference} is currently bounded to the
     * calling thread. This feature is only available to non-static factories,
     * as such information is not available for factories operating in a static
     * context.</p>
     * 
     * @param builder builder of {@code ComponentObject}s, which is aware of the 
     * static property of this factory.
     * @throws ComponentCreationException indicates that components cannot be 
     * created during initialization of this factory.
     */
    void init(ComponentObjectBuilder<T> builder) throws 
            ComponentCreationException;

    /**
     * <p>Allows the factory to optionally create and return a new 
     * {@code ComponentOject} for the bounded {@code ComponentReference}. If
     * a {@code ComponentObject} is indeed created it is returned, otherwise
     * {@code null} is returned.</p>
     * 
     * <p>This method is never called for static factories.</p>
     * 
     * @return a newly created {@code ComponentObject}, or {@code null} if no
     * object is created.
     * @throws ComponentCreationException indicate that component cannot be
     * created.
     */
    ComponentObject<T> create() throws ComponentCreationException;
    
    /**
     * Returns a {@code ComponentObject}. Which object is returned, or if a new 
     * {@code ComponentObject} must is created is not specified by this 
     * interface. Implementations can choose to use the bounded 
     * {@code ComponentReference} to determine which object to return in case
     * of a non-static factory.
     * 
     * @return a {@code ComponentObject}. This method never returns {@code null}.
     * @throws ComponentCreationException indicate that component cannot be
     * returned upon request.
     */
    ComponentObject<T> getComponentObject() throws ComponentCreationException;
    
    /**
     * Allows the factory to remove any {@code ComponentObject}s that are
     * related to the bounded {@code ComponentReference}.
     * 
     * <p>This method is never called for static factories.</p>
     */
    void remove();
    
    /**
     * The {@code destroy} method is is called by the container if no more 
     * objects will be requested from this factory. Implementations may choose
     * to destroy all {@code ComponentObject}s that have ever been created by
     * this factory.
     */
    void destroy();
}
