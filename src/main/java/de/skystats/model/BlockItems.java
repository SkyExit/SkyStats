package de.skystats.model;

import org.bukkit.Material;
import java.util.Set;

/** Vanilla stores placements under the used-item ID, including plants with different block IDs. */
public final class BlockItems {
    private static final Set<String> PLANTING_ITEMS = Set.of(
            "wheat_seeds", "beetroot_seeds", "melon_seeds", "pumpkin_seeds", "torchflower_seeds",
            "pitcher_pod", "cocoa_beans",
            "nether_wart", "redstone", "string");
    private BlockItems() {}

    public static boolean isPlacement(String id) {
        if (!id.startsWith("minecraft:")) return false;
        String path = id.substring("minecraft:".length());
        if (PLANTING_ITEMS.contains(path)) return true;
        Material material = Material.matchMaterial(path);
        return material != null && material.isBlock() && material.isItem() && !material.isAir();
    }
}
