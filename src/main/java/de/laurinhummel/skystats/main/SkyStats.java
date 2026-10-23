package de.laurinhummel.skystats.main;

import de.laurinhummel.skystats.commands.CompareCommand;
import de.laurinhummel.skystats.commands.LevelCommand;
import de.laurinhummel.skystats.commands.StatsCommand;
import de.laurinhummel.skystats.listeners.PlayerDeathListener;
import de.laurinhummel.skystats.listeners.StopStatsInvItemMovement;
import de.laurinhummel.skystats.menus.StatsMenuService;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SkyStats extends JavaPlugin {
    private PlayerScheduler scheduler;
    private StatsMenuService menus;

    @Override
    public void onEnable() {
        scheduler = new PlayerScheduler(this);
        menus = new StatsMenuService(this, scheduler);
        Objects.requireNonNull(getCommand("stats"), "Missing stats command")
                .setExecutor(new StatsCommand(this, scheduler, menus));
        Objects.requireNonNull(getCommand("level"), "Missing level command")
                .setExecutor(new LevelCommand(this, scheduler));
        Objects.requireNonNull(getCommand("compare"), "Missing compare command")
                .setExecutor(new CompareCommand(this, scheduler, menus));
        getServer().getPluginManager().registerEvents(new StopStatsInvItemMovement(), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(), this);
        getServer().getPluginManager().registerEvents(menus, this);
    }

    @Override
    public void onDisable() {
        if (menus != null) {
            menus.clear();
        }
        if (scheduler != null) {
            scheduler.close();
        }
    }
}
