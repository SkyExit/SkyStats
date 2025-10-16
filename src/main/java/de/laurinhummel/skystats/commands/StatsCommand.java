package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.misc.McColors;
import de.laurinhummel.skystats.managers.ItemsManager;
import de.laurinhummel.skystats.misc.SkyLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class StatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            Player target = (args.length == 1) ? ((Bukkit.getPlayer(args[0]) != null) ? Bukkit.getPlayer(args[0])
                    : (Player) sender) : (Player) sender;

            ItemsManager itemsManager = new ItemsManager(target);
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
        } else { SkyLogger.sendConsole("You can't open the stats menu in the console!"); }
        return true;
    }
}
