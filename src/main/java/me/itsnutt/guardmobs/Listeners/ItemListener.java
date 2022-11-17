package me.itsnutt.guardmobs.Listeners;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ItemListener implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEvent event){
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        Action action = event.getAction();
        Location playerLocation = player.getLocation();
        RegionManager regionManager = Util.getRegionManager(((CraftWorld) playerLocation.getWorld()).getHandle());
        ProtectedRegion protectedRegion = null;

        if (action != Action.RIGHT_CLICK_BLOCK)return;


        if (event.getHand() != EquipmentSlot.HAND)return;

        Location blockClicked = event.getClickedBlock().getLocation();

        net.minecraft.world.item.ItemStack nmsInHand = CraftItemStack.asNMSCopy(inHand);
        if (!nmsInHand.hasTag())return;


        if (!Util.isGuardMobString(nmsInHand.getTag().getString("guardMob")))return;

        event.setCancelled(true);

        boolean isOwner = false;
        ApplicableRegionSet applicableRegionSet = regionManager.getApplicableRegions(BlockVector3.at(blockClicked.getBlockX(),
                blockClicked.getBlockY(), blockClicked.getBlockZ()));
        for (ProtectedRegion region : applicableRegionSet){
            if (region.isOwner(WorldGuardPlugin.inst().wrapPlayer(player))){
                isOwner = true;
                protectedRegion = region;
                break;
            }
        }

        if (!isOwner){
            player.sendMessage(ChatColor.DARK_RED + "You Do Not Own This Region!");
            return;
        }

        String string = nmsInHand.getTag().getString("guardMob");
        String[] raw = string.split(":");
        GuardMob.CustomEntityType customEntityType = Util.parseCustomEntityType(raw[0]);
        int tier = Integer.parseInt(raw[1]);
        UUID uuid = nmsInHand.getTag().contains("guardMobID") ? GuardMobProfile.byteArrayToUUID(nmsInHand.getTag().getByteArray("guardMobID")) : null;
        Util.spawnGuardMob(customEntityType, blockClicked.add(0.5,1,0.5), protectedRegion.getId(), tier, uuid);
        player.getInventory().removeItem(inHand);

    }
}
