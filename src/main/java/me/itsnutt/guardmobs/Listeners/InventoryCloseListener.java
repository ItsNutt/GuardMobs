package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.Data.GuardMobData;
import me.itsnutt.guardmobs.Mobs.Armorable;
import me.itsnutt.guardmobs.Mobs.GuardMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){

        if (event.getInventory().getHolder() instanceof GuardMob guardMob){
            GuardMobData.saveGuardMobInventory(guardMob);
            if (guardMob instanceof Armorable armorable){
                armorable.refreshArmor();
            }
        }
    }
}
