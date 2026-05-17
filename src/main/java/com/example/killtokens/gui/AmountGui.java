package com.example.killtokens.gui;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AmountGui {

    public enum Type { TOKEN_WITHDRAW, REFINED_WITHDRAW, COMPRESSED_WITHDRAW, TOKEN_CASHOUT }

    // Row 1 interior slots (col 1-7 between borders at 9 and 17)
    public static final int[] AMOUNT_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    // -1 means "All" (uses max)
    static final int[] BASE_AMOUNTS = {1, 5, 10, 16, 32, 64, -1};

    public static final int SLOT_BACK = 22;
    public static final int SIZE = 27;

    private final KillTokensPlugin plugin;

    public AmountGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the amount-selection GUI.
     * For TOKEN_CASHOUT, {@code max} is balance / cashTokens (number of cashouts available).
     * For all other types, {@code max} is the available item balance.
     */
    public void open(Player player, Type type, int max) {
        Inventory inv = Bukkit.createInventory(null, SIZE, titleFor(type));

        ItemStack border = pane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack fill = pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) inv.setItem(i, fill);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 18; i < 27; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);

        inv.setItem(4, headerItem(type, max));

        for (int i = 0; i < AMOUNT_SLOTS.length; i++) {
            int raw = BASE_AMOUNTS[i];
            int amt = (raw == -1) ? max : raw;
            inv.setItem(AMOUNT_SLOTS[i], amountButton(type, amt, max, raw == -1));
        }

        inv.setItem(SLOT_BACK, item(Material.ARROW, "&7← Back", "&7Return to the main menu."));

        player.openInventory(inv);
    }

    public String titleFor(Type type) {
        switch (type) {
            case TOKEN_WITHDRAW:    return MessageUtil.color("&8» Withdraw Kill Tokens");
            case REFINED_WITHDRAW:  return MessageUtil.color("&8» Withdraw Refined Ore");
            case COMPRESSED_WITHDRAW: return MessageUtil.color("&8» Withdraw Compressed");
            case TOKEN_CASHOUT:     return MessageUtil.color("&8» Cash Out Tokens");
            default: return "Select Amount";
        }
    }

    private ItemStack headerItem(Type type, int max) {
        switch (type) {
            case TOKEN_WITHDRAW:
                return item(Material.GOLD_INGOT, "&6&lWithdraw Kill Tokens",
                    "&7Available: &e" + max + " tokens",
                    "&7Choose how many to withdraw.");
            case TOKEN_CASHOUT: {
                int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
                double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
                return item(Material.EMERALD, "&a&lCash Out Tokens",
                    "&7Rate: &e" + cashTokens + " tokens &7→ &a$" + String.format("%.2f", cashAmount),
                    "&7Cashouts available: &e" + max,
                    "&7Choose how many cashouts to perform.");
            }
            case REFINED_WITHDRAW:
                return item(Material.BLUE_DYE, "&b&lWithdraw Refined Ore",
                    "&7Available: &b" + max,
                    "&7Choose how many to withdraw.");
            case COMPRESSED_WITHDRAW:
                return item(Material.AMETHYST_SHARD, "&d&lWithdraw Compressed",
                    "&7Available: &d" + max,
                    "&7Choose how many to withdraw.");
            default:
                return item(Material.PAPER, "&fSelect Amount", "&7Available: &e" + max);
        }
    }

    private ItemStack amountButton(Type type, int amount, int max, boolean isAll) {
        boolean canAfford = amount > 0 && amount <= max;
        Material mat = canAfford ? Material.LIME_CONCRETE : Material.RED_CONCRETE;

        if (type == Type.TOKEN_CASHOUT) {
            int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
            double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
            String label = isAll
                ? (amount > 0 ? "&6All (&e" + amount + "x&6)" : "&cAll (none)")
                : "&e" + amount + "x &6Cashout";
            String detail = amount > 0
                ? "&7Cost: &e" + (amount * cashTokens) + " tokens &7→ &a$" + String.format("%.2f", amount * cashAmount)
                : "&cNo cashouts available.";
            return item(mat, label, detail);
        }

        String typeName = typeName(type);
        String label = isAll
            ? (amount > 0 ? "&bAll (&e" + amount + "&b)" : "&cAll (none)")
            : "&e" + amount + " &b" + typeName;
        String detail = canAfford
            ? "&aClick to withdraw &e" + amount + "&a."
            : (amount <= 0 ? "&cNone available." : "&cNot enough (have &e" + max + "&c).");
        return item(mat, label, detail);
    }

    private String typeName(Type type) {
        switch (type) {
            case TOKEN_WITHDRAW: return "Token";
            case REFINED_WITHDRAW: return "Refined";
            case COMPRESSED_WITHDRAW: return "Compressed";
            default: return "Item";
        }
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        List<String> colored = Arrays.stream(lore).map(MessageUtil::color).collect(Collectors.toList());
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
}
