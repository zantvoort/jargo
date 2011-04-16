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
package org.jargo.deploy;

/**
 * Installs deployable objects. Note that deployers can be called concurrently
 * from multiple threads.
 *
 * @author Leon van Zantvoort
 * @see Deployable
 */
public interface Deployer extends Deployable {
    
    /**
     * A deployer is always associated with a parent deployer (except for the
     * root deployer). 
     */
    void setParent(Deployer deployer);
    
    /**
     * If the specified deployable object is supported by this deployer, the
     * deployer can install the object. If not, the deployer may simply ignore
     * the object. The deployer is allowed to submit new deployable objects to
     * the parent deployer from this method.
     */
    void deploy(Deployable deployable) throws Exception;
    
    /**
     * If the specified deployable object is supported by this deployer, the
     * deployer can deinstall the object. If not, the deployer may simply ignore
     * the object. The deployer is allowed to submit new deployable objects to
     * the parent deployer from this method.
     */
    void undeploy(Deployable deployable) throws Exception;
}
