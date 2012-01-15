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

/**
 * This class as the same semantics as <code>ThreadLocal</code>.
 *
 * @author Leon van Zantvoort
 */
class MemorySafeThreadLocal<T> {

    private static class Nullable<T> {
        private T value;
        public Nullable(T value) {
            this.value = value;
        }
    }
    
    private final MemorySafeReference<ThreadLocal<Nullable<T>>> threadLocal = new MemorySafeReference<ThreadLocal<Nullable<T>>>(new ThreadLocal<Nullable<T>>());

    public T get() {
        Nullable<T> nullable = threadLocal.get().get();
        if (nullable == null) {
            nullable = new Nullable<T>(initialValue());
            threadLocal.get().set(nullable);
        }
        return nullable.value;
    }
    
    public void set(T value) {
        threadLocal.get().set(new Nullable<T>(value));
    }
    
    protected T initialValue() {
        return null;
    }
}
