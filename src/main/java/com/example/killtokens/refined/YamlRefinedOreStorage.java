package com.example.killtokens.refined;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class YamlRefinedOreStorage implements RefinedOreStorage {

    private final File file;
    private final Logger logger;
    private final Map<UUID, RefinedPlayerData> players = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    public YamlRefinedOreStorage(File dataFolder, Logger logger) {
        this.logger = logger;
        this.file = new File(dataFolder, "refined_data.yml");
        dataFolder.mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Cannot create refined_data.yml: " + e.getMessage(), e);
            }
        }
        load();
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "players." + key + ".";
                players.put(uuid, new RefinedPlayerData(
                    config.getInt(path + "refined-balance", 0),
                    config.getInt(path + "compressed-balance", 0),
                    config.getInt(path + "refined-total", 0),
                    config.getInt(path + "compressed-total", 0),
                    config.getInt(path + "refined-pity", 0),
                    config.getInt(path + "compressed-pity", 0),
                    config.getBoolean(path + "auto-storing", false)
                ));
            } catch (IllegalArgumentException e) {
                logger.warning("Skipping invalid refined_data.yml UUID: " + key);
            }
        }
    }

    @Override
    public int getRefinedBalance(UUID uuid) {
        return data(uuid).refinedBalance;
    }

    @Override
    public void setRefinedBalance(UUID uuid, int amount) {
        data(uuid).refinedBalance = Math.max(0, amount);
        dirty.add(uuid);
    }

    @Override
    public void addRefined(UUID uuid, int amount) {
        setRefinedBalance(uuid, getRefinedBalance(uuid) + amount);
    }

    @Override
    public int getCompressedBalance(UUID uuid) {
        return data(uuid).compressedBalance;
    }

    @Override
    public void setCompressedBalance(UUID uuid, int amount) {
        data(uuid).compressedBalance = Math.max(0, amount);
        dirty.add(uuid);
    }

    @Override
    public void addCompressed(UUID uuid, int amount) {
        setCompressedBalance(uuid, getCompressedBalance(uuid) + amount);
    }

    @Override
    public int getRefinedTotal(UUID uuid) {
        return data(uuid).refinedTotal;
    }

    @Override
    public void addRefinedTotal(UUID uuid, int amount) {
        RefinedPlayerData data = data(uuid);
        data.refinedTotal = Math.max(0, data.refinedTotal + amount);
        dirty.add(uuid);
    }

    @Override
    public int getCompressedTotal(UUID uuid) {
        return data(uuid).compressedTotal;
    }

    @Override
    public void addCompressedTotal(UUID uuid, int amount) {
        RefinedPlayerData data = data(uuid);
        data.compressedTotal = Math.max(0, data.compressedTotal + amount);
        dirty.add(uuid);
    }

    @Override
    public int getRefinedPity(UUID uuid) {
        return data(uuid).refinedPity;
    }

    @Override
    public void setRefinedPity(UUID uuid, int value) {
        data(uuid).refinedPity = Math.max(0, value);
        dirty.add(uuid);
    }

    @Override
    public int getCompressedPity(UUID uuid) {
        return data(uuid).compressedPity;
    }

    @Override
    public void setCompressedPity(UUID uuid, int value) {
        data(uuid).compressedPity = Math.max(0, value);
        dirty.add(uuid);
    }

    @Override
    public boolean isAutoStoring(UUID uuid) {
        return data(uuid).autoStoring;
    }

    @Override
    public void setAutoStoring(UUID uuid, boolean value) {
        data(uuid).autoStoring = value;
        dirty.add(uuid);
    }

    @Override
    public void flush() {
        if (dirty.isEmpty() && file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (UUID uuid : dirty) {
            RefinedPlayerData data = data(uuid);
            String path = "players." + uuid + ".";
            config.set(path + "refined-balance", data.refinedBalance);
            config.set(path + "compressed-balance", data.compressedBalance);
            config.set(path + "refined-total", data.refinedTotal);
            config.set(path + "compressed-total", data.compressedTotal);
            config.set(path + "refined-pity", data.refinedPity);
            config.set(path + "compressed-pity", data.compressedPity);
            config.set(path + "auto-storing", data.autoStoring);
        }

        try {
            config.save(file);
            dirty.clear();
        } catch (IOException e) {
            logger.severe("Failed to save refined_data.yml: " + e.getMessage());
        }
    }

    private RefinedPlayerData data(UUID uuid) {
        return players.computeIfAbsent(uuid, ignored -> new RefinedPlayerData());
    }

    private static class RefinedPlayerData {
        private int refinedBalance;
        private int compressedBalance;
        private int refinedTotal;
        private int compressedTotal;
        private int refinedPity;
        private int compressedPity;
        private boolean autoStoring;

        private RefinedPlayerData() {
        }

        private RefinedPlayerData(
            int refinedBalance,
            int compressedBalance,
            int refinedTotal,
            int compressedTotal,
            int refinedPity,
            int compressedPity,
            boolean autoStoring
        ) {
            this.refinedBalance = Math.max(0, refinedBalance);
            this.compressedBalance = Math.max(0, compressedBalance);
            this.refinedTotal = Math.max(0, refinedTotal);
            this.compressedTotal = Math.max(0, compressedTotal);
            this.refinedPity = Math.max(0, refinedPity);
            this.compressedPity = Math.max(0, compressedPity);
            this.autoStoring = autoStoring;
        }
    }
}
