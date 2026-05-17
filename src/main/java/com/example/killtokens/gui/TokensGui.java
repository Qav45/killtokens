package com.example.killtokens.gui;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedOreStorage;
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
import java.util.UUID;
import java.util.stream.Collectors;

public class TokensGui {

    public static final int SIZE = 54;

    // Kill Tokens section (left side, col 1)
    public static final int SLOT_TOKEN_BALANCE  = 10; // row 1, col 1
    public static final int SLOT_TOKEN_WITHDRAW = 19; // row 2, col 1
    public static final int SLOT_TOKEN_CASHOUT  = 28; // row 3, col 1

    // Refined Ore section (right side, cols 5 and 7)
    public static final int SLOT_REFINED_BALANCE    = 14; // row 1, col 5
    public static final int SLOT_COMPRESSED_BALANCE = 16; // row 1, col 7
    public static final int SLOT_REFINED_TOGGLE     = 23; // row 2, col 5
    public static final int SLOT_REFINED_STORE      = 25; // row 2, col 7
    public static final int SLOT_REFINED_WITHDRAW   = 32; // row 3, col 5
    public static final int SLOT_COMPRESSED_WITHDRAW = 34; // row 3, col 7

    public static final int SLOT_CLOSE = 49;

    // Col 4 center divider slots (rows 1-4)
    private static final int[] DIVIDER_SLOTS = {13, 22, 31, 40};

    private final KillTokensPlugin plugin;

    public TokensGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, title());

        ItemStack purple = pane(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack gray   = pane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack cyan   = pane(Material.CYAN_STAINED_GLASS_PANE);

        // Fill interior with gray
        for (int i = 0; i < SIZE; i++) inv.setItem(i, gray);

        // Purple border: top row, bottom row, left col, right col
        for (int i = 0; i < 9; i++)  inv.setItem(i, purple);
        for (int i = 45; i < 54; i++) inv.setItem(i, purple);
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, purple);
            inv.setItem(i + 8, purple);
        }

        // Cyan center column divider
        for (int s : DIVIDER_SLOTS) inv.setItem(s, cyan);

        // Row 4 decorative — section labels
        inv.setItem(37, item(Material.GOLD_NUGGET,    "&6&lKill Tokens",   "&7Manage your kill tokens."));
        inv.setItem(41, item(Material.BLUE_DYE,       "&b&lRefined Ore",   "&7Manage your refined ore storage."));

        UUID uuid = player.getUniqueId();
        RefinedOreStorage refined = plugin.getRefinedStorage();
        int tokens       = plugin.getStorage().getTokens(uuid);
        int cashTokens   = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
        int refinedBal   = refined.getRefinedBalance(uuid);
        int compBal      = refined.getCompressedBalance(uuid);
        boolean autoStore = refined.isAutoStoring(uuid);

        // --- Kill Tokens section ---
        inv.setItem(SLOT_TOKEN_BALANCE, item(Material.GOLD_INGOT, "&6&lKill Tokens",
            "&7Balance: &e" + tokens,
            "&7Cashout rate: &e" + cashTokens + " tokens &7→ &a$" + String.format("%.2f", cashAmount),
            "&7Tokens needed: &e" + Math.max(0, cashTokens - tokens)));

        inv.setItem(SLOT_TOKEN_WITHDRAW, item(Material.HOPPER, "&6Withdraw Tokens",
            tokens > 0
                ? "&7Balance: &e" + tokens + " tokens"
                : "&cNo tokens to withdraw.",
            tokens > 0 ? "&eClick to select amount." : ""));

        boolean canCashout = tokens >= cashTokens;
        inv.setItem(SLOT_TOKEN_CASHOUT, item(
            canCashout ? Material.EMERALD : Material.GRAY_DYE,
            canCashout ? "&aCash Out" : "&7Cash Out &c(Locked)",
            "&7Convert &e" + cashTokens + " tokens &7into &a$" + String.format("%.2f", cashAmount),
            canCashout
                ? "&eClick to select amount."
                : "&cNeed &e" + Math.max(0, cashTokens - tokens) + " &cmore tokens."));

        // --- Refined Ore section ---
        inv.setItem(SLOT_REFINED_BALANCE, item(Material.DIAMOND, "&b&lRefined Ore",
            "&7Stored: &b" + refinedBal,
            "&7Lifetime: &f" + refined.getRefinedTotal(uuid)));

        inv.setItem(SLOT_COMPRESSED_BALANCE, item(Material.AMETHYST_SHARD, "&d&lCompressed Refined",
            "&7Stored: &d" + compBal,
            "&7Lifetime: &f" + refined.getCompressedTotal(uuid)));

        inv.setItem(SLOT_REFINED_TOGGLE, item(
            autoStore ? Material.LIME_DYE : Material.RED_DYE,
            autoStore ? "&aAuto-Storage &l[ON]" : "&cAuto-Storage &l[OFF]",
            "&7Auto-stores mined refined ore drops.",
            "&eClick to toggle."));

        inv.setItem(SLOT_REFINED_STORE, item(Material.CHEST, "&bStore Physical Refined",
            "&7Moves Refined Ore and Compressed",
            "&7from inventory into virtual storage.",
            "&eClick to store."));

        inv.setItem(SLOT_REFINED_WITHDRAW, item(Material.DROPPER, "&bWithdraw Refined Ore",
            refinedBal > 0
                ? "&7Stored: &b" + refinedBal
                : "&cNone stored.",
            refinedBal > 0 ? "&eClick to select amount." : ""));

        inv.setItem(SLOT_COMPRESSED_WITHDRAW, item(Material.DISPENSER, "&dWithdraw Compressed",
            compBal > 0
                ? "&7Stored: &d" + compBal
                : "&cNone stored.",
            compBal > 0 ? "&eClick to select amount." : ""));

        // Close button
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&cClose", "&7Click to close this menu."));

        player.openInventory(inv);
    }

    public String title() {
        return MessageUtil.color(plugin.getConfig().getString("gui-title", "&5&lKill Tokens Menu"));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        List<String> colored = Arrays.stream(lore)
            .map(MessageUtil::color)
            .collect(Collectors.toList());
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
