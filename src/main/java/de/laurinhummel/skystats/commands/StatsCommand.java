package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.misc.McColors;
import de.laurinhummel.skystats.misc.SkyStatsItems;
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
            SkyStatsItems statsItems = new SkyStatsItems();
            Inventory menu = Bukkit.getServer().createInventory(player, 9 * 4, McColors.AQUA + "Sky" + McColors.GOLD + "Stats");

            for (int i : new int[] {0,9,18,27,8,17,26,35}) {
                menu.setItem(i, statsItems.getPlaceholder());
            }

            menu.setItem(4, statsItems.getPlayerHead(player));

            menu.setItem(19, statsItems.getPlayTimeStatistics(player));
            menu.setItem(20, statsItems.getCombatStatistics(player));

            player.openInventory(menu);
        }
        return true;
    }
}
