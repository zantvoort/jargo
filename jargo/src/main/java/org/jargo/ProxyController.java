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
package org.jargo;

// Created by container

/**
 * 
 * @author Leon van Zantvoort
 */
public interface ProxyController {

    /**
     * Returns {@code true} if the subsequent method calls on the controlled
     * proxy object from this thread must not be intercepted by the 
     * proxy. Instead, the original method implementation must be invoked 
     * directly.
     */
    boolean isNoOp();
    
    /**
     * Pushes the specified {@code noOp} flag to the calling thread's noOp stack.
     * 
     * @param noOp {@code true} if subsequent calls from this thread must not be
     * intercepted by the proxy, {@code false} otherwise.
     */
    void attach(boolean noOp);
    
    /**
     * Pops a noOp flag from the calling thread's noOp stack.
     */
    void detach();
}
