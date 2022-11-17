package me.itsnutt.guardmobs.Data;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.nio.ByteBuffer;
import java.util.UUID;

public class GuardMobProfile {

    private final GuardMob.CustomEntityType type;
    private final Location spawnLocation;
    private final String regionID;
    private final int tier;
    private final UUID guardMobID;

    public GuardMobProfile(GuardMob.CustomEntityType customEntityType, Location spawnLocation, String regionID, Integer tier, UUID guardMobID){
        this.type = customEntityType;
        this.spawnLocation = spawnLocation;
        this.regionID = regionID;
        this.tier = tier;
        this.guardMobID = guardMobID;
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

    public byte[] uuidToByteArray(){
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(0, guardMobID.getMostSignificantBits());
        bb.putLong(1, guardMobID.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID byteArrayToUUID(byte[] bytes){
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong(0);
        long low = bb.getLong(1);
        return new UUID(high, low);
    }

    public static GuardMobProfile deserialize(String string, byte[] bytes){
        if (string == null){
            return null;
        }
        if (bytes == null){
            return null;
        }
        if (bytes.length == 0){
            return null;
        }

        String[] strings = string.split(":");
        Location location = new Location(Bukkit.getWorld(UUID.fromString(strings[1])), Double.parseDouble(strings[2]), Double.parseDouble(strings[3]),
                Double.parseDouble(strings[4]));
        return new GuardMobProfile(Util.parseCustomEntityType(strings[0]), location, strings[5], Integer.parseInt(strings[6]), byteArrayToUUID(bytes));
    }

    public void spawnGuardMob(){
        Util.spawnGuardMob(type, spawnLocation, regionID, tier, guardMobID);
    }
}
