package de.laurinhummel.skystats.commands;

import de.laurinhummel.skystats.managers.LevelManager;
import de.laurinhummel.skystats.managers.StatisticsManager;
import de.laurinhummel.skystats.misc.SkyStrings;
import de.laurinhummel.skystats.scheduling.PlayerScheduler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class LevelCommand extends PlayerCommand {
    public LevelCommand(Plugin plugin, PlayerScheduler scheduler) {
        super(plugin, scheduler, "skystats.level");
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length != 0) {
            error(player, "Usage: /level");
            return;
        }
        LevelManager level = new LevelManager(new StatisticsManager(player::getStatistic).getTotalEXP());
        player.sendMessage(SkyStrings.SKYSTATS + "----------------------------");
        player.sendMessage(SkyStrings.SKYSTATS + "Level: " + level.getLevel());
        player.sendMessage(SkyStrings.SKYSTATS + "Exp: " + level.getProgressBar(13));
        player.sendMessage(SkyStrings.SKYSTATS + "----------------------------");
    }
}
