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

        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        if (Util.isSpawnerItem(player.getInventory().getItemInMainHand())) event.setCancelled(true);

        if (Util.isArmor(player.getInventory().getItemInMainHand()))return;

        if (!(craftEntity.getHandle() instanceof GuardMob guardMob)) return;

        if (!Util.isAlly(player, guardMob.getRegionID())) return;

        player.openInventory(guardMob.getInventory());

    }

    /*
    @EventHandler
    public void onPlayerInteractEntityArmorable(PlayerInteractEntityEvent event){
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        CraftEntity craftEntity = (CraftEntity) entity;
        ItemStack inHand = event.getPlayer().getInventory().getItemInMainHand();

        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        if (Util.isSpawnerItem(inHand)) event.setCancelled(true);

        net.minecraft.world.entity.EquipmentSlot equipmentSlot;
        if (Util.isArmor(inHand)) {
            event.setCancelled(true);
            if (Util.isHelmet(inHand)){
                equipmentSlot = net.minecraft.world.entity.EquipmentSlot.HEAD;
            } else if (Util.isChestPlate(inHand)){
                equipmentSlot = net.minecraft.world.entity.EquipmentSlot.CHEST;
            } else if (Util.isLeggings(inHand)){
                equipmentSlot = net.minecraft.world.entity.EquipmentSlot.LEGS;
            } else if (Util.isBoots(inHand)){
                equipmentSlot = net.minecraft.world.entity.EquipmentSlot.FEET;
            } else {return;}
        }else{
            return;
        }

        if (!(craftEntity.getHandle() instanceof GuardMob guardMob)) return;

        if (!Util.isOwner(player, guardMob.getRegionID()))return;

        if (!(craftEntity.getHandle() instanceof Armorable armorable))return;

        ItemStack worn = armorable.get(equipmentSlot);
        if (armorable.addArmorPiece(player.getInventory().getItemInMainHand())){
            player.getInventory().remove(player.getInventory().getItemInMainHand());
            player.getInventory().setItemInMainHand(worn);
        }
    }

     */
}
