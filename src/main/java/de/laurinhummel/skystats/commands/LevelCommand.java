package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.LevelManager;
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
            LevelManager levelManager = new LevelManager(player);

            player.sendMessage(SkyStrings.SKYSTATS + "----------------------------");
            player.sendMessage(SkyStrings.SKYSTATS + "Level: " + levelManager.getLevel());
            player.sendMessage(SkyStrings.SKYSTATS + "Exp: " + levelManager.getProgressBar(13));
            player.sendMessage(SkyStrings.SKYSTATS + "----------------------------");
        }
        return true;
    }
}
