package me.itsnutt.guardmobs.Goals;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;

public class CustomMoveToSpawnGoal extends MoveToBlockGoal {

    //This Class Is Pretty Much Just A MoveToBlockGoal with the target block always being the Guard Mob's spawn position
    //Used purely to ensure the Guard Mob does not stray from their spawn point
    public CustomMoveToSpawnGoal(PathfinderMob var0, double var1, int var3) {
        super(var0, var1, var3);
    }

    @Override
    protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
        return true;
    }

    //ONLY USE FOR GuardMobs!
    @Override
    protected boolean findNearestBlock(){
        if (mob instanceof GuardMob){
            this.blockPos = ((GuardMob) mob).getSpawnBlockPos();
            return true;
        }
        return false;
    }
}
