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

import java.util.List;

/**
 * <p>Provides information on the component definiton.</p>
 * 
 * @author Leon van Zantvoort
 */
public interface ComponentMetaData<T> extends MetaData {


    /**
     * Returns a string that is unique to this configuration within the Jargo container.
     */
    String getComponentId();

    /**
     * Returns the name of the component. Note uniqueness of this name is not guaranteed.
     */
    String getComponentName();

    /**
     * Returns the component unit of which this component is part of.
     */
    ComponentUnit getComponentUnit();

    /**
     * Returns the class type of the underlying component implementation.
     */
    Class<T> getType();
    
    /**
     * Returns a list of interfaces that are exposed by this component.
     */
    List<Class<?>> getInterfaces();
    
    /**
     * Returns {@code true} if the component returned by {@code getComponent}
     * is a proxy to the underlying component instance.
     */
    boolean isProxy();
    
    /**
     * Returns {@code true} if this component represents a vanilla object.
     */
    boolean isVanilla();

    /**
     * Returns {@code true} if this component is created from a static context.
     */
    boolean isStatic();
    
    /**
     * Returns a list of meta data objects of the component.
     */
    List<MetaData> getMetaData();
}
