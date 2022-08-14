package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.misc.SkyStrings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;

            player.sendMessage(SkyStrings.SKYSTATS + "--------------------------------------------");
            player.sendMessage(SkyStrings.SKYSTATS + "Level: ");
            player.sendMessage(SkyStrings.SKYSTATS + "Exp: ");
            player.sendMessage(SkyStrings.SKYSTATS + "--------------------------------------------");
        }
        return false;
    }
}
