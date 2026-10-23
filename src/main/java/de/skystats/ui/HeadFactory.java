package de.skystats.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/** Public profile APIs only; network lookups never run on a server tick. */
public final class HeadFactory implements AutoCloseable {
    // Minecraft Heads: Oak Wood Arrow Left / Right, IDs 2762 / 2763.
    public static final String LEFT_TEXTURE = "f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2";
    public static final String RIGHT_TEXTURE = "d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158";
    private record Identity(UUID uuid, String name) {}
    private static final class Cached {
        final CompletableFuture<Object> profile = new CompletableFuture<>();
        volatile long expiresAt = System.nanoTime() + TimeUnit.MINUTES.toNanos(30);
        Cached() {
            profile.whenComplete((value, error) -> expiresAt = System.nanoTime()
                    + TimeUnit.MINUTES.toNanos(value == null ? 1 : 30));
        }
    }
    private final Method createProfile, setOwner, getTextures, setTextures, setSkin, getSkin, update, complete, playerProfile;
    private final Class<?> profileType;
    private final Logger logger;
    private final Map<Identity, Cached> cache = new LinkedHashMap<>(64, .75f, true);
    private final ExecutorService executor;
    private volatile boolean closed;

    public HeadFactory(Logger logger) {
        this(logger, new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(64), runnable -> {
                    Thread thread = new Thread(runnable, "SkyStats-skins"); thread.setDaemon(true); return thread;
                }, new ThreadPoolExecutor.AbortPolicy()));
    }
    HeadFactory(Logger logger, ExecutorService executor) {
        this.logger = logger; this.executor = executor;
        try {
            Method factory, owner;
            // Paper's native profile API retains prefilled properties on current servers.
            try {
                factory = Bukkit.class.getMethod("createProfile", UUID.class, String.class);
                owner = SkullMeta.class.getMethod("setPlayerProfile", factory.getReturnType());
            } catch (NoSuchMethodException ignored) {
                factory = Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class);
                owner = SkullMeta.class.getMethod("setOwnerProfile", factory.getReturnType());
            }
            createProfile = factory; setOwner = owner; profileType = factory.getReturnType();
            Method textures = null, copy = null, skin = null, readSkin = null;
            try {
                textures = profileType.getMethod("getTextures");
                copy = profileType.getMethod("setTextures", textures.getReturnType());
                skin = textures.getReturnType().getMethod("setSkin", java.net.URL.class);
                readSkin = textures.getReturnType().getMethod("getSkin");
            } catch (NoSuchMethodException ignored) { /* Paper 1.18 uses ProfileProperty. */ }
            getTextures = textures; setTextures = copy; setSkin = skin; getSkin = readSkin;
            Method async = null, blocking = null;
            try { async = profileType.getMethod("update"); }
            catch (NoSuchMethodException ignored) { blocking = profileType.getMethod("complete", boolean.class, boolean.class); }
            update = async; complete = blocking;
            playerProfile = Player.class.getMethod("getPlayerProfile");
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("No compatible public skull profile API", error); }
    }

    public ItemStack arrow(boolean right) {
        String texture = right ? RIGHT_TEXTURE : LEFT_TEXTURE;
        try {
            // The texture and its stable UUID are sufficient; no invented player name.
            Object profile = createProfile.invoke(null, UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)), null);
            if (setTextures != null) {
                Object textures = getTextures.invoke(profile);
                setSkin.invoke(textures, URI.create("https://textures.minecraft.net/texture/" + texture).toURL());
                setTextures.invoke(profile, textures);
            } else {
                Class<?> property = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                String value = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/" + texture + "\"}}}").getBytes(StandardCharsets.UTF_8));
                profileType.getMethod("setProperty", property).invoke(profile, property.getConstructor(String.class, String.class).newInstance("textures", value));
            }
            return head(profile);
        } catch (ReflectiveOperationException | java.net.MalformedURLException error) { throw new IllegalStateException("Cannot create arrow head", error); }
    }

    /** Called on the player's entity thread; logged-in skins need no remote lookup. */
    public void remember(Player player) {
        if (closed) return;
        try {
            Object profile = playerProfile.invoke(player);
            if (!hasSkin(profile)) return;
            Identity identity = new Identity(player.getUniqueId(), validName(player.getName()));
            synchronized (cache) {
                Cached entry = cache.get(identity);
                if (entry != null) entry.profile.complete(profile);
                Cached fresh = new Cached(); fresh.profile.complete(profile);
                cache.put(identity, fresh); trimCache();
            }
        } catch (ReflectiveOperationException error) { logger.warning("Cannot cache player skin: " + error); }
    }

    public CompletableFuture<ItemStack> player(UUID uuid, String name) {
        Identity identity = new Identity(uuid, validName(name));
        Cached entry;
        synchronized (cache) {
            if (closed) return CompletableFuture.completedFuture(new ItemStack(Material.PLAYER_HEAD));
            entry = cache.get(identity);
            if (entry == null || System.nanoTime() - entry.expiresAt >= 0) {
                entry = new Cached();
                cache.put(identity, entry); trimCache();
                Cached pending = entry;
                try { executor.execute(() -> {
                    if (pending.profile.isDone() || closed) return;
                    pending.profile.completeOnTimeout(null, 15, TimeUnit.SECONDS);
                    try { pending.profile.complete(resolve(identity)); }
                    catch (InterruptedException error) { Thread.currentThread().interrupt(); pending.profile.complete(null); }
                    catch (Exception error) { pending.profile.complete(null); }
                }); } catch (RejectedExecutionException ignored) { pending.profile.complete(null); }
            }
        }
        return entry.profile.thenApply(profile -> {
            try { return head(profile); }
            catch (ReflectiveOperationException error) {
                logger.warning("Cannot apply player skin: " + error);
                return new ItemStack(Material.PLAYER_HEAD);
            }
        });
    }

    private Object resolve(Identity identity) throws Exception {
        // Offline-mode UUIDs cannot be queried at Mojang. Resolve their real skin by name.
        UUID lookupId = identity.uuid().version() == 3 && identity.name() != null ? null : identity.uuid();
        Object profile = createProfile.invoke(null, lookupId, identity.name());
        if (!hasSkin(profile)) {
            if (update != null) profile = ((CompletableFuture<?>) update.invoke(profile)).get(10, TimeUnit.SECONDS);
            else complete.invoke(profile, true, true);
        }
        if (!hasSkin(profile)) return null;
        Object owner = createProfile.invoke(null, identity.uuid(), identity.name());
        if (setTextures != null) setTextures.invoke(owner, getTextures.invoke(profile));
        else profileType.getMethod("setProperties", Collection.class).invoke(owner, profileType.getMethod("getProperties").invoke(profile));
        return owner;
    }

    private boolean hasSkin(Object profile) throws ReflectiveOperationException {
        if (profile == null) return false;
        if (getSkin == null) return (boolean) profileType.getMethod("hasTextures").invoke(profile);
        Object textures = getTextures.invoke(profile);
        return textures != null && getSkin.invoke(textures) != null;
    }
    private ItemStack head(Object profile) throws ReflectiveOperationException {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        if (profile != null) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            setOwner.invoke(meta, profile); item.setItemMeta(meta);
        }
        return item;
    }
    private static String validName(String name) { return name != null && name.matches("[A-Za-z0-9_]{1,16}") ? name : null; }
    private void trimCache() {
        while (cache.size() > 512) cache.remove(cache.keySet().iterator().next());
    }
    @Override public void close() {
        closed = true;
        executor.shutdownNow();
        synchronized (cache) { cache.values().forEach(entry -> entry.profile.complete(null)); cache.clear(); }
    }
}
