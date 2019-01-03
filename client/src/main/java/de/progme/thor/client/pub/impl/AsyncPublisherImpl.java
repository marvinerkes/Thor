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

import de.progme.thor.client.pub.AsyncPublisher;
import de.progme.thor.client.pub.Publisher;
import de.progme.thor.shared.config.ClusterServer;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by Marvin Erkes on 07.05.2017.
 */
public class AsyncPublisherImpl implements AsyncPublisher {

    private ExecutorService executorService;

    private Publisher publisher;

    public AsyncPublisherImpl(ExecutorService executorService, Publisher publisher) {

        this.executorService = executorService;
        this.publisher = publisher;
    }

    @Override
    public void disconnect(boolean force) {

        publisher.disconnect(force);
    }

    @Override
    public void disconnect() {

        disconnect(true);
    }

    @Override
    public void publish(String channel, JSONObject jsonObject) {

        executorService.execute(() -> publisher.publish(channel, jsonObject));
    }

    @Override
    public void publishAll(String channel, JSONObject... jsonObjects) {

        executorService.execute(() -> publisher.publish(channel, jsonObjects));
    }

    @Override
    public void publish(String channel, String subscriberName, JSONObject jsonObject) {

        executorService.execute(() -> publisher.publish(channel, subscriberName, jsonObject));
    }

    @Override
    public void publishAll(String channel, String subscriberName, JSONObject... jsonObjects) {

        executorService.execute(() -> publisher.publishAll(channel, subscriberName, jsonObjects));
    }

    @Override
    public void publish(String channel, String json) {

        executorService.execute(() -> publisher.publish(channel, json));
    }

    @Override
    public void publish(String channel, String json, String subscriberName) {

        executorService.execute(() -> publisher.publish(channel, json, subscriberName));
    }

    @Override
    public void publish(String channel, Object object) {

        executorService.execute(() -> publisher.publish(channel, object));
    }

    @Override
    public void publishAll(String channel, Object... objects) {

        executorService.execute(() -> publisher.publishAll(channel, objects));
    }

    @Override
    public void publish(String channel, String subscriberName, Object object) {

        executorService.execute(() -> publisher.publish(channel, subscriberName, object));
    }

    @Override
    public void publishAll(String channel, String subscriberName, Object... objects) {

        executorService.execute(() -> publisher.publishAll(channel, subscriberName, objects));
    }

    @Override
    public boolean connected() {

        return publisher.connected();
    }

    @Override
    public AsyncPublisher async() {

        return this;
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return publisher.clusterServers();
    }

    @Override
    public ExecutorService executorService() {

        return executorService;
    }
}
