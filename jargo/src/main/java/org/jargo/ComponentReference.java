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
 * A component reference is created upon client request, through a 
 * <tt>ComponentContext</tt> instance.
 * 
 * @author Leon van Zantvoort
 * @see ComponentContext
 */
public interface ComponentReference<T> extends Destroyable {

    ComponentMetaData<T> getComponentMetaData();
    
    /**
     * Returns the object associated with this component reference.
     */
    Object getInfo();
    
    /**
     * Returns a component stub. This stub is always related to this component
     * reference object. Method invocations on the component stub cause this
     * component reference to be associated with the component context of
     * the underlying component instance.
     */
    Object getComponent();

    /**
     * Returns {@code true} if specified {@code event} is supported by this
     * reference.
     */
    boolean isExecutable(Event event);
    
    /**
     * Executes the specified {@code event}.
     * 
     * @throws ComponentExecutionExceptionresult of event execution.
     */
    Object execute(Event event) throws ComponentEventException;

    /**
     * Causes this reference to be invalidated and removed. As a result,
     * {@code isValid} returns {@code false}. Events can still be executed
     * while the reference is being invalidated.
     * 
     * This method is idempotent.
     */
    void invalidate();
    
    /**
     * Returns {@code true} if this reference if valid. If {@code false} is 
     * returned, the reference is being destroyed and finally removed, resulting
     * in {@code isRemoved} to return {@code true}.
     *
     * @return {@code true} if this reference is valid, {@code false} otherwise.
     */
    boolean isValid();
    
    /**
     * Remove the reference immediately. The reference is not being invalidated.
     */
    void remove();
    
    /**
     * Returns {@code true} if this reference has been removed, it
     * can no longer execute events, {@code false} otherwise.
     *
     * @return {@code true} if this reference is removed, {@code false} otherwise.
     */
    boolean isRemoved();
    
    /**
     * Returns {@code true} if this reference represents a weak component
     * reference. {@code false} if this reference represents a strong component
     * reference.
     *
     * <p>The container automatically invalidates component references that are 
     * no longer strongly reachable. If an application no longer points to a
     * particular strong component reference, the reference is said to be 
     * weakly reachable and is to be invalidated by the container.
     * 
     * Non proxy components returned by {@code getComponent} keep a reference 
     * to the component reference used to created the component. Hence, weak 
     * component references and their components do not prevent the component 
     * reference from being invalidated.
     * 
     * @return {@code true} if this reference represents a weak reference, 
     * {@code false} if this reference represents a strong reference.
     */
    boolean isWeak();

    /**
     * Returns the weak {@code ComponentReference} counterpart for {@code this}
     * reference.
     */
    ComponentReference<T> weakReference();
}
