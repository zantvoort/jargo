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

import org.jargo.deploy.Deployer;
import org.jargo.deploy.Deployable;
import org.jargo.spi.ClassLoaderProvider;
import org.jargo.spi.ComponentAliasProvider;
import org.jargo.spi.ComponentExceptionHandlerProvider;
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
import org.jargo.spi.ComponentLifecycleProvider;
import org.jargo.spi.ComponentReferenceLifecycleProvider;
import org.jargo.spi.LifecycleProvider;
import org.jargo.spi.MetaDataProvider;

/**
 * @author Leon van Zantvoort
 */
final class ProvidersImpl implements Providers, Deployer {
    
    private ClassLoaderProvider classLoaderProvider;
    private DependencyInspectorProvider dependencyInspectorProvider;
    private EventExecutorProvider eventExectorProvider;
    private EventFactoryProvider eventFactoryProvider;
    private EventInterceptorFactoryProvider eventInterceptorProvider;
    private InjectionFactoryProvider injectorProvider;
    private InvocationFactoryProvider invocationFactoryProvider;
    private InvocationInterceptorFactoryProvider invocationInterceptorProvider;
    private ComponentConfigurationProvider componentConfigurationProvider;
    private ComponentAliasProvider componentAliasProvider;
    private ComponentExceptionHandlerProvider componentExceptionHandlerProvider;
    private ComponentLifecycleProvider componentLifecycleProvider;
    private ComponentObjectFactoryProvider componentObjectFactoryProvider;
    private ComponentReferenceLifecycleProvider componentReferenceLifecycleProvider;
    private ExecutorHandleProvider executorHandleProvider;
    private ObjectFactoryProvider objectFactoryProvider;
    private LifecycleProvider lifecycleProvider;
    private MetaDataProvider metaDataProvider;
    
    public void setParent(Deployer parent) {
    }
    
    public void deploy(Deployable deployable) {
        if (deployable instanceof ClassLoaderProviders) {
            classLoaderProvider = (ClassLoaderProviders) deployable;
        } else if (deployable instanceof DependencyInspectorProviders) {
            dependencyInspectorProvider = (DependencyInspectorProviders) deployable;
        } else if (deployable instanceof EventExecutorProviders) {
            eventExectorProvider = (EventExecutorProviders) deployable;
        } else if (deployable instanceof EventFactoryProviders) {
            eventFactoryProvider = (EventFactoryProviders) deployable;
        } else if (deployable instanceof EventInterceptorFactoryProviders) {
            eventInterceptorProvider = (EventInterceptorFactoryProviders) deployable;
        } else if (deployable instanceof InjectionFactoryProviders) {
            injectorProvider = (InjectionFactoryProviders) deployable;
        } else if (deployable instanceof InvocationFactoryProviders) {
            invocationFactoryProvider = (InvocationFactoryProviders) deployable;
        } else if (deployable instanceof InvocationInterceptorFactoryProviders) {
            invocationInterceptorProvider = (InvocationInterceptorFactoryProviders) deployable;
        } else if (deployable instanceof ComponentConfigurationProviders) {
            componentConfigurationProvider = (ComponentConfigurationProviders) deployable;
        } else if (deployable instanceof ComponentAliasProviders) {
            componentAliasProvider = (ComponentAliasProviders) deployable;
        } else if (deployable instanceof ComponentExceptionHandlerProvider) {
            componentExceptionHandlerProvider = (ComponentExceptionHandlerProvider) deployable;
        } else if (deployable instanceof ComponentLifecycleProviders) {
            componentLifecycleProvider = (ComponentLifecycleProviders) deployable;
        } else if (deployable instanceof ComponentObjectFactoryProviders) {
            componentObjectFactoryProvider = (ComponentObjectFactoryProviders) deployable;
        } else if (deployable instanceof ComponentReferenceLifecycleProviders) {
            componentReferenceLifecycleProvider = (ComponentReferenceLifecycleProviders) deployable;
        } else if (deployable instanceof ExecutorHandleProviders) {
            executorHandleProvider = (ExecutorHandleProviders) deployable;
        } else if (deployable instanceof ObjectFactoryProviders) {
            objectFactoryProvider = (ObjectFactoryProviders) deployable;
        } else if (deployable instanceof LifecycleProviders) {
            lifecycleProvider = (LifecycleProviders) deployable;
        } else if (deployable instanceof MetaDataProviders) {
            metaDataProvider = (MetaDataProviders) deployable;
        }
    }
    
    public void undeploy(Deployable deployable) {
    }

    public ClassLoaderProvider getClassLoaderProvider() {
        return classLoaderProvider;
    }

    public DependencyInspectorProvider getDependencyInspectorProvider() {
        return dependencyInspectorProvider;
    }
    
    public EventExecutorProvider getEventExecutorProvider() {
        return eventExectorProvider;
    }

    public EventFactoryProvider getEventFactoryProvider() {
        return eventFactoryProvider;
    }
    
    public EventInterceptorFactoryProvider getEventInterceptorFactoryProvider() {
        return eventInterceptorProvider;
    }

    public InjectionFactoryProvider getInjectionFactoryProvider() {
        return injectorProvider;
    }

    public InvocationFactoryProvider getInvocationFactoryProvider() {
        return invocationFactoryProvider;
    }

    public InvocationInterceptorFactoryProvider getInvocationInterceptorFactoryProvider() {
        return invocationInterceptorProvider;
    }

    public ComponentConfigurationProvider getComponentConfigurationProvider() {
        return componentConfigurationProvider;
    }

    public ComponentAliasProvider getComponentAliasProvider() {
        return componentAliasProvider;
    }
    
    public ComponentExceptionHandlerProvider getComponentExceptionHandlerProvider() {
        return componentExceptionHandlerProvider;
    }
    
    public ComponentLifecycleProvider getComponentLifecycleProvider() {
        return componentLifecycleProvider;
    }

    public ComponentObjectFactoryProvider getComponentObjectFactoryProvider() {
        return componentObjectFactoryProvider;
    }
    
    public ComponentReferenceLifecycleProvider getComponentReferenceLifecycleProvider() {
        return componentReferenceLifecycleProvider;
    }

    public ExecutorHandleProvider getExecutorHandleProvider() {
        return executorHandleProvider;
    }
    
    public ObjectFactoryProvider getObjectFactoryProvider() {
        return objectFactoryProvider;
    }

    public LifecycleProvider getLifecycleProvider() {
        return lifecycleProvider;
    }

    public MetaDataProvider getMetaDataProvider() {
        return metaDataProvider;
    }
}
