package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.menus.StatsMenuService;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommandsTest {
    private Plugin plugin;
    private PlayerScheduler scheduler;
    private StatsMenuService menus;
    private Player player;
    private Command command;

    @BeforeEach
    void setup() {
        plugin = mock(Plugin.class);
        scheduler = mock(PlayerScheduler.class);
        menus = mock(StatsMenuService.class);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.hasPermission(anyString())).thenReturn(true);
        command = mock(Command.class);
        when(scheduler.run(eq(player), any())).thenAnswer(call -> {
            ((Runnable) call.getArgument(1)).run();
            return CompletableFuture.completedFuture(null);
        });
    }

    @Test
    void statsWithoutArgumentsShowsSelf() {
        assertTrue(new StatsCommand(plugin, scheduler, menus).onCommand(player, command, "stats", new String[0]));
        verify(menus).openStats(player, player);
    }

    @Test
    void unknownPlayersNeverFallBackToSelf() {
        try (var bukkit = mockStatic(Bukkit.class)) {
            new StatsCommand(plugin, scheduler, menus).onCommand(player, command, "stats", new String[]{"missing"});
            new CompareCommand(plugin, scheduler, menus).onCommand(player, command, "compare", new String[]{"missing", "alsoMissing"});
            verifyNoInteractions(menus);
            verify(player, times(2)).sendMessage(contains("not online"));
        }
    }

    @Test
    void invalidArgumentCountsAreHandled() {
        new StatsCommand(plugin, scheduler, menus).onCommand(player, command, "stats", new String[]{"a", "b"});
        new CompareCommand(plugin, scheduler, menus).onCommand(player, command, "compare", new String[]{"a"});
        new LevelCommand(plugin, scheduler).onCommand(player, command, "level", new String[]{"a"});
        verifyNoInteractions(menus);
        verify(player, times(3)).sendMessage(contains("Usage:"));
    }

    @Test
    void permissionsAreEnforcedIncludingOtherPlayers() {
        when(player.hasPermission("skystats.stats")).thenReturn(false);
        new StatsCommand(plugin, scheduler, menus).onCommand(player, command, "stats", new String[0]);
        verifyNoInteractions(menus);
        when(player.hasPermission("skystats.stats")).thenReturn(true);
        when(player.hasPermission("skystats.stats.others")).thenReturn(false);
        Player other = mock(Player.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayerExact("Other")).thenReturn(other);
            new StatsCommand(plugin, scheduler, menus).onCommand(player, command, "stats", new String[]{"Other"});
        }
        verifyNoInteractions(menus);
        verify(player, times(2)).sendMessage(contains("permission"));
    }

    @Test
    void consoleGetsFeedbackWithoutPlayerCast() {
        ConsoleCommandSender console = mock(ConsoleCommandSender.class);
        new LevelCommand(plugin, scheduler).onCommand(console, command, "level", new String[0]);
        verify(console).sendMessage(contains("console"));
        verifyNoInteractions(scheduler);
    }

    @Test
    void commandWaitsForTheViewerScheduler() {
        AtomicReference<Runnable> queued = new AtomicReference<>();
        when(scheduler.run(eq(player), any())).thenAnswer(call -> {
            queued.set(call.getArgument(1));
            return new CompletableFuture<Void>();
        });
        new StatsCommand(plugin, scheduler, menus).onCommand(player, command, "stats", new String[0]);
        verifyNoInteractions(menus);
        queued.get().run();
        verify(menus).openStats(player, player);
    }
}
