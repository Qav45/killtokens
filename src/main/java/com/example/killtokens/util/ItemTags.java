package com.example.killtokens.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Tags plugin-issued items with an invisible PersistentDataContainer marker so they
 * can be identified reliably. Display-name matching alone is forgeable: any player
 * can rename a matching material in an anvil and pass it off as a plugin item.
 */
public final class ItemTags {

    /** PDC key (killtokens:item-type) holding the item type string. */
    private static final String KEY = "item-type";

    public static final String TYPE_TOKEN = "token";
    public static final String TYPE_REFINED = "refined";
    public static final String TYPE_COMPRESSED = "compressed";

    private ItemTags() {
    }

    public static void tag(Plugin plugin, ItemMeta meta, String type) {
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, type);
    }

    public static boolean is(Plugin plugin, ItemStack item, String type) {
        if (item == null || !item.hasItemMeta()) return false;
        String value = item.getItemMeta().getPersistentDataContainer()
            .get(key(plugin), PersistentDataType.STRING);
        return type.equals(value);
    }

    private static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, KEY);
    }
}
