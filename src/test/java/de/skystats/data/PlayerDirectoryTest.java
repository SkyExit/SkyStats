package de.skystats.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
        assertEquals("Unknown (" + uuid.toString().substring(0, 8) + ")", names.name(uuid));
        assertTrue(names.complete("").isEmpty());
        assertTrue(names.find(uuid.toString().substring(0, 8)).isEmpty());
    }
    @Test void recoversAndPersistsOfflineNamesMissingFromBothCaches() throws Exception {
        UUID uuid = UUID.randomUUID();
        AtomicInteger lookups = new AtomicInteger();
        Path persisted = root.resolve("players.json");
        PlayerDirectory names = new PlayerDirectory(persisted, id -> {
            assertEquals(uuid, id); lookups.incrementAndGet(); return "OfflineUser";
        });
        List<String> warnings = new ArrayList<>();
        names.load(root.resolve("missing.json"), warnings);
        names.resolve(uuid, warnings); names.resolve(uuid, warnings); names.publish(Set.of(uuid)); names.save();
        assertEquals("OfflineUser", names.name(uuid));
        assertEquals(List.of(uuid), names.find("offlineuser"));
        assertEquals(List.of("OfflineUser"), names.complete("off"));
        assertEquals(1, lookups.get());
        PlayerDirectory reloaded = new PlayerDirectory(persisted);
        reloaded.load(root.resolve("missing.json"), warnings); reloaded.publish(Set.of(uuid));
        assertEquals("OfflineUser", reloaded.name(uuid));
        assertEquals(List.of(uuid), reloaded.find("OfflineUser"));
        assertTrue(warnings.isEmpty());
    }
    @Test void cacheAndJoinedNamesTakePrecedenceOverSavedOfflineProfiles() throws Exception {
        UUID uuid = UUID.randomUUID();
        Path persisted = root.resolve("players.json"), cache = root.resolve("usercache.json");
        Files.writeString(persisted, "{\"" + uuid + "\":\"OldName\"}");
        Files.writeString(cache, "[{\"uuid\":\"" + uuid + "\",\"name\":\"CachedName\"}]");
        PlayerDirectory names = new PlayerDirectory(persisted, id -> fail("Cached names must not require an offline lookup"));
        List<String> warnings = new ArrayList<>();
        names.load(cache, warnings); names.resolve(uuid, warnings);
        assertEquals("CachedName", names.name(uuid));
        names.remember(uuid, "JoinedName"); names.load(cache, warnings); names.resolve(uuid, warnings);
        assertEquals("JoinedName", names.name(uuid));
    }
    @Test void skipsMalformedCacheEntriesWithoutLosingFollowingOfflineNames() throws Exception {
        UUID uuid = UUID.randomUUID();
        Path persisted = root.resolve("players.json"), cache = root.resolve("usercache.json");
        Files.writeString(persisted, "{\"broken\":{},\"" + uuid + "\":\"PersistedName\"}");
        Files.writeString(cache, "[null,{},7,{\"uuid\":\"invalid\",\"name\":\"Broken\"},{\"uuid\":\"" + uuid + "\",\"name\":\"OfflineUser\"}]");
        PlayerDirectory names = new PlayerDirectory(persisted);
        List<String> warnings = new ArrayList<>();
        names.load(cache, warnings); names.publish(Set.of(uuid));
        assertEquals("OfflineUser", names.name(uuid));
        assertEquals(List.of(uuid), names.find("OfflineUser"));
        assertTrue(warnings.isEmpty());
    }
    @Test void missingOrInvalidOfflineNamesAreNotPersistedAsPlayerNames() throws Exception {
        UUID uuid = UUID.randomUUID();
        for (String result : Arrays.asList(null, "", "invalid name", uuid.toString())) {
            Path persisted = root.resolve("players.json");
            PlayerDirectory names = new PlayerDirectory(persisted, ignored -> result);
            List<String> warnings = new ArrayList<>();
            names.resolve(uuid, warnings); names.publish(Set.of(uuid)); names.save();
            assertTrue(names.name(uuid).startsWith("Unknown ("));
            assertEquals(List.of(uuid), names.find(uuid.toString()));
            assertTrue(names.complete("").isEmpty());
            assertEquals("{}", Files.readString(persisted));
            assertTrue(warnings.isEmpty());
        }
    }
    @Test void aConcurrentJoinWinsOverAnOlderOfflineName() {
        UUID uuid = UUID.randomUUID();
        PlayerDirectory[] directory = new PlayerDirectory[1];
        directory[0] = new PlayerDirectory(root.resolve("players.json"), ignored -> {
            directory[0].remember(uuid, "NewName");
            return "OldName";
        });
        directory[0].resolve(uuid, new ArrayList<>());
        assertEquals("NewName", directory[0].name(uuid));
    }
}
