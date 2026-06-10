package com.example.killtokens;

import com.example.killtokens.api.KillTokensApi;
import com.example.killtokens.commands.RefinedCommand;
import com.example.killtokens.commands.RefinedUCommand;
import com.example.killtokens.commands.TokensCommand;
import com.example.killtokens.gui.AmountGui;
import com.example.killtokens.gui.GuiListener;
import com.example.killtokens.gui.ShopGui;
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
import org.bukkit.Material;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class KillTokensPlugin extends JavaPlugin {

    private TokenStorage storage;
    private RefinedOreStorage refinedStorage;
    private DupeProtectionService dupeProtection;
    private Economy economy;
    private TokensGui tokensGui;
    private AmountGui amountGui;
    private ShopGui shopGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        validateConfiguredMaterials();
        storage = new YamlTokenStorage(getDataFolder(), getLogger());
        refinedStorage = new YamlRefinedOreStorage(getDataFolder(), getLogger());
        dupeProtection = new DupeProtectionService(this);

        if (!hookVault()) {
            getLogger().severe("Vault not found or no economy provider registered. Disabling KillTokens.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        tokensGui = new TokensGui(this);
        amountGui = new AmountGui(this);
        shopGui = new ShopGui(this);

        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new RefinedOreListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, tokensGui, amountGui, shopGui), this);

        TokensCommand tokensCommand = new TokensCommand(this);
        getCommand("tokens").setExecutor(tokensCommand);
        getCommand("tokens").setTabCompleter(tokensCommand);

        RefinedCommand refinedCommand = new RefinedCommand(this);
        getCommand("refined").setExecutor(refinedCommand);
        getCommand("refined").setTabCompleter(refinedCommand);

        getCommand("refinedu").setExecutor(new RefinedUCommand(this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RefinedPAPIExpansion(this).register();
            getLogger().info("PlaceholderAPI found - placeholders registered.");
        }

        KillTokensApi.register(this);

        // Flush both storages every 5 minutes to prevent data loss on crash.
        // Runs on the main thread: YamlConfiguration is not thread-safe, and an
        // async save racing main-thread writes can corrupt the file or throw
        // ConcurrentModificationException. The files are tiny, so a sync save is fine.
        getServer().getScheduler().runTaskTimer(this,
            () -> {
                try {
                    storage.flush();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error flushing storage", e);
                }
                try {
                    refinedStorage.flush();
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error flushing refinedStorage", e);
                }
            },
            6000L, 6000L);
    }

    @Override
    public void onDisable() {
        KillTokensApi.unregister();
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

    public AmountGui getAmountGui() {
        return amountGui;
    }

    public ShopGui getShopGui() {
        return shopGui;
    }

    public DupeProtectionService getDupeProtection() {
        return dupeProtection;
    }

    private void validateConfiguredMaterials() {
        for (String path : new String[]{"refined.refined-item", "refined.compressed-item"}) {
            String configured = getConfig().getString(path);
            if (configured != null && Material.matchMaterial(configured) == null) {
                getLogger().warning("Config '" + path + "' = '" + configured
                    + "' is not a valid material on this server version; using the built-in fallback.");
            }
        }
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
