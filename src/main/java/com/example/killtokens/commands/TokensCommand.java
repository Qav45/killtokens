package com.example.killtokens.commands;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import com.example.killtokens.util.TokenItemFactory;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TokensCommand implements CommandExecutor, TabCompleter {

    private final KillTokensPlugin plugin;

    public TokensCommand(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "gui" : args[0].toLowerCase();

        // Admin subcommands work from console too
        switch (sub) {
            case "set":
            case "add":
            case "remove":
                return handleAdmin(sender, sub, args);
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players (console may use set/add/remove).");
            return true;
        }

        Player player = (Player) sender;

        switch (sub) {
            case "gui":      plugin.getTokensGui().open(player); return true;
            case "balance":  return handleBalance(player);
            case "withdraw": return handleWithdraw(player, args);
            case "cashout":  return handleCashout(player);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleBalance(Player player) {
        int tokens = plugin.getStorage().getTokens(player.getUniqueId());
        int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
        double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);

        player.sendMessage(MessageUtil.color("&8&l===================="));
        player.sendMessage(MessageUtil.color("&6&l  KillTokens Balance"));
        player.sendMessage(MessageUtil.color("&8&l===================="));
        player.sendMessage(MessageUtil.color("&7  Kill Tokens: &a" + tokens));
        player.sendMessage(MessageUtil.color("&7  Cash Out: &e" + cashTokens + " tokens &7→ &a$" + String.format("%.2f", cashAmount)));
        player.sendMessage(MessageUtil.color("&8&l===================="));
        return true;
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(MessageUtil.color("&cUsage: /tokens withdraw <amount>"));
            return true;
        }

        int amount = parsePositiveInt(player, args[1]);
        if (amount < 0) return true;
        if (amount > 64) {
            player.sendMessage(MessageUtil.color("&cYou can only withdraw up to 64 tokens at a time."));
            return true;
        }

        if (!plugin.getDupeProtection().begin(player, "tokens-withdraw")) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return true;
        }

        try {
            int balance = plugin.getStorage().getTokens(player.getUniqueId());
            if (balance < amount) {
                player.sendMessage(MessageUtil.color("&cInsufficient tokens. You have &e" + balance + " &ctokens."));
                return true;
            }

            // Deduct before giving to prevent dupe on crash
            plugin.getStorage().setTokens(player.getUniqueId(), balance - amount);
            plugin.getStorage().flush();

            Map<Integer, ItemStack> leftover = player.getInventory().addItem(TokenItemFactory.make(plugin, amount));
            if (!leftover.isEmpty()) {
                plugin.getDupeProtection().flag(player, "tokens-withdraw", "Inventory overflow while withdrawing tokens");
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                player.sendMessage(MessageUtil.color("&eInventory full! Excess tokens dropped at your location."));
            } else {
                player.sendMessage(MessageUtil.color("&aWithdrawn &e" + amount + " &atokens to your inventory."));
            }
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }
        return true;
    }

    private boolean handleCashout(Player player) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(MessageUtil.color("&cEconomy system is unavailable. Please contact an administrator."));
            return true;
        }

        if (!plugin.getDupeProtection().begin(player, "tokens-cashout")) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return true;
        }

        try {
            int cashTokens = plugin.getConfig().getInt("cash-tokens", 10);
            double cashAmount = plugin.getConfig().getDouble("cash-amount", 100.0);
            int balance = plugin.getStorage().getTokens(player.getUniqueId());

            if (balance < cashTokens) {
                player.sendMessage(MessageUtil.color(
                    "&cYou need &e" + cashTokens + " &ctokens to cash out. You have &e" + balance + "&c."));
                return true;
            }

            // Deduct before depositing to prevent dupe on crash
            plugin.getStorage().setTokens(player.getUniqueId(), balance - cashTokens);
            plugin.getStorage().flush();
            EconomyResponse response = economy.depositPlayer(player, cashAmount);
            if (!response.transactionSuccess()) {
                plugin.getStorage().setTokens(player.getUniqueId(), balance);
                plugin.getStorage().flush();
                plugin.getDupeProtection().flag(player, "tokens-cashout", "Vault deposit failed after token deduction; balance refunded");
                player.sendMessage(MessageUtil.color("&cCashout failed. Your tokens were refunded."));
                return true;
            }
            player.sendMessage(MessageUtil.color(
                "&aCashed out &e" + cashTokens + " &atokens for &a$" + String.format("%.2f", cashAmount) + "&a!"));
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission("killtokens.admin")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(MessageUtil.color("&cUsage: /tokens " + sub + " <player> <amount>"));
            return true;
        }

        int amount = sub.equals("set")
            ? parseNonNegativeInt(sender, args[2])
            : parsePositiveInt(sender, args[2]);
        if (amount < 0) return true;

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        switch (sub) {
            case "set":
                plugin.getStorage().setTokens(target.getUniqueId(), amount);
                sender.sendMessage(MessageUtil.color("&aSet &e" + args[1] + "&a's tokens to &e" + amount + "&a."));
                break;
            case "add":
                plugin.getStorage().addTokens(target.getUniqueId(), amount);
                sender.sendMessage(MessageUtil.color("&aAdded &e" + amount + " &atokens to &e" + args[1] + "&a."));
                break;
            case "remove":
                int current = plugin.getStorage().getTokens(target.getUniqueId());
                plugin.getStorage().setTokens(target.getUniqueId(), Math.max(0, current - amount));
                sender.sendMessage(MessageUtil.color("&aRemoved &e" + amount + " &atokens from &e" + args[1] + "&a."));
                break;
        }
        plugin.getStorage().flush();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = sender instanceof Player
                ? new ArrayList<>(Arrays.asList("gui", "balance", "withdraw", "cashout"))
                : new ArrayList<>();
            if (sender.hasPermission("killtokens.admin")) {
                subs.addAll(Arrays.asList("set", "add", "remove"));
            }
            return subs.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && sender.hasPermission("killtokens.admin")) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("add") || sub.equals("remove")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private static int parsePositiveInt(CommandSender sender, String arg) {
        int amount = parseNonNegativeInt(sender, arg);
        if (amount == 0) {
            sender.sendMessage(MessageUtil.color("&cAmount must be a positive number."));
            return -1;
        }
        return amount;
    }

    private static int parseNonNegativeInt(CommandSender sender, String arg) {
        int amount;
        try {
            amount = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.color("&cInvalid amount. Please enter a whole number."));
            return -1;
        }
        if (amount < 0) {
            sender.sendMessage(MessageUtil.color("&cAmount cannot be negative."));
            return -1;
        }
        return amount;
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveOfflinePlayer(String name) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!target.hasPlayedBefore()) {
            plugin.getLogger().warning("Writing token data for player who may have never joined: " + name);
        }
        return target;
    }

    private void sendUsage(Player player) {
        player.sendMessage(MessageUtil.color("&6Usage: /tokens <gui|balance|withdraw|cashout|set|add|remove>"));
    }
}
