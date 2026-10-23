package de.skystats.data;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class PlayerDirectory {
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Map<UUID, String> liveNames = new ConcurrentHashMap<>();
    private final Path persisted;
    private final Function<UUID, String> offlineNames;
    private volatile Map<UUID, String> known = Map.of();
    public PlayerDirectory(Path persisted) { this(persisted, uuid -> null); }
    public PlayerDirectory(Path persisted, Function<UUID, String> offlineNames) {
        this.persisted = persisted; this.offlineNames = offlineNames;
    }

    public void remember(UUID uuid, String name) {
        if (validName(name)) { names.put(uuid, name); liveNames.put(uuid, name); }
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
                    if (!element.isJsonObject()) continue;
                    JsonObject entry = element.getAsJsonObject();
                    putName(entry.get("uuid"), entry.get("name"));
                }
            } else {
                root.getAsJsonObject().entrySet().forEach(entry -> putName(new JsonPrimitive(entry.getKey()), entry.getValue()));
            }
        } catch (IOException | RuntimeException error) { warnings.add("Could not read player name cache: " + path.getFileName()); }
    }
    private static boolean validName(String name) { return name != null && name.matches("[A-Za-z0-9_]{1,16}"); }
    private void putName(JsonElement uuid, JsonElement name) {
        if (uuid == null || !uuid.isJsonPrimitive() || name == null || !name.isJsonPrimitive()) return;
        try { if (validName(name.getAsString())) names.put(UUID.fromString(uuid.getAsString()), name.getAsString()); }
        catch (IllegalArgumentException ignored) { /* A malformed identity never changes another UUID. */ }
    }
    /** Called by the data worker because the server may read saved player data. */
    public void resolve(UUID uuid, List<String> warnings) {
        if (names.containsKey(uuid)) return;
        try {
            String name = offlineNames.apply(uuid);
            if (validName(name)) names.putIfAbsent(uuid, name);
        } catch (RuntimeException error) {
            warnings.add("Could not resolve player name: " + uuid);
        }
    }
    public String name(UUID uuid) { return names.getOrDefault(uuid, "Unknown (" + uuid.toString().substring(0, 8) + ")"); }
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
        return searchable().values().stream().filter(PlayerDirectory::validName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))).distinct().sorted(String.CASE_INSENSITIVE_ORDER).limit(100).toList();
    }
    public void save() throws IOException {
        Files.createDirectories(persisted.getParent());
        Path temp = persisted.resolveSibling(persisted.getFileName() + ".tmp");
        Files.writeString(temp, new GsonBuilder().setPrettyPrinting().create().toJson(new TreeMap<>(names)), StandardCharsets.UTF_8);
        try { Files.move(temp, persisted, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, persisted, StandardCopyOption.REPLACE_EXISTING); }
    }
}
