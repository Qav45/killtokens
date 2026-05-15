package com.example.killtokens.gui;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;

public class GuiListener implements Listener {

    private final KillTokensPlugin plugin;
    private final TokensGui gui;

    public GuiListener(KillTokensPlugin plugin, TokensGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String expectedTitle = MessageUtil.color(plugin.getConfig().getString("gui-title", "&8&lKill Tokens"));
        if (!event.getView().getTitle().equals(expectedTitle)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) return;

        // Dupe protection: one operation at a time per player
        if (!plugin.tryLock(player.getUniqueId())) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return;
        }

        try {
            switch (slot) {
                case TokensGui.SLOT_WITHDRAW: handleWithdraw(player); break;
                case TokensGui.SLOT_CASHOUT:  handleCashout(player);  break;
                case TokensGui.SLOT_CLOSE:    player.closeInventory(); return;
                default: return;
            }
        } finally {
            plugin.unlock(player.getUniqueId());
        }

        gui.open(player);
    }

    private void handleWithdraw(Player player) {
        int balance = plugin.getStorage().getTokens(player.getUniqueId());
        if (balance < 1) {
            player.sendMessage(MessageUtil.color("&cYou don't have any tokens to withdraw."));
            return;
        }

        String itemName = plugin.getConfig().getString("token-item-name", "&6Kill Token");
        ItemStack tokenItem = new ItemStack(Material.GOLD_NUGGET, 1);
        ItemMeta meta = tokenItem.getItemMeta();
        meta.setDisplayName(MessageUtil.color(itemName));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7Earned through honorable combat"),
            MessageUtil.color("&7Use &e/tokens cashout &7to exchange for money")
        ));
        tokenItem.setItemMeta(meta);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(tokenItem);
        if (!leftover.isEmpty()) {
            player.sendMessage(MessageUtil.color("&cYour inventory is full!"));
            return;
        }

        plugin.getStorage().setTokens(player.getUniqueId(), balance - 1);
        plugin.getStorage().flush();
        player.sendMessage(MessageUtil.color("&aWithdrawn &e1 &atoken to your inventory."));
    }

    private void handleCashout(Player player) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(MessageUtil.color("&cEconomy system is unavailable."));
            return;
        }

        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
        int balance = plugin.getStorage().getTokens(player.getUniqueId());

        if (balance < cashTokens) {
            player.sendMessage(MessageUtil.color(
                "&cYou need &e" + cashTokens + " &ctokens to cash out. You have &e" + balance + "&c."));
            return;
        }

        plugin.getStorage().setTokens(player.getUniqueId(), balance - cashTokens);
        plugin.getStorage().flush();
        economy.depositPlayer(player, cashAmount);
        player.sendMessage(MessageUtil.color(
            "&aCashed out &e" + cashTokens + " &atokens for &a$" + String.format("%.2f", cashAmount) + "&a!"));
    }
}
