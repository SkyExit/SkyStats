package de.skystats.data;

import de.skystats.model.*;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatsServiceTest {
    @TempDir Path root;
    private static final StatKey STONE = new StatKey("minecraft:mined", "minecraft:stone");
    private final LiveStatsReader live = mock(LiveStatsReader.class);
    private StatsService service(int limit) {
        return new StatsService(root.resolve("stats"), root.resolve("usercache.json"), new PlayerDirectory(root.resolve("plugin/players.json")),
                new StatsFileReader(1024 * 1024, 1000), live, Logger.getAnonymousLogger(), 1000, limit, 5, Duration.ofSeconds(30), Duration.ZERO);
    }
    private void file(UUID uuid, long stone) throws Exception {
        Files.createDirectories(root.resolve("stats"));
        Files.writeString(root.resolve("stats/" + uuid + ".json"), "{\"stats\":{\"minecraft:mined\":{\"minecraft:stone\":" + stone + "}}}");
    }
    @Test void replacesOnlineDiskValuesAndCoalescesConcurrentReads() throws Exception {
        UUID onlineId = UUID.randomUUID(), offlineId = UUID.randomUUID(); file(onlineId, 5); file(offlineId, 300);
        Player player = mock(Player.class); when(player.getUniqueId()).thenReturn(onlineId); when(player.getName()).thenReturn("Online");
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<PlayerStats> pending = new CompletableFuture<>();
        when(live.read(eq(player), any())).thenAnswer(call -> { started.countDown(); return pending; });
        try (StatsService service = service(1000)) {
            service.joined(player);
            CompletableFuture<Snapshot> first = service.snapshot(false);
            assertTrue(started.await(3, TimeUnit.SECONDS));
            assertSame(first, service.snapshot(true));
            Snapshot empty = Snapshot.aggregate(Map.of(), List.of());
            assertSame(pending, service.personal(onlineId, empty));
            pending.complete(new PlayerStats(onlineId, "Online", Map.of(STONE, 100L)));
            Snapshot snapshot = first.get(3, TimeUnit.SECONDS);
            assertEquals(400, snapshot.total(STONE)); assertEquals(2, snapshot.players().size());
            assertEquals(100, snapshot.players().get(onlineId).value(STONE));
            verify(live, times(1)).read(eq(player), any());
            assertSame(snapshot, service.snapshot(false).get(3, TimeUnit.SECONDS));
        }
    }
    @Test void skipsCorruptFileAndReadsOfflinePersonalDataAgain() throws Exception {
        UUID id = UUID.randomUUID(); file(id, 100);
        Files.writeString(root.resolve("stats/" + UUID.randomUUID() + ".json"), "{\"stats\":");
        try (StatsService service = service(1000)) {
            Snapshot snapshot = service.snapshot(false).get(3, TimeUnit.SECONDS);
            assertEquals(100, snapshot.total(STONE)); assertEquals(1, snapshot.warnings().size());
            file(id, 200);
            assertEquals(200, service.personal(id, snapshot).get(3, TimeUnit.SECONDS).value(STONE));
            assertNull(service.personal(UUID.randomUUID(), snapshot).get(3, TimeUnit.SECONDS));
        }
    }
    @Test void enforcesGlobalDatasetLimitBeforeRankingAllocation() throws Exception {
        file(UUID.randomUUID(), 1); file(UUID.randomUUID(), 1);
        try (StatsService service = service(1)) { assertThrows(ExecutionException.class, () -> service.snapshot(false).get(3, TimeUnit.SECONDS)); }
    }
    @Test void loadsOfflinePlayersFromRealModernLayoutWithNamesAndNoDuplicateLegacyValues() throws Exception {
        UUID id = UUID.randomUUID();
        Path world = root.resolve("world");
        Files.createDirectories(world.resolve("players/stats"));
        Files.createDirectories(world.resolve("stats"));
        String filename = id + ".json";
        Files.writeString(world.resolve("players/stats/" + filename), "{\"stats\":{\"minecraft:mined\":{\"minecraft:stone\":300}}}");
        Files.writeString(world.resolve("stats/" + filename), "{\"stats\":{\"minecraft:mined\":{\"minecraft:stone\":999}}}");
        Files.writeString(root.resolve("usercache.json"), "[{\"uuid\":\"" + id + "\",\"name\":\"OfflineUser\"}]");
        for (Path entry : List.of(world, world.resolve("dimensions/minecraft/overworld"))) {
            try (StatsService service = new StatsService(StatsPaths.primaryStatistics(entry), root.resolve("usercache.json"),
                    new PlayerDirectory(root.resolve("plugin/players.json")), new StatsFileReader(1024 * 1024, 1000), live,
                    Logger.getAnonymousLogger(), 1000, 1000, 5, Duration.ofSeconds(30), Duration.ZERO)) {
                Snapshot snapshot = service.snapshot(false).get(3, TimeUnit.SECONDS);
                assertEquals(1, snapshot.players().size());
                assertEquals(300, snapshot.total(STONE));
                assertEquals("OfflineUser", snapshot.ranking(STONE).get(0).name());
                assertEquals(List.of(id), service.names().find("OfflineUser"));
                assertEquals(List.of("OfflineUser"), service.names().complete("Off"));
                assertEquals(300, service.personal(id, snapshot).get(3, TimeUnit.SECONDS).value(STONE));
                assertTrue(snapshot.warnings().isEmpty());
            }
        }
        verifyNoInteractions(live);
    }
    @Test void reportsMissingDirectoryInsteadOfSilentlyShowingOnlyOnlinePlayers() throws Exception {
        try (StatsService service = service(1000)) {
            Snapshot snapshot = service.snapshot(false).get(3, TimeUnit.SECONDS);
            assertTrue(snapshot.warnings().stream().anyMatch(warning -> warning.contains("Statistikordner")));
        }
    }
    @Test void shutdownCancelsPendingLiveReads() throws Exception {
        Player player = mock(Player.class); UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid); when(player.getName()).thenReturn("Online");
        CompletableFuture<PlayerStats> pending = new CompletableFuture<>(); when(live.read(eq(player), any())).thenReturn(pending);
        StatsService service = service(1000); service.joined(player);
        service.personal(uuid, Snapshot.aggregate(Map.of(), List.of())); service.close();
        assertTrue(pending.isCancelled()); assertTrue(service.snapshot(false).isCompletedExceptionally());
    }
}
