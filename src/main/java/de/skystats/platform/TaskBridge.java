package de.skystats.platform;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Uses public scheduler APIs reflectively to keep Java 17 / Spigot 1.18 linkage. */
public final class TaskBridge {
    private final Plugin plugin;
    private final Method entityScheduler;
    private final Method entityRun;
    private final Method entityDelayed;
    private final Method globalGetter;
    private final Method globalRun;

    public TaskBridge(Plugin plugin) {
        this.plugin = plugin;
        Method getter = null, run = null, delayed = null, global = null, globalTask = null;
        try {
            getter = Entity.class.getMethod("getScheduler");
            Class<?> scheduler = getter.getReturnType();
            run = scheduler.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
            delayed = scheduler.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
            global = Bukkit.class.getMethod("getGlobalRegionScheduler");
            globalTask = global.getReturnType().getMethod("run", Plugin.class, Consumer.class);
        } catch (NoSuchMethodException ignored) {
            getter = null;
        }
        this.entityScheduler = getter; this.entityRun = run; this.entityDelayed = delayed;
        this.globalGetter = global; this.globalRun = globalTask;
        if (isFolia() && getter == null) throw new IllegalStateException("Folia scheduler API is unavailable");
    }

    public static boolean isFolia() {
        try { Class.forName("io.papermc.paper.threadedregions.RegionizedServer"); return true; }
        catch (ClassNotFoundException ignored) { return false; }
    }

    public void entity(Player player, Runnable action, Runnable retired) { entityLater(player, action, retired, 0); }
    public void entityLater(Player player, Runnable action, Runnable retired, long ticks) {
        if (!plugin.isEnabled()) { retired.run(); return; }
        try {
            Runnable guarded = () -> { if (plugin.isEnabled() && player.isOnline()) action.run(); else retired.run(); };
            if (entityScheduler != null) {
                Object scheduler = entityScheduler.invoke(player);
                Consumer<Object> task = ignored -> guarded.run();
                Object scheduled = ticks > 0 ? entityDelayed.invoke(scheduler, plugin, task, retired, ticks)
                        : entityRun.invoke(scheduler, plugin, task, retired);
                if (scheduled == null) retired.run();
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, guarded, Math.max(1, ticks));
            }
        } catch (ReflectiveOperationException | IllegalStateException exception) {
            retired.run();
            if (plugin.isEnabled()) plugin.getLogger().warning("Cannot schedule player task: " + exception);
        }
    }

    public void global(Runnable action) {
        if (!plugin.isEnabled()) return;
        try {
            if (entityScheduler != null) globalRun.invoke(globalGetter.invoke(null), plugin, (Consumer<Object>) ignored -> action.run());
            else Bukkit.getScheduler().runTask(plugin, action);
        } catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot schedule global task", exception); }
    }

    public CompletableFuture<Void> entityFuture(Player player, Runnable action) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        entity(player, () -> { try { action.run(); result.complete(null); } catch (Throwable error) { result.completeExceptionally(error); } },
                () -> result.completeExceptionally(new IllegalStateException("Player task retired")));
        return result;
    }
}
