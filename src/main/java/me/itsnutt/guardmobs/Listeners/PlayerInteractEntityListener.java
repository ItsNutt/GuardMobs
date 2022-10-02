package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractEntityListener implements Listener {

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        CraftEntity craftEntity = (CraftEntity) entity;

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        if (Util.isSpawnerItem(player.getInventory().getItemInMainHand()))event.setCancelled(true);

        if (!(craftEntity.getHandle() instanceof GuardMob guardMob))return;

        player.openInventory(guardMob.getInventory());

    }
}
