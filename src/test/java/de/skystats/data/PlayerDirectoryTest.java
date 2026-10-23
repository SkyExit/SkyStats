package de.skystats.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayerDirectoryTest {
    @TempDir Path root;
    @Test void renamesKeepIdentityAndNeverMergeSameNameUuids() throws Exception {
        UUID original = UUID.randomUUID(), other = UUID.randomUUID();
        Path persisted = root.resolve("players.json");
        PlayerDirectory names = new PlayerDirectory(persisted);
        names.remember(original, "OldName"); names.publish(Set.of(original)); names.save();
        PlayerDirectory reloaded = new PlayerDirectory(persisted);
        reloaded.load(root.resolve("missing.json"), new ArrayList<>());
        reloaded.remember(original, "NewName"); reloaded.remember(other, "NewName"); reloaded.publish(Set.of(original, other));
        assertTrue(reloaded.find("OldName").isEmpty()); assertEquals(2, reloaded.find("NewName").size());
        assertEquals(List.of(original), reloaded.find(original.toString()));
        assertEquals(List.of("NewName"), reloaded.complete("new"));
    }
    @Test void preservesUnknownUuidsForDirectLookup() {
        UUID uuid = UUID.randomUUID(); PlayerDirectory names = new PlayerDirectory(root.resolve("players.json"));
        names.publish(Set.of(uuid)); assertEquals(List.of(uuid), names.find(uuid.toString()));
    }
}
