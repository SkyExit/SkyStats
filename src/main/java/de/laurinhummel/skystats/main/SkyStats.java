package de.laurinhummel.skystats.main;

import org.bukkit.plugin.java.JavaPlugin;

public final class SkyStats extends JavaPlugin {
    private static SkyStats plugin;

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public static SkyStats getPlugin() {
        return plugin;
    }
}
