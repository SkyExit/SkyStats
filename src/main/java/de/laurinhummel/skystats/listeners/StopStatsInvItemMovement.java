package de.laurinhummel.skystats.listeners;

import de.laurinhummel.skystats.menus.StatsInventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class StopStatsInvItemMovement implements Listener {
    @EventHandler
    public void onInvItemMove(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StatsInventoryHolder) {
            // Includes shift-clicks, hotbar swaps and double-click collection.
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInvItemDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StatsInventoryHolder) {
            event.setCancelled(true);
        }
    }
}
