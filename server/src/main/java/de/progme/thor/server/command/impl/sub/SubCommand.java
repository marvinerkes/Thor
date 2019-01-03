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

package de.progme.thor.server.command.impl.sub;

import de.progme.thor.client.sub.Subscriber;
import de.progme.thor.client.sub.SubscriberFactory;
import de.progme.thor.server.Thor;
import de.progme.thor.server.command.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marvin Erkes on 05.05.2017.
 */
public class SubCommand extends Command {

    public static List<Subscriber> SUBSCRIBERS = new ArrayList<>();

    public SubCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length == 1) {
            Subscriber subscriber = SubscriberFactory.create("localhost", Thor.getConfig().port());
            subscriber.subscribe(args[0], SubCommandChannelHandler.class);

            SUBSCRIBERS.add(subscriber);

            Thor.getLogger().info("Subscribed channel " + args[0]);
        } else {
            Thor.getLogger().info("Usage: sub <Channel>");
        }

        return true;
    }
}
