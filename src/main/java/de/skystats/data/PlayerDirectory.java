package de.skystats.data;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDirectory {
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, String> liveNames = new ConcurrentHashMap<>();
    private final Path persisted;
    private volatile Map<UUID, String> known = Map.of();
    public PlayerDirectory(Path persisted) { this.persisted = persisted; }

    public void remember(UUID uuid, String name) {
        if (name != null && name.matches("[A-Za-z0-9_]{1,16}")) { names.put(uuid, name); liveNames.put(uuid, name); }
    }

    public void load(Path userCache, List<String> warnings) {
        readNames(persisted, false, warnings);
        readNames(userCache, true, warnings);
        names.putAll(liveNames);
    }
    private void readNames(Path path, boolean array, List<String> warnings) {
        if (!Files.isRegularFile(path)) return;
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (array) {
                for (JsonElement element : root.getAsJsonArray()) {
                    JsonObject entry = element.getAsJsonObject();
                    putName(entry.get("uuid").getAsString(), entry.get("name").getAsString());
                }
            } else {
                root.getAsJsonObject().entrySet().forEach(entry -> putName(entry.getKey(), entry.getValue().getAsString()));
            }
        } catch (IOException | RuntimeException error) { warnings.add("Namensdatei konnte nicht gelesen werden: " + path.getFileName()); }
    }
    private void putName(String uuid, String name) {
        try { if (name.matches("[A-Za-z0-9_]{1,16}")) names.put(UUID.fromString(uuid), name); }
        catch (IllegalArgumentException ignored) { /* A malformed identity never changes another UUID. */ }
    }
    public String name(UUID uuid) { return names.getOrDefault(uuid, uuid.toString().substring(0, 8)); }
    public void publish(Set<UUID> uuids) {
        Map<UUID, String> copy = new HashMap<>(); uuids.forEach(uuid -> copy.put(uuid, name(uuid))); known = Map.copyOf(copy);
    }
    private Map<UUID, String> searchable() {
        Map<UUID, String> result = new HashMap<>(known);
        liveNames.forEach(result::put);
        return result;
    }
    public List<UUID> find(String input) {
        Map<UUID, String> searchable = searchable();
        try { UUID uuid = UUID.fromString(input); return searchable.containsKey(uuid) ? List.of(uuid) : List.of(); }
        catch (IllegalArgumentException ignored) { return searchable.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(input)).map(Map.Entry::getKey).sorted().toList(); }
    }
    public List<String> complete(String prefix) {
        return searchable().values().stream().filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).distinct().sorted(String.CASE_INSENSITIVE_ORDER).limit(100).toList();
    }
    public void save() throws IOException {
        Files.createDirectories(persisted.getParent());
        Path temp = persisted.resolveSibling(persisted.getFileName() + ".tmp");
        Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(new TreeMap<>(names)), StandardCharsets.UTF_8);
        try { Files.move(temp, persisted, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, persisted, StandardCopyOption.REPLACE_EXISTING); }
    }
}
