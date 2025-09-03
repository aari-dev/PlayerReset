package dev.aari.playerreset;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerReset extends JavaPlugin {
    private static PlayerReset instance;
    private DatabaseManager databaseManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        setupEconomy();
        setupDatabase();

        getCommand("reset").setExecutor(new ResetCommand());
        getServer().getPluginManager().registerEvents(new GUIListener(), this);

        getLogger().info("PlayerReset has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("PlayerReset has been disabled!");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Money reset will be disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("Economy provider not found! Money reset will be disabled.");
            return;
        }
        economy = rsp.getProvider();
    }

    private void setupDatabase() {
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
    }

    public static PlayerReset getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEconomy() {
        return economy != null;
    }
}
