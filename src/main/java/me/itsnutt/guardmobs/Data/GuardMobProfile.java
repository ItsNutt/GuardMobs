package me.itsnutt.guardmobs.Data;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

public class GuardMobProfile {

    private final GuardMob.CustomEntityType type;
    private final Location spawnLocation;
    private final String regionID;
    private final int tier;

    public GuardMobProfile(GuardMob.CustomEntityType customEntityType, Location spawnLocation, String regionID, Integer tier){
        this.type = customEntityType;
        this.spawnLocation = spawnLocation;
        this.regionID = regionID;
        this.tier = tier;
    }

    public GuardMob.CustomEntityType getType() {
        return type;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public int getTier() {
        return tier;
    }

    public String getRegionID(){
        return regionID;
    }

    public String serialize(){
        return type.name()+":"+spawnLocation.getWorld().getUID()+":"+spawnLocation.getX()+":"+spawnLocation.getY()+":"+
                spawnLocation.getZ()+":"+regionID+":"+tier;
    }

    public static GuardMobProfile deserialize(String string){
        if (string == null){
            return null;
        }

        String[] strings = string.split(":");
        Location location = new Location(Bukkit.getWorld(UUID.fromString(strings[1])), Double.parseDouble(strings[2]), Double.parseDouble(strings[3]),
                Double.parseDouble(strings[4]));
        return new GuardMobProfile(Util.parseCustomEntityType(strings[0]), location, strings[5], Integer.parseInt(strings[6]));
    }

    public void spawnGuardMob(){
        Util.spawnGuardMob(this.type, this.spawnLocation, this.regionID, this.tier);
    }
}
