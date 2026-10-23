package de.skystats.ui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;
import static org.mockito.Mockito.*;

class InventoryGuardTest {
    private final MenuController menus = mock(MenuController.class);
    private final InventoryGuard guard = new InventoryGuard(menus);
    private final Player player = mock(Player.class);
    private final StatsHolder holder = new StatsHolder(UUID.randomUUID(), Route.main(), null, List.of());
    private final Inventory top = mock(Inventory.class);
    private final InventoryView view = mock(InventoryView.class);
    private InventoryClickEvent click(ClickType click, int rawSlot, InventoryAction action) {
        when(top.getHolder()).thenReturn(holder); when(view.getTopInventory()).thenReturn(top);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view); when(event.getWhoClicked()).thenReturn(player);
        when(event.getClick()).thenReturn(click); when(event.getRawSlot()).thenReturn(rawSlot); when(event.getAction()).thenReturn(action);
        return event;
    }
    @ParameterizedTest @EnumSource(ClickType.class) void cancelsEveryClickType(ClickType type) {
        InventoryClickEvent event = click(type, 0, InventoryAction.UNKNOWN);
        guard.click(event); verify(event).setCancelled(true);
        if (type == ClickType.LEFT || type == ClickType.RIGHT) verify(menus).click(player, holder, 0); else verifyNoInteractions(menus);
    }
    @ParameterizedTest @EnumSource(InventoryAction.class) void cancelsEveryActionFromLowerInventory(InventoryAction action) {
        InventoryClickEvent event = click(ClickType.SHIFT_LEFT, 60, action);
        guard.click(event); verify(event).setCancelled(true); verifyNoInteractions(menus);
    }
    @Test void cancelsDragAndCreativeActions() {
        when(top.getHolder()).thenReturn(holder); when(view.getTopInventory()).thenReturn(top);
        InventoryDragEvent drag = mock(InventoryDragEvent.class); when(drag.getView()).thenReturn(view);
        guard.drag(drag); verify(drag).setCancelled(true);
        InventoryCreativeEvent creative = mock(InventoryCreativeEvent.class); when(creative.getView()).thenReturn(view);
        guard.creative(creative); verify(creative).setCancelled(true);
    }
    @Test void leavesUnrelatedInventoriesUntouched() {
        InventoryClickEvent event = click(ClickType.LEFT, 0, InventoryAction.PICKUP_ALL);
        when(top.getHolder()).thenReturn(null); guard.click(event); verify(event, never()).setCancelled(anyBoolean()); verifyNoInteractions(menus);
    }
}
