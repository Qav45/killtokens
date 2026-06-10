package com.example.killtokens.commands;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.refined.RefinedItemFactory;
import com.example.killtokens.refined.RefinedOreStorage;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RefinedCommand implements CommandExecutor, TabCompleter {

    private final KillTokensPlugin plugin;

    public RefinedCommand(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("killtokens.refined")) {
            player.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            plugin.getTokensGui().open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("balance")) {
            return handleBalance(player);
        }

        switch (args[0].toLowerCase()) {
            case "storage": return handleStorage(player);
            case "store": return handleStore(player);
            case "withdraw": return handleWithdraw(player, args);
            default:
                sendUsage(player);
                return true;
        }
    }

    private boolean handleBalance(Player player) {
        RefinedOreStorage storage = plugin.getRefinedStorage();
        UUID uuid = player.getUniqueId();
        player.sendMessage(MessageUtil.color("&8&l===================="));
        player.sendMessage(MessageUtil.color("&b&l  Refined Storage"));
        player.sendMessage(MessageUtil.color("&8&l===================="));
        player.sendMessage(MessageUtil.color("&7  Refined Ore: &b" + storage.getRefinedBalance(uuid)));
        player.sendMessage(MessageUtil.color("&7  Compressed: &b" + storage.getCompressedBalance(uuid)));
        player.sendMessage(MessageUtil.color("&7  Lifetime Refined: &f" + storage.getRefinedTotal(uuid)));
        player.sendMessage(MessageUtil.color("&7  Lifetime Compressed: &f" + storage.getCompressedTotal(uuid)));
        player.sendMessage(MessageUtil.color("&7  Auto Storage: " + (storage.isAutoStoring(uuid) ? "&aEnabled" : "&cDisabled")));
        player.sendMessage(MessageUtil.color("&8&l===================="));
        return true;
    }

    private boolean handleStorage(Player player) {
        if (!plugin.getDupeProtection().begin(player, "refined-storage-toggle")) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return true;
        }
        try {
        RefinedOreStorage storage = plugin.getRefinedStorage();
        UUID uuid = player.getUniqueId();
        boolean enabled = !storage.isAutoStoring(uuid);
        storage.setAutoStoring(uuid, enabled);
        storage.flush();
        player.sendMessage(MessageUtil.color(enabled ? "&aRefined auto-storage enabled." : "&cRefined auto-storage disabled."));
        return true;
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }
    }

    private boolean handleStore(Player player) {
        if (!plugin.getDupeProtection().begin(player, "refined-store")) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return true;
        }
        try {
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
            return true;
        }

        player.getInventory().setStorageContents(contents);
        RefinedOreStorage storage = plugin.getRefinedStorage();
        UUID uuid = player.getUniqueId();
        storage.addRefined(uuid, refined);
        storage.addCompressed(uuid, compressed);
        storage.flush();
        player.sendMessage(MessageUtil.color("&aStored &b" + refined + " Refined Ore &aand &b" + compressed + " Compressed&a."));
        return true;
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }
    }

    private boolean handleWithdraw(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(MessageUtil.color("&cUsage: /refined withdraw <refined|compressed> <amount>"));
            return true;
        }

        int amount = parsePositiveInt(player, args[2]);
        if (amount < 0) return true;

        if (!plugin.getDupeProtection().begin(player, "refined-withdraw")) {
            player.sendMessage(MessageUtil.color("&cPlease wait before performing another action."));
            return true;
        }
        try {
        String type = args[1].toLowerCase();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        UUID uuid = player.getUniqueId();
        if (type.equals("refined")) {
            int balance = storage.getRefinedBalance(uuid);
            if (balance < amount) {
                player.sendMessage(MessageUtil.color("&cInsufficient Refined Ore. You have &e" + balance + "&c."));
                return true;
            }
            storage.setRefinedBalance(uuid, balance - amount);
            giveSplit(player, true, amount);
        } else if (type.equals("compressed")) {
            int balance = storage.getCompressedBalance(uuid);
            if (balance < amount) {
                player.sendMessage(MessageUtil.color("&cInsufficient Compressed Refined Ore. You have &e" + balance + "&c."));
                return true;
            }
            storage.setCompressedBalance(uuid, balance - amount);
            giveSplit(player, false, amount);
        } else {
            player.sendMessage(MessageUtil.color("&cUsage: /refined withdraw <refined|compressed> <amount>"));
            return true;
        }

        storage.flush();
        player.sendMessage(MessageUtil.color("&aWithdrawn &e" + amount + " &a" + type + " to your inventory."));
        return true;
        } finally {
            plugin.getDupeProtection().end(player.getUniqueId());
        }
    }

    private void giveSplit(Player player, boolean refined, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(64, remaining);
            ItemStack item = refined
                ? RefinedItemFactory.makeRefined(plugin, stackAmount)
                : RefinedItemFactory.makeCompressed(plugin, stackAmount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                plugin.getDupeProtection().flag(player, "refined-withdraw", "Inventory overflow while withdrawing refined storage");
                leftover.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
            }
            remaining -= stackAmount;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("balance", "storage", "store", "withdraw").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("withdraw")) {
            return Arrays.asList("refined", "compressed").stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private int parsePositiveInt(Player player, String arg) {
        try {
            int amount = Integer.parseInt(arg);
            if (amount <= 0) {
                player.sendMessage(MessageUtil.color("&cAmount must be positive."));
                return -1;
            }
            return amount;
        } catch (NumberFormatException e) {
            player.sendMessage(MessageUtil.color("&cAmount must be a whole number."));
            return -1;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(MessageUtil.color("&6Usage: /refined <balance|storage|store|withdraw>"));
    }
}
