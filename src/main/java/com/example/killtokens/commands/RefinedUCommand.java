package com.example.killtokens.commands;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        plugin.getShopGui().open(player, null);
        return true;
    }
}
