package de.laurinhummel.skystats.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class StopStatsInvItemMovement implements Listener {
    @EventHandler
    public void onInvItemMove(InventoryClickEvent event) {
        if(event.getView().getTitle().contains("Sky")) {
            event.setCancelled(true);
        }
    }
}
