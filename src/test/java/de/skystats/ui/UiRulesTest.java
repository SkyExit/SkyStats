package de.skystats.ui;

import de.skystats.model.StatKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiRulesTest {
    @Test void formatsTicksCentimetersAndDamage() {
        assertEquals("12 Tage, 6 Stunden, 14 Minuten", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:play_time"), (12 * 1440L + 6 * 60 + 14) * 1200));
        assertEquals("12,34 m (Blöcke)", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:walk_one_cm"), 1234));
        assertEquals("1,5 km", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:walk_one_cm"), 150000));
        assertEquals("2,5 Schadenspunkte", ValueFormatter.format(new StatKey("minecraft:custom", "minecraft:damage_taken"), 25));
    }
    @Test void paginationNeverUsesNavigationSlots() {
        assertEquals(new Page(0, 1, 0, 0), Page.of(0, 99));
        assertEquals(new Page(0, 1, 0, 45), Page.of(45, 0));
        assertEquals(new Page(1, 2, 45, 46), Page.of(46, 1));
        assertEquals(new Page(2, 3, 90, 91), Page.of(91, 99));
        assertFalse(Page.of(46, 0).hasPrevious()); assertTrue(Page.of(46, 0).hasNext());
        assertFalse(Page.of(46, 1).hasNext()); assertTrue(Page.of(46, 1).hasPrevious());
    }
    @Test void germanLabelsAreLoadedWithNamespacedKeys() { assertEquals("Sprünge", new Labels().name(new StatKey("minecraft:custom", "minecraft:jump"))); }
}
