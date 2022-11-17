package me.itsnutt.guardmobs.Mobs;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobData;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.Data.StatConfiguration;
import me.itsnutt.guardmobs.Goals.CustomFollowGoal;
import me.itsnutt.guardmobs.Goals.CustomMeleeAttackGoal;
import me.itsnutt.guardmobs.Goals.CustomMoveToSpawnGoal;
import me.itsnutt.guardmobs.Goals.CustomTargetingGoal;
import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.UUID;

public class Swordsman extends Skeleton implements GuardMob, InventoryHolder, Armorable {

    private final boolean targetNonTeamPlayers;
    private final boolean targetHostileMobs;
    private final CustomEntityType customEntityType = CustomEntityType.SWORDSMAN;
    private final String regionID;
    private final Location spawnLocation;
    private final int tier;
    private Inventory inventory;
    private MovementSetting movementSetting = MovementSetting.STAY_AT_SPAWN;
    private org.bukkit.entity.Player following = null;
    private final UUID guardMobID;

    /*
     * The Concept of 'Tiers' is as follows:
     * -The differences between each subsequent tier are not huge, but they are noticeable
     * -The difference in strength between tier 1 and 5 is very noticeable
     * -The difference in strength between tier 1 and 2 is moderately noticeable
     * -The difference in strength between tier 4 and 5 is barely noticeable
     * -As tier goes up, attack damage, health, healing interval, and "intelligence" (targeting efficiency and capability) improve
     * -Diminishing returns is the name of the game, though this is not true for health and damage (as they scale linearly)
     */

    public Swordsman(Location spawnLocation, String regionID, Integer tier, UUID guardMobID){
        super(EntityType.SKELETON, ((CraftWorld) spawnLocation.getWorld()).getHandle());
        int tempTier;

        this.regionID = regionID;
        this.spawnLocation = spawnLocation;
        this.targetHostileMobs = true;
        this.targetNonTeamPlayers = true;
        tempTier = tier;
        if (tier > 5){
            tempTier = 5;
        }
        this.tier = tempTier;

        this.guardMobID = guardMobID==null ? UUID.randomUUID() : guardMobID;

        this.setUUID(UUID.randomUUID());

        this.persist = true;
        this.setPersistenceRequired();

        StatConfiguration stats = GuardMobs.getStatConfig();
        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(stats.getConfigHealth(customEntityType, tier));
        this.setHealth(getMaxHealth());

        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(stats.getConfigDamage(customEntityType, tier));

        this.setItemSlot(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(Material.IRON_HELMET)));

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.DAMAGE_ALL, (int) Math.floor((double)tier/2), true);
        sword.setItemMeta(swordMeta);
        this.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(sword));

        this.setCanPickUpLoot(false);

        Style style = Style.EMPTY;
        style = style.withColor(ChatFormatting.DARK_RED);
        this.setCustomName(Component.literal("Swordsman " + "lvl" + tier).setStyle(style));
        this.setCustomNameVisible(true);

        inventory = GuardMobData.getGuardMobInventory(this);
        inventory = Util.prepareInventory(this);

        this.goalSelector.removeAllGoals();
        this.targetSelector.removeAllGoals();

        this.goalSelector.addGoal( 1, new FloatGoal(this));
        this.goalSelector.addGoal( 2, new CustomMeleeAttackGoal(this, 1 + ((double) tier/10), true));
        this.goalSelector.addGoal( 4, new CustomMoveToSpawnGoal(this, 1,0));
        this.goalSelector.addGoal( 5, new CustomFollowGoal(this, 1.3 + ((double)tier/10)));
        this.goalSelector.addGoal( 6, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        this.goalSelector.addGoal( 7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal( 1, new CustomTargetingGoal(this));
        this.conversionTime = -1;

        this.teleportToWithTicket(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
    }

    @Override
    public ProtectedRegion getRegion(){
        return Util.getRegionManager(level).getRegion(regionID);
    }

    @Override
    public boolean getTargetNonTeamPlayers() {
        return targetNonTeamPlayers;
    }

    @Override
    public boolean getTargetHostileMobs() {
        return targetHostileMobs;
    }

    @Override
    public CustomEntityType getEntityType() {
        return customEntityType;
    }

    @Override
    public String getRegionID() {
        return regionID;
    }

    @Override
    public Location getLocation() {
        return new Location(getLevel().getWorld(), getX(), getY(), getZ());
    }

    @Override
    public BlockPos getSpawnBlockPos() {
        return new BlockPos(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
    }

    @Override
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    @Override
    public void reevaluateTarget(){
        if (this.getTarget() == null){
            return;
        }

        HashSet<Entity> potentialTargets;
        if (this.getLocation().getWorld() != null){
            potentialTargets = new HashSet<>(this.getLocation().getWorld().getNearbyEntities((this).getLocation(), 16 + tier, 8, 16 + tier));
            potentialTargets.removeIf(entity -> !(((CraftEntity) entity).getHandle() instanceof Monster) && !(((CraftEntity) entity).getHandle() instanceof Player));
        }else {return;}
        Entity target = this.getTarget().getBukkitEntity();

        if (this.getLastHurtByMob() != null){
            if (!Util.isAlly(this.getLastHurtByMob().getBukkitEntity(), this.regionID)){
                if (target.getLocation().distance(this.getLocation()) > this.getLastHurtByMob().getBukkitEntity().getLocation().distance(this.getLocation())) {
                    this.setTarget(this.getLastHurtByMob(), EntityTargetEvent.TargetReason.CUSTOM, false);
                    return;
                }
            }
        }
        for (Entity entity : potentialTargets){
            net.minecraft.world.entity.Entity potentialTarget = ((CraftEntity) entity).getHandle();
            if (potentialTarget instanceof Monster monster && targetHostileMobs){
                if (monster.getTarget() != this){
                    continue;
                }
                if (Util.hasSameRegionID(entity, regionID)){
                    continue;
                }
            }else if (potentialTarget instanceof Player player && targetNonTeamPlayers){
                if (Util.isRegionMember((org.bukkit.entity.Player) player.getBukkitEntity(), regionID)){
                    continue;
                }
            }
            if (entity.getLocation().distance(this.getLocation()) < target.getLocation().distance(this.getLocation())){
                this.setTarget((LivingEntity) potentialTarget , EntityTargetEvent.TargetReason.CUSTOM, false);
                return;
            }
        }
    }

    @Override
    public int getTier() {
        return tier;
    }

    @Override
    public GuardMobProfile getProfile() {
        return new GuardMobProfile(customEntityType, spawnLocation, regionID, tier, guardMobID);
    }

    private int reevaluationTickCount = 0;
    private int healTickCount = 0;

    @Override
    public void tick(){
        super.tick();

        //Reevaluation Timer
        if (reevaluationTickCount != (Math.floor((double)40/tier))){
            reevaluationTickCount++;
        } else {
            reevaluationTickCount = 0;
            reevaluateTarget();
        }

        //Heal Timer
        if (this.getHealth() != this.getMaxHealth()){
            if (healTickCount != (Math.floor((double) 25/tier))*20) {
                if (this.getTarget() == null) {
                    healTickCount++;
                } else {
                    healTickCount = 0;
                }
            } else {
                this.heal(1);
                Util.spawnHearts(this);
                healTickCount = 0;
            }
        } else {
            healTickCount = 0;
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public net.minecraft.world.entity.Entity getEntity() {
        return this;
    }

    @Override
    public MovementSetting getMovementSetting() {
        return movementSetting;
    }

    //ALWAYS CALL Util.prepareInventory *AFTER* CALLING THIS METHOD!
    @Override
    public void setMovementSetting(MovementSetting movementSetting) {
        this.movementSetting = movementSetting;
    }

    @Override
    public org.bukkit.entity.Player getFollowing() {
        return following;
    }

    @Override
    public void setFollowing(org.bukkit.entity.Player player) {
        following = player;
        Util.prepareInventory(this);
    }

    @Override
    public UUID getGuardMobID(){
        return guardMobID;
    }

    @Override
    public void refreshInventory() {
        inventory = Util.prepareInventory(this);
        refreshArmor();
        GuardMobData.saveGuardMobInventory(this);
    }

    @Override
    public void refreshArmor() {
        ItemStack helmet;
        if (inventory.getItem(0) != null){
            helmet = Util.isHelmet(inventory.getItem(0)) ? inventory.getItem(0) : new ItemStack(Material.JACK_O_LANTERN);
        } else {
            helmet = new ItemStack(Material.GLASS);
        }
        this.setItemSlot(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(helmet));

        ItemStack chestplate;
        if (inventory.getItem(9) != null){
            chestplate = Util.isChestPlate(inventory.getItem(9)) ? inventory.getItem(9) : null;
        } else {
            chestplate = null;
        }
        this.setItemSlot(EquipmentSlot.CHEST, CraftItemStack.asNMSCopy(chestplate));

        ItemStack legs;
        if (inventory.getItem(18) != null){
            legs = Util.isLeggings(inventory.getItem(18)) ? inventory.getItem(18) : null;
        } else {
            legs = null;
        }
        this.setItemSlot(EquipmentSlot.LEGS, CraftItemStack.asNMSCopy(legs));

        ItemStack boots;
        if (inventory.getItem(27) != null){
            boots = Util.isBoots(inventory.getItem(27)) ? inventory.getItem(27) : null;
        } else {
            boots = null;
        }
        this.setItemSlot(EquipmentSlot.FEET, CraftItemStack.asNMSCopy(boots));
    }

    @Override
    public ItemStack getHead() {
        return CraftItemStack.asBukkitCopy(getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public ItemStack getChest() {
        return CraftItemStack.asBukkitCopy(getItemBySlot(EquipmentSlot.CHEST));
    }

    @Override
    public ItemStack getLegs() {
        return CraftItemStack.asBukkitCopy(getItemBySlot(EquipmentSlot.LEGS));
    }

    @Override
    public ItemStack getFeet() {
        return CraftItemStack.asBukkitCopy(getItemBySlot(EquipmentSlot.FEET));
    }

    @Override
    public boolean addArmorPiece(ItemStack itemStack) {
        EquipmentSlot equipmentSlot;
        int slot;
        if (Util.isHelmet(itemStack)) {
            equipmentSlot = EquipmentSlot.HEAD;
            slot = 0;
        } else if (Util.isChestPlate(itemStack)) {
            equipmentSlot = EquipmentSlot.CHEST;
            slot = 9;
        } else if (Util.isLeggings(itemStack)) {
            equipmentSlot = EquipmentSlot.LEGS;
            slot = 18;
        } else if (Util.isBoots(itemStack)) {
            equipmentSlot = EquipmentSlot.FEET;
            slot = 27;
        } else {
            return false;
        }
        setItemSlot(equipmentSlot, CraftItemStack.asNMSCopy(itemStack));
        inventory.setItem(slot, itemStack);
        return true;
    }

    @Override
    public ItemStack get(EquipmentSlot equipmentSlot) {
        return CraftItemStack.asBukkitCopy(getItemBySlot(equipmentSlot));
    }
}
