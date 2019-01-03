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

package de.progme.thor.client.pub.impl;

import com.google.gson.Gson;
import de.progme.thor.client.pub.AsyncPublisher;
import de.progme.thor.client.pub.Publisher;
import de.progme.thor.shared.config.ClusterServer;
import de.progme.thor.shared.net.OpCode;
import de.progme.thor.shared.nio.NioSocketClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class PublisherImpl extends NioSocketClient implements Publisher {

    private Gson gson = new Gson();

    private ExecutorService executorService;

    private AsyncPublisher asyncPublisher;

    public PublisherImpl(String host, int port) {

        this(Collections.singletonList(new ClusterServer(host, port)));
    }

    public PublisherImpl(List<ClusterServer> clusterServers) {

        super(clusterServers);

        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("AsyncPublisher Thread");

            return thread;
        });
        this.asyncPublisher = new AsyncPublisherImpl(executorService, this);
    }

    @Override
    public void clientConnected() {

        // Not needed
    }

    @Override
    public void clientReconnected() {

        // Not needed
    }

    @Override
    public void received(JSONObject jsonObject) {

        // Not needed
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
    public void publish(String channel, JSONObject jsonObject) {

        publish(channel, null, jsonObject);
    }

    @Override
    public void publishAll(String channel, JSONObject... jsonObjects) {

        for (JSONObject jsonObject : jsonObjects) {
            publish(channel, jsonObject);
        }
    }

    @Override
    public void publish(String channel, String subscriberName, JSONObject jsonObject) {

        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("channel cannot be null or empty");
        }

        if (jsonObject == null || jsonObject.length() == 0) {
            throw new IllegalArgumentException("jsonObject cannot be null or empty");
        }

        // Set the op code, channel
        jsonObject.put("op", OpCode.OP_BROADCAST.getCode());
        jsonObject.put("ch", channel);
        // Set the subscriber name if it is not null
        if (subscriberName != null) {
            jsonObject.put("su", subscriberName);
        }

        write(jsonObject);
    }

    @Override
    public void publishAll(String channel, String subscriberName, JSONObject... jsonObjects) {

        for (JSONObject jsonObject : jsonObjects) {
            publish(channel, subscriberName, jsonObject);
        }
    }

    @Override
    public void publish(String channel, String json) {

        publish(channel, null, json);
    }

    @Override
    public void publish(String channel, String subscriberName, String json) {

        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("json cannot be null or empty");
        }

        try {
            // Publish it as JSONObject that we can add the op code and channel
            publish(channel, subscriberName, new JSONObject(json));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publish(String channel, Object object) {

        publish(channel, null, object);
    }

    @Override
    public void publishAll(String channel, Object... objects) {

        for (Object object : objects) {
            publish(channel, null, object);
        }
    }

    @Override
    public void publish(String channel, String subscriberName, Object object) {

        if (object == null) {
            throw new IllegalArgumentException("object cannot be null");
        }

        // Publish the serialized object as json string
        publish(channel, subscriberName, gson.toJson(object));
    }

    @Override
    public void publishAll(String channel, String subscriberName, Object... objects) {

        for (Object object : objects) {
            publish(channel, subscriberName, object);
        }
    }

    @Override
    public boolean connected() {

        return super.isConnected();
    }

    @Override
    public AsyncPublisher async() {

        return asyncPublisher;
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return Collections.unmodifiableList(super.clusterServers());
    }
}
