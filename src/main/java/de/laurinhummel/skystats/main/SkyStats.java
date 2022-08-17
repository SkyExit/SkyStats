package de.laurinhummel.skystats.main;

import de.laurinhummel.skystats.commands.LevelCommand;
import de.laurinhummel.skystats.commands.SkyifyCommand;
import de.laurinhummel.skystats.commands.StatsCommand;
import de.laurinhummel.skystats.listeners.StopSkyifiedItemsAnvilUsage;
import de.laurinhummel.skystats.listeners.StopStatsInvItemMovement;
import de.laurinhummel.skystats.misc.SkyLogger;
import de.laurinhummel.skystats.misc.SkyStrings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SkyStats extends JavaPlugin {
    private static SkyStats plugin;
    private FileConfiguration dataFile;

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        SkyLogger.sendConsole("Loading config file...");
        createCustomConfig();

        getCommand("stats").setExecutor(new StatsCommand());
        getCommand("level").setExecutor(new LevelCommand());
        getCommand("skyify").setExecutor(new SkyifyCommand());

        pluginManager.registerEvents((Listener)new StopStatsInvItemMovement(), (Plugin) this);
        pluginManager.registerEvents((Listener) new StopSkyifiedItemsAnvilUsage(), (Plugin) this);
    }

    @Override
    public void onDisable() {
    }

    private void createCustomConfig() {
        File customConfigFile = new File(getDataFolder(), "data.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("data.yml", false); }
        dataFile = new YamlConfiguration();
        try { dataFile.load(customConfigFile); }
        catch (IOException | InvalidConfigurationException e) { e.printStackTrace(); }
    }

    public FileConfiguration getDataFile() {
        return this.dataFile;
    }

    public static SkyStats getPlugin() {
        return plugin;
    }
}
