package de.skystats.data;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.skystats.model.StatKey;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class StatsFileReader {
    private final long maxBytes;
    private final int maxEntries;
    public StatsFileReader(long maxBytes, int maxEntries) { this.maxBytes = maxBytes; this.maxEntries = maxEntries; }

    public Map<StatKey, Long> read(Path file) throws IOException {
        if (Files.size(file) > maxBytes) throw new IOException("Statistics file exceeds size limit");
        Map<StatKey, Long> values = new HashMap<>();
        int count = 0;
        try (JsonReader reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("stats")) { reader.skipValue(); continue; }
                reader.beginObject();
                while (reader.hasNext()) {
                    String type = reader.nextName();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        if (++count > maxEntries) throw new IOException("Statistics entry limit exceeded");
                        String id = reader.nextName();
                        if (reader.peek() != JsonToken.NUMBER) { reader.skipValue(); continue; }
                        String raw = reader.nextString();
                        try {
                            long value = new BigDecimal(raw).longValueExact();
                            if (value > 0) values.merge(new StatKey(type, id), value, Math::max);
                        } catch (ArithmeticException | IllegalArgumentException ignored) { /* Invalid individual counters are ignored. */ }
                    }
                    reader.endObject();
                }
                reader.endObject();
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("Trailing JSON data");
        } catch (IllegalStateException exception) { throw new IOException("Invalid statistics JSON", exception); }
        return values;
    }
}
