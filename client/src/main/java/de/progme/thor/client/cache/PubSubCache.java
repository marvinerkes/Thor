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

package de.progme.thor.client.cache;

import de.progme.thor.shared.config.ClusterServer;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Created by Marvin Erkes on 13.06.2017.
 */
public interface PubSubCache {

    /**
     * Disconnects the pub sub cache.
     *
     * @param force If true, the publisher will not try to reconnect
     */
    void disconnect(boolean force);

    /**
     * Disconnects the pub sub cache without trying to reconnect.
     *
     * Same as invoking disconnect(true).
     */
    void disconnect();

    /**
     * Puts a key with its json object and the expire.
     *
     * @param key The key.
     * @param value The value.
     * @param expire The expire in seconds.
     */
    void put(String key, JSONObject value, int expire);

    /**
     * Puts a key with its json object.
     * This key will not expire!
     *
     * @param key The key.
     * @param value The value.
     */
    void put(String key, JSONObject value);

    /**
     * Puts a key with its object value and the expire.
     *
     * @param key The key.
     * @param value The value.
     * @param expire The expire in seconds.
     */
    void putObject(String key, Object value, int expire);

    /**
     * Puts a key with its object value.
     * This key will not expire!
     *
     * @param key The key.
     * @param value The value.
     */
    void putObject(String key, Object value);

    /**
     * Gets the value associated with that key and calls the consumer with the value.
     *
     * @param key The key.
     * @param consumer The consumer.
     */
    void get(String key, Consumer<JSONObject> consumer);

    /**
     * Returns if the given key is in the cache.
     *
     * @param key The key.
     */
    Future<Boolean> has(String key);

    /**
     * Gets the value as a class object and calls the consumer with this class.
     *
     * @param key The key.
     * @param consumer The consumer.
     */
    <T> void getClass(String key, Consumer<T> consumer, Class<T> clazz);

    /**
     * Removes a key and its value from the pub sub cache.
     *
     * @param key The key to remove.
     */
    void remove(String key);

    /**
     * Sets the expire time in seconds for the given key.
     *
     * @param key the key.
     * @param secondsToLive expire time in seconds.
     */
    void expire(String key, int secondsToLive);

    /**
     * Gets the time how long the key lives until it's expired.
     *
     * @param key The key.
     * @param consumer The consumer.
     */
    void expire(String key, Consumer<Integer> consumer);

    /**
     * Returns the async pub sub cache implementation.
     *
     * @return The async pub sub cache.
     */
    AsyncPubSubCache async();

    /**
     * Returns an unmodifiable list of the cluster servers.
     *
     * @return The unmodifiable list of the cluster servers.
     */
    List<ClusterServer> clusterServers();
}
