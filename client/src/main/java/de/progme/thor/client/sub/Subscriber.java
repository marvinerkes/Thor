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

import de.progme.thor.client.sub.impl.handler.ChannelHandler;
import de.progme.thor.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public interface Subscriber {

    /**
     * Disconnects the subscriber.
     * If you are not forcing a disconnect, this method will block until the reconnect is successful.
     *
     * @param force If true, the subscriber will not try to reconnect
     */
    void disconnect(boolean force);

    /**
     * Disconnects the subscriber without trying to reconnect.
     *
     * Same as invoking disconnect(true).
     */
    void disconnect();

    /**
     * Checks if the given channel is subscribed.
     *
     * @param channel The channel to check.
     * @return True if it is subscribed, otherwise false.
     */
    boolean hasSubscription(String channel);

    /**
     * Subscribes a channel and sets the handler for it.
     * <p>
     * If the channel is already subscribed, the channel handler will be overwritten.
     *
     * @param channel The channel to subscribe to.
     * @param handler The handler which is responsible for the messages received in that channel.
     */
    void subscribe(String channel, Class<? extends ChannelHandler> handler);

    /**
     * Subscribes a channel and sets the handler for it.
     * The class must have a Channel annotation with the channel the class is responsible for.
     * <p>
     * If the channel is already subscribed, the channel handler will be overwritten.
     *
     * @param handler The handler which is responsible for the messages received in that channel.
     */
    void subscribe(Class<? extends ChannelHandler> handler);

    /**
     * Subscribes a channel and sets the multi handler for it.
     * The class must have a Channel annotation with the channel the class is responsible for.
     * <p>
     * If the channel is already subscribed, the multi channel handler will be overwritten.
     *
     * @param handler The handler which is responsible for the messages received in that channel and the key value method matches.
     */
    void subscribeMulti(Class<?> handler);

    /**
     * Unsubscribe a channel.
     *
     * @param channel The channel to unsubscribe.
     */
    void unsubscribe(String channel);

    /**
     * Returns if the subscriber is connected.
     *
     * @return True if connected, otherwise false.
     */
    boolean connected();

    /**
     * Returns the name of the subscriber.
     *
     * @return The name.
     */
    String name();

    /**
     * Returns an unmodifiable list of the cluster servers.
     *
     * @return The unmodifiable list of the cluster servers.
     */
    List<ClusterServer> clusterServers();
}
