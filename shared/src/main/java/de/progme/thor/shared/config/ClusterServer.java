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

package de.progme.thor.shared.config;

public class ClusterServer {

    private String host;

    private int port;

    public ClusterServer(String host, int port) {

        this.host = host;
        this.port = port;
    }

    public String host() {

        return host;
    }

    public int port() {

        return port;
    }

    @Override
    public String toString() {

        return "ClusterServer{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}