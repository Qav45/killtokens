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
    private final AmountGui amountGui;

    public GuiListener(KillTokensPlugin plugin, TokensGui gui, AmountGui amountGui) {
        this.plugin = plugin;
        this.gui = gui;
        this.amountGui = amountGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (title.equals(gui.title())) {
            event.setCancelled(true);
            if (slot >= 0 && slot < TokensGui.SIZE) {
                handleMainGui(player, slot);
            }
            return;
        }

        for (AmountGui.Type type : AmountGui.Type.values()) {
            if (title.equals(amountGui.titleFor(type))) {
                event.setCancelled(true);
                if (slot >= 0 && slot < AmountGui.SIZE) {
                    handleAmountGui(player, slot, type);
                }
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Main GUI
    // -------------------------------------------------------------------------

    private void handleMainGui(Player player, int slot) {
        if (slot == TokensGui.SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case TokensGui.SLOT_TOKEN_WITHDRAW: {
                int balance = plugin.getStorage().getTokens(player.getUniqueId());
                amountGui.open(player, AmountGui.Type.TOKEN_WITHDRAW, balance);
                break;
            }
            case TokensGui.SLOT_TOKEN_CASHOUT: {
                int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
                int balance = plugin.getStorage().getTokens(player.getUniqueId());
                int maxCashouts = (cashTokens > 0) ? balance / cashTokens : 0;
                amountGui.open(player, AmountGui.Type.TOKEN_CASHOUT, maxCashouts);
                break;
            }
            case TokensGui.SLOT_REFINED_TOGGLE: {
                if (!plugin.getDupeProtection().begin(player, "gui-refined-toggle")) {
                    player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
                    return;
                }
                try {
                    toggleRefinedStorage(player);
                } finally {
                    plugin.getDupeProtection().end(player.getUniqueId());
                }
                gui.open(player);
                break;
            }
            case TokensGui.SLOT_REFINED_STORE: {
                if (!plugin.getDupeProtection().begin(player, "gui-refined-store")) {
                    player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
                    return;
                }
                try {
                    storePhysicalRefined(player);
                } finally {
                    plugin.getDupeProtection().end(player.getUniqueId());
                }
                gui.open(player);
                break;
            }
            case TokensGui.SLOT_REFINED_WITHDRAW: {
                int balance = plugin.getRefinedStorage().getRefinedBalance(player.getUniqueId());
                amountGui.open(player, AmountGui.Type.REFINED_WITHDRAW, balance);
                break;
            }
            case TokensGui.SLOT_COMPRESSED_WITHDRAW: {
                int balance = plugin.getRefinedStorage().getCompressedBalance(player.getUniqueId());
                amountGui.open(player, AmountGui.Type.COMPRESSED_WITHDRAW, balance);
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Amount GUI
    // -------------------------------------------------------------------------

    private void handleAmountGui(Player player, int slot, AmountGui.Type type) {
        if (slot == AmountGui.SLOT_BACK) {
            gui.open(player);
            return;
        }

        int idx = slotToAmountIndex(slot);
        if (idx < 0) return;

        int rawAmount = AmountGui.BASE_AMOUNTS[idx];

        if (!plugin.getDupeProtection().begin(player, "amount-gui-" + type.name())) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return;
        }
        try {
            switch (type) {
                case TOKEN_WITHDRAW:     performTokenWithdraw(player, rawAmount);     break;
                case TOKEN_CASHOUT:      performTokenCashout(player, rawAmount);      break;
                case REFINED_WITHDRAW:   performRefinedWithdraw(player, rawAmount, true);  break;
                case COMPRESSED_WITHDRAW: performRefinedWithdraw(player, rawAmount, false); break;
            }
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }

        // Re-open main GUI after a successful action
        gui.open(player);
    }

    private int slotToAmountIndex(int slot) {
        for (int i = 0; i < AmountGui.AMOUNT_SLOTS.length; i++) {
            if (AmountGui.AMOUNT_SLOTS[i] == slot) return i;
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Transactions
    // -------------------------------------------------------------------------

    private void performTokenWithdraw(Player player, int rawAmount) {
        UUID uuid = player.getUniqueId();
        int balance = plugin.getStorage().getTokens(uuid);

        // -1 means "All"
        int amount = (rawAmount == -1) ? balance : rawAmount;

        if (amount <= 0) {
            player.sendMessage(MessageUtil.color("&cYou don't have any tokens to withdraw."));
            return;
        }
        if (amount > balance) {
            player.sendMessage(MessageUtil.color("&cInsufficient tokens. You have &e" + balance + "&c."));
            return;
        }

        plugin.getStorage().setTokens(uuid, balance - amount);
        plugin.getStorage().flush();

        giveTokens(player, amount);
        player.sendMessage(MessageUtil.color("&aWithdrawn &e" + amount + " &atoken(s) to your inventory."));
    }

    private void performTokenCashout(Player player, int rawAmount) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(MessageUtil.color("&cEconomy system is unavailable. Contact an administrator."));
            return;
        }

        UUID uuid = player.getUniqueId();
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);

        if (cashTokens <= 0) {
            player.sendMessage(MessageUtil.color("&cCashout is misconfigured. Contact an administrator."));
            return;
        }

        int balance = plugin.getStorage().getTokens(uuid);
        int maxCashouts = balance / cashTokens;

        // -1 means "All"
        int times = (rawAmount == -1) ? maxCashouts : rawAmount;

        if (times <= 0) {
            player.sendMessage(MessageUtil.color("&cNo cashouts available."));
            return;
        }
        if (times > maxCashouts) {
            player.sendMessage(MessageUtil.color("&cNot enough tokens. You can perform &e" + maxCashouts + " &ccashout(s)."));
            return;
        }

        int totalCost = times * cashTokens;
        double totalPayout = times * cashAmount;

        // Deduct before depositing to prevent dupe on crash
        plugin.getStorage().setTokens(uuid, balance - totalCost);
        plugin.getStorage().flush();

        EconomyResponse response = economy.depositPlayer(player, totalPayout);
        if (!response.transactionSuccess()) {
            plugin.getStorage().setTokens(uuid, balance);
            plugin.getStorage().flush();
            plugin.getDupeProtection().flag(player, "gui-cashout", "Vault deposit failed after deduction; refunded");
            player.sendMessage(MessageUtil.color("&cCashout failed. Your tokens were refunded."));
            return;
        }

        player.sendMessage(MessageUtil.color(
            "&aCashed out &e" + times + "x &a(&e" + totalCost + " tokens&a) for &a$" +
            String.format("%.2f", totalPayout) + "&a!"));
    }

    private void performRefinedWithdraw(Player player, int rawAmount, boolean isRefined) {
        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        int balance = isRefined ? storage.getRefinedBalance(uuid) : storage.getCompressedBalance(uuid);

        int amount = (rawAmount == -1) ? balance : rawAmount;

        if (amount <= 0) {
            player.sendMessage(MessageUtil.color(isRefined
                ? "&cNo Refined Ore stored." : "&cNo Compressed Refined Ore stored."));
            return;
        }
        if (amount > balance) {
            player.sendMessage(MessageUtil.color("&cInsufficient " + (isRefined ? "Refined Ore" : "Compressed") +
                ". You have &e" + balance + "&c."));
            return;
        }

        if (isRefined) {
            storage.setRefinedBalance(uuid, balance - amount);
        } else {
            storage.setCompressedBalance(uuid, balance - amount);
        }
        storage.flush();

        giveRefinedSplit(player, isRefined, amount);
        player.sendMessage(MessageUtil.color(isRefined
            ? "&aWithdrawn &b" + amount + " Refined Ore&a."
            : "&aWithdrawn &d" + amount + " Compressed Refined Ore&a."));
    }

    // -------------------------------------------------------------------------
    // Non-withdraw actions
    // -------------------------------------------------------------------------

    private void toggleRefinedStorage(Player player) {
        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        boolean enabled = !storage.isAutoStoring(uuid);
        storage.setAutoStoring(uuid, enabled);
        storage.flush();
        player.sendMessage(MessageUtil.color(enabled
            ? "&aRefined auto-storage &lenabled&a." : "&cRefined auto-storage &ldisabled&c."));
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
            player.sendMessage(MessageUtil.color("&eNo refined ore items found in inventory."));
            return;
        }
        player.getInventory().setStorageContents(contents);
        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        storage.addRefined(uuid, refined);
        storage.addCompressed(uuid, compressed);
        storage.flush();
        player.sendMessage(MessageUtil.color("&aStored &b" + refined + " Refined Ore &aand &d" + compressed + " Compressed&a."));
    }

    // -------------------------------------------------------------------------
    // Item helpers
    // -------------------------------------------------------------------------

    private void giveTokens(Player player, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(64, remaining);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(tokenItem(stack));
            if (!leftover.isEmpty()) {
                plugin.getDupeProtection().flag(player, "gui-token-withdraw", "Inventory overflow withdrawing tokens");
                leftover.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            }
            remaining -= stack;
        }
    }

    private void giveRefinedSplit(Player player, boolean refined, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(64, remaining);
            ItemStack item = refined
                ? RefinedItemFactory.makeRefined(plugin, stack)
                : RefinedItemFactory.makeCompressed(plugin, stack);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                plugin.getDupeProtection().flag(player, "gui-refined-withdraw", "Inventory overflow withdrawing refined");
                leftover.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
            }
            remaining -= stack;
        }
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
