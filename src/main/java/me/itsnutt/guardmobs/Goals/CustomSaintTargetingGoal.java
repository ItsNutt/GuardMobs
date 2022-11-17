package me.itsnutt.guardmobs.Goals;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Mobs.Saint;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.HashSet;

public class CustomSaintTargetingGoal extends TargetGoal {
    public CustomSaintTargetingGoal(Mob entityinsentient) {
        super(entityinsentient, false);
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public void start(){
        //Only Use This Goal For Saint
        if (!(mob instanceof Saint)){
            return;
        }
        //Remove Dead Targets
        if (mob.getTarget() != null){
            if (!mob.getTarget().isAlive()){
                mob.setTarget(null);
            }
        }
        //Remove Full Health Friendly Targets
        if (mob.getTarget() != null){
            if (Util.isAlly(mob.getTarget().getBukkitEntity(), ((Saint) mob).getRegionID())){
                if (Util.hasAllSaintBuffs((org.bukkit.entity.LivingEntity) mob.getTarget().getBukkitEntity(), ((Saint) mob).getTier())){
                    mob.setTarget(null);
                }
            }
        }


        //Getting Potential Targets
        HashSet<Entity> potentialTargets = new HashSet<>(((GuardMob) mob).getLocation().getWorld().getNearbyEntities(((GuardMob) mob)
                .getLocation(), 16 + ((GuardMob) mob).getTier(), 8, 16 + ((GuardMob)mob).getTier()));

        //Remove Potential Targets That Aren't Monsters, Players, or GuardMobs
        potentialTargets.removeIf(entity -> !(((CraftEntity) entity).getHandle() instanceof net.minecraft.world.entity.monster.Monster) &&
                !(((CraftEntity) entity).getHandle() instanceof net.minecraft.world.entity.player.Player) &&
                !(((CraftEntity) entity).getHandle() instanceof GuardMob));
        //Remove Potential Targets That Cannot Be Seen
        potentialTargets.removeIf(entity -> !mob.hasLineOfSight(((CraftEntity) entity).getHandle()));
        //Remove Self
        potentialTargets.removeIf(entity -> entity == mob.getBukkitEntity());

        potentialTargets.removeIf(entity -> entity instanceof Player player && player.getGameMode() == GameMode.CREATIVE);

        //No Potential Targets == No Targeting
        if (potentialTargets.isEmpty()){
            return;
        }

        for (Entity entity : potentialTargets){
            net.minecraft.world.entity.Entity potentialTarget = ((CraftEntity) entity).getHandle();
            if (potentialTarget instanceof GuardMob && potentialTarget instanceof LivingEntity livingEntity){
                if (livingEntity.getMobType() != MobType.UNDEAD) {
                    if (!Util.hasSaintBuff((org.bukkit.entity.LivingEntity) livingEntity.getBukkitEntity(), 1)){
                        mob.setTarget(livingEntity, EntityTargetEvent.TargetReason.CUSTOM, false);
                        return;
                    }
                }
            }
            if (potentialTarget instanceof net.minecraft.world.entity.player.Player player){
                if (Util.isRegionMember((org.bukkit.entity.Player) player.getBukkitEntity(), ((Saint) mob).getRegionID())){
                    if (!Util.hasAllSaintBuffs(player.getBukkitEntity(), ((Saint) mob).getTier())){
                        mob.setTarget(player, EntityTargetEvent.TargetReason.CUSTOM, false);
                        return;
                    }
                }
            }
        }

    }
}
