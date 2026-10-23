package de.laurinhummel.skystats.scheduling;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PlayerSchedulerTest {
    private Plugin plugin;
    private Player player;

    @BeforeEach
    void setup() {
        plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
    }

    @Test
    void spigotSchedulesBeforeAccessingPlayer() {
        Server server = mock(Server.class);
        BukkitScheduler bukkit = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(bukkit);
        AtomicReference<Runnable> queued = new AtomicReference<>();
        when(bukkit.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
            queued.set(call.getArgument(1));
            return null;
        });
        PlayerScheduler scheduler = new PlayerScheduler(plugin);
        var result = scheduler.supply(player, () -> 42);
        assertFalse(result.isDone());
        verify(player, never()).isOnline();
        queued.get().run();
        assertEquals(42, result.join());
    }

    @Test
    void foliaUsesEntitySchedulerWithoutBukkitFallback() {
        ScheduledPlayer foliaPlayer = mock(ScheduledPlayer.class);
        when(foliaPlayer.isOnline()).thenReturn(true);
        EntityScheduler entity = mock(EntityScheduler.class);
        when(foliaPlayer.getScheduler()).thenReturn(entity);
        AtomicReference<Runnable> queued = new AtomicReference<>();
        when(entity.execute(eq(plugin), any(), any(), eq(1L))).thenAnswer(call -> {
            queued.set(call.getArgument(1));
            return true;
        });
        PlayerScheduler scheduler = new PlayerScheduler(plugin, ScheduledPlayer.class);
        var result = scheduler.supply(foliaPlayer, () -> 42);
        assertFalse(result.isDone());
        verify(foliaPlayer, never()).isOnline();
        queued.get().run();
        assertEquals(42, result.join());
        verify(plugin, never()).getServer();
    }

    @Test
    void retiredSchedulerCompletesRequestInsteadOfHanging() {
        ScheduledPlayer foliaPlayer = mock(ScheduledPlayer.class);
        EntityScheduler entity = mock(EntityScheduler.class);
        when(foliaPlayer.getScheduler()).thenReturn(entity);
        PlayerScheduler scheduler = new PlayerScheduler(plugin, ScheduledPlayer.class);
        var result = scheduler.supply(foliaPlayer, () -> fail("Retired player accessed"));
        assertInstanceOf(PlayerScheduler.PlayerUnavailableException.class,
                assertThrows(CompletionException.class, result::join).getCause());
        verify(plugin, never()).getServer();
    }

    @Test
    void disconnectBetweenSchedulingAndExecutionCompletesRequest() {
        ScheduledPlayer foliaPlayer = mock(ScheduledPlayer.class);
        EntityScheduler entity = mock(EntityScheduler.class);
        when(foliaPlayer.getScheduler()).thenReturn(entity);
        AtomicReference<Runnable> retired = new AtomicReference<>();
        when(entity.execute(any(), any(), any(), anyLong())).thenAnswer(call -> {
            retired.set(call.getArgument(2));
            return true;
        });
        var result = new PlayerScheduler(plugin, ScheduledPlayer.class)
                .supply(foliaPlayer, () -> fail("Disconnected player accessed"));
        retired.get().run();
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    void shutdownPreventsPendingAndNewActions() {
        ScheduledPlayer foliaPlayer = mock(ScheduledPlayer.class);
        EntityScheduler entity = mock(EntityScheduler.class);
        when(foliaPlayer.getScheduler()).thenReturn(entity);
        AtomicReference<Runnable> queued = new AtomicReference<>();
        when(entity.execute(any(), any(), any(), anyLong())).thenAnswer(call -> {
            queued.set(call.getArgument(1));
            return true;
        });
        AtomicBoolean ran = new AtomicBoolean();
        PlayerScheduler scheduler = new PlayerScheduler(plugin, ScheduledPlayer.class);
        var pending = scheduler.run(foliaPlayer, () -> ran.set(true));
        scheduler.close();
        queued.get().run();
        assertFalse(ran.get());
        assertTrue(pending.isCompletedExceptionally());
        assertTrue(scheduler.run(foliaPlayer, () -> fail("Disabled scheduler ran")).isCompletedExceptionally());
    }

    public interface ScheduledPlayer extends Player { EntityScheduler getScheduler(); }
    public interface EntityScheduler {
        boolean execute(Plugin plugin, Runnable action, Runnable retired, long delay);
    }
}
