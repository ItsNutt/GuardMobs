package me.itsnutt.guardmobs.Mobs;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.UUID;

public class Archer extends Skeleton implements GuardMob, InventoryHolder {

    private final boolean targetNonTeamPlayers;
    private final boolean targetHostileMobs;
    private final CustomEntityType customEntityType = CustomEntityType.ARCHER;
    private final String regionID;
    private final Location spawnLocation;
    private final int tier;
    private Inventory inventory;
    private MovementSetting movementSetting = MovementSetting.STAY_AT_SPAWN;
    private org.bukkit.entity.Player following = null;

    /*
    * The Concept of 'Tiers' is as follows:
    * -The differences between each subsequent tier are not huge, but they are noticeable
    * -The difference in strength between tier 1 and 5 is very noticeable
    * -The difference in strength between tier 1 and 2 is moderately noticeable
    * -The difference in strength between tier 4 and 5 is barely noticeable
    * -As tier goes up, attack damage, health, healing interval, and "intelligence" (targeting efficiency and capability) improve
    * -Diminishing returns is the name of the game, though this is not true for health and damage (as they scale linearly)
     */

    public Archer(Location spawnLocation, String regionID, Integer tier){
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
        this.setUUID(UUID.randomUUID());

        this.persist = true;
        this.setPersistenceRequired();

        StatConfiguration stats = GuardMobs.getStatConfig();
        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(stats.getConfigHealth(customEntityType, tier));
        this.setHealth(getMaxHealth());

        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(stats.getConfigDamage(customEntityType, tier));

        this.setItemSlot(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(new ItemStack(Material.JACK_O_LANTERN)));

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.ARROW_DAMAGE, (int) Math.floor((double)tier/2), true);
        bow.setItemMeta(bowMeta);
        this.setItemSlot(EquipmentSlot.MAINHAND, CraftItemStack.asNMSCopy(bow));

        this.setCanPickUpLoot(false);

        Style style = Style.EMPTY;
        style = style.withColor(ChatFormatting.DARK_RED);
        this.setCustomName(Component.literal("Archer " + "lvl" + tier).setStyle(style));
        this.setCustomNameVisible(true);

        this.inventory = Bukkit.createInventory(this, 9, ChatColor.BLACK + "Archer Menu");
        inventory = Util.prepareInventory(this);

        this.goalSelector.removeAllGoals();
        this.targetSelector.removeAllGoals();

        this.goalSelector.addGoal( 1, new FloatGoal(this));
        this.goalSelector.addGoal( 2, new RangedBowAttackGoal<Skeleton>(this, 1 + ((double) tier/10), (int) Math.floor(20/(double)tier+1), 16)); //var3 = attack delay
        this.goalSelector.addGoal( 4, new CustomMoveToSpawnGoal(this, 1,0));
        this.goalSelector.addGoal( 5, new CustomFollowGoal(this, 1.3 + ((double)tier/10)));
        this.goalSelector.addGoal( 6, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        this.goalSelector.addGoal( 7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal( 3, new CustomTargetingGoal(this));
        this.conversionTime = -1;

        this.teleportToWithTicket(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
    }


    @Override
    public void performRangedAttack(LivingEntity entityliving, float f) {
        net.minecraft.world.item.ItemStack itemstack = this.getProjectile(this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW)));
        AbstractArrow entityarrow = this.getArrow(itemstack, f);
        double d0 = entityliving.getX() - this.getX();
        double d1 = entityliving.getY(0.3333333333333333D) - entityarrow.getY();
        double d2 = entityliving.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);

        //Increase Arrow Velocity
        d0*=2;
        d2*=2;

        //f1 = accuracy (higher number = less accurate)
        entityarrow.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, 5-tier);

        EntityShootBowEvent event = CraftEventFactory.callEntityShootBowEvent(this, this.getMainHandItem(), null, entityarrow, InteractionHand.MAIN_HAND, 0.8F, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
        } else {
            if (event.getProjectile() == entityarrow.getBukkitEntity()) {
                this.level.addFreshEntity(entityarrow);
            }

            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
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
}
