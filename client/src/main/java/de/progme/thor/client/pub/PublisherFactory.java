/*
 * Copyright (c) 2017 Marvin Erkes
 *
 * This file is part of Thor.
 *
 * Thor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.progme.thor.client.pub;

import de.progme.thor.client.pub.impl.PublisherImpl;
import de.progme.thor.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public final class PublisherFactory {

    private PublisherFactory() {
        // no instance
    }

    /**
     * Creates a new publisher instance which connects to the given host and port.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A new instance of a publisher implementation.
     */
    public static Publisher create(String host, int port) {

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

        return new PublisherImpl(host, port);
    }

    /**
     * Creates a new publisher instance which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @return A new instance of a publisher implementation.
     */
    public static Publisher create(List<ClusterServer> clusterServers) {

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        return new PublisherImpl(clusterServers);
    }
}
