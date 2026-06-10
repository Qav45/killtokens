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
    private final Map<UUID, Long> lastActionAt = new HashMap<>();
    private final Map<UUID, Integer> flagCounts = new HashMap<>();
    private final Map<UUID, String> lastFlags = new HashMap<>();

    public DupeProtectionService(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gates an economy-changing operation. Returns false if another operation is
     * still active (re-entrancy, e.g. an event fired from inside a transaction) or
     * if the per-player cooldown has not elapsed. The cooldown is the part that
     * actually matters in practice: all handlers run on the main thread, so true
     * overlap is rare, but rapid duplicate clicks (common from Bedrock clients via
     * Geyser) arrive as distinct sequential events the overlap check cannot see.
     */
    public synchronized boolean begin(Player player, String operation) {
        UUID uuid = player.getUniqueId();
        String active = activeOperations.get(uuid);
        if (active != null) {
            flag(player, operation, "Blocked overlapping operation while " + active + " was active");
            return false;
        }

        long cooldownMs = plugin.getConfig().getLong("security.action-cooldown-ms", 300L);
        long now = System.currentTimeMillis();
        Long last = lastActionAt.get(uuid);
        if (last != null && now - last < cooldownMs) {
            return false;
        }

        lastActionAt.put(uuid, now);
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
