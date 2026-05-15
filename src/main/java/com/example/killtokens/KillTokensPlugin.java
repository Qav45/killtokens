package com.example.killtokens;

import com.example.killtokens.commands.TokensCommand;
import com.example.killtokens.gui.GuiListener;
import com.example.killtokens.gui.TokensGui;
import com.example.killtokens.listeners.PlayerDeathListener;
import com.example.killtokens.placeholders.KillTokensExpansion;
import com.example.killtokens.storage.TokenStorage;
import com.example.killtokens.storage.YamlTokenStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KillTokensPlugin extends JavaPlugin {

    private TokenStorage storage;
    private Economy economy;
    private TokensGui tokensGui;

    // Per-player lock to prevent concurrent GUI/command double-spend
    private final Set<UUID> processing = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        storage = new YamlTokenStorage(getDataFolder(), getLogger());

        if (!hookVault()) {
            getLogger().severe("Vault not found or no economy provider registered. Disabling KillTokens.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        tokensGui = new TokensGui(this);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, tokensGui), this);

        TokensCommand executor = new TokensCommand(this);
        getCommand("tokens").setExecutor(executor);
        getCommand("tokens").setTabCompleter(executor);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new KillTokensExpansion(this).register();
            getLogger().info("PlaceholderAPI found — placeholders registered.");
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.flush();
        }
    }

    /** Returns true and acquires the lock if the player is not already processing. */
    public synchronized boolean tryLock(UUID uuid) {
        return processing.add(uuid);
    }

    public synchronized void unlock(UUID uuid) {
        processing.remove(uuid);
    }

    public TokenStorage getStorage() {
        return storage;
    }

    public Economy getEconomy() {
        return economy;
    }

    public TokensGui getTokensGui() {
        return tokensGui;
    }

    private boolean hookVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
            .getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }
}
