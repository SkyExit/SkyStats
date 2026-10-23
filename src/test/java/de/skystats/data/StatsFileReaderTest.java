package de.skystats.data;

import de.skystats.model.StatKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class StatsFileReaderTest {
    @TempDir Path directory;
    private final StatsFileReader reader = new StatsFileReader(1024 * 1024, 1000);
    private Path file(String json) throws IOException { return Files.writeString(directory.resolve("stats.json"), json); }

    @Test void readsAllTypesAndPreservesUnknownIdentifiers() throws Exception {
        var values = reader.read(file("""
            {"DataVersion":2975,"stats":{"minecraft:mined":{"minecraft:stone":18492},
            "minecraft:broken":{"minecraft:iron_pickaxe":2},"minecraft:custom":{"minecraft:play_time":1200},
            "minecraft:future":{"minecraft:new_counter":42}}}
            """));
        assertEquals(4, values.size());
        assertEquals(18492L, values.get(new StatKey("minecraft:mined", "minecraft:stone")));
        assertEquals(2L, values.get(new StatKey("minecraft:broken", "minecraft:iron_pickaxe")));
        assertEquals(42L, values.get(new StatKey("minecraft:future", "minecraft:new_counter")));
    }
    @Test void aliasesPlayTimeWithoutDoubleCounting() throws Exception {
        var values = reader.read(file("{\"stats\":{\"minecraft:custom\":{\"minecraft:play_time\":2400,\"minecraft:play_one_minute\":1200}}}"));
        assertEquals(1, values.size()); assertEquals(2400L, values.values().iterator().next());
    }
    @Test void ignoresNegativeFractionalOverflowAndNonNumericValues() throws Exception {
        var values = reader.read(file("{\"stats\":{\"minecraft:custom\":{\"minecraft:a\":-5,\"minecraft:b\":2.3,\"minecraft:c\":9223372036854775808,\"minecraft:d\":\"x\",\"minecraft:e\":null,\"minecraft:jump\":5}}}"));
        assertEquals(1, values.size());
    }
    @Test void rejectsPartialJsonAndTrailingContent() throws Exception {
        assertThrows(IOException.class, () -> reader.read(file("{\"stats\":{")));
        assertThrows(IOException.class, () -> reader.read(file("{} {}")));
    }
    @Test void enforcesSizeAndEntryLimits() throws Exception {
        Path path = file("{\"stats\":{\"minecraft:custom\":{\"minecraft:a\":1,\"minecraft:b\":2}}}");
        assertThrows(IOException.class, () -> new StatsFileReader(5, 10).read(path));
        assertThrows(IOException.class, () -> new StatsFileReader(1000, 1).read(path));
    }
}
