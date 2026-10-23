package de.skystats.ui;

import de.skystats.model.StatKey;
import org.bukkit.Material;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

public final class Labels {
    private record Presentation(String name, Material icon) {}
    private final java.util.Map<String, Presentation> custom = new java.util.HashMap<>();
    public Labels() {
        try (var stream = Labels.class.getResourceAsStream("/statistics.properties")) {
            if (stream == null) throw new IllegalStateException("Missing statistic presentations");
            Properties labels = new Properties();
            labels.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            labels.forEach((key, value) -> {
                String[] parts = value.toString().split("\\|");
                Material icon = Material.PAPER;
                for (int i = 1; i < parts.length; i++) {
                    Material candidate = Material.matchMaterial(parts[i]);
                    if (usable(candidate)) { icon = candidate; break; }
                }
                custom.put("minecraft:" + key, new Presentation(parts[0], icon));
            });
        } catch (IOException error) { throw new IllegalStateException("Cannot load labels", error); }
    }
    public String name(StatKey key) {
        Presentation presentation = custom.get(key.id());
        if (key.type().equals("minecraft:custom") && presentation != null) return presentation.name();
        String path = key.id().substring(key.id().indexOf(':') + 1).replace('_', ' ');
        return Character.toUpperCase(path.charAt(0)) + path.substring(1);
    }
    public Material icon(StatKey key) {
        if (key.type().equals("minecraft:custom")) {
            Presentation presentation = custom.get(key.id());
            if (presentation != null) return presentation.icon();
            if (key.isTime()) return Material.CLOCK;
            if (key.isDistance()) return Material.LEATHER_BOOTS;
            if (key.isDamage()) return Material.IRON_CHESTPLATE;
            return Material.PAPER;
        }
        String path = key.id().substring(key.id().indexOf(':') + 1).toUpperCase(Locale.ROOT);
        boolean entity = key.type().equals("minecraft:killed") || key.type().equals("minecraft:killed_by");
        Material material = Material.matchMaterial(path + (entity ? "_SPAWN_EGG" : ""));
        if (usable(material)) return material;
        if (entity) return switch (path) {
            case "ENDER_DRAGON" -> Material.DRAGON_HEAD;
            case "WITHER" -> Material.WITHER_SKELETON_SKULL;
            case "IRON_GOLEM" -> Material.IRON_BLOCK;
            case "SNOW_GOLEM", "SNOWMAN" -> Material.CARVED_PUMPKIN;
            case "ARMOR_STAND" -> Material.ARMOR_STAND;
            case "GIANT" -> Material.ZOMBIE_HEAD;
            case "ILLUSIONER" -> Material.BOW;
            case "PLAYER" -> Material.PLAYER_HEAD;
            default -> Material.BONE;
        };
        Material replacement = Material.matchMaterial(switch (path) {
            case "WATER", "BUBBLE_COLUMN" -> "WATER_BUCKET";
            case "LAVA" -> "LAVA_BUCKET";
            case "POWDER_SNOW" -> "POWDER_SNOW_BUCKET";
            case "FROSTED_ICE" -> "ICE";
            case "TALL_SEAGRASS" -> "SEAGRASS";
            case "WATER_CAULDRON", "LAVA_CAULDRON", "POWDER_SNOW_CAULDRON" -> "CAULDRON";
            case "POTTED_AZALEA_BUSH" -> "AZALEA";
            case "POTTED_FLOWERING_AZALEA_BUSH" -> "FLOWERING_AZALEA";
            case "FIRE", "SOUL_FIRE" -> "FLINT_AND_STEEL";
            case "CARROTS" -> "CARROT";
            case "POTATOES" -> "POTATO";
            case "BEETROOTS" -> "BEETROOT_SEEDS";
            case "COCOA" -> "COCOA_BEANS";
            case "SWEET_BERRY_BUSH" -> "SWEET_BERRIES";
            case "CAVE_VINES", "CAVE_VINES_PLANT" -> "GLOW_BERRIES";
            case "PUMPKIN_STEM", "ATTACHED_PUMPKIN_STEM" -> "PUMPKIN_SEEDS";
            case "MELON_STEM", "ATTACHED_MELON_STEM" -> "MELON_SEEDS";
            case "REDSTONE_WIRE" -> "REDSTONE";
            case "TRIPWIRE" -> "STRING";
            case "PISTON_HEAD", "MOVING_PISTON" -> "PISTON";
            case "NETHER_PORTAL", "END_PORTAL", "END_GATEWAY" -> "ENDER_EYE";
            case "TORCHFLOWER_CROP" -> "TORCHFLOWER_SEEDS";
            case "PITCHER_CROP" -> "PITCHER_POD";
            case "BIG_DRIPLEAF_STEM" -> "BIG_DRIPLEAF";
            case "BAMBOO_SAPLING" -> "BAMBOO";
            default -> path.replaceFirst("^POTTED_", "").replace("CANDLE_CAKE", "CANDLE")
                    .replace("_CORAL_WALL_FAN", "_CORAL_FAN").replace("_WALL_HANGING_SIGN", "_HANGING_SIGN")
                    .replace("_WALL_SIGN", "_SIGN").replace("_WALL_BANNER", "_BANNER")
                    .replace("_WALL_HEAD", "_HEAD").replace("_WALL_SKULL", "_SKULL")
                    .replace("_WALL_TORCH", "_TORCH").replace("WALL_TORCH", "TORCH")
                    .replace("KELP_PLANT", "KELP").replace("_VINES_PLANT", "_VINES");
        });
        return usable(replacement) ? replacement : Material.PAPER;
    }
    public boolean hasLabel(StatKey key) { return key.type().equals("minecraft:custom") && custom.containsKey(key.id()); }
    public boolean usesItemName(StatKey key) {
        if (key.type().equals("minecraft:custom")) return false;
        String path = key.id().substring(key.id().indexOf(':') + 1);
        if (key.type().equals("minecraft:killed") || key.type().equals("minecraft:killed_by")) {
            return usable(Material.matchMaterial(path + "_SPAWN_EGG"));
        }
        Material material = Material.matchMaterial(path);
        return usable(material) && material == icon(key);
    }
    private static boolean usable(Material material) { return material != null && material.isItem() && !material.isAir(); }
}
