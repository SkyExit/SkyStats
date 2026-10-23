package de.skystats.ui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.SkullMeta;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/** Custom heads use embedded texture URLs and public profile APIs only. */
public final class HeadFactory {
    public static final String LEFT_TEXTURE = "a185c97dbb8353de652698d24b64327b793a3f32a98be67b719fbedab35e";
    public static final String RIGHT_TEXTURE = "31c0ededd7115fc1b23d51ce966358b27195daf26ebb6e45a66c34c69c34091";
    private final Method createProfile, getTextures, setSkin, setOwner;
    private final boolean modern;
    public HeadFactory() {
        try {
            Method factory;
            try { factory = Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class); }
            catch (NoSuchMethodException ignored) { factory = null; }
            modern = factory != null;
            if (modern) {
                createProfile = factory;
                Class<?> profile = factory.getReturnType();
                getTextures = profile.getMethod("getTextures");
                setSkin = getTextures.getReturnType().getMethod("setSkin", java.net.URL.class);
                setOwner = SkullMeta.class.getMethod("setOwnerProfile", profile);
            } else {
                createProfile = Bukkit.class.getMethod("createProfile", UUID.class, String.class);
                getTextures = null; setSkin = null;
                setOwner = SkullMeta.class.getMethod("setPlayerProfile", createProfile.getReturnType());
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("No compatible public skull profile API", error); }
    }
    public ItemStack arrow(boolean right) {
        String texture = right ? RIGHT_TEXTURE : LEFT_TEXTURE;
        return head(UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)), "SkyStatsArrow", texture);
    }
    public ItemStack player(UUID uuid, String name) { return head(uuid, name.matches("[A-Za-z0-9_]{1,16}") ? name : "Unknown", null); }
    private ItemStack head(UUID uuid, String name, String texture) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        try {
            Object profile = createProfile.invoke(null, uuid, name);
            if (texture != null) {
                String url = "https://textures.minecraft.net/texture/" + texture;
                if (modern) setSkin.invoke(getTextures.invoke(profile), URI.create(url).toURL());
                else {
                    Class<?> property = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
                    String encoded = Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}").getBytes(StandardCharsets.UTF_8));
                    Object value = property.getConstructor(String.class, String.class).newInstance("textures", encoded);
                    createProfile.getReturnType().getMethod("setProperty", property).invoke(profile, value);
                }
            }
            setOwner.invoke(meta, profile);
            item.setItemMeta(meta);
            return item;
        } catch (ReflectiveOperationException | java.net.MalformedURLException error) { throw new IllegalStateException("Cannot create textured player head", error); }
    }
}
