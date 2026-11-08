package de.skystats.ui;

import de.skystats.data.StatCatalog;
import de.skystats.model.*;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StatisticPresentationTest {
    private final Labels labels = new Labels();
    private static StatKey key(String type, String id) { return new StatKey("minecraft:" + type, "minecraft:" + id); }

    @Test void everyApiStatisticHasExactlyOneCategoryAndEveryCustomStatisticHasADedicatedIcon() {
        for (StatKey key : new StatCatalog().keys()) {
            assertEquals(1, Arrays.stream(Category.values()).filter(category -> category.contains(key)).count(), key.technicalName());
            if (key.type().equals("minecraft:custom")) {
                assertTrue(labels.hasLabel(key), key.technicalName());
                assertNotEquals(Material.PAPER, labels.icon(key), key.technicalName());
            }
        }
    }

    @Test void placementsAndItemUsesNeverOverlapIncludingPlantingAndDualPurposeItems() {
        for (String id : List.of("stone", "oak_planks", "oak_sign", "wheat_seeds", "cocoa_beans", "redstone", "string")) {
            assertTrue(Category.PLACED.contains(key("used", id)), id);
            assertFalse(Category.USED.contains(key("used", id)), id);
        }
        for (String id : List.of("diamond_pickaxe", "flint_and_steel", "water_bucket", "bow", "apple", "carrot", "potato", "glow_berries", "sweet_berries", "oak_boat", "armor_stand")) {
            assertTrue(Category.USED.contains(key("used", id)), id);
            assertFalse(Category.PLACED.contains(key("used", id)), id);
        }
        assertTrue(Category.ACTIVITIES.contains(new StatKey("future:unknown", "future:entry")));
    }

    @Test void knownMinedBlocksAlwaysHaveARepresentativeInventoryItem() {
        List<StatKey> missing = new StatCatalog().keys().stream()
                .filter(key -> key.type().equals("minecraft:mined"))
                .filter(key -> labels.icon(key) == Material.PAPER).sorted().toList();
        assertEquals(List.of(), missing);
    }

    @Test void allTimeStatisticsIncludeRestAndDeathAndPlayerEntitiesAreIncluded() {
        for (String id : List.of("play_time", "total_world_time", "time_since_death", "time_since_rest", "sneak_time")) {
            assertTrue(Category.PLAY_TIME.contains(key("custom", id)), id);
        }
        for (String type : List.of("killed", "killed_by")) {
            assertEquals(1, Arrays.stream(Category.values()).filter(category -> category.contains(key(type, "player"))).count());
        }
        assertTrue(Category.KILLED.contains(key("custom", "player_kills")));
        assertTrue(Category.KILLED_BY.contains(PlayerStats.UNKNOWN_DEATHS));
        assertEquals("Unknown / environmental deaths", labels.name(PlayerStats.UNKNOWN_DEATHS));
        assertEquals(Material.FEATHER, labels.icon(PlayerStats.UNKNOWN_DEATHS));
        assertFalse(new StatCatalog().keys().contains(key("killed", "player")));
    }

    @Test void iconsRepresentMovementInteractionsEntitiesAndNonItemBlocks() {
        assertEquals(Material.STRIDER_SPAWN_EGG, labels.icon(key("custom", "strider_one_cm")));
        assertEquals(Material.ELYTRA, labels.icon(key("custom", "fly_one_cm")));
        assertEquals(Material.ELYTRA, labels.icon(key("custom", "aviate_one_cm")));
        assertEquals(Material.MINECART, labels.icon(key("custom", "minecart_one_cm")));
        assertEquals(Material.SMITHING_TABLE, labels.icon(key("custom", "interact_with_smithing_table")));
        assertEquals(Material.DRAGON_HEAD, labels.icon(key("killed", "ender_dragon")));
        assertEquals(Material.WITHER_SKELETON_SKULL, labels.icon(key("killed_by", "wither")));
        assertEquals(Material.WATER_BUCKET, labels.icon(key("mined", "water")));
        assertEquals(Material.OAK_SIGN, labels.icon(key("mined", "oak_wall_sign")));
        assertEquals(Material.PAPER, labels.icon(key("mined", "unknown_future_block")));
        assertTrue(labels.usesItemName(key("mined", "stone")));
        assertTrue(labels.usesItemName(key("killed", "zombie")));
        assertFalse(labels.usesItemName(key("custom", "strider_one_cm")));
        assertFalse(labels.usesItemName(key("killed", "iron_golem")));
        assertEquals("Iron golem", labels.name(key("killed", "iron_golem")));
    }

    @Test void contentAndCategoriesLeaveScopeBordersAndNavigationFree() {
        Set<Integer> slots = new HashSet<>();
        for (int i = 0; i < Page.CONTENT_SIZE; i++) {
            int slot = MenuLayout.contentSlot(i);
            assertTrue(slot < 45 && slot % 9 != 0 && slot % 9 != 8);
            assertTrue(slots.add(slot));
        }
        Set<Integer> categories = new HashSet<>();
        for (Category category : Category.values()) {
            int slot = MenuLayout.slot(category);
            assertTrue(slots.contains(slot));
            assertNotEquals(MenuLayout.CENTER, slot);
            assertTrue(categories.add(slot));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> MenuLayout.contentSlot(35));
    }

    @Test void scopesFollowViewedSubjectAndServerRoutes() {
        UUID viewer = UUID.randomUUID(), target = UUID.randomUUID();
        assertEquals(ViewScope.PERSONAL, ViewScope.of(Route.main(), viewer, viewer));
        assertEquals(ViewScope.OTHER, ViewScope.of(Route.main(), viewer, target));
        assertEquals(ViewScope.SERVER, ViewScope.of(Route.main().child(Route.Mode.MEDALS, null, null, null), viewer, target));
        assertEquals(ViewScope.PERSONAL, ViewScope.of(Route.main().child(Route.Mode.PROFILE, null, null, viewer), viewer, target));
    }
}
