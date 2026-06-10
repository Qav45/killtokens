package com.example.killtokens.gui;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedItemFactory;
import com.example.killtokens.refined.RefinedOreStorage;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TokensGui {

    public static final int SIZE = 54;

    // Refined Ore is the main stage (center), Kill Tokens sit in a side panel
    // on the left behind a divider column.
    //
    //  Col:   0    1        2    3      4        5        6      7    8
    //  R0:  [ B    B        B    B      B     PROFILE     B      B    B ]
    //  R1:  [ B  TKN_BAL    ║    .   REF_BAL  REF_STAT  CMP_BAL  .    B ]
    //  R2:  [ B  TKN_WIT    ║    .   REF_WIT     .      CMP_WIT  .    B ]
    //  R3:  [ B  TKN_CSH    ║    .   TG_AUTO     .      STR_REF  .    B ]
    //  R4:  [ B  accent     ║    .   accent      .      accent   .    B ]
    //  R5:  [ B    B        B    B      B      CLOSE     B       B    B ]

    public static final int SLOT_PROFILE = 5;

    // Side panel — Kill Tokens (col 1)
    public static final int SLOT_TOKEN_BALANCE  = 10; // R1 C1
    public static final int SLOT_TOKEN_WITHDRAW = 19; // R2 C1
    public static final int SLOT_TOKEN_CASHOUT  = 28; // R3 C1

    // Main stage — Refined Ore (cols 4-6)
    public static final int SLOT_REFINED_BALANCE     = 13; // R1 C4
    public static final int SLOT_REFINED_STATS       = 14; // R1 C5
    public static final int SLOT_COMPRESSED_BALANCE  = 15; // R1 C6

    public static final int SLOT_REFINED_WITHDRAW    = 22; // R2 C4
    public static final int SLOT_COMPRESSED_WITHDRAW = 24; // R2 C6

    public static final int SLOT_REFINED_TOGGLE      = 31; // R3 C4
    public static final int SLOT_REFINED_STORE       = 33; // R3 C6

    public static final int SLOT_CLOSE = 50; // R5 C5

    private final KillTokensPlugin plugin;

    public TokensGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        GuiHolder holder = GuiHolder.main();
        Inventory inv = Bukkit.createInventory(holder, SIZE, title());
        holder.setInventory(inv);

        ItemStack border = pane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE);

        // Fill all with gray, then stamp the black border ring
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);
        for (int i = 0; i < 9; i++)  inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9,     border);
            inv.setItem(row * 9 + 8, border);
        }

        // Divider column separating the token side panel from the refined main stage
        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9 + 2, border);
        }

        // Colored accents under each section
        inv.setItem(37, pane(Material.ORANGE_STAINED_GLASS_PANE));     // tokens
        inv.setItem(40, pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE)); // refined
        inv.setItem(42, pane(Material.PURPLE_STAINED_GLASS_PANE));     // compressed

        UUID uuid = player.getUniqueId();
        RefinedOreStorage refined = plugin.getRefinedStorage();
        int tokens       = plugin.getStorage().getTokens(uuid);
        int cashTokens   = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
        int refinedBal   = refined.getRefinedBalance(uuid);
        int compBal      = refined.getCompressedBalance(uuid);
        boolean autoStore = refined.isAutoStoring(uuid);

        inv.setItem(SLOT_PROFILE, profileHead(player, tokens, refinedBal, compBal));

        // ----- Side panel: Kill Tokens -----

        inv.setItem(SLOT_TOKEN_BALANCE, item(Material.GOLD_INGOT, "&6&lKill Tokens",
            "&7Balance: &e" + tokens,
            "&7Cashout rate: &e" + cashTokens + " &7» &a$" + String.format("%.2f", cashAmount),
            "&7Still need: &e" + Math.max(0, cashTokens - tokens)));

        inv.setItem(SLOT_TOKEN_WITHDRAW, item(Material.HOPPER,
            tokens > 0 ? "&6Withdraw Tokens" : "&7Withdraw Tokens &c(Empty)",
            "&7Balance: &e" + tokens,
            "",
            tokens > 0 ? "&e» Click to select amount" : "&cNo tokens to withdraw."));

        boolean canCashout = tokens >= cashTokens;
        inv.setItem(SLOT_TOKEN_CASHOUT, item(
            canCashout ? Material.EMERALD : Material.GRAY_DYE,
            canCashout ? "&aCash Out" : "&7Cash Out &c(Locked)",
            "&7Convert &e" + cashTokens + " tokens &7» &a$" + String.format("%.2f", cashAmount),
            "",
            canCashout ? "&e» Click to select amount" : "&cNeed &e" + Math.max(0, cashTokens - tokens) + " &cmore."));

        // ----- Main stage: Refined Ore -----

        inv.setItem(SLOT_REFINED_BALANCE, item(RefinedItemFactory.refinedMaterial(plugin), "&b&lRefined Ore",
            "&7Stored: &b" + refinedBal));

        inv.setItem(SLOT_REFINED_STATS, refinedStats(refined, uuid));

        inv.setItem(SLOT_COMPRESSED_BALANCE, item(RefinedItemFactory.compressedMaterial(plugin), "&d&lCompressed Refined Ore",
            "&7Stored: &d" + compBal));

        inv.setItem(SLOT_REFINED_WITHDRAW, item(Material.DROPPER,
            refinedBal > 0 ? "&bWithdraw Refined Ore" : "&7Withdraw Refined &c(Empty)",
            "&7Stored: &b" + refinedBal,
            "",
            refinedBal > 0 ? "&e» Click to select amount" : "&cNone stored."));

        inv.setItem(SLOT_COMPRESSED_WITHDRAW, item(Material.DISPENSER,
            compBal > 0 ? "&dWithdraw Compressed" : "&7Withdraw Compressed &c(Empty)",
            "&7Stored: &d" + compBal,
            "",
            compBal > 0 ? "&e» Click to select amount" : "&cNone stored."));

        inv.setItem(SLOT_REFINED_TOGGLE, item(
            autoStore ? Material.LIME_DYE : Material.RED_DYE,
            autoStore ? "&aAuto-Storage &l[ON]" : "&cAuto-Storage &l[OFF]",
            "&7Automatically stores mined refined ore.",
            "",
            "&e» Click to toggle"));

        inv.setItem(SLOT_REFINED_STORE, item(Material.CHEST, "&bStore Physical Refined",
            "&7Collects Refined + Compressed from your",
            "&7inventory into virtual storage.",
            "",
            "&e» Click to store"));

        // Close
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&cClose", "&7Click to close."));

        player.openInventory(inv);
    }

    public String title() {
        return MessageUtil.color(plugin.getConfig().getString("gui-title", "&8&lInstellar Storage"));
    }

    private ItemStack refinedStats(RefinedOreStorage refined, UUID uuid) {
        int orePity = plugin.getConfig().getInt("refined.ore-pity", 100);
        int compressedPity = plugin.getConfig().getInt("refined.compressed-pity", 5000);
        return item(Material.NETHER_STAR, "&b&lRefined Statistics",
            "&7Lifetime Refined: &b" + refined.getRefinedTotal(uuid),
            "&7Lifetime Compressed: &d" + refined.getCompressedTotal(uuid),
            "",
            "&7Refined pity: &f" + refined.getRefinedPity(uuid) + "&7/&f" + orePity,
            "&7Compressed pity: &f" + refined.getCompressedPity(uuid) + "&7/&f" + compressedPity);
    }

    private ItemStack profileHead(Player player, int tokens, int refinedBal, int compBal) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(MessageUtil.color("&e&l" + player.getName()));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&bRefined: &f" + refinedBal),
            MessageUtil.color("&dCompressed: &f" + compBal),
            MessageUtil.color("&6Tokens: &e" + tokens)
        ));
        head.setItemMeta(meta);
        return head;
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
