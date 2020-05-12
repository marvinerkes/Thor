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

package de.progme.thor.shared.nio;

import de.progme.thor.shared.config.ClusterServer;
import de.progme.thor.shared.net.ConnectException;
import de.progme.thor.shared.pipeline.ChannelUtil;
import de.progme.thor.shared.pipeline.PipelineUtils;
import de.progme.thor.shared.pipeline.initialize.ClientChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Marvin Erkes on 16.06.2017.
 */
@ChannelHandler.Sharable
public abstract class NioSocketClient extends SimpleChannelInboundHandler<JSONObject> {

    private static final int CONNECT_TIMEOUT = 2000;

    private List<ClusterServer> clusterServers = new ArrayList<>();

    private EventLoopGroup eventLoopGroup;

    private Channel channel;

    private boolean connected;

    private AtomicBoolean reconnecting = new AtomicBoolean(false);

    private Queue<JSONObject> sendQueue = new ConcurrentLinkedQueue<>();

    protected String name;

    private String host;

    private int port;

    public NioSocketClient(List<ClusterServer> clusterServers, String name) {

        // Randomize the list to give a chance for a better use of the cluster
        Collections.shuffle(clusterServers);

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }

        this.clusterServers = clusterServers;
        this.name = name;

        ClusterServer first = clusterServers.get(0);

        this.host = first.host();
        this.port = first.port();

        if (!connect(host, port)) {
            throw new ConnectException("cannot initially connect to " + first.host() + ":" + first.port());
        }
    }

    public NioSocketClient(List<ClusterServer> clusterServers) {

        this(clusterServers, "server");
    }

    public abstract void clientConnected();

    public abstract void clientReconnected();

    public abstract void received(JSONObject jsonObject);

    public boolean connect(String host, int port) {

        eventLoopGroup = PipelineUtils.newEventLoopGroup(1);
        ChannelFuture channelFuture = new Bootstrap()
                .group(eventLoopGroup)
                .channel(PipelineUtils.getChannel())
                .handler(new ClientChannelInitializer(this))
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT)
                .connect(host, port);

        channelFuture.awaitUninterruptibly();

        channel = channelFuture.channel();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {

                connected = channelFuture.isSuccess();

                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return connected;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        clientConnected();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        reconnect();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        ChannelUtil.closeOnFlush(channel);

        if (!(cause instanceof IOException)) {
            cause.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JSONObject jsonObject) throws Exception {

        received(jsonObject);
    }

    private void addToQueue(JSONObject jsonObject) {

        // Only queue up to 100 messages
        if (sendQueue.size() < 100) {
            sendQueue.offer(jsonObject);
        }
    }

    private void reconnect() {

        if (!reconnecting.get()) {
            reconnecting.set(true);

            connected = false;

            channel.eventLoop().schedule(() -> {

                if (!connect(host, port)) {
                    reconnecting.set(false);

                    reconnect();
                } else {
                    reconnecting.set(false);

                    clientReconnected();

                    try {
                        // Give the subscriber a chance to connect first
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //Resend the queued messages if available
                    while (sendQueue.size() > 0) {
                        JSONObject jsonObject = sendQueue.poll();
                        if (jsonObject != null) {
                            write(jsonObject);
                        }
                    }
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    public void close(boolean force) {

        if (connected) {
            connected = false;

            channel.close();
            if (force) {
                try {
                    eventLoopGroup.shutdownGracefully().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (!force) {
                reconnect();
            }
        }
    }

    public void write(JSONObject jsonObject, boolean queueEnabled) {

        if (!channel.isActive() && queueEnabled) {
            addToQueue(jsonObject);
            return;
        }

        channel.writeAndFlush(jsonObject);
    }

    public void write(JSONObject jsonObject) {

        write(jsonObject, true);
    }

    public List<ClusterServer> clusterServers() {

        return clusterServers;
    }

    public boolean isConnected() {

        return connected;
    }
}
