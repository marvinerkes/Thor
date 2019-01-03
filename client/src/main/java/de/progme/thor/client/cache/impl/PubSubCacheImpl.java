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

import com.google.gson.Gson;
import de.progme.thor.client.cache.AsyncPubSubCache;
import de.progme.thor.client.cache.Cacheable;
import de.progme.thor.client.cache.PubSubCache;
import de.progme.thor.shared.config.ClusterServer;
import de.progme.thor.shared.net.OpCode;
import de.progme.thor.shared.nio.NioSocketClient;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by Marvin Erkes on 13.06.2017.
 */
public class PubSubCacheImpl extends NioSocketClient implements PubSubCache {

    private static final AtomicInteger CALLBACK_COUNTER = new AtomicInteger(0);

    private Map<Integer, Consumer> callbacks = new ConcurrentHashMap<>();

    private Gson gson = new Gson();

    private ExecutorService executorService;

    private AsyncPubSubCache asyncPubSubCache;

    public PubSubCacheImpl(String host, int port) {

        this(Collections.singletonList(new ClusterServer(host, port)));
    }

    public PubSubCacheImpl(List<ClusterServer> clusterServers) {

        super(clusterServers);

        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("PubSubCache Thread");

            return thread;
        });
        this.asyncPubSubCache = new AsyncPubSubCacheImpl(executorService, this);
    }

    @Override
    public void clientConnected() {

        // Not needed
    }

    @Override
    public void clientReconnected() {

        // Not needed
    }

    @SuppressWarnings("unchecked")
    @Override
    public void received(JSONObject jsonObject) {

        Integer op = ((Integer) jsonObject.remove("op"));

        OpCode opCode = OpCode.of(op);

        switch (opCode) {
            case OP_CACHE_GET:
                int id = jsonObject.getInt("id");
                Object value = (jsonObject.has("value")) ? jsonObject.get("value") : null;

                try {
                    // Remove the consumer and accept it if it is not null
                    Consumer remove = callbacks.remove(id);
                    if (remove != null) {
                        remove.accept(value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case OP_CACHE_HAS:
                int hasId = jsonObject.getInt("id");

                try {
                    // Remove the consumer and accept it if it is not null
                    Consumer remove = callbacks.remove(hasId);
                    if (remove != null) {
                        remove.accept(new JSONObject().put("has", jsonObject.getBoolean("has")));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void disconnect(boolean force) {

        close(force);

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public void disconnect() {

        disconnect(true);
    }

    @Override
    public void put(String key, JSONObject value, int expire) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null or empty");
        }

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_ADD.getCode())
                .put("key", key)
                .put("value", value)
                .put("expire", expire);

        write(jsonObject);
    }

    @Override
    public void put(String key, JSONObject value) {

        put(key, value, -1);
    }

    @Override
    public void putObject(String key, Object value, int expire) {

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null or empty");
        }

        if (!(value instanceof Cacheable)) {
            throw new IllegalArgumentException("value must implement the 'Cacheable' class");
        }

        put(key, new JSONObject(gson.toJson(value)), expire);
    }

    @Override
    public void putObject(String key, Object value) {

        putObject(key, value, -1);
    }

    @Override
    public void expire(String key, int secondsToLive) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_SET_EXPIRE.getCode())
                .put("key", key)
                .put("expire", (secondsToLive > 0) ? secondsToLive : -1);

        write(jsonObject);
    }

    @Override
    public void expire(String key, Consumer<Integer> consumer) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (consumer == null) {
            throw new IllegalArgumentException("consumer cannot be null or empty");
        }

        int id = CALLBACK_COUNTER.getAndIncrement();

        callbacks.put(id, consumer);

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_GET_EXPIRE.getCode())
                .put("key", key)
                .put("id", id);

        write(jsonObject);
    }

    @Override
    public void get(String key, Consumer<JSONObject> consumer) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (consumer == null) {
            throw new IllegalArgumentException("consumer cannot be null or empty");
        }

        int id = CALLBACK_COUNTER.getAndIncrement();

        // TODO: 14.06.2016 Maybe improve
        callbacks.put(id, consumer);

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_GET.getCode())
                .put("key", key)
                .put("id", id);

        write(jsonObject);
    }

    @Override
    public Future<Boolean> has(String key) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        return executorService.submit(() -> {

            int id = CALLBACK_COUNTER.getAndIncrement();

            AtomicBoolean has = new AtomicBoolean(false);

            CountDownLatch countDownLatch = new CountDownLatch(1);

            callbacks.put(id, new Consumer<JSONObject>() {

                @Override
                public void accept(JSONObject jsonObject) {

                    has.set(jsonObject.getBoolean("has"));

                    countDownLatch.countDown();
                }
            });

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_CACHE_HAS.getCode())
                    .put("key", key)
                    .put("id", id);

            write(jsonObject);

            countDownLatch.await();

            return has.get();
        });
    }

    @Override
    public <T> void getClass(String key, Consumer<T> consumer, Class<T> clazz) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (consumer == null) {
            throw new IllegalArgumentException("consumer cannot be null or empty");
        }

        if (clazz == null) {
            throw new IllegalArgumentException("clazz cannot be null or empty");
        }

        int id = CALLBACK_COUNTER.getAndIncrement();

        // TODO: 14.06.2017 Maybe improve
        // Get the class as string and deserialize it
        callbacks.put(id, new Consumer<String>() {

            @Override
            public void accept(String s) {

                consumer.accept(gson.fromJson(s, clazz));
            }
        });

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_GET.getCode())
                .put("key", key)
                .put("id", id);

        write(jsonObject);
    }

    @Override
    public void remove(String key) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_REMOVE.getCode())
                .put("key", key);

        write(jsonObject);
    }

    @Override
    public AsyncPubSubCache async() {

        return asyncPubSubCache;
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return Collections.unmodifiableList(super.clusterServers());
    }
}
