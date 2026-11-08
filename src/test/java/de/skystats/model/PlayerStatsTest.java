package de.skystats.model;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatsTest {
    private static final StatKey PLAYER_DEATHS = new StatKey("minecraft:killed_by", "minecraft:player");
    private static final StatKey ZOMBIE_DEATHS = new StatKey("minecraft:killed_by", "minecraft:zombie");
    private PlayerStats player(Map<StatKey, Long> values) { return new PlayerStats(UUID.randomUUID(), "Player", values); }

    @Test void residualIncludesEnvironmentalDeathsWithoutInventingIndividualCauses() {
        Map<StatKey, Long> vanilla = Map.of(PlayerStats.DEATHS, 20L, PLAYER_DEATHS, 3L, ZOMBIE_DEATHS, 5L);
        PlayerStats stats = player(vanilla);
        assertEquals(12, stats.value(PlayerStats.UNKNOWN_DEATHS));
        assertEquals(20, stats.values().entrySet().stream().filter(e -> Category.KILLED_BY.contains(e.getKey())).mapToLong(Map.Entry::getValue).sum());
        assertFalse(vanilla.containsKey(PlayerStats.UNKNOWN_DEATHS));
        assertEquals(stats.values(), player(stats.values()).values());
    }
    @Test void recalculatesResidualAfterALiveUpdateAndClampsInconsistentVanillaCounters() {
        Map<StatKey, Long> values = new HashMap<>(player(Map.of(PlayerStats.DEATHS, 10L)).values());
        values.put(PlayerStats.DEATHS, 15L); values.put(PLAYER_DEATHS, 4L);
        assertEquals(11, player(values).value(PlayerStats.UNKNOWN_DEATHS));
        assertEquals(0, player(Map.of(PlayerStats.DEATHS, 1L, PLAYER_DEATHS, 4L)).value(PlayerStats.UNKNOWN_DEATHS));
        assertEquals(0, player(Map.of(PLAYER_DEATHS, Long.MAX_VALUE, ZOMBIE_DEATHS, 1L)).value(PlayerStats.UNKNOWN_DEATHS));
    }
    @Test void duplicatePlayerKillRepresentationsAreNeverAddedTogether() {
        StatKey typed = new StatKey("minecraft:killed", "minecraft:player");
        PlayerStats stats = player(Map.of(typed, 7L, PlayerStats.PLAYER_KILLS, 5L));
        assertEquals(7, stats.value(PlayerStats.PLAYER_KILLS));
        assertFalse(stats.values().containsKey(typed));
    }
}
