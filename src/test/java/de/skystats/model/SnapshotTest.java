package de.skystats.model;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotTest {
    private static final StatKey STONE = new StatKey("minecraft:mined", "minecraft:stone");
    private static final StatKey JUMP = new StatKey("minecraft:custom", "minecraft:jump");
    private static final StatKey WALK = new StatKey("minecraft:custom", "minecraft:walk_one_cm");
    private static PlayerStats player(String name, Map<StatKey, Long> values) { return new PlayerStats(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name, values); }
    private static Snapshot snapshot(PlayerStats... players) {
        Map<UUID, PlayerStats> data = new HashMap<>(); for (PlayerStats player : players) data.put(player.uuid(), player);
        return Snapshot.aggregate(data, List.of());
    }

    @Test void awardsCompetitionRanksAndNoZeroMedals() {
        PlayerStats a = player("Anna", Map.of(STONE, 100L));
        PlayerStats b = player("Ben", Map.of(STONE, 100L));
        PlayerStats c = player("Chris", Map.of(STONE, 50L));
        PlayerStats d = player("Dana", Map.of(STONE, 0L));
        Snapshot result = snapshot(c, d, b, a);
        assertEquals(List.of(1, 1, 3), result.ranking(STONE).stream().map(Snapshot.RankedPlayer::rank).toList());
        assertEquals(List.of("Anna", "Ben", "Chris"), result.ranking(STONE).stream().map(Snapshot.RankedPlayer::name).toList());
        assertEquals(250, result.total(STONE));
        assertEquals(1, result.medals().stream().filter(m -> m.uuid().equals(a.uuid())).findFirst().orElseThrow().gold());
        assertEquals(1, result.medals().stream().filter(m -> m.uuid().equals(c.uuid())).findFirst().orElseThrow().bronze());
        assertNull(result.position(STONE, d.uuid()));
        assertTrue(result.medals().stream().noneMatch(m -> m.uuid().equals(d.uuid())));
    }
    @Test void totalMedalsTakePrecedenceOverGold() {
        PlayerStats a = player("Anna", Map.of(STONE, 10L, JUMP, 10L));
        PlayerStats b = player("Ben", Map.of(STONE, 1L, JUMP, 1L, WALK, 1L));
        Snapshot result = snapshot(a, b);
        assertEquals("Ben", result.medals().get(0).name());
        assertEquals(3, result.medals().get(0).total());
        assertEquals(1, result.medals().get(0).gold());
        assertEquals(2, result.medals().get(0).silver());
    }
    @Test void identicalMedalCountsShareRankAndUseNamesOnlyForDisplayOrder() {
        Snapshot result = snapshot(player("Zoe", Map.of(STONE, 3L)), player("Anna", Map.of(JUMP, 3L)));
        assertEquals(List.of("Anna", "Zoe"), result.medals().stream().map(Snapshot.Medals::name).toList());
        assertEquals(List.of(1, 1), result.medals().stream().map(Snapshot.Medals::rank).toList());
    }
    @Test void groupingDoesNotDuplicateCompetitions() {
        Snapshot result = snapshot(player("Anna", Map.of(WALK, 100L)));
        assertTrue(Category.GENERAL.contains(WALK)); assertTrue(Category.MOVEMENT.contains(WALK));
        assertEquals(1, result.medals().get(0).total());
    }
    @Test void usesLongTotalsAndSaturatesInsteadOfOverflowing() {
        assertEquals(4_000_000_000L, snapshot(player("A", Map.of(STONE, 2_000_000_000L)), player("B", Map.of(STONE, 2_000_000_000L))).total(STONE));
        assertEquals(Long.MAX_VALUE, snapshot(player("A", Map.of(STONE, Long.MAX_VALUE)), player("B", Map.of(STONE, 1L))).total(STONE));
    }
    @Test void aggregatesLargeSparseDatasetOffTick() {
        assertTimeout(Duration.ofSeconds(15), () -> {
            Map<UUID, PlayerStats> players = new HashMap<>();
            for (int i = 0; i < 2000; i++) {
                Map<StatKey, Long> values = new HashMap<>();
                for (int j = 0; j < 80; j++) values.put(new StatKey("minecraft:mined", "minecraft:test_" + j), (long) i + 1);
                PlayerStats player = player("Player" + i, values); players.put(player.uuid(), player);
            }
            Snapshot result = Snapshot.aggregate(players, List.of());
            assertEquals(80, result.keys().size()); assertEquals(2000, result.players().size());
            assertEquals(2_001_000L, result.total(new StatKey("minecraft:mined", "minecraft:test_0")));
            assertEquals(3, result.medals().size());
        });
    }
}
