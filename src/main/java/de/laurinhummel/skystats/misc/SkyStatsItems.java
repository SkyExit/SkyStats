package de.laurinhummel.skystats.misc;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;

public class SkyStatsItems {
    public ItemStack getPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta placeholdermeta = placeholder.getItemMeta();
        assert placeholdermeta != null;
        placeholdermeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        placeholdermeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        placeholdermeta.setDisplayName(McColors.RED + "PLACEHOLDER");
        placeholder.setItemMeta(placeholdermeta);

        return placeholder;
    }

    public ItemStack getPlayerHead(OfflinePlayer player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);

        return head;
    }

    public ItemStack getPlayTimeStatistics(Player player) {
        ItemStack playTimeStatistics = new ItemStack(Material.CLOCK);
        ItemMeta playTimeStatisticsMeta = playTimeStatistics.getItemMeta();
        assert playTimeStatisticsMeta != null;
        playTimeStatisticsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        playTimeStatisticsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        playTimeStatisticsMeta.setDisplayName(McColors.AQUA + "Play Time Statistics");

        ArrayList<String> playTimeStatisticsLore = new ArrayList<>();
            playTimeStatisticsLore.add(McColors.GRAY + "Total world time: " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.TOTAL_WORLD_TIME)));
            playTimeStatisticsLore.add(McColors.GRAY + "Time since last death: " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.TIME_SINCE_DEATH)));
            playTimeStatisticsLore.add(McColors.GRAY + "Time since last rest: " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.TIME_SINCE_REST)));
            playTimeStatisticsLore.add(McColors.GRAY + "Total time sneaked: " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.SNEAK_TIME)));

        playTimeStatisticsMeta.setLore(playTimeStatisticsLore);
        playTimeStatistics.setItemMeta(playTimeStatisticsMeta);

        return playTimeStatistics;
    }

    public ItemStack getCombatStatistics(Player player) {
        ItemStack combatStatistics = new ItemStack(Material.IRON_SWORD);
        ItemMeta combatStatisticsMeta = combatStatistics.getItemMeta();
        assert combatStatisticsMeta != null;
        combatStatisticsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        combatStatisticsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        combatStatisticsMeta.setDisplayName(McColors.AQUA + "Combat Statistics");

        ArrayList<String> combatStatisticsLore = new ArrayList<>();
            combatStatisticsLore.add(McColors.GRAY + "Total damage dealt: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DAMAGE_DEALT)));
            combatStatisticsLore.add(McColors.GRAY + "Total damage taken: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DAMAGE_TAKEN)));
            combatStatisticsLore.add(McColors.GRAY + "Total player kills: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.PLAYER_KILLS)));
            combatStatisticsLore.add(McColors.GRAY + "Total mob kills: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.MOB_KILLS)));
            combatStatisticsLore.add(McColors.GRAY + "Total deaths: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DEATHS)));

        combatStatisticsMeta.setLore(combatStatisticsLore);
        combatStatistics.setItemMeta(combatStatisticsMeta);

        return combatStatistics;
    }

}
