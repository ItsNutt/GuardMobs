package me.itsnutt.guardmobs.Data;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;

public class PriceConfiguration {
    private final FileConfiguration fileConfiguration;

    public PriceConfiguration(FileConfiguration fileConfiguration){
        initFileConfig(fileConfiguration);
        this.fileConfiguration = fileConfiguration;
    }

    private final HashMap<String, HashMap<Integer, Double>> upgradePrices = new HashMap<>();

    private void initPrices(HashMap<String, HashMap<Integer, Double>> hashMap){

        for (GuardMob.CustomEntityType type : Util.getAllGuardMobTypesEnum()){

            HashMap<Integer, Double> prices = new HashMap<>();

            for (int count = 2; count < 6; count++){
                prices.put( count, fileConfiguration.getDouble("upgradePrices." + type.name() + "." + count));
            }
            hashMap.put(type.name(), prices);
        }

    }

    public void initPrices(){
        initPrices(upgradePrices);
    }

    public HashMap<String, HashMap<Integer, Double>> getUpgradePrices(){
        return upgradePrices;
    }

    public void initFileConfig(FileConfiguration config){
        config.addDefault("useConfig", false);
        for (GuardMob.CustomEntityType type : Util.getAllGuardMobTypesEnum()){

            for (int i = 2; i < 6; i++){
                config.addDefault("upgradePrices." + type.name() + "." + i, 0);
            }
        }
        config.options().copyDefaults(true);
    }

    public Double getUpgradePrice(GuardMob.CustomEntityType type, Integer tier){
        return getUpgradePrices().get(type.name()).get(tier);
    }
}
