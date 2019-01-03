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

package de.progme.thor.client.cache.impl;

import de.progme.thor.client.cache.AsyncPubSubCache;
import de.progme.thor.client.cache.PubSubCache;
import de.progme.thor.shared.config.ClusterServer;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Created by Marvin Erkes on 15.06.2017.
 */
public class AsyncPubSubCacheImpl implements AsyncPubSubCache {

    private ExecutorService executorService;

    private PubSubCache pubSubCache;

    public AsyncPubSubCacheImpl(ExecutorService executorService, PubSubCache pubSubCache) {

        this.executorService = executorService;
        this.pubSubCache = pubSubCache;
    }

    @Override
    public ExecutorService executorService() {

        return executorService;
    }

    @Override
    public void disconnect(boolean force) {

        pubSubCache.disconnect(force);
    }

    @Override
    public void disconnect() {

        pubSubCache.disconnect();
    }

    @Override
    public void put(String key, JSONObject value, int expire) {

        executorService.execute(() -> pubSubCache.put(key, value, expire));
    }

    @Override
    public void put(String key, JSONObject value) {

        executorService.execute(() -> pubSubCache.put(key, value));
    }

    @Override
    public void putObject(String key, Object value, int expire) {

        executorService.execute(() -> pubSubCache.putObject(key, value, expire));
    }

    @Override
    public void putObject(String key, Object value) {

        executorService.execute(() -> pubSubCache.putObject(key, value));
    }

    @Override
    public void get(String key, Consumer<JSONObject> consumer) {

        executorService.execute(() -> pubSubCache.get(key, consumer));
    }

    @Override
    public Future<Boolean> has(String key) {

        return pubSubCache.has(key);
    }

    @Override
    public <T> void getClass(String key, Consumer<T> consumer, Class<T> clazz) {

        executorService.execute(() -> pubSubCache.getClass(key, consumer, clazz));
    }

    @Override
    public void remove(String key) {

        executorService.execute(() -> pubSubCache.remove(key));
    }

    @Override
    public void expire(String key, int secondsToLive) {

        executorService.execute(() -> pubSubCache.expire(key, secondsToLive));
    }

    @Override
    public void expire(String key, Consumer<Integer> consumer) {

        executorService.execute(() -> pubSubCache.expire(key, consumer));
    }

    @Override
    public AsyncPubSubCache async() {

        return this;
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return pubSubCache.clusterServers();
    }
}
