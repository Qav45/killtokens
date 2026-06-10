package com.example.killtokens.listeners;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final KillTokensPlugin plugin;

    public PlayerDeathListener(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;

        int reward = plugin.getConfig().getInt("kill-reward", 1);
        plugin.getStorage().addTokens(killer.getUniqueId(), reward);
        int newBalance = plugin.getStorage().getTokens(killer.getUniqueId());

        killer.sendMessage(MessageUtil.color("&a+" + reward + " Kill Token awarded!"));
        killer.sendMessage(MessageUtil.color("&7Balance: &e" + newBalance + " tokens &7| Use &e/tokens balance &7for more info."));
    }
}
