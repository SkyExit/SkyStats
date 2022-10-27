package de.laurinhummel.skystats.listeners;

import de.laurinhummel.skystats.managers.SkyifyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class SkyifyPerformAction implements Listener {
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack item = event.getPlayer().getItemInUse();
        SkyifyManager manager = new SkyifyManager(item);
        if(manager.isSkyify()) {

        }
    }
}
