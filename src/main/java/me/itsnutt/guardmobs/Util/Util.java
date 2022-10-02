package me.itsnutt.guardmobs.Util;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Mobs.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class Util {

    private static final NamespacedKey regionKey = new NamespacedKey(GuardMobs.getInstance(), "guardMobsRegion");
    private static final NamespacedKey profileKey = new NamespacedKey(GuardMobs.getInstance(), "guardMobProfile");

    private static final List<String> guardMobTypesString = new ArrayList<>(){{
        add(GuardMob.CustomEntityType.ARCHER.name());
        add(GuardMob.CustomEntityType.SWORDSMAN.name());
        add(GuardMob.CustomEntityType.BEAR.name());
        add(GuardMob.CustomEntityType.TITAN.name());
        add(GuardMob.CustomEntityType.MAGE.name());
        add(GuardMob.CustomEntityType.CONJURER.name());
    }};

    private static final List<GuardMob.CustomEntityType> guardMobTypesEnum = new ArrayList<>(){{
        add(GuardMob.CustomEntityType.ARCHER);
        add(GuardMob.CustomEntityType.SWORDSMAN);
        add(GuardMob.CustomEntityType.BEAR);
        add(GuardMob.CustomEntityType.TITAN);
        add(GuardMob.CustomEntityType.MAGE);
        add(GuardMob.CustomEntityType.CONJURER);
    }};

    public static List<GuardMob.CustomEntityType> getAllGuardMobTypesEnum(){
        return guardMobTypesEnum;
    }

    public static List<String> getAllGuardMobTypesString(){
        return guardMobTypesString;
    }

    public static RegionManager getRegionManager(Level level){
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(WorldGuard.getInstance().getPlatform().getMatcher().getWorldByName(level.getWorld().getName()));
    }

    public static ItemStack getSpawnItem(GuardMob.CustomEntityType customEntityType, Integer tier){
        ItemStack item;
        switch (customEntityType){
            case ARCHER -> item = new ItemStack(Material.SKELETON_SPAWN_EGG, 1);
            case SWORDSMAN -> item = new ItemStack(Material.STRAY_SPAWN_EGG, 1);
            case BEAR -> item = new ItemStack(Material.POLAR_BEAR_SPAWN_EGG, 1);
            case TITAN -> item = new ItemStack(Material.HUSK_SPAWN_EGG, 1);
            case MAGE -> item = new ItemStack(Material.WITCH_SPAWN_EGG, 1);
            case CONJURER -> item = new ItemStack(Material.EVOKER_SPAWN_EGG, 1);
            default -> throw new NullPointerException("Invalid Guard Mob Type!");
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + customEntityType.name() + " " + tier);
        item.setItemMeta(meta);
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
        CompoundTag compoundTag = (nmsItem.hasTag() ? nmsItem.getTag() : new CompoundTag());
        compoundTag.putString("guardMob", customEntityType.name() + ":" + tier);
        nmsItem.setTag(compoundTag);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    //For Use ONLY WHEN CHECKING NBT OF GUARD MOB SPAWNER ITEMS
    public static boolean isGuardMobString(String string){
        if (string == null)return false;
        String[] raw = string.split(":");

        return guardMobTypesString.contains(raw[0]);
    }

    public static void spawnGuardMob(GuardMob.CustomEntityType customEntityType, Location spawnLocation, String regionID, Integer tier){

        CraftEntity craftEntity;
        CraftWorld world = (CraftWorld) spawnLocation.getWorld();

        switch (customEntityType){
            case ARCHER -> {
                Archer archer = new Archer(spawnLocation, regionID, tier);
                craftEntity = archer.getBukkitEntity();
            }
            case SWORDSMAN -> {
                Swordsman swordsman = new Swordsman(spawnLocation, regionID, tier);
                craftEntity = swordsman.getBukkitEntity();
            }
            case BEAR -> {
                Bear bear = new Bear(spawnLocation, regionID, tier);
                craftEntity = bear.getBukkitEntity();
            }
            case TITAN -> {
                Titan titan = new Titan(spawnLocation, regionID, tier);
                craftEntity = titan.getBukkitEntity();
            }
            case MAGE -> {
                Mage mage = new Mage(spawnLocation, regionID, tier);
                craftEntity = mage.getBukkitEntity();
            }
            case CONJURER -> {
                Conjurer conjurer = new Conjurer(spawnLocation, regionID, tier);
                craftEntity = conjurer.getBukkitEntity();
            }
            default -> throw new NullPointerException("Invalid Guard Mob Type!");
        }
        world.addEntityToWorld(craftEntity.getHandle(), CreatureSpawnEvent.SpawnReason.CUSTOM);
        craftEntity.getPersistentDataContainer().set(Util.getRegionKey(), PersistentDataType.STRING, regionID);
        craftEntity.getPersistentDataContainer().set(Util.getProfileKey(), PersistentDataType.STRING, ((GuardMob) craftEntity.getHandle()).getProfile().serialize());

    }

    public static boolean hasSameRegionID(Entity entity, String regionID){
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (container.has(getRegionKey(), PersistentDataType.STRING)){
            return container.get(getRegionKey(), PersistentDataType.STRING).equalsIgnoreCase(regionID);
        }
        return false;
    }

    public static GuardMob.CustomEntityType parseCustomEntityType(String string){

        for (GuardMob.CustomEntityType customEntityType : guardMobTypesEnum){
            if (customEntityType.name().equalsIgnoreCase(string)){
                return customEntityType;
            }
        }
        return null;
    }

    public static boolean isRegionMember(Player player, String regionID){
        RegionManager regionManager = getRegionManager(((CraftWorld) player.getWorld()).getHandle());
        ProtectedRegion region = regionManager.getRegion(regionID);
        return region.isMember(WorldGuardPlugin.inst().wrapPlayer(player));
    }

    public static boolean isUninitiatedGuardMob(Entity entity){
        CraftEntity craftEntity = (CraftEntity) entity;
        PersistentDataContainer container = craftEntity.getPersistentDataContainer();
        GuardMobProfile guardMobProfile = null;

        if (container.has(Util.getProfileKey(), PersistentDataType.STRING)){
            guardMobProfile = GuardMobProfile.deserialize(container.get(Util.getProfileKey(), PersistentDataType.STRING));
        }
        return guardMobProfile != null;
    }

    public static Inventory prepareInventory(GuardMob guardMob){
        Inventory inventory = guardMob.getInventory();
        ItemStack spawnEgg = Util.getSpawnItem(guardMob.getEntityType(), guardMob.getTier());
        ItemMeta eggMeta = spawnEgg.getItemMeta();
        eggMeta.setDisplayName(ChatColor.GOLD + "Revert To Egg");
        spawnEgg.setItemMeta(eggMeta);

        inventory.setItem(3, spawnEgg);

        ItemStack gold = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldMeta = gold.getItemMeta();
        goldMeta.setDisplayName(ChatColor.GOLD + "Upgrade");

        List<String> lore = new ArrayList<>();
        String price = GuardMobs.useConfig() ? String.valueOf(GuardMobs.getPriceConfig().getUpgradePrice(guardMob.getEntityType(), guardMob.getTier()+1)) : "1000";
        lore.add("$" + price);
        goldMeta.setLore(lore);
        gold.setItemMeta(goldMeta);

        net.minecraft.world.item.ItemStack nmsGold = CraftItemStack.asNMSCopy(gold);
        CompoundTag compoundTag = (nmsGold.hasTag() ? nmsGold.getTag() : new CompoundTag());
        compoundTag.putString("upgradePrice", price);
        nmsGold.setTag(compoundTag);

        gold = CraftItemStack.asBukkitCopy(nmsGold);

        if (guardMob.getTier() < 5){
            inventory.setItem(5, gold);
        }

        return inventory;
    }

    public static Double getUpgradePrice(ItemStack itemStack){
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        if (!nmsItem.hasTag())return 0d;

        return Double.valueOf(nmsItem.getTag().getString("upgradePrice"));
    }

    public static boolean isSpawnerItem(ItemStack itemStack){
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        if (!nmsItem.hasTag())return false;
        return isGuardMobString(nmsItem.getTag().getString("guardMob"));
    }

    public static boolean isAlly(Entity entity, String regionID){
        if (entity instanceof GuardMob guardMob){
            if (guardMob.getRegionID().equalsIgnoreCase(regionID)){
                return true;
            }
        }
        if (entity instanceof Player player){
            return isRegionMember(player, regionID);
        }
        return false;
    }

    public static NamespacedKey getRegionKey(){
        return regionKey;
    }

    public static NamespacedKey getProfileKey(){return profileKey;}

    public static void spawnHearts(GuardMob guardMob){
        World world = guardMob.getLocation().getWorld();
        world.spawnParticle(Particle.HEART, guardMob.getLocation().add(0,2.4,0), guardMob.getTier());
    }
}
