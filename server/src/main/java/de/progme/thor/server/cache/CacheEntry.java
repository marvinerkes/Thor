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

package de.progme.thor.server.cache;

/**
 * Created by Marvin Erkes on 13.06.2017.
 */
public class CacheEntry {

    private long expireBy;

    private Object value;

    public CacheEntry(long expireBy, Object value) {

        this.expireBy = expireBy;
        this.value = value;
    }

    public long expireBy() {

        return expireBy;
    }

    public void expireBy(long expireBy) {

        this.expireBy = expireBy;
    }

    public Object value() {

        return value;
    }
}
