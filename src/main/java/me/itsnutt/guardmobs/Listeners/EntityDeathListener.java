package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Mobs.Archer;
import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Mobs.Swordsman;
import me.itsnutt.guardmobs.Util.Util;
import net.minecraft.world.entity.monster.Monster;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Arrays;

public class EntityDeathListener implements Listener {

    @EventHandler
    public void onCreeperDeath(EntityDeathEvent event){

        if (event.getEntityType() != EntityType.CREEPER)return;

        CraftEntity craftEntity = (CraftEntity) event.getEntity();
        Monster monster = (Monster) craftEntity.getHandle();
        if (!(monster.lastHurtByMob instanceof GuardMob guardMob))return;

        if (!(guardMob instanceof Archer) && !(guardMob instanceof Swordsman))return;

        ItemStack[] musicDiscs = {new ItemStack(Material.MUSIC_DISC_13),new ItemStack(Material.MUSIC_DISC_FAR),new ItemStack(Material.MUSIC_DISC_CHIRP),
                new ItemStack(Material.MUSIC_DISC_11), new ItemStack(Material.MUSIC_DISC_BLOCKS),new ItemStack(Material.MUSIC_DISC_CAT),
                new ItemStack(Material.MUSIC_DISC_MALL), new ItemStack(Material.MUSIC_DISC_MELLOHI), new ItemStack(Material.MUSIC_DISC_STAL),
                new ItemStack(Material.MUSIC_DISC_STRAD), new ItemStack(Material.MUSIC_DISC_13),new ItemStack(Material.MUSIC_DISC_WAIT),
                new ItemStack(Material.MUSIC_DISC_WARD)};

        event.getDrops().removeIf(item -> Arrays.stream(musicDiscs).toList().contains(item));
    }

    @EventHandler
    public void onGuardMobDeath(EntityDeathEvent event){

        if (((CraftEntity)event.getEntity()).getHandle() instanceof GuardMob guardMob){
            event.getDrops().clear();
            File file = new File("plugins/GuardMobs/GuardMobsData/"+guardMob.getGuardMobID().toString()+".data");
            if (!file.delete()){
                GuardMobs.getInstance().getLogger().severe("GUARDMOB INVENTORY NOT DELETED! THIS IS A BUG! MANUALLY DELETE '" +
                        guardMob.getGuardMobID().toString() + ".data' !");
            }
            event.getDrops().add(Util.getRevivalToken(guardMob));
        }
    }
}
