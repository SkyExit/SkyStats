package de.laurinhummel.skystats.managers;

import org.bukkit.Statistic;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsManagerTest {
    @Test
    void retainsExperienceWeights() {
        Map<Statistic, Integer> values = Map.of(Statistic.TOTAL_WORLD_TIME, 72_000,
                Statistic.PLAYER_KILLS, 3, Statistic.MOB_KILLS, 5,
                Statistic.DAMAGE_DEALT, 100, Statistic.DAMAGE_TAKEN, 70,
                Statistic.CRAFTING_TABLE_INTERACTION, 2, Statistic.CLIMB_ONE_CM, 1000);
        StatisticsManager manager = new StatisticsManager(stat -> values.getOrDefault(stat, 0));
        assertEquals(360, manager.getPlayTimeEXP());
        assertEquals(70, manager.getCombatEXP());
        assertEquals(100, manager.getInteractionEXP());
        assertEquals(4, manager.getTravelEXP());
        assertEquals(534, manager.getTotalEXP());
    }

    @Test
    void highStatisticsDoNotOverflow() {
        StatisticsManager manager = new StatisticsManager(stat -> Integer.MAX_VALUE);
        assertEquals(30_586_302_800L, manager.getCombatEXP());
        assertEquals(1_975_684_955_240L, manager.getInteractionEXP());
        assertTrue(manager.getTotalEXP() > Integer.MAX_VALUE);
        assertTrue(new LevelManager(manager.getTotalEXP()).getLevel() > 1);
    }

    @Test
    void negativeStatisticsDoNotSubtractExperience() {
        assertEquals(0, new StatisticsManager(stat -> -1).getTotalEXP());
    }
}
