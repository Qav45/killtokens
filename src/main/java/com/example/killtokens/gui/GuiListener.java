package com.example.killtokens.gui;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedItemFactory;
import com.example.killtokens.refined.RefinedOreStorage;
import com.example.killtokens.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

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
        if (!event.getView().getTitle().equals(gui.title())) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= TokensGui.SIZE) return;

        if (slot == TokensGui.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (!plugin.getDupeProtection().begin(player, "central-gui-click-" + slot)) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return;
        }

        try {
            switch (slot) {
                case TokensGui.SLOT_TOKEN_WITHDRAW: withdrawToken(player); break;
                case TokensGui.SLOT_TOKEN_CASHOUT: cashout(player); break;
                case TokensGui.SLOT_REFINED_TOGGLE: toggleRefinedStorage(player); break;
                case TokensGui.SLOT_REFINED_STORE: storePhysicalRefined(player); break;
                case TokensGui.SLOT_REFINED_WITHDRAW: withdrawRefined(player, true); break;
                case TokensGui.SLOT_COMPRESSED_WITHDRAW: withdrawRefined(player, false); break;
                default: return;
            }
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }

        gui.open(player);
    }

    private void withdrawToken(Player player) {
        UUID uuid = player.getUniqueId();
        int balance = plugin.getStorage().getTokens(uuid);
        if (balance < 1) {
            player.sendMessage(MessageUtil.color("&cYou don't have any tokens to withdraw."));
            return;
        }

        plugin.getStorage().setTokens(uuid, balance - 1);
        plugin.getStorage().flush();

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(tokenItem(1));
        if (!leftover.isEmpty()) {
            plugin.getDupeProtection().flag(player, "central-gui-token-withdraw", "Inventory overflow while withdrawing token");
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.sendMessage(MessageUtil.color("&aWithdrawn &e1 &atoken to your inventory."));
    }

    private void cashout(Player player) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(MessageUtil.color("&cEconomy system is unavailable."));
            return;
        }

        UUID uuid = player.getUniqueId();
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
        int balance = plugin.getStorage().getTokens(uuid);
        if (balance < cashTokens) {
            player.sendMessage(MessageUtil.color("&cYou need &e" + cashTokens + " &ctokens to cash out."));
            return;
        }

        plugin.getStorage().setTokens(uuid, balance - cashTokens);
        plugin.getStorage().flush();
        EconomyResponse response = economy.depositPlayer(player, cashAmount);
        if (!response.transactionSuccess()) {
            plugin.getStorage().setTokens(uuid, balance);
            plugin.getStorage().flush();
            plugin.getDupeProtection().flag(player, "central-gui-cashout", "Vault deposit failed after token deduction; balance refunded");
            player.sendMessage(MessageUtil.color("&cCashout failed. Your tokens were refunded."));
            return;
        }

        player.sendMessage(MessageUtil.color("&aCashed out &e" + cashTokens + " &atokens for &a$" + String.format("%.2f", cashAmount) + "&a!"));
    }

    private void toggleRefinedStorage(Player player) {
        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        boolean enabled = !storage.isAutoStoring(uuid);
        storage.setAutoStoring(uuid, enabled);
        storage.flush();
        player.sendMessage(MessageUtil.color(enabled ? "&aRefined auto-storage enabled." : "&cRefined auto-storage disabled."));
    }

    private void storePhysicalRefined(Player player) {
        int refined = 0;
        int compressed = 0;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (RefinedItemFactory.isRefined(plugin, item)) {
                refined += item.getAmount();
                contents[i] = null;
            } else if (RefinedItemFactory.isCompressed(plugin, item)) {
                compressed += item.getAmount();
                contents[i] = null;
            }
        }

        if (refined == 0 && compressed == 0) {
            player.sendMessage(MessageUtil.color("&eYou do not have any refined ore items to store."));
            return;
        }

        player.getInventory().setStorageContents(contents);
        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        storage.addRefined(uuid, refined);
        storage.addCompressed(uuid, compressed);
        storage.flush();
        player.sendMessage(MessageUtil.color("&aStored &b" + refined + " Refined Ore &aand &3" + compressed + " Compressed&a."));
    }

    private void withdrawRefined(Player player, boolean refined) {
        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        int balance = refined ? storage.getRefinedBalance(uuid) : storage.getCompressedBalance(uuid);
        if (balance < 1) {
            player.sendMessage(MessageUtil.color(refined ? "&cNo Refined Ore stored." : "&cNo Compressed Refined Ore stored."));
            return;
        }

        if (refined) {
            storage.setRefinedBalance(uuid, balance - 1);
        } else {
            storage.setCompressedBalance(uuid, balance - 1);
        }
        storage.flush();

        ItemStack item = refined ? RefinedItemFactory.makeRefined(plugin, 1) : RefinedItemFactory.makeCompressed(plugin, 1);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            plugin.getDupeProtection().flag(player, "central-gui-refined-withdraw", "Inventory overflow while withdrawing refined item");
            leftover.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
        }
        player.sendMessage(MessageUtil.color(refined ? "&aWithdrew &b1 Refined Ore&a." : "&aWithdrew &31 Compressed Refined Ore&a."));
    }

    private ItemStack tokenItem(int amount) {
        String itemName = plugin.getConfig().getString("token-item-name", "&6Kill Token");
        ItemStack tokenItem = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = tokenItem.getItemMeta();
        meta.setDisplayName(MessageUtil.color(itemName));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7Earned through honorable combat"),
            MessageUtil.color("&7Use &e/tokens cashout &7to exchange for money")
        ));
        tokenItem.setItemMeta(meta);
        return tokenItem;
    }
}
