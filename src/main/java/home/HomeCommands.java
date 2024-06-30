package home;

import org.bukkit.block.Block;
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

        List<String> FIRST_ARGUMENT = List.of(ROOT);
        List<String> SECOND_ARGUMENT = List.of(LIST, ADD, REMOVE, TP);
    }

    private final HomeManager homeManager;

    public HomeCommands(HomeManager homeManager) {
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player player) {
            if(args.length == 1 && args[0].equalsIgnoreCase(CommandStrings.LIST)) {
                homeManager.sendMessage(sender, homeManager.getAllHomeNames(player.getUniqueId()));
                return true;
            }
            else if(args.length == 2) {
                if(args[0].equalsIgnoreCase(CommandStrings.ADD)) {
                    Block targetBlock = player.getTargetBlockExact(homeManager.MAX_BLOCK_DISTANCE);
                    if(targetBlock != null) {
                        ErrorMessage message = homeManager.addHome(player.getUniqueId(), args[1], targetBlock.getLocation());
                        if(message != ErrorMessage.NO_ERROR) {
                            homeManager.sendErrorMessage(sender, message.getMessage());
                            return false;
                        }
                        homeManager.sendMessage(sender, String.format(HomeConstants.HOME_ADDED, args[1]));
                        return true;
                    }
                    else {
                        homeManager.sendMessage(sender, HomeConstants.ERROR_INVALID_TARGET_BLOCK);
                        return false;
                    }
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
                    ErrorMessage message = homeManager.removeHome(player.getUniqueId(), args[1]);
                    if(message != ErrorMessage.NO_ERROR) {
                        homeManager.sendErrorMessage(sender, message.getMessage());
                        return false;
                    }
                    homeManager.sendMessage(sender, String.format(HomeConstants.HOME_REMOVED, args[1]));
                    return true;
                }
                else if(args[0].equalsIgnoreCase(CommandStrings.TP)) {
                    ErrorMessage message = homeManager.teleportToHome(player.getUniqueId(), args[1]);
                    if(message != ErrorMessage.NO_ERROR) {
                        homeManager.sendErrorMessage(sender, message.getMessage());
                        return false;
                    }
                    homeManager.sendMessage(sender, String.format(HomeConstants.HOME_TELEPORTED, args[1]));
                    return true;
                }
            }
        }
        else {
            homeManager.sendErrorMessage(sender, ErrorMessage.NOT_A_PLAYER.getMessage());
            return false;
        }
        homeManager.sendMessage(sender, ErrorMessage.UNKNOWN_SYNTAX.getMessage());
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.SECOND_ARGUMENT;
        }
        else if(args.length == 2) {
            if(sender instanceof Player player) {
                return homeManager.getAllHomeNames(player.getUniqueId());
            }
            else {
                homeManager.sendErrorMessage(sender, ErrorMessage.NOT_A_PLAYER.getMessage());
            }
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}