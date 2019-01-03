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

package de.progme.thor.shared.net;

/**
 * Created by Marvin Erkes on 14.06.2017.
 */
public enum OpCode {

    OP_UNKNOWN(-1),
    OP_REGISTER_CHANNEL(0),
    OP_UNREGISTER_CHANNEL(1),
    OP_BROADCAST(2),
    OP_SUBSCRIBER_SET_NAME(3),
    OP_CACHE_ADD(4),
    OP_CACHE_GET(5),
    OP_CACHE_REMOVE(6),
    OP_CACHE_SET_EXPIRE(7),
    OP_CACHE_GET_EXPIRE(8),
    OP_CLUSTER_INFO_SET(9),
    OP_KEEP_ALIVE(10),
    OP_CACHE_HAS(11);

    private int code;

    OpCode(int code) {

        this.code = code;
    }

    public int getCode() {

        return code;
    }

    public static OpCode of(Integer code) {

        if (code == null) {
            return OP_UNKNOWN;
        }

        for (OpCode opCode : values()) {
            if (opCode.code == code) {
                return opCode;
            }
        }

        return OP_UNKNOWN;
    }
}
