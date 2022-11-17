package me.itsnutt.guardmobs.Mobs;

import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface Armorable {

    void refreshArmor();

    ItemStack getHead();

    ItemStack getChest();

    ItemStack getLegs();

    ItemStack getFeet();

    boolean addArmorPiece(ItemStack itemStack);

    ItemStack get(EquipmentSlot equipmentSlot);

}
