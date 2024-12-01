package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.CompareManager;
import de.laurinhummel.skystats.managers.ItemsManager;
import de.laurinhummel.skystats.misc.McColors;
import de.laurinhummel.skystats.misc.SkyLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CompareCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        Player player1, player2;
        if(sender instanceof Player) {
            if(args.length == 2) {
                player1 = Bukkit.getPlayer(args[0]) != null ? Bukkit.getPlayer(args[0]) : (Player) sender;
                player2 = Bukkit.getPlayer(args[1]) != null ? Bukkit.getPlayer(args[1]) : (Player) sender;
            } else { SkyLogger.sendPlayer((Player) sender, "You need to provide two playernames", SkyLogger.LogType.ERROR); return false;}

            ItemsManager im1 = new ItemsManager(player1);
            ItemsManager im2 = new ItemsManager(player2);
            Inventory menu = Bukkit.getServer().createInventory((Player) sender, 9 * 4, McColors.AQUA + "Sky" + McColors.GOLD + "Stats");

            for (int i : new int[] {4, 4+9, 4+2*9, 4+3*9, 0,9,18,27,8,17,26,35}) {
                menu.setItem(i, im1.getPlaceholder());
            }

            menu.setItem(2, im1.getPlayerHead());
            menu.setItem(6, im2.getPlayerHead());

            menu.setItem(19, im1.getTravelStatistics());
            menu.setItem(20, im1.getPlayTimeStatistics());
            menu.setItem(21, im1.getCombatStatistics());
            menu.setItem(29, im1.getInteractionStatistics());

            menu.setItem(23, im2.getTravelStatistics());
            menu.setItem(24, im2.getPlayTimeStatistics());
            menu.setItem(25, im2.getCombatStatistics());
            menu.setItem(33, im2.getInteractionStatistics());

            /*
            menu.setItem(21, itemsManager.getPlayTimeStatistics());
            menu.setItem(22, itemsManager.getCombatStatistics());
            menu.setItem(23, itemsManager.getTravelStatistics());
            menu.setItem(31, itemsManager.getInteractionStatistics());

             */

            ((Player) sender).openInventory(menu);
        } else { SkyLogger.sendConsole("You can't open the stats menu in the console!"); }
        return true;
    }
}
