package de.skystats.model;

import java.util.*;

public record PlayerStats(UUID uuid, String name, Map<StatKey, Long> values) {
    public static final StatKey PLAYER_KILLS = new StatKey("minecraft:custom", "minecraft:player_kills");
    public static final StatKey DEATHS = new StatKey("minecraft:custom", "minecraft:deaths");
    public static final StatKey UNKNOWN_DEATHS = new StatKey("skystats:deaths", "skystats:unknown");
    private static final StatKey TYPED_PLAYER_KILLS = new StatKey("minecraft:killed", "minecraft:player");

    public PlayerStats {
        Map<StatKey, Long> normalized = new HashMap<>(values);
        // Vanilla exposes player kills twice. Keep one competition and one category entry.
        long kills = Math.max(normalized.getOrDefault(PLAYER_KILLS, 0L), normalized.getOrDefault(TYPED_PLAYER_KILLS, 0L));
        normalized.remove(TYPED_PLAYER_KILLS);
        if (kills > 0) normalized.put(PLAYER_KILLS, kills);
        normalized.remove(UNKNOWN_DEATHS);
        long attributed = 0;
        for (var entry : normalized.entrySet()) {
            if (entry.getKey().type().equals("minecraft:killed_by") && entry.getValue() > 0)
                attributed = Snapshot.saturatedAdd(attributed, entry.getValue());
        }
        long deaths = normalized.getOrDefault(DEATHS, 0L);
        if (deaths > attributed) normalized.put(UNKNOWN_DEATHS, deaths - attributed);
        values = Map.copyOf(normalized);
    }
    public long value(StatKey key) { return values.getOrDefault(key, 0L); }
}
