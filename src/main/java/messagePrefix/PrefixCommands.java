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

import java.util.Arrays;
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

    public static final String PREFIX_NOT_FOUND = "Prefix nicht gefunden";

    private final PrefixManager prefixManager;

    public PrefixCommands(PrefixManager prefixManager) {
        this.prefixManager = prefixManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            prefixManager.sendMessage(sender, ErrorMessage.NOT_A_PLAYER.message());
            return false;
        }

        if(args.length == 1) {
            if(args[0].equalsIgnoreCase(CommandStrings.LIST)) {
                prefixManager.sendMessage(sender, prefixManager.getPrefixesAsPreviewStrings(player));
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
                if(prefixManager.removePrefixFromPlayer(player)) {
                    prefixManager.sendMessage(sender, "Prefix entfernt");
                }
                else {
                    prefixManager.sendMessage(sender, PREFIX_NOT_FOUND);
                }
                return true;
            }
        }
        else if(args.length == 2) {
            if(args[0].equalsIgnoreCase(CommandStrings.ADD)) {
                if(prefixManager.addPrefixToPlayer(player, args[1])) {
                    prefixManager.sendMessage(sender, "Prefix hinzugefügt");
                }
                else {
                    prefixManager.sendMessage(sender, PREFIX_NOT_FOUND);
                }
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.DELETE) && prefixManager.hasAdminPermission(player)) {
                if(prefixManager.removePrefix(args[1])) {
                    prefixManager.sendMessage(sender, "Prefix gelöscht");
                }
                else {
                    prefixManager.sendMessage(sender, PREFIX_NOT_FOUND);
                }
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.FORCE_REMOVE) && prefixManager.hasAdminPermission(player)) {
                Player p = Bukkit.getPlayer(args[1]);
                if(p != null) {
                    prefixManager.removePrefixFromPlayer(p);
                    prefixManager.sendMessage(sender, "Removed prefix from " + p.getName());
                }
                else {
                    prefixManager.sendMessage(sender, ErrorMessage.PLAYER_NOT_FOUND.message());
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
                }
                else {
                    prefixManager.sendMessage(sender, "Prefix or Player not found");
                }
                return true;
            }
        }
        else if(args.length == 5) {
            if(args[0].equalsIgnoreCase(CommandStrings.CREATE)) {
                if(prefixManager.hasAdminPermission(player)) {
                    if(prefixManager.addNewPrefix(args[1], args[2], args[3], HelperFunctions.isArgumentTrue(args[4]))) {
                        prefixManager.sendMessage(sender, "Prefix erstellt");
                    }
                    else {
                        prefixManager.sendMessage(sender, PREFIX_NOT_FOUND);
                    }
                }
                else {
                    prefixManager.sendMessage(sender, ErrorMessage.NO_PERMISSION.message());
                }
                return true;
            }
        }
        prefixManager.sendMessage(sender, ErrorMessage.UNKNOWN_SYNTAX.message());
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            return List.of(ErrorMessage.NOT_A_PLAYER.message());
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
                return Arrays.stream(ChatColor.values()).map(ChatColor::name).toList();
            }
        }
        else if(args.length == 4) {
            if(args[0].equalsIgnoreCase(CommandStrings.CREATE)) {
                return Arrays.stream(ChatColor.values()).map(ChatColor::name).toList();
            }
        }
        else if(args.length == 5) {
            return Stream.concat(ManagedPlugin.ENABLE_STRINGS.stream(), ManagedPlugin.DISABLE_STRINGS.stream()).toList();
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}
