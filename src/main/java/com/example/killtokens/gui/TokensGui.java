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
import java.util.stream.Collectors;

public class TokensGui {

    public static final int SLOT_BALANCE  = 4;
    public static final int SLOT_WITHDRAW = 10;
    public static final int SLOT_CASHOUT  = 16;
    public static final int SLOT_CLOSE    = 22;

    private final KillTokensPlugin plugin;

    public TokensGui(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui-title", "&8&lKill Tokens"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack filler = filler();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        int balance    = plugin.getStorage().getTokens(player.getUniqueId());
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmt = plugin.getConfig().getDouble("cash-amount", 100.0);
        boolean canCashout = balance >= cashTokens;

        inv.setItem(SLOT_BALANCE, buildItem(Material.EMERALD, "&a&lYour Tokens",
            "&7Balance: &a" + balance + " tokens",
            "&7Cashout rate: &e" + cashTokens + " &7tokens &7→ &a$" + String.format("%.2f", cashAmt)));

        inv.setItem(SLOT_WITHDRAW, buildItem(Material.GOLD_NUGGET, "&6&lWithdraw Token",
            "&7Withdraw &e1 token &7to your inventory.",
            "&7Current balance: &e" + balance));

        inv.setItem(SLOT_CASHOUT, buildItem(
            canCashout ? Material.GOLD_INGOT : Material.IRON_INGOT,
            canCashout ? "&a&lCash Out" : "&c&lCash Out &7(need more tokens)",
            "&7Exchange &e" + cashTokens + " tokens &7for &a$" + String.format("%.2f", cashAmt),
            canCashout
                ? "&aClick to cash out now!"
                : "&cNeed &e" + (cashTokens - balance) + " &cmore tokens."));

        inv.setItem(SLOT_CLOSE, buildItem(Material.BARRIER, "&c&lClose", "&7Close this menu."));

        player.openInventory(inv);
    }

    private ItemStack buildItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        meta.setLore(Arrays.stream(lore).map(MessageUtil::color).collect(Collectors.toList()));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
