package de.laurinhummel.skystats.listeners;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class StopSkyifiedItemsAnvilUsage implements Listener {
    @EventHandler
    public void onSkyifyItemAnvil(InventoryClickEvent event) {
        if(event.getCurrentItem() == null) return;
        if(event.getClickedInventory().getType().equals(InventoryType.ANVIL)) {
            ItemStack item = event.getCurrentItem();
            if(item.hasItemMeta()) {
                if(!item.getItemMeta().getLore().isEmpty()) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
