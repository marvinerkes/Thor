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

package de.progme.thor.server.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Marvin Erkes on 17.07.2015.
 */
public class CommandManager {

    private Map<String, Command> commands = new HashMap<>();

    public Command findCommand(String name) {

        return (commands.containsKey(name)) ? commands.get(name) : commands.values().stream().filter((Command c) -> c.isValidAlias(name)).findFirst().orElse(null);
    }

    public void addCommand(Command command) {

        commands.put(command.getName(), command);
    }

    public List<Command> getCommands() {

        return new ArrayList<>(commands.values());
    }
}
