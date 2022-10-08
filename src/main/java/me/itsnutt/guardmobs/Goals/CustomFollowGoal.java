package me.itsnutt.guardmobs.Goals;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;

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
        return isValidMoveSetting(guardMob);
    }

    @Override
    public boolean canUse(){
        super.canUse();
        if (!(mob instanceof GuardMob guardMob))return false;
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

        Player player = ((CraftPlayer) guardMob.getFollowing()).getHandle();
        this.blockPos = player.getOnPos();
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
}
