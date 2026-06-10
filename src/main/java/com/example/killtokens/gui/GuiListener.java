package com.example.killtokens.gui;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedItemFactory;
import com.example.killtokens.refined.RefinedOreStorage;
import com.example.killtokens.util.MessageUtil;
import com.example.killtokens.util.TokenItemFactory;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class GuiListener implements Listener {

    private final KillTokensPlugin plugin;
    private final TokensGui gui;
    private final AmountGui amountGui;
    private final ShopGui shopGui;

    public GuiListener(KillTokensPlugin plugin, TokensGui gui, AmountGui amountGui, ShopGui shopGui) {
        this.plugin = plugin;
        this.gui = gui;
        this.amountGui = amountGui;
        this.shopGui = shopGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder)) return;
        GuiHolder holder = (GuiHolder) event.getInventory().getHolder();

        // Cancel everything in our views, including shift-clicks and hotbar swaps
        // from the player's own inventory.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        // Raw slots >= top inventory size are the player's own inventory.
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.getView()) {
            case MAIN:   handleMainGui(player, slot); break;
            case AMOUNT: handleAmountGui(player, slot, holder.getAmountType()); break;
            case SHOP:   handleShopGui(player, slot, holder.getShopSection()); break;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        // Without this, items dragged across our GUI slots vanish when the view closes.
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Main GUI
    // -------------------------------------------------------------------------

    private void handleMainGui(Player player, int slot) {
        if (slot == TokensGui.SLOT_CLOSE) {
            click(player);
            player.closeInventory();
            return;
        }

        switch (slot) {
            case TokensGui.SLOT_TOKEN_WITHDRAW: {
                click(player);
                int balance = plugin.getStorage().getTokens(player.getUniqueId());
                amountGui.open(player, AmountGui.Type.TOKEN_WITHDRAW, balance);
                break;
            }
            case TokensGui.SLOT_TOKEN_CASHOUT: {
                click(player);
                int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
                int balance = plugin.getStorage().getTokens(player.getUniqueId());
                int maxCashouts = (cashTokens > 0) ? balance / cashTokens : 0;
                amountGui.open(player, AmountGui.Type.TOKEN_CASHOUT, maxCashouts);
                break;
            }
            case TokensGui.SLOT_REFINED_TOGGLE: {
                if (!plugin.getDupeProtection().begin(player, "gui-refined-toggle")) {
                    denyBusy(player);
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
                    denyBusy(player);
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
                click(player);
                int balance = plugin.getRefinedStorage().getRefinedBalance(player.getUniqueId());
                amountGui.open(player, AmountGui.Type.REFINED_WITHDRAW, balance);
                break;
            }
            case TokensGui.SLOT_COMPRESSED_WITHDRAW: {
                click(player);
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
            click(player);
            gui.open(player);
            return;
        }

        int idx = slotToAmountIndex(slot);
        if (idx < 0) return;

        int rawAmount = AmountGui.BASE_AMOUNTS[idx];

        if (!plugin.getDupeProtection().begin(player, "amount-gui-" + type.name())) {
            denyBusy(player);
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

        // Refresh the main GUI after the action
        gui.open(player);
    }

    // -------------------------------------------------------------------------
    // Shop GUI
    // -------------------------------------------------------------------------

    private void handleShopGui(Player player, int slot, String sectionId) {
        java.util.List<String> sections = shopGui.sectionIds();

        int tabIdx = ShopGui.tabIndex(slot, sections.size());
        if (tabIdx >= 0) {
            click(player);
            shopGui.open(player, sections.get(tabIdx));
            return;
        }

        if (slot == ShopGui.SLOT_CLOSE) {
            click(player);
            player.closeInventory();
            return;
        }

        int itemIdx = -1;
        for (int i = 0; i < ShopGui.ITEM_SLOTS.length; i++) {
            if (ShopGui.ITEM_SLOTS[i] == slot) { itemIdx = i; break; }
        }
        if (itemIdx < 0) return;

        java.util.List<ShopGui.ShopItem> items = shopGui.items(sectionId);
        if (itemIdx >= items.size()) return;

        if (!plugin.getDupeProtection().begin(player, "shop-buy")) {
            denyBusy(player);
            return;
        }
        try {
            performPurchase(player, items.get(itemIdx));
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }

        // Refresh so balances and stock displays update
        shopGui.open(player, sectionId);
    }

    private void performPurchase(Player player, ShopGui.ShopItem item) {
        UUID uuid = player.getUniqueId();
        RefinedOreStorage refined = plugin.getRefinedStorage();
        Economy economy = plugin.getEconomy();

        // Verify every cost up front
        int refinedBal = refined.getRefinedBalance(uuid);
        int compBal = refined.getCompressedBalance(uuid);
        int tokenBal = plugin.getStorage().getTokens(uuid);

        if (item.costRefined > refinedBal) {
            deny(player);
            player.sendMessage(MessageUtil.color("&cYou need &b" + item.costRefined + " Refined Ore &c(have " + refinedBal + ")."));
            return;
        }
        if (item.costCompressed > compBal) {
            deny(player);
            player.sendMessage(MessageUtil.color("&cYou need &d" + item.costCompressed + " Compressed &c(have " + compBal + ")."));
            return;
        }
        if (item.costTokens > tokenBal) {
            deny(player);
            player.sendMessage(MessageUtil.color("&cYou need &e" + item.costTokens + " Kill Tokens &c(have " + tokenBal + ")."));
            return;
        }
        if (item.costMoney > 0) {
            if (economy == null) {
                deny(player);
                player.sendMessage(MessageUtil.color("&cEconomy system is unavailable. Contact an administrator."));
                return;
            }
            if (!economy.has(player, item.costMoney)) {
                deny(player);
                player.sendMessage(MessageUtil.color("&cYou need &a$" + String.format("%.2f", item.costMoney) + "&c."));
                return;
            }
        }

        // Deduct virtual currencies first (cheap to refund), then money
        if (item.costRefined > 0)    refined.setRefinedBalance(uuid, refinedBal - item.costRefined);
        if (item.costCompressed > 0) refined.setCompressedBalance(uuid, compBal - item.costCompressed);
        if (item.costTokens > 0)     plugin.getStorage().setTokens(uuid, tokenBal - item.costTokens);

        if (item.costMoney > 0) {
            EconomyResponse response = economy.withdrawPlayer(player, item.costMoney);
            if (!response.transactionSuccess()) {
                // Refund virtual currencies
                if (item.costRefined > 0)    refined.setRefinedBalance(uuid, refinedBal);
                if (item.costCompressed > 0) refined.setCompressedBalance(uuid, compBal);
                if (item.costTokens > 0)     plugin.getStorage().setTokens(uuid, tokenBal);
                refined.flush();
                plugin.getStorage().flush();
                plugin.getDupeProtection().flag(player, "shop-buy", "Vault withdraw failed after virtual deduction; refunded");
                deny(player);
                player.sendMessage(MessageUtil.color("&cPurchase failed. Your balances were refunded."));
                return;
            }
        }
        refined.flush();
        plugin.getStorage().flush();

        // Deliver
        if (item.giveItem) {
            ItemStack purchased = new ItemStack(item.material, item.amount);
            if (item.name != null || !item.lore.isEmpty()) {
                org.bukkit.inventory.meta.ItemMeta meta = purchased.getItemMeta();
                if (item.name != null) meta.setDisplayName(MessageUtil.color(item.name));
                if (!item.lore.isEmpty()) {
                    java.util.List<String> colored = new java.util.ArrayList<>();
                    for (String line : item.lore) colored.add(MessageUtil.color(line));
                    meta.setLore(colored);
                }
                purchased.setItemMeta(meta);
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(purchased);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
                player.sendMessage(MessageUtil.color("&eInventory full! Your purchase dropped at your location."));
            }
        }
        for (String command : item.commands) {
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                command.replace("{player}", player.getName()));
        }

        success(player);
        player.sendMessage(MessageUtil.color("&aPurchased &f" + item.displayName() + "&a!"));
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
            deny(player);
            player.sendMessage(MessageUtil.color("&cYou don't have any tokens to withdraw."));
            return;
        }
        if (amount > balance) {
            deny(player);
            player.sendMessage(MessageUtil.color("&cInsufficient tokens. You have &e" + balance + "&c."));
            return;
        }

        plugin.getStorage().setTokens(uuid, balance - amount);
        plugin.getStorage().flush();

        giveTokens(player, amount);
        success(player);
        player.sendMessage(MessageUtil.color("&aWithdrawn &e" + amount + " &atoken(s) to your inventory."));
    }

    private void performTokenCashout(Player player, int rawAmount) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            deny(player);
            player.sendMessage(MessageUtil.color("&cEconomy system is unavailable. Contact an administrator."));
            return;
        }

        UUID uuid = player.getUniqueId();
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);

        if (cashTokens <= 0) {
            deny(player);
            player.sendMessage(MessageUtil.color("&cCashout is misconfigured. Contact an administrator."));
            return;
        }

        int balance = plugin.getStorage().getTokens(uuid);
        int maxCashouts = balance / cashTokens;

        // -1 means "All"
        int times = (rawAmount == -1) ? maxCashouts : rawAmount;

        if (times <= 0) {
            deny(player);
            player.sendMessage(MessageUtil.color("&cNo cashouts available."));
            return;
        }
        if (times > maxCashouts) {
            deny(player);
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
            deny(player);
            player.sendMessage(MessageUtil.color("&cCashout failed. Your tokens were refunded."));
            return;
        }

        success(player);
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
            deny(player);
            player.sendMessage(MessageUtil.color(isRefined
                ? "&cNo Refined Ore stored." : "&cNo Compressed Refined Ore stored."));
            return;
        }
        if (amount > balance) {
            deny(player);
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
        success(player);
        player.sendMessage(MessageUtil.color(isRefined
            ? "&aWithdrawn &b" + amount + " Refined Ore&a."
            : "&aWithdrawn &b" + amount + " Compressed Refined Ore&a."));
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
        success(player);
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
            deny(player);
            player.sendMessage(MessageUtil.color("&eNo refined ore items found in inventory."));
            return;
        }
        player.getInventory().setStorageContents(contents);
        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        storage.addRefined(uuid, refined);
        storage.addCompressed(uuid, compressed);
        storage.flush();
        success(player);
        player.sendMessage(MessageUtil.color("&aStored &b" + refined + " Refined Ore &aand &b" + compressed + " Compressed&a."));
    }

    // -------------------------------------------------------------------------
    // Item helpers
    // -------------------------------------------------------------------------

    private void giveTokens(Player player, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(64, remaining);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(TokenItemFactory.make(plugin, stack));
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

    // -------------------------------------------------------------------------
    // Feedback sounds (mapped by Geyser for Bedrock clients)
    // -------------------------------------------------------------------------

    private void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    private void success(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
    }

    private void deny(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1.0f);
    }

    private void denyBusy(Player player) {
        deny(player);
        player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
    }
}
