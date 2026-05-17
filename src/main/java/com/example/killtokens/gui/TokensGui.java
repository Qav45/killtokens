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

    // 3x3 grid layout (rows 1-3, cols 1/4/7)
    //
    //  Col:  0  1         2  3    4            5  6    7              8
    //  R0:  [O  O         O  O    O            O  O    O              O]
    //  R1:  [O TKN_BAL    L  L  REF_BAL        L  L  CMP_BAL          O]
    //  R2:  [O TKN_WIT    L  L  REF_WIT        L  L  CMP_WIT          O]
    //  R3:  [O TKN_CSH    L  L  TG_AUTO        L  L  STR_REF          O]
    //  R4:  [O  L         L  L    L            L  L    L              O]
    //  R5:  [O  O         O  O   CLOSE         O  O    O              O]

    public static final int SLOT_TOKEN_BALANCE       = 10; // R1 C1
    public static final int SLOT_REFINED_BALANCE     = 13; // R1 C4
    public static final int SLOT_COMPRESSED_BALANCE  = 16; // R1 C7

    public static final int SLOT_TOKEN_WITHDRAW      = 19; // R2 C1
    public static final int SLOT_REFINED_WITHDRAW    = 22; // R2 C4
    public static final int SLOT_COMPRESSED_WITHDRAW = 25; // R2 C7

    public static final int SLOT_TOKEN_CASHOUT       = 28; // R3 C1
    public static final int SLOT_REFINED_TOGGLE      = 31; // R3 C4
    public static final int SLOT_REFINED_STORE       = 34; // R3 C7

    public static final int SLOT_CLOSE = 49; // R5 C4

    private final KillTokensPlugin plugin;

    public TokensGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, title());

        ItemStack orange = pane(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStack lime   = pane(Material.LIME_STAINED_GLASS_PANE);

        // Fill all with lime first, then stamp orange border
        for (int i = 0; i < SIZE; i++) inv.setItem(i, lime);
        // Top and bottom rows: orange
        for (int i = 0; i < 9; i++)  inv.setItem(i, orange);
        for (int i = 45; i < 54; i++) inv.setItem(i, orange);
        // Left and right columns: orange
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9,     orange);
            inv.setItem(row * 9 + 8, orange);
        }

        UUID uuid = player.getUniqueId();
        RefinedOreStorage refined = plugin.getRefinedStorage();
        int tokens       = plugin.getStorage().getTokens(uuid);
        int cashTokens   = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
        int refinedBal   = refined.getRefinedBalance(uuid);
        int compBal      = refined.getCompressedBalance(uuid);
        boolean autoStore = refined.isAutoStoring(uuid);

        // Row 1 — Balances
        inv.setItem(SLOT_TOKEN_BALANCE, item(Material.GOLD_INGOT, "&6&lKill Tokens",
            "&7Balance: &e" + tokens,
            "&7Cashout: &e" + cashTokens + " &7→ &a$" + String.format("%.2f", cashAmount),
            "&7Still need: &e" + Math.max(0, cashTokens - tokens)));

        inv.setItem(SLOT_REFINED_BALANCE, item(Material.DIAMOND, "&b&lRefined Ore",
            "&7Stored: &b" + refinedBal,
            "&7Lifetime: &f" + refined.getRefinedTotal(uuid)));

        inv.setItem(SLOT_COMPRESSED_BALANCE, item(Material.AMETHYST_SHARD, "&b&lCompressed Refined Ore",
            "&7Stored: &b" + compBal,
            "&7Lifetime: &f" + refined.getCompressedTotal(uuid)));

        // Row 2 — Withdraws
        inv.setItem(SLOT_TOKEN_WITHDRAW, item(Material.HOPPER,
            tokens > 0 ? "&6Withdraw Tokens" : "&7Withdraw Tokens &c(Empty)",
            "&7Balance: &e" + tokens,
            tokens > 0 ? "&eClick to select amount." : "&cNo tokens to withdraw."));

        inv.setItem(SLOT_REFINED_WITHDRAW, item(Material.DROPPER,
            refinedBal > 0 ? "&bWithdraw Refined Ore" : "&7Withdraw Refined &c(Empty)",
            "&7Stored: &b" + refinedBal,
            refinedBal > 0 ? "&eClick to select amount." : "&cNone stored."));

        inv.setItem(SLOT_COMPRESSED_WITHDRAW, item(Material.DISPENSER,
            compBal > 0 ? "&bWithdraw Compressed" : "&7Withdraw Compressed &c(Empty)",
            "&7Stored: &b" + compBal,
            compBal > 0 ? "&eClick to select amount." : "&cNone stored."));

        // Row 3 — Actions
        boolean canCashout = tokens >= cashTokens;
        inv.setItem(SLOT_TOKEN_CASHOUT, item(
            canCashout ? Material.EMERALD : Material.GRAY_DYE,
            canCashout ? "&aCash Out" : "&7Cash Out &c(Locked)",
            "&7Convert &e" + cashTokens + " tokens &7→ &a$" + String.format("%.2f", cashAmount),
            canCashout ? "&eClick to select amount." : "&cNeed &e" + Math.max(0, cashTokens - tokens) + " &cmore."));

        inv.setItem(SLOT_REFINED_TOGGLE, item(
            autoStore ? Material.LIME_DYE : Material.RED_DYE,
            autoStore ? "&aAuto-Storage &l[ON]" : "&cAuto-Storage &l[OFF]",
            "&7Automatically stores mined refined ore.",
            "&eClick to toggle."));

        inv.setItem(SLOT_REFINED_STORE, item(Material.CHEST, "&bStore Physical Refined",
            "&7Collects Refined + Compressed from inventory",
            "&7into virtual storage.",
            "&eClick to store."));

        // Close
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&cClose", "&7Click to close."));

        player.openInventory(inv);
    }

    public String title() {
        return MessageUtil.color(plugin.getConfig().getString("gui-title", "&6◆ &e&lKill Tokens &6◆"));
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
