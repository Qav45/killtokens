package com.example.killtokens.placeholders;

import com.example.killtokens.KillTokensPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Available placeholders:
 *   %killtokens_balance%       – player's current token balance
 *   %killtokens_kill_reward%   – tokens awarded per kill
 *   %killtokens_cashout_cost%  – tokens required for a cashout
 *   %killtokens_cashout_value% – money received per cashout
 *   %killtokens_tokens_needed% – tokens still needed until next cashout (0 if ready)
 *   %killtokens_can_cashout%   – "true" if player has enough tokens, "false" otherwise
 */
public class KillTokensExpansion extends PlaceholderExpansion {

    private final KillTokensPlugin plugin;

    public KillTokensExpansion(KillTokensPlugin plugin) {
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

        int balance    = plugin.getStorage().getTokens(player.getUniqueId());
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmt = plugin.getConfig().getDouble("cash-amount", 100.0);
        int killReward = plugin.getConfig().getInt("kill-reward", 1);

        switch (params.toLowerCase()) {
            case "balance":       return String.valueOf(balance);
            case "kill_reward":   return String.valueOf(killReward);
            case "cashout_cost":  return String.valueOf(cashTokens);
            case "cashout_value": return String.format("%.2f", cashAmt);
            case "tokens_needed": return String.valueOf(Math.max(0, cashTokens - balance));
            case "can_cashout":   return balance >= cashTokens ? "true" : "false";
            default:              return null;
        }
    }
}
