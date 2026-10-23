package de.skystats.data;

import de.skystats.model.*;
import org.bukkit.entity.Player;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.function.Supplier;

public final class StatsService implements AutoCloseable {
    private final Path directory, userCache;
    private final PlayerDirectory names;
    private final StatsFileReader reader;
    private final LiveStatsReader liveReader;
    private final ConcurrentMap<UUID, Player> online = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<PlayerStats>> liveReads = new HashMap<>();
    private final ThreadPoolExecutor executor;
    private final Logger logger;
    private final int maxFiles, maxTotalValues, timeoutSeconds;
    private final Duration cacheDuration, minimumRefresh;
    private volatile Snapshot cached;
    private CompletableFuture<Snapshot> inFlight;
    private Instant lastAttempt = Instant.EPOCH;
    private volatile boolean closed;

    public StatsService(Path directory, Path userCache, PlayerDirectory names, StatsFileReader reader,
                        LiveStatsReader liveReader, Logger logger, int maxFiles, int maxTotalValues, int timeoutSeconds,
                        Duration cacheDuration, Duration minimumRefresh) {
        this.directory = directory; this.userCache = userCache; this.names = names; this.reader = reader;
        this.liveReader = liveReader; this.logger = logger; this.maxFiles = maxFiles; this.maxTotalValues = maxTotalValues; this.timeoutSeconds = timeoutSeconds;
        this.cacheDuration = cacheDuration; this.minimumRefresh = minimumRefresh;
        executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(64), runnable -> {
            Thread thread = new Thread(runnable, "SkyStats-data"); thread.setDaemon(true); return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void joined(Player player) { names.remember(player.getUniqueId(), player.getName()); online.put(player.getUniqueId(), player); }
    public void left(Player player) { online.remove(player.getUniqueId(), player); }
    public <T> CompletableFuture<T> compute(Supplier<T> supplier) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Plugin disabled"));
        try { return CompletableFuture.supplyAsync(supplier, executor); }
        catch (RejectedExecutionException error) { return CompletableFuture.failedFuture(error); }
    }
    public PlayerDirectory names() { return names; }
    public Snapshot cached() { return cached; }

    public synchronized CompletableFuture<Snapshot> snapshot(boolean refresh) {
        if (closed) return CompletableFuture.failedFuture(new IllegalStateException("Plugin disabled"));
        if (inFlight != null && !inFlight.isDone()) return inFlight;
        Instant now = Instant.now();
        if (cached != null && ((!refresh && Duration.between(cached.createdAt(), now).compareTo(cacheDuration) < 0)
                || Duration.between(lastAttempt, now).compareTo(minimumRefresh) < 0)) return CompletableFuture.completedFuture(cached);
        if (cached == null && Duration.between(lastAttempt, now).compareTo(minimumRefresh) < 0) {
            return CompletableFuture.failedFuture(new IllegalStateException("Bitte warte kurz vor dem nächsten Ladeversuch."));
        }
        lastAttempt = now;
        Map<UUID, Player> capturedOnline = Map.copyOf(online);
        inFlight = compute(this::readDisk).thenCompose(disk -> {
            Map<UUID, CompletableFuture<PlayerStats>> pending = new HashMap<>();
            capturedOnline.forEach((uuid, player) -> pending.put(uuid, readLive(uuid, player, disk.players.get(uuid))));
            return CompletableFuture.allOf(pending.values().toArray(CompletableFuture[]::new))
                    .orTimeout(timeoutSeconds, TimeUnit.SECONDS).thenApplyAsync(ignored -> {
                        pending.forEach((uuid, result) -> disk.players.put(uuid, result.join()));
                        checkDatasetLimit(disk.players);
                        Snapshot snapshot = Snapshot.aggregate(disk.players, disk.warnings);
                        names.publish(disk.players.keySet());
                        try { names.save(); } catch (IOException error) { logger.warning("Cannot persist player names: " + error.getMessage()); }
                        if (!closed) cached = snapshot;
                        return snapshot;
                    }, executor);
        });
        return inFlight;
    }

    public CompletableFuture<PlayerStats> personal(UUID uuid, Snapshot snapshot) {
        Player player = online.get(uuid);
        PlayerStats disk = snapshot.players().get(uuid);
        if (player != null) return readLive(uuid, player, disk);
        return compute(() -> {
            Path file = directory.resolve(uuid + ".json");
            if (!Files.isRegularFile(file)) return null;
            try { return new PlayerStats(uuid, names.name(uuid), reader.read(file)); }
            catch (IOException error) { throw new CompletionException(error); }
        });
    }

    private synchronized CompletableFuture<PlayerStats> readLive(UUID uuid, Player player, PlayerStats disk) {
        CompletableFuture<PlayerStats> pending = liveReads.get(uuid);
        if (pending != null && !pending.isDone()) return pending;
        CompletableFuture<PlayerStats> read = liveReader.read(player, disk).orTimeout(timeoutSeconds, TimeUnit.SECONDS);
        liveReads.put(uuid, read);
        read.whenComplete((result, error) -> { synchronized (this) { liveReads.remove(uuid, read); } });
        return read;
    }

    private void checkDatasetLimit(Map<UUID, PlayerStats> players) {
        long count = 0;
        for (PlayerStats player : players.values()) {
            count += player.values().size();
            if (count > maxTotalValues) throw new CompletionException(new IOException("Dataset limit exceeded; increase max-total-values"));
        }
    }

    private record DiskData(Map<UUID, PlayerStats> players, List<String> warnings) {}
    private DiskData readDisk() {
        Map<UUID, PlayerStats> players = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        names.load(userCache, warnings);
        if (Files.isDirectory(directory)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
                int count = 0, failed = 0;
                long totalValues = 0;
                for (Path file : files) {
                    if (closed || Thread.currentThread().isInterrupted()) throw new CancellationException("Statistics service stopped");
                    String filename = file.getFileName().toString();
                    UUID uuid;
                    try { uuid = UUID.fromString(filename.substring(0, filename.length() - 5)); }
                    catch (IllegalArgumentException ignored) { continue; }
                    if (++count > maxFiles) throw new IOException("Player file limit exceeded; increase max-player-files");
                    try {
                        Map<StatKey, Long> values = reader.read(file);
                        names.resolve(uuid, warnings);
                        players.put(uuid, new PlayerStats(uuid, names.name(uuid), values));
                        totalValues += values.size();
                    } catch (IOException | RuntimeException error) {
                        if (++failed <= 5) logger.warning("Skipping invalid statistics file " + filename + ": " + error.getMessage());
                    }
                    if (totalValues > maxTotalValues) throw new IOException("Dataset limit exceeded; increase max-total-values");
                }
                if (failed > 0) warnings.add(failed + " fehlerhafte Statistikdatei(en) ausgelassen");
            } catch (IOException error) { throw new CompletionException(error); }
        } else {
            warnings.add("Statistikordner nicht gefunden: " + directory);
            logger.warning("Statistics directory not found: " + directory + "; check stats-directory in config.yml");
        }
        return new DiskData(players, warnings);
    }

    @Override public synchronized void close() {
        closed = true;
        if (inFlight != null) inFlight.cancel(false);
        List.copyOf(liveReads.values()).forEach(future -> future.cancel(false)); liveReads.clear();
        online.clear(); executor.shutdownNow();
    }
}
