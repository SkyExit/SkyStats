package de.laurinhummel.skystats.misc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SkyDataFormatterTest {
    @ParameterizedTest
    @CsvSource({"0,00m 00s", "1199,00m 59s", "1200,01m 00s", "72000,01h 00m 00s",
            "143980,01h 59m 59s", "144000,02h 00m 00s", "-20,00m 00s"})
    void formatsTickBoundaries(long ticks, String expected) {
        assertEquals(expected, SkyDataFormatter.formatTicks(ticks));
    }

    @Test
    void doesNotOverflowForLongDurations() {
        assertEquals("596523h 14m 08s", SkyDataFormatter.formatTicks(42_949_672_960L));
        assertEquals(3600, SkyDataFormatter.formatTicksSeconds(72000));
    }
}
