package com.example.killtokens.security;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DupeProtectionService {

    private static final String ALERT_PERMISSION = "killtokens.alerts";

    private final KillTokensPlugin plugin;
    private final Map<UUID, String> activeOperations = new HashMap<>();
    private final Map<UUID, Integer> flagCounts = new HashMap<>();
    private final Map<UUID, String> lastFlags = new HashMap<>();

    public DupeProtectionService(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean begin(Player player, String operation) {
        UUID uuid = player.getUniqueId();
        String active = activeOperations.get(uuid);
        if (active != null) {
            flag(player, operation, "Blocked overlapping operation while " + active + " was active");
            return false;
        }
        activeOperations.put(uuid, operation);
        return true;
    }

    public synchronized void end(UUID uuid) {
        activeOperations.remove(uuid);
    }

    public synchronized void flag(Player player, String operation, String reason) {
        UUID uuid = player.getUniqueId();
        int flags = flagCounts.getOrDefault(uuid, 0) + 1;
        flagCounts.put(uuid, flags);
        String message = player.getName() + " [" + operation + "] " + reason + " (flags: " + flags + ")";
        lastFlags.put(uuid, Instant.now() + " - " + message);

        plugin.getLogger().warning("[DupeFlag] " + message);
        String chat = MessageUtil.color("&c&lDupe Flag &8> &e" + message);
        Bukkit.getOnlinePlayers().stream()
            .filter(staff -> staff.hasPermission(ALERT_PERMISSION))
            .forEach(staff -> staff.sendMessage(chat));
    }

    public synchronized int getFlagCount(UUID uuid) {
        return flagCounts.getOrDefault(uuid, 0);
    }

    public synchronized String getLastFlag(UUID uuid) {
        return lastFlags.getOrDefault(uuid, "none");
    }
}
