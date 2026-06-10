package com.example.killtokens.refined;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.ItemTags;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class RefinedItemFactory {

    private RefinedItemFactory() {
    }

    public static ItemStack makeRefined(KillTokensPlugin plugin, int amount) {
        ItemStack item = new ItemStack(refinedMaterial(plugin), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color("&b&lRefined Ore"));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7You can get this from mining ores not blocks."),
            MessageUtil.color("&7Chance: &f1/" + plugin.getConfig().getInt("refined.ore-chance", 85)),
            MessageUtil.color("&7Pity: &fGuaranteed after " + plugin.getConfig().getInt("refined.ore-pity", 100) + " ores")
        ));
        ItemTags.tag(plugin, meta, ItemTags.TYPE_REFINED);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeCompressed(KillTokensPlugin plugin, int amount) {
        ItemStack item = new ItemStack(compressedMaterial(plugin), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color("&b&lCompressed Refined Ore"));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7You can get this from mining ores not blocks."),
            MessageUtil.color("&7Chance: &f1/" + plugin.getConfig().getInt("refined.compressed-chance", 3500)),
            MessageUtil.color("&7Pity: &fGuaranteed after " + plugin.getConfig().getInt("refined.compressed-pity", 5000) + " ores")
        ));
        ItemTags.tag(plugin, meta, ItemTags.TYPE_COMPRESSED);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isRefined(KillTokensPlugin plugin, ItemStack item) {
        if (ItemTags.is(plugin, item, ItemTags.TYPE_REFINED)) return true;
        return acceptLegacy(plugin) && hasName(item, refinedMaterial(plugin), "Refined Ore");
    }

    public static boolean isCompressed(KillTokensPlugin plugin, ItemStack item) {
        if (ItemTags.is(plugin, item, ItemTags.TYPE_COMPRESSED)) return true;
        return acceptLegacy(plugin) && hasName(item, compressedMaterial(plugin), "Compressed Refined Ore");
    }

    public static Material refinedMaterial(KillTokensPlugin plugin) {
        return configMaterial(plugin, "refined.refined-item", Material.BLUE_DYE);
    }

    public static Material compressedMaterial(KillTokensPlugin plugin) {
        return configMaterial(plugin, "refined.compressed-item", Material.DIAMOND_CHESTPLATE);
    }

    /**
     * Pre-1.2 items only carried a display name, which any player can forge with an
     * anvil rename. The fallback exists so old legitimate items stay storable; turn
     * off refined.accept-legacy-items once players have cycled their old drops.
     */
    private static boolean acceptLegacy(KillTokensPlugin plugin) {
        return plugin.getConfig().getBoolean("refined.accept-legacy-items", true);
    }

    private static boolean hasName(ItemStack item, Material material, String name) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && org.bukkit.ChatColor.stripColor(meta.getDisplayName()).equals(name);
    }

    private static Material configMaterial(KillTokensPlugin plugin, String path, Material fallback) {
        String configured = plugin.getConfig().getString(path, fallback.name());
        Material material = Material.matchMaterial(configured);
        return material == null ? fallback : material;
    }
}
