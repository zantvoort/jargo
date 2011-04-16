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

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jargo.ComponentAlias;
import org.jargo.deploy.Deployer;
import org.jargo.deploy.Deployable;

/**
 * @author Leon van Zantvoort
 */
final class ComponentAliasDeployer implements Deployer {
    
    private final Logger logger;
    private final Lock lock;
    private final ComponentRegistry registry;
    
    public ComponentAliasDeployer(ComponentRegistry registry, Lock lock) {
        this.logger = Logger.getLogger(getClass().getName());
        this.registry = registry;
        this.lock = lock;
    }
    
    public void setParent(Deployer parent) {
    }
    
    public void deploy(Deployable deployable) throws Exception {
        if (deployable instanceof ComponentAliasDeployable) {
            List<ComponentAlias> aliases =
                    ((ComponentAliasDeployable) deployable).
                    getComponentAliases();
            
            boolean commit = false;
            lock.lock();
            try {
                for (ComponentAlias alias : aliases) {
                    logger.info("Adding " +
                            (alias.override() ? "override-" : "") + "alias '" +
                            alias.getComponentAlias() + "' for component '" +
                            alias.getComponentName() + "'.");
                    registry.addAlias(alias);
                }
                commit = true;
            } finally {
                try {
                    if (!commit) {
                        for (ComponentAlias alias : aliases) {
                            try {
                                logger.info("Removing " +
                                        (alias.override() ? "override-" : "") + "alias '" +
                                        alias.getComponentAlias() + "' for component '" +
                                        alias.getComponentName() + "'.");
                                registry.removeAlias(alias);
                            } catch (RuntimeException e) {
                                logger.log(Level.WARNING, "THROW", e);
                            }
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }
    
    public void undeploy(Deployable deployable) {
        if (deployable instanceof ComponentAliasDeployable) {
            List<ComponentAlias> aliases =
                    ((ComponentAliasDeployable) deployable).
                    getComponentAliases();
            
            lock.lock();
            try {
                for (ComponentAlias alias : aliases) {
                    try {
                        logger.info("Removing " +
                                (alias.override() ? "override-" : "") + "alias '" +
                                alias.getComponentAlias() + "' for component '" +
                                alias.getComponentName() + "'.");
                        registry.removeAlias(alias);
                    } catch (RuntimeException e) {
                        logger.log(Level.WARNING, "THROW", e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
