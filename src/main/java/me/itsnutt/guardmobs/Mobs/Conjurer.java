package me.itsnutt.guardmobs.Mobs;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.Goals.CustomMoveToSpawnGoal;
import me.itsnutt.guardmobs.Goals.CustomTargetingGoal;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.UUID;

public class Conjurer extends Evoker implements GuardMob, InventoryHolder {

    private final boolean targetNonTeamPlayers;
    private final boolean targetHostileMobs;
    private final CustomEntityType customEntityType = CustomEntityType.CONJURER;
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

    public Conjurer(Location spawnLocation, String regionID, Integer tier){
        super(EntityType.EVOKER, ((CraftWorld) spawnLocation.getWorld()).getHandle());
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

        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(6+tier);

        this.setCanPickUpLoot(false);

        Style style = Style.EMPTY;
        style = style.withColor(ChatFormatting.DARK_RED);
        this.setCustomName(Component.literal("Conjurer " + "lvl" + tier).setStyle(style));
        this.setCustomNameVisible(true);

        this.inventory = Bukkit.createInventory(this, 9, ChatColor.BLACK + "Conjurer Menu");
        inventory = Util.prepareInventory(this);

        this.goalSelector.removeAllGoals();
        this.targetSelector.removeAllGoals();

        this.goalSelector.addGoal( 0, new FloatGoal(this));
        this.goalSelector.addGoal( 1, new Conjurer.ConjurerCastingSpellGoal());
        this.goalSelector.addGoal( 2, new AvoidEntityGoal(this, Creeper.class, 8.0F, 0.6D, 1.0D));
        this.goalSelector.addGoal( 3, new ConjurerSummonSpellGoal());
        this.goalSelector.addGoal( 4, new ConjurerAttackSpellGoal());

        this.goalSelector.addGoal( 5, new CustomMoveToSpawnGoal(this, 1,0));
        this.goalSelector.addGoal( 6, new LookAtPlayerGoal(this, LivingEntity.class, 8));
        this.goalSelector.addGoal( 7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal( 3, new CustomTargetingGoal(this));

        this.teleportToWithTicket(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private class ConjurerCastingSpellGoal extends SpellcasterCastingSpellGoal {
        ConjurerCastingSpellGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public void tick() {
            if (Conjurer.this.getTarget() != null) {
                Conjurer.this.getLookControl().setLookAt(Conjurer.this.getTarget(), (float)Conjurer.this.getMaxHeadYRot(), (float)Conjurer.this.getMaxHeadXRot());
            }
        }
    }

    private class ConjurerSummonSpellGoal extends SpellcasterUseSpellGoal {
        private final TargetingConditions vexCountTargeting = TargetingConditions.forNonCombat().range(16.0D).ignoreLineOfSight().ignoreInvisibilityTesting();

        ConjurerSummonSpellGoal() {

        }

        public boolean canUse() {
            if (!super.canUse()) {
                return false;
            } else {
                int i = Conjurer.this.level.getNearbyEntities(Summoned.class, this.vexCountTargeting, Conjurer.this, Conjurer.this.getBoundingBox().inflate(20.0D)).size();
                return Conjurer.this.random.nextInt(tier) + 1 > i;
            }
        }

        protected int getCastingTime() {
            return 100 - (tier*15);
        }

        protected int getCastingInterval() {
            return 340 - (tier*25);
        }

        protected void performSpellCasting() {
            ServerLevel worldserver = (ServerLevel)Conjurer.this.level;

            for(int i = 0; i < Math.ceil((double) tier/4); ++i) {
                BlockPos blockposition = Conjurer.this.blockPosition().offset(-2 + Conjurer.this.random.nextInt(5), 1, -2 + Conjurer.this.random.nextInt(5));
                Summoned summoned = new Summoned(Conjurer.this);
                summoned.moveTo(blockposition, 0.0F, 0.0F);
                summoned.finalizeSpawn(worldserver, Conjurer.this.level.getCurrentDifficultyAt(blockposition), MobSpawnType.MOB_SUMMONED, null, null);
                summoned.setOwner(Conjurer.this);
                summoned.setBoundOrigin(blockposition);
                summoned.setLimitedLife(20 * (30 + Conjurer.this.random.nextInt(90)));
                CraftEntity craftEntity = summoned.getBukkitEntity();
                worldserver.addFreshEntityWithPassengers(craftEntity.getHandle(), CreatureSpawnEvent.SpawnReason.SPELL);
                craftEntity.getPersistentDataContainer().set(Util.getRegionKey(), PersistentDataType.STRING ,Conjurer.this.regionID);

            }

        }

        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_SUMMON;
        }

        protected IllagerSpell getSpell() {
            return IllagerSpell.SUMMON_VEX;
        }
    }

    private class ConjurerAttackSpellGoal extends SpellcasterUseSpellGoal {
        ConjurerAttackSpellGoal() {

        }

        protected int getCastingTime() {
            return 40 - (tier*6);
        }

        protected int getCastingInterval() {
            return 100 - (tier * 15);
        }

        protected void performSpellCasting() {
            LivingEntity entityliving = Conjurer.this.getTarget();
            double d0 = Math.min(entityliving.getY(), Conjurer.this.getY());
            double d1 = Math.max(entityliving.getY(), Conjurer.this.getY()) + 1.0D;
            float f = (float) Mth.atan2(entityliving.getZ() - Conjurer.this.getZ(), entityliving.getX() - Conjurer.this.getX());
            int i;
            if (Conjurer.this.distanceToSqr(entityliving) < 9.0D) {
                float f1;
                for(i = 0; i < 5; ++i) {
                    f1 = f + (float)i * 3.1415927F * 0.4F;
                    this.createSpellEntity(Conjurer.this.getX() + (double)Mth.cos(f1) * 1.5D, Conjurer.this.getZ() + (double)Mth.sin(f1) * 1.5D, d0, d1, f1, 0);
                }

                for(i = 0; i < 8; ++i) {
                    f1 = f + (float)i * 3.1415927F * 2.0F / 8.0F + 1.2566371F;
                    this.createSpellEntity(Conjurer.this.getX() + (double)Mth.cos(f1) * 2.5D, Conjurer.this.getZ() + (double)Mth.sin(f1) * 2.5D, d0, d1, f1, 3);
                }
            } else {
                for(i = 0; i < 16; ++i) {
                    double d2 = 1.25D * (double)(i + 1);
                    int j = 1 * i;
                    this.createSpellEntity(Conjurer.this.getX() + (double)Mth.cos(f) * d2, Conjurer.this.getZ() + (double)Mth.sin(f) * d2, d0, d1, f, j);
                }
            }

        }

        private void createSpellEntity(double d0, double d1, double d2, double d3, float f, int i) {
            BlockPos blockposition = new BlockPos(d0, d3, d1);
            boolean flag = false;
            double d4 = 0.0D;

            do {
                BlockPos blockposition1 = blockposition.below();
                BlockState iblockdata = Conjurer.this.level.getBlockState(blockposition1);
                if (iblockdata.isFaceSturdy(Conjurer.this.level, blockposition1, Direction.UP)) {
                    if (!Conjurer.this.level.isEmptyBlock(blockposition)) {
                        BlockState iblockdata1 = Conjurer.this.level.getBlockState(blockposition);
                        VoxelShape voxelshape = iblockdata1.getCollisionShape(Conjurer.this.level, blockposition);
                        if (!voxelshape.isEmpty()) {
                            d4 = voxelshape.max(Direction.Axis.Y);
                        }
                    }

                    flag = true;
                    break;
                }

                blockposition = blockposition.below();
            } while(blockposition.getY() >= Mth.floor(d2) - 1);

            if (flag) {
                Conjurer.this.level.addFreshEntity(new EvokerFangs(Conjurer.this.level, d0, (double)blockposition.getY() + d4, d1, f, i, Conjurer.this));
            }

        }

        protected SoundEvent getSpellPrepareSound() {
            return SoundEvents.EVOKER_PREPARE_ATTACK;
        }

        protected IllagerSpell getSpell() {
            return IllagerSpell.FANGS;
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

        HashSet<Entity> potentialTargets = new HashSet<>(this.getLocation().getWorld().getNearbyEntities((this).getLocation(), 16 + tier, 8, 16 + tier));
        potentialTargets.removeIf(entity -> !(((CraftEntity) entity).getHandle() instanceof Monster) && !(((CraftEntity) entity).getHandle() instanceof Player));

        Entity target = this.getTarget().getBukkitEntity();
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
    public net.minecraft.world.entity.Entity getEntity() {
        return this;
    }
}
