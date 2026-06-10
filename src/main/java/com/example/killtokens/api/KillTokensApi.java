package com.example.killtokens.api;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedItemFactory;
import com.example.killtokens.util.TokenItemFactory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Stable entry point for other plugins.
 *
 * <pre>{@code
 * KillTokensApi api = KillTokensApi.get();
 * if (api != null) {
 *     api.addTokens(player.getUniqueId(), 5);
 * }
 * }</pre>
 *
 * All methods must be called from the main server thread. {@link #get()} returns
 * null while KillTokens is disabled, so depend on it via softdepend/depend in your
 * plugin.yml and check for null.
 */
public final class KillTokensApi {

    private static KillTokensApi instance;

    private final KillTokensPlugin plugin;

    private KillTokensApi(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return the API, or null if KillTokens is not enabled. */
    public static KillTokensApi get() {
        return instance;
    }

    // ----- Kill tokens -----

    public int getTokens(UUID player) {
        return plugin.getStorage().getTokens(player);
    }

    public void setTokens(UUID player, int amount) {
        plugin.getStorage().setTokens(player, amount);
        plugin.getStorage().flush();
    }

    public void addTokens(UUID player, int amount) {
        plugin.getStorage().addTokens(player, amount);
        plugin.getStorage().flush();
    }

    // ----- Refined ore storage -----

    public int getRefinedBalance(UUID player) {
        return plugin.getRefinedStorage().getRefinedBalance(player);
    }

    public int getCompressedBalance(UUID player) {
        return plugin.getRefinedStorage().getCompressedBalance(player);
    }

    public void addRefined(UUID player, int amount) {
        plugin.getRefinedStorage().addRefined(player, amount);
        plugin.getRefinedStorage().flush();
    }

    public void addCompressed(UUID player, int amount) {
        plugin.getRefinedStorage().addCompressed(player, amount);
        plugin.getRefinedStorage().flush();
    }

    public boolean isAutoStoring(UUID player) {
        return plugin.getRefinedStorage().isAutoStoring(player);
    }

    // ----- Plugin items (PDC-tagged; safe to trade in shops) -----

    public ItemStack createTokenItem(int amount) {
        return TokenItemFactory.make(plugin, amount);
    }

    public ItemStack createRefinedItem(int amount) {
        return RefinedItemFactory.makeRefined(plugin, amount);
    }

    public ItemStack createCompressedItem(int amount) {
        return RefinedItemFactory.makeCompressed(plugin, amount);
    }

    public boolean isTokenItem(ItemStack item) {
        return TokenItemFactory.isToken(plugin, item);
    }

    public boolean isRefinedItem(ItemStack item) {
        return RefinedItemFactory.isRefined(plugin, item);
    }

    public boolean isCompressedItem(ItemStack item) {
        return RefinedItemFactory.isCompressed(plugin, item);
    }

    // ----- Lifecycle (internal) -----

    public static void register(KillTokensPlugin plugin) {
        instance = new KillTokensApi(plugin);
    }

    public static void unregister() {
        instance = null;
    }
}
