package de.skystats.data;

import de.skystats.model.*;
import de.skystats.platform.TaskBridge;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class LiveStatsReader {
    private final TaskBridge tasks;
    private final StatCatalog catalog;
    private final int batchSize;
    public LiveStatsReader(TaskBridge tasks, StatCatalog catalog, int batchSize) { this.tasks = tasks; this.catalog = catalog; this.batchSize = batchSize; }
    public CompletableFuture<PlayerStats> read(Player player, PlayerStats disk) {
        CompletableFuture<PlayerStats> result = new CompletableFuture<>();
        Map<StatKey, Long> values = new HashMap<>(disk == null ? Map.of() : disk.values());
        class Batch implements Runnable {
            private int cursor;
            @Override public void run() {
                if (result.isDone()) return;
                try {
                    int end = Math.min(cursor + batchSize, catalog.entries().size());
                    while (cursor < end) {
                        StatCatalog.Entry entry = catalog.entries().get(cursor++);
                        try {
                            long value = entry.read(player);
                            if (value > 0) values.put(entry.key(), value); else values.remove(entry.key());
                        } catch (IllegalArgumentException ignored) { /* Some registry combinations have no Vanilla statistic. */ }
                    }
                    if (cursor < catalog.entries().size()) tasks.entityLater(player, this, this::retired, 1);
                    else result.complete(new PlayerStats(player.getUniqueId(), player.getName(), values));
                } catch (Throwable error) { result.completeExceptionally(error); }
            }
            void retired() { result.completeExceptionally(new IllegalStateException("Player disconnected during live statistics read")); }
        }
        Batch batch = new Batch();
        tasks.entity(player, batch, batch::retired);
        return result;
    }
}
