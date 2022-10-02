package me.itsnutt.guardmobs.Mobs;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.Goals.CustomMoveToSpawnGoal;
import me.itsnutt.guardmobs.Goals.CustomTargetingGoal;
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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.UUID;

public class Mage extends Witch implements GuardMob, InventoryHolder {

    private final boolean targetNonTeamPlayers;
    private final boolean targetHostileMobs;
    private final CustomEntityType customEntityType = CustomEntityType.MAGE;
    private final String regionID;
    private final Location spawnLocation;
    private final int tier;
    private Inventory inventory;

    /*
     * The Concept of 'Tiers' is as follows:
     * -The differences between each subsequent tier are not huge, but they are noticeable
     * -The difference in strength between tier 1 and 5 is very noticeable
     * -The difference in strength between tier 1 and 2 is moderately noticeable
     * -The difference in strength between tier 4 and 5 is barely noticeable
     * -As tier goes up, attack damage, health, healing interval, and "intelligence" (targeting efficiency and capability) improve
     * -Diminishing returns is the name of the game, though this is not true for health and damage (as they scale linearly)
     */

    public Mage(Location spawnLocation, String regionID, Integer tier){
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
        this.setUUID(UUID.randomUUID());

        this.persist = true;
        this.setPersistenceRequired();
        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(26+(tier*4));
        this.setHealth(26+(tier*4));

        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(0);

        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta potionMeta = potion.getItemMeta();
        PotionMeta actualPotionMeta = (PotionMeta) potionMeta;
        PotionEffect potionEffect = new PotionEffect(PotionEffectType.HARM, 0, tier);
        actualPotionMeta.addCustomEffect(potionEffect, true);
        potion.setItemMeta(actualPotionMeta);
        this.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(potion));

        this.setCanPickUpLoot(false);

        Style style = Style.EMPTY;
        style = style.withColor(ChatFormatting.DARK_RED);
        this.setCustomName(Component.literal("Mage " + "lvl" + tier).setStyle(style));
        this.setCustomNameVisible(true);

        this.inventory = Bukkit.createInventory(this, 9, ChatColor.BLACK + "Mage Menu");
        inventory = Util.prepareInventory(this);

        this.goalSelector.removeAllGoals();
        this.targetSelector.removeAllGoals();

        this.goalSelector.addGoal( 1, new FloatGoal(this));
        this.goalSelector.addGoal( 2, new RangedAttackGoal(this, 1 + ((double) tier/10), (int) Math.ceil(30/(double)tier+1), 10)); //var3 = attack delay
        this.goalSelector.addGoal( 4, new CustomMoveToSpawnGoal(this, 1,0));
        this.goalSelector.addGoal( 5, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        this.goalSelector.addGoal( 6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal( 3, new CustomTargetingGoal(this));

        this.teleportToWithTicket(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
    }

    @Override
    public void performRangedAttack(LivingEntity entityliving, float f) {
        if (!this.isDrinkingPotion()) {
            Vec3 vec3d = entityliving.getDeltaMovement();
            double d0 = entityliving.getX() + vec3d.x - this.getX();
            double d1 = entityliving.getEyeY() - 1.100000023841858D - this.getY();
            double d2 = entityliving.getZ() + vec3d.z - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            Potion potionregistry = Potions.HARMING;
            if (tier == 5){
                potionregistry = Potions.STRONG_HARMING;
            }

            if (d3 >= 8.0D && !entityliving.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && this.random.nextFloat() < 0.50F) {
                potionregistry = Potions.SLOWNESS;
                if (tier == 5){
                    potionregistry = Potions.STRONG_SLOWNESS;
                }
            } else if (entityliving.getHealth() >= 8.0F && !entityliving.hasEffect(MobEffects.POISON) && entityliving.getMobType() != MobType.UNDEAD
            && entityliving.getMobType() != MobType.ARTHROPOD) {
                potionregistry = Potions.POISON;
                if (tier == 5){
                    potionregistry = Potions.STRONG_POISON;
                }
            } else if (d3 <= 3.0D && !entityliving.hasEffect(MobEffects.WEAKNESS) && this.random.nextFloat() < 0.25F) {
                potionregistry = Potions.WEAKNESS;
            } else if (entityliving.getMobType() == MobType.UNDEAD){
                potionregistry = Potions.HEALING;
                if (tier == 5){
                    potionregistry = Potions.STRONG_HEALING;
                }
            }

            /*
            if (entityliving instanceof Player player){
                if (Util.isRegionMember((org.bukkit.entity.Player) player.getBukkitEntity(), this.regionID)){
                    if (player.getHealth() >= player.getMaxHealth()){
                        this.setTarget(null);
                        return;
                    }
                    potionregistry = Potions.REGENERATION;
                    if (tier == 5){
                        potionregistry = Potions.STRONG_REGENERATION;
                    }
                }
            }

            if (entityliving instanceof GuardMob){
                if (Util.hasSameRegionID(entityliving.getBukkitEntity(), this.regionID)){
                    if (entityliving.getHealth() >= entityliving.getMaxHealth()){
                        this.setTarget(null);
                        return;
                    }
                    if (entityliving.getMobType() == MobType.UNDEAD){
                        potionregistry = Potions.HARMING;
                        if (tier == 5){
                            potionregistry = Potions.STRONG_HARMING;
                        }
                    } else {
                        potionregistry = Potions.HEALING;
                        if (tier == 5){
                            potionregistry = Potions.STRONG_HEALING;
                        }
                    }
                }
            }

             */

            ThrownPotion entitypotion = new ThrownPotion(this.level, this);
            entitypotion.setItem(PotionUtils.setPotion(new net.minecraft.world.item.ItemStack(Items.SPLASH_POTION), potionregistry));
            entitypotion.setXRot(entitypotion.getXRot() - 20.0F);
            entitypotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
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
        return new GuardMobProfile(customEntityType, spawnLocation, regionID, tier);
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
}
