package de.laurinhummel.skystats.menus;

import de.laurinhummel.skystats.managers.ItemsManager;
import de.laurinhummel.skystats.managers.PlayerStatistics;
import de.laurinhummel.skystats.misc.SkyLogger;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class StatsMenuService implements Listener {
    private final Plugin plugin;
    private final PlayerScheduler scheduler;
    private final Map<UUID, Object> requests = new ConcurrentHashMap<>();

    public StatsMenuService(Plugin plugin, PlayerScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    public void openStats(Player viewer, Player target) {
        load(viewer, scheduler.supply(target, () -> PlayerStatistics.capture(target)), snapshot -> {
            ItemsManager items = new ItemsManager(snapshot);
            Inventory inventory = new StatsInventoryHolder().getInventory();
            fillBorders(inventory, items, false);
            inventory.setItem(4, items.getPlayerHead());
            inventory.setItem(21, items.getPlayTimeStatistics());
            inventory.setItem(22, items.getCombatStatistics());
            inventory.setItem(23, items.getTravelStatistics());
            inventory.setItem(31, items.getInteractionStatistics());
            viewer.openInventory(inventory);
        });
    }

    public void openComparison(Player viewer, Player first, Player second) {
        CompletableFuture<PlayerStatistics> firstData = scheduler.supply(first, () -> PlayerStatistics.capture(first));
        // Comparing a player to themselves must use the exact same snapshot.
        CompletableFuture<PlayerStatistics> secondData = first == second ? firstData
                : scheduler.supply(second, () -> PlayerStatistics.capture(second));
        load(viewer, firstData.thenCombine(secondData, Comparison::new), comparison -> {
            ItemsManager left = new ItemsManager(comparison.first());
            ItemsManager right = new ItemsManager(comparison.second());
            Inventory inventory = new StatsInventoryHolder().getInventory();
            fillBorders(inventory, left, true);
            inventory.setItem(2, left.getPlayerHead());
            inventory.setItem(6, right.getPlayerHead());
            inventory.setItem(19, left.getTravelStatistics());
            inventory.setItem(20, left.getPlayTimeStatistics());
            inventory.setItem(21, left.getCombatStatistics());
            inventory.setItem(29, left.getInteractionStatistics());
            inventory.setItem(23, right.getTravelStatistics());
            inventory.setItem(24, right.getPlayTimeStatistics());
            inventory.setItem(25, right.getCombatStatistics());
            inventory.setItem(33, right.getInteractionStatistics());
            viewer.openInventory(inventory);
        });
    }

    private <T> void load(Player viewer, CompletableFuture<T> data, Consumer<T> open) {
        UUID viewerId = viewer.getUniqueId();
        Object request = new Object();
        requests.put(viewerId, request);
        data.whenComplete((snapshot, failure) -> {
            if (!plugin.isEnabled()) {
                requests.remove(viewerId, request);
                return;
            }
            scheduler.run(viewer, () -> {
                if (!requests.remove(viewerId, request)) {
                    return;
                }
                if (failure == null) {
                    open.accept(snapshot);
                } else {
                    Throwable cause = failure instanceof CompletionException ? failure.getCause() : failure;
                    if (!(cause instanceof PlayerScheduler.PlayerUnavailableException)) {
                        plugin.getLogger().log(Level.SEVERE, "Could not read player statistics", cause);
                    }
                    SkyLogger.sendPlayer(viewer, "Could not load statistics. The player may have disconnected.", SkyLogger.LogType.ERROR);
                }
            }).whenComplete((ignored, dispatchFailure) -> {
                requests.remove(viewerId, request);
                if (dispatchFailure != null && plugin.isEnabled()
                        && !(dispatchFailure instanceof PlayerScheduler.PlayerUnavailableException)) {
                    plugin.getLogger().log(Level.SEVERE, "Could not open statistics menu", dispatchFailure);
                }
            });
        });
    }

    private static void fillBorders(Inventory inventory, ItemsManager items, boolean comparison) {
        for (int slot : new int[]{0, 9, 18, 27, 8, 17, 26, 35}) {
            inventory.setItem(slot, items.getPlaceholder());
        }
        if (comparison) {
            for (int slot : new int[]{4, 13, 22, 31}) {
                inventory.setItem(slot, items.getPlaceholder());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        requests.remove(event.getPlayer().getUniqueId());
    }

    public void clear() { requests.clear(); }

    private record Comparison(PlayerStatistics first, PlayerStatistics second) { }
}
