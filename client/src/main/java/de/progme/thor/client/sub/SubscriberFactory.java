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

package de.progme.thor.client.sub;

import de.progme.thor.client.sub.impl.SubscriberImpl;
import de.progme.thor.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public final class SubscriberFactory {

    private SubscriberFactory() {
        // no instance
    }

    /**
     * Creates a new subscriber instance which connects to the given host and port.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(String host, int port) {

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

        return new SubscriberImpl(host, port);
    }

    /**
     * Creates a new subscriber instance which connects to the given host and port.
     *
     * @param host           The host to connect to.
     * @param port           The port to connect to.
     * @param subscriberName The subscriber name.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(String host, int port, String subscriberName) {

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

        if (subscriberName == null || subscriberName.isEmpty()) {
            throw new IllegalArgumentException("subscriberName cannot be null or empty");
        }

        return new SubscriberImpl(host, port, subscriberName);
    }

    /**
     * Creates a new subscriber instance with the given name which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @param subscriberName The subscriber name.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(List<ClusterServer> clusterServers, String subscriberName) {

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        if (subscriberName == null || subscriberName.isEmpty()) {
            throw new IllegalArgumentException("subscriberName cannot be null or empty");
        }

        return new SubscriberImpl(clusterServers, subscriberName);
    }

    /**
     * Creates a new subscriber instance which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(List<ClusterServer> clusterServers) {

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        return new SubscriberImpl(clusterServers);
    }
}
