package de.skystats.model;

public enum Category {
    MINED("Blocks mined", "IRON_PICKAXE", "Mined"),
    CRAFTED("Items crafted", "CRAFTING_TABLE", "Crafted"),
    USED("Items used", "FLINT_AND_STEEL", "Used"),
    PLACED("Blocks placed", "BRICKS", "Placed"),
    BROKEN("Items broken", "ANVIL", "Broken"),
    PICKED_UP("Items picked up", "HOPPER", "Picked up"),
    DROPPED("Items dropped", "DROPPER", "Dropped"),
    KILLED("Mobs killed", "IRON_SWORD", "Killed"),
    KILLED_BY("Killed by mobs", "SKELETON_SKULL", "Deaths caused"),
    MOVEMENT("Movement", "LEATHER_BOOTS", "Distance"),
    PLAY_TIME("Play time", "CLOCK", "Time"),
    COMBAT("Combat", "SHIELD", "Value"),
    INTERACTIONS("Interactions", "CHEST", "Interactions"),
    ACTIVITIES("Activities", "FISHING_ROD", "Value");

    private final String title, icon, valueLabel;
    Category(String title, String icon, String valueLabel) {
        this.title = title; this.icon = icon; this.valueLabel = valueLabel;
    }
    public String shortName() { return title; }
    public String title() { return title; }
    public String icon() { return icon; }
    public String valueLabel() { return valueLabel; }

    public boolean contains(StatKey key) { return forKey(key) == this; }

    /** Each statistic has exactly one category; player entities are never mob entries. */
    public static Category forKey(StatKey key) {
        return switch (key.type()) {
            case "minecraft:mined" -> MINED;
            case "minecraft:crafted" -> CRAFTED;
            case "minecraft:used" -> BlockItems.isPlacement(key.id()) ? PLACED : USED;
            case "minecraft:broken" -> BROKEN;
            case "minecraft:picked_up" -> PICKED_UP;
            case "minecraft:dropped" -> DROPPED;
            case "minecraft:killed" -> key.isPlayerEntity() ? null : KILLED;
            case "minecraft:killed_by" -> key.isPlayerEntity() ? null : KILLED_BY;
            case "minecraft:custom" -> {
                if (key.isDistance()) yield MOVEMENT;
                if (key.isTime()) yield PLAY_TIME;
                if (key.isDamage() || switch (key.id()) {
                    case "minecraft:deaths", "minecraft:mob_kills", "minecraft:player_kills",
                         "minecraft:raid_trigger", "minecraft:raid_win", "minecraft:target_hit" -> true;
                    default -> false;
                }) yield COMBAT;
                String path = key.id().substring(key.id().indexOf(':') + 1);
                if (path.startsWith("interact_with_") || path.startsWith("open_") || path.startsWith("inspect_")
                        || path.startsWith("clean_") || switch (path) {
                    case "talk_to_villager", "traded_with_villager", "fill_cauldron", "use_cauldron",
                         "play_noteblock", "tune_noteblock", "pot_flower", "trigger_trapped_chest",
                         "enchant_item", "play_record", "bell_ring" -> true;
                    default -> false;
                }) yield INTERACTIONS;
                yield ACTIVITIES;
            }
            default -> ACTIVITIES;
        };
    }
}
