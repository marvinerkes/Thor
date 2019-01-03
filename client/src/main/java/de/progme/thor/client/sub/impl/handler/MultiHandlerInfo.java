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

package de.progme.thor.client.sub.impl.handler;

import de.progme.thor.client.sub.impl.handler.annotation.Key;
import de.progme.thor.client.sub.impl.handler.annotation.Value;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marvin Erkes on 25.03.2017.
 */
public class MultiHandlerInfo {

    private List<Entry> entries = new ArrayList<>();

    private Object object;

    public MultiHandlerInfo(List<Entry> entries, Object object) {

        this.entries = entries;
        this.object = object;
    }

    public List<Entry> entries() {

        return entries;
    }

    public Object object() {

        return object;
    }

    public static class Entry {

        private Key key;

        private Value value;

        private Class<?> paramClass;

        private ClassType classType;

        private Method method;

        public Entry(Key key, Value value, Class<?> paramClass, ClassType classType, Method method) {

            this.key = key;
            this.value = value;
            this.paramClass = paramClass;
            this.classType = classType;
            this.method = method;
        }

        public Key key() {

            return key;
        }

        public Value value() {

            return value;
        }

        public Class<?> paramClass() {

            return paramClass;
        }

        public ClassType classType() {

            return classType;
        }

        public Method method() {

            return method;
        }
    }
}
