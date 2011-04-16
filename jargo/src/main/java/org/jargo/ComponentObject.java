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
 * <p>The {@code ComponentObject} represents the lowest level wrapper for the
 * actual client components.</p>
 * 
 * <p>{@code ComponentObject}s are created by the container. During creation
 * the container invokes the {@code create} method of all registered 
 * {@code ComponentObjectLifecycle} for that particular component. These 
 * lifecycle methods are responsible for initializing the client component.
 * Depending on the {@code ComponentObjectLifecycle} implementations, this could
 * be similar to the EJB3 {@code PostConstruct}, or {@code ejbCreate} methods.
 * </p>
 * 
 * <p>Initialized {@code ComponentObject}s can perform operations on the client
 * components via the {@code execute} method.</p>
 * 
 * <p>The container destroys the {@code ComponentObject} by invoking the 
 * {@code destroy} method. This {@code destroy} call is delegated to all
 * registered {@code ComponentObjectLifecycle} objects. 
 * Depending on the {@code ComponentObjectLifecycle} implementations, this could
 * be similar to the EJB3 {@code PreDestroy}, or {@code ejbRemove} methods.</p>
 * 
 * @see ComponentObjectLifecycle
 * @author Leon van Zantvoort
 */
public interface ComponentObject<T> {
    
    /**
     * Executes the specified {@code event} on the underlying instance.
     * 
     * @return the result of the execution.
     * @throws ComponentEventException if execution result in an exception. 
     * This exception is wrapped in the {@code ComponentEventException}.
     */
    Object execute(Event event) throws ComponentEventException;
    
    /**
     * Returns the underlying instance on which all operations are performed.
     */
    T getInstance();
    
    /**
     * Invokes the destroy method of all {@code ComponentObjectLifecycle}s 
     * registered for this component object.
     * 
     * This method is idempotent.
     */
    void destroy();
}
