package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ChunkLoadListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event){
        Chunk chunk = event.getChunk();
        Entity[] entities = chunk.getEntities();

        for (Entity entity : entities){
            CraftEntity craftEntity = (CraftEntity) entity;
            PersistentDataContainer container = craftEntity.getPersistentDataContainer();

            if (!Util.isUninitiatedGuardMob(entity)){
                continue;
            }

            GuardMobProfile profile = GuardMobProfile.deserialize(container.get(Util.getProfileKey(), PersistentDataType.STRING), container.get(Util.getUuidKey(), PersistentDataType.BYTE_ARRAY));
            if (profile == null){
                continue;
            }
            craftEntity.getHandle().remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            profile.spawnGuardMob();
        }
    }
}
