package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.menus.StatsMenuService;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class CompareCommand extends PlayerCommand {
    private final StatsMenuService menus;

    public CompareCommand(Plugin plugin, PlayerScheduler scheduler, StatsMenuService menus) {
        super(plugin, scheduler, "skystats.compare");
        this.menus = menus;
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length != 2) {
            error(player, "Usage: /compare <player1> <player2>");
            return;
        }
        Player first = Bukkit.getPlayerExact(args[0]);
        Player second = Bukkit.getPlayerExact(args[1]);
        if (first == null || second == null) {
            error(player, "Player '" + (first == null ? args[0] : args[1]) + "' is not online.");
            return;
        }
        menus.openComparison(player, first, second);
    }
}
