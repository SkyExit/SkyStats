package de.laurinhummel.skystats.menus;

import de.laurinhummel.skystats.managers.ItemsManager;
import de.laurinhummel.skystats.managers.PlayerStatistics;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StatsMenuServiceTest {
    private Plugin plugin;
    private PlayerScheduler scheduler;
    private Player viewer;
    private Player target;
    private StatsMenuService menus;
    private List<Runnable> viewerTasks;

    @BeforeEach
    void setup() {
        plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        scheduler = mock(PlayerScheduler.class);
        viewer = mock(Player.class);
        target = mock(Player.class);
        when(viewer.getUniqueId()).thenReturn(UUID.randomUUID());
        menus = new StatsMenuService(plugin, scheduler);
        viewerTasks = new ArrayList<>();
        when(scheduler.run(eq(viewer), any())).thenAnswer(call -> {
            Runnable action = call.getArgument(1);
            CompletableFuture<Void> completed = new CompletableFuture<>();
            viewerTasks.add(() -> {
                action.run();
                completed.complete(null);
            });
            return completed;
        });
    }

    @Test
    void crossRegionComparisonWaitsForBothSnapshotsAndViewerThread() {
        Player second = mock(Player.class);
        CompletableFuture<PlayerStatistics> firstData = new CompletableFuture<>();
        CompletableFuture<PlayerStatistics> secondData = new CompletableFuture<>();
        when(scheduler.<PlayerStatistics>supply(eq(target), any())).thenReturn(firstData);
        when(scheduler.<PlayerStatistics>supply(eq(second), any())).thenReturn(secondData);
        menus.openComparison(viewer, target, second);
        firstData.complete(mock(PlayerStatistics.class));
        assertTrue(viewerTasks.isEmpty());
        verify(viewer, never()).openInventory(any(Inventory.class));
        secondData.complete(mock(PlayerStatistics.class));
        assertEquals(1, viewerTasks.size());
        verify(viewer, never()).openInventory(any(Inventory.class));
        Inventory inventory = mock(Inventory.class);
        try (var bukkit = mockStatic(Bukkit.class); var items = mockConstruction(ItemsManager.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(36), anyString())).thenReturn(inventory);
            viewerTasks.getFirst().run();
            verify(viewer).openInventory(inventory);
            assertEquals(2, items.constructed().size());
        }
        // Live state is never read from either target by the menu thread.
        verifyNoInteractions(target, second);
    }

    @Test
    void olderRequestCannotOverwriteNewerRequest() {
        CompletableFuture<PlayerStatistics> oldRequest = new CompletableFuture<>();
        CompletableFuture<PlayerStatistics> newRequest = new CompletableFuture<>();
        var snapshots = new java.util.ArrayDeque<>(List.of(oldRequest, newRequest));
        when(scheduler.<PlayerStatistics>supply(eq(target), any())).thenAnswer(call -> snapshots.removeFirst());
        menus.openStats(viewer, target);
        menus.openStats(viewer, target);
        oldRequest.complete(mock(PlayerStatistics.class));
        newRequest.complete(mock(PlayerStatistics.class));
        Inventory inventory = mock(Inventory.class);
        try (var bukkit = mockStatic(Bukkit.class); var items = mockConstruction(ItemsManager.class)) {
            bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(36), anyString())).thenReturn(inventory);
            viewerTasks.forEach(Runnable::run);
            verify(viewer, times(1)).openInventory(inventory);
        }
    }

    @Test
    void disconnectInvalidatesAnOutstandingRequest() {
        CompletableFuture<PlayerStatistics> data = new CompletableFuture<>();
        when(scheduler.<PlayerStatistics>supply(eq(target), any())).thenReturn(data);
        menus.openStats(viewer, target);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(quit.getPlayer()).thenReturn(viewer);
        menus.onQuit(quit);
        data.complete(mock(PlayerStatistics.class));
        viewerTasks.forEach(Runnable::run);
        verify(viewer, never()).openInventory(any(Inventory.class));
    }

    @Test
    void targetDisconnectReportsAnErrorOnTheViewerThread() {
        when(scheduler.supply(eq(target), any())).thenReturn(CompletableFuture.failedFuture(new PlayerScheduler.PlayerUnavailableException()));
        menus.openStats(viewer, target);
        verify(viewer, never()).sendMessage(anyString());
        viewerTasks.forEach(Runnable::run);
        verify(viewer).sendMessage(contains("disconnected"));
        verify(viewer, never()).openInventory(any(Inventory.class));
    }
}
