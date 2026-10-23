package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.misc.SkyLogger;
import de.laurinhummel.skystats.misc.SkyStrings;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

abstract class PlayerCommand implements CommandExecutor {
    private final Plugin plugin;
    private final PlayerScheduler scheduler;
    private final String permission;

    protected PlayerCommand(Plugin plugin, PlayerScheduler scheduler, String permission) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.permission = permission;
    }

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(SkyStrings.NOTINCONSOLE);
            return true;
        }
        scheduler.run(player, () -> {
            if (!player.hasPermission(permission)) {
                SkyLogger.sendPlayer(player, SkyStrings.NOPERMISSION, SkyLogger.LogType.ERROR);
                return;
            }
            execute(player, args);
        }).whenComplete((ignored, failure) -> {
            if (failure != null && plugin.isEnabled()
                    && !(failure instanceof PlayerScheduler.PlayerUnavailableException)) {
                plugin.getLogger().log(Level.SEVERE, "Could not execute /" + label, failure);
            }
        });
        return true;
    }

    protected abstract void execute(Player player, String[] args);

    protected void error(Player player, String message) {
        SkyLogger.sendPlayer(player, message, SkyLogger.LogType.ERROR);
    }
}
