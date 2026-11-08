package de.skystats.model;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MedalAwardTest {
    @Test void privateAwardsSortByMedalThenDescendingValueAndFindTierPages() {
        UUID target = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        Map<StatKey, Long> aValues = new HashMap<>(), bValues = new HashMap<>(), cValues = new HashMap<>();
        for (int i = 0; i < 80; i++) {
            StatKey key = new StatKey("minecraft:mined", "minecraft:entry_" + i);
            aValues.put(key, (long) i + 1);
            if (i >= 40) bValues.put(key, 1000L);
            if (i >= 75) cValues.put(key, 2000L);
        }
        Snapshot snapshot = Snapshot.aggregate(Map.of(target, new PlayerStats(target, "A", aValues),
                b, new PlayerStats(b, "B", bValues), c, new PlayerStats(c, "C", cValues)), List.of());
        List<MedalAward> awards = MedalAward.forPlayer(snapshot, target);
        assertEquals(List.of(40L, 39L, 38L), awards.stream().limit(3).map(a -> a.placement().value()).toList());
        assertEquals(75, awards.get(40).placement().value());
        assertEquals(80, awards.get(75).placement().value());
        assertEquals(0, MedalAward.firstIndex(awards, 1));
        assertEquals(40, MedalAward.firstIndex(awards, 2));
        assertEquals(75, MedalAward.firstIndex(awards, 3));
        assertEquals(-1, MedalAward.firstIndex(List.of(), 1));
        assertEquals(-1, MedalAward.firstIndex(awards.subList(0, 40), 2));
        assertEquals(0, MedalAward.firstIndex(awards.subList(40, 75), 2));
        assertEquals(-1, MedalAward.firstIndex(awards.subList(40, 75), 1));
    }
}
