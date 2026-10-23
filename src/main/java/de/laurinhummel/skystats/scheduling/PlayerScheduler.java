package de.laurinhummel.skystats.scheduling;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Uses Paper/Folia's entity scheduler when present, and Bukkit's on Spigot. */
public final class PlayerScheduler implements AutoCloseable {
    private final Plugin plugin;
    private final Method getScheduler;
    private final Method execute;
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public PlayerScheduler(Plugin plugin) {
        this(plugin, Entity.class);
    }

    PlayerScheduler(Plugin plugin, Class<?> entityType) {
        this.plugin = plugin;
        Method getter = null;
        Method executor = null;
        try {
            getter = entityType.getMethod("getScheduler");
            executor = getter.getReturnType().getMethod("execute", Plugin.class,
                    Runnable.class, Runnable.class, long.class);
        } catch (NoSuchMethodException exception) {
            if (getter != null) {
                throw new IllegalStateException("Unsupported entity scheduler API", exception);
            }
            // Plain Spigot does not expose Paper's scheduler API.
        }
        getScheduler = getter;
        execute = executor;
    }

    public <T> CompletableFuture<T> supply(Player player, Supplier<T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        pending.add(result);
        result.whenComplete((value, failure) -> pending.remove(result));
        if (closed || !plugin.isEnabled()) {
            result.completeExceptionally(new IllegalStateException("SkyStats is disabled"));
            return result;
        }
        Runnable retired = () -> result.completeExceptionally(new PlayerUnavailableException());
        Runnable task = () -> {
            if (result.isDone()) {
                return;
            }
            try {
                if (!player.isOnline()) {
                    retired.run();
                } else {
                    result.complete(action.get());
                }
            } catch (Exception exception) {
                result.completeExceptionally(exception);
            }
        };
        try {
            if (getScheduler == null) {
                plugin.getServer().getScheduler().runTask(plugin, task);
            } else if (!((boolean) execute.invoke(getScheduler.invoke(player), plugin, task, retired, 1L))) {
                retired.run();
            }
        } catch (InvocationTargetException exception) {
            result.completeExceptionally(exception.getCause());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }

    public CompletableFuture<Void> run(Player player, Runnable action) {
        return supply(player, () -> {
            action.run();
            return null;
        });
    }

    @Override
    public void close() {
        closed = true;
        pending.forEach(future -> future.completeExceptionally(new IllegalStateException("SkyStats is disabled")));
    }

    public static final class PlayerUnavailableException extends IllegalStateException {
        public PlayerUnavailableException() {
            super("The player is no longer available");
        }
    }
}
