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

package de.progme.thor.server.command.impl;

import de.progme.thor.server.Thor;
import de.progme.thor.server.command.Command;
import de.progme.thor.shared.net.OpCode;
import org.json.JSONObject;

/**
 * Created by Marvin Erkes on 22.06.2017.
 */
public class SetCommand extends Command {

    public SetCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length < 2) {
            Thor.getLogger().info("Usage: set <Key> [Expire] <Value>");
            return false;
        }

        int expire = -1;
        String value = args[1];

        if (args.length >= 3) {
            try {
                expire = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                Thor.getLogger().info("'" + args[1] + "' is not a number");
                return false;
            }

            String[] values = new String[args.length - 2];
            System.arraycopy(args, 2, values, 0, args.length - 2);

            value = String.join(" ", values);
        }

        Thor.getServer().cache().put(args[0], value);

        Thor.getServer().clusterBroadcast(null, new JSONObject()
                .put("op", OpCode.OP_CACHE_ADD.getCode())
                .put("key", args[0])
                .put("value", value)
                .put("expire", expire));

        return true;
    }
}
