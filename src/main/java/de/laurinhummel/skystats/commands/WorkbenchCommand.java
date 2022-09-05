package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.PermissionManager;
import de.laurinhummel.skystats.misc.SkyLogger;
import de.laurinhummel.skystats.misc.SkyStrings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorkbenchCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            if(PermissionManager.hasPermission(player, "Workbench")) {
                SkyLogger.sendPlayer(player, "Opening workbench...", SkyLogger.LogType.INFO);
                player.openWorkbench(null, true);
            } else { SkyLogger.sendPlayer(player, SkyStrings.NOPERMISSION, SkyLogger.LogType.ERROR); }
        } else { SkyLogger.sendConsole(SkyStrings.SKYSTATS + "You can't open the workbench in the console!"); }
        return true;
    }
}
