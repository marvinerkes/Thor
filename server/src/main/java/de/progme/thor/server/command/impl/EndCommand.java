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

/**
 * Created by Marvin Erkes on 05.05.2017.
 */
public class EndCommand extends Command {

    public EndCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        Thor.getInstance().stop();

        return true;
    }
}
