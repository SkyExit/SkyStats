package de.skystats.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;

public final class InventoryGuard implements Listener {
    private final MenuController menus;
    public InventoryGuard(MenuController menus) { this.menus = menus; }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof StatsHolder holder)) return;
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player && event.getRawSlot() >= 0 && event.getRawSlot() < 54
                && (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)) {
            menus.click(player, holder, event.getRawSlot());
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StatsHolder) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void creative(InventoryCreativeEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof StatsHolder) event.setCancelled(true);
    }
    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof StatsHolder holder && event.getPlayer() instanceof Player player) menus.closed(player, holder);
    }
}
