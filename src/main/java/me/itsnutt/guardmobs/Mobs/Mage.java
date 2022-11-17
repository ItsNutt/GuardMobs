package me.itsnutt.guardmobs.Mobs;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobData;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.Data.StatConfiguration;
import me.itsnutt.guardmobs.Goals.CustomFollowGoal;
import me.itsnutt.guardmobs.Goals.CustomMoveToSpawnGoal;
import me.itsnutt.guardmobs.Goals.CustomTargetingGoal;
import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.HashSet;
import java.util.UUID;

public class Mage extends Witch implements GuardMob, InventoryHolder, Armorable {

    private final boolean targetNonTeamPlayers;
    private final boolean targetHostileMobs;
    private final CustomEntityType customEntityType = CustomEntityType.MAGE;
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

    public Mage(Location spawnLocation, String regionID, Integer tier, UUID guardMobID){
        super(EntityType.WITCH, ((CraftWorld) spawnLocation.getWorld()).getHandle());
        int tempTier;

        this.regionID = regionID;
        this.spawnLocation = spawnLocation;
        targetHostileMobs = true;
        targetNonTeamPlayers = true;
        tempTier = tier;
        if (tier > 5){
            tempTier = 5;
        }
        this.tier = tempTier;

        this.guardMobID = guardMobID==null ? UUID.randomUUID() : guardMobID;

        this.setUUID(UUID.randomUUID());

        persist = true;
        setPersistenceRequired();

        StatConfiguration stats = GuardMobs.getStatConfig();
        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(stats.getConfigHealth(customEntityType, tier));
        this.setHealth(getMaxHealth());

        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(stats.getConfigDamage(customEntityType, tier));

        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta potionMeta = potion.getItemMeta();
        PotionMeta actualPotionMeta = (PotionMeta) potionMeta;
        actualPotionMeta.setColor(Color.MAROON);
        potion.setItemMeta(actualPotionMeta);
        setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(potion));

        setCanPickUpLoot(false);

        Style style = Style.EMPTY;
        style = style.withColor(ChatFormatting.DARK_RED);
        setCustomName(Component.literal("Mage " + "lvl" + tier).setStyle(style));
        setCustomNameVisible(true);

        inventory = GuardMobData.getGuardMobInventory(this);
        inventory = Util.prepareInventory(this);

        goalSelector.removeAllGoals();
        targetSelector.removeAllGoals();

        goalSelector.addGoal( 1, new FloatGoal(this));
        goalSelector.addGoal( 2, new RangedAttackGoal(this, 1 + ((double) tier/10), (int) Math.ceil(30/(double)tier+1), 10)); //var3 = attack delay
        goalSelector.addGoal( 4, new CustomMoveToSpawnGoal(this, 1,0));
        goalSelector.addGoal( 5, new CustomFollowGoal(this, 1.3 + ((double)tier/10)));
        goalSelector.addGoal( 6, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        goalSelector.addGoal( 7, new RandomLookAroundGoal(this));

        targetSelector.addGoal( 3, new CustomTargetingGoal(this));

        teleportToWithTicket(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
    }

    @Override
    public void performRangedAttack(LivingEntity entityliving, float f) {
        if (!isDrinkingPotion()) {
            Vec3 vec3d = entityliving.getDeltaMovement();
            double d0 = entityliving.getX() + vec3d.x - getX();
            double d1 = entityliving.getEyeY() - 1.100000023841858D - getY();
            double d2 = entityliving.getZ() + vec3d.z - getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            Potion potionregistry = tier==5 ? Potions.STRONG_HARMING : Potions.HARMING;

            if (d3 >= 8.0D && !entityliving.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && random.nextFloat() < 0.50F) {
                potionregistry = tier==5 ? Potions.STRONG_SLOWNESS : Potions.SLOWNESS;
            } else if (entityliving.getHealth() >= 8.0F && !entityliving.hasEffect(MobEffects.POISON) && entityliving.getMobType() != MobType.UNDEAD
            && entityliving.getMobType() != MobType.ARTHROPOD) {
                potionregistry = tier==5 ? Potions.STRONG_POISON : Potions.POISON;
            } else if (d3 <= 3.0D && !entityliving.hasEffect(MobEffects.WEAKNESS) && random.nextFloat() < 0.25F) {
                potionregistry = Potions.WEAKNESS;
            } else if (entityliving.getMobType() == MobType.UNDEAD){
                potionregistry = tier==5 ? Potions.STRONG_HEALING : Potions.HEALING;
            }

            org.bukkit.entity.LivingEntity entityLivingBukkit = (org.bukkit.entity.LivingEntity) entityliving.getBukkitEntity();
            boolean doAttack = true;
            HashSet<Entity> surrounding = new HashSet<>(entityLivingBukkit.getWorld().getNearbyEntities(entityLivingBukkit.getLocation(), 3.5, 3.5, 3.5));
            surrounding.removeIf(entity -> !(entity instanceof org.bukkit.entity.LivingEntity) && entity == this.getBukkitEntity());
            for (Entity entity : surrounding){
                if (!(entity instanceof org.bukkit.entity.LivingEntity livingEntitySurrounding))continue;
                LivingEntity livingEntity = ((CraftLivingEntity) livingEntitySurrounding).getHandle();
                if ((Util.isAlly(livingEntity.getBukkitEntity(), regionID) && livingEntity.getMobType() != MobType.UNDEAD)){
                    doAttack = false;
                    break;
                }
                if ((!Util.isAlly(livingEntity.getBukkitEntity(), regionID) && livingEntity.getMobType() == MobType.UNDEAD)){
                    doAttack = false;
                    break;
                }

            }

            if (!doAttack)return;

            ThrownPotion entitypotion = new ThrownPotion(level, this);
            entitypotion.setItem(PotionUtils.setPotion(new net.minecraft.world.item.ItemStack(Items.SPLASH_POTION), potionregistry));
            entitypotion.setXRot(entitypotion.getXRot() - 20.0F);
            entitypotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
            if (!isSilent()) {
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITCH_THROW, getSoundSource(), 1.0F, 0.8F + random.nextFloat() * 0.4F);
            }
            level.addFreshEntity(entitypotion);
        }
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
        if (getTarget() == null){
            return;
        }

        HashSet<Entity> potentialTargets;
        if (getLocation().getWorld() != null){
            potentialTargets = new HashSet<>(getLocation().getWorld().getNearbyEntities((this).getLocation(), 16 + tier, 8, 16 + tier));
            potentialTargets.removeIf(entity -> !(((CraftEntity) entity).getHandle() instanceof Monster) && !(((CraftEntity) entity).getHandle() instanceof Player));
            potentialTargets.removeIf(entity -> ((CraftEntity)entity).getHandle() instanceof GuardMob);
        }else {return;}

        Entity target = getTarget().getBukkitEntity();

        if (getLastHurtByMob() != null){
            if (!Util.isAlly(getLastHurtByMob().getBukkitEntity(), regionID)){
                if (target.getLocation().distance(getLocation()) > getLastHurtByMob().getBukkitEntity().getLocation().distance(getLocation())) {
                    setTarget(getLastHurtByMob(), EntityTargetEvent.TargetReason.CUSTOM, false);
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
            if (entity.getLocation().distance(getLocation()) < target.getLocation().distance(getLocation())){
                setTarget((LivingEntity) potentialTarget , EntityTargetEvent.TargetReason.CUSTOM, false);
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
        if (getHealth() != getMaxHealth()){
            if (healTickCount != (Math.floor((double) 25/tier))*20) {
                if (getTarget() == null) {
                    healTickCount++;
                } else {
                    healTickCount = 0;
                }
            } else {
                heal((float) Math.ceil((double) tier/2));
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
            helmet = null;
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
