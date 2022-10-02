package me.itsnutt.guardmobs.Command;

import me.itsnutt.guardmobs.Util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GetSpawnerItemTabCompleter implements TabCompleter {

    private static final List<String> entityTypes = Util.getAllGuardMobTypesString();
    private static final List<String> tiers = Arrays.asList("1", "2", "3", "4", "5");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1){
            return StringUtil.copyPartialMatches(args[0], entityTypes, new ArrayList<>());
        }

        if (args.length == 2){
            return StringUtil.copyPartialMatches(args[1], tiers, new ArrayList<>());
        }
        return null;
    }
}
