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
    private record Award(StatKey key, Snapshot.RankedPlayer placement) {}
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
            service.snapshot(false).thenCompose(snapshot -> {
                UUID target;
                if (targetName == null) target = viewer.getUniqueId();
                else {
                    List<UUID> found = service.names().find(targetName);
                    if (found.isEmpty()) throw new CompletionException(new IllegalArgumentException("Unbekannter Spieler oder noch keine gespeicherten Statistiken vorhanden."));
                    if (found.size() > 1) throw new CompletionException(new IllegalArgumentException("Dieser Name gehört zu mehreren UUIDs. Bitte verwende /stats <UUID>."));
                    target = found.get(0);
                }
                return service.personal(target, snapshot).thenApply(stats -> {
                    if (stats == null) throw new CompletionException(new IllegalArgumentException("Noch keine Statistiken vorhanden."));
                    return new StatsHolder.Context(snapshot, stats);
                });
            }).whenComplete((context, error) -> tasks.entity(viewer, () -> {
                if (!current(viewer, loading)) return;
                if (error != null) failure(viewer, loading, error);
                else render(viewer, Route.main(), context, List.of());
            }, () -> {}));
        }, () -> {});
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
        return holder;
    }
    private void show(Player viewer, StatsHolder holder) {
        viewer.openInventory(holder.getInventory());
        active.put(viewer.getUniqueId(), holder);
    }
    private StatsHolder loading(Player viewer, Route route, StatsHolder.Context context) {
        StatsHolder holder = inventory(viewer, route, context, List.of(), "Lädt: " + route.title());
        holder.getInventory().setItem(22, item(Material.CLOCK, "§eStatistiken werden geladen …", List.of("§7Vanilla-Daten werden gelesen.")));
        holder.getInventory().setItem(49, item(Material.PAPER, "§eBitte warten", List.of("§7Dateien und Ranglisten werden asynchron geladen.")));
        if (context != null && route.parent() != null) button(holder, 48, item(Material.OAK_DOOR, "§eZurück", List.of()), () -> navigate(viewer, route.parent(), context));
        show(viewer, holder);
        return holder;
    }
    private void failure(Player viewer, StatsHolder loading, Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause instanceof IllegalArgumentException ? cause.getMessage() : "Statistiken konnten nicht geladen werden. Bitte erneut versuchen.";
        if (!(cause instanceof IllegalArgumentException)) plugin.getLogger().warning("Statistics request failed: " + cause);
        loading.getInventory().setItem(22, item(Material.BARRIER, "§cKeine Anzeige möglich", List.of("§7" + message)));
        loading.getInventory().setItem(49, item(Material.PAPER, "§cLaden fehlgeschlagen", List.of("§7Schließe das Menü und versuche /stats erneut.")));
        viewer.sendMessage("§c" + message);
    }

    private void navigate(Player viewer, Route route, StatsHolder.Context context) {
        if (route.mode() == Route.Mode.MAIN || route.mode() == Route.Mode.SERVER) { render(viewer, route, context, List.of()); return; }
        StatsHolder loading = loading(viewer, route, context);
        service.compute(() -> rows(route, context)).whenComplete((rows, error) -> tasks.entity(viewer, () -> {
            if (!current(viewer, loading)) return;
            if (error != null) failure(viewer, loading, error); else render(viewer, route, context, rows);
        }, () -> {}));
    }
    private List<?> rows(Route route, StatsHolder.Context context) {
        Snapshot snapshot = context.snapshot();
        return switch (route.mode()) {
            case PERSONAL_CATEGORY, SERVER_CATEGORY -> {
                boolean server = route.mode() == Route.Mode.SERVER_CATEGORY;
                Set<StatKey> keys = new HashSet<>(server ? snapshot.keys() : context.target().values().keySet());
                if (!hideZeros) keys.addAll(catalog.keys());
                yield keys.stream().filter(route.category()::contains)
                        .filter(key -> !hideZeros || (server ? snapshot.total(key) : context.target().value(key)) > 0)
                        .sorted(Comparator.<StatKey>comparingLong(key -> server ? snapshot.total(key) : context.target().value(key)).reversed().thenComparing(Comparator.naturalOrder())).toList();
            }
            case RANKING -> snapshot.ranking(route.key());
            case MEDALS -> snapshot.medals();
            case PROFILE -> snapshot.keys().stream().map(key -> new Award(key, snapshot.position(key, route.subject())))
                    .filter(award -> award.placement() != null && award.placement().rank() <= 3)
                    .sorted(Comparator.comparingInt((Award award) -> award.placement().rank()).thenComparing(Award::key)).toList();
            default -> List.of();
        };
    }
    private void refresh(Player viewer, StatsHolder holder) {
        long now = System.nanoTime();
        Long previous = refreshTimes.get(viewer.getUniqueId());
        if (previous != null && now - previous < refreshCooldownNanos) { viewer.sendMessage("§eBitte warte kurz vor der nächsten Aktualisierung."); return; }
        refreshTimes.put(viewer.getUniqueId(), now);
        Route route = holder.route();
        StatsHolder loading = loading(viewer, route, holder.context());
        service.snapshot(true).thenCompose(snapshot -> service.personal(holder.context().target().uuid(), snapshot)
                .thenApply(target -> new StatsHolder.Context(snapshot, target == null ? holder.context().target() : target)))
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
        if (route.subject() != null && context.snapshot().players().containsKey(route.subject())) viewedName = context.snapshot().players().get(route.subject()).name();
        StatsHolder holder = inventory(viewer, route, context, rows, route.title() + " | " + viewedName);
        if (route.mode() == Route.Mode.MAIN || route.mode() == Route.Mode.SERVER) {
            boolean server = route.mode() == Route.Mode.SERVER;
            for (int slot = 0; slot < 45; slot++) holder.getInventory().setItem(slot, item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of()));
            for (int slot : new int[] {13, 21, 23, 31}) holder.getInventory().setItem(slot, item(Material.CYAN_STAINED_GLASS_PANE, " ", List.of()));
            for (Category category : Category.values()) {
                Route child = here.child(server ? Route.Mode.SERVER_CATEGORY : Route.Mode.PERSONAL_CATEGORY, category, null, null);
                button(holder, MenuLayout.slot(category), item(Material.valueOf(category.icon()), "§b" + category.title(), server ? List.of("§7Serverweit, inklusive Offline-Spieler", "§eKlicken: öffnen") : List.of("§eKlicken: öffnen")), () -> navigate(viewer, child, context));
            }
            if (server) button(holder, MenuLayout.CENTER, item(Material.GOLD_INGOT, "§6Medaillen-Ranking", List.of("§7Gesamtzahl, dann Gold / Silber / Bronze", "§7Gleichstände: 1, 1, 3; Nullwerte ohne Medaille")), () -> navigate(viewer, here.child(Route.Mode.MEDALS, null, null, null), context));
            else button(holder, MenuLayout.CENTER, item(Material.GOLD_BLOCK, "§6Server-Leaderboards", List.of("§7Alle bekannten Spieler, auch offline", "§eKlicken: Bestenlisten und Medaillen")), () -> navigate(viewer, here.child(Route.Mode.SERVER, null, null, null), context));
        } else {
            for (int i = page.from(); i < page.to(); i++) renderRow(viewer, holder, i - page.from(), rows.get(i));
            if (rows.isEmpty()) holder.getInventory().setItem(22, item(Material.PAPER, "§eNoch keine Statistiken vorhanden", List.of("§7Keine passenden positiven Vanilla-Werte.")));
        }
        if (page.hasPrevious()) button(holder, 45, decorate(heads.arrow(false), "§eVorherige Seite", List.of()), () -> render(viewer, here.page(page.index() - 1), context, rows));
        if (page.hasNext()) button(holder, 53, decorate(heads.arrow(true), "§eNächste Seite", List.of()), () -> render(viewer, here.page(page.index() + 1), context, rows));
        button(holder, 48, item(Material.OAK_DOOR, route.parent() == null ? "§eSchließen" : "§eZurück", List.of()), () -> {
            if (here.parent() == null) viewer.closeInventory(); else navigate(viewer, here.parent(), context);
        });
        List<String> status = new ArrayList<>(List.of("§7" + route.title(),
                "§7Einträge: " + ValueFormatter.number(rows.size()),
                "§7Server-Stand: " + DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(context.snapshot().createdAt()),
                "§7Live-Werte werden über mehrere Ticks gelesen."));
        context.snapshot().warnings().stream().limit(5).forEach(warning -> status.add("§e" + warning));
        if (route.key() != null) status.add("§8" + route.key().technicalName());
        holder.getInventory().setItem(49, item(Material.PAPER, "§bSeite " + (page.index() + 1) + " / " + page.count(), status));
        button(holder, 50, item(Material.SUNFLOWER, "§aAktualisieren", List.of("§7Vanilla-Daten erneut laden", "§7Serverweiter Cache schützt vor häufigen Berechnungen.")), () -> refresh(viewer, holder));
        show(viewer, holder);
    }

    private void renderRow(Player viewer, StatsHolder holder, int slot, Object row) {
        StatsHolder.Context context = holder.context();
        Route route = holder.route();
        if (row instanceof StatKey key) {
            List<String> lore = new ArrayList<>();
            boolean server = route.mode() == Route.Mode.SERVER_CATEGORY;
            lore.add("§7" + (server ? "Serverweit – " : "") + route.category().valueLabel() + ": §f" + ValueFormatter.format(key, server ? context.snapshot().total(key) : context.target().value(key)));
            if (server) {
                lore.add("§7Spieler: alle bekannten UUIDs");
                lore.add("");
                List<Snapshot.RankedPlayer> leaders = context.snapshot().ranking(key).stream().limit(3).toList();
                leaders.forEach(player -> lore.add(placement(player, key)));
                Snapshot.RankedPlayer own = context.snapshot().position(key, viewer.getUniqueId());
                if (leaders.stream().noneMatch(player -> player.uuid().equals(viewer.getUniqueId()))) {
                    lore.add("");
                    lore.add(own == null ? "§e–. " + viewer.getName() + " – " + ValueFormatter.format(key, 0) : "§eDein Rang: " + placement(own, key));
                }
                lore.add(""); lore.add("§eKlicken: komplette Rangliste");
                holder.action(slot, () -> navigate(viewer, route.child(Route.Mode.RANKING, route.category(), key, null), context));
            }
            lore.add("§8" + key.technicalName());
            ItemStack icon = new ItemStack(labels.icon(key));
            boolean defaultName = !labels.hasLabel(key) && icon.getType() != Material.PAPER;
            holder.getInventory().setItem(slot, decorate(icon, defaultName ? null : "§b" + labels.name(key), lore));
        } else if (row instanceof Snapshot.RankedPlayer player) {
            playerHead(viewer, holder, slot, player.uuid(), player.name(), medalColor(player.rank()) + "#" + player.rank() + " " + player.name(),
                    List.of("§7Wert: §f" + ValueFormatter.format(route.key(), player.value()), "§7" + medalName(player.rank()), "§8" + player.uuid(), "§8" + route.key().technicalName()));
        } else if (row instanceof Snapshot.Medals medals) {
            playerHead(viewer, holder, slot, medals.uuid(), medals.name(), "§b#" + medals.rank() + " " + medals.name(),
                    List.of("§fGesamt: " + ValueFormatter.number(medals.total()), "§6Gold: " + medals.gold(), "§7Silber: " + medals.silver(), "§cBronze: " + medals.bronze(), "§eKlicken: Medaillenprofil", "§8" + medals.uuid()));
            holder.action(slot, () -> navigate(viewer, route.child(Route.Mode.PROFILE, null, null, medals.uuid()), context));
        } else if (row instanceof Award award) {
            button(holder, slot, decorate(new ItemStack(labels.icon(award.key())), medalColor(award.placement().rank()) + medalName(award.placement().rank()) + " – " + labels.name(award.key()),
                    List.of("§7Wert: " + ValueFormatter.format(award.key(), award.placement().value()), "§8" + award.key().technicalName(), "§eKlicken: komplette Rangliste")),
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

    private static String placement(Snapshot.RankedPlayer player, StatKey key) { return medalColor(player.rank()) + player.rank() + ". " + player.name() + " – " + ValueFormatter.format(key, player.value()) + (player.rank() <= 3 ? " [" + medalName(player.rank()) + "]" : ""); }
    private static String medalColor(int rank) { return rank == 1 ? "§6" : rank == 2 ? "§f" : rank == 3 ? "§c" : "§7"; }
    private static String medalName(int rank) { return rank == 1 ? "Gold" : rank == 2 ? "Silber" : rank == 3 ? "Bronze" : "Keine Medaille"; }
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
