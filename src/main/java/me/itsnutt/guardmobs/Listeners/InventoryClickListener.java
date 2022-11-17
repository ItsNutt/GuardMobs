package me.itsnutt.guardmobs.Listeners;

import me.itsnutt.guardmobs.Data.GuardMobData;
import me.itsnutt.guardmobs.GuardMobs;
import me.itsnutt.guardmobs.Mobs.Armorable;
import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import net.minecraft.world.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryInteract(InventoryClickEvent event){
        Inventory inventory = event.getClickedInventory();
        Player player = (Player) event.getWhoClicked();
        InventoryHolder inventoryHolder = inventory.getHolder();
        ItemStack itemStack = event.getCurrentItem();
        Economy economy = GuardMobs.getEconomy();

        if (!(inventoryHolder instanceof GuardMob guardMob))return;
        Integer[] normalSlots = {8,17,26};
        Integer[] armorableSlots = {0,9,18,27};
        if (Arrays.stream(normalSlots).toList().contains(event.getSlot())){
            event.setCancelled(true);
        }

        if (guardMob instanceof Armorable){
            if (Arrays.stream(armorableSlots).toList().contains(event.getSlot())){
                return;
            }
        }

        if (itemStack == null)return;

        if (!(itemStack.hasItemMeta()))return;

        if (itemStack.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.GOLD + "Revert To Egg")) {

            Entity entity = guardMob.getEntity();

            net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(Util.getSpawnItem(guardMob.getEntityType(), guardMob.getTier()));
            CompoundTag compoundTag = (nmsItem.hasTag() ? nmsItem.getTag() : new CompoundTag());
            compoundTag.putByteArray("guardMobID", guardMob.getProfile().uuidToByteArray());
            nmsItem.setTag(compoundTag);

            GuardMobData.saveGuardMobInventory(guardMob);
            player.getInventory().addItem(CraftItemStack.asBukkitCopy(nmsItem));
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
            Util.spawnGuardMob(guardMob.getEntityType(), guardMob.getSpawnLocation(), guardMob.getRegionID(), guardMob.getTier()+1, guardMob.getGuardMobID());
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

    @EventHandler
    public void onInventoryArmorSlotInteract(InventoryClickEvent event){
        ItemStack onCursor = event.getCursor();
        Integer[] armorableSlots = {0,9,18,27};
        Inventory inventory = event.getClickedInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();
        int slot = event.getSlot();

        if (!(inventoryHolder instanceof GuardMob guardMob))return;
        if (!(guardMob instanceof Armorable armorable))return;

        if (!Arrays.stream(armorableSlots).toList().contains(slot)){
            return;
        }
        event.setCancelled(true);

        if (onCursor == null){
            return;
        }

        if (!Util.isArmor(onCursor)){
            if (onCursor.getType() != Material.AIR){
                return;
            }
            switch (slot){
                case 0 -> {
                    if (armorable.getHead().getType() == Material.GLASS){
                        return;
                    }
                    event.setCursor(armorable.getHead());
                    inventory.setItem(0, null);
                }
                case 9 -> {
                    event.setCursor(armorable.getChest());
                    inventory.setItem(9, null);
                }
                case 18 -> {
                    event.setCursor(armorable.getLegs());
                    inventory.setItem(18, null);
                }
                case 27 -> {
                    event.setCursor(armorable.getFeet());
                    inventory.setItem(27, null);
                }
                default -> {
                    return;
                }
            }
            Util.prepareNullArmor(inventory);
            return;
        }

        ItemStack oldArmor;
        switch (slot){
            case 0 -> {
                if (!Util.isHelmet(onCursor)){
                    return;
                }
                oldArmor = armorable.getHead();
            }
            case 9 -> {
                if (!Util.isChestPlate(onCursor)){
                    return;
                }
                oldArmor = armorable.getChest();
            }
            case 18 -> {
                if (!Util.isLeggings(onCursor)){
                    return;
                }
                oldArmor = armorable.getLegs();
            }
            case 27 -> {
                if (!Util.isBoots(onCursor)){
                    return;
                }
                oldArmor = armorable.getFeet();
            }
            default -> {
                return;
            }
        }
        if (oldArmor.getType() == Material.GLASS || oldArmor.getType() == Material.BARRIER){
            oldArmor = null;
        }
        armorable.addArmorPiece(onCursor);
        event.setCursor(oldArmor);
    }

    @EventHandler
    public void onInventoryTotemSlotInteract(InventoryClickEvent event){
        ItemStack onCursor = event.getCursor();
        ItemStack slotOne = event.getCurrentItem();
        Inventory inventory = event.getClickedInventory();
        InventoryHolder inventoryHolder = inventory.getHolder();

        if (!(inventoryHolder instanceof GuardMob guardMob))return;
        if (!(guardMob instanceof Armorable armorable))return;

        if (event.getSlot() != 1)return;
        event.setCancelled(true);

        if (slotOne.getType() != Material.TOTEM_OF_UNDYING && onCursor.getType() != Material.TOTEM_OF_UNDYING)return;

        if (onCursor.getType() == Material.AIR && slotOne.getType() == Material.TOTEM_OF_UNDYING){
            ItemStack barrier = Util.setItemDisplayName(new ItemStack(Material.BARRIER), ChatColor.DARK_RED + "Totem of Undying Slot");

            event.setCursor(guardMob.getInventory().getItem(1));
            guardMob.getInventory().setItem(1, barrier);
            return;
        }

        if (onCursor.getType() == Material.TOTEM_OF_UNDYING && slotOne.getType() == Material.BARRIER){
            guardMob.getInventory().setItem(1, onCursor);
            event.setCursor(null);
        }
    }
}
