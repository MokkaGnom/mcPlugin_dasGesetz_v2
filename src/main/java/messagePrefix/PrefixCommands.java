package messagePrefix;

import manager.ManagedPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;
import utility.HelperFunctions;

import java.util.List;
import java.util.stream.Stream;

public class PrefixCommands implements TabExecutor
{
    public interface CommandStrings
    {
        String ROOT = "prefix";
        String ADD = "add";
        String REMOVE = "remove";
        String CREATE = "create";
        String DELETE = "delete";
        String LIST = "list";
        String FORCE_ADD = "forceAdd";
        String FORCE_REMOVE = "forceRemove";

        List<String> FIRST_ARGUMENT = List.of(ADD, REMOVE, CREATE, DELETE, LIST, FORCE_ADD, FORCE_REMOVE);
        List<String> FIRST_ARGUMENT_USER = List.of(ADD, REMOVE, LIST);
    }

    private final PrefixManager prefixManager;

    public PrefixCommands(PrefixManager prefixManager) {
        this.prefixManager = prefixManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.getMessage());
            return false;
        }

        if(args.length == 1) {
            if(args[0].equalsIgnoreCase(CommandStrings.LIST)) {
                for(String s : prefixManager.getPrefixesAsPreviewStrings(player)) {
                    prefixManager.sendMessageDirect(player, s);
                }
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
                if(prefixManager.removePrefixFromPlayer(player)) {
                    prefixManager.sendMessage(player, "removed");
                }
                else {
                    prefixManager.sendMessage(player, "notFound");
                }
                return true;
            }
        }
        else if(args.length == 2) {
            if(args[0].equalsIgnoreCase(CommandStrings.ADD)) {
                if(prefixManager.addPrefixToPlayer(player, args[1])) {
                    prefixManager.sendMessage(player, "added");
                }
                else {
                    prefixManager.sendMessage(player, "notFound");
                }
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.DELETE) && prefixManager.hasAdminPermission(player)) {
                if(prefixManager.removePrefix(args[1])) {
                    prefixManager.sendMessage(player, "deleted");
                }
                else {
                    prefixManager.sendMessage(player, "notFound");
                }
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.FORCE_REMOVE) && prefixManager.hasAdminPermission(player)) {
                Player p = Bukkit.getPlayer(args[1]);
                if(p != null) {
                    prefixManager.removePrefixFromPlayer(p);
                    prefixManager.sendMessageFormat(player, "removedFrom", p.getName());
                }
                else {
                    prefixManager.sendMessage(player, ErrorMessage.PLAYER_NOT_FOUND);
                }
                return true;
            }
        }
        else if(args.length == 3) {
            if(args[0].equalsIgnoreCase(CommandStrings.FORCE_ADD) && prefixManager.hasAdminPermission(player)) {
                Player p = Bukkit.getPlayer(args[2]);
                Prefix prefix = prefixManager.getPrefix(args[1]);
                if(p != null && prefix != null) {
                    prefixManager.addPrefixToPlayer(p, prefix);
                    prefixManager.sendMessageFormat(player, "addedTo", p.getName());
                    prefixManager.sendMessageFormat(p, "addedP", prefix.prefix());
                }
                else {
                    prefixManager.sendMessage(player, "notFoundP");
                }
                return true;
            }
        }
        else if(args.length == 5) {
            if(args[0].equalsIgnoreCase(CommandStrings.CREATE)) {
                if(prefixManager.hasAdminPermission(player)) {
                    if(prefixManager.addNewPrefix(args[1], args[2], args[3], HelperFunctions.isArgumentTrue(args[4]))) {
                        prefixManager.sendMessage(player, "created");
                    }
                    else {
                        prefixManager.sendMessage(player, "notFound");
                    }
                }
                else {
                    prefixManager.sendMessage(player, ErrorMessage.NO_PERMISSION);
                }
                return true;
            }
        }
        prefixManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            return List.of();
        }

        if(args.length == 1) {
            return prefixManager.hasAdminPermission(player) ? CommandStrings.FIRST_ARGUMENT : CommandStrings.FIRST_ARGUMENT_USER;
        }
        else if(args.length == 2) {
            if(args[0].equalsIgnoreCase(CommandStrings.ADD) || args[0].equalsIgnoreCase(CommandStrings.DELETE) || args[0].equalsIgnoreCase(CommandStrings.FORCE_ADD)) {
                return prefixManager.getPrefixes(player).stream().map(Prefix::prefix).toList();
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.FORCE_REMOVE)) {
                return null;
            }
        }
        else if(args.length == 3) {
            if(args[0].equalsIgnoreCase(CommandStrings.FORCE_ADD)) {
                return null;
            }
            if(args[0].equalsIgnoreCase(CommandStrings.CREATE)) {
                return PrefixManager.AVAILABLE_COLORS.stream().map(ChatColor::name).toList();
            }
        }
        else if(args.length == 4) {
            if(args[0].equalsIgnoreCase(CommandStrings.CREATE)) {
                return PrefixManager.AVAILABLE_COLORS.stream().map(ChatColor::name).toList();
            }
        }
        else if(args.length == 5) {
            return Stream.concat(ManagedPlugin.ENABLE_STRINGS.stream(), ManagedPlugin.DISABLE_STRINGS.stream()).toList();
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}
