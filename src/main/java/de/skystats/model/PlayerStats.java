package de.skystats.model;

import java.util.Map;
import java.util.UUID;

public record PlayerStats(UUID uuid, String name, Map<StatKey, Long> values) {
    public PlayerStats { values = Map.copyOf(values); }
    public long value(StatKey key) { return values.getOrDefault(key, 0L); }
}
