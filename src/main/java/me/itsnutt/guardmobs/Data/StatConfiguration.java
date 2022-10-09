package me.itsnutt.guardmobs.Data;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class StatConfiguration {
    private final FileConfiguration fileConfiguration;

    public StatConfiguration(FileConfiguration fileConfiguration){
        setFileConfig(fileConfiguration);
        this.fileConfiguration = fileConfiguration;
    }

    private HashMap<String, HashMap<String, Double>> statConfig = new HashMap<>();

    private void initStatConfig(HashMap<String, HashMap<String, Double>> hashMap){
        if (!fileConfiguration.getBoolean("useConfig", false)) {
            statConfig = vanillaStats;
            return;
        }

        for (GuardMob.CustomEntityType type : Util.getAllGuardMobTypesEnum()){

            HashMap<String, Double> stats = new HashMap<>();

            stats.put("baseHealth", fileConfiguration.getDouble(type.name() + ".baseHealth", 20));
            stats.put("addendHealthPerTier", fileConfiguration.getDouble(type.name() + ".addendHealthPerTier", 4));
            stats.put("baseDamage", fileConfiguration.getDouble(type.name() + ".baseDamage", 5));
            stats.put("addendDamagePerTier", fileConfiguration.getDouble(type.name() + ".addendDamagePerTier", 2));
            for (int count = 2; count < 6; count++){
                stats.put( String.valueOf(count), fileConfiguration.getDouble( type.name() + ".upgradePrices." + count, 1000*count));
            }
            hashMap.put(type.name(), stats);
        }

    }

    public void initStatConfig(){
        initStatConfig(statConfig);
    }

    public HashMap<String, HashMap<String, Double>> getStatConfig(){
        return statConfig;
    }

    public void setFileConfig(FileConfiguration config){
        config.addDefault("useConfig", false);

        for (Map.Entry<String, HashMap<String, Double>> entry : vanillaStats.entrySet()){
            for (Map.Entry<String, Double> stats : entry.getValue().entrySet()){
                config.addDefault(entry.getKey() + "." + stats.getKey(), stats.getValue());
            }
        }
        config.options().copyDefaults(true);

    }

    public Double getUpgradePrice(GuardMob.CustomEntityType type, Integer tier){
        return getStatConfig().get(type.name()).get(String.valueOf(tier));
    }

    public Double getConfigHealth(GuardMob.CustomEntityType type, Integer tier){
        return getStatConfig().get(type.name()).get("baseHealth") + (getStatConfig().get(type.name()).get("addendHealthPerTier")*tier);
    }

    public Double getConfigDamage(GuardMob.CustomEntityType type, Integer tier){
        return getStatConfig().get(type.name()).get("baseDamage") + (getStatConfig().get(type.name()).get("addendDamagePerTier")*tier);
    }

    private final HashMap<String, HashMap<String, Double>> vanillaStats = new HashMap<>(){{
        HashMap<String, Double> archer = new HashMap<>();
        archer.put("baseHealth", 24.0);
        archer.put("addendHealthPerTier", 4.0);
        archer.put("baseDamage", 2.0);
        archer.put("addendDamagePerTier", 1.0);
        archer.put("2", 2000.0);
        archer.put("3", 5000.0);
        archer.put("4", 7500.0);
        archer.put("5", 10000.0);
        put(GuardMob.CustomEntityType.ARCHER.name(), archer);

        HashMap<String, Double> bear = new HashMap<>();
        bear.put("baseHealth", 30.0);
        bear.put("addendHealthPerTier", 5.0);
        bear.put("baseDamage", 4.0);
        bear.put("addendDamagePerTier", 1.0);
        bear.put("2", 2000.0);
        bear.put("3", 5000.0);
        bear.put("4", 7500.0);
        bear.put("5", 10000.0);
        put(GuardMob.CustomEntityType.BEAR.name(), bear);

        HashMap<String, Double> conjurer = new HashMap<>();
        conjurer.put("baseHealth", 26.0);
        conjurer.put("addendHealthPerTier", 4.0);
        conjurer.put("baseDamage", 6.0);
        conjurer.put("addendDamagePerTier", 1.0);
        conjurer.put("2", 2000.0);
        conjurer.put("3", 5000.0);
        conjurer.put("4", 7500.0);
        conjurer.put("5", 10000.0);
        put(GuardMob.CustomEntityType.CONJURER.name(), conjurer);

        HashMap<String, Double> mage = new HashMap<>();
        mage.put("baseHealth", 26.0);
        mage.put("addendHealthPerTier", 4.0);
        mage.put("baseDamage", 0.0);
        mage.put("addendDamagePerTier", 0.0);
        mage.put("2", 2000.0);
        mage.put("3", 5000.0);
        mage.put("4", 7500.0);
        mage.put("5", 10000.0);
        put(GuardMob.CustomEntityType.MAGE.name(), mage);

        HashMap<String, Double> saint = new HashMap<>();
        saint.put("baseHealth", 28.0);
        saint.put("addendHealthPerTier", 4.0);
        saint.put("baseDamage", 0.0);
        saint.put("addendDamagePerTier", 0.0);
        saint.put("2", 2000.0);
        saint.put("3", 5000.0);
        saint.put("4", 7500.0);
        saint.put("5", 10000.0);
        put(GuardMob.CustomEntityType.SAINT.name(), saint);

        HashMap<String, Double> swordsman = new HashMap<>();
        swordsman.put("baseHealth", 24.0);
        swordsman.put("addendHealthPerTier", 4.0);
        swordsman.put("baseDamage", 2.0);
        swordsman.put("addendDamagePerTier", 1.0);
        swordsman.put("2", 2000.0);
        swordsman.put("3", 5000.0);
        swordsman.put("4", 7500.0);
        swordsman.put("5", 10000.0);
        put(GuardMob.CustomEntityType.SWORDSMAN.name(), swordsman);

        HashMap<String, Double> titan = new HashMap<>();
        titan.put("baseHealth", 100.0);
        titan.put("addendHealthPerTier", 15.0);
        titan.put("baseDamage", 15.0);
        titan.put("addendDamagePerTier", 2.0);
        titan.put("2", 2000.0);
        titan.put("3", 5000.0);
        titan.put("4", 7500.0);
        titan.put("5", 10000.0);
        put(GuardMob.CustomEntityType.TITAN.name(), titan);
    }};
}
