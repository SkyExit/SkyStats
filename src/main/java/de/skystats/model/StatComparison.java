package de.skystats.model;

import java.util.Map;

/** Calculated only for the category or statistic the viewer has opened. */
public record StatComparison(StatKey unit, long personal, long server, long personalTicks, long serverTicks) {
    private static final StatKey PLAY_TIME = new StatKey("minecraft:custom", "minecraft:play_time");
    private static final StatKey COUNT = new StatKey("skystats:summary", "skystats:count");
    private static final StatKey DISTANCE = new StatKey("minecraft:custom", "minecraft:walk_one_cm");

    public static StatComparison of(Snapshot snapshot, PlayerStats target, Category category, StatKey key, boolean serverView) {
        StatKey unit = key != null ? key : switch (category) {
            case MOVEMENT -> DISTANCE;
            case PLAY_TIME -> PLAY_TIME;
            case COMBAT -> null; // Damage, kills and raid counters have incompatible units.
            default -> COUNT;
        };
        long total = 0, ticks = 0;
        for (PlayerStats saved : snapshot.players().values()) {
            // A freshly opened personal profile replaces its older snapshot values once.
            PlayerStats player = !serverView && saved.uuid().equals(target.uuid()) ? target : saved;
            total = Snapshot.saturatedAdd(total, value(player, category, key));
            ticks = Snapshot.saturatedAdd(ticks, player.value(PLAY_TIME));
        }
        if (!serverView && !snapshot.players().containsKey(target.uuid())) {
            total = Snapshot.saturatedAdd(total, value(target, category, key));
            ticks = Snapshot.saturatedAdd(ticks, target.value(PLAY_TIME));
        }
        return new StatComparison(unit, value(target, category, key), total, target.value(PLAY_TIME), ticks);
    }

    private static long value(PlayerStats player, Category category, StatKey key) {
        if (key != null) return player.value(key);
        if (category == Category.PLAY_TIME) return player.value(PLAY_TIME);
        if (category == Category.COMBAT) return 0;
        long sum = 0;
        for (Map.Entry<StatKey, Long> entry : player.values().entrySet()) {
            if (category.contains(entry.getKey()) && entry.getValue() > 0)
                sum = Snapshot.saturatedAdd(sum, entry.getValue());
        }
        return sum;
    }
}
