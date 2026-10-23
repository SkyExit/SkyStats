package de.laurinhummel.skystats.managers;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;

import java.util.EnumMap;
import java.util.Map;

/** A snapshot captured exclusively on the target player's owning thread. */
public final class PlayerStatistics {
    private final String name;
    private final PlayerProfile profile;
    private final Map<Statistic, Integer> statistics;

    private PlayerStatistics(String name, PlayerProfile profile, Map<Statistic, Integer> statistics) {
        this.name = name;
        this.profile = profile.clone();
        this.statistics = Map.copyOf(statistics);
    }

    public static PlayerStatistics capture(Player player) {
        Map<Statistic, Integer> values = new EnumMap<>(Statistic.class);
        for (Statistic statistic : Statistic.values()) {
            if (statistic.getType() == Statistic.Type.UNTYPED) {
                values.put(statistic, player.getStatistic(statistic));
            }
        }
        return new PlayerStatistics(player.getName(), player.getPlayerProfile(), values);
    }

    public String getName() { return name; }
    public PlayerProfile getProfile() { return profile.clone(); }
    public int getStatistic(Statistic statistic) { return statistics.getOrDefault(statistic, 0); }
}
