package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.LevelBorder;
import de.laurinhummel.skystats.managers.StatisticsManager;
import de.laurinhummel.skystats.misc.McColors;
import de.laurinhummel.skystats.managers.ItemsManager;
import de.laurinhummel.skystats.misc.SkyDataFormatter;
import de.laurinhummel.skystats.misc.SkyLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (args.length == 1) ? ((Bukkit.getPlayer(args[0]) != null) ? Bukkit.getPlayer(args[0])
                    : (Player) sender) : (Player) sender;

            ItemsManager itemsManager = new ItemsManager(player);
            Inventory menu = Bukkit.getServer().createInventory(player, 9 * 4, McColors.AQUA + "Sky" + McColors.GOLD + "Stats");

            for (int i : new int[] {0,9,18,27,8,17,26,35}) {
                menu.setItem(i, itemsManager.getPlaceholder());
            }

            menu.setItem(4, itemsManager.getPlayerHead());

            menu.setItem(21, itemsManager.getPlayTimeStatistics());
            menu.setItem(22, itemsManager.getCombatStatistics());
            menu.setItem(23, itemsManager.getTravelStatistics());
            menu.setItem(31, itemsManager.getInteractionStatistics());

            player.openInventory(menu);
        } else {
            /*
            OfflinePlayer player = Bukkit.getOfflinePlayer("SkyExit");
            StatisticsManager sm = new StatisticsManager((Player) player);
            LevelBorder levelBorder = new LevelBorder(sm.getPlayerLevel());

            SkyLogger.sendConsole("--------------" + McColors.AQUA + "Statistics for " + McColors.GOLD + "SkyExit" +
                    McColors.RESET + "--------------");
            SkyLogger.sendConsole(McColors.GRAY + "PlayTime EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getPlayTimeEXP()));
            SkyLogger.sendConsole(McColors.GRAY + "Combat EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getCombatEXP()));
            SkyLogger.sendConsole(McColors.GRAY + "Travel EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getTravelEXP()));
            SkyLogger.sendConsole(McColors.GRAY + "Interaction EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getInteractionEXP()));
            SkyLogger.sendConsole(levelBorder.getMin() + " / " + levelBorder.getMax());

             */
        }
        return true;
    }
}
