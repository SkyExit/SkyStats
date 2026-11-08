package de.skystats.model;

import java.time.Instant;
import java.util.*;

public final class Snapshot {
    public record RankedPlayer(UUID uuid, String name, long value, int rank) {}
    public record Medals(UUID uuid, String name, long gold, long silver, long bronze, int rank) {
        public long total() { return gold + silver + bronze; }
    }
    private final Map<UUID, PlayerStats> players;
    private final Map<StatKey, Long> totals;
    private final Map<StatKey, List<RankedPlayer>> rankings;
    private final Map<StatKey, Map<UUID, RankedPlayer>> positions;
    private final List<Medals> medals;
    private final Instant createdAt;
    private final List<String> warnings;

    private Snapshot(Map<UUID, PlayerStats> players, Map<StatKey, Long> totals,
                     Map<StatKey, List<RankedPlayer>> rankings, Map<StatKey, Map<UUID, RankedPlayer>> positions,
                     List<Medals> medals, List<String> warnings) {
        this.players = Map.copyOf(players); this.totals = Map.copyOf(totals);
        this.rankings = Map.copyOf(rankings); this.positions = Map.copyOf(positions);
        this.medals = List.copyOf(medals); this.createdAt = Instant.now(); this.warnings = List.copyOf(warnings);
    }

    public static Snapshot aggregate(Map<UUID, PlayerStats> players, List<String> warnings) {
        Map<StatKey, List<PlayerStats>> competitors = new HashMap<>();
        Map<StatKey, Long> totals = new HashMap<>();
        for (PlayerStats player : players.values()) {
            player.values().forEach((key, value) -> {
                if (value > 0) {
                    totals.merge(key, value, Snapshot::saturatedAdd);
                    competitors.computeIfAbsent(key, ignored -> new ArrayList<>()).add(player);
                }
            });
        }
        Map<StatKey, List<RankedPlayer>> rankings = new HashMap<>();
        Map<StatKey, Map<UUID, RankedPlayer>> positions = new HashMap<>();
        Map<UUID, long[]> counts = new HashMap<>();
        competitors.forEach((key, participants) -> {
            participants.sort(Comparator.<PlayerStats>comparingLong(p -> p.value(key)).reversed()
                    .thenComparing(PlayerStats::name, String.CASE_INSENSITIVE_ORDER).thenComparing(PlayerStats::uuid));
            List<RankedPlayer> ranked = new ArrayList<>(participants.size());
            Map<UUID, RankedPlayer> byPlayer = new HashMap<>();
            int rank = 0;
            long previous = -1;
            for (int i = 0; i < participants.size(); i++) {
                PlayerStats player = participants.get(i);
                long value = player.value(key);
                if (value != previous) rank = i + 1;
                previous = value;
                RankedPlayer row = new RankedPlayer(player.uuid(), player.name(), value, rank);
                ranked.add(row); byPlayer.put(player.uuid(), row);
                if (rank <= 3) counts.computeIfAbsent(player.uuid(), ignored -> new long[3])[rank - 1]++;
            }
            rankings.put(key, List.copyOf(ranked)); positions.put(key, Map.copyOf(byPlayer));
        });
        List<Medals> medals = new ArrayList<>();
        counts.forEach((uuid, count) -> medals.add(new Medals(uuid, players.get(uuid).name(), count[0], count[1], count[2], 0)));
        medals.sort(Comparator.comparingLong(Medals::total).reversed()
                .thenComparing(Comparator.comparingLong(Medals::gold).reversed())
                .thenComparing(Comparator.comparingLong(Medals::silver).reversed())
                .thenComparing(Comparator.comparingLong(Medals::bronze).reversed())
                .thenComparing(Medals::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Medals::uuid));
        List<Medals> rankedMedals = new ArrayList<>(medals.size());
        int rank = 0; Medals previous = null;
        for (int i = 0; i < medals.size(); i++) {
            Medals current = medals.get(i);
            if (previous == null || current.total() != previous.total() || current.gold() != previous.gold()
                    || current.silver() != previous.silver() || current.bronze() != previous.bronze()) rank = i + 1;
            rankedMedals.add(new Medals(current.uuid(), current.name(), current.gold(), current.silver(), current.bronze(), rank));
            previous = current;
        }
        return new Snapshot(players, totals, rankings, positions, rankedMedals, warnings);
    }

    public static long saturatedAdd(long left, long right) { return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right; }
    public Map<UUID, PlayerStats> players() { return players; }
    public Set<StatKey> keys() { return totals.keySet(); }
    public long total(StatKey key) { return totals.getOrDefault(key, 0L); }
    public List<RankedPlayer> ranking(StatKey key) { return rankings.getOrDefault(key, List.of()); }
    public RankedPlayer position(StatKey key, UUID uuid) { return positions.getOrDefault(key, Map.of()).get(uuid); }
    public List<Medals> medals() { return medals; }
    public Instant createdAt() { return createdAt; }
    public List<String> warnings() { return warnings; }
}
