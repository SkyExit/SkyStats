package de.skystats.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class StatsPathsTest {
    @TempDir Path root;

    @Test void handlesClassicStorage() throws Exception {
        Files.createDirectories(root.resolve("stats"));
        assertEquals(root.resolve("stats"), StatsPaths.primaryStatistics(root));
    }
    @Test void handlesModernDimensionsAndCustomNamespaces() {
        for (String dimension : new String[] {"minecraft/overworld", "minecraft/the_nether", "custom/moon"}) {
            assertEquals(root.resolve("players/stats"), StatsPaths.primaryStatistics(root.resolve("dimensions/" + dimension)));
        }
    }
    @Test void detectsModernStorageFromWorldRootAndIgnoresObsoleteLegacyFolder() throws Exception {
        Files.createDirectories(root.resolve("stats"));
        Files.createDirectories(root.resolve("players/stats"));
        assertEquals(root.resolve("players/stats"), StatsPaths.primaryStatistics(root));
    }
    @Test void selectsModernStorageBeforeFirstStatsFileExists() throws Exception {
        Files.createDirectories(root.resolve("players/data"));
        assertEquals(root.resolve("players/stats"), StatsPaths.primaryStatistics(root));
    }
}
