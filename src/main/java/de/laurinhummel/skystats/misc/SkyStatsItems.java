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
        combatStatisticsMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        combatStatisticsMeta.setDisplayName(McColors.AQUA + "Combat Statistics"+ McColors.GRAY + " - "
                + McColors.GREEN + "Total");

        ArrayList<String> combatStatisticsLore = new ArrayList<>();
            combatStatisticsLore.add(McColors.GRAY + "- damage dealt: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DAMAGE_DEALT)));
            combatStatisticsLore.add(McColors.GRAY + "- damage taken: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DAMAGE_TAKEN)));
            combatStatisticsLore.add(McColors.GRAY + "- player kills: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.PLAYER_KILLS)));
            combatStatisticsLore.add(McColors.GRAY + "- mob kills: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.MOB_KILLS)));
            combatStatisticsLore.add(McColors.GRAY + "- deaths: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DEATHS)));

        combatStatisticsMeta.setLore(combatStatisticsLore);
        combatStatistics.setItemMeta(combatStatisticsMeta);

        return combatStatistics;
    }

    public ItemStack getTravelStatistics(Player player) {
        ItemStack travelStatistics = new ItemStack(Material.DIAMOND_BOOTS);
        ItemMeta travelStatisticsMeta = travelStatistics.getItemMeta();
        assert travelStatisticsMeta != null;
        travelStatisticsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        travelStatisticsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        travelStatisticsMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        travelStatisticsMeta.setDisplayName(McColors.AQUA + "Travel Statistics" + McColors.GRAY + " - "
                                            + McColors.GREEN + "Total distance");

        ArrayList<String> travelStatisticsLore = new ArrayList<>();
            travelStatisticsLore.add(McColors.GRAY + "- climbed: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.CLIMB_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- crouched: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.CROUCH_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- fallen: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.FALL_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- sprinted: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.SPRINT_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- swum: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.SWIM_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- walked: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.WALK_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- walked on water: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.WALK_ON_WATER_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- walked under water: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.WALK_UNDER_WATER_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- by boat: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.BOAT_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- by elytra: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.AVIATE_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- by horse: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.HORSE_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- by minecart: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.MINECART_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- by pig: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.PIG_ONE_CM)));
            travelStatisticsLore.add(McColors.GRAY + "- by strider: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.STRIDER_ONE_CM)));

        travelStatisticsMeta.setLore(travelStatisticsLore);
        travelStatistics.setItemMeta(travelStatisticsMeta);

        return travelStatistics;
    }

    public ItemStack getMiningStatistics(Player player) {
        ItemStack combatStatistics = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta combatStatisticsMeta = combatStatistics.getItemMeta();
        assert combatStatisticsMeta != null;
        combatStatisticsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        combatStatisticsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        combatStatisticsMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        combatStatisticsMeta.setDisplayName(McColors.AQUA + "Mining Statistics"+ McColors.GRAY + " - "
                + McColors.GREEN + "Total");

        ArrayList<String> combatStatisticsLore = new ArrayList<>();
        combatStatisticsLore.add(McColors.GRAY + "- damage dealt: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DAMAGE_DEALT)));
        combatStatisticsLore.add(McColors.GRAY + "- damage taken: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DAMAGE_TAKEN)));
        combatStatisticsLore.add(McColors.GRAY + "- player kills: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.PLAYER_KILLS)));
        combatStatisticsLore.add(McColors.GRAY + "- mob kills: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.MOB_KILLS)));
        combatStatisticsLore.add(McColors.GRAY + "- deaths: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.DEATHS)));

        combatStatisticsMeta.setLore(combatStatisticsLore);
        combatStatistics.setItemMeta(combatStatisticsMeta);

        return combatStatistics;
    }

}
