package manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;
import utility.HelperFunctions;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManagerCommands implements TabExecutor, ManagedPlugin
{
    public interface CommandStrings
    {
        String ROOT = "dgManager";
        String MANAGE = "Manage";
        String PERMISSION = "Permission";
        String SET_CONFIG = "SetConfig";
        String GET_CONFIG = "GetConfig";

        List<String> FIRST_ARGUMENT = List.of(MANAGE, PERMISSION, SET_CONFIG, GET_CONFIG);
        List<String> ENABLE_DISABLE_STRINGS = Stream.concat(DISABLE_STRINGS.stream(), ENABLE_STRINGS.stream()).collect(Collectors.toList());
    }

    public ManagerCommands() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sendMessage(sender, ErrorMessage.NOT_A_PLAYER.message());
            return false;
        }

        if(args.length == 1) {
            sendMessage(sender, ErrorMessage.UNKNOWN_ARGUMENT.message());
            return false;
        }
        else if(args.length == 2) {
            if(args[0].equalsIgnoreCase(CommandStrings.GET_CONFIG)) {
                sendMessage(sender, Manager.getInstance().getConfigEntry(args[1]).toString());
                return true;
            }
            else {
                sendMessage(sender, ErrorMessage.UNKNOWN_ARGUMENT.message());
                return false;
            }
        }
        else if(args.length == 3) {
            if(args[0].equalsIgnoreCase(CommandStrings.MANAGE)) {
                ManagedPlugin plugin = Manager.getInstance().getSubPlugins().keySet().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(args[1]))
                        .findFirst().orElse(null);
                if(plugin != null) {
                    if(HelperFunctions.isArgumentTrue(args[2])) {
                        Manager.getInstance().enablePlugin(plugin);
                        sendMessage(sender, String.format("Plugin: \"%s\" wurde aktiviert", plugin.getName()));
                    }
                    else {
                        Manager.getInstance().disablePlugin(plugin);
                        sendMessage(sender, String.format("Plugin: \"%s\" wurde deaktiviert", plugin.getName()));
                    }
                }
                else {
                    sendMessage(sender, "Unknown Plugin!");
                }
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.SET_CONFIG)) {
                sendMessage(sender, (
                        Manager.getInstance().setConfigEntry(args[1], args[2]) ?
                                "Successful!" : "Path not found!"
                ));
                return true;
            }
            else {
                sendMessage(sender, ErrorMessage.UNKNOWN_ARGUMENT.message());
                return false;
            }
        }
        else if(args.length == 4) {
            if(args[0].equalsIgnoreCase(CommandStrings.PERMISSION)) {
                String permission = Manager.getInstance().getPermissions().stream()
                        .filter(s -> s.equalsIgnoreCase(args[1]))
                        .findFirst().orElse(null);
                if(permission != null) {
                    Player p = Bukkit.getPlayer(args[2]);
                    if(p != null) {
                        if(HelperFunctions.isArgumentTrue(args[3])) {
                            Manager.getInstance().addPermissionToUser(p, permission);
                            sendMessage(sender, String.format("Permission \"%s\" added to \"%s\"", permission, p.getName()));
                        }
                        else {
                            Manager.getInstance().removePermissionFromUser(p, permission);
                            sendMessage(sender, String.format("Permission \"%s\" removed from \"%s\"", permission, p.getName()));
                        }
                    }
                    else {
                        sendMessage(sender, "Unknown player!");
                    }
                }
                else {
                    sendMessage(sender, "Unknown permission!");
                }
                return true;
            }
            else {
                sendMessage(sender, ErrorMessage.UNKNOWN_ARGUMENT.message());
                return false;
            }
        }
        else {
            sendMessage(sender, ErrorMessage.UNKNOWN_SYNTAX.message());
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.FIRST_ARGUMENT;
        }
        else if(args.length == 2) {
            if(args[0].equalsIgnoreCase(CommandStrings.MANAGE)) {
                return Manager.getInstance().getSubPlugins().keySet().stream().map(ManagedPlugin::getName).toList();
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.PERMISSION)) {
                return Manager.getInstance().getPermissions();
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.GET_CONFIG) || args[0].equalsIgnoreCase(CommandStrings.SET_CONFIG)) {
                return Manager.getInstance().getConfig().getKeys(true).stream().toList();
            }
        }
        else if(args.length == 3) {
            if(args[0].equalsIgnoreCase(CommandStrings.PERMISSION)) {
                return null;
            }
            return CommandStrings.ENABLE_DISABLE_STRINGS;
        }
        else if(args.length == 4) {
            if(args[0].equalsIgnoreCase(CommandStrings.PERMISSION)) {
                return CommandStrings.ENABLE_DISABLE_STRINGS;
            }
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }

    @Override
    public boolean onEnable() {
        try {
            Manager.getInstance().getCommand(CommandStrings.ROOT).setExecutor(this);
            Manager.getInstance().getCommand(CommandStrings.ROOT).setTabCompleter(this);
            return true;
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
    }

    @Override
    public void onDisable() {
        try {
            Manager.getInstance().getCommand(CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(CommandStrings.ROOT).setTabCompleter(null);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.GOLD;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.dgManagerPermission");
    }

    @Override
    public String getName() {
        return "DG-Manager";
    }
}
