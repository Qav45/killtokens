package com.example.killtokens.refined;

import com.example.killtokens.KillTokensPlugin;
import com.example.killtokens.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RefinedOreListener implements Listener {

    private static final Set<Material> ORE_TYPES = new HashSet<>(Arrays.asList(
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
        Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
        Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE
    ));

    private final KillTokensPlugin plugin;

    public RefinedOreListener(KillTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        if (!ORE_TYPES.contains(event.getBlock().getType())) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (plugin.getConfig().getBoolean("refined.skip-silk-touch", true)
            && tool != null
            && tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        RefinedOreStorage storage = plugin.getRefinedStorage();
        int refinedPity = storage.getRefinedPity(uuid) + 1;
        int compressedPity = storage.getCompressedPity(uuid) + 1;
        storage.setRefinedPity(uuid, refinedPity);
        storage.setCompressedPity(uuid, compressedPity);

        int refinedPityLimit = plugin.getConfig().getInt("refined.ore-pity", 100);
        int compressedPityLimit = plugin.getConfig().getInt("refined.compressed-pity", 5000);

        boolean pityTriggered = false;
        if (compressedPity >= compressedPityLimit) {
            storage.setCompressedPity(uuid, 0);
            giveCompressed(player, uuid, 1, " &7(pity)");
            pityTriggered = true;
        } else if (refinedPity >= refinedPityLimit) {
            storage.setRefinedPity(uuid, 0);
            giveRefined(player, uuid, 1, " &7(pity)");
            pityTriggered = true;
        }

        if (pityTriggered) return;

        double multiplier = bonusMultiplier(player);
        // Fortune is intentionally ignored; the Skript behavior rolls once per eligible ore block.
        int compressedChance = adjustedDenominator(plugin.getConfig().getInt("refined.compressed-chance", 3500), multiplier);
        int refinedChance = adjustedDenominator(plugin.getConfig().getInt("refined.ore-chance", 85), multiplier);

        if (ThreadLocalRandom.current().nextInt(1, compressedChance + 1) == 1) {
            storage.setCompressedPity(uuid, 0);
            giveCompressed(player, uuid, 1, "");
        } else if (ThreadLocalRandom.current().nextInt(1, refinedChance + 1) == 1) {
            storage.setRefinedPity(uuid, 0);
            giveRefined(player, uuid, 1, "");
        }
    }

    private void giveRefined(Player player, UUID uuid, int qty, String suffix) {
        RefinedOreStorage storage = plugin.getRefinedStorage();
        storage.addRefinedTotal(uuid, qty);
        if (storage.isAutoStoring(uuid)) {
            storage.addRefined(uuid, qty);
            player.sendMessage(MessageUtil.color("&b+" + qty + " Refined Ore &7stored" + suffix + "&7."));
            return;
        }
        giveItems(player, RefinedItemFactory.makeRefined(plugin, qty));
        player.sendMessage(MessageUtil.color("&b+" + qty + " Refined Ore" + suffix + "&7."));
    }

    private void giveCompressed(Player player, UUID uuid, int qty, String suffix) {
        RefinedOreStorage storage = plugin.getRefinedStorage();
        storage.addCompressedTotal(uuid, qty);
        if (storage.isAutoStoring(uuid)) {
            storage.addCompressed(uuid, qty);
            player.sendMessage(MessageUtil.color("&3+" + qty + " Compressed Refined Ore &7stored" + suffix + "&7."));
        } else {
            giveItems(player, RefinedItemFactory.makeCompressed(plugin, qty));
            player.sendMessage(MessageUtil.color("&3+" + qty + " Compressed Refined Ore" + suffix + "&7."));
        }

        if (plugin.getConfig().getBoolean("refined.broadcast-drops", true)) {
            Bukkit.broadcastMessage(MessageUtil.color("&3&l" + player.getName() + " found Compressed Refined Ore!"));
        }
    }

    private void giveItems(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(extra -> player.getWorld().dropItemNaturally(player.getLocation(), extra));
            player.sendMessage(MessageUtil.color("&eInventory full! Excess refined drops fell at your location."));
        }
    }

    private int adjustedDenominator(int base, double multiplier) {
        return Math.max(1, (int) Math.floor(Math.max(1, base) / multiplier));
    }

    private double bonusMultiplier(Player player) {
        double bonus = 0.0;
        if (wearingFullRefinedDiamond(player.getInventory())) bonus += 0.50;
        if (isNamed(player.getInventory().getItemInMainHand(), "Refined Pickaxe")) bonus += 0.50;
        bonus += luckBonus(player.getInventory().getItemInMainHand());
        return 1.0 + bonus;
    }

    private boolean wearingFullRefinedDiamond(PlayerInventory inventory) {
        return isNamed(inventory.getHelmet(), "Refined")
            && isNamed(inventory.getChestplate(), "Refined")
            && isNamed(inventory.getLeggings(), "Refined")
            && isNamed(inventory.getBoots(), "Refined")
            && inventory.getHelmet().getType() == Material.DIAMOND_HELMET
            && inventory.getChestplate().getType() == Material.DIAMOND_CHESTPLATE
            && inventory.getLeggings().getType() == Material.DIAMOND_LEGGINGS
            && inventory.getBoots().getType() == Material.DIAMOND_BOOTS;
    }

    private boolean isNamed(ItemStack item, String namePart) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && ChatColor.stripColor(meta.getDisplayName()).contains(namePart);
    }

    private double luckBonus(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0.0;

        for (String rawLine : meta.getLore()) {
            String line = ChatColor.stripColor(rawLine);
            if (!line.contains("Luck")) continue;
            int level = parseLuckLevel(line);
            if (level >= 5) return 0.30;
            if (level > 0) return level * 0.05;
        }
        return 0.0;
    }

    private int parseLuckLevel(String line) {
        String digits = line.replaceAll("\\D+", "");
        if (!digits.isEmpty()) return Integer.parseInt(digits);
        if (line.contains(" V")) return 5;
        if (line.contains(" IV")) return 4;
        if (line.contains(" III")) return 3;
        if (line.contains(" II")) return 2;
        if (line.contains(" I")) return 1;
        return 0;
    }
}
