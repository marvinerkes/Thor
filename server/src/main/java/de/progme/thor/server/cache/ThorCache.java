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

import de.progme.thor.server.Thor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Marvin Erkes on 13.06.2017.
 */
public class ThorCache {

    private static final String SNAPSHOT_FORMAT = "Thor memory snapshot format v.0.0.1";

    private Logger logger = Thor.getLogger();

    private Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService;

    public ThorCache(int cleanupInterval, int snapshotInterval) {

        if (cleanupInterval > 0) {
            executorService = Executors.newScheduledThreadPool(1);
            executorService.scheduleAtFixedRate(this::cleanup, cleanupInterval, cleanupInterval, TimeUnit.SECONDS);
        }

        if (snapshotInterval > 0) {
            if (executorService == null) {
                executorService = Executors.newScheduledThreadPool(1);
            }

            executorService.scheduleAtFixedRate(this::snapshot, snapshotInterval, snapshotInterval, TimeUnit.SECONDS);
        }

        File snapshotsFolder = new File("snapshots");
        if (!snapshotsFolder.exists()) {
            if (!snapshotsFolder.mkdir()) {
                throw new IllegalStateException("cannot create 'snapshots' folder");
            }
        }
    }

    private void cleanup() {

        for(Iterator<CacheEntry> it = cache.values().iterator(); it.hasNext(); ) {
            CacheEntry cacheEntry = it.next();

            long timestamp = cacheEntry.expireBy();
            if (timestamp != -1 && System.currentTimeMillis() > timestamp) {
                it.remove();

                logger.log(Level.FINE, "[Cache] Value '" + cacheEntry.value() + "' has been removed from the cache");
            }
        }
    }

    public void snapshot() {

        if (cache.size() > 0) {
            File file = Paths.get("snapshots", "snapshot-0.thor").toFile();

            int id = 0;
            while (file.exists()) {
                file = new File("snapshots/snapshot-" + (id++) + ".thor");
            }

            Thor.getLogger().log(Level.INFO, "[Cache] Creating snapshot from {0} entries", cache.size());

            Map<String, CacheEntry> currentCache = new HashMap<>(cache);

            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
                bufferedWriter.write(SNAPSHOT_FORMAT);
                bufferedWriter.newLine();
                for (Map.Entry<String, CacheEntry> cacheEntry : currentCache.entrySet()) {
                    bufferedWriter.write(cacheEntry.getKey());
                    bufferedWriter.write("=");
                    bufferedWriter.write(cacheEntry.getValue().value().toString());
                    bufferedWriter.write("=");
                    bufferedWriter.write(String.valueOf(cacheEntry.getValue().expireBy()));
                    bufferedWriter.newLine();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Thor.getLogger().log(Level.INFO, "Snapshot {0} saved in 'snapshots' folder", file.getName());
        }
    }

    public void loadSnapshot(String path) {

        Path file = Paths.get(path);

        Thor.getLogger().log(Level.INFO, "[Cache] Loading snapshot from {0}", path);

        try {
            List<String> lines = Files.readAllLines(file);
            if (lines.size() == 0) {
                Thor.getLogger().log(Level.SEVERE, "Snapshot file is empty!");
            }

            if (!lines.get(0).equals(SNAPSHOT_FORMAT)) {
                Thor.getLogger().log(Level.SEVERE, "Invalid snapshot format version!");
            }

            for (int i = 1; i < lines.size(); i++) {
                String[] splitted = lines.get(i).split("=");
                if (splitted.length != 3) {
                    Thor.getLogger().log(Level.SEVERE, "Invalid file format!");
                    break;
                }
                String key = splitted[0];
                String value = splitted[1];
                String expire = splitted[2];

                cache.put(key, new CacheEntry(Long.parseLong(expire), value));
            }

            Thor.getLogger().log(Level.INFO, "[Cache] Loaded {0} entries fom snapshot file", lines.size() - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {

        if(executorService != null) {
            executorService.shutdown();
        }
    }

    public void put(String key, Object value, int secondsToLive) {

        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }

        long expireBy = secondsToLive != -1 ? System.currentTimeMillis() + (secondsToLive * 1000) : secondsToLive;

        cache.put(key, new CacheEntry(expireBy, value));
    }

    public void put(String key, Object value) {

        put(key, value, -1);
    }

    public Object get(String key) {

        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return null;
        }

        long timestamp = entry.expireBy();
        if (timestamp != -1 && System.currentTimeMillis() > timestamp) {
            remove(key);
            return null;
        }

        return entry.value();
    }

    public Object getAndRemove(String key) {

        if (key == null) {
            return null;
        }

        CacheEntry entry = cache.get(key);

        if (entry != null) {
            return cache.remove(key).value();
        }

        return null;
    }

    public boolean remove(String key) {

        return getAndRemove(key) != null;
    }

    public boolean has(String key) {

        return get(key) != null;
    }

    public void expire(String key, int secondsToLive) {

        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return;
        }

        long expireBy = secondsToLive != -1 ? System.currentTimeMillis() + (secondsToLive * 1000) : secondsToLive;

        entry.expireBy(expireBy);
    }

    public long expire(String key) {

        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return -1;
        }

        long expireBy = entry.expireBy();

        return (expireBy != -1) ? (expireBy - System.currentTimeMillis()) / 1000 : expireBy;
    }
}
