package com.example.killtokens.refined;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class RefinedItemFactory {

    private RefinedItemFactory() {
    }

    public static ItemStack makeRefined(KillTokensPlugin plugin, int amount) {
        ItemStack item = new ItemStack(configMaterial(plugin, "refined.refined-item", Material.BLUE_DYE), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color("&b&lRefined Ore"));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7You can get this from mining ores not blocks."),
            MessageUtil.color("&7Chance: &f1/" + plugin.getConfig().getInt("refined.ore-chance", 85)),
            MessageUtil.color("&7Pity: &fGuaranteed after " + plugin.getConfig().getInt("refined.ore-pity", 100) + " ores")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeCompressed(KillTokensPlugin plugin, int amount) {
        ItemStack item = new ItemStack(configMaterial(plugin, "refined.compressed-item", Material.NAUTILUS_SHELL), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color("&3&lCompressed Refined Ore"));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7A dense cache of refined ore energy."),
            MessageUtil.color("&7Chance: &f1/" + plugin.getConfig().getInt("refined.compressed-chance", 3500)),
            MessageUtil.color("&7Pity: &fGuaranteed after " + plugin.getConfig().getInt("refined.compressed-pity", 5000) + " ores")
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isRefined(KillTokensPlugin plugin, ItemStack item) {
        return hasName(item, configMaterial(plugin, "refined.refined-item", Material.BLUE_DYE), "Refined Ore");
    }

    public static boolean isCompressed(KillTokensPlugin plugin, ItemStack item) {
        return hasName(item, configMaterial(plugin, "refined.compressed-item", Material.NAUTILUS_SHELL), "Compressed Refined Ore");
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
