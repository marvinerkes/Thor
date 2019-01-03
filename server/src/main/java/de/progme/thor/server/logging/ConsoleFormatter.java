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

package de.progme.thor.server.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class ConsoleFormatter extends Formatter {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String format(LogRecord record) {

        StringBuilder builder = new StringBuilder();
        builder.append("[").append(simpleDateFormat.format(new Date(record.getMillis()))).append("] ");
        builder.append("[").append(record.getLevel()).append("] ");
        builder.append(formatMessage(record));
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable throwable = record.getThrown();
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String exceptionString = sw.toString();
            builder.append(exceptionString);
        }
        builder.append("\n");

        return builder.toString();
    }
}
