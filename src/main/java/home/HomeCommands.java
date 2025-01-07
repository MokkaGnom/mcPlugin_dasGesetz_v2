package home;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.util.List;

public class HomeCommands implements TabExecutor
{
    public interface CommandStrings
    {
        String ROOT = "home";
        String LIST = "list";
        String ADD = "add";
        String REMOVE = "remove";
        String TP = "tp";

        List<String> FIRST_ARGUMENT = List.of(LIST, ADD, REMOVE, TP);
    }

    private final HomeManager homeManager;

    public HomeCommands(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player player) {
            if(args.length == 1 && args[0].equalsIgnoreCase(CommandStrings.LIST)) {
                List<String> homeNames = homeManager.getAllHomeNames(player.getUniqueId());
                if(homeNames.isEmpty()) {
                    homeManager.sendMessage(player, "noHomes");
                }
                else {
                    for(String homeName : homeNames) {
                        homeManager.sendMessageDirect(player, homeName);
                    }
                }
                return true;
            }
            else if(args.length == 2) {
                if(args[0].equalsIgnoreCase(CommandStrings.ADD)) {
                    switch(homeManager.addHome(player.getUniqueId(), args[1], player.getLocation().getBlock().getLocation())) {
                        case 0 -> homeManager.sendMessage(player, "added");
                        case 1 -> homeManager.sendMessage(player, "homeExists");
                        case 2 -> homeManager.sendMessage(player, "maxHomes");
                    }
                    return true;
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
                    switch(homeManager.removeHome(player.getUniqueId(), args[1])) {
                        case 0 -> homeManager.sendMessage(player, "removed");
                        case 1 -> homeManager.sendMessage(player, "homeNotFound");
                        case 2 -> homeManager.sendMessage(player, ErrorMessage.UNKNOWN_ERROR);
                    }
                    return true;
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.TP)) {
                    switch(homeManager.teleportToHome(player.getUniqueId(), args[1])) {
                        case 0 -> homeManager.sendMessageFormat(player, "tp", args[1]);
                        case 1 -> homeManager.sendMessage(player, "homeNotFound");
                    }
                    return true;
                }
            }
        }
        else {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.getMessage());
            return false;
        }
        homeManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.FIRST_ARGUMENT;
        }
        else if(args.length == 2) {
            if(sender instanceof Player player) {
                return homeManager.getAllHomeNames(player.getUniqueId());
            }
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}