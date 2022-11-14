package de.laurinhummel.skystats.managers;

import de.laurinhummel.skystats.main.SkyStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class DataManager {
    private static final FileConfiguration data = SkyStats.getPlugin().getDataFile();
    private enum StorageType {
        BLOCKS_MINED,
        BLOCKS_PLACED
    }

    public static int getBlocksMined(Player player) {
        return data.getInt(player.getName() + ".BLOCKS_MINED");
    }
}
