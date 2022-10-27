package de.laurinhummel.skystats.managers;

import de.laurinhummel.skystats.misc.McColors;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class SkyifyManager {
    public enum ITEMTYPE {
        TOOL(new Material[]{Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE}),
        COMBAT(new Material[]{Material.DIAMOND_SWORD, Material.DIAMOND_AXE}),
        ARMOR(new Material[]{Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE});

        private Material[] materials;
        ITEMTYPE(Material[] material) {
            this.materials = material;
        }
        public Material[] getMaterials() {
            return this.materials;
        }
    }

    private ITEMTYPE itemtype;
    private ItemStack itemStack;
    private int level;

    public SkyifyManager(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemMeta getDefaultSkyifyMeta() {
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
            itemMeta.setDisplayName(McColors.GOLD + "Lvl. 1 " + McColors.RESET + "- " + McColors.AQUA + itemStack.getType().toString().replace("_", " ").toUpperCase(Locale.ENGLISH));
            itemMeta.setUnbreakable(true);
            itemMeta.addEnchant(Enchantment.MENDING, 2, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        ArrayList<String> lore = new ArrayList<>();
            lore.add(0, "test");
            lore.add(lore.size(), "");
            lore.add(lore.size(), McColors.GOLD + "SKYIFYED ITEM");

        itemMeta.setLore(lore);

        return itemMeta;
    }

    public int getLevel() {
        ItemMeta itemMeta = itemStack.getItemMeta();
        String[] metaString = itemMeta.toString().split(" ");
        return Integer.parseInt(metaString[1]);
    }

    public boolean isSkyify() {
        if(itemStack.hasItemMeta() && Objects.requireNonNull(itemStack.getItemMeta()).getLore() != null) {
            if(itemStack.getItemMeta().getLore().toString().contains("SKYIF")) {
                return true;
            }
        }
        return false;
    }
}
