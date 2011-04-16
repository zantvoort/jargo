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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jargo.ComponentContext;
import org.jargo.ConstructorInjection;
import org.jargo.InjectionFactory;
import org.jargo.spi.InjectionFactoryProvider;
import org.jargo.ComponentConfiguration;
import org.jargo.SetterInjection;

/**
 * @author Leon van Zantvoort
 */
final class InjectionFactories<T> implements InjectionFactory<T> {
    
    private final ComponentConfiguration<T> configuration;
    private final InjectionFactoryProvider provider;
    
    public InjectionFactories(ComponentConfiguration<T> configuration, Providers providers) {
        this.configuration = configuration;
        this.provider = providers.getInjectionFactoryProvider();
    }

    public ConstructorInjection<T> getConstructorInjection(
            Class<?> cls, ComponentContext<T> ctx) {
        ConstructorInjection<T> injection = null;
        for (InjectionFactory<T> factory : provider.getInjectionFactories(configuration)) {
            injection = factory.getConstructorInjection(cls, ctx);
            if (injection != null) {
                break;
            }
        }
        return injection;
    }
    
    public List<SetterInjection> getSetterInjections(Class<?> cls, 
            ComponentContext<T> ctx) {
        List<SetterInjection> injections = new ArrayList<SetterInjection>();
        for (InjectionFactory<T> factory : provider.getInjectionFactories(configuration)) {
            injections.addAll(factory.getSetterInjections(cls, ctx));
        }
        return Collections.unmodifiableList(injections);
    }
}
