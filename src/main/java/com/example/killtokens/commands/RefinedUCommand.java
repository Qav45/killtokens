package com.example.killtokens.commands;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.List;

public class RefinedUCommand implements CommandExecutor {

    private final KillTokensPlugin plugin;

    public RefinedUCommand(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("killtokens.refinedu")) {
            player.sendMessage(MessageUtil.color("&cYou don't have permission to use this command."));
            return true;
        }

        if (!plugin.getConfig().getBoolean("refinedu.enabled", true)) {
            player.sendMessage(MessageUtil.color("&cThe Refined U shop is currently disabled."));
            return true;
        }

        int shopId = plugin.getConfig().getInt("refinedu.shopkeeper-id", 78);
        String openCommand = plugin.getConfig().getString("refinedu.open-command", "shopkeeper open {shopId}");
        if (openCommand == null || openCommand.trim().isEmpty()) {
            player.sendMessage(MessageUtil.color("&cThe Refined U shop command is not configured."));
            plugin.getLogger().warning("refinedu.open-command is empty.");
            return true;
        }

        openCommand = openCommand
            .replace("{shopId}", String.valueOf(shopId))
            .replace("{player}", player.getName())
            .replaceFirst("^/+", "");

        boolean dispatchAsConsole = plugin.getConfig().getBoolean("refinedu.dispatch-as-console", false);
        boolean dispatched;
        if (dispatchAsConsole) {
            dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), openCommand);
        } else {
            PermissionAttachment attachment = grantTemporaryPermissions(player);
            try {
                dispatched = Bukkit.dispatchCommand(player, openCommand);
            } finally {
                if (attachment != null) {
                    player.removeAttachment(attachment);
                }
            }
        }

        if (!dispatched) {
            player.sendMessage(MessageUtil.color("&cUnable to open the Refined U shop. Please contact staff."));
            plugin.getLogger().warning("Failed to dispatch Refined U command: " + openCommand);
        }
        return true;
    }

    private PermissionAttachment grantTemporaryPermissions(Player player) {
        List<String> permissions = plugin.getConfig().getStringList("refinedu.temporary-permissions");
        if (permissions.isEmpty()) {
            return null;
        }

        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String permission : permissions) {
            if (permission != null && !permission.trim().isEmpty()) {
                attachment.setPermission(permission.trim(), true);
            }
        }
        player.recalculatePermissions();
        return attachment;
    }
}
