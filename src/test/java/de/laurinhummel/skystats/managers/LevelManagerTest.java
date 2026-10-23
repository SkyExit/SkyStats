package de.laurinhummel.skystats.managers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class LevelManagerTest {
    @ParameterizedTest
    @CsvSource({"0,1,0,220", "219,1,0,220", "220,2,220,460", "459,2,220,460",
            "460,3,460,720", "719,3,460,720", "720,4,720,1000", "1000,5,1000,1300"})
    void levelBoundaries(long exp, int level, long min, long max) {
        LevelManager manager = new LevelManager(exp);
        assertEquals(level, manager.getLevel());
        assertEquals(min, manager.getMinEXP());
        assertEquals(max, manager.getMaxEXP());
        assertTrue(manager.getPercentage() >= 0 && manager.getPercentage() <= 100);
        assertDoesNotThrow(() -> manager.getProgressBar(15));
    }

    @Test
    void playerLevelsDoNotOverwriteEachOther() {
        LevelManager first = new LevelManager(110);
        LevelManager second = new LevelManager(1000);
        assertEquals(1, first.getLevel());
        assertEquals(50, first.getPercentage());
        assertEquals(5, second.getLevel());
        assertEquals(0, second.getPercentage());
    }

    @Test
    void handlesLargeAndInvalidValues() {
        for (long exp : new long[]{Integer.MAX_VALUE, 2_000_000_000_000L, Long.MAX_VALUE}) {
            LevelManager manager = new LevelManager(exp);
            assertTrue(manager.getLevel() > 1);
            assertTrue(manager.getMinEXP() <= exp);
            assertTrue(manager.getMaxEXP() >= exp);
            assertTrue(manager.getLevelDuration() > 0);
            assertDoesNotThrow(() -> manager.getProgressBar(0));
            assertDoesNotThrow(() -> manager.getProgressBar(15));
        }
        assertEquals(0, new LevelManager(-5).getLevelProgress());
        assertThrows(IllegalArgumentException.class, () -> new LevelManager(0).getProgressBar(-1));
    }
}
