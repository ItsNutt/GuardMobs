package me.itsnutt.guardmobs.Goals;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.phys.Vec3;

public class CustomControlledWanderGoal extends WaterAvoidingRandomStrollGoal {

    private final double range;

    public CustomControlledWanderGoal(PathfinderMob var0, double var1, double range) {
        super(var0, var1);
        this.range = range;
    }

    @Override
    public boolean canContinueToUse(){
        if (!(mob instanceof GuardMob guardMob)){
            return false;
        }
        return !this.mob.getNavigation().isDone() && guardMob.getLocation().distance(guardMob.getSpawnLocation()) < range;
    }

    //ONLY USE FOR GuardMobs!
    @Override
    public boolean canUse() {
        if (this.mob.isVehicle() || !(mob instanceof GuardMob)) {
            return false;
        } else {
            if (!this.forceTrigger) {
                if (this.mob.getNoActionTime() >= 100) {
                    return false;
                }

                if (this.mob.getRandom().nextInt(reducedTickDelay(this.interval)) != 0) {
                    return false;
                }
            }

            Vec3 var0 = this.getPosition();
            if (var0 == null) {
                return false;
            } else {
                this.wantedX = var0.x;
                this.wantedY = var0.y;
                this.wantedZ = var0.z;
                this.forceTrigger = false;
                return true;
            }
        }
    }
}
