package com.example.killtokens.commands;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "balance":  return handleBalance(player);
            case "withdraw": return handleWithdraw(player, args);
            case "cashout":  return handleCashout(player);
            case "set":      return handleSet(player, args);
            case "add":      return handleAdd(player, args);
            case "remove":   return handleRemove(player, args);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleBalance(Player player) {
        int tokens = plugin.getStorage().getTokens(player.getUniqueId());
        int cashTokens = plugin.getConfig().getInt("cash-tokens");
        double cashAmount = plugin.getConfig().getDouble("cash-amount");

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

        int balance = plugin.getStorage().getTokens(player.getUniqueId());
        if (balance < amount) {
            player.sendMessage(MessageUtil.color("&cInsufficient tokens. You have &e" + balance + " &ctokens."));
            return true;
        }

        plugin.getStorage().setTokens(player.getUniqueId(), balance - amount);

        String itemName = plugin.getConfig().getString("token-item-name", "&6Kill Token");
        ItemStack tokenItem = new ItemStack(Material.GOLD_NUGGET, amount);
        ItemMeta meta = tokenItem.getItemMeta();
        meta.setDisplayName(MessageUtil.color(itemName));
        meta.setLore(Arrays.asList(
            MessageUtil.color("&7Earned through honorable combat"),
            MessageUtil.color("&7Use &e/tokens cashout &7to exchange for money")
        ));
        tokenItem.setItemMeta(meta);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(tokenItem);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            player.sendMessage(MessageUtil.color("&eInventory full! Excess tokens dropped at your location."));
        } else {
            player.sendMessage(MessageUtil.color("&aWithdrawn &e" + amount + " &atokens to your inventory."));
        }
        return true;
    }

    private boolean handleCashout(Player player) {
        Economy economy = plugin.getEconomy();
        if (economy == null) {
            player.sendMessage(MessageUtil.color("&cEconomy system is unavailable. Please contact an administrator."));
            return true;
        }

        int cashTokens = plugin.getConfig().getInt("cash-tokens");
        double cashAmount = plugin.getConfig().getDouble("cash-amount");
        int balance = plugin.getStorage().getTokens(player.getUniqueId());

        if (balance < cashTokens) {
            player.sendMessage(MessageUtil.color(
                "&cYou need &e" + cashTokens + " &ctokens to cash out. You have &e" + balance + "&c."));
            return true;
        }

        plugin.getStorage().setTokens(player.getUniqueId(), balance - cashTokens);
        economy.depositPlayer(player, cashAmount);
        player.sendMessage(MessageUtil.color(
            "&aCashed out &e" + cashTokens + " &atokens for &a$" + String.format("%.2f", cashAmount) + "&a!"));
        return true;
    }

    private boolean handleSet(Player player, String[] args) {
        if (!player.hasPermission("killtokens.admin")) {
            player.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(MessageUtil.color("&cUsage: /tokens set <player> <amount>"));
            return true;
        }

        int amount = parseNonNegativeInt(player, args[2]);
        if (amount < 0) return true;

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        plugin.getStorage().setTokens(target.getUniqueId(), amount);
        player.sendMessage(MessageUtil.color("&aSet &e" + args[1] + "&a's tokens to &e" + amount + "&a."));
        return true;
    }

    private boolean handleAdd(Player player, String[] args) {
        if (!player.hasPermission("killtokens.admin")) {
            player.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(MessageUtil.color("&cUsage: /tokens add <player> <amount>"));
            return true;
        }

        int amount = parsePositiveInt(player, args[2]);
        if (amount < 0) return true;

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        plugin.getStorage().addTokens(target.getUniqueId(), amount);
        player.sendMessage(MessageUtil.color("&aAdded &e" + amount + " &atokens to &e" + args[1] + "&a."));
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        if (!player.hasPermission("killtokens.admin")) {
            player.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(MessageUtil.color("&cUsage: /tokens remove <player> <amount>"));
            return true;
        }

        int amount = parsePositiveInt(player, args[2]);
        if (amount < 0) return true;

        OfflinePlayer target = resolveOfflinePlayer(args[1]);
        int current = plugin.getStorage().getTokens(target.getUniqueId());
        plugin.getStorage().setTokens(target.getUniqueId(), current - amount);
        player.sendMessage(MessageUtil.color("&aRemoved &e" + amount + " &atokens from &e" + args[1] + "&a."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("balance", "withdraw", "cashout"));
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

    /** Returns -1 (sentinel) and sends an error if parsing fails or amount <= 0. */
    private static int parsePositiveInt(Player player, String arg) {
        int amount = parseNonNegativeInt(player, arg);
        if (amount == 0) {
            player.sendMessage(MessageUtil.color("&cAmount must be a positive number."));
            return -1;
        }
        return amount;
    }

    /** Returns -1 (sentinel) and sends an error if parsing fails or amount < 0. */
    private static int parseNonNegativeInt(Player player, String arg) {
        int amount;
        try {
            amount = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.color("&cInvalid amount. Please enter a whole number."));
            return -1;
        }
        if (amount < 0) {
            player.sendMessage(MessageUtil.color("&cAmount cannot be negative."));
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
        player.sendMessage(MessageUtil.color("&6Usage: /tokens <balance|withdraw|cashout|set|add|remove>"));
    }
}
