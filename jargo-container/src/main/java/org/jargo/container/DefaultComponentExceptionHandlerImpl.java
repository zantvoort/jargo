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

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import org.jargo.ComponentException;
import org.jargo.ComponentExceptionHandler;
import org.jargo.ComponentExecutionException;

/**
 *
 * @author Leon van Zantvoort
 */
final class DefaultComponentExceptionHandlerImpl implements 
        ComponentExceptionHandler {
    
    public void onException(Method method, ComponentException e) throws Throwable {
        try {
            try {
                throw e;
            } catch (ComponentExecutionException e2) {
                throw e2.getCause();
            }
        } catch (Throwable e2) {
            // Check if exception is supported by throws clause of underlying method.
            boolean supported = e2 instanceof RuntimeException || e2 instanceof Error;
            if (!supported) {
                for (Class<?> cls : method.getExceptionTypes()) {
                    if (cls.isAssignableFrom(e.getClass())) {
                        supported = true;
                        break;
                    }
                }
            }
            if (supported) {
                throw e2;
            } else {
                throw new UndeclaredThrowableException(e2);
            }
        }
    }
}
