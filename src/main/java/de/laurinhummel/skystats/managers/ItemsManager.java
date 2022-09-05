package de.laurinhummel.skystats.managers;

import de.laurinhummel.skystats.misc.McColors;
import de.laurinhummel.skystats.misc.SkyDataFormatter;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;

public class ItemsManager {
    private final Player player;
    public ItemsManager(Player player) { this.player = player; }
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

    public ItemStack getPlayerHead() {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        meta.setOwningPlayer(player);
        meta.setDisplayName(McColors.AQUA + "Statistics for " + McColors.GOLD + player.getName());

        StatisticsManager sm = new StatisticsManager(player);
        LevelManager level = new LevelManager(player);

        ArrayList<String> headLore = new ArrayList<>();
            headLore.add(McColors.GRAY + "PlayTime EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getPlayTimeEXP()));
            headLore.add(McColors.GRAY + "Combat EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getCombatEXP()));
            headLore.add(McColors.GRAY + "Travel EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getTravelEXP()));
            headLore.add(McColors.GRAY + "Interaction EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getInteractionEXP()));
            headLore.add(McColors.GRAY + "--> Total EXP: " + McColors.GOLD + SkyDataFormatter.formatNumber(sm.getTotalEXP()));
            headLore.add("");
            headLore.add(level.getProgressBar(15));
            headLore.add(level.getProgressBarUnderline());

        meta.setLore(headLore);
        head.setItemMeta(meta);

        return head;
    }

    public ItemStack getPlayTimeStatistics() {
        ItemStack playTimeStatistics = new ItemStack(Material.CLOCK);
        ItemMeta playTimeStatisticsMeta = playTimeStatistics.getItemMeta();
        assert playTimeStatisticsMeta != null;
        playTimeStatisticsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        playTimeStatisticsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        playTimeStatisticsMeta.setDisplayName(McColors.AQUA + "Play Time Statistics" + " - "
                + McColors.GREEN + "Total time");

        ArrayList<String> playTimeStatisticsLore = new ArrayList<>();
        playTimeStatisticsLore.add(McColors.GRAY + "- in the world : " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.TOTAL_WORLD_TIME)));
        playTimeStatisticsLore.add(McColors.GRAY + "- since last death: " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.TIME_SINCE_DEATH)));
        playTimeStatisticsLore.add(McColors.GRAY + "- since last rest: " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.TIME_SINCE_REST)));
        playTimeStatisticsLore.add(McColors.GRAY + "- sneaked: " + McColors.GOLD + SkyDataFormatter.formatTicks(player.getStatistic(Statistic.SNEAK_TIME)));

        playTimeStatisticsMeta.setLore(playTimeStatisticsLore);
        playTimeStatistics.setItemMeta(playTimeStatisticsMeta);

        return playTimeStatistics;
    }

    public ItemStack getCombatStatistics() {
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

    public ItemStack getTravelStatistics() {
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
        travelStatisticsLore.add(McColors.GRAY + "- flown: " + McColors.GOLD + SkyDataFormatter.formatNumberCM(player.getStatistic(Statistic.FLY_ONE_CM)));
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

    public ItemStack getInteractionStatistics() {
        ItemStack interactionStatistics = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta interactionStatisticsMeta = interactionStatistics.getItemMeta();
        assert interactionStatisticsMeta != null;
        interactionStatisticsMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        interactionStatisticsMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        interactionStatisticsMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        interactionStatisticsMeta.setDisplayName(McColors.AQUA + "Interaction Statistics"+ McColors.GRAY + " - "
                + McColors.GREEN + "Total interactions with");

        ArrayList<String> interactionStatisticsLore = new ArrayList<>();
        interactionStatisticsLore.add(McColors.GRAY + "- crafting table: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.CRAFTING_TABLE_INTERACTION)));
        interactionStatisticsLore.add(McColors.GRAY + "- furnace: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.FURNACE_INTERACTION)));
        interactionStatisticsLore.add(McColors.GRAY + "- campfire: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.INTERACT_WITH_CAMPFIRE)));
        interactionStatisticsLore.add(McColors.GRAY + "- anvil: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.INTERACT_WITH_ANVIL)));
        interactionStatisticsLore.add(McColors.GRAY + "- blast furnace: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.INTERACT_WITH_BLAST_FURNACE)));
        interactionStatisticsLore.add(McColors.GRAY + "- smoker: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.INTERACT_WITH_SMOKER)));
        interactionStatisticsLore.add(McColors.GRAY + "- stonecutter: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.INTERACT_WITH_STONECUTTER)));
        interactionStatisticsLore.add(McColors.GRAY + "- beacon: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.BEACON_INTERACTION)));
        interactionStatisticsLore.add(McColors.GRAY + "- brewingstand: " + McColors.GOLD + SkyDataFormatter.formatNumber(player.getStatistic(Statistic.BREWINGSTAND_INTERACTION)));

        interactionStatisticsMeta.setLore(interactionStatisticsLore);
        interactionStatistics.setItemMeta(interactionStatisticsMeta);

        return interactionStatistics;
    }

}
