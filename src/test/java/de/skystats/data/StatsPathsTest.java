package de.skystats.data;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class StatsPathsTest {
    @Test void handlesClassicAndModernDimensionLayoutsExactlyOnce() {
        assertEquals(Path.of("/server/world/stats"), StatsPaths.primaryStatistics(Path.of("/server/world")));
        assertEquals(Path.of("/server/world/stats"), StatsPaths.primaryStatistics(Path.of("/server/world/dimensions/minecraft/overworld")));
        assertEquals(Path.of("/server/world/stats"), StatsPaths.primaryStatistics(Path.of("/server/world/dimensions/minecraft/the_nether")));
    }
}
