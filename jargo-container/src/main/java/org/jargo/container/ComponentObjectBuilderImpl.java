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

import org.jargo.ComponentConfiguration;
import org.jargo.ComponentCreationException;
import org.jargo.ComponentObject;
import org.jargo.ComponentObjectBuilder;
import org.jargo.ComponentReference;
import org.jargo.ComponentStateException;

/**
 * @author Leon van Zantvoort
 */
final class ComponentObjectBuilderImpl<T> implements ComponentObjectBuilder<T> {
    
    private final ManagedComponentContext<T> ctx;
    private final ComponentConfiguration<T> configuration;
    private final ComponentRegistry registry;
    private final boolean statik;
    
    public ComponentObjectBuilderImpl(ManagedComponentContext<T> ctx,
            ComponentConfiguration<T> configuration, 
            ComponentRegistry registry) {
        this.ctx = ctx;
        this.configuration = configuration;
        this.registry = registry;
        this.statik = ctx.getComponentMetaData().isStatic();
    }

    public ComponentObject<T> newInstance() throws ComponentCreationException {
        return new ComponentObjectImpl<T>(ctx, configuration, registry);
    }
    
    public ComponentReference<T> reference() throws ComponentStateException {
        if (statik) {
            throw new ComponentStateException(configuration.getComponentName(),
                    "Call not allowed for static components.");
        }
        ComponentReference<T> reference = ctx.reference();
        assert reference != null;
        return reference;
    }
    
    public void attach(ComponentReference<T> reference) throws 
            ComponentStateException {
        if (statik) {
            throw new ComponentStateException(configuration.getComponentName(),
                    "Call not allowed for static components.");
        }
        ctx.attach(reference);
    }

    public void detach() throws ComponentStateException {
        if (statik) {
            throw new ComponentStateException(configuration.getComponentName(),
                    "Call not allowed for static components.");
        }
        ctx.detach();
    }
}
