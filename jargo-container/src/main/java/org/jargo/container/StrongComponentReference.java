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

import org.jargo.ComponentEventException;
import org.jargo.ComponentMetaData;
import org.jargo.ComponentReference;
import org.jargo.Event;

/**
 * 
 * @author Leon van Zantvoort
 */
final class StrongComponentReference<T> implements ComponentReference<T> {
    
    private final WeakComponentReference<T> reference;
    
    private Object component;
    
    public StrongComponentReference(WeakComponentReference<T> reference) {
        this.reference = reference;
    }

    public ComponentMetaData<T> getComponentMetaData() {
        return reference.getComponentMetaData();
    }
    
    public Object getInfo() {
        return reference.getInfo();
    }

    public void setComponent(Object component) {
        this.component = component;
    }
    
    public Object getComponent() {
        return component;
    }
    
    public boolean isExecutable(Event event) {
        return reference.isExecutable(event);
    }

    public Object execute(Event event) throws ComponentEventException {
        return reference.doExecute(event, this);
    }
    
    public void invalidate() {
        reference.invalidate();
    }

    public boolean isValid() {
        return reference.isValid();
    }
    
    public void remove() {
        reference.remove();
    }
    
    public boolean isRemoved() {
        return reference.isRemoved();
    }
 
    public boolean isWeak() {
        return weakReference() == this;
    }
    
    public ComponentReference<T> weakReference() {
        return reference;
    }

    public void addDestroyHook(Runnable hook) {
        reference.addDestroyHook(hook);
    }
    
    public boolean removeDestroyHook(Runnable hook) {
        return reference.removeDestroyHook(hook);
    }

    @Override 
    public int hashCode() {
        return reference.hashCode();
    }
    
    @Override 
    public boolean equals(Object obj) {
        final boolean value;
        if (obj instanceof ComponentReference) {
            value = reference == ((ComponentReference) obj).weakReference();
        } else {
            value = false;
        }
        return value;
    }
    
    @Override 
    public String toString() {
        return reference.toString();
    }
}
