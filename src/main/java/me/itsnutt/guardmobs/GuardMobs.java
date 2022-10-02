package me.itsnutt.guardmobs;

import me.itsnutt.guardmobs.Commands.GetSpawnerItem;
import me.itsnutt.guardmobs.Data.PriceConfiguration;
import me.itsnutt.guardmobs.Listeners.ChunkLoadListener;
import me.itsnutt.guardmobs.Listeners.InventoryClickListener;
import me.itsnutt.guardmobs.Listeners.ItemListener;
import me.itsnutt.guardmobs.Listeners.PlayerInteractEntityListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class GuardMobs extends JavaPlugin {

    private static GuardMobs instance;

    public static GuardMobs getInstance(){
        return instance;
    }

    private static Economy econ = null;

    private final FileConfiguration config = this.getConfig();

    public static Economy getEconomy(){
        return econ;
    }

    private final Logger log = getLogger();

    private static boolean useConfig;

    public static boolean useConfig(){
        return useConfig;
    }

    private static PriceConfiguration priceConfig;

    public static PriceConfiguration getPriceConfig(){
        return priceConfig;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getCommand("getSpawnerItem").setExecutor(new GetSpawnerItem());
        getServer().getPluginManager().registerEvents(new ItemListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);

        if (!setupEconomy() ) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                log.severe("Economy plugin not found!");
            }else{
                log.severe("Vault not found!");
            }
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        priceConfig = new PriceConfiguration(config);
        saveConfig();

        useConfig = config.getBoolean("useConfig");
        if (useConfig()){
            priceConfig.initPrices();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
