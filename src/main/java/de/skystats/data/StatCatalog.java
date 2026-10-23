package de.skystats.data;

import de.skystats.model.StatKey;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import java.util.*;

public final class StatCatalog {
    public record Entry(StatKey key, Statistic statistic, Material material, EntityType entity) {
        public long read(Player player) {
            if (material != null) return player.getStatistic(statistic, material);
            if (entity != null) return player.getStatistic(statistic, entity);
            return player.getStatistic(statistic);
        }
    }
    private static final Map<String, String> CUSTOM_IDS = Map.ofEntries(
            Map.entry("PLAY_ONE_MINUTE", "play_time"), Map.entry("DROP_COUNT", "drop"),
            Map.entry("TALKED_TO_VILLAGER", "talk_to_villager"), Map.entry("TRADED_WITH_VILLAGER", "traded_with_villager"),
            Map.entry("CAKE_SLICES_EATEN", "eat_cake_slice"), Map.entry("CAULDRON_FILLED", "fill_cauldron"),
            Map.entry("CAULDRON_USED", "use_cauldron"), Map.entry("ARMOR_CLEANED", "clean_armor"),
            Map.entry("BANNER_CLEANED", "clean_banner"), Map.entry("BREWINGSTAND_INTERACTION", "interact_with_brewingstand"),
            Map.entry("BEACON_INTERACTION", "interact_with_beacon"), Map.entry("DROPPER_INSPECTED", "inspect_dropper"),
            Map.entry("HOPPER_INSPECTED", "inspect_hopper"), Map.entry("DISPENSER_INSPECTED", "inspect_dispenser"),
            Map.entry("NOTEBLOCK_PLAYED", "play_noteblock"), Map.entry("NOTEBLOCK_TUNED", "tune_noteblock"),
            Map.entry("FLOWER_POTTED", "pot_flower"), Map.entry("TRAPPED_CHEST_TRIGGERED", "trigger_trapped_chest"),
            Map.entry("ENDERCHEST_OPENED", "open_enderchest"), Map.entry("ITEM_ENCHANTED", "enchant_item"),
            Map.entry("RECORD_PLAYED", "play_record"), Map.entry("FURNACE_INTERACTION", "interact_with_furnace"),
            Map.entry("CRAFTING_TABLE_INTERACTION", "interact_with_crafting_table"), Map.entry("CHEST_OPENED", "open_chest"),
            Map.entry("SHULKER_BOX_OPENED", "open_shulker_box"));
    private static final Map<String, String> TYPED_IDS = Map.of(
            "MINE_BLOCK", "mined", "CRAFT_ITEM", "crafted", "USE_ITEM", "used", "BREAK_ITEM", "broken",
            "PICKUP", "picked_up", "DROP", "dropped", "KILL_ENTITY", "killed", "ENTITY_KILLED_BY", "killed_by");
    private final List<Entry> entries;
    private final Set<StatKey> keys;

    public StatCatalog() {
        List<Entry> collected = new ArrayList<>();
        for (Statistic statistic : Statistic.values()) {
            if (statistic.getType() == Statistic.Type.UNTYPED) {
                String id = CUSTOM_IDS.getOrDefault(statistic.name(), statistic.getKey().getKey());
                collected.add(new Entry(new StatKey("minecraft:custom", "minecraft:" + id), statistic, null, null));
                continue;
            }
            String type = TYPED_IDS.getOrDefault(statistic.name(), statistic.getKey().getKey());
            if (statistic.getType() == Statistic.Type.ENTITY) {
                for (EntityType entity : EntityType.values()) {
                    if (entity == EntityType.UNKNOWN || entity == EntityType.PLAYER || entity.getKey() == null) continue;
                    collected.add(new Entry(new StatKey("minecraft:" + type, entity.getKey().toString()), statistic, null, entity));
                }
            } else {
                for (Material material : Material.values()) {
                    if (material.isLegacy() || material.isAir()) continue;
                    if (statistic.isBlock() ? !material.isBlock() : !material.isItem()) continue;
                    if (statistic.name().equals("BREAK_ITEM") && material.getMaxDurability() <= 0) continue;
                    collected.add(new Entry(new StatKey("minecraft:" + type, material.getKey().toString()), statistic, material, null));
                }
            }
        }
        entries = List.copyOf(collected);
        Set<StatKey> distinct = new HashSet<>();
        entries.forEach(entry -> distinct.add(entry.key()));
        keys = Set.copyOf(distinct);
    }
    public List<Entry> entries() { return entries; }
    public Set<StatKey> keys() { return keys; }
}
