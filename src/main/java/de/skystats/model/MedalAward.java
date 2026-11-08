package de.skystats.model;

import java.util.*;

public record MedalAward(StatKey key, Snapshot.RankedPlayer placement) {
    public static List<MedalAward> forPlayer(Snapshot snapshot, UUID player) {
        return snapshot.keys().stream().map(key -> new MedalAward(key, snapshot.position(key, player)))
                .filter(award -> award.placement() != null && award.placement().rank() <= 3)
                .sorted(Comparator.comparingInt((MedalAward award) -> award.placement().rank())
                        .thenComparing(Comparator.comparingLong((MedalAward award) -> award.placement().value()).reversed())
                        .thenComparing(MedalAward::key)).toList();
    }

    public static int firstIndex(List<?> awards, int rank) {
        // Awards are sorted by rank. Keep page switches cheap even for large profiles.
        int low = 0, high = awards.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (!(awards.get(middle) instanceof MedalAward award)) return -1;
            if (award.placement().rank() < rank) low = middle + 1;
            else high = middle;
        }
        return low < awards.size() && awards.get(low) instanceof MedalAward award && award.placement().rank() == rank ? low : -1;
    }
}
