package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.SkyifyManager;
import de.laurinhummel.skystats.misc.McColors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SkyifyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack item = player.getInventory().getItemInMainHand();
                if(!EnchantmentTarget.TOOL.includes(item)) { player.sendMessage("no"); return true; }
            item.setItemMeta(new SkyifyManager(item).getDefaultSkyifyMeta());
            player.getInventory().setItemInMainHand(item);
        }
        return false;
    }
}
