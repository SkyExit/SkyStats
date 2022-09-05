package de.laurinhummel.skystats.managers;

import de.laurinhummel.skystats.main.SkyStats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Objects;

public class PermissionManager {
    static FileConfiguration config = SkyStats.getPlugin().getPermissionFile();

    public static boolean hasPermission(Player player, String permission) {
        try {
            String groupA = config.getString("Groups.Admin");
            if(groupA != null) { if(player.getName().contains(groupA)) { return true; }}
            return Objects.equals(config.getString("Permissions." + permission), "Player");
        } catch (Exception ex) {
            return false;
        }
    }
}