package dev.lorenzz.lxsmp.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceholderCache {

    private static final class Entry {
        final String value;
        final long expiresAtNanos;

        Entry(String value, long expiresAtNanos) {
            this.value = value;
            this.expiresAtNanos = expiresAtNanos;
        }
    }

    private final long ttlNanos;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Entry>> data =
            new ConcurrentHashMap<>();

    public PlaceholderCache(long ttlMillis) {
        this.ttlNanos = ttlMillis * 1_000_000L;
    }

    @FunctionalInterface
    public interface Resolver {
        String resolve(String template);
    }

    public String get(UUID uuid, String template, Resolver resolver) {
        ConcurrentHashMap<String, Entry> inner = data.get(uuid);
        long now = System.nanoTime();
        if (inner != null) {
            Entry e = inner.get(template);
            if (e != null && e.expiresAtNanos > now) return e.value;
        } else {
            inner = data.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>(8));
        }
        String resolved = resolver.resolve(template);
        inner.put(template, new Entry(resolved, now + ttlNanos));
        return resolved;
    }

    public void invalidate(UUID uuid) {
        data.remove(uuid);
    }

    public void clear() {
        data.clear();
    }

    public void sweep() {
        long now = System.nanoTime();
        for (Map.Entry<UUID, ConcurrentHashMap<String, Entry>> e : data.entrySet()) {
            e.getValue().entrySet().removeIf(ent -> ent.getValue().expiresAtNanos <= now);
            if (e.getValue().isEmpty()) data.remove(e.getKey());
        }
    }
}

