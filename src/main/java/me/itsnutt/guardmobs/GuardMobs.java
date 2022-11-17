package me.itsnutt.guardmobs;

import me.itsnutt.guardmobs.Command.GetSpawnerItem;
import me.itsnutt.guardmobs.Command.GetSpawnerItemTabCompleter;
import me.itsnutt.guardmobs.Data.StatConfiguration;
import me.itsnutt.guardmobs.Listeners.*;
import me.itsnutt.guardmobs.Util.Util;
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

    private static StatConfiguration statConfig;

    public static StatConfiguration getStatConfig(){
        return statConfig;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getCommand("getSpawnerItem").setExecutor(new GetSpawnerItem());
        getCommand("getSpawnerItem").setTabCompleter(new GetSpawnerItemTabCompleter());
        getServer().getPluginManager().registerEvents(new ItemListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractEntityListener(), this);
        getServer().getPluginManager().registerEvents(new ChunkLoadListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryClickListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDamageListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(), this);

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

        statConfig = new StatConfiguration(config);
        saveConfig();
        statConfig.initStatConfig();

        Util.initSpawnChunkGuardMobs().runTask(this);
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
