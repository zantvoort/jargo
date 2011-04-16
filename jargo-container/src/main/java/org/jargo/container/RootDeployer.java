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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import org.jargo.deploy.Deployer;
import org.jargo.deploy.Deployable;

/**
 * @author Leon van Zantvoort
 */
final class RootDeployer implements Deployer {
    
    private final Collection<Deployer> deployers;
    
    public RootDeployer() {
        this.deployers = new CopyOnWriteArraySet<Deployer>();
    }

    public void setParent(Deployer parent) {
    }

    public void deploy(final Deployable deployable) throws Exception {
        for (Deployer deployer : deployers) {
            deployer.deploy(deployable);
        }
        if (deployable instanceof Deployer) {
            ((Deployer) deployable).setParent(this);
            deployers.add((Deployer) deployable);
        }
    }
    
    @SuppressWarnings("finally")
    public void undeploy(final Deployable deployable) throws Exception {
        if (deployable instanceof Deployer) {
            deployers.remove(deployable);
        }
        Exception caught = null;
        for (Deployer deployer : deployers) {
            try {
                deployer.undeploy(deployable);
            } catch (Exception e) {
                if (caught == null) {
                    caught = e;
                }
            } finally {
                continue;
            }
        }
        if (caught != null) {
            throw caught;
        }
    }
}
