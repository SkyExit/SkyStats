package de.laurinhummel.skystats.listeners;

import de.laurinhummel.skystats.menus.StatsInventoryHolder;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class StopStatsInvItemMovementTest {
    @Test
    void blocksClicksAndDragsInStatisticsMenus() {
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(mock(StatsInventoryHolder.class));
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(click.getView()).thenReturn(view);
        when(drag.getView()).thenReturn(view);
        StopStatsInvItemMovement listener = new StopStatsInvItemMovement();
        listener.onInvItemMove(click);
        listener.onInvItemDrag(drag);
        verify(click).setCancelled(true);
        verify(drag).setCancelled(true);
    }

    @Test
    void doesNotBlockOtherInventoriesContainingSkyInTheirTitle() {
        Inventory inventory = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        when(view.getTitle()).thenReturn("SkyBlock shop");
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(click.getView()).thenReturn(view);
        when(drag.getView()).thenReturn(view);
        StopStatsInvItemMovement listener = new StopStatsInvItemMovement();
        listener.onInvItemMove(click);
        listener.onInvItemDrag(drag);
        verify(click, never()).setCancelled(anyBoolean());
        verify(drag, never()).setCancelled(anyBoolean());
    }
}
