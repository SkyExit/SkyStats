package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.menus.StatsMenuService;
import de.laurinhummel.skystats.misc.SkyStrings;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StatsCommand extends PlayerCommand {
    private final StatsMenuService menus;

    public StatsCommand(Plugin plugin, PlayerScheduler scheduler, StatsMenuService menus) {
        super(plugin, scheduler, "skystats.stats");
        this.menus = menus;
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length > 1) {
            error(player, "Usage: /stats [player]");
            return;
        }
        Player target = args.length == 0 ? player : Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            error(player, "Player '" + args[0] + "' is not online.");
            return;
        }
        if (!player.getUniqueId().equals(target.getUniqueId()) && !player.hasPermission("skystats.stats.others")) {
            error(player, SkyStrings.NOPERMISSION);
            return;
        }
        menus.openStats(player, target);
    }
}
