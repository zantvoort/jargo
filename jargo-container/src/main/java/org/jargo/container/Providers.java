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

import org.jargo.deploy.Deployable;
import org.jargo.spi.ClassLoaderProvider;
import org.jargo.spi.ComponentAliasProvider;
import org.jargo.spi.DependencyInspectorProvider;
import org.jargo.spi.EventExecutorProvider;
import org.jargo.spi.EventFactoryProvider;
import org.jargo.spi.EventInterceptorFactoryProvider;
import org.jargo.spi.ExecutorHandleProvider;
import org.jargo.spi.InjectionFactoryProvider;
import org.jargo.spi.InvocationFactoryProvider;
import org.jargo.spi.InvocationInterceptorFactoryProvider;
import org.jargo.spi.ObjectFactoryProvider;
import org.jargo.spi.ComponentObjectFactoryProvider;
import org.jargo.spi.ComponentConfigurationProvider;
import org.jargo.spi.ComponentExceptionHandlerProvider;
import org.jargo.spi.ComponentLifecycleProvider;
import org.jargo.spi.ComponentReferenceLifecycleProvider;
import org.jargo.spi.LifecycleProvider;
import org.jargo.spi.MetaDataProvider;

/**
 * @author Leon van Zantvoort
 */
interface Providers extends Deployable {
    
    ClassLoaderProvider getClassLoaderProvider();

    DependencyInspectorProvider getDependencyInspectorProvider();
    
    EventExecutorProvider getEventExecutorProvider();

    EventFactoryProvider getEventFactoryProvider();
    
    EventInterceptorFactoryProvider getEventInterceptorFactoryProvider();

    InjectionFactoryProvider getInjectionFactoryProvider();

    InvocationFactoryProvider getInvocationFactoryProvider();

    InvocationInterceptorFactoryProvider getInvocationInterceptorFactoryProvider();

    ComponentConfigurationProvider getComponentConfigurationProvider();

    ComponentAliasProvider getComponentAliasProvider();

    ComponentExceptionHandlerProvider getComponentExceptionHandlerProvider();
    
    ComponentObjectFactoryProvider getComponentObjectFactoryProvider();

    ComponentLifecycleProvider getComponentLifecycleProvider();

    ComponentReferenceLifecycleProvider getComponentReferenceLifecycleProvider();

    ExecutorHandleProvider getExecutorHandleProvider();
    
    ObjectFactoryProvider getObjectFactoryProvider();

    LifecycleProvider getLifecycleProvider();

    MetaDataProvider getMetaDataProvider();
}
