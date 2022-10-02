package me.itsnutt.guardmobs.Goals;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class CustomMeleeAttackGoal extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private Path path;
    private double pathedTargetX;
    private double pathedTargetY;
    private double pathedTargetZ;
    private int ticksUntilNextPathRecalculation;
    private int ticksUntilNextAttack;
    private final int attackInterval = 20;
    private long lastCanUseCheck;
    private static final long COOLDOWN_BETWEEN_CAN_USE_CHECKS = 20L;

    //This is literally just MeleeAttackGoal but modified to improve higher tier GuardMobs' attack delays
    public CustomMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followUnseenTarget) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followUnseenTarget;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    //ONLY USE FOR GuardMobs
    public boolean canUse() {
        if (!(mob instanceof GuardMob)){
            return false;
        }
        long var0 = this.mob.level.getGameTime();
        if (var0 - this.lastCanUseCheck < 20L) {
            return false;
        } else {
            this.lastCanUseCheck = var0;
            LivingEntity var2 = this.mob.getTarget();
            if (var2 == null) {
                return false;
            } else if (!var2.isAlive()) {
                return false;
            } else {
                this.path = this.mob.getNavigation().createPath(var2, 0);
                if (this.path != null) {
                    return true;
                } else {
                    return this.getAttackReachSqr(var2) >= this.mob.distanceToSqr(var2.getX(), var2.getY(), var2.getZ());
                }
            }
        }
    }

    public boolean canContinueToUse() {
        LivingEntity var0 = this.mob.getTarget();
        if (var0 == null) {
            return false;
        } else if (!var0.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        } else if (!this.mob.isWithinRestriction(var0.blockPosition())) {
            return false;
        } else {
            return !(var0 instanceof Player) || !var0.isSpectator() && !((Player)var0).isCreative();
        }
    }

    public void start() {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.ticksUntilNextAttack = 0;
    }

    public void stop() {
        LivingEntity var0 = this.mob.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(var0)) {
            this.mob.setTarget(null);
        }

        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        LivingEntity var0 = this.mob.getTarget();
        if (var0 != null) {
            this.mob.getLookControl().setLookAt(var0, 30.0F, 30.0F);
            double var1 = this.mob.distanceToSqr(var0.getX(), var0.getY(), var0.getZ());
            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(var0)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || var0.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.mob.getRandom().nextFloat() < 0.05F)) {
                this.pathedTargetX = var0.getX();
                this.pathedTargetY = var0.getY();
                this.pathedTargetZ = var0.getZ();
                this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                if (var1 > 1024.0D) {
                    this.ticksUntilNextPathRecalculation += 10;
                } else if (var1 > 256.0D) {
                    this.ticksUntilNextPathRecalculation += 5;
                }

                if (!this.mob.getNavigation().moveTo(var0, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }

                this.ticksUntilNextPathRecalculation = this.adjustedTickDelay(this.ticksUntilNextPathRecalculation);
            }

            this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
            this.checkAndPerformAttack(var0, var1);
        }
    }

    protected void checkAndPerformAttack(LivingEntity var0, double var1) {
        double var3 = this.getAttackReachSqr(var0);
        if (var1 <= var3 && this.ticksUntilNextAttack <= 0) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.doHurtTarget(var0);
        }

    }

    protected void resetAttackCooldown() {
        if (!(mob instanceof GuardMob guardMob)){
            return;
        }
        this.ticksUntilNextAttack = this.adjustedTickDelay((int) Math.floor((double) 20/guardMob.getTier()));
    }

    protected boolean isTimeToAttack() {
        return this.ticksUntilNextAttack <= 0;
    }

    protected int getTicksUntilNextAttack() {
        return this.ticksUntilNextAttack;
    }

    protected int getAttackInterval() {
        if (!(mob instanceof GuardMob guardMob)){
            return 20;
        }
        return this.adjustedTickDelay((int) Math.floor((double) 20/guardMob.getTier()));
    }

    protected double getAttackReachSqr(LivingEntity var0) {
        return this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + var0.getBbWidth();
    }
}
