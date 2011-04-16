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

import java.util.LinkedList;
import java.util.NoSuchElementException;


/**
 * Optimized stack implementation that prevents object creation in most 
 * cases.
 */
final class JargoStack<T> {

    private T instance;
    private LinkedList<T> instances;
    
    public void push(T t) throws NullPointerException {
        if (t == null) {
            throw new NullPointerException();
        }
        if (instance == null) {
            instance = t;
        } else {
            if (instances == null) {
                instances = new LinkedList<T>();
            }
            instances.addFirst(instance);
            instance = t;
        }
    }
    
    public T poll() {
        final T t;
        if (instance == null) {
            t = null;
        } else {
            t = instance;
            if (instances == null) {
                instance = null;
            } else {
                instance = instances.poll();
            }
        }
        return t;
    }
    
    public T pop() throws NoSuchElementException {
        T t = poll();
        if (t == null) {
            throw new NoSuchElementException();
        }
        return t;
    }
    
    public T peek() {
        return instance;
    }
}