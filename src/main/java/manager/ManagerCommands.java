package manager;

import manager.performance.PerformanceTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import playerTrophy.PlayerTrophyManager;
import utility.ErrorMessage;
import utility.HelperFunctions;

import java.util.ArrayList;
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
        String PERFORMANCE = "Performance";

        List<String> FIRST_ARGUMENT = List.of(MANAGE, PERMISSION, PERFORMANCE);
        List<String> SECOND_ARGUMENT = null;
        List<String> THIRD_ARGUMENT = Stream.concat(DISABLE_STRINGS.stream(), ENABLE_STRINGS.stream()).collect(Collectors.toList());
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
            if(args[0].equalsIgnoreCase(CommandStrings.PERFORMANCE)) {
                sendMessage(sender, PerformanceTracker.INSTANCE.getObjectCountAsStringOutput(PerformanceTracker.INSTANCE.getPerformancePlugins(Manager.getInstance().getSubPlugins().keySet())));
                return true;
            }
            else if(args[0].equalsIgnoreCase("Test")) {
                ((PlayerTrophyManager) Manager.getInstance().getSubPlugin(PlayerTrophyManager.class)).onPlayerDeath(new PlayerDeathEvent(player, DamageSource.builder(DamageType.PLAYER_ATTACK).withCausingEntity(player).build(), new ArrayList<ItemStack>(), 0, "test"));
                sendMessage(sender, "Test-Event triggered!");
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
            else if(args[0].equalsIgnoreCase(CommandStrings.PERMISSION)) {
                String permission = Manager.getInstance().getPermissions().stream()
                        .filter(s -> s.equalsIgnoreCase(args[1]))
                        .findFirst().orElse(null);
                if(permission != null) {
                    if(HelperFunctions.isArgumentTrue(args[2])) {
                        Manager.getInstance().addPermissionToUser(player, permission);
                        sendMessage(sender, String.format("Permission \"%s\" added to \"%s\"", permission, player.getName()));
                    }
                    else {
                        Manager.getInstance().removePermissionFromUser(player, permission);
                        sendMessage(sender, String.format("Permission \"%s\" removed from \"%s\"", permission, player.getName()));
                    }
                }
                else {
                    sendMessage(sender, "Unknown Permission!");
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
        }
        else if(args.length == 3) {
            return CommandStrings.THIRD_ARGUMENT;
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
