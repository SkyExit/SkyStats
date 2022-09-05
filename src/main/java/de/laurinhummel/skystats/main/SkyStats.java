package de.laurinhummel.skystats.main;

import de.laurinhummel.skystats.commands.EnderchestCommand;
import de.laurinhummel.skystats.commands.LevelCommand;
import de.laurinhummel.skystats.commands.StatsCommand;
import de.laurinhummel.skystats.commands.WorkbenchCommand;
import de.laurinhummel.skystats.listeners.StopStatsInvItemMovement;
import de.laurinhummel.skystats.misc.SkyLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class SkyStats extends JavaPlugin {
    private static SkyStats plugin;
    private FileConfiguration dataFile;
    private FileConfiguration permFile;

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        SkyLogger.sendConsole("Loading data file...");
        createDataFile();
        SkyLogger.sendConsole("Loading permission file...");
        createPermissionFile();

        SkyLogger.sendConsole("Registering commands...");
            getCommand("stats").setExecutor(new StatsCommand());
            getCommand("level").setExecutor(new LevelCommand());
            getCommand("workbench").setExecutor(new WorkbenchCommand());
            getCommand("enderchest").setExecutor(new EnderchestCommand());
            //getCommand("skyify").setExecutor(new SkyifyCommand());



        pluginManager.registerEvents((Listener)new StopStatsInvItemMovement(), this);
        //pluginManager.registerEvents((Listener) new StopSkyifiedItemsAnvilUsage(), (Plugin) this);
    }

    @Override
    public void onDisable() {
    }

    private void createDataFile() {
        File customConfigFile = new File(getDataFolder(), "data.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("data.yml", false); }
        dataFile = new YamlConfiguration();
        try { dataFile.load(customConfigFile); }
        catch (IOException | InvalidConfigurationException e) { e.printStackTrace(); }
    }

    private void createPermissionFile() {
        File customConfigFile = new File(getDataFolder(), "permission.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("permission.yml", false); }
        permFile = new YamlConfiguration();
        try { permFile.load(customConfigFile); }
        catch (IOException | InvalidConfigurationException e) { e.printStackTrace(); }
    }

    public FileConfiguration getDataFile() {
        return this.dataFile;
    }
    public FileConfiguration getPermissionFile() {
        return this.permFile;
    }

    public static SkyStats getPlugin() {
        return plugin;
    }
}
