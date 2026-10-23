package de.skystats.ui;

import de.skystats.model.StatKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiRulesTest {
    @Test void formatsTicksCentimetersAndDamage() {
        assertEquals("12d 6h 14m 0s", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:play_time"), (12 * 1440L + 6 * 60 + 14) * 1200));
        assertEquals("0d 0h 0m 3s", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:time_since_rest"), 60));
        assertEquals("12.34 m (blocks)", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:walk_one_cm"), 1234));
        assertEquals("1.5 km", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:walk_one_cm"), 150000));
        assertEquals("2.5 damage points", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:damage_taken"), 25));
    }
    @Test void paginationNeverUsesNavigationSlots() {
        assertEquals(new Page(0, 1, 0, 0), Page.of(0, 99));
        assertEquals(new Page(0, 1, 0, 35), Page.of(35, 0));
        assertEquals(new Page(1, 2, 35, 36), Page.of(36, 1));
        assertEquals(new Page(2, 3, 70, 71), Page.of(71, 99));
        assertFalse(Page.of(36, 0).hasPrevious()); assertTrue(Page.of(36, 0).hasNext());
        assertFalse(Page.of(36, 1).hasNext()); assertTrue(Page.of(36, 1).hasPrevious());
    }
    @Test void englishLabelsAreLoadedWithNamespacedKeys() { assertEquals("Jumps", new Labels().name(new StatKey("minecraft:custom", "minecraft:jump"))); }
}
