package de.skystats.ui;

import de.skystats.data.*;
import de.skystats.model.*;
import de.skystats.platform.TaskBridge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

public final class MenuController {
    private record Prepared(StatsHolder.Context context, List<?> rows) {}
    private final Plugin plugin;
    private final TaskBridge tasks;
    private final StatsService service;
    private final StatCatalog catalog;
    private final Labels labels;
    private final HeadFactory heads;
    private final boolean hideZeros;
    private final long refreshCooldownNanos;
    private final Map<UUID, StatsHolder> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> refreshTimes = new ConcurrentHashMap<>();

    public MenuController(Plugin plugin, TaskBridge tasks, StatsService service, StatCatalog catalog, boolean hideZeros, int cooldown) {
        this.plugin = plugin; this.tasks = tasks; this.service = service; this.catalog = catalog;
        this.hideZeros = hideZeros; this.labels = new Labels(); this.heads = new HeadFactory(plugin.getLogger());
        this.refreshCooldownNanos = TimeUnit.SECONDS.toNanos(cooldown);
    }

    public void command(Player viewer, String targetName) {
        tasks.entity(viewer, () -> {
            heads.remember(viewer);
            StatsHolder loading = loading(viewer, Route.main(), null);
            CompletableFuture<UUID> target = targetName == null ? CompletableFuture.completedFuture(viewer.getUniqueId())
                    : resolveTarget(targetName);
            target.thenCompose(service::personal).thenApply(stats -> {
                    if (stats == null) throw new CompletionException(new IllegalArgumentException("No statistics available yet."));
                    return new StatsHolder.Context(null, stats);
            }).whenComplete((context, error) -> tasks.entity(viewer, () -> {
                if (!current(viewer, loading)) return;
                if (error != null) failure(viewer, loading, error);
                else render(viewer, Route.main(), context, List.of());
            }, () -> {}));
        }, () -> {});
    }

    private CompletableFuture<UUID> resolveTarget(String name) {
        List<UUID> known = service.names().find(name);
        // Only an unknown name during the initial name-index load needs the server scan.
        return (known.isEmpty() ? service.snapshot(false).thenApply(snapshot -> service.names().find(name))
                : CompletableFuture.completedFuture(known)).thenApply(found -> {
            if (found.isEmpty()) throw new IllegalArgumentException("Unknown player or no saved statistics available yet.");
            if (found.size() > 1) throw new IllegalArgumentException("This name belongs to multiple UUIDs. Please use /stats <UUID>.");
            return found.get(0);
        });
    }

    public boolean current(Player player, StatsHolder holder) {
        return player.isOnline() && holder.viewer().equals(player.getUniqueId()) && active.get(player.getUniqueId()) == holder
                && player.getOpenInventory().getTopInventory().getHolder() == holder;
    }
    public void closed(Player player, StatsHolder holder) { active.remove(player.getUniqueId(), holder); }
    public void quit(Player player) { active.remove(player.getUniqueId()); refreshTimes.remove(player.getUniqueId()); }
    public void joined(Player player) { tasks.entity(player, () -> heads.remember(player), () -> {}); }
    public void click(Player player, StatsHolder holder, int slot) {
        if (!current(player, holder)) return;
        Runnable action = holder.action(slot);
        if (action != null) tasks.entity(player, () -> { if (current(player, holder)) action.run(); }, () -> {});
    }

    private StatsHolder inventory(Player viewer, Route route, StatsHolder.Context context, List<?> rows, String title) {
        StatsHolder holder = new StatsHolder(viewer.getUniqueId(), route, context, rows);
        holder.attach(Bukkit.createInventory(holder, 54, title));
        for (int i = 45; i < 54; i++) holder.getInventory().setItem(i, item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
        if (context != null) {
            ViewScope scope = ViewScope.of(route, viewer.getUniqueId(), context.target().uuid());
            for (int row = 0; row < 5; row++) for (int column : new int[] {0, 8}) {
                holder.getInventory().setItem(row * 9 + column, item(scope.border(), "§e" + scope.label(), List.of()));
            }
        }
        return holder;
    }
    private void show(Player viewer, StatsHolder holder) {
        viewer.openInventory(holder.getInventory());
        active.put(viewer.getUniqueId(), holder);
    }
    private StatsHolder loading(Player viewer, Route route, StatsHolder.Context context) {
        StatsHolder holder = inventory(viewer, route, context, List.of(), "Loading: " + route.title());
        holder.getInventory().setItem(22, item(Material.CLOCK, "§eLoading statistics ...", List.of("§7Reading Vanilla data.")));
        holder.getInventory().setItem(49, item(Material.PAPER, "§ePlease wait", List.of("§7Loading data ...")));
        backButton(viewer, holder);
        show(viewer, holder);
        return holder;
    }
    private void failure(Player viewer, StatsHolder loading, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause instanceof IllegalArgumentException ? cause.getMessage() : "Could not load statistics. Please try again.";
        if (!(cause instanceof IllegalArgumentException)) plugin.getLogger().warning("Statistics request failed: " + cause);
        loading.getInventory().setItem(22, item(Material.BARRIER, "§cUnable to display statistics", List.of("§7" + message)));
        loading.getInventory().setItem(49, item(Material.PAPER, "§cLoading failed", List.of("§7Close this menu and try /stats again.")));
        viewer.sendMessage("§c" + message);
    }

    private void navigate(Player viewer, Route route, StatsHolder.Context context) {
        if (route.mode() == Route.Mode.MAIN) { render(viewer, route, context, List.of()); return; }
        StatsHolder loading = loading(viewer, route, context);
        boolean personal = route.mode() == Route.Mode.PERSONAL_CATEGORY || route.mode() == Route.Mode.STATISTIC;
        CompletableFuture<Snapshot> snapshot = !personal && (route.mode() == Route.Mode.SERVER || context.snapshot() == null)
                ? service.snapshot(false) : CompletableFuture.completedFuture(context.snapshot());
        snapshot.thenCompose(data -> service.compute(() -> {
            StatsHolder.Context ready = context.withSnapshot(data);
            if (hasComparison(route) && data != null) ready = ready.withComparison(StatComparison.of(data, ready.target(), route.category(), route.key(), route.serverView()));
            return new Prepared(ready, rows(route, ready));
        })).whenComplete((prepared, error) -> tasks.entity(viewer, () -> {
            if (!current(viewer, loading)) return;
            if (error != null) failure(viewer, loading, error); else render(viewer, route, prepared.context(), prepared.rows());
        }, () -> {}));
    }
    private static boolean hasComparison(Route route) {
        return route.mode() == Route.Mode.PERSONAL_CATEGORY || route.mode() == Route.Mode.SERVER_CATEGORY
                || route.mode() == Route.Mode.STATISTIC || route.mode() == Route.Mode.RANKING;
    }

    private boolean canViewPlayer(Player viewer, UUID uuid) {
        if (uuid.equals(viewer.getUniqueId()) || viewer.hasPermission("stats.others")) return true;
        viewer.sendMessage("§cYou do not have permission to view other players' statistics.");
        return false;
    }

    private void openPlayer(Player viewer, StatsHolder origin, UUID uuid) {
        if (!canViewPlayer(viewer, uuid)) return;
        StatsHolder.ReturnPoint returnTo = new StatsHolder.ReturnPoint(origin.route(), origin.context());
        StatsHolder.Context pending = new StatsHolder.Context(null, origin.context().target(), returnTo, null);
        StatsHolder loading = loading(viewer, Route.main(), pending);
        service.personal(uuid).whenComplete((target, error) -> tasks.entity(viewer, () -> {
            if (!current(viewer, loading)) return;
            if (error != null) failure(viewer, loading, error);
            else if (target == null) failure(viewer, loading, new IllegalArgumentException("No statistics available yet."));
            else render(viewer, Route.main(), new StatsHolder.Context(null, target, returnTo, null), List.of());
        }, () -> {}));
    }
    private List<?> rows(Route route, StatsHolder.Context context) {
        Snapshot snapshot = context.snapshot();
        return switch (route.mode()) {
            case PERSONAL_CATEGORY, SERVER_CATEGORY -> {
                boolean server = route.mode() == Route.Mode.SERVER_CATEGORY;
                Set<StatKey> keys = new HashSet<>(server ? snapshot.keys() : context.target().values().keySet());
                if (!hideZeros) keys.addAll(catalog.keys());
                if (route.category() == Category.PLAY_TIME) catalog.keys().stream().filter(StatKey::isTime).forEach(keys::add);
                yield keys.stream().filter(route.category()::contains)
                        .filter(key -> !hideZeros || route.category() == Category.PLAY_TIME || (server ? snapshot.total(key) : context.target().value(key)) > 0)
                        .sorted(Comparator.<StatKey>comparingLong(key -> server ? snapshot.total(key) : context.target().value(key)).reversed().thenComparing(Comparator.naturalOrder())).toList();
            }
            case RANKING -> snapshot.ranking(route.key());
            case MEDALS -> snapshot.medals();
            case PROFILE -> MedalAward.forPlayer(snapshot, route.subject());
            default -> List.of();
        };
    }
    private void refresh(Player viewer, StatsHolder holder) {
        long now = System.nanoTime();
        Long previous = refreshTimes.get(viewer.getUniqueId());
        if (previous != null && now - previous < refreshCooldownNanos) { viewer.sendMessage("§ePlease wait before refreshing again."); return; }
        refreshTimes.put(viewer.getUniqueId(), now);
        Route route = holder.route();
        StatsHolder loading = loading(viewer, route, holder.context());
        StatsHolder.Context previousContext = holder.context();
        CompletableFuture<StatsHolder.Context> refreshed = route.serverView() || route.mode() == Route.Mode.PROFILE
                ? service.snapshot(true).thenApply(previousContext::withSnapshot)
                : service.personal(previousContext.target().uuid()).thenApply(target -> {
                    if (target == null) throw new IllegalArgumentException("No statistics available yet.");
                    return previousContext.withTarget(target).withSnapshot(null);
                });
        if (hasComparison(route) && !route.serverView()) service.snapshot(true).exceptionally(error -> null);
        refreshed
                .whenComplete((context, error) -> tasks.entity(viewer, () -> {
                    if (!current(viewer, loading)) return;
                    if (error != null) failure(viewer, loading, error); else navigate(viewer, route, context);
                }, () -> {}));
    }

    private void render(Player viewer, Route route, StatsHolder.Context context, List<?> rows) {
        Page page = Page.of(rows.size(), route.page());
        route = route.page(page.index());
        Route here = route;
        String viewedName = context.target().name();
        if (route.subject() != null && context.snapshot() != null && context.snapshot().players().containsKey(route.subject())) viewedName = context.snapshot().players().get(route.subject()).name();
        ViewScope scope = ViewScope.of(route, viewer.getUniqueId(), context.target().uuid());
        String title = scope == ViewScope.SERVER ? route.title() + " | Server"
                : route.title() + " | " + (scope == ViewScope.PERSONAL ? "You: " : "Player: ") + viewedName;
        StatsHolder holder = inventory(viewer, route, context, rows, title);
        if (route.mode() == Route.Mode.MAIN || route.mode() == Route.Mode.SERVER) {
            boolean server = route.mode() == Route.Mode.SERVER;
            for (int index = 0; index < Page.CONTENT_SIZE; index++) holder.getInventory().setItem(MenuLayout.contentSlot(index), item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
            for (int slot : new int[] {13, 21, 23, 31}) holder.getInventory().setItem(slot, item(Material.CYAN_STAINED_GLASS_PANE, " ", List.of()));
            for (Category category : Category.values()) {
                Route child = here.child(server ? Route.Mode.SERVER_CATEGORY : Route.Mode.PERSONAL_CATEGORY, category, null, null);
                List<String> description = new ArrayList<>(List.of("§7" + scope.label()));
                if (category == Category.PLACED) description.add("§7Block placements and planting.");
                if (category == Category.USED) description.add("§7Tools, food, buckets and other item uses.");
                if (server) description.add("§7Including offline players");
                description.add("§eClick to open");
                button(holder, MenuLayout.slot(category), item(Material.valueOf(category.icon()), "§b" + category.title(), description), () -> navigate(viewer, child, context));
            }
            if (server) button(holder, MenuLayout.CENTER, item(Material.GOLD_INGOT, "§6Medal ranking", List.of("§7Total medals, then gold / silver / bronze", "§7Ties: 1, 1, 3; zero values earn no medals")), () -> navigate(viewer, here.child(Route.Mode.MEDALS, null, null, null), context));
            else button(holder, MenuLayout.CENTER, item(Material.GOLD_BLOCK, "§6Server leaderboards", List.of("§7All known players, including offline players", "§eClick for leaderboards and medals")), () -> navigate(viewer, here.child(Route.Mode.SERVER, null, null, null), context));
        } else if (route.mode() == Route.Mode.STATISTIC) {
            StatKey key = route.key();
            List<String> detail = new ArrayList<>(List.of("§7Personal value: §f" + ValueFormatter.format(key, context.target().value(key)),
                    "§8" + key.technicalName(), "§eClick for full leaderboard"));
            deathExplanation(key, detail);
            button(holder, 22, decorate(new ItemStack(labels.icon(key)), labels.usesItemName(key) ? null : "§b" + labels.name(key), detail),
                    () -> navigate(viewer, here.child(Route.Mode.RANKING, here.category(), key, null), context));
        } else {
            for (int i = page.from(); i < page.to(); i++) renderRow(viewer, holder, MenuLayout.contentSlot(i - page.from()), rows.get(i));
            if (rows.isEmpty()) holder.getInventory().setItem(22, item(Material.PAPER, "§eNo statistics available yet", List.of("§7No matching positive Vanilla values.")));
        }
        if (page.hasPrevious()) button(holder, hasComparison(route) ? 47 : 45, decorate(heads.arrow(false), "§ePrevious page", List.of()), () -> render(viewer, here.page(page.index() - 1), context, rows));
        if (page.hasNext()) button(holder, 53, decorate(heads.arrow(true), "§eNext page", List.of()), () -> render(viewer, here.page(page.index() + 1), context, rows));
        backButton(viewer, holder);
        if (hasComparison(route)) comparisonItems(holder);
        if (route.mode() == Route.Mode.MAIN) button(holder, 52, item(Material.GOLD_NUGGET, "§6Medal profile", List.of("§7Gold, silver and bronze awards", "§eClick to open")),
                () -> navigate(viewer, here.child(Route.Mode.PROFILE, null, null, context.target().uuid()), context));
        if (route.mode() == Route.Mode.PROFILE) medalSwitch(viewer, holder);
        List<String> status = new ArrayList<>(List.of("§7" + scope.label(), "§7" + route.title(),
                "§7Entries: " + ValueFormatter.number(route.mode() == Route.Mode.STATISTIC ? 1 : rows.size())));
        if (scope == ViewScope.SERVER || route.mode() == Route.Mode.PROFILE || hasComparison(route) && context.snapshot() != null) {
            status.add("§7Server snapshot: " + DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(context.snapshot().createdAt()));
            context.snapshot().warnings().stream().limit(5).forEach(warning -> status.add("§e" + warning));
        }
        if (scope != ViewScope.SERVER && route.mode() != Route.Mode.PROFILE) status.add("§7Personal values read when opened or refreshed.");
        if (route.key() != null) status.add("§8" + route.key().technicalName());
        holder.getInventory().setItem(49, item(Material.PAPER, "§bPage " + (page.index() + 1) + " / " + page.count(), status));
        button(holder, 50, item(Material.SUNFLOWER, "§aRefresh", List.of("§7Reload Vanilla data", "§7Server rankings use the configured cache and cooldown.")), () -> refresh(viewer, holder));
        show(viewer, holder);
        if (hasComparison(route) && context.snapshot() == null) loadComparison(viewer, holder);
    }

    private void renderRow(Player viewer, StatsHolder holder, int slot, Object row) {
        StatsHolder.Context context = holder.context();
        Route route = holder.route();
        if (row instanceof StatKey key) {
            List<String> lore = new ArrayList<>();
            boolean server = route.mode() == Route.Mode.SERVER_CATEGORY;
            lore.add("§7" + (server ? "Server total - " : "") + route.category().valueLabel() + ": §f" + ValueFormatter.format(key, server ? context.snapshot().total(key) : context.target().value(key)));
            if (server) {
                lore.add("§7Players: all known UUIDs");
                lore.add("");
                List<Snapshot.RankedPlayer> leaders = context.snapshot().ranking(key).stream().limit(3).toList();
                leaders.forEach(player -> lore.add(placement(player, key)));
                Snapshot.RankedPlayer own = context.snapshot().position(key, viewer.getUniqueId());
                if (leaders.stream().noneMatch(player -> player.uuid().equals(viewer.getUniqueId()))) {
                    lore.add("");
                    lore.add(own == null ? "§e–. " + viewer.getName() + " – " + ValueFormatter.format(key, 0) : "§eYour rank: " + placement(own, key));
                }
                lore.add(""); lore.add("§eClick for full leaderboard");
                holder.action(slot, () -> navigate(viewer, route.child(Route.Mode.RANKING, route.category(), key, null), context));
            } else {
                lore.add("§eClick for server share and hourly rate");
                holder.action(slot, () -> navigate(viewer, route.child(Route.Mode.STATISTIC, route.category(), key, null), context));
            }
            deathExplanation(key, lore);
            lore.add("§8" + key.technicalName());
            ItemStack icon = new ItemStack(labels.icon(key));
            boolean defaultName = labels.usesItemName(key);
            holder.getInventory().setItem(slot, decorate(icon, defaultName ? null : "§b" + labels.name(key), lore));
        } else if (row instanceof Snapshot.RankedPlayer player) {
            playerHead(viewer, holder, slot, player.uuid(), player.name(), medalColor(player.rank()) + "#" + player.rank() + " " + player.name(),
                    List.of("§7Value: §f" + ValueFormatter.format(route.key(), player.value()), "§7" + medalName(player.rank()), "§eClick for player statistics", "§8" + player.uuid(), "§8" + route.key().technicalName()));
            holder.action(slot, () -> openPlayer(viewer, holder, player.uuid()));
        } else if (row instanceof Snapshot.Medals medals) {
            playerHead(viewer, holder, slot, medals.uuid(), medals.name(), "§b#" + medals.rank() + " " + medals.name(),
                    List.of("§fTotal: " + ValueFormatter.number(medals.total()), "§6Gold: " + medals.gold(), "§7Silver: " + medals.silver(), "§cBronze: " + medals.bronze(), "§eClick for individual medals", "§8" + medals.uuid()));
            holder.action(slot, () -> {
                if (canViewPlayer(viewer, medals.uuid())) {
                    navigate(viewer, route.child(Route.Mode.PROFILE, null, null, medals.uuid()), context);
                }
            });
        } else if (row instanceof MedalAward award) {
            button(holder, slot, decorate(new ItemStack(labels.icon(award.key())), labels.usesItemName(award.key()) ? null : "§b" + labels.name(award.key()),
                    List.of(medalColor(award.placement().rank()) + medalName(award.placement().rank()), "§7Value: " + ValueFormatter.format(award.key(), award.placement().value()), "§8" + award.key().technicalName(), "§eClick for full leaderboard")),
                    () -> navigate(viewer, route.child(Route.Mode.RANKING, null, award.key(), null), context));
        }
    }

    private void playerHead(Player viewer, StatsHolder holder, int slot, UUID uuid, String playerName, String title, List<String> lore) {
        holder.getInventory().setItem(slot, item(Material.PLAYER_HEAD, title, lore));
        CompletableFuture<ItemStack> resolved = heads.player(uuid, playerName);
        ItemStack ready = resolved.getNow(null);
        if (ready != null) { holder.getInventory().setItem(slot, decorate(ready, title, lore)); return; }
        resolved.thenAccept(head -> tasks.entity(viewer, () -> {
            if (current(viewer, holder)) holder.getInventory().setItem(slot, decorate(head, title, lore));
        }, () -> {}));
    }

    private void backButton(Player viewer, StatsHolder holder) {
        Route route = holder.route();
        StatsHolder.Context context = holder.context();
        boolean back = route.parent() != null || context != null && context.returnTo() != null;
        button(holder, 48, decorate(back ? heads.arrow(false) : heads.closeButton(), back ? "§eBack" : "§c✕ Close", List.of()), () -> {
            if (route.parent() != null) navigate(viewer, route.parent(), context);
            else if (context != null && context.returnTo() != null) {
                StatsHolder.ReturnPoint previous = context.returnTo();
                navigate(viewer, previous.route(), previous.context());
            } else viewer.closeInventory();
        });
    }

    private void comparisonItems(StatsHolder holder) {
        StatComparison comparison = holder.context().comparison();
        if (comparison == null) {
            holder.getInventory().setItem(45, item(Material.CHEST, "§bTotals and server share", List.of("§7Loading server comparison ...")));
            holder.getInventory().setItem(46, item(Material.CLOCK, "§bRelative to play time", List.of("§7Loading server comparison ...", "§7Your statistics are already available.")));
            return;
        }
        boolean server = holder.route().serverView();
        List<String> totals = new ArrayList<>(), rates = new ArrayList<>();
        if (comparison.unit() == null) {
            totals.add("§7These statistics use different units.");
            totals.add("§7Open an individual statistic to compare it.");
            rates.add("§7Open an individual statistic for its rate.");
        } else {
            if (!server) totals.add("§7Personal total: §f" + ValueFormatter.format(comparison.unit(), comparison.personal()));
            totals.add("§7Server total: §f" + ValueFormatter.format(comparison.unit(), comparison.server()));
            if (!server) totals.add("§7Share of server: §f" + ValueFormatter.share(comparison.personal(), comparison.server()));
            if (holder.route().category() == Category.PLAY_TIME && holder.route().key() == null)
                totals.add("§7Time played; overlapping timers are not added.");
            if (!server) rates.add("§7Personal: §f" + ValueFormatter.hourly(comparison.unit(), comparison.personal(), comparison.personalTicks()));
            rates.add("§7Server: §f" + ValueFormatter.hourly(comparison.unit(), comparison.server(), comparison.serverTicks()));
        }
        if (!server) rates.add("§7Personal play time: " + ValueFormatter.time(comparison.personalTicks()));
        rates.add("§7Combined server play time: " + ValueFormatter.time(comparison.serverTicks()));
        rates.add("§7Server rates use all players' combined play time.");
        holder.getInventory().setItem(45, item(Material.CHEST, "§bTotals and server share", totals));
        holder.getInventory().setItem(46, item(Material.CLOCK, "§bRelative to play time", rates));
    }

    private void loadComparison(Player viewer, StatsHolder holder) {
        Route route = holder.route();
        service.snapshot(false).thenCompose(snapshot -> service.compute(() -> holder.context().withSnapshot(snapshot)
                .withComparison(StatComparison.of(snapshot, holder.context().target(), route.category(), route.key(), false))))
                .whenComplete((context, error) -> tasks.entity(viewer, () -> {
                    if (!current(viewer, holder)) return;
                    if (error == null) render(viewer, route, context, holder.rows());
                    else {
                        List<String> message = List.of("§7Server comparison is unavailable.", "§7Refresh to try again; personal values remain available.");
                        holder.getInventory().setItem(45, item(Material.CHEST, "§eComparison unavailable", message));
                        holder.getInventory().setItem(46, item(Material.CLOCK, "§eComparison unavailable", message));
                    }
                }, () -> {}));
    }

    private void medalSwitch(Player viewer, StatsHolder holder) {
        List<String> destinations = new ArrayList<>();
        int next = 0;
        for (int step = 1; step <= 3; step++) {
            int rank = (holder.route().medalTier() + step - 1) % 3 + 1;
            if (MedalAward.firstIndex(holder.rows(), rank) >= 0) { next = rank; break; }
        }
        for (int rank = 1; rank <= 3; rank++) {
            int first = MedalAward.firstIndex(holder.rows(), rank);
            destinations.add(medalColor(rank) + medalName(rank) + (first < 0 ? ": none" : ": page " + (first / Page.CONTENT_SIZE + 1)));
        }
        if (next == 0) {
            holder.getInventory().setItem(52, item(Material.PAPER, "§7No medals to jump to", destinations));
            return;
        }
        int rank = next;
        int destination = MedalAward.firstIndex(holder.rows(), rank) / Page.CONTENT_SIZE;
        destinations.add("§7Click repeatedly to cycle medal types.");
        Material icon = rank == 1 ? Material.GOLD_INGOT : rank == 2 ? Material.IRON_INGOT : Material.COPPER_INGOT;
        button(holder, 52, item(icon, medalColor(rank) + "Jump to first " + medalName(rank), destinations),
                () -> render(viewer, holder.route().medalPage(rank, destination), holder.context(), holder.rows()));
    }

    private static void deathExplanation(StatKey key, List<String> lore) {
        if (key.equals(PlayerStats.UNKNOWN_DEATHS)) {
            lore.add("§7Total deaths minus deaths attributed to entities.");
            lore.add("§7Includes falls, void and other unrecorded causes.");
            lore.add("§7Vanilla does not store these causes separately.");
        }
    }

    private static String placement(Snapshot.RankedPlayer player, StatKey key) { return medalColor(player.rank()) + player.rank() + ". " + player.name() + " – " + ValueFormatter.format(key, player.value()) + (player.rank() <= 3 ? " [" + medalName(player.rank()) + "]" : ""); }
    private static String medalColor(int rank) { return rank == 1 ? "§6" : rank == 2 ? "§f" : rank == 3 ? "§c" : "§7"; }
    private static String medalName(int rank) { return rank == 1 ? "Gold" : rank == 2 ? "Silver" : rank == 3 ? "Bronze" : "No medal"; }
    private static void button(StatsHolder holder, int slot, ItemStack item, Runnable action) { holder.getInventory().setItem(slot, item); holder.action(slot, action); }
    private static ItemStack item(Material material, String name, List<String> lore) { return decorate(new ItemStack(material), name, lore); }
    private static ItemStack decorate(ItemStack item, String name, List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { if (name != null) meta.setDisplayName(name); meta.setLore(lore); meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS); item.setItemMeta(meta); }
        return item;
    }

    public void close() {
        if (!TaskBridge.isFolia()) Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof StatsHolder) player.closeInventory();
        });
        active.clear(); refreshTimes.clear(); heads.close();
    }
}
