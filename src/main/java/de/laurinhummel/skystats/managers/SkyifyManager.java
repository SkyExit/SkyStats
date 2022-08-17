package de.laurinhummel.skystats.managers;

import de.laurinhummel.skystats.misc.McColors;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

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

    public SkyifyManager(ItemStack itemStack, int lvl) {
        this.itemStack = itemStack;
        this.level = lvl;
    }

    public ItemMeta getSkyifyMeta() {
        ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(McColors.GOLD + "Lvl. 1 " + McColors.RESET + "- " + McColors.AQUA + itemStack.getType().toString().replace("_", " ").toLowerCase());
            itemMeta.setUnbreakable(true);
            itemMeta.addEnchant(Enchantment.MENDING, 2, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        ArrayList<String> lore = new ArrayList<>();
            lore.add(0, "test");
            lore.add(lore.size(), "");
            lore.add(lore.size(), McColors.GOLD + "SKYIFYED ITEM");

        itemMeta.setLore(lore);

        return itemMeta;
    }
}
