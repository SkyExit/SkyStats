package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.shortcuts.ItemStacks;
import de.laurinhummel.skystats.shortcuts.McColors;
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
            ItemStacks itemStacks = new ItemStacks();
            Inventory menu = Bukkit.getServer().createInventory(player, 9 * 4, McColors.AQUA + "Sky" + McColors.GOLD + "Stats");

            for (int i : new int[] {0,9,18,27,8,17,26,35}) {
                menu.setItem(i, itemStacks.getPlaceholder());
            }

            menu.setItem(4, itemStacks.getPlayerHead(player));
        }
        return false;
    }
}
