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

import de.progme.thor.shared.config.ClusterServer;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public interface Publisher {

    /**
     * Disconnects the publisher.
     *
     * @param force If true, the publisher will not try to reconnect
     */
    void disconnect(boolean force);

    /**
     * Disconnects the publisher without trying to reconnect.
     *
     * Same as invoking disconnect(true).
     */
    void disconnect();

    /**
     * Publishes the given JSONObject to the given channel.
     *
     * @param channel    The channel.
     * @param jsonObject The json as JSONObject.
     */
    void publish(String channel, JSONObject jsonObject);

    /**
     * Publishes the given JSONObject's to the given channel.
     *
     * @param channel     The channel.
     * @param jsonObjects The JSONObject's.
     */
    void publishAll(String channel, JSONObject... jsonObjects);

    /**
     * Publishes the given JSONObject to the given channel and subscriber.
     *
     * @param channel        The channel.
     * @param subscriberName The name of the subscriber to publish to.
     * @param jsonObject     The json as JSONObject.
     */
    void publish(String channel, String subscriberName, JSONObject jsonObject);

    /**
     * Publishes the given JSONObject's to the given channel and subscriber.
     *
     * @param channel        The channel.
     * @param subscriberName The name of the subscriber to publish to.
     * @param jsonObjects    The JSONObject's.
     */
    void publishAll(String channel, String subscriberName, JSONObject... jsonObjects);

    /**
     * Publishes the given json to the given channel.
     *
     * @param channel The channel.
     * @param json    The raw json string.
     */
    void publish(String channel, String json);

    /**
     * Publishes the given json to the given channel and subscriber.
     *
     * @param channel        The channel.
     * @param json           The raw json string.
     * @param subscriberName The name of the subscriber to publish to.
     */
    void publish(String channel, String json, String subscriberName);

    /**
     * Publishes the given object as json string to the given channel.
     * The object will be serialized with gson to a json string.
     *
     * @param channel The channel.
     * @param object  The object which can be serialized with gson.
     */
    void publish(String channel, Object object);

    /**
     * Publishes the given objects as json string to the given channel.
     * The objects will be serialized with gson to a json string.
     *
     * @param channel The channel.
     * @param objects The objects which can be serialized with gson.
     */
    void publishAll(String channel, Object... objects);

    /**
     * Publishes the given object as json string to the given channel.
     * The object will be serialized with gson to a json string.
     *
     * @param channel        The channel.
     * @param subscriberName The name of the subscriber to publish to.
     * @param object         The object which can be serialized with gson.
     */
    void publish(String channel, String subscriberName, Object object);

    /**
     * Publishes the given objects as json string to the given channel.
     * The objects will be serialized with gson to a json string.
     *
     * @param channel        The channel.
     * @param subscriberName The name of the subscriber to publish to.
     * @param objects        The objects which can be serialized with gson.
     */
    void publishAll(String channel, String subscriberName, Object... objects);

    /**
     * Returns if the publisher is connected.
     *
     * @return True if connected, otherwise false.
     */
    boolean connected();

    /**
     * Returns the async publisher implementation.
     *
     * @return The async publisher.
     */
    AsyncPublisher async();

    /**
     * Returns an unmodifiable list of the cluster servers.
     *
     * @return The unmodifiable list of the cluster servers.
     */
    List<ClusterServer> clusterServers();
}
