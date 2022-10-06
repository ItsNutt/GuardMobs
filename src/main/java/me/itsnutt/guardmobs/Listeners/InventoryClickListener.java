package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event){
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getWhoClicked();
        InventoryHolder inventoryHolder = inventory.getHolder();
        ItemStack itemStack = event.getCurrentItem();
        Economy economy = GuardMobs.getEconomy();

        if (!(inventoryHolder instanceof GuardMob guardMob))return;
        event.setCancelled(true);

        if (itemStack == null)return;

        if (!(itemStack.hasItemMeta()))return;

        if (itemStack.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GOLD + "Revert To Egg")) {

            Entity entity = guardMob.getEntity();
            player.getInventory().addItem(Util.getSpawnItem(guardMob.getEntityType(), guardMob.getTier()));
            entity.remove(Entity.RemovalReason.DISCARDED);
            player.closeInventory();
        }

        if (itemStack.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GOLD + "Upgrade")){

            double price = Util.getUpgradePrice(event.getCurrentItem());
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
            if (economy.getBalance(offlinePlayer) < price) return;

            economy.withdrawPlayer(offlinePlayer, price);
            player.sendMessage(ChatColor.DARK_RED + "-$" + price);
            guardMob.getEntity().remove(Entity.RemovalReason.DISCARDED);
            Util.spawnGuardMob(guardMob.getEntityType(), guardMob.getSpawnLocation(), guardMob.getRegionID(), guardMob.getTier()+1);
            player.sendMessage(ChatColor.DARK_GREEN + guardMob.getEntityType().name() + " Upgraded");
            player.closeInventory();
        }

        if (itemStack.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.DARK_RED + "Stay At Spawn")){
            guardMob.setMovementSetting(GuardMob.MovementSetting.FOLLOW);
            guardMob.setFollowing(player);
            Util.prepareInventory(guardMob);
        }

        if (itemStack.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GOLD + "Follow")){
            guardMob.setMovementSetting(GuardMob.MovementSetting.STAY_AT_SPAWN);
            guardMob.setFollowing(null);
            Util.prepareInventory(guardMob);
        }
    }
}
