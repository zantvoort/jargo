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
import org.jargo.InvocationFactory;
import org.jargo.spi.InvocationFactoryProvider;
import org.jargo.ComponentConfiguration;

/**
 *
 * @author Leon van Zantvoort
 */
final class InvocationFactoryProviders extends 
        AbstractProviders<InvocationFactoryProvider> implements 
        InvocationFactoryProvider {
    
    public List<InvocationFactory> getInvocationFactories(ComponentConfiguration
            configuration) {
        // No need to cache result.
        List<InvocationFactory> generators = 
                new ArrayList<InvocationFactory>();
        for (InvocationFactoryProvider provider : getProviders()) {
            generators.addAll(provider.getInvocationFactories(configuration));
        }
        return Collections.unmodifiableList(generators);
    }
}
