package me.itsnutt.guardmobs.Mobs;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.itsnutt.guardmobs.Data.GuardMobProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public interface GuardMob {

    enum CustomEntityType{
        ARCHER,
        SWORDSMAN,
        BEAR,
        TITAN,
        MAGE,
        CONJURER,
        SAINT
    }

    enum MovementSetting{
        STAY_AT_SPAWN,
        FOLLOW,
        WANDER
    }

    boolean getTargetNonTeamPlayers();

    boolean getTargetHostileMobs();

    CustomEntityType getEntityType();

    String getRegionID();

    ProtectedRegion getRegion();

    Location getLocation();

    BlockPos getSpawnBlockPos();

    Location getSpawnLocation();

    void reevaluateTarget();

    int getTier();

    GuardMobProfile getProfile();

    Inventory getInventory();

    Entity getEntity();

    MovementSetting getMovementSetting();

    void setMovementSetting(MovementSetting movementSetting);

    Player getFollowing();

    void setFollowing(Player player);
}
