package de.skystats.probe;

import de.skystats.SkyStatsPlugin;
import de.skystats.data.StatsService;
import de.skystats.data.StatCatalog;
import de.skystats.model.*;
import de.skystats.platform.TaskBridge;
import de.skystats.ui.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** Runs only in disposable local test servers; never packaged in SkyStats. */
public final class ProbePlugin extends JavaPlugin implements Listener {
    private TaskBridge tasks;
    private Player player;
    private final List<String> passed = new ArrayList<>();
    private long slowestTickNanos;
    private long lastTickNanos;
    private volatile boolean complete;
    private StatsService service;
    private boolean networkPhase;
    private int networkClicks;

    @Override public void onEnable() {
        tasks = new TaskBridge(this);
        try {
            var field = SkyStatsPlugin.class.getDeclaredField("service"); field.setAccessible(true);
            service = (StatsService) field.get(getServer().getPluginManager().getPlugin("SkyStats"));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        getServer().getPluginManager().registerEvents(this, this);
    }
    @EventHandler public void joined(PlayerJoinEvent event) {
        if (!event.getPlayer().getName().equals("StatsTester")) return;
        player = event.getPlayer();
        tasks.entityLater(player, () -> protect(this::start), () -> fail("Player retired"), 5);
        tasks.entityLater(player, this::heartbeat, () -> {}, 1);
    }
    private void heartbeat() {
        long now = System.nanoTime();
        if (lastTickNanos > 0) slowestTickNanos = Math.max(slowestTickNanos, now - lastTickNanos);
        lastTickNanos = now;
        if (!complete) tasks.entityLater(player, this::heartbeat, () -> {}, 1);
    }
    private void start() {
        player.setInvulnerable(true);
        player.setHealth(20.0);
        seedSkin();
        player.setStatistic(Statistic.MINE_BLOCK, Material.STONE, 100);
        player.setStatistic(Statistic.MINE_BLOCK, Material.DIRT, 20);
        player.setStatistic(Statistic.JUMP, 7);
        player.setStatistic(Statistic.PLAY_ONE_MINUTE, (12 * 1440 + 6 * 60 + 14) * 1200);
        player.setStatistic(Statistic.TIME_SINCE_DEATH, 2400);
        player.setStatistic(Statistic.TIME_SINCE_REST, 3600);
        player.setStatistic(Statistic.USE_ITEM, Material.STONE, 12);
        player.setStatistic(Statistic.USE_ITEM, Material.WHEAT_SEEDS, 3);
        player.setStatistic(Statistic.USE_ITEM, Material.DIAMOND_PICKAXE, 9);
        player.setStatistic(Statistic.USE_ITEM, Material.OAK_BOAT, 4);
        player.setStatistic(Statistic.STRIDER_ONE_CM, 2000);
        player.setStatistic(Statistic.AVIATE_ONE_CM, 10000);
        player.setStatistic(Statistic.KILL_ENTITY, org.bukkit.entity.EntityType.ZOMBIE, 2);
        player.performCommand("stats");
        waitFor(() -> menu(Route.Mode.MAIN), () -> {
            check(holder().getInventory().getSize() == 54, "own-54-slot-menu");
            personalNameOnlyInTitle("StatsTester", "own-main");
            scopeBorder(Material.EMERALD, "own-scope");
            check(holder().context().snapshot() == null, "personal-menu-independent-of-server-snapshot");
            check(type(48) == Material.PLAYER_HEAD && textured(48) && name(48).contains("Close"), "textured-red-x-close-button");
            Labels labels = new Labels();
            for (StatKey key : new StatCatalog().keys()) {
                check(Arrays.stream(Category.values()).filter(category -> category.contains(key)).count() == 1, "runtime-category-" + key.technicalName(), false);
                if (key.type().equals("minecraft:custom")) {
                    check(labels.hasLabel(key) && labels.icon(key) != Material.PAPER, "runtime-label-and-icon-" + key.id(), false);
                }
                if (key.type().equals("minecraft:mined")) {
                    check(labels.icon(key) != Material.PAPER, "runtime-block-icon-" + key.id(), false);
                }
            }
            passed.add("all-runtime-statistics-categorized-and-custom-icons-reviewed");
            check(type(MenuLayout.CENTER) == Material.GOLD_BLOCK, "centered-leaderboard-button");
            for (Category category : Category.values()) check(type(MenuLayout.slot(category)) == Material.valueOf(category.icon()), "category-layout-" + category);
            Set<Integer> slots = new HashSet<>();
            for (Category category : Category.values()) slots.add(MenuLayout.slot(category));
            for (int slot : slots) check(slots.contains(slot / 9 * 9 + 8 - slot % 9) && slots.contains((4 - slot / 9) * 9 + slot % 9), "symmetric-menu-" + slot);
            attacks();
            networkAttacks(() -> {
            click(MenuLayout.slot(Category.MINED));
            waitFor(() -> menu(Route.Mode.PERSONAL_CATEGORY) && holder().context().comparison() != null, () -> {
                check(holder().route().category() == Category.MINED, "mined-category-mapping");
                check(type(MenuLayout.contentSlot(0)) == Material.STONE && lore(MenuLayout.contentSlot(0)).contains("100"), "live-values-and-personal-sorting");
                check(type(45) == Material.CHEST && lore(45).contains("Personal total:") && lore(45).contains("Share of server:"), "lazy-category-totals-and-server-share");
                check(type(46) == Material.CLOCK && lore(46).contains("/ hour"), "category-play-time-rates");
                personalNameOnlyInTitle("StatsTester", "own-category");
                click(MenuLayout.contentSlot(0));
                waitFor(() -> menu(Route.Mode.STATISTIC) && holder().context().comparison() != null, () -> {
                check(lore(45).contains("Personal total: 100") && lore(45).contains("Server total: 2,600"), "individual-item-comparison");
                click(48); waitFor(() -> menu(Route.Mode.PERSONAL_CATEGORY), () -> {
                click(48);
                waitFor(() -> menu(Route.Mode.MAIN), this::updatedCategories);
                });
                });
            });
            });
        });
    }
    private void updatedCategories() {
        openCategory(Category.MOVEMENT, () -> {
            check(type(statSlot("minecraft:strider_one_cm")) == Material.STRIDER_SPAWN_EGG, "strider-spawn-egg");
            check(type(statSlot("minecraft:aviate_one_cm")) == Material.ELYTRA, "elytra-icon");
            check(name(statSlot("minecraft:strider_one_cm")).equals("Distance by strider"), "english-custom-label");
            click(48); waitFor(() -> menu(Route.Mode.MAIN), () -> openCategory(Category.PLAY_TIME, () -> {
                check(type(statSlot("minecraft:time_since_death")) == Material.TOTEM_OF_UNDYING, "time-since-death");
                check(type(statSlot("minecraft:time_since_rest")) == Material.RED_BED, "time-since-rest");
                click(48); waitFor(() -> menu(Route.Mode.MAIN), () -> openCategory(Category.PLACED, () -> {
                    check(statSlot("minecraft:stone") >= 0 && statSlot("minecraft:wheat_seeds") >= 0, "placements-and-seeds");
                    check(statSlot("minecraft:diamond_pickaxe") < 0, "tools-excluded-from-placements");
                    var stone = holder().getInventory().getItem(statSlot("minecraft:stone")).getItemMeta();
                    check(!stone.hasDisplayName(), "minecraft-item-name-left-for-client-translation");
                    click(48); waitFor(() -> menu(Route.Mode.MAIN), () -> openCategory(Category.USED, () -> {
                        check(statSlot("minecraft:diamond_pickaxe") >= 0 && statSlot("minecraft:oak_boat") >= 0, "tools-and-entity-items-used");
                        check(statSlot("minecraft:stone") < 0 && statSlot("minecraft:wheat_seeds") < 0, "placements-excluded-from-items-used");
                        click(48); waitFor(() -> menu(Route.Mode.MAIN), this::offline);
                    }));
                }));
            }));
        });
    }
    private void openCategory(Category category, Runnable next) {
        click(MenuLayout.slot(category));
        waitFor(() -> menu(Route.Mode.PERSONAL_CATEGORY) && holder().route().category() == category && holder().context().comparison() != null, next);
    }
    private int statSlot(String id) {
        for (int i = 0; i < holder().rows().size() && i < Page.CONTENT_SIZE; i++) {
            if (holder().rows().get(i) instanceof StatKey key && key.id().equals(id)) return MenuLayout.contentSlot(i);
        }
        return -1;
    }
    private void scopeBorder(Material material, String label) {
        for (int row = 0; row < 5; row++) for (int column : new int[] {0, 8}) {
            check(type(row * 9 + column) == material, label, false);
        }
        passed.add(label);
    }
    private void networkAttacks(Runnable next) {
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        player.getInventory().setItem(9, new ItemStack(Material.COBBLESTONE, 12));
        player.getInventory().setItemInOffHand(new ItemStack(Material.STICK, 3));
        player.setItemOnCursor(new ItemStack(Material.DIRT, 2));
        ItemStack[] ownBefore = player.getInventory().getContents();
        ItemStack cursorBefore = player.getItemOnCursor().clone();
        ItemStack[] guiBefore = holder().getInventory().getContents();
        networkPhase = true;
        String expectedTitle = player.getOpenInventory().getTitle();
        CompletableFuture.runAsync(() -> {
            try { Files.writeString(Path.of("network-ready.txt"), expectedTitle); }
            catch (Exception error) { fail(error.toString()); }
        });
        waitFor(() -> networkClicks >= 10, () -> tasks.entityLater(player, () -> protect(() -> {
            networkPhase = false;
            check(Arrays.equals(ownBefore, player.getInventory().getContents()), "network-attacks-player-inventory-unchanged");
            check(cursorBefore.equals(player.getItemOnCursor()), "network-attacks-cursor-unchanged");
            check(Arrays.equals(guiBefore, holder().getInventory().getContents()), "network-attacks-gui-unchanged");
            next.run();
        }), () -> fail("Player retired"), 5));
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void networkClick(InventoryClickEvent event) {
        if (!networkPhase || !(event.getView().getTopInventory().getHolder() instanceof StatsHolder)) return;
        if (!event.isCancelled()) fail("Network inventory click was not cancelled");
        networkClicks++;
    }
    private void offline() {
        player.performCommand("stats OfflineA");
        waitFor(() -> menu(Route.Mode.MAIN) && holder().context().target().name().equals("OfflineA"), () -> {
            passed.add("offline-player-lookup");
            personalNameOnlyInTitle("OfflineA", "offline-main");
            scopeBorder(Material.DIAMOND, "other-player-scope");
            click(MenuLayout.slot(Category.MINED));
            waitFor(() -> menu(Route.Mode.PERSONAL_CATEGORY) && holder().context().comparison() != null, () -> {
                check(type(MenuLayout.contentSlot(0)) == Material.STONE && lore(MenuLayout.contentSlot(0)).contains("1,000"), "offline-file-value");
                personalNameOnlyInTitle("OfflineA", "offline-category");
                check(type(53) == Material.PLAYER_HEAD, "next-page-player-head");
                check(textured(53), "next-head-texture");
                click(53);
                waitFor(() -> menu(Route.Mode.PERSONAL_CATEGORY) && holder().route().page() == 1, () -> {
                    check(type(47) == Material.PLAYER_HEAD && textured(47), "previous-page-textured-head");
                    check(lore(45).contains("Personal total: 1,050"), "category-total-includes-every-page");
                    check(name(49).contains("2 / 2"), "page-counter");
                    personalNameOnlyInTitle("OfflineA", "offline-category-second-page");
                    click(47);
                    waitFor(() -> menu(Route.Mode.PERSONAL_CATEGORY) && holder().route().page() == 0, () -> {
                    click(48); waitFor(() -> menu(Route.Mode.MAIN), this::offlineDetails);
                    });
                });
            });
        });
    }
    private void offlineDetails() {
        openCategory(Category.PLAY_TIME, () -> {
            check(lore(statSlot("minecraft:time_since_rest")).contains("0d 0h 0m 0s"), "zero-rest-timer-visible");
            check(lore(statSlot("minecraft:time_since_death")).contains("0d 0h 0m 0s"), "zero-death-timer-visible");
            click(48); waitFor(() -> menu(Route.Mode.MAIN), () -> openCategory(Category.KILLED, () -> {
                check(statSlot("minecraft:player_kills") >= 0 && statSlot("minecraft:zombie") >= 0, "player-kills-and-mob-kills");
                check(lore(45).contains("Personal total: 104"), "player-kills-counted-once");
                click(48); waitFor(() -> menu(Route.Mode.MAIN), () -> openCategory(Category.KILLED_BY, () -> {
                    check(statSlot("minecraft:player") >= 0 && statSlot("minecraft:zombie") >= 0, "player-and-mob-caused-deaths");
                    check(lore(statSlot("skystats:unknown")).contains("9") && lore(statSlot("skystats:unknown")).contains("falls, void"), "unknown-environmental-death-residual");
                    check(lore(45).contains("Personal total: 20"), "death-category-total");
                    click(48); waitFor(() -> menu(Route.Mode.MAIN), this::server);
                }));
            }));
        });
    }
    private void server() {
        click(MenuLayout.CENTER);
        waitFor(() -> menu(Route.Mode.SERVER), () -> {
            scopeBorder(Material.GOLD_INGOT, "server-scope");
            check(!player.getOpenInventory().getTitle().contains("OfflineA"), "server-title-independent-of-personal-target");
            check(holder().context().snapshot().total(new StatKey("minecraft:killed_by", "minecraft:player")) >= 9, "server-player-death-rankings");
            check(holder().context().snapshot().total(PlayerStats.PLAYER_KILLS) >= 99, "server-player-kill-rankings");
            check(holder().context().snapshot().ranking(new StatKey("minecraft:killed", "minecraft:player")).isEmpty(), "no-duplicate-player-kill-competition");
            click(50);
            waitFor(() -> menu(Route.Mode.SERVER), () -> {
                service.snapshot(true).whenComplete((snapshot, error) -> tasks.entity(player, () -> protect(() -> {
                    if (error != null) throw new IllegalStateException(error);
                    click(MenuLayout.slot(Category.MINED));
                    waitFor(() -> menu(Route.Mode.SERVER_CATEGORY), this::leaderboard);
                }), () -> fail("Player retired")));
            });
        });
    }
    private void leaderboard() {
        check(type(MenuLayout.contentSlot(0)) == Material.STONE, "server-total-sorting");
        String lore = lore(MenuLayout.contentSlot(0));
        check(lore.contains("2,600"), "online-file-replaced-with-live-value-once");
        check(lore.contains("OfflineA") && lore.contains("OfflineB") && lore.contains("OfflineC"), "offline-top-three");
        check(lore.contains("1. OfflineA") && lore.contains("1. OfflineB") && lore.contains("3. OfflineC"), "competition-ranks-1-1-3");
        check(lore.contains("4. StatsTester") && lore.contains("100"), "viewer-position-outside-top-three");
        click(MenuLayout.contentSlot(0));
        waitFor(() -> menu(Route.Mode.RANKING) && textured(MenuLayout.contentSlot(3)), () -> {
            check(name(MenuLayout.contentSlot(0)).contains("OfflineA") && name(MenuLayout.contentSlot(1)).contains("OfflineB"), "detail-player-ranking");
            check(type(MenuLayout.contentSlot(0)) == Material.PLAYER_HEAD, "ranking-player-heads");
            check(textured(MenuLayout.contentSlot(3)), "online-player-skin-retained-in-ranking");
            Route origin = holder().route();
            click(MenuLayout.contentSlot(0));
            waitFor(() -> menu(Route.Mode.MAIN) && holder().context().target().name().equals("OfflineA"), () -> {
            check(holder().context().returnTo() != null, "leaderboard-click-opens-personal-player-profile");
            click(48); waitFor(() -> menu(Route.Mode.RANKING), () -> {
            check(holder().route().equals(origin), "player-profile-back-to-same-leaderboard");
            click(48);
            waitFor(() -> menu(Route.Mode.SERVER_CATEGORY), () -> {
                click(48); waitFor(() -> menu(Route.Mode.SERVER), () -> {
                    click(MenuLayout.CENTER); waitFor(() -> menu(Route.Mode.MEDALS), this::medals);
                });
            });
            });
            });
        });
    }
    private void seedSkin() {
        try (HeadFactory heads = new HeadFactory(getLogger())) {
            Object arrow = SkullMeta.class.getMethod("getPlayerProfile").invoke(heads.arrow(true).getItemMeta());
            var getter = Player.class.getMethod("getPlayerProfile");
            Object profile = getter.invoke(player);
            Class<?> api = getter.getReturnType();
            try {
                var textures = api.getMethod("getTextures");
                api.getMethod("setTextures", textures.getReturnType()).invoke(profile, textures.invoke(arrow));
            } catch (NoSuchMethodException ignored) {
                api.getMethod("setProperties", Collection.class).invoke(profile, api.getMethod("getProperties").invoke(arrow));
            }
            Player.class.getMethod("setPlayerProfile", api).invoke(player, profile);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private void medals() {
        check(type(MenuLayout.contentSlot(0)) == Material.PLAYER_HEAD && lore(MenuLayout.contentSlot(0)).contains("Total:"), "medal-counts-and-player-heads");
        check(type(53) == Material.PLAYER_HEAD, "medal-ranking-pagination");
        click(53);
        waitFor(() -> menu(Route.Mode.MEDALS) && holder().route().page() == 1, () -> {
            Route origin = holder().route();
            UUID subject = ((Snapshot.Medals) holder().rows().get(Page.CONTENT_SIZE)).uuid();
            click(MenuLayout.contentSlot(0));
            waitFor(() -> menu(Route.Mode.PROFILE) && subject.equals(holder().route().subject()), () -> {
                check(lore(MenuLayout.contentSlot(0)).contains("minecraft:"), "medal-profile-individual-statistics");
                check(holder().rows().get(0) instanceof MedalAward award && award.placement().uuid().equals(subject), "medal-profile-selected-player-awards");
                personalNameOnlyInTitle(holder().context().snapshot().players().get(subject).name(), "medal-profile");
                check(holder().action(52) != null, "medal-tier-jump-switch");
                click(52);
                waitFor(() -> menu(Route.Mode.PROFILE) && holder().route().medalTier() > 0, () -> {
                    click(48);
                    waitFor(() -> menu(Route.Mode.MEDALS), () -> {
                        check(holder().route().equals(origin), "medal-player-profile-back-to-same-page");
                        closedLoadingMenu();
                    });
                });
            });
        });
    }
    private void closedLoadingMenu() {
        player.performCommand("stats");
        waitFor(() -> player.getOpenInventory().getTopInventory().getHolder() instanceof StatsHolder loading && loading.context() == null, () -> {
            player.closeInventory();
            tasks.entityLater(player, () -> protect(() -> {
                check(!(player.getOpenInventory().getTopInventory().getHolder() instanceof StatsHolder), "closed-menu-not-reopened-by-delayed-load");
                player.performCommand("stats NoSuchPlayer123");
                waitFor(() -> player.getOpenInventory().getTopInventory().getItem(22) != null && type(22) == Material.BARRIER, () -> {
                    passed.add("unknown-player-message");
                    check(service.names().complete("Off").contains("OfflineA"), "offline-tab-completion");
                    finish();
                });
            }), () -> fail("Player retired"), 80);
        });
    }
    private void personalNameOnlyInTitle(String target, String label) {
        check(player.getOpenInventory().getTitle().contains(target), label + "-name-in-title");
        for (ItemStack item : holder().getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            var meta = item.getItemMeta();
            check(!meta.hasDisplayName() || !ChatColor.stripColor(meta.getDisplayName()).contains(target), label + "-no-name-in-item", false);
            check(!meta.hasLore() || meta.getLore().stream().noneMatch(line -> ChatColor.stripColor(line).contains(target)), label + "-no-name-in-lore", false);
        }
        passed.add(label + "-name-only-in-title");
    }
    private void attacks() {
        InventoryView view = player.getOpenInventory();
        ItemStack[] before = view.getTopInventory().getContents();
        int count = 0;
        for (ClickType click : ClickType.values()) for (int slot : new int[] {0, 13, 45, 49, 53, 54, 60, -999}) {
            if (click == ClickType.LEFT || click == ClickType.RIGHT) continue;
            InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, slot, click, InventoryAction.UNKNOWN, click == ClickType.NUMBER_KEY ? 0 : -1);
            getServer().getPluginManager().callEvent(event);
            check(event.isCancelled(), "cancel-click-" + click + "-" + slot, false); count++;
        }
        for (InventoryAction action : InventoryAction.values()) {
            InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 60, ClickType.SHIFT_LEFT, action);
            getServer().getPluginManager().callEvent(event);
            check(event.isCancelled(), "cancel-action-" + action, false); count++;
        }
        InventoryDragEvent drag = new InventoryDragEvent(view, new ItemStack(Material.DIRT), new ItemStack(Material.DIRT, 2), false, Map.of(0, new ItemStack(Material.DIRT), 60, new ItemStack(Material.DIRT)));
        getServer().getPluginManager().callEvent(drag); check(drag.isCancelled(), "cancel-drag");
        InventoryCreativeEvent creative = new InventoryCreativeEvent(view, InventoryType.SlotType.CONTAINER, 0, new ItemStack(Material.DIAMOND_BLOCK));
        getServer().getPluginManager().callEvent(creative); check(creative.isCancelled(), "cancel-creative");
        check(Arrays.equals(before, view.getTopInventory().getContents()), "attack-content-unchanged");
        passed.add("cancelled-click-and-action-combinations=" + count);
    }
    private boolean textured(int slot) {
        SkullMeta meta = (SkullMeta) holder().getInventory().getItem(slot).getItemMeta();
        try {
            try {
                Object profile = SkullMeta.class.getMethod("getOwnerProfile").invoke(meta);
                if (profile == null) return false;
                Object textures = Class.forName("org.bukkit.profile.PlayerProfile").getMethod("getTextures").invoke(profile);
                return Class.forName("org.bukkit.profile.PlayerTextures").getMethod("getSkin").invoke(textures) != null;
            } catch (NoSuchMethodException ignored) {
                Object profile = SkullMeta.class.getMethod("getPlayerProfile").invoke(meta);
                if (profile == null) return false;
                var properties = (Collection<?>) Class.forName("com.destroystokyo.paper.profile.PlayerProfile").getMethod("getProperties").invoke(profile);
                return !properties.isEmpty();
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private void click(int slot) {
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER, slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) throw new AssertionError("Navigation click was not cancelled");
    }
    private boolean menu(Route.Mode mode) { return player.getOpenInventory().getTopInventory().getHolder() instanceof StatsHolder current && current.context() != null && current.route().mode() == mode && current.getInventory().getItem(50) != null && current.getInventory().getItem(50).getType() == Material.SUNFLOWER; }
    private StatsHolder holder() { return (StatsHolder) player.getOpenInventory().getTopInventory().getHolder(); }
    private Material type(int slot) { ItemStack item = player.getOpenInventory().getTopInventory().getItem(slot); return item == null ? Material.AIR : item.getType(); }
    private String lore(int slot) { var meta = player.getOpenInventory().getTopInventory().getItem(slot).getItemMeta(); return ChatColor.stripColor(String.join("\n", meta.getLore() == null ? List.of() : meta.getLore())); }
    private String name(int slot) { return ChatColor.stripColor(player.getOpenInventory().getTopInventory().getItem(slot).getItemMeta().getDisplayName()); }
    private void check(boolean condition, String label) { check(condition, label, true); }
    private void check(boolean condition, String label, boolean record) { if (!condition) throw new AssertionError(label); if (record) passed.add(label); }
    private void waitFor(BooleanSupplier ready, Runnable action) { waitFor(ready, action, 800); }
    private void waitFor(BooleanSupplier ready, Runnable action, int attempts) {
        tasks.entityLater(player, () -> protect(() -> {
            if (ready.getAsBoolean()) action.run();
            else if (attempts <= 0) throw new AssertionError("Timed out waiting for menu; current title=" + player.getOpenInventory().getTitle());
            else waitFor(ready, action, attempts - 1);
        }), () -> fail("Player retired"), 1);
    }
    private void protect(Runnable runnable) { try { if (!complete) runnable.run(); } catch (Throwable error) { error.printStackTrace(); fail(error.toString()); } }
    private void finish() {
        complete = true;
        List<String> lines = new ArrayList<>(List.of("PASS", "server=" + Bukkit.getVersion(), "slowest-entity-tick-interval-ms=" + slowestTickNanos / 1_000_000));
        lines.addAll(passed); write(lines);
    }
    private void fail(String message) { if (complete) return; complete = true; List<String> lines = new ArrayList<>(List.of("FAIL", message)); lines.addAll(passed); write(lines); }
    private void write(List<String> lines) {
        CompletableFuture.runAsync(() -> { try { Files.write(Path.of("probe-result.txt"), lines); } catch (Exception error) { getLogger().severe(error.toString()); } });
    }
}
