package de.laurinhummel.skystats.listeners;

import de.laurinhummel.skystats.managers.SkyifyManager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class StopSkyifiedItemsAnvilUsage implements Listener {
    @EventHandler
    public void onSkyifyItemAnvil(InventoryClickEvent event) {
        if(event.getCurrentItem() == null) return;
        if(event.getClickedInventory().getType().equals(InventoryType.ANVIL)) {
            ItemStack item = event.getCurrentItem();
            SkyifyManager manager = new SkyifyManager(event.getCurrentItem());
            if(manager.isSkyify()) {
                event.setCancelled(true);
                event.getWhoClicked().closeInventory();
            }
        }
    }
}
