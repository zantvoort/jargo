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

import org.jargo.container.ManagedComponentContext;
import java.util.List;
import org.jargo.ComponentAlias;
import org.jargo.ComponentNotFoundException;
import org.jargo.ComponentReference;
import org.jargo.EventFactory;
import org.jargo.ExecutorHandle;
import org.jargo.InvocationFactory;
import org.jargo.ComponentConfiguration;
import org.jargo.ComponentCreationException;
import org.jargo.ComponentExceptionHandler;
import org.jargo.ComponentFactory;
import org.jargo.ComponentLifecycle;
import org.jargo.ComponentMetaData;
import org.jargo.ComponentNotActiveException;
import org.jargo.ComponentObjectFactory;

/**
 * @author Leon van Zantvoort
 */
interface ComponentRegistry {

    Providers getProviders();
    
    void create(ComponentConfiguration<?> configuration);
    
    void destroy(String componentName);
    
    void activate(ManagedComponentContext<?> ctx);
    
    void addAlias(ComponentAlias alias);
    
    void removeAlias(ComponentAlias alias);
    
    /**
     * Returns true component name.
     */
    String getComponentName(String alias);
    
    boolean exists(String componentName, boolean useAlias);
    
    ComponentConfiguration<?> getComponentConfiguration(String componentName) 
            throws ComponentNotFoundException;
    
    ComponentFactory<?> lookup(String componentName) throws 
            ComponentNotFoundException;
    
    List<ComponentFactory<?>> list();
    
    <T> List<ComponentFactory<? extends T>> list(Class<T> type);
    
    <T> ComponentReference<T> createReference(
            ComponentConfiguration<T> configuration,
            Object info) throws ComponentCreationException, 
            ComponentNotActiveException;
    
    <T> ComponentObjectFactory<T> getComponentObjectFactory(
            ComponentConfiguration<T> configuration) throws 
            ComponentNotFoundException;

    <T> ComponentMetaData<T> getComponentMetaData(
            ComponentConfiguration<T> configuration) throws 
            ComponentNotFoundException;
    
    <T> List<ComponentLifecycle<T>> getComponentLifecycles(
            ComponentConfiguration<T> configuration) throws ComponentNotFoundException;
    
    EventFactory getEventFactory(ComponentConfiguration<?> configuration) throws 
            ComponentNotFoundException;
    
    InvocationFactory getInvocationFactory(
            ComponentConfiguration<?> configuration) throws 
            ComponentNotFoundException;
    
    ExecutorHandle getExecutorHandle(ComponentConfiguration<?> configuration) 
            throws ComponentNotFoundException;
    
    ComponentExceptionHandler getComponentExceptionHandler(
            ComponentConfiguration<?> configuration) throws 
            ComponentNotFoundException;
}
