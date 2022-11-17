package me.itsnutt.guardmobs.Mobs;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobData;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.Data.StatConfiguration;
import me.itsnutt.guardmobs.Goals.CustomFollowGoal;
import me.itsnutt.guardmobs.Goals.CustomMoveToSpawnGoal;
import me.itsnutt.guardmobs.Goals.CustomSaintTargetingGoal;
import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Saint extends Witch implements GuardMob, InventoryHolder, Armorable {

    private final boolean targetNonTeamPlayers;
    private final boolean targetHostileMobs;
    private final CustomEntityType customEntityType = CustomEntityType.SAINT;
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

    public Saint(Location spawnLocation, String regionID, Integer tier, UUID guardMobID){
        super(EntityType.WITCH, ((CraftWorld) spawnLocation.getWorld()).getHandle());
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

        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta potionMeta = potion.getItemMeta();
        PotionMeta actualPotionMeta = (PotionMeta) potionMeta;
        actualPotionMeta.setColor(Color.FUCHSIA);
        potion.setItemMeta(actualPotionMeta);
        this.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(potion));

        this.setCanPickUpLoot(false);

        Style style = Style.EMPTY;
        style = style.withColor(ChatFormatting.DARK_RED);
        this.setCustomName(Component.literal("Saint " + "lvl" + tier).setStyle(style));
        this.setCustomNameVisible(true);

        inventory = GuardMobData.getGuardMobInventory(this);
        inventory = Util.prepareInventory(this);

        this.goalSelector.removeAllGoals();
        this.targetSelector.removeAllGoals();

        this.goalSelector.addGoal( 1, new FloatGoal(this));
        this.goalSelector.addGoal( 2, new RangedAttackGoal(this, 1 + ((double) tier/10), (int) Math.ceil(30/(double)tier+1), 10)); //var3 = attack delay
        this.goalSelector.addGoal( 4, new CustomMoveToSpawnGoal(this, 1,0));
        this.goalSelector.addGoal( 5, new CustomFollowGoal(this, 1.3 + ((double)tier/10)));
        this.goalSelector.addGoal( 6, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        this.goalSelector.addGoal( 7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal( 3, new CustomSaintTargetingGoal(this));

        this.teleportToWithTicket(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
    }

    @Override
    public void performRangedAttack(LivingEntity entityliving, float f) {
        if (!this.isDrinkingPotion()) {
            if (getTarget() == this && this.getHealth() == this.getMaxHealth()){
                this.setTarget(null);
                return;
            }
            if (this.getTarget() == null){
                return;
            }
            if (Util.isAlly(this.getTarget().getBukkitEntity(), regionID) && this.getTarget().getMobType() != MobType.UNDEAD){
                if (Util.hasAllSaintBuffs((org.bukkit.entity.LivingEntity) this.getTarget().getBukkitEntity(), tier)){
                    this.setTarget(null);
                    return;
                }
            }
            Vec3 vec3d = entityliving.getDeltaMovement();
            double d0 = entityliving.getX() + vec3d.x - this.getX();
            double d1 = entityliving.getEyeY() - 1.100000023841858D - this.getY();
            double d2 = entityliving.getZ() + vec3d.z - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            Potion potionregistry = Potions.HEALING;
            if (tier == 5){
                potionregistry = Potions.STRONG_HEALING;
            }

            org.bukkit.entity.LivingEntity entityLivingBukkit = (CraftLivingEntity) entityliving.getBukkitEntity();
            HashSet<Entity> surroundingTarget = new HashSet<>(entityLivingBukkit.getWorld().getNearbyEntities(entityLivingBukkit.getLocation(), 2.5, 2, 2.5));
            surroundingTarget.removeIf(entity -> !(entity instanceof LivingEntity));
            boolean doAttack = true;
            for (Entity entity : surroundingTarget){
                if (!(entity instanceof org.bukkit.entity.LivingEntity livingEntityBukkit))continue;
                LivingEntity livingEntity = ((CraftLivingEntity) livingEntityBukkit).getHandle();
                if ((Util.isAlly(livingEntity.getBukkitEntity(), regionID) && livingEntity.getMobType() == MobType.UNDEAD)){
                    doAttack = false;
                    break;
                }
                if ((!Util.isAlly(livingEntity.getBukkitEntity(), regionID) && livingEntity.getMobType() != MobType.UNDEAD)){
                    doAttack = false;
                    break;
                }
                
            }
            
            if (!doAttack){
                this.setTarget(null);
                return;
            }


            if (entityliving instanceof Player player){
                if (Util.isRegionMember((org.bukkit.entity.Player) player.getBukkitEntity(), this.regionID)){
                    org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) player.getBukkitEntity();
                    List<Potion> potions = new ArrayList<>();

                    if (tier >= 1 && !Util.hasSaintBuff( bukkitPlayer, 1)) {
                        potions.add(Potions.HEALING);
                        if (tier == 5) {
                            potions.add(Potions.STRONG_HEALING);
                        }
                    }
                    if (tier >= 2 && !Util.hasSaintBuff( bukkitPlayer, 2)) {
                        potions.add(Potions.REGENERATION);
                        if (tier == 5) {
                            potions.add(Potions.STRONG_REGENERATION);
                        }
                    }
                    if (tier >= 3 && !Util.hasSaintBuff( bukkitPlayer, 3)) {
                        potions.add(Potions.FIRE_RESISTANCE);
                        if (tier == 5) {
                            potions.add(Potions.LONG_FIRE_RESISTANCE);
                        }
                    }
                    if (tier >= 4 && !Util.hasSaintBuff( bukkitPlayer, 4)) {
                        potions.add(Potions.SWIFTNESS);
                        if (tier == 5) {
                            potions.add(Potions.STRONG_SWIFTNESS);
                        }
                    }
                    if (tier == 5  && !Util.hasSaintBuff( bukkitPlayer, 5)) {
                        potions.add(Potions.STRENGTH);
                    }
                    if (potions.isEmpty()){
                        this.setTarget(null);
                        return;
                    }
                    potionregistry = potions.get(ThreadLocalRandom.current().nextInt(0, potions.size()));
                }
            }

            ThrownPotion entitypotion = new ThrownPotion(this.level, this);
            entitypotion.setItem(PotionUtils.setPotion(new net.minecraft.world.item.ItemStack(Items.SPLASH_POTION), potionregistry));
            entitypotion.setXRot(entitypotion.getXRot() - 20.0F);
            entitypotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.6F, 7.0F);
            if (!this.isSilent()) {
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            }
            this.level.addFreshEntity(entitypotion);
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
        if (this.getTarget() == null){
            return;
        }

        HashSet<Entity> potentialTargets;
        if (this.getLocation().getWorld() != null){
            potentialTargets = new HashSet<>(this.getLocation().getWorld().getNearbyEntities((this).getLocation(), 16 + tier, 8, 16 + tier));
            potentialTargets.removeIf(entity -> !(((CraftEntity) entity).getHandle() instanceof GuardMob) && !(((CraftEntity) entity).getHandle() instanceof Player));
            if (this.getHealth() >= this.getMaxHealth()){
                potentialTargets.remove(this);
            }
        }else {return;}

        for (Entity entity : potentialTargets){
            net.minecraft.world.entity.Entity potentialTarget = ((CraftEntity) entity).getHandle();
            if (potentialTarget instanceof GuardMob && potentialTarget instanceof LivingEntity livingEntity){
                if (livingEntity.getMobType() != MobType.UNDEAD) {
                    if (!Util.hasSaintBuff((org.bukkit.entity.LivingEntity) livingEntity.getBukkitEntity(), 1)){
                        this.setTarget(livingEntity, EntityTargetEvent.TargetReason.CUSTOM, false);
                        return;
                    }
                }
            }
            if (potentialTarget instanceof Player player){
                if (Util.isRegionMember((org.bukkit.entity.Player) player.getBukkitEntity(), regionID)){
                    if (!Util.hasAllSaintBuffs(player.getBukkitEntity(), tier)){
                        this.setTarget(player, EntityTargetEvent.TargetReason.CUSTOM, false);
                        return;
                    }
                }
            }
        }
        this.setTarget(null);
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
                this.heal((float) Math.ceil((double) tier/2));
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
