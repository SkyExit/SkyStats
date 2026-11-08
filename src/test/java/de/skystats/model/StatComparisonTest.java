package de.skystats.model;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StatComparisonTest {
    private static final StatKey STONE = new StatKey("minecraft:mined", "minecraft:stone");
    private static final StatKey DIRT = new StatKey("minecraft:mined", "minecraft:dirt");
    private static final StatKey TIME = new StatKey("minecraft:custom", "minecraft:play_time");
    private PlayerStats player(String name, long stone, long dirt, long ticks) {
        return new PlayerStats(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name, Map.of(STONE, stone, DIRT, dirt, TIME, ticks));
    }
    private Snapshot snapshot(PlayerStats... players) {
        Map<UUID, PlayerStats> data = new HashMap<>(); for (PlayerStats player : players) data.put(player.uuid(), player);
        return Snapshot.aggregate(data, List.of());
    }
    @Test void categoryTotalsIncludeAllPagesAndReplaceAnOlderPersonalSampleExactlyOnce() {
        PlayerStats old = player("A", 10, 5, 36000), fresh = player("A", 30, 10, 72000), other = player("B", 60, 20, 144000);
        StatComparison result = StatComparison.of(snapshot(old, other), fresh, Category.MINED, null, false);
        assertEquals(40, result.personal()); assertEquals(120, result.server());
        assertEquals(72000, result.personalTicks()); assertEquals(216000, result.serverTicks());
        assertEquals(95, StatComparison.of(snapshot(old, other), fresh, Category.MINED, null, true).server());
    }
    @Test void itemComparisonUsesOnlyThatItemAndIncludesANewPlayer() {
        PlayerStats fresh = player("A", 30, 10, 72000), other = player("B", 60, 20, 144000);
        StatComparison result = StatComparison.of(snapshot(other), fresh, Category.MINED, STONE, false);
        assertEquals(30, result.personal()); assertEquals(90, result.server()); assertEquals(STONE, result.unit());
    }
    @Test void overlappingTimersAndMixedCombatUnitsAreNotAdded() {
        PlayerStats player = new PlayerStats(UUID.randomUUID(), "A", Map.of(TIME, 72000L,
                new StatKey("minecraft:custom", "minecraft:time_since_rest"), 90000L));
        assertEquals(72000, StatComparison.of(snapshot(player), player, Category.PLAY_TIME, null, false).server());
        assertNull(StatComparison.of(snapshot(player), player, Category.COMBAT, null, false).unit());
    }
    @Test void categorySumsSaturateWithoutOverflow() {
        PlayerStats player = player("A", Long.MAX_VALUE, 10, 0);
        assertEquals(Long.MAX_VALUE, StatComparison.of(snapshot(player), player, Category.MINED, null, false).server());
    }
}
