package de.laurinhummel.skystats.menus;

import de.laurinhummel.skystats.misc.McColors;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class StatsInventoryHolder implements InventoryHolder {
    private final Inventory inventory;

    public StatsInventoryHolder() {
        inventory = Bukkit.createInventory(this, 36, McColors.AQUA + "Sky" + McColors.GOLD + "Stats");
    }

    @Override
    public Inventory getInventory() { return inventory; }
}
