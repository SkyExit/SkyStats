package de.skystats.ui;

import de.skystats.data.*;
import de.skystats.model.*;
import de.skystats.platform.TaskBridge;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MenuControllerTest {
    private static final StatKey STONE = new StatKey("minecraft:mined", "minecraft:stone");
    private final StatsService service = mock(StatsService.class);
    private final TaskBridge tasks = mock(TaskBridge.class);
    private final Map<UUID, PlayerStats> profiles = new HashMap<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedConstruction<HeadFactory> headFactory;
    private MenuController menus;

    @BeforeEach void setup() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(mock(ItemFactory.class));
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), eq(54), anyString())).thenAnswer(call -> {
            Inventory inventory = mock(Inventory.class);
            when(inventory.getHolder()).thenReturn(call.getArgument(0));
            return inventory;
        });
        headFactory = mockConstruction(HeadFactory.class, (head, context) -> {
            when(head.arrow(anyBoolean())).thenAnswer(call -> new ItemStack(Material.PLAYER_HEAD));
            when(head.closeButton()).thenAnswer(call -> new ItemStack(Material.PLAYER_HEAD));
            when(head.player(any(), any())).thenAnswer(call -> CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD)));
        });
        doAnswer(call -> { ((Runnable) call.getArgument(1)).run(); return null; }).when(tasks).entity(any(), any(), any());
        when(service.compute(any())).thenAnswer(call -> CompletableFuture.completedFuture(((Supplier<?>) call.getArgument(0)).get()));
        when(service.personal(any())).thenAnswer(call -> CompletableFuture.completedFuture(profiles.get(call.getArgument(0))));
        for (int i = 0; i < 45; i++) {
            UUID id = UUID.randomUUID(); profiles.put(id, new PlayerStats(id, "P" + i, Map.of(STONE, 100L - i)));
        }
        useSnapshot(Snapshot.aggregate(profiles, List.of()));
        Plugin plugin = mock(Plugin.class); when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        menus = new MenuController(plugin, tasks, service, new StatCatalog(), true, 1);
    }
    @AfterEach void close() { headFactory.close(); bukkit.close(); }

    private void useSnapshot(Snapshot snapshot) { when(service.snapshot(anyBoolean())).thenReturn(CompletableFuture.completedFuture(snapshot)); }
    private Player viewer() {
        Player player = mock(Player.class); UUID id = UUID.randomUUID();
        profiles.put(id, new PlayerStats(id, "Viewer", Map.of(STONE, 5L)));
        when(player.getUniqueId()).thenReturn(id); when(player.getName()).thenReturn("Viewer");
        when(player.isOnline()).thenReturn(true); when(player.hasPermission("stats.others")).thenReturn(true);
        AtomicReference<Inventory> current = new AtomicReference<>(mock(Inventory.class));
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenAnswer(call -> current.get());
        when(player.getOpenInventory()).thenReturn(view);
        doAnswer(call -> { current.set(call.getArgument(0)); return view; }).when(player).openInventory(any(Inventory.class));
        doAnswer(call -> { current.set(mock(Inventory.class)); return null; }).when(player).closeInventory();
        return player;
    }
    private StatsHolder holder(Player player) { return (StatsHolder) player.getOpenInventory().getTopInventory().getHolder(); }
    private void click(Player player, int slot) { menus.click(player, holder(player), slot); }
    private void ranking(Player player) {
        menus.command(player, null); click(player, MenuLayout.CENTER);
        click(player, MenuLayout.slot(Category.MINED)); click(player, MenuLayout.contentSlot(0));
        assertEquals(Route.Mode.RANKING, holder(player).route().mode());
    }

    @Test void twoViewersReturnToTheirOwnLeaderboardAndPageAfterProfileNavigationAndRefresh() {
        Player first = viewer(), second = viewer(); ranking(first); ranking(second);
        click(first, 53);
        Route firstOrigin = holder(first).route(), secondOrigin = holder(second).route();
        UUID firstTarget = ((Snapshot.RankedPlayer) holder(first).rows().get(35)).uuid();
        UUID secondTarget = ((Snapshot.RankedPlayer) holder(second).rows().get(0)).uuid();
        click(first, MenuLayout.contentSlot(0)); click(second, MenuLayout.contentSlot(0));
        assertEquals(firstTarget, holder(first).context().target().uuid());
        assertEquals(secondTarget, holder(second).context().target().uuid());
        assertEquals(Route.Mode.MAIN, holder(first).route().mode());
        click(first, MenuLayout.slot(Category.MINED)); click(first, 48); click(first, 50);
        assertEquals(firstTarget, holder(first).context().target().uuid());
        click(first, 48); click(second, 48);
        assertEquals(firstOrigin, holder(first).route());
        assertEquals(secondOrigin, holder(second).route());
        assertEquals(1, holder(first).route().page()); assertEquals(0, holder(second).route().page());
    }
    @Test void profileClicksRespectOtherPlayerPermissionAndLateLoadsDoNotReopenClosedMenus() {
        Player player = viewer(); ranking(player);
        StatsHolder ranking = holder(player);
        when(player.hasPermission("stats.others")).thenReturn(false);
        click(player, MenuLayout.contentSlot(0)); assertSame(ranking, holder(player));
        verify(player).sendMessage("§cYou do not have permission to view other players' statistics.");
        when(player.hasPermission("stats.others")).thenReturn(true);
        Snapshot.RankedPlayer row = (Snapshot.RankedPlayer) ranking.rows().get(0);
        CompletableFuture<PlayerStats> pending = new CompletableFuture<>(); when(service.personal(row.uuid())).thenReturn(pending);
        click(player, MenuLayout.contentSlot(0));
        StatsHolder loading = holder(player); menus.closed(player, loading); player.closeInventory();
        pending.complete(profiles.get(row.uuid()));
        assertNull(holder(player));
    }
    @Test void categoryAndItemComparisonsAreLazyAndRetainTheirBackRoute() {
        Player player = viewer(); menus.command(player, null);
        verify(service, never()).snapshot(anyBoolean());
        assertNull(holder(player).context().comparison());
        click(player, MenuLayout.slot(Category.MINED));
        assertEquals(5, holder(player).context().comparison().personal());
        assertEquals(3515, holder(player).context().comparison().server());
        Route category = holder(player).route();
        click(player, MenuLayout.contentSlot(0));
        assertEquals(Route.Mode.STATISTIC, holder(player).route().mode());
        assertEquals(STONE, holder(player).context().comparison().unit());
        click(player, 48); assertEquals(category, holder(player).route());
    }
    @Test void medalSwitchCyclesAllTiersEvenWhenTheirFirstAwardsShareOnePage() {
        Player player = viewer(); UUID target = player.getUniqueId(), b = UUID.randomUUID(), c = UUID.randomUUID();
        StatKey gold = new StatKey("minecraft:mined", "minecraft:gold_ore"), silver = new StatKey("minecraft:mined", "minecraft:iron_ore"), bronze = new StatKey("minecraft:mined", "minecraft:copper_ore");
        profiles.put(target, new PlayerStats(target, "Viewer", Map.of(gold, 10L, silver, 5L, bronze, 1L)));
        useSnapshot(Snapshot.aggregate(Map.of(target, profiles.get(target), b, new PlayerStats(b, "B", Map.of(silver, 6L, bronze, 3L)),
                c, new PlayerStats(c, "C", Map.of(bronze, 2L))), List.of()));
        menus.command(player, null); click(player, 52);
        assertEquals(Route.Mode.PROFILE, holder(player).route().mode());
        for (int rank : new int[] {1, 2, 3, 1}) {
            click(player, 52); assertEquals(rank, holder(player).route().medalTier());
            assertEquals(0, holder(player).route().page());
        }
    }
    @Test void medalLeaderboardOpensIndividualAwardsAndReturnsToTheSamePage() {
        StatKey diamond = new StatKey("minecraft:mined", "minecraft:diamond_ore");
        profiles.replaceAll((uuid, stats) -> new PlayerStats(uuid, stats.name(), Map.of(STONE, stats.value(STONE), diamond, 10L)));
        useSnapshot(Snapshot.aggregate(profiles, List.of()));
        Player player = viewer(); menus.command(player, null); click(player, MenuLayout.CENTER); click(player, MenuLayout.CENTER);
        click(player, 53);
        StatsHolder ranking = holder(player);
        assertEquals(1, ranking.route().page());
        Snapshot.Medals row = (Snapshot.Medals) ranking.rows().get(Page.CONTENT_SIZE);
        int firstSlot = MenuLayout.contentSlot(0);

        when(player.hasPermission("stats.others")).thenReturn(false);
        click(player, firstSlot);
        assertSame(ranking, holder(player));
        when(player.hasPermission("stats.others")).thenReturn(true);

        click(player, firstSlot);
        StatsHolder profile = holder(player);
        assertEquals(Route.Mode.PROFILE, profile.route().mode());
        assertEquals(row.uuid(), profile.route().subject());
        assertEquals(List.of(new MedalAward(diamond, new Snapshot.RankedPlayer(row.uuid(), row.name(), 10L, 1))), profile.rows());
        verify(service, never()).personal(row.uuid());

        click(player, 48);
        assertEquals(ranking.route(), holder(player).route());
    }
    @Test void personalCategoriesStayUsableWhileServerComparisonIsPendingOrFails() {
        Player player = viewer(); CompletableFuture<Snapshot> pending = new CompletableFuture<>();
        when(service.snapshot(anyBoolean())).thenReturn(pending);
        menus.command(player, null); click(player, MenuLayout.slot(Category.MINED));
        assertEquals(Route.Mode.PERSONAL_CATEGORY, holder(player).route().mode());
        assertEquals(List.of(STONE), holder(player).rows());
        assertNull(holder(player).context().comparison());
        click(player, MenuLayout.contentSlot(0));
        assertEquals(Route.Mode.STATISTIC, holder(player).route().mode());
        StatsHolder details = holder(player);
        pending.completeExceptionally(new IllegalStateException("Server unavailable"));
        assertSame(details, holder(player));
        click(player, 48); assertEquals(Route.Mode.PERSONAL_CATEGORY, holder(player).route().mode());
    }
}
