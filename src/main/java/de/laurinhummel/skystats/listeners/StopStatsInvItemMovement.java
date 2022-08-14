package de.laurinhummel.skystats.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class StopStatsInvItemMovement implements Listener {
    @EventHandler
    public void onInvItemMove(InventoryClickEvent event) {
        if(event.getInventory().getName().equals("SkyStats")) {
            event.setCancelled(true);
        }
    }
}
