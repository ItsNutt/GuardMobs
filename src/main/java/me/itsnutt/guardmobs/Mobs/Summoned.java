package me.itsnutt.guardmobs.Mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.EnumSet;

public class Summoned extends Vex {

    public Summoned(Conjurer conjurer){
        super(EntityType.VEX, conjurer.level);
        owner = conjurer;

        ((org.bukkit.entity.LivingEntity) this.getBukkitEntity()).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(3+(Math.floor(((GuardMob)owner).getTier())));
        this.setOwner(owner);
    }

    Mob owner;

    @Override
    protected void registerGoals() {

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new Summoned.SummonedChargeAttackGoal());
        this.goalSelector.addGoal(8, new Summoned.SummonedRandomMoveGoal());
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));

        this.targetSelector.addGoal(2, new Summoned.SummonedCopyOwnerTargetGoal(this));
        //this.targetSelector.addGoal(3, new CustomTargetingGoal(this));


    }

    private class SummonedChargeAttackGoal extends Goal {
        public SummonedChargeAttackGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        public boolean canUse() {
            LivingEntity entityliving = Summoned.this.getTarget();
            return entityliving != null && entityliving.isAlive() && !Summoned.this.getMoveControl().hasWanted() && Summoned.this.random.nextInt(reducedTickDelay(7)) == 0 && Summoned.this.distanceToSqr(entityliving) > 4.0D;
        }

        public boolean canContinueToUse() {
            return Summoned.this.getMoveControl().hasWanted() && Summoned.this.isCharging() && Summoned.this.getTarget() != null && Summoned.this.getTarget().isAlive();
        }

        public void start() {
            LivingEntity entityliving = Summoned.this.getTarget();
            if (entityliving != null) {
                Vec3 vec3d = entityliving.getEyePosition();
                Summoned.this.moveControl.setWantedPosition(vec3d.x, vec3d.y, vec3d.z, 1.0D);
            }

            Summoned.this.setIsCharging(true);
            Summoned.this.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
        }

        public void stop() {
            Summoned.this.setIsCharging(false);
        }

        public boolean requiresUpdateEveryTick() {
            return true;
        }

        public void tick() {
            LivingEntity entityliving = Summoned.this.getTarget();
            if (entityliving != null) {
                if (Summoned.this.getBoundingBox().intersects(entityliving.getBoundingBox())) {
                    Summoned.this.doHurtTarget(entityliving);
                    Summoned.this.setIsCharging(false);
                } else {
                    double d0 = Summoned.this.distanceToSqr(entityliving);
                    if (d0 < 9.0D) {
                        Vec3 vec3d = entityliving.getEyePosition();
                        Summoned.this.moveControl.setWantedPosition(vec3d.x, vec3d.y, vec3d.z, 1.0D);
                    }
                }
            }

        }
    }

    private class SummonedCopyOwnerTargetGoal extends TargetGoal {
        private final TargetingConditions copyOwnerTargeting = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting();

        public SummonedCopyOwnerTargetGoal(PathfinderMob entitycreature) {
            super(entitycreature, false);
        }

        public boolean canUse() {
            return Summoned.this.owner != null && Summoned.this.owner.getTarget() != null && this.canAttack(Summoned.this.owner.getTarget(), this.copyOwnerTargeting);
        }

        public void start() {
            Summoned.this.setTarget(Summoned.this.owner.getTarget(), EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true);
            super.start();
        }
    }

    private class SummonedRandomMoveGoal extends Goal {
        public SummonedRandomMoveGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        public boolean canUse() {
            return !Summoned.this.getMoveControl().hasWanted() && Summoned.this.random.nextInt(reducedTickDelay(7)) == 0;
        }

        public boolean canContinueToUse() {
            return false;
        }

        public void tick() {
            BlockPos blockposition = Summoned.this.getBoundOrigin();
            if (blockposition == null) {
                blockposition = Summoned.this.blockPosition();
            }

            for(int i = 0; i < 3; ++i) {
                BlockPos blockposition1 = blockposition.offset(Summoned.this.random.nextInt(15) - 7, Summoned.this.random.nextInt(11) - 5, Summoned.this.random.nextInt(15) - 7);
                if (Summoned.this.level.isEmptyBlock(blockposition1)) {
                    Summoned.this.moveControl.setWantedPosition((double)blockposition1.getX() + 0.5D, (double)blockposition1.getY() + 0.5D, (double)blockposition1.getZ() + 0.5D, 0.25D);
                    if (Summoned.this.getTarget() == null) {
                        Summoned.this.getLookControl().setLookAt((double)blockposition1.getX() + 0.5D, (double)blockposition1.getY() + 0.5D, (double)blockposition1.getZ() + 0.5D, 180.0F, 20.0F);
                    }
                    break;
                }
            }

        }
    }
}
