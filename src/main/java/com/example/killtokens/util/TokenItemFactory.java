package com.example.killtokens.util;

import com.example.killtokens.KillTokensPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Single source of truth for the physical kill-token item.
 * Items are PDC-tagged so other plugins (shops, sinks) can verify authenticity
 * with {@link #isToken(KillTokensPlugin, ItemStack)} instead of comparing names.
 */
public final class TokenItemFactory {

    private TokenItemFactory() {
    }

    public static ItemStack make(KillTokensPlugin plugin, int amount) {
        String itemName = plugin.getConfig().getString("token-item-name", "&6Kill Token");
        ItemStack item = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(itemName));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7Earned through honorable combat"),
            MessageUtil.color("&7Use &e/tokens cashout &7to exchange for money")
        ));
        ItemTags.tag(plugin, meta, ItemTags.TYPE_TOKEN);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isToken(KillTokensPlugin plugin, ItemStack item) {
        return ItemTags.is(plugin, item, ItemTags.TYPE_TOKEN);
    }
}
