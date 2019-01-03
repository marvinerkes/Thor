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

package de.progme.thor.client.cache;

import java.util.concurrent.ExecutorService;

/**
 * Created by Marvin Erkes on 15.06.2017.
 */
public interface AsyncPubSubCache extends PubSubCache {

    /**
     * Returns the underlying executor service of this async pub sub cache.
     *
     * @return The executor service.
     */
    ExecutorService executorService();
}
