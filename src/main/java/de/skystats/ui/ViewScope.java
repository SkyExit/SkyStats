package de.skystats.ui;

import org.bukkit.Material;
import java.util.UUID;

public enum ViewScope {
    PERSONAL("Your statistics", Material.EMERALD),
    OTHER("Player statistics", Material.DIAMOND),
    SERVER("Server statistics", Material.GOLD_INGOT);

    private final String label;
    private final Material border;
    ViewScope(String label, Material border) { this.label = label; this.border = border; }
    public String label() { return label; }
    public Material border() { return border; }
    public static ViewScope of(Route route, UUID viewer, UUID target) {
        if (route.serverView()) return SERVER;
        UUID subject = route.subject() == null ? target : route.subject();
        return viewer.equals(subject) ? PERSONAL : OTHER;
    }
}
