package me.itsnutt.guardmobs.Data;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GuardMobData implements Serializable {

    @Serial
    private static transient final long serialVersionUID = -1681012206529286330L;

    public final List<Map<String, Object>> guardMobInventory;

    public GuardMobData(List<Map<String, Object>> guardMobInventory){
        this.guardMobInventory = guardMobInventory;
    }

    public GuardMobData(GuardMobData loadedData){
        this.guardMobInventory = loadedData.guardMobInventory;
    }

    public boolean saveData(String filePath){
        try{
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(new GZIPOutputStream(new FileOutputStream(filePath)));
            out.writeObject(this);
            out.close();
            return true;
        } catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    public static GuardMobData loadData(String filePath){
        try{
            BukkitObjectInputStream in = new BukkitObjectInputStream(new GZIPInputStream(new FileInputStream(filePath)));
           GuardMobData guardMobData = (GuardMobData) in.readObject();
            in.close();
            return guardMobData;
        } catch (ClassNotFoundException | IOException e){
            new GuardMobData((List<Map<String, Object>>) null).saveData(filePath);
            return null;
        }
    }

    public static Inventory getGuardMobInventory(GuardMob guardMob){
        try{
           GuardMobData guardMobData = new GuardMobData(GuardMobData.loadData("plugins/GuardMobs/GuardMobsData/"+guardMob.getGuardMobID().toString()+".data"));
            Inventory inventory = Bukkit.createInventory((InventoryHolder) guardMob, 36, ChatColor.BLACK + guardMob.getEntityType().name() + " MENU");
            List<Map<String, Object>> saved = guardMobData.guardMobInventory;
           for (int i = 0; i < saved.size(); i++){
               ItemStack itemStack = saved.get(i)==null ? null : ItemStack.deserialize(saved.get(i));
               inventory.setItem(i, itemStack);
           }
           return inventory;
        } catch (NullPointerException ignored){
            saveGuardMobInventory(guardMob);
            return Bukkit.createInventory((InventoryHolder) guardMob, 36, ChatColor.BLACK + guardMob.getEntityType().name() + " MENU");
        }
    }

    public static void saveGuardMobInventory(GuardMob guardMob){
        File file = new File("plugins/GuardMobs/GuardMobsData");
        file.mkdir();
        List<Map<String, Object>> saveThis = new ArrayList<>();
        Inventory inventory = guardMob.getInventory();
        if (inventory != null){
            for (int i = 0; i < inventory.getSize(); i++) {
                Map<String, Object> sItem = inventory.getItem(i) == null ? null : inventory.getItem(i).serialize();
                saveThis.add(sItem);
            }
        }
        new GuardMobData(saveThis).saveData("plugins/GuardMobs/GuardMobsData/"+guardMob.getGuardMobID().toString()+".data");
    }
}
