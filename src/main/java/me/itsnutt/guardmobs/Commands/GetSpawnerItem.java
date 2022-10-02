package me.itsnutt.guardmobs.Commands;

import me.itsnutt.guardmobs.Mobs.GuardMob;
import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GetSpawnerItem implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)){
            return false;
        }
        String badArgs = ChatColor.DARK_RED + "Incorrect Arguments";

        if (args.length != 2){
            player.sendMessage(badArgs);
            return true;
        }

        if (Util.parseCustomEntityType(args[0]) == null){
            player.sendMessage(badArgs);
            return true;
        }
        GuardMob.CustomEntityType customEntityType = Util.parseCustomEntityType(args[0]);
        int tier;
        try {
            tier = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex){
            player.sendMessage(badArgs);
            return true;
        }

        player.getInventory().addItem(Util.getSpawnItem(customEntityType, tier));

        return true;
    }
}
