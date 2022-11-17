package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class EntityDamageListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event){

        Entity victim = event.getEntity();
        net.minecraft.world.entity.Entity nmsVictim = ((CraftEntity) victim).getHandle();
        Entity damager = event.getDamager();

        if (nmsVictim instanceof GuardMob guardMob){
            if (Util.isAlly(damager, guardMob.getRegionID())){
                event.setCancelled(true);
                return;
            }
        }

        if (nmsVictim instanceof GuardMob guardMob){
            LivingEntity livingEntity = (LivingEntity) guardMob.getEntity();
            if (Util.hasTotem(guardMob)){
                if (event.getFinalDamage() >= livingEntity.getHealth()){
                    event.setCancelled(true);
                    livingEntity.setHealth(livingEntity.getMaxHealth());
                    guardMob.getInventory().setItem(1, null);
                    guardMob.refreshInventory();
                    guardMob.getLocation().getWorld().playSound(guardMob.getLocation(), Sound.ITEM_TOTEM_USE, 3, 1.25f);
                }
            }
        }
    }
}
