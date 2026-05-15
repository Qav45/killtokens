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

/**
 * 45-slot (5-row) layout:
 *
 *  Row 0  [BK][BK][BK][BK][BK][BK][BK][BK][BK]   black border
 *  Row 1  [BK][GY][GY][GY][BAL][GY][GY][GY][BK]   balance  (slot 13)
 *  Row 2  [BK][GY][GY][GY][PRG][GY][GY][GY][BK]   progress (slot 22)
 *  Row 3  [BK][GY][WIT][GY][GY][GY][CSH][GY][BK]  actions  (slots 29, 33)
 *  Row 4  [BK][BK][BK][BK][CLO][BK][BK][BK][BK]   close    (slot 40)
 */
public class TokensGui {

    public static final int SLOT_BALANCE  = 13;
    public static final int SLOT_PROGRESS = 22;
    public static final int SLOT_WITHDRAW = 29;
    public static final int SLOT_CASHOUT  = 33;
    public static final int SLOT_CLOSE    = 40;

    // Slots that are part of the black outer border
    private static final int[] BORDER_SLOTS = {
        0,  1,  2,  3,  4,  5,  6,  7,  8,   // row 0
        9,  17,                                 // row 1 edges
        18, 26,                                 // row 2 edges
        27, 35,                                 // row 3 edges
        36, 37, 38, 39, 40, 41, 42, 43, 44     // row 4
    };

    private final KillTokensPlugin plugin;

    public TokensGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui-title", "&8&lKill Tokens"));
        Inventory inv = Bukkit.createInventory(null, 45, title);

        // Fill inner area with gray, then border with black
        ItemStack gray = pane(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack black = pane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < 45; i++) {
            inv.setItem(i, gray);
        }
        for (int s : BORDER_SLOTS) {
            inv.setItem(s, black);
        }

        int balance    = plugin.getStorage().getTokens(player.getUniqueId());
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmt = plugin.getConfig().getDouble("cash-amount", 100.0);
        int needed     = Math.max(0, cashTokens - balance);
        boolean ready  = balance >= cashTokens;

        inv.setItem(SLOT_BALANCE, buildBalance(balance, cashTokens, cashAmt));
        inv.setItem(SLOT_PROGRESS, buildProgress(balance, cashTokens, needed, ready));
        inv.setItem(SLOT_WITHDRAW, buildWithdraw(balance));
        inv.setItem(SLOT_CASHOUT, buildCashout(cashTokens, cashAmt, needed, ready));
        inv.setItem(SLOT_CLOSE, buildClose());

        player.openInventory(inv);
    }

    // ── Item builders ──────────────────────────────────────────────────────────

    private ItemStack buildBalance(int balance, int cashTokens, double cashAmt) {
        return buildItem(Material.NETHER_STAR, "&6&lKill Tokens",
            "&8&m                    ",
            "&7  Balance    &8» &e" + balance + " tokens",
            "&7  Cash rate  &8» &e" + cashTokens + " &7→ &a$" + String.format("%.2f", cashAmt),
            "&8&m                    ");
    }

    private ItemStack buildProgress(int balance, int cashTokens, int needed, boolean ready) {
        if (ready) {
            return buildItem(Material.LIME_DYE, "&a&lReady to Cash Out!",
                "&8&m                    ",
                "&7  You have &e" + balance + " &7of &e" + cashTokens + " &7required tokens.",
                "&7  Click &aCash Out &7below to redeem.",
                "&8&m                    ");
        }

        String bar = buildBar(balance, cashTokens, 16);
        return buildItem(Material.CLOCK, "&e&lProgress",
            "&8&m                    ",
            "&7  " + bar,
            "&7  &e" + balance + " &7/ &e" + cashTokens + " &8(&c" + needed + " more needed&8)",
            "&8&m                    ");
    }

    private ItemStack buildWithdraw(int balance) {
        if (balance < 1) {
            return buildItem(Material.GOLD_NUGGET, "&7&lWithdraw",
                "&8&m                    ",
                "&7  Take a token from your balance",
                "&7  and place it in your inventory.",
                "",
                "&c  No tokens to withdraw.",
                "&8&m                    ");
        }
        return buildItem(Material.GOLD_NUGGET, "&6&lWithdraw",
            "&8&m                    ",
            "&7  Take a token from your balance",
            "&7  and place it in your inventory.",
            "",
            "&e  Click to withdraw &61 token&e.",
            "&8&m                    ");
    }

    private ItemStack buildCashout(int cashTokens, double cashAmt, int needed, boolean ready) {
        if (ready) {
            return buildItem(Material.GOLD_INGOT, "&a&lCash Out",
                "&8&m                    ",
                "&7  Convert &e" + cashTokens + " tokens &7into",
                "&7  &a$" + String.format("%.2f", cashAmt) + " &7in economy currency.",
                "",
                "&a  Click to cash out now!",
                "&8&m                    ");
        }
        return buildItem(Material.IRON_INGOT, "&c&lCash Out &8(Locked)",
            "&8&m                    ",
            "&7  Convert &e" + cashTokens + " tokens &7into",
            "&7  &a$" + String.format("%.2f", cashAmt) + " &7in economy currency.",
            "",
            "&c  Need &e" + needed + " &cmore tokens.",
            "&8&m                    ");
    }

    private ItemStack buildClose() {
        return buildItem(Material.BARRIER, "&c&lClose",
            "&7  Click to close this menu.");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Generates a simple colored progress bar string. */
    private static String buildBar(int current, int max, int length) {
        int filled = (int) Math.round((double) current / max * length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(i < filled ? "&a" : "&8").append("▌");
        }
        return sb.toString();
    }

    private ItemStack buildItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        List<String> colored = Arrays.stream(lore)
            .map(MessageUtil::color)
            .collect(Collectors.toList());
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack pane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        meta.setLore(Collections.emptyList());
        item.setItemMeta(meta);
        return item;
    }
}
