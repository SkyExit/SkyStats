package de.laurinhummel.skystats.main;

import de.laurinhummel.skystats.shortcuts.SkyStrings;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class SkyStats extends JavaPlugin {
    private static SkyStats plugin;
    private FileConfiguration dataFile;

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        System.out.println(SkyStrings.SKYSTATSCONSOLE + "Loading config file...");
        createCustomConfig();
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
