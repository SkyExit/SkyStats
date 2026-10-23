package de.skystats.ui;

import de.skystats.model.StatKey;
import org.bukkit.Material;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public final class Labels {
    private final Properties labels = new Properties();
    public Labels() {
        try (var stream = Labels.class.getResourceAsStream("/labels-de.properties")) {
            if (stream != null) labels.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException error) { throw new IllegalStateException("Cannot load labels", error); }
    }
    public String name(StatKey key) { return labels.getProperty(key.id(), key.id()); }
    public Material icon(StatKey key) {
        if (key.type().equals("minecraft:custom")) {
            if (key.isTime()) return Material.CLOCK;
            if (key.isDistance()) return Material.LEATHER_BOOTS;
            if (key.isDamage()) return Material.IRON_CHESTPLATE;
            String id = key.id();
            if (id.endsWith(":jump")) return Material.SLIME_BLOCK;
            if (id.endsWith(":deaths")) return Material.SKELETON_SKULL;
            if (id.contains("kill")) return Material.IRON_SWORD;
            return Material.PAPER;
        }
        String path = key.id().substring(key.id().indexOf(':') + 1).toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(path + (key.type().equals("minecraft:killed") || key.type().equals("minecraft:killed_by") ? "_SPAWN_EGG" : ""));
        return material != null && material.isItem() && !material.isAir() ? material : Material.PAPER;
    }
    public boolean hasLabel(StatKey key) { return labels.containsKey(key.id()); }
}
