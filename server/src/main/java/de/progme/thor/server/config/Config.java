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

package de.progme.thor.server.config;

import de.progme.thor.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class Config {

    private String host;

    private int port;

    private int backlog = 50;

    private boolean debug = true;

    private int workerThreads = Runtime.getRuntime().availableProcessors();

    private List<ClusterServer> cluster;

    private int cleanupInterval = -1;

    private int snapshotInterval = -1;

    public Config(String host, int port, int backlog, boolean debug, int workerThreads, List<ClusterServer> cluster, int cleanupInterval, int snapshotInterval) {

        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.debug = debug;
        this.workerThreads = workerThreads;
        this.cluster = cluster;
        this.cleanupInterval = cleanupInterval;
        this.snapshotInterval = snapshotInterval;
    }

    public String host() {

        return host;
    }

    public int port() {

        return port;
    }

    public int backlog() {

        return backlog;
    }

    public boolean debug() {

        return debug;
    }

    public int workerThreads() {

        return workerThreads;
    }

    public List<ClusterServer> cluster() {

        return cluster;
    }

    public int cleanupInterval() {

        return cleanupInterval;
    }

    public int snapshotInterval() {

        return snapshotInterval;
    }

    @Override
    public String toString() {

        return "Config{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", backlog=" + backlog +
                ", debug=" + debug +
                ", workerThreads=" + workerThreads +
                ", cluster=" + cluster +
                ", cleanupInterval=" + cleanupInterval +
                ", snapshotInterval=" + snapshotInterval +
                '}';
    }
}
