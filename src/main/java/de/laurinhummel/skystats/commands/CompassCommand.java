package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.PermissionManager;
import de.laurinhummel.skystats.misc.McColors;
import de.laurinhummel.skystats.misc.SkyLogger;
import de.laurinhummel.skystats.misc.SkyStrings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

public class CompassCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            if(args.length == 2) {
                double X = Double.parseDouble(args[0]);
                double Z = Double.parseDouble(args[1]);
                if (PermissionManager.hasPermission(player, "Compass")) {
                    if (player.getInventory().getItemInMainHand().isSimilar(new ItemStack(Material.COMPASS))) {
                        Location lc = new Location(player.getWorld(), X, 0.0, Z);
                        player.setCompassTarget(lc);
                        SkyLogger.sendPlayer(player, "Compass location set to " + McColors.GOLD + "X: " + McColors.AQUA + X + " and " +
                                McColors.GOLD + "Z: " + McColors.AQUA + Z, SkyLogger.LogType.INFO);
                    } else { SkyLogger.sendPlayer(player, "You need to have a compass in your main hand!", SkyLogger.LogType.ERROR); }
                } else { SkyLogger.sendPlayer(player, SkyStrings.NOPERMISSION, SkyLogger.LogType.ERROR); }
            } else { SkyLogger.sendPlayer(player, "You have to provide coordinates!", SkyLogger.LogType.ERROR); }
        } else { SkyLogger.sendConsole(SkyStrings.NOTINCONSOLE); }
        return true;
    }
}
