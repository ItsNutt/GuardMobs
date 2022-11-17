package me.itsnutt.guardmobs.Goals;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.util.Vector;

public class CustomFollowGoal extends MoveToBlockGoal {

    public CustomFollowGoal(PathfinderMob pathfinderMob, double speedModifier) {
        super(pathfinderMob, speedModifier, 0);
    }
    @Override
    protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
        return true;
    }

    protected GuardMob guardMob = (GuardMob) mob;

    protected int nextStartTick(PathfinderMob var0) {
        return reducedTickDelay(var0.getRandom().nextInt(40));
    }

    @Override
    public boolean canContinueToUse(){
        GuardMob guardMob = (GuardMob) mob;
        if(!isValidMoveSetting(guardMob))return false;
        if (guardMob.getLocation().distance(guardMob.getFollowing().getLocation()) < 3.5)return false;
        return isValidMoveSetting(guardMob);
    }

    @Override
    public boolean canUse(){
        super.canUse();
        if (!(mob instanceof GuardMob guardMob))return false;
        if (guardMob.getLocation().distance(guardMob.getFollowing().getLocation()) < 3.5)return false;
        return isValidMoveSetting(guardMob);
    }

    @Override
    protected boolean findNearestBlock(){
        if (!(mob instanceof GuardMob guardMob))return false;
        if (!isValidMoveSetting(guardMob))return false;
        Player player = ((CraftPlayer) guardMob.getFollowing()).getHandle();
        this.blockPos = player.getOnPos();
        return true;
    }

    @Override
    public void tick() {
        if (!(mob instanceof GuardMob guardMob))return;
        if (!isValidMoveSetting(guardMob))return;

        /*
        int x = ThreadLocalRandom.current().nextInt(0, 2);
        boolean left = x == 0;
        int offset = 1;
        if (left)offset *= -1;
        double multiplier = 2.5;
        Location location = guardMob.getFollowing().getLocation();
        Vector direction = new Vector();
        switch (guardMob.getFollowing().getFacing()){
            case NORTH -> direction.setX(direction.getX()+offset);
            case EAST -> direction.setZ(direction.getZ()+offset);
            case SOUTH -> direction.setX(direction.getX()-offset);
            case WEST -> direction.setZ(direction.getZ()-offset);
        }
        blockPos = Util.getBlockPosFromVector(location.add(direction.multiply(multiplier)).toVector());

         */
        double radius = 2;
        Vector vec = guardMob.getFollowing().getLocation().toVector();
        Vector finalVec = null;
        for (double x = vec.getX() - radius; x < vec.getX()+radius; x++){
            for (double z = vec.getZ() - radius; z < vec.getZ()+radius; z++){
                Vector tempVec = new Vector(x, vec.getY(), z);
                if (finalVec == null){
                    finalVec = tempVec;
                    continue;
                }
                if (guardMob.getLocation().distance(Util.locFromVec(guardMob.getEntity().getBukkitEntity(), tempVec)) <
                        guardMob.getLocation().distance(Util.locFromVec(guardMob.getEntity().getBukkitEntity(), finalVec))){
                    finalVec = tempVec;
                }
            }
        }
        blockPos = Util.getBlockPosFromVector(finalVec);
        mob.getNavigation().moveTo(blockPos.getX(), blockPos.getY(), blockPos.getZ(), speedModifier);
        super.tick();
    }

    public boolean isValidMoveSetting(GuardMob guardMob){
        if (guardMob.getMovementSetting() != GuardMob.MovementSetting.FOLLOW)return false;
        if (guardMob.getFollowing() == null){
            guardMob.setMovementSetting(GuardMob.MovementSetting.STAY_AT_SPAWN);
            Util.prepareInventory(guardMob);
            return false;
        }
        return true;
    }

    @Override
    public double acceptedDistance(){
        return 2.5;
    }
}
