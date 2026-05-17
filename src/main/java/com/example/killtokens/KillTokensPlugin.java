package com.example.killtokens;

import com.example.killtokens.commands.RefinedCommand;
import com.example.killtokens.commands.TokensCommand;
import com.example.killtokens.gui.GuiListener;
import com.example.killtokens.gui.TokensGui;
import com.example.killtokens.listeners.PlayerDeathListener;
import com.example.killtokens.placeholders.RefinedPAPIExpansion;
import com.example.killtokens.refined.RefinedOreListener;
import com.example.killtokens.refined.RefinedOreStorage;
import com.example.killtokens.refined.YamlRefinedOreStorage;
import com.example.killtokens.security.DupeProtectionService;
import com.example.killtokens.storage.TokenStorage;
import com.example.killtokens.storage.YamlTokenStorage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class KillTokensPlugin extends JavaPlugin {

    private TokenStorage storage;
    private RefinedOreStorage refinedStorage;
    private DupeProtectionService dupeProtection;
    private Economy economy;
    private TokensGui tokensGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        storage = new YamlTokenStorage(getDataFolder(), getLogger());
        refinedStorage = new YamlRefinedOreStorage(getDataFolder(), getLogger());
        dupeProtection = new DupeProtectionService(this);

        if (!hookVault()) {
            getLogger().severe("Vault not found or no economy provider registered. Disabling KillTokens.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        tokensGui = new TokensGui(this);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new RefinedOreListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, tokensGui), this);

        TokensCommand tokensCommand = new TokensCommand(this);
        getCommand("tokens").setExecutor(tokensCommand);
        getCommand("tokens").setTabCompleter(tokensCommand);

        RefinedCommand refinedCommand = new RefinedCommand(this);
        getCommand("refined").setExecutor(refinedCommand);
        getCommand("refined").setTabCompleter(refinedCommand);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RefinedPAPIExpansion(this).register();
            getLogger().info("PlaceholderAPI found - placeholders registered.");
        }
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.flush();
        }
        if (refinedStorage != null) {
            refinedStorage.flush();
        }
    }

    public TokenStorage getStorage() {
        return storage;
    }

    public RefinedOreStorage getRefinedStorage() {
        return refinedStorage;
    }

    public Economy getEconomy() {
        return economy;
    }

    public TokensGui getTokensGui() {
        return tokensGui;
    }

    public DupeProtectionService getDupeProtection() {
        return dupeProtection;
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
