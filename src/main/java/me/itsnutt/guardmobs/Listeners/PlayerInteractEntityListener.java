package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.Mobs.GuardMob;
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

        if (!(craftEntity.getHandle() instanceof GuardMob guardMob))return;

        player.openInventory(guardMob.getInventory());

        /*
        new BukkitRunnable() {
            @Override
            public void run() {
                Chunk chunk = ((Entity) craftEntity).getLocation().getChunk();

                final String[] string = new String[1];
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        string[0] = chunk.isLoaded() ? "Chunk is Loaded 1" : "Chunk is Not Loaded 1";
                        System.out.println(string[0]);

                        string[0] = chunk.isEntitiesLoaded() ? "Entities Are Loaded 1" : "Entities Are Not Loaded 1";
                        System.out.println(string[0]);

                        chunk.load();
                        string[0] = chunk.isLoaded() ? "Chunk is Loaded 2" : "Chunk is Not Loaded 2";
                        System.out.println(string[0]);

                        string[0] = chunk.isEntitiesLoaded() ? "Entities Are Loaded 2" : "Entities Are Not Loaded 2";
                        System.out.println(string[0]);

                        chunk.getEntities();
                        string[0] = chunk.isLoaded() ? "Chunk is Loaded 3" : "Chunk is Not Loaded 3";
                        System.out.println(string[0]);

                        string[0] = chunk.isEntitiesLoaded() ? "Entities Are Loaded 3" : "Entities Are Not Loaded 3";
                        System.out.println(string[0]);

                        System.out.println(entity.getUniqueId());
                    }
                }.runTaskLater(GuardMobs.getInstance(), 400);
                System.out.println(entity.getUniqueId());

                string[0] = chunk.isLoaded() ? "Chunk is Loaded 0" : "Chunk is Not Loaded 0";
                System.out.println(string[0]);

                string[0] = chunk.isEntitiesLoaded() ? "Entities Are Loaded 0" : "Entities Are Not Loaded 0";
                System.out.println(string[0]);


            }
        }.runTaskLater(GuardMobs.getInstance(), 10);

         */
    }
}
