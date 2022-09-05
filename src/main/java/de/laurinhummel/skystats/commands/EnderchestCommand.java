package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.PermissionManager;
import de.laurinhummel.skystats.misc.SkyLogger;
import de.laurinhummel.skystats.misc.SkyStrings;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EnderchestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            if(PermissionManager.hasPermission(player, "Enderchest")) {
                SkyLogger.sendPlayer(player, "Opening endechest...", SkyLogger.LogType.INFO);
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
                player.openInventory(player.getEnderChest());
            } else { SkyLogger.sendPlayer(player, SkyStrings.NOPERMISSION, SkyLogger.LogType.ERROR); }
        } else { SkyLogger.sendConsole(SkyStrings.SKYSTATS + "You can't open the enderchest in the console!"); }
        return true;
    }
}
