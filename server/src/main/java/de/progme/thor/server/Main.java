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

import com.google.gson.Gson;
import de.progme.thor.server.config.Config;
import de.progme.thor.shared.config.ClusterServer;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Marvin Erkes on 05.05.2017.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        Config config = null;

        if (args.length > 0) {
            Options options = new Options();
            options.addOption("h", true, "Address to bind to");
            options.addOption("p", true, "Port to bind to");
            options.addOption("b", true, "The backlog");
            options.addOption("t", true, "Worker thread count");
            options.addOption("d", false, "If debug is enabled or not");
            options.addOption("c", true, "Add server as a cluster");
            options.addOption("ci", true, "Sets the cache check interval");
            options.addOption("si", true, "Sets the snapshot interval");

            CommandLineParser commandLineParser = new BasicParser();
            CommandLine commandLine = commandLineParser.parse(options, args);

            if (commandLine.hasOption("h") && commandLine.hasOption("p") && commandLine.hasOption("b") && commandLine.hasOption("t")) {

                List<ClusterServer> clusterServers = new ArrayList<>();

                if (commandLine.hasOption("c")) {
                    for (String c : commandLine.getOptionValues("c")) {
                        String[] splitted = c.split(":");
                        clusterServers.add(new ClusterServer(splitted[0], Integer.parseInt(splitted[1])));
                    }
                }

                config = new Config(commandLine.getOptionValue("h"),
                        Integer.parseInt(commandLine.getOptionValue("p")),
                        Integer.parseInt(commandLine.getOptionValue("b")),
                        commandLine.hasOption("d"),
                        Integer.parseInt(commandLine.getOptionValue("t")),
                        clusterServers,
                        (commandLine.hasOption("ci")) ? Integer.parseInt(commandLine.getOptionValue("ci")) : 300,
                        (commandLine.hasOption("si")) ? Integer.parseInt(commandLine.getOptionValue("si")) : -1);
            } else {
                System.out.println("Usage: java -jar thor-server.jar -h <Host> -p <Port> -b <Backlog> -t <Threads> [-c IP:Port IP:Port] [-d]");
                System.out.println("Example (with debugging enabled): java -jar thor-server.jar -h localhost -p 1337 -b 100 -t 4 -d");
                System.out.println("Example (with debugging enabled and cluster setup): java -jar thor-server.jar -h localhost -p 1337 -b 100 -t 4 -c localhost:1338 -d");
                System.exit(-1);
            }
        } else {
            File configFile = new File("config.json");
            if (!configFile.exists()) {
                try {
                    Files.copy(Thor.class.getClassLoader().getResourceAsStream("config.json"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.err.println("Unable to load default config!");
                    System.exit(-1);
                }
            }

            try {
                config = new Gson().fromJson(Files.lines(configFile.toPath()).map(String::toString).collect(Collectors.joining(" ")), Config.class);
            } catch (IOException e) {
                System.err.println("Unable to load 'config.json' in current directory!");
                System.exit(-1);
            }
        }

        if (config == null) {
            System.err.println("Failed to create a Config!");
            System.err.println("Please check the program parameters or the 'config.json' file!");
        } else {
            System.err.println("Using Config: " + config);

            Thor thor = new Thor(config);
            thor.init();
            thor.start();
            thor.stop();
        }
    }
}
