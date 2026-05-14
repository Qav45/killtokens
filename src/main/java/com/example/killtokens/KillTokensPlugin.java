package com.example.killtokens;

import com.example.killtokens.commands.TokensCommand;
import com.example.killtokens.listeners.PlayerDeathListener;
import com.example.killtokens.storage.TokenStorage;
import com.example.killtokens.storage.YamlTokenStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class KillTokensPlugin extends JavaPlugin {

    private TokenStorage storage;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        storage = new YamlTokenStorage(getDataFolder());

        if (!hookVault()) {
            getLogger().severe("Vault not found or no economy provider registered. Disabling KillTokens.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        TokensCommand executor = new TokensCommand(this);
        getCommand("tokens").setExecutor(executor);
        getCommand("tokens").setTabCompleter(executor);
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.flush();
        }
    }

    public TokenStorage getStorage() {
        return storage;
    }

    public Economy getEconomy() {
        return economy;
    }

    private boolean hookVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
            .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
}
