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

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Memory safe references are references that can be centrally cleared.
 *
 * @author Leon van Zantvoort
 */
final class MemorySafeReference<T> {

    private static final Set<MemorySafeReference<?>> references =
            Collections.newSetFromMap(new WeakHashMap<MemorySafeReference<?>, Boolean>());
    
    private T referent;
    
    public MemorySafeReference(T value) {
        synchronized (references) {
            set(value);
            references.add(this);
        }
    }
    
    public T get() {
        return referent;
    }

    private void set(T value) {
        this.referent = value;
    }

    /**
     * Cleans all referents.
     */
    public static void clear() {
        synchronized (references) {
            for (Iterator<MemorySafeReference<?>> i = references.iterator(); i.hasNext();) {
                i.next().set(null);
                i.remove();
            }
        }
    }
}
