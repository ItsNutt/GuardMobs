package me.itsnutt.guardmobs.Util;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Mobs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;

public class Util {

    private static final NamespacedKey regionKey = new NamespacedKey(GuardMobs.getInstance(), "guardMobsRegion");
    private static final NamespacedKey profileKey = new NamespacedKey(GuardMobs.getInstance(), "guardMobProfile");
    private static final NamespacedKey uuidKey = new NamespacedKey(GuardMobs.getInstance(), "guardMobUUID");

    private static final List<String> guardMobTypesString = new ArrayList<>(){{
        add(GuardMob.CustomEntityType.ARCHER.name());
        add(GuardMob.CustomEntityType.SWORDSMAN.name());
        add(GuardMob.CustomEntityType.BEAR.name());
        add(GuardMob.CustomEntityType.TITAN.name());
        add(GuardMob.CustomEntityType.MAGE.name());
        add(GuardMob.CustomEntityType.CONJURER.name());
        add(GuardMob.CustomEntityType.SAINT.name());
    }};

    private static final List<GuardMob.CustomEntityType> guardMobTypesEnum = new ArrayList<>(){{
        add(GuardMob.CustomEntityType.ARCHER);
        add(GuardMob.CustomEntityType.SWORDSMAN);
        add(GuardMob.CustomEntityType.BEAR);
        add(GuardMob.CustomEntityType.TITAN);
        add(GuardMob.CustomEntityType.MAGE);
        add(GuardMob.CustomEntityType.CONJURER);
        add(GuardMob.CustomEntityType.SAINT);
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
            case SAINT -> item = new ItemStack(Material.BLAZE_SPAWN_EGG, 1);
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

    public static void spawnGuardMob(GuardMob.CustomEntityType customEntityType, Location spawnLocation, String regionID, Integer tier, UUID uuid){

        CraftEntity craftEntity;
        CraftWorld world = (CraftWorld) spawnLocation.getWorld();

        switch (customEntityType){
            case ARCHER -> {
                Archer archer = new Archer(spawnLocation, regionID, tier, uuid);
                craftEntity = archer.getBukkitEntity();
            }
            case SWORDSMAN -> {
                Swordsman swordsman = new Swordsman(spawnLocation, regionID, tier, uuid);
                craftEntity = swordsman.getBukkitEntity();
            }
            case BEAR -> {
                Bear bear = new Bear(spawnLocation, regionID, tier, uuid);
                craftEntity = bear.getBukkitEntity();
            }
            case TITAN -> {
                Titan titan = new Titan(spawnLocation, regionID, tier, uuid);
                craftEntity = titan.getBukkitEntity();
            }
            case MAGE -> {
                Mage mage = new Mage(spawnLocation, regionID, tier, uuid);
                craftEntity = mage.getBukkitEntity();
            }
            case CONJURER -> {
                Conjurer conjurer = new Conjurer(spawnLocation, regionID, tier, uuid);
                craftEntity = conjurer.getBukkitEntity();
            }
            case SAINT -> {
                Saint saint = new Saint(spawnLocation, regionID, tier, uuid);
                craftEntity = saint.getBukkitEntity();
            }
            default -> throw new NullPointerException("Invalid Guard Mob Type!");
        }
        world.addEntityToWorld(craftEntity.getHandle(), CreatureSpawnEvent.SpawnReason.CUSTOM);
        craftEntity.getPersistentDataContainer().set(Util.getRegionKey(), PersistentDataType.STRING, regionID);
        craftEntity.getPersistentDataContainer().set(Util.getProfileKey(), PersistentDataType.STRING, ((GuardMob) craftEntity.getHandle()).getProfile().serialize());
        craftEntity.getPersistentDataContainer().set(Util.getUuidKey(), PersistentDataType.BYTE_ARRAY, ((GuardMob) craftEntity.getHandle()).getProfile().uuidToByteArray());
        if (craftEntity.getHandle() instanceof Armorable armorable){
            armorable.refreshArmor();
        }
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

        if (craftEntity.getHandle() instanceof GuardMob) return false;

        if (container.has(Util.getProfileKey(), PersistentDataType.STRING) && container.has(Util.getUuidKey(), PersistentDataType.BYTE_ARRAY)){
            guardMobProfile = GuardMobProfile.deserialize(container.get(Util.getProfileKey(), PersistentDataType.STRING), container.get(Util.getUuidKey(), PersistentDataType.BYTE_ARRAY));
        }
        return guardMobProfile != null;
    }


    public static void debugMsg(String string){
        System.out.println("Debug Message " + string);

    }

    public static BukkitRunnable initSpawnChunkGuardMobs(){
        return new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()){
                    Chunk spawnChunk = world.getSpawnLocation().getChunk();
                    for (int z = spawnChunk.getZ()-9; z < spawnChunk.getZ()+10; z++) {
                        for (int x = spawnChunk.getX()-9; x < spawnChunk.getX()+10; x++) {
                            Chunk chunk = world.getChunkAt(x,z);
                            for (Entity entity : chunk.getEntities()){
                                if (isUninitiatedGuardMob(entity)){
                                    CraftEntity craftEntity = (CraftEntity) entity;
                                    PersistentDataContainer container = craftEntity.getPersistentDataContainer();
                                    GuardMobProfile profile = GuardMobProfile.deserialize(container.get(getProfileKey(), PersistentDataType.STRING), container.get(getUuidKey(), PersistentDataType.BYTE_ARRAY));
                                    if (profile != null){
                                        profile.spawnGuardMob();
                                    }
                                    entity.remove();
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    public static Inventory prepareInventory(GuardMob guardMob){

        Inventory inventory = Bukkit.createInventory((InventoryHolder) guardMob, 36, ChatColor.BLACK + guardMob.getEntityType().name().toLowerCase(Locale.ROOT) + " Menu");
        if (guardMob.getInventory() != null){
            inventory.setContents(guardMob.getInventory().getContents());
        }
        ItemStack spawnEgg = Util.getSpawnItem(guardMob.getEntityType(), guardMob.getTier());
        ItemMeta eggMeta = spawnEgg.getItemMeta();
        eggMeta.setDisplayName(ChatColor.GOLD + "Revert To Egg");
        spawnEgg.setItemMeta(eggMeta);

        inventory.setItem(8, spawnEgg);

        ItemStack gold = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldMeta = gold.getItemMeta();
        goldMeta.setDisplayName(ChatColor.GOLD + "Upgrade");

        List<String> goldLore = new ArrayList<>();
        String price = String.valueOf(GuardMobs.getStatConfig().getUpgradePrice(guardMob.getEntityType(), guardMob.getTier()+1));
        goldLore.add("$" + price);
        goldMeta.setLore(goldLore);
        gold.setItemMeta(goldMeta);

        net.minecraft.world.item.ItemStack nmsGold = CraftItemStack.asNMSCopy(gold);
        CompoundTag compoundTag = (nmsGold.hasTag() ? nmsGold.getTag() : new CompoundTag());
        compoundTag.putString("upgradePrice", price);
        nmsGold.setTag(compoundTag);

        gold = CraftItemStack.asBukkitCopy(nmsGold);

        ItemStack moveSetting = null;
        switch (guardMob.getMovementSetting()){
            case STAY_AT_SPAWN -> {
                moveSetting = new ItemStack(Material.RED_BED);
                ItemMeta moveSettingMeta = moveSetting.getItemMeta();
                moveSettingMeta.setDisplayName(ChatColor.DARK_RED + "Stay At Spawn");
                moveSetting.setItemMeta(moveSettingMeta);}
            case FOLLOW -> {
                moveSetting = new ItemStack(Material.LEAD);
                ItemMeta moveSettingMeta = moveSetting.getItemMeta();
                moveSettingMeta.setDisplayName(ChatColor.GOLD + "Follow");
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GOLD + "Following " + guardMob.getFollowing().getDisplayName());
                moveSettingMeta.setLore(lore);

                moveSetting.setItemMeta(moveSettingMeta);
            }
        }

        ItemStack helmet = new ItemStack(Material.BARRIER);
        ItemStack chestplate = new ItemStack(Material.BARRIER);
        ItemStack legs = new ItemStack(Material.BARRIER);
        ItemStack boots = new ItemStack(Material.BARRIER);

        if (guardMob instanceof Armorable){
            helmet = inventory.getItem(0)==null ? setItemDisplayName(helmet, ChatColor.DARK_RED + "Helmet Slot") : inventory.getItem(0);
            chestplate = inventory.getItem(9)==null ? setItemDisplayName(chestplate, ChatColor.DARK_RED +"Chest Slot") : inventory.getItem(9);
            legs = inventory.getItem(18)==null ? setItemDisplayName(legs, ChatColor.DARK_RED + "Legs Slot") : inventory.getItem(18);
            boots = inventory.getItem(27)==null ? setItemDisplayName(boots, ChatColor.DARK_RED + "Boots Slot") : inventory.getItem(27);
            if (guardMob.getEntity().getType() == EntityType.SKELETON && !isHelmet(helmet)){
                helmet = new ItemStack(Material.GLASS);
            }
            inventory.setItem(0, helmet);
            inventory.setItem(9, chestplate);
            inventory.setItem(18, legs);
            inventory.setItem(27, boots);
        }

        ItemStack totem = inventory.getItem(1)==null ? setItemDisplayName(new ItemStack(Material.BARRIER), ChatColor.DARK_RED + "Totem of Undying Slot") : inventory.getItem(1);
        inventory.setItem(1, totem);


        if (moveSetting != null){
            inventory.setItem(17, moveSetting);
        }

        if (guardMob.getTier() < 5){
            inventory.setItem(26, gold);
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
        net.minecraft.world.entity.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        if (nmsEntity instanceof GuardMob guardMob){
            if (guardMob.getRegionID().equalsIgnoreCase(regionID)){
                return true;
            }
        }
        if (entity instanceof Player player){
            return isRegionMember(player, regionID);
        }
        return false;
    }

    public static boolean isOwner(Player player, String regionID){
        RegionManager regionManager = getRegionManager(((CraftWorld)player.getWorld()).getHandle());
        if (regionManager.getRegion(regionID) == null){
            return false;
        }
        return regionManager.getRegion(regionID).isOwner(WorldGuardPlugin.inst().wrapPlayer(player));
    }

    public static boolean hasAllSaintBuffs(LivingEntity livingEntity, Integer tier){
        net.minecraft.world.entity.LivingEntity entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        boolean fullHealth = entityLiving.getHealth() >= entityLiving.getMaxHealth();
        boolean regeneration = livingEntity.hasPotionEffect(PotionEffectType.REGENERATION);
        boolean fireResist = livingEntity.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);
        boolean speed = livingEntity.hasPotionEffect(PotionEffectType.SPEED);
        boolean strength = livingEntity.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE);

        switch (tier){
            case 1 -> {return fullHealth;}
            case 2 -> {if (regeneration)return fullHealth;}
            case 3 -> {if (fireResist && regeneration)return fullHealth;}
            case 4 -> {if (speed && fireResist && regeneration)return fullHealth;}
            case 5 -> {if (strength && speed && fireResist && regeneration)return fullHealth;}
        }
        return false;
    }

    public static boolean hasSaintBuff(LivingEntity livingEntity, Integer tier){
        net.minecraft.world.entity.LivingEntity entityLiving = ((CraftLivingEntity) livingEntity).getHandle();
        boolean fullHealth = entityLiving.getHealth() >= entityLiving.getMaxHealth();
        boolean regeneration = livingEntity.hasPotionEffect(PotionEffectType.REGENERATION);
        boolean fireResist = livingEntity.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE);
        boolean speed = livingEntity.hasPotionEffect(PotionEffectType.SPEED);
        boolean strength = livingEntity.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE);

        return switch (tier) {
            case 1 -> fullHealth;
            case 2 -> regeneration;
            case 3 -> fireResist;
            case 4 -> speed;
            case 5 -> strength;
            default -> false;
        };
    }

    public static NamespacedKey getRegionKey(){
        return regionKey;
    }

    public static NamespacedKey getProfileKey(){return profileKey;}

    public static NamespacedKey getUuidKey(){return uuidKey;}

    public static void spawnHearts(GuardMob guardMob){
        World world = guardMob.getLocation().getWorld();
        world.spawnParticle(Particle.HEART, guardMob.getLocation().add(0,2.4,0), guardMob.getTier());
    }

    public static BlockPos getBlockPosFromVector(Vector vector){
        return new BlockPos(vector.getX(), vector.getY(), vector.getZ());
    }

    public static Location locFromVec(Entity entity,Vector vector){
        return new Location(entity.getWorld(), vector.getX(), vector.getY(), vector.getZ());
    }

    public static boolean hasEntityLineOfSight(GuardMob guardMob, Entity target){
        World world = guardMob.getLocation().getWorld();
        Vector vec1 = guardMob.getLocation().toVector();
        Vector vec2 = target.getLocation().toVector();
        Vector vector = new Vector(vec2.getX() - vec1.getX(), vec2.getY()-vec1.getY(), vec2.getZ() - vec1.getZ());
        Predicate<Entity> predicate = entity -> entity != guardMob.getEntity().getBukkitEntity() && entity instanceof LivingEntity;
        RayTraceResult result = world.rayTraceEntities(guardMob.getLocation().add(0, guardMob.getEntity().getEyeHeight(),0), vector.normalize(), 16, 0.5, predicate);
        return !(Util.isAlly(result.getHitEntity(), guardMob.getRegionID()));
    }

    public static boolean isHelmet(ItemStack itemStack){
        Material[] helmets = {Material.DIAMOND_HELMET, Material.GOLDEN_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.TURTLE_HELMET, Material.LEATHER_HELMET,
        Material.NETHERITE_HELMET, Material.JACK_O_LANTERN};
        return Arrays.stream(helmets).toList().contains(itemStack.getType());
    }

    public static boolean isChestPlate(ItemStack itemStack){
        Material[] helmets = {Material.DIAMOND_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
                Material.LEATHER_CHESTPLATE, Material.NETHERITE_CHESTPLATE};
        return Arrays.stream(helmets).toList().contains(itemStack.getType());
    }

    public static boolean isLeggings(ItemStack itemStack){
        Material[] helmets = {Material.DIAMOND_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
                Material.LEATHER_LEGGINGS, Material.NETHERITE_LEGGINGS};
        return Arrays.stream(helmets).toList().contains(itemStack.getType());
    }

    public static boolean isBoots(ItemStack itemStack){
        Material[] helmets = {Material.DIAMOND_BOOTS, Material.GOLDEN_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                Material.LEATHER_BOOTS, Material.NETHERITE_BOOTS};
        return Arrays.stream(helmets).toList().contains(itemStack.getType());
    }

    public static boolean isArmor(ItemStack itemStack){
        return isHelmet(itemStack) || isChestPlate(itemStack) || isLeggings(itemStack) || isBoots(itemStack);
    }

    public static ItemStack setItemDisplayName(ItemStack itemStack, String name) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(name);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static boolean playerInventoryIsFull(Player player){
        PlayerInventory inventory = player.getInventory();
        int maxSize = 36;
        maxSize+=inventory.getArmorContents().length;
        return inventory.getContents().length == maxSize;
    }

    public static ItemStack getRevivalToken(GuardMob guardMob){
        ItemStack token = new ItemStack(Material.MUSIC_DISC_5);
        ItemMeta tokenMeta = token.getItemMeta();
        tokenMeta.setDisplayName(ChatColor.GOLD + "Revival Token");
        List<String> lore = new ArrayList<>();
        lore.add("Type: " + ChatColor.GREEN + guardMob.getEntityType().name());
        lore.add("Tier: " + ChatColor.GREEN + guardMob.getTier());
        lore.add("Respawn Price: " + ChatColor.GREEN + "$" + GuardMobs.getStatConfig().getUpgradePrice(guardMob.getEntityType(), guardMob.getTier()));
        tokenMeta.setLore(lore);
        token.setItemMeta(tokenMeta);

        net.minecraft.world.item.ItemStack nmsToken = CraftItemStack.asNMSCopy(token);
        CompoundTag tag = nmsToken.hasTag() ? nmsToken.getTag() : new CompoundTag();
        tag.putString("guardMobProfile", guardMob.getProfile().serialize());
        tag.putByteArray("guardMobUUID", guardMob.getProfile().uuidToByteArray());
        nmsToken.setTag(tag);


        return CraftItemStack.asBukkitCopy(nmsToken);
    }

    public static void prepareNullArmor(Inventory inventory){
        if (inventory.getItem(0)==null){
            inventory.setItem(0, setItemDisplayName(new ItemStack(Material.BARRIER), ChatColor.DARK_RED + "Helmet Slot"));
            if (inventory.getHolder() instanceof Armorable armorable){
                if (armorable instanceof Skeleton){
                    inventory.setItem(0, new ItemStack(Material.GLASS));
                }
            }
        }
        if (inventory.getItem(9)==null){
            inventory.setItem(9, setItemDisplayName(new ItemStack(Material.BARRIER), ChatColor.DARK_RED + "Chest Slot"));
        }
        if (inventory.getItem(18)==null){
            inventory.setItem(18, setItemDisplayName(new ItemStack(Material.BARRIER), ChatColor.DARK_RED + "Legs Slot"));
        }
        if (inventory.getItem(27)==null){
            inventory.setItem(27, setItemDisplayName(new ItemStack(Material.BARRIER), ChatColor.DARK_RED + "Boots Slot"));
        }
    }

    public static boolean hasTotem(GuardMob guardMob){
        return guardMob.getInventory().getItem(1).getType()==Material.TOTEM_OF_UNDYING;
    }

}
