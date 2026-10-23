package de.laurinhummel.skystats.managers;

import org.bukkit.Statistic;
import java.util.function.ToIntFunction;

public class StatisticsManager {
    private final ToIntFunction<Statistic> statistics;
    public StatisticsManager(ToIntFunction<Statistic> statistics) {
        this.statistics = statistics;
    }

    private long g(Statistic statistic) { return Math.max(0, statistics.applyAsInt(statistic)); }

    public long getTravelEXP() {
        return (long) ((g(Statistic.CLIMB_ONE_CM) / 2.5
                        + g(Statistic.CROUCH_ONE_CM) / 1.3
                        + g(Statistic.FALL_ONE_CM) / 10.0
                        + g(Statistic.SPRINT_ONE_CM) / 5.6
                        + g(Statistic.FLY_ONE_CM) / 5.8
                        + g(Statistic.SWIM_ONE_CM) / 2.2
                        + g(Statistic.WALK_ONE_CM) / 4.3
                        + g(Statistic.BOAT_ONE_CM) / 8.0
                        + g(Statistic.AVIATE_ONE_CM) / 20.0
                        + g(Statistic.HORSE_ONE_CM) / 12.0)
                        / 100);
    }

    public long getPlayTimeEXP() { return g(Statistic.TOTAL_WORLD_TIME) / 200; }

    public long getCombatEXP() {
        return g(Statistic.DAMAGE_DEALT) / 10
                + g(Statistic.DAMAGE_TAKEN) / 7
                + g(Statistic.PLAYER_KILLS) * 10
                + g(Statistic.MOB_KILLS) * 4;
    }

    public long getInteractionEXP() {
        return (g(Statistic.CRAFTING_TABLE_INTERACTION) * 5
                + g(Statistic.FURNACE_INTERACTION) * 10
                + g(Statistic.INTERACT_WITH_CAMPFIRE) * 7
                + g(Statistic.INTERACT_WITH_ANVIL) * 10
                + g(Statistic.INTERACT_WITH_BLAST_FURNACE) * 10
                + g(Statistic.INTERACT_WITH_SMOKER) * 10
                + g(Statistic.INTERACT_WITH_STONECUTTER) * 10
                + g(Statistic.BEACON_INTERACTION) * 20
                + g(Statistic.BREWINGSTAND_INTERACTION) * 10) * 10;
    }

    public long getTotalEXP() {
        return getTravelEXP()
                + getPlayTimeEXP()
                + getCombatEXP()
                + getInteractionEXP();
    }
}
