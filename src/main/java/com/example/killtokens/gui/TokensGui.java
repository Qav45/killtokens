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
    public static final int SLOT_TOKEN_BALANCE = 10;
    public static final int SLOT_TOKEN_WITHDRAW = 19;
    public static final int SLOT_TOKEN_CASHOUT = 28;
    public static final int SLOT_REFINED_BALANCE = 12;
    public static final int SLOT_COMPRESSED_BALANCE = 14;
    public static final int SLOT_REFINED_TOGGLE = 21;
    public static final int SLOT_REFINED_STORE = 23;
    public static final int SLOT_REFINED_WITHDRAW = 30;
    public static final int SLOT_COMPRESSED_WITHDRAW = 32;
    public static final int SLOT_DUPE_FLAGS = 16;
    public static final int SLOT_CLOSE = 49;

    private final KillTokensPlugin plugin;

    public TokensGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, title());
        ItemStack gray = pane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack black = pane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, gray);
        }
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, black);
            inv.setItem(45 + i, black);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, black);
            inv.setItem(i + 8, black);
        }

        UUID uuid = player.getUniqueId();
        RefinedOreStorage refined = plugin.getRefinedStorage();
        int tokens = plugin.getStorage().getTokens(uuid);
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
        int refinedBalance = refined.getRefinedBalance(uuid);
        int compressedBalance = refined.getCompressedBalance(uuid);
        boolean autoStore = refined.isAutoStoring(uuid);

        inv.setItem(SLOT_TOKEN_BALANCE, item(Material.GOLD_NUGGET, "&6&lKill Tokens",
            "&7Balance: &e" + tokens,
            "&7Cashout: &e" + cashTokens + " &7tokens for &a$" + String.format("%.2f", cashAmount),
            "&7Needed: &e" + Math.max(0, cashTokens - tokens)));
        inv.setItem(SLOT_TOKEN_WITHDRAW, item(Material.HOPPER, "&6Withdraw Kill Token",
            "&7Moves &e1 &7virtual token to your inventory.",
            tokens > 0 ? "&eClick to withdraw." : "&cNo tokens available."));
        inv.setItem(SLOT_TOKEN_CASHOUT, item(tokens >= cashTokens ? Material.GOLD_INGOT : Material.IRON_INGOT,
            tokens >= cashTokens ? "&aCash Out" : "&cCash Out Locked",
            "&7Convert &e" + cashTokens + " &7tokens into &a$" + String.format("%.2f", cashAmount),
            tokens >= cashTokens ? "&eClick to cash out." : "&cNeed " + Math.max(0, cashTokens - tokens) + " more tokens."));

        inv.setItem(SLOT_REFINED_BALANCE, item(Material.BLUE_DYE, "&b&lRefined Ore",
            "&7Stored: &b" + refinedBalance,
            "&7Lifetime: &f" + refined.getRefinedTotal(uuid)));
        inv.setItem(SLOT_COMPRESSED_BALANCE, item(Material.NAUTILUS_SHELL, "&3&lCompressed Refined Ore",
            "&7Stored: &3" + compressedBalance,
            "&7Lifetime: &f" + refined.getCompressedTotal(uuid)));
        inv.setItem(SLOT_REFINED_TOGGLE, item(autoStore ? Material.LIME_DYE : Material.RED_DYE,
            autoStore ? "&aAuto Storage Enabled" : "&cAuto Storage Disabled",
            "&7Automatically stores mined refined drops.",
            "&eClick to toggle."));
        inv.setItem(SLOT_REFINED_STORE, item(Material.CHEST, "&bStore Physical Refined Items",
            "&7Moves Refined Ore and Compressed Refined Ore",
            "&7from inventory into virtual storage.",
            "&eClick to store."));
        inv.setItem(SLOT_REFINED_WITHDRAW, item(Material.DROPPER, "&bWithdraw Refined Ore",
            "&7Moves &e1 &7stored Refined Ore to inventory.",
            refinedBalance > 0 ? "&eClick to withdraw." : "&cNone stored."));
        inv.setItem(SLOT_COMPRESSED_WITHDRAW, item(Material.DISPENSER, "&3Withdraw Compressed",
            "&7Moves &e1 &7stored Compressed Refined Ore to inventory.",
            compressedBalance > 0 ? "&eClick to withdraw." : "&cNone stored."));

        inv.setItem(SLOT_DUPE_FLAGS, item(Material.REDSTONE_TORCH, "&c&lDupe Flags",
            "&7Flag count: &c" + plugin.getDupeProtection().getFlagCount(uuid),
            "&7Last flag: &f" + plugin.getDupeProtection().getLastFlag(uuid)));
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&cClose", "&7Click to close."));

        player.openInventory(inv);
    }

    public String title() {
        return MessageUtil.color(plugin.getConfig().getString("gui-title", "&8&lKill Tokens"));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        List<String> coloredLore = Arrays.stream(lore)
            .map(MessageUtil::color)
            .collect(Collectors.toList());
        meta.setLore(coloredLore);
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
