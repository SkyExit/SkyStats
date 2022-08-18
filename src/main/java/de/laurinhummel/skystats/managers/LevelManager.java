package de.laurinhummel.skystats.managers;

import org.bukkit.entity.Player;


public class LevelManager {
    private Player player;
    private static StatisticsManager sm;

    public LevelManager(Player player) {
        this.player = player;
        sm = new StatisticsManager(player);
    }

    public Level getPlayerLevel() {
        int min = 0;
        int max = 220;
        int level = 1;

        for(int i = 0; min < sm.getTotalEXP(); i++) {
            min = min + 200 + (i * 20);
            max = (min + 200 + (i * 20)) - 1;
            if(max < sm.getTotalEXP()) { level = i-1; }
            else { break; }
        }

        return new Level(level, new LevelBorder(min, max), sm.getTotalEXP());
    }
}
