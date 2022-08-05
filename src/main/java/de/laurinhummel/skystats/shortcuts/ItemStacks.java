package de.laurinhummel.skystats.shortcuts;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemStacks {
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

}
