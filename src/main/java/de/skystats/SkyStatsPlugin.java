package de.skystats;

import de.skystats.data.*;
import de.skystats.platform.TaskBridge;
import de.skystats.ui.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class SkyStatsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private StatsService service;
    private MenuController menus;

    @Override public void onEnable() {
        saveDefaultConfig();
        TaskBridge tasks = new TaskBridge(this);
        StatCatalog catalog = new StatCatalog();
        Path serverRoot = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        String configured = getConfig().getString("stats-directory", "");
        Path stats = configured.isBlank() ? StatsPaths.primaryStatistics(Bukkit.getWorlds().get(0).getWorldFolder().toPath()) : serverRoot.resolve(configured).normalize();
        PlayerDirectory names = new PlayerDirectory(getDataFolder().toPath().resolve("players.json"));
        int cooldown = positive("refresh-cooldown-seconds", 5, 3600);
        service = new StatsService(stats, serverRoot.resolve("usercache.json"), names,
                new StatsFileReader(positive("max-file-size-mib", 16, 1024) * 1024L * 1024L, positive("max-statistics-per-player", 100000, 1000000)),
                new LiveStatsReader(tasks, catalog, positive("live-reads-per-tick", 400, 10000)), getLogger(),
                positive("max-player-files", 100000, 1000000), positive("max-total-values", 2000000, 20000000), positive("live-read-timeout-seconds", 30, 300),
                Duration.ofSeconds(positive("cache-seconds", 30, 86400)), Duration.ofSeconds(cooldown));
        menus = new MenuController(this, tasks, service, catalog, getConfig().getBoolean("hide-zero-values", true), cooldown);
        PluginCommand command = Objects.requireNonNull(getCommand("stats"));
        command.setExecutor(this); command.setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new InventoryGuard(menus), this);
        Bukkit.getOnlinePlayers().forEach(service::joined);
        service.snapshot(false).exceptionally(error -> { getLogger().warning("Initial statistics load failed: " + error); return null; });
        getLogger().info("Reading Vanilla statistics from " + stats + "; " + catalog.keys().size() + " API combinations; scheduler=" + (TaskBridge.isFolia() ? "Folia" : "Paper/Spigot"));
    }
    private int positive(String key, int fallback, int maximum) { return Math.max(1, Math.min(maximum, getConfig().getInt(key, fallback))); }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Dieser Befehl kann nur im Spiel verwendet werden."); return true; }
        if (!player.hasPermission("stats.use")) { player.sendMessage("§cDu darfst keine Statistiken öffnen."); return true; }
        if (args.length > 1) { player.sendMessage("§eVerwendung: /stats [Spieler]"); return true; }
        if (args.length == 1 && !player.hasPermission("stats.others") && !args[0].equalsIgnoreCase(player.getName()) && !args[0].equalsIgnoreCase(player.getUniqueId().toString())) {
            player.sendMessage("§cDu darfst keine Statistiken anderer Spieler öffnen."); return true;
        }
        menus.command(player, args.length == 0 ? null : args[0]);
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !sender.hasPermission("stats.use")) return List.of();
        if (!sender.hasPermission("stats.others")) return sender instanceof Player player && player.getName().toLowerCase(java.util.Locale.ROOT).startsWith(args[0].toLowerCase(java.util.Locale.ROOT)) ? List.of(player.getName()) : List.of();
        return service.names().complete(args[0]);
    }
    @EventHandler public void join(PlayerJoinEvent event) { service.joined(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) { service.left(event.getPlayer()); menus.quit(event.getPlayer()); }
    @Override public void onDisable() {
        if (menus != null) menus.close();
        if (service != null) service.close();
    }
}
