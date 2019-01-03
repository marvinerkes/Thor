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

package de.progme.thor.server;

import de.progme.thor.server.cache.ThorCache;
import de.progme.thor.server.config.Config;
import de.progme.thor.server.network.Connection;
import de.progme.thor.server.network.initialize.ServerChannelInitializer;
import de.progme.thor.server.network.initialize.ClusterPublisherChannelInitializer;
import de.progme.thor.shared.config.ClusterServer;
import de.progme.thor.shared.net.OpCode;
import de.progme.thor.shared.pipeline.PipelineUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class ThorServer {

    private static final Logger LOGGER = Thor.getLogger();

    private String host;

    private int port;

    private int backlog;

    private Map<String, List<Connection>> channelSessions = new ConcurrentHashMap<>();

    private int workerThreads;

    private List<ClusterPublisher> clusterPublisher = new ArrayList<>();

    private ThorCache cache;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Channel serverChannel;

    public ThorServer(String host, int port, int backlog, boolean debug, int workerThreads, List<ClusterServer> cluster, int cleanupInterval, int snapshotInterval) {

        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.workerThreads = workerThreads;
        this.cache = new ThorCache(cleanupInterval, snapshotInterval);

        LOGGER.setLevel((debug) ? Level.FINE : Level.INFO);

        start();

        // Check if there are cluster servers to avoid unnecessary logic execution
        if (cluster.size() > 0) {
            new Thread(() -> {
                while (cluster.size() > 0) {
                    LOGGER.info("Trying to connecting to all cluster servers");

                    Iterator<ClusterServer> clusterServerIterator = cluster.iterator();
                    while (clusterServerIterator.hasNext()) {
                        ClusterServer clusterServer = clusterServerIterator.next();

                        // Remove the own endpoint of this instance (does not work if it is bound to 0.0.0.0)
                        if (clusterServer.port() == port && clusterServer.host().equals(host)) {
                            clusterServerIterator.remove();
                            continue;
                        }

                        try {
                            ClusterPublisher cb = new ClusterPublisher(clusterServer.host(), clusterServer.port());

                            if (cb.connect()) {
                                clusterPublisher.add(cb);

                                clusterServerIterator.remove();

                                cb.write(new JSONObject()
                                        .put("op", OpCode.OP_CLUSTER_INFO_SET.getCode())
                                        .put("host", host)
                                        .put("port", port));

                                LOGGER.log(Level.INFO, "Connected to cluster server {0}:{1}", new Object[]{clusterServer.host(), String.valueOf(clusterServer.port())});
                            } else {
                                LOGGER.log(Level.SEVERE, "Could not connect to cluster server {0}:{1}", new Object[]{clusterServer.host(), String.valueOf(clusterServer.port())});
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Could not connect to cluster server {0}:{1}", new Object[]{clusterServer.host(), String.valueOf(clusterServer.port())});
                        }
                    }

                    if (cluster.size() == 0) {
                        break;
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LOGGER.info("Cluster servers are connected successfully!");
            }).start();
        }
    }

    public ThorServer(Config config) {

        this(config.host(), config.port(), config.backlog(), config.debug(), config.workerThreads(), config.cluster(), config.cleanupInterval(), config.snapshotInterval());
    }

    private void start() {

        if (PipelineUtils.isEpoll()) {
            LOGGER.info("Using high performance epoll event notification mechanism");
        } else {
            LOGGER.info("Using normal select/poll event notification mechanism");
        }

        bossGroup = PipelineUtils.newEventLoopGroup(1);
        workerGroup = PipelineUtils.newEventLoopGroup(workerThreads);

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverChannel = serverBootstrap
                    .group(bossGroup, workerGroup)
                    .channel(PipelineUtils.getServerChannel())
                    .childHandler(new ServerChannelInitializer(this))
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_BACKLOG, backlog)
                    .bind(new InetSocketAddress(host, port)).sync().channel();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOGGER.log(Level.INFO, "Thor server started on {0}:{1}", new Object[]{host, String.valueOf(port)});
    }

    public void stop() {

        LOGGER.info("Server will stop");

        serverChannel.close();

        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        // Close the cache
        cache.close();

        LOGGER.info("Server stopped!");
    }

    public void subscribeChannel(String channel, Connection connection) {

        if (channelSessions.containsKey(channel)) {
            channelSessions.get(channel).add(connection);
        } else {
            channelSessions.put(channel, new CopyOnWriteArrayList<>(Collections.singletonList(connection)));
        }

        LOGGER.log(Level.FINE, "[{0}] Channel subscribed: {1}", new Object[]{connection.remoteAddress().toString(), channel});
    }

    public void unsubscribeChannel(String channel, Connection connection) {

        if (channelSessions.containsKey(channel)) {
            channelSessions.get(channel).remove(connection);

            LOGGER.log(Level.FINE, "[{0}] Channel unsubscribed: {1}", new Object[]{connection.remoteAddress().toString(), channel});
        }
    }

    public void removeClient(Connection connection) {

        for (String s : connection.channels()) {
            channelSessions.get(s).remove(connection);
        }

        if (!connection.channels().isEmpty()) {
            LOGGER.log(Level.FINE, "[{0}] Channels unsubscribed from {1}: {2}", new Object[]{connection.remoteAddress().toString(), connection.name(), String.join(", ", connection.channels())});
        }
    }

    public void broadcast(Connection con, String channel, JSONObject data) {

        if (channelSessions.containsKey(channel)) {
            channelSessions.get(channel).stream().forEach(connection -> connection.send(data));
        }

        // Broadcast it to the cluster if possible
        clusterPubSubBroadcast(con, channel, data);
    }

    public void broadcastTo(Connection con, String channel, JSONObject data, String subscriberName) {

        JSONObject clusterData = new JSONObject(data);

        if (channelSessions.containsKey(channel)) {
            // Remove the subscriber name to save bandwidth and remove the unneeded key
            data.remove("su");

            // Get the correct data to send
            //String broadcastData = data.toString();

            // Find the subscribers with that name and route it to these
            for (Connection filteredConnection : channelSessions.get(channel).stream().filter(connection -> connection.name().equals(subscriberName)).collect(Collectors.toList())) {
                filteredConnection.send(data);
            }
        }

        // Broadcast it to the cluster if possible
        clusterPubSubBroadcast(con, channel, clusterData);
    }

    private void clusterPubSubBroadcast(Connection connection, String channel, JSONObject data) {

        if (clusterPublisher.size() > 0) {
            JSONObject clusterMessage = new JSONObject(data)
                    .put("op", OpCode.OP_BROADCAST.getCode())
                    .put("ch", channel);

            clusterBroadcast(connection, clusterMessage);
        }
    }

    public void clusterBroadcast(Connection connection, JSONObject data) {

        // Publish it to all clusters but exclude the server which has sent it
        // if it comes from another Thor server but also publish it
        // if a normal publisher client has sent it
        clusterPublisher.stream().filter(cl -> ((connection == null || connection.host() == null) || (connection.host() != null && connection.port() != cl.port && !connection.host().equals(cl.host))))
                .forEach(cl -> cl.write(data));
    }

    public ThorCache cache() {

        return cache;
    }

    public static class ClusterPublisher extends SimpleChannelInboundHandler<JSONObject> {

        private String host;

        private int port;

        private boolean connected;

        private Channel channel;

        public ClusterPublisher(String host, int port) {

            this.host = host;
            this.port = port;
        }

        private boolean connect() {

            ChannelFuture channelFuture = new Bootstrap()
                    .group(PipelineUtils.newEventLoopGroup(1))
                    .channel(PipelineUtils.getChannel())
                    .handler(new ClusterPublisherChannelInitializer(this))
                    .option(ChannelOption.TCP_NODELAY, true)
                    .connect(host, port);

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
                countDownLatch.await(4, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return connected;
        }

        public void write(JSONObject jsonObject) {

            channel.writeAndFlush(jsonObject);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject) throws Exception {

        }
    }
}
