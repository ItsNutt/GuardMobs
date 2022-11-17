package me.itsnutt.guardmobs.Goals;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.HashSet;

public class CustomTargetingGoal extends TargetGoal {
    public CustomTargetingGoal(Mob entityinsentient) {
        super(entityinsentient, false);
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start(){
        //Only Use This Goal For GuardMobs
        if (!(mob instanceof GuardMob)){
            return;
        }
        //Remove Dead Targets
        if (mob.getTarget() != null){
            if (!mob.getTarget().isAlive()){
                mob.setTarget(null);
            }
        }
        //Prioritizing Living Things That Hurt The GuardMob
        if (mob.getLastHurtByMob() != null){
            if (mob.getLastHurtByMob().isAlive()){
                if (mob.getLastHurtByMob().getBukkitEntity() instanceof Player player){
                    if (Util.isRegionMember(player, ((GuardMob) mob).getRegionID())){
                        mob.setLastHurtByMob(null);
                    } else {
                        mob.setTarget(mob.lastHurtByMob, EntityTargetEvent.TargetReason.CUSTOM, false);
                        return;
                    }
                }else if (Util.hasSameRegionID(mob.lastHurtByMob.getBukkitEntity(), ((GuardMob) mob).getRegionID())){
                    mob.setLastHurtByMob(null);
                }else{
                    mob.setTarget(mob.lastHurtByMob, EntityTargetEvent.TargetReason.CUSTOM, false);
                    return;
                }
            }else{
                mob.setLastHurtByMob(null);
            }
        }
        //Getting Potential Targets
        HashSet<Entity> potentialTargets = new HashSet<>(((GuardMob) mob).getLocation().getWorld().getNearbyEntities(((GuardMob) mob)
                .getLocation(), 16 + ((GuardMob) mob).getTier(), 8, 16 + ((GuardMob)mob).getTier()));

        //Remove Potential Targets That Aren't Monsters or Players
        potentialTargets.removeIf(entity -> !(((CraftEntity) entity).getHandle() instanceof net.minecraft.world.entity.monster.Monster) &&
                !(((CraftEntity) entity).getHandle() instanceof net.minecraft.world.entity.player.Player));
        //Remove Potential Targets That Cannot Be Seen
        potentialTargets.removeIf(entity -> !mob.hasLineOfSight(((CraftEntity) entity).getHandle()));
        //Remove Self
        potentialTargets.removeIf(entity -> entity == mob.getBukkitEntity());
        potentialTargets.removeIf(entity -> entity instanceof Player player && player.getGameMode() == GameMode.CREATIVE);

        //No Potential Targets == No Targeting
        if (potentialTargets.isEmpty()){
            return;
        }

        //Prioritize Targeting Non-Friendly Players Over Hostile Mobs
        if (((GuardMob) mob).getTargetNonTeamPlayers()){
            for (Entity entity : potentialTargets){
                if (entity instanceof Player player){
                    if (!Util.isRegionMember(player, ((GuardMob) mob).getRegionID())){
                        mob.setTarget(((CraftPlayer)player).getHandle(), EntityTargetEvent.TargetReason.CUSTOM, false);
                        return;
                    }
                }
            }
        }

        //Don't Need to Run Through Entities We've Already Checked Aren't Potential Targets
        potentialTargets.removeIf(entity -> entity instanceof Player);

        //Target Hostile Mobs
        if (((GuardMob) mob).getTargetHostileMobs()){
            for (Entity entity : potentialTargets){
                if (entity instanceof Monster){
                    if (Util.hasSameRegionID(entity, ((GuardMob) mob).getRegionID())){
                        continue;
                    }
                    mob.setTarget((LivingEntity) ((CraftEntity) entity).getHandle(), EntityTargetEvent.TargetReason.CUSTOM, false);
                    return;
                }
            }
        }

    }
}
