package de.skystats.ui;

import de.skystats.model.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.*;

public final class StatsHolder implements InventoryHolder {
    public record Context(Snapshot snapshot, PlayerStats target) {}
    private final UUID viewer;
    private final Route route;
    private final Context context;
    private final List<?> rows;
    private final Map<Integer, Runnable> actions = new HashMap<>();
    private Inventory inventory;
    public StatsHolder(UUID viewer, Route route, Context context, List<?> rows) {
        this.viewer = viewer; this.route = route; this.context = context; this.rows = rows;
    }
    @Override public Inventory getInventory() { return Objects.requireNonNull(inventory, "Inventory is not attached yet"); }
    public void attach(Inventory inventory) { if (this.inventory != null) throw new IllegalStateException("Inventory already attached"); this.inventory = inventory; }
    public UUID viewer() { return viewer; }
    public Route route() { return route; }
    public Context context() { return context; }
    public List<?> rows() { return rows; }
    public void action(int slot, Runnable action) { actions.put(slot, action); }
    public Runnable action(int slot) { return actions.get(slot); }
}
