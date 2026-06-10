package com.example.killtokens.gui;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedOreStorage;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in "Refined U" shop opened with /refinedu.
 *
 * Everything is config-driven (refinedu.sections): each section becomes a tab in
 * the top row, and each item in a section is a buy button. Costs can mix refined
 * ore, compressed refined ore, kill tokens, and Vault money. A purchase can give
 * the displayed item and/or run console commands.
 */
public class ShopGui {

    public static final int SIZE = 54;

    // Interior 7x4 grid for purchasable items
    public static final int[] ITEM_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };

    public static final int SLOT_BALANCE = 45;
    public static final int SLOT_CLOSE = 49;

    private final KillTokensPlugin plugin;

    public ShopGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public String title() {
        return MessageUtil.color(plugin.getConfig().getString("refinedu.title", "&8&lRefined U Shop"));
    }

    public List<String> sectionIds() {
        ConfigurationSection sections = plugin.getConfig().getConfigurationSection("refinedu.sections");
        if (sections == null) return Collections.emptyList();
        return new ArrayList<>(sections.getKeys(false));
    }

    /** Tab slots sit centered in the top row. Returns -1 if the index doesn't fit. */
    public static int tabSlot(int index, int count) {
        if (count > 7 || index < 0 || index >= count) return -1;
        return (9 - count) / 2 + index;
    }

    /** Inverse of {@link #tabSlot}: which section index a raw slot points at, or -1. */
    public static int tabIndex(int slot, int count) {
        if (count == 0 || count > 7 || slot < 0 || slot > 8) return -1;
        int index = slot - (9 - count) / 2;
        return (index >= 0 && index < count) ? index : -1;
    }

    public void open(Player player, String sectionId) {
        List<String> sections = sectionIds();
        if (sections.isEmpty()) {
            player.sendMessage(MessageUtil.color("&cThe shop has no sections configured. See config.yml (refinedu.sections)."));
            return;
        }
        if (sectionId == null || !sections.contains(sectionId)) {
            sectionId = sections.get(0);
        }

        GuiHolder holder = GuiHolder.shop(sectionId);
        Inventory inv = Bukkit.createInventory(holder, SIZE, title());
        holder.setInventory(inv);

        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);
        for (int i = 0; i < 9; i++)  inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9,     border);
            inv.setItem(row * 9 + 8, border);
        }

        // Section tabs
        for (int i = 0; i < sections.size(); i++) {
            int slot = tabSlot(i, sections.size());
            if (slot >= 0) {
                inv.setItem(slot, tabItem(sections.get(i), sections.get(i).equals(sectionId)));
            }
        }

        // Items of the selected section
        List<ShopItem> items = items(sectionId);
        for (int i = 0; i < items.size() && i < ITEM_SLOTS.length; i++) {
            inv.setItem(ITEM_SLOTS[i], items.get(i).buildDisplay(plugin));
        }

        inv.setItem(SLOT_BALANCE, balanceItem(player));
        inv.setItem(SLOT_CLOSE, simpleItem(Material.BARRIER, "&cClose", "&7Click to close."));

        player.openInventory(inv);
    }

    public List<ShopItem> items(String sectionId) {
        List<ShopItem> result = new ArrayList<>();
        List<Map<?, ?>> raw = plugin.getConfig().getMapList("refinedu.sections." + sectionId + ".items");
        for (Map<?, ?> map : raw) {
            ShopItem item = ShopItem.parse(plugin, sectionId, map);
            if (item != null) result.add(item);
        }
        return result;
    }

    private ItemStack tabItem(String sectionId, boolean selected) {
        String path = "refinedu.sections." + sectionId + ".";
        String name = plugin.getConfig().getString(path + "name", sectionId);
        Material icon = Material.matchMaterial(plugin.getConfig().getString(path + "icon", "CHEST"));
        if (icon == null) icon = Material.CHEST;

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color((selected ? "&a» " : "&7") + name));
        meta.setLore(Collections.singletonList(MessageUtil.color(
            selected ? "&aCurrently viewing." : "&e» Click to view")));
        if (selected) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack balanceItem(Player player) {
        UUID uuid = player.getUniqueId();
        RefinedOreStorage refined = plugin.getRefinedStorage();
        List<String> lore = new ArrayList<>();
        lore.add("&bRefined: &f" + refined.getRefinedBalance(uuid));
        lore.add("&dCompressed: &f" + refined.getCompressedBalance(uuid));
        lore.add("&6Tokens: &e" + plugin.getStorage().getTokens(uuid));
        if (plugin.getEconomy() != null) {
            lore.add("&aMoney: &f$" + String.format("%.2f", plugin.getEconomy().getBalance(player)));
        }
        return simpleItem(Material.GOLD_INGOT, "&e&lYour Balances", lore.toArray(new String[0]));
    }

    private ItemStack simpleItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(MessageUtil.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        meta.setLore(Collections.emptyList());
        item.setItemMeta(meta);
        return item;
    }

    // -------------------------------------------------------------------------

    /** One purchasable entry, parsed leniently from a config map. */
    public static final class ShopItem {

        public final Material material;
        public final int amount;
        public final String name;
        public final List<String> lore;
        public final int costRefined;
        public final int costCompressed;
        public final int costTokens;
        public final double costMoney;
        public final List<String> commands;
        public final boolean giveItem;

        private ShopItem(Material material, int amount, String name, List<String> lore,
                         int costRefined, int costCompressed, int costTokens, double costMoney,
                         List<String> commands, boolean giveItem) {
            this.material = material;
            this.amount = amount;
            this.name = name;
            this.lore = lore;
            this.costRefined = costRefined;
            this.costCompressed = costCompressed;
            this.costTokens = costTokens;
            this.costMoney = costMoney;
            this.commands = commands;
            this.giveItem = giveItem;
        }

        @SuppressWarnings("unchecked")
        static ShopItem parse(KillTokensPlugin plugin, String sectionId, Map<?, ?> map) {
            Object matName = map.get("material");
            Material material = matName == null ? null : Material.matchMaterial(String.valueOf(matName));
            if (material == null) {
                plugin.getLogger().warning("Shop section '" + sectionId + "': skipping item with invalid material '" + matName + "'.");
                return null;
            }

            int amount = intOf(map.get("amount"), 1);
            String name = map.get("name") == null ? null : String.valueOf(map.get("name"));

            List<String> lore = new ArrayList<>();
            if (map.get("lore") instanceof List) {
                for (Object line : (List<Object>) map.get("lore")) lore.add(String.valueOf(line));
            }

            int costRefined = 0, costCompressed = 0, costTokens = 0;
            double costMoney = 0;
            if (map.get("cost") instanceof Map) {
                Map<?, ?> cost = (Map<?, ?>) map.get("cost");
                costRefined = intOf(cost.get("refined"), 0);
                costCompressed = intOf(cost.get("compressed"), 0);
                costTokens = intOf(cost.get("tokens"), 0);
                costMoney = doubleOf(cost.get("money"), 0);
            }
            if (costRefined <= 0 && costCompressed <= 0 && costTokens <= 0 && costMoney <= 0) {
                plugin.getLogger().warning("Shop section '" + sectionId + "': skipping item '" + matName + "' with no cost.");
                return null;
            }

            List<String> commands = new ArrayList<>();
            if (map.get("commands") instanceof List) {
                for (Object cmd : (List<Object>) map.get("commands")) commands.add(String.valueOf(cmd));
            }
            boolean giveItem = !(map.get("give-item") instanceof Boolean) || (Boolean) map.get("give-item");

            return new ShopItem(material, Math.max(1, amount), name, lore,
                costRefined, costCompressed, costTokens, costMoney, commands, giveItem);
        }

        public ItemStack buildDisplay(KillTokensPlugin plugin) {
            ItemStack item = new ItemStack(material, Math.min(64, amount));
            ItemMeta meta = item.getItemMeta();
            if (name != null) meta.setDisplayName(MessageUtil.color(name));
            List<String> display = new ArrayList<>();
            for (String line : lore) display.add(MessageUtil.color(line));
            display.add("");
            display.add(MessageUtil.color("&7Cost:"));
            if (costRefined > 0)    display.add(MessageUtil.color("  &b" + costRefined + " Refined Ore"));
            if (costCompressed > 0) display.add(MessageUtil.color("  &d" + costCompressed + " Compressed"));
            if (costTokens > 0)     display.add(MessageUtil.color("  &e" + costTokens + " Kill Tokens"));
            if (costMoney > 0)      display.add(MessageUtil.color("  &a$" + String.format("%.2f", costMoney)));
            display.add("");
            display.add(MessageUtil.color("&e» Click to buy"));
            meta.setLore(display);
            item.setItemMeta(meta);
            return item;
        }

        public String displayName() {
            return name != null ? MessageUtil.color(name) : material.name();
        }

        private static int intOf(Object value, int def) {
            if (value instanceof Number) return ((Number) value).intValue();
            try {
                return value == null ? def : Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                return def;
            }
        }

        private static double doubleOf(Object value, double def) {
            if (value instanceof Number) return ((Number) value).doubleValue();
            try {
                return value == null ? def : Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }
}
