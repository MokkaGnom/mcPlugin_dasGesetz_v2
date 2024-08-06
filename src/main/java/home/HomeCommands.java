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
                homeManager.sendMessage(sender, homeNames.isEmpty() ? List.of(HomeManager.HomeConstants.NO_HOMES_FOUND) : homeNames);
                return true;
            }
            else if(args.length == 2) {
                if(args[0].equalsIgnoreCase(CommandStrings.ADD)) {
                    ErrorMessage message = homeManager.addHome(player.getUniqueId(), args[1], player.getLocation().getBlock().getLocation());
                    homeManager.sendMessage(sender, message != ErrorMessage.NO_ERROR ? message.message() : String.format(HomeManager.HomeConstants.HOME_ADDED, args[1]));
                    return true;
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
                    ErrorMessage message = homeManager.removeHome(player.getUniqueId(), args[1]);
                    homeManager.sendMessage(sender, message != ErrorMessage.NO_ERROR ? message.message() : String.format(HomeManager.HomeConstants.HOME_REMOVED, args[1]));
                    return true;
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.TP)) {
                    ErrorMessage message = homeManager.teleportToHome(player.getUniqueId(), args[1]);
                    homeManager.sendMessage(sender, message != ErrorMessage.NO_ERROR ? message.message() : String.format(HomeManager.HomeConstants.HOME_TELEPORTED, args[1]));
                    return true;
                }
            }
        }
        else {
            homeManager.sendMessage(sender, ErrorMessage.NOT_A_PLAYER.message());
            return false;
        }
        homeManager.sendMessage(sender, ErrorMessage.UNKNOWN_SYNTAX.message());
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
            else {
                homeManager.sendMessage(sender, ErrorMessage.NOT_A_PLAYER.message());
            }
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}