package de.skystats.model;

import java.util.Map;
import java.util.Objects;

public record StatKey(String type, String id) implements Comparable<StatKey> {
    private static final Map<String, String> ALIASES = Map.of(
            "minecraft:play_one_minute", "minecraft:play_time");

    public StatKey {
        Objects.requireNonNull(type);
        Objects.requireNonNull(id);
        if (!type.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid statistic identifier");
        }
        if (type.equals("minecraft:custom")) id = ALIASES.getOrDefault(id, id);
    }

    public boolean isDistance() { return type.equals("minecraft:custom") && id.endsWith("_one_cm"); }
    public boolean isTime() {
        return type.equals("minecraft:custom") && (id.endsWith("_time") || id.equals("minecraft:time_since_death") || id.equals("minecraft:time_since_rest"));
    }
    public boolean isPlayTime() { return type.equals("minecraft:custom") && id.equals("minecraft:play_time"); }
    public boolean isDamage() { return type.equals("minecraft:custom") && id.startsWith("minecraft:damage_"); }
    public String technicalName() { return type + " / " + id; }
    @Override public int compareTo(StatKey other) {
        int order = type.compareTo(other.type);
        return order == 0 ? id.compareTo(other.id) : order;
    }
}
