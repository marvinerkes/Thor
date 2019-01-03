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

package de.progme.thor.client.sub.impl;

import com.google.gson.Gson;
import de.progme.thor.client.sub.Subscriber;
import de.progme.thor.client.sub.impl.handler.ChannelHandler;
import de.progme.thor.client.sub.impl.handler.ClassType;
import de.progme.thor.client.sub.impl.handler.HandlerInfo;
import de.progme.thor.client.sub.impl.handler.MultiHandlerInfo;
import de.progme.thor.client.sub.impl.handler.annotation.Channel;
import de.progme.thor.client.sub.impl.handler.annotation.Key;
import de.progme.thor.client.sub.impl.handler.annotation.Value;
import de.progme.thor.client.util.NameGeneratorUtil;
import de.progme.thor.shared.config.ClusterServer;
import de.progme.thor.shared.net.OpCode;
import de.progme.thor.shared.nio.NioSocketClient;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class SubscriberImpl extends NioSocketClient implements Subscriber {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private Map<String, HandlerInfo> handlers = new HashMap<>();

    private Map<String, MultiHandlerInfo> multiHandlers = new HashMap<>();

    private Gson gson = new Gson();

    public SubscriberImpl(String host, int port) {

        this(host, port, NameGeneratorUtil.generateName("subscriber", ID_COUNTER.getAndIncrement()));
    }

    public SubscriberImpl(String host, int port, String name) {

        this(Collections.singletonList(new ClusterServer(host, port)), name);
    }

    public SubscriberImpl(List<ClusterServer> clusterServers) {

        this(clusterServers, NameGeneratorUtil.generateName("subscriber", ID_COUNTER.getAndIncrement()));
    }

    public SubscriberImpl(List<ClusterServer> clusterServers, String name) {

        super(clusterServers, name);
    }

    @Override
    public void clientConnected() {

        // Register with our name
        write(new JSONObject()
                .put("op", OpCode.OP_SUBSCRIBER_SET_NAME.getCode())
                .put("su", name));
    }

    @Override
    public void clientReconnected() {

        if (handlers != null) {
            // Resubscribe the normal handlers
            for (Map.Entry<String, HandlerInfo> handlerInfoEntry : handlers.entrySet()) {
                subscribe(handlerInfoEntry.getValue().messageHandler().getClass());
            }
        }

        if (multiHandlers != null) {
            // Resubscribe the multi handlers
            for (Map.Entry<String, MultiHandlerInfo> handlerInfoEntry : multiHandlers.entrySet()) {
                subscribeMulti(handlerInfoEntry.getValue().object().getClass());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void received(JSONObject jsonObject) {

        String channel = ((String) jsonObject.remove("ch"));

        if (channel == null || channel.isEmpty()) {
            return;
        }

        HandlerInfo handlerInfo = handlers.get(channel);

        if (handlerInfo != null) {
            if (handlerInfo.classType() == ClassType.JSON) {
                handlerInfo.messageHandler().onMessage(channel, jsonObject);
            } else {
                handlerInfo.messageHandler().onMessage(channel, gson.fromJson(jsonObject.toString(), handlerInfo.clazz()));
            }
        } else {
            MultiHandlerInfo multiHandlerInfo = multiHandlers.get(channel);

            if (multiHandlerInfo != null) {
                //noinspection Convert2streamapi
                for (MultiHandlerInfo.Entry entry : multiHandlerInfo.entries()) {
                    if (!jsonObject.isNull(entry.key().value())) {
                        if (jsonObject.get(entry.key().value()).equals(entry.value().value())) {
                            // Remove matched key value pair
                            jsonObject.remove(entry.key().value());

                            if (entry.classType() == ClassType.JSON) {
                                try {
                                    // Invoke the matching method
                                    entry.method().invoke(multiHandlerInfo.object(), jsonObject);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    // Deserialize with gson
                                    entry.method().invoke(multiHandlerInfo.object(), gson.fromJson(jsonObject.toString(), entry.paramClass()));
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String getChannelFromAnnotation(Class<?> clazz) {

        if (!clazz.isAnnotationPresent(Channel.class)) {
            throw new IllegalArgumentException("the handler class " + clazz.getSimpleName() + " has no 'Channel' annotation");
        }

        String channel = clazz.getAnnotation(Channel.class).value();

        if (channel.isEmpty()) {
            throw new IllegalStateException("value of the 'Channel' annotation of class " + clazz.getSimpleName() + " is empty");
        }

        return channel;
    }

    @Override
    public void disconnect(boolean force) {

        close(force);
    }

    @Override
    public void disconnect() {

        disconnect(true);
    }

    @Override
    public boolean hasSubscription(String channel) {

        return handlers.containsKey(channel) || multiHandlers.containsKey(channel);
    }

    @Deprecated
    @Override
    public void subscribe(String channel, Class<? extends ChannelHandler> handler) {

        try {
            //noinspection unchecked
            handlers.put(channel, new HandlerInfo(handler.newInstance()));

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_REGISTER_CHANNEL.getCode())
                    .put("ch", channel);

            write(jsonObject, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void subscribe(Class<? extends ChannelHandler> handler) {

        // Get channel and check the class for annotation etc.
        String channel = getChannelFromAnnotation(handler);

        //noinspection deprecation
        subscribe(channel, handler);
    }

    @Override
    public void subscribeMulti(Class<?> handler) {

        // Get channel and check the class for annotation etc.
        String channel = getChannelFromAnnotation(handler);

        try {
            List<MultiHandlerInfo.Entry> entries = new ArrayList<>();

            Object object = handler.newInstance();
            for (Method method : object.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() == 1) {
                    if (method.isAnnotationPresent(Key.class) && method.isAnnotationPresent(Value.class)) {
                        entries.add(new MultiHandlerInfo.Entry(method.getAnnotation(Key.class), method.getAnnotation(Value.class), method.getParameterTypes()[0], (method.getParameterTypes()[0].getSimpleName().equals("JSONObject")) ? ClassType.JSON : ClassType.GSON, method));
                    }
                }
            }

            multiHandlers.put(channel, new MultiHandlerInfo(entries, object));

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_REGISTER_CHANNEL.getCode())
                    .put("ch", channel);

            write(jsonObject, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsubscribe(String channel) {

        // Only send unsubscribe if the channel is subscribed
        if (handlers.containsKey(channel) || multiHandlers.containsKey(channel)) {
            handlers.remove(channel);
            multiHandlers.remove(channel);

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_UNREGISTER_CHANNEL.getCode())
                    .put("ch", channel);

            write(jsonObject);
        }
    }

    @Override
    public boolean connected() {

        return isConnected();
    }

    @Override
    public String name() {

        return name;
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return Collections.unmodifiableList(super.clusterServers());
    }
}
