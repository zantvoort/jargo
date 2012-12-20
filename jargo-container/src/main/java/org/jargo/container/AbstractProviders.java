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

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.jargo.ComponentApplicationException;
import org.jargo.deploy.Deployable;
import org.jargo.deploy.Deployer;
import org.jargo.deploy.SequentialDeployable;
import org.jargo.spi.Provider;

/**
 * @author Leon van Zantvoort
 */
abstract class AbstractProviders<T extends Provider> implements 
        Provider, Deployer {
    
    private final List<T> internalProviders;
    private final AtomicReference<List<T>> providers;
    
    private final Class<T> type;
    
    public AbstractProviders() {
        List<T> tmp = Collections.emptyList();
        this.internalProviders = new CopyOnWriteArrayList<T>();
        this.providers = new AtomicReference<List<T>>(tmp);
        
        Class sub = getClass();
        while (!sub.getSuperclass().equals(AbstractProviders.class)) {
            sub = sub.getSuperclass();
        }
        
        Type superType = sub.getGenericSuperclass();
        if (superType instanceof ParameterizedType) {
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) ((ParameterizedType) superType).
                    getActualTypeArguments()[0]; 
            this.type = type;
        } else {
            throw new ComponentApplicationException(
                    "Failed to list parameterized type: " + getClass() + ".");
        }
    }
    
    public List<T> getProviders() {
        return providers.get();
    }
    
    public Sequence sequence(SequentialDeployable deployable) {
        return Sequence.EQUAL;
    }
    
    public void setParent(Deployer deployer) {
    }

    public void deploy(Deployable deployable) throws Exception {
        if (type.isInstance(deployable)) {
            @SuppressWarnings("unchecked")
            T t = (T) deployable;
            internalProviders.add(t);
            providers.set(getSortedList());
        }
    }
    
    public void undeploy(Deployable deployable) throws Exception {
        if (type.isInstance(deployable)) {
            internalProviders.remove(deployable);
            providers.set(getSortedList());
        }
    }
    
    private List<T> getSortedList() {
        List<T> newList = new ArrayList<T>(internalProviders);
        Collections.sort(newList, new SequentialDeployableComparator<T>());
        return Collections.unmodifiableList(newList);
    }
    
    private static final class SequentialDeployableComparator<T extends Provider> 
            implements Comparator<T>, Serializable {

        private static final long serialVersionUID = -7664394978854897001L;
        
        public int compare(T t1, T t2) {
            return getValue(t1.sequence(t2)) - getValue(t2.sequence(t1));
        }

        private int getValue(SequentialDeployable.Sequence seq) {
            switch (seq) {
                case BEFORE:
                    return -1;
                case EQUAL:
                    return 0;
                case AFTER:
                    return 1;
                default:
                    return 0;
            }
        }
    }
}
