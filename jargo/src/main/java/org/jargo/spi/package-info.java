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

/**
 * <p>Jargo is a framework for building application containers. It does not 
 * provide any container logic, it merely provides a pluggable architecture
 * typically for IoC containers. In other words, the implementation for building
 * an application container must be loaded in addition to Jargo. This is where
 * the {@code org.jargo.spi} package comes in. This package contains a set of 
 * <b>Service Provider Interface</b>s, which form the gateway to the actual 
 * implementation of the application container. All provider interfaces listed 
 * in this package extend {@link org.jargo.spi.Provider}, the base interface for 
 * all providers. Implementation for these provider interfaces is hooked into 
 * the container framework at startup by means of <b>jar-file service 
 * discovery</b>.</p>
 * 
 * <p>In more detail, the container framework queries the classloader that is 
 * responsible for loading the container runtime for all files referenced by the 
 * path {@code "META-INF/services/org.jargo.deploy.Deployable"}. These files 
 * contain class names of concrete implementations for the provider interfaces 
 * mentioned above. These classes are instantiated and installed in the 
 * container framework.</p>
 * 
 * <p>Once the container framework has installed its logic by through the 
 * provider interfaces it is ready to start the actual application container. 
 * The container frameworks scans for application jar-files and submits these to 
 * the chain of provider interfaces, which is outlined by the following 
 * paragraps.</p>
 * 
 * <p>A jar-file is identified by a URL. This URL is passed to the registered
 * {@link org.jargo.spi.ClassLoaderProvider}s. The resulting classloader is
 * wrapped by the container in a {@code ComponentUnit} object.</p>
 * 
 * <p>Next, the container framework iterates through all 
 * {@link org.jargo.spi.ComponentAliasProvider}s and 
 * {@link org.jargo.spi.ComponentConfigurationProvider}s to lookup Jargo 
 * components. The aliases and component configurations are registered 
 * internally.</p>
 * 
 * <p>Registration of component configurations starts with obtaining a
 * {@code ComponentObjectFactory} from the 
 * {@link org.jargo.spi.ComponentObjectFactoryProvider}. The Jargo component is
 * a vanilla component, unless the {@code ComponentObjectFactory} instance 
 * implements the {@code ComponentObjectPool} interface, a sub interface of 
 * {@code ComponentObjectFactory}. Pooled components do not expose the actual
 * objects directly to the client application. Instead a stub, implementing the
 * proxy pattern, interacts with the {@code ComponentObjectPool} to delegate
 * business method invocations to one of the pooled objects. Additionally,
 * the container framework looks up optional {@code EvenFactory}s, 
 * {@code InvocationFactory}s, a {@code ComponentExceptionHandler} and an 
 * {@code ExecutorHandle} through the
 * {@link org.jargo.spi.EventFactoryProvider}, 
 * {@link org.jargo.spi.InvocationFactoryProvider},
 * {@link org.jargo.spi.ComponentExceptionHandlerProvider} and 
 * {@link org.jargo.spi.ExecutorHandleProvider} respectively. The resulting
 * objects are used to construct a {@code ComponentMetaData} object for the 
 * component. The component meta data object holds zero or more 
 * {@code MetaData} objects, which are requested from the 
 * {@link org.jargo.spi.MetaDataProvider}.</p>
 * 
 * <p>The container framework now loads an optional {@code ComponentLifecycle}
 * from the {@link org.jargo.spi.ComponentLifecycleProvider} using the 
 * {@code Executor} obtained through the {@code ExexcutorHandleProvider}.</p>
 * 
 * <p>The container iterates through all components of this unit and activates
 * them individually. This is done by invoking the component's 
 * {@code ComponentObjectFactory.init()} method. This {@code init} method can be 
 * used to pre-load {@code ComponentObject}s. From this point the component is 
 * said to be activated.</p>
 * 
 * <p>For those components for which a {@code ComponentLifecycle} exist, the
 * {@code create} method is called. This lifecycle method allow components to
 * be created on deployment. This is typically used for constructing 
 * {@code static} components.</p>
 * 
 * <!--
 * ComponentReferenceLifecycleProvider
 * DependencyInspectorProvider
 * EventExecutorProvider
 * EventInterceptorFactoryProvider
 * InjectionFactoryProvider
 * InvocationInterceptorFactoryProvider
 * LifecycleProvider
 * ObjectFactoryProvider
 * -->
 */
package org.jargo.spi;

