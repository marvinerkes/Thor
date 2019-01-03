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

package de.progme.thor.server.network;

import de.progme.thor.server.Thor;
import de.progme.thor.server.ThorServer;
import de.progme.thor.shared.net.OpCode;
import de.progme.thor.shared.pipeline.ChannelUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class Connection extends SimpleChannelInboundHandler<JSONObject> {

    private static final Logger LOGGER = Thor.getLogger();

    private ThorServer server;

    private List<String> channels = new ArrayList<>();

    private SocketAddress remoteAddress;

    private Channel channel;

    private String host;

    private int port;

    private String name;

    public Connection(ThorServer server, Channel channel) {

        this.server = server;
        this.channel = channel;
        this.remoteAddress = channel.remoteAddress();
    }

    public void send(JSONObject jsonObject) {

        channel.writeAndFlush(jsonObject);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        server.removeClient(this);

        LOGGER.log(Level.FINE, "[{0}] Connection closed", remoteAddress.toString());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        LOGGER.log(Level.FINE, "[{0}] New connection", remoteAddress.toString());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JSONObject jsonObject) throws Exception {

        if (jsonObject.isNull("op")) {
            return;
        }

        int op = ((Integer) jsonObject.remove("op"));

        OpCode opCode = OpCode.of(op);

        switch (opCode) {
            case OP_BROADCAST:
                if (!jsonObject.has("su")) {
                    // Broadcast it to all subscriber
                    server.broadcast(this, jsonObject.getString("ch"), jsonObject);
                } else {
                    // Broadcast to specific subscriber
                    server.broadcastTo(this, jsonObject.getString("ch"), jsonObject, jsonObject.getString("su"));
                }
                break;
            case OP_CACHE_GET:
                String getKey = jsonObject.getString("key");
                int getCallbackId = jsonObject.getInt("id");

                Object getValue = server.cache().get(getKey);

                JSONObject getResponse = new JSONObject()
                        .put("op", 5)
                        .put("id", getCallbackId)
                        .put("value", getValue);

                send(getResponse);

                LOGGER.log(Level.FINE, "[{0}] Got cache entry {1}={2} and a callback id of {3}", new Object[] {remoteAddress.toString(), getKey, getValue, getCallbackId});
                break;
            case OP_CACHE_ADD:
                String key = jsonObject.getString("key");
                Object value = jsonObject.get("value");
                int expire = jsonObject.getInt("expire");

                server.cache().put(key, value, expire);

                server.clusterBroadcast(this, jsonObject.put("op", OpCode.OP_CACHE_ADD.getCode()));

                LOGGER.log(Level.FINE, "[{0}] Added cache entry {1}={2} with an expire of {3}", new Object[] {remoteAddress.toString(), key, value, expire});
                break;
            case OP_CACHE_REMOVE:
                String removeKey = jsonObject.getString("key");

                server.cache().remove(removeKey);

                server.clusterBroadcast(this, jsonObject.put("op", OpCode.OP_CACHE_REMOVE.getCode()));

                LOGGER.log(Level.FINE, "[{0}] Removed cache entry with key {1}", new Object[] {remoteAddress.toString(), removeKey});
                break;
            case OP_CACHE_HAS:
                boolean has = server.cache().has(jsonObject.getString("key"));

                JSONObject hasResponse = new JSONObject()
                        .put("op", OpCode.OP_CACHE_HAS.getCode())
                        .put("id", jsonObject.getInt("id"))
                        .put("has", has);

                send(hasResponse);
                break;
            case OP_CACHE_SET_EXPIRE:
                String expireKey = jsonObject.getString("key");
                int expireSeconds = jsonObject.getInt("expire");

                server.cache().expire(expireKey, expireSeconds);

                server.clusterBroadcast(this, jsonObject.put("op", OpCode.OP_CACHE_SET_EXPIRE.getCode()));

                LOGGER.log(Level.FINE, "[{0}] Set expire seconds for key {1} to {2} seconds", new Object[] {remoteAddress.toString(), expireKey, expireSeconds});
                break;
            case OP_CACHE_GET_EXPIRE:
                String expireGetKey = jsonObject.getString("key");
                int expireGetCallbackId = jsonObject.getInt("id");

                int expireGetValue = ((int) server.cache().expire(expireGetKey));

                JSONObject expireGetResponse = new JSONObject()
                        .put("op", 5)
                        .put("id", expireGetCallbackId)
                        .put("value", expireGetValue);

                send(expireGetResponse);

                LOGGER.log(Level.FINE, "[{0}] Got expire in time for key {1} which will expire in {2} seconds", new Object[] {remoteAddress.toString(), expireGetKey, expireGetValue});
                break;
            case OP_REGISTER_CHANNEL:
                String channelToRegister = jsonObject.getString("ch");

                server.subscribeChannel(channelToRegister, this);
                channels.add(channelToRegister);
                break;
            case OP_UNREGISTER_CHANNEL:
                String channelToRemove = jsonObject.getString("ch");

                server.unsubscribeChannel(channelToRemove, this);
                channels.remove(channelToRemove);
                break;
            case OP_SUBSCRIBER_SET_NAME:
                name = jsonObject.getString("su");

                LOGGER.log(Level.FINE, "[{0}] Subscriber name set to: {1}", new Object[]{remoteAddress.toString(), name});
                break;
            case OP_CLUSTER_INFO_SET:
                host = jsonObject.getString("host");
                port = jsonObject.getInt("port");

                LOGGER.log(Level.FINE, "[{0}] Cluster info set to: {1}:{2}", new Object[]{remoteAddress.toString(), host, String.valueOf(port)});
                break;
            case OP_KEEP_ALIVE:
                // Ignore for now
                //LOGGER.log(Level.FINE, "[{0}] Keep alive time: {1}", new Object[]{remoteAddress.toString(), System.currentTimeMillis()});
                break;
            case OP_UNKNOWN:
                LOGGER.log(Level.WARNING, "[{0}] Unknown OP code received: {0}", new Object[]{remoteAddress.toString(), op});
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        ChannelUtil.closeOnFlush(channel);

        if (!(cause instanceof IOException)) {
            cause.printStackTrace();

            LOGGER.log(Level.SEVERE, "Error: " + cause.toString());
        }
    }

    public List<String> channels() {

        return channels;
    }

    public SocketAddress remoteAddress() {

        return remoteAddress;
    }

    public String host() {

        return host;
    }

    public int port() {

        return port;
    }

    public String name() {

        return name;
    }

    public boolean connected() {

        return channel.isActive();
    }
}
