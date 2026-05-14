package com.example.killtokens.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YamlTokenStorage implements TokenStorage {

    private final File file;
    private final YamlConfiguration config;

    public YamlTokenStorage(File dataFolder) {
        this.file = new File(dataFolder, "tokens.yml");
        dataFolder.mkdirs();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Cannot create tokens.yml: " + e.getMessage(), e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public int getTokens(UUID uuid) {
        return config.getInt("killtokens." + uuid, 0);
    }

    @Override
    public void setTokens(UUID uuid, int amount) {
        config.set("killtokens." + uuid, Math.max(0, amount));
    }

    @Override
    public void addTokens(UUID uuid, int amount) {
        setTokens(uuid, getTokens(uuid) + amount);
    }

    @Override
    public void flush() {
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save tokens.yml: " + e.getMessage(), e);
        }
    }
}
