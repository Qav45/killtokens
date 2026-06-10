package com.example.killtokens.placeholders;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedOreStorage;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class RefinedPAPIExpansion extends PlaceholderExpansion {

    private final KillTokensPlugin plugin;

    public RefinedPAPIExpansion(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "killtokens";
    }

    @Override
    public @NotNull String getAuthor() {
        List<String> authors = plugin.getDescription().getAuthors();
        return authors.isEmpty() ? "Unknown" : String.join(", ", authors);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        UUID uuid = player.getUniqueId();
        RefinedOreStorage refined = plugin.getRefinedStorage();
        switch (params.toLowerCase()) {
            case "refined_balance": return String.valueOf(refined.getRefinedBalance(uuid));
            case "compressed_balance": return String.valueOf(refined.getCompressedBalance(uuid));
            case "refined_total": return String.valueOf(refined.getRefinedTotal(uuid));
            case "compressed_total": return String.valueOf(refined.getCompressedTotal(uuid));
            case "refined_pity": return String.valueOf(refined.getRefinedPity(uuid));
            case "compressed_pity": return String.valueOf(refined.getCompressedPity(uuid));
            case "auto_storage": return refined.isAutoStoring(uuid) ? "true" : "false";
            case "dupe_flags": return String.valueOf(plugin.getDupeProtection().getFlagCount(uuid));
            case "last_dupe_flag": return plugin.getDupeProtection().getLastFlag(uuid);
            default: return existingTokenPlaceholder(uuid, params);
        }
    }

    private String existingTokenPlaceholder(UUID uuid, String params) {
        int balance = plugin.getStorage().getTokens(uuid);
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
        int killReward = plugin.getConfig().getInt("kill-reward", 1);

        switch (params.toLowerCase()) {
            case "balance": return String.valueOf(balance);
            case "kill_reward": return String.valueOf(killReward);
            case "cashout_cost": return String.valueOf(cashTokens);
            case "cashout_value": return String.format("%.2f", cashAmount);
            case "tokens_needed": return String.valueOf(Math.max(0, cashTokens - balance));
            case "can_cashout": return balance >= cashTokens ? "true" : "false";
            case "cashout_ready": return balance >= cashTokens ? "true" : "false";
            case "total_virtual_items":
                RefinedOreStorage refined = plugin.getRefinedStorage();
                return String.valueOf(balance + refined.getRefinedBalance(uuid) + refined.getCompressedBalance(uuid));
            default: return null;
        }
    }
}
