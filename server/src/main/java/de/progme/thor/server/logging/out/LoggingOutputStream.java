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

package de.progme.thor.server.logging.out;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingOutputStream extends ByteArrayOutputStream {

    private static final String separator = System.getProperty("line.separator");

    private final Logger logger;

    private final Level level;

    public LoggingOutputStream(Logger logger, Level level) {

        this.logger = logger;
        this.level = level;
    }

    @Override
    public void flush() throws IOException {

        String contents = toString("UTF-8");
        super.reset();
        if (!contents.isEmpty() && !contents.equals(separator)) {
            logger.logp(level, "", "", contents);
        }
    }
}