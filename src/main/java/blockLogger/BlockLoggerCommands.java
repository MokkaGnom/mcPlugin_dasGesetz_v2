package blockLogger;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.util.List;

public class BlockLoggerCommands implements TabExecutor
{
    public static final String ADDED_MESSAGE = "Added \"%s\": %b";
    public static final String REMOVED_MESSAGE = "Removed \"%s\": %b";
    public static final String UNKNOWN_MATERIAL = "Unknown material \"%s\"";

    public interface CommandStrings
    {
        String ROOT = "blocklogger";
        String LIST = "list";
        String ADD = "add";
        String REMOVE = "remove";

        List<String> FIRST_ARGUMENTS = List.of(LIST, ADD, REMOVE);
    }

    private final BlockLoggerManager blm;

    public BlockLoggerCommands(BlockLoggerManager blm) {
        this.blm = blm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.message());
            return true;
        }

        if(!blm.hasDefaultUsePermission(sender)) {
            sender.sendMessage(ErrorMessage.NO_PERMISSION.message());
            return true;
        }

        if(args.length == 1 && args[0].equalsIgnoreCase(CommandStrings.LIST)) {
            if(blm.getBlockLoggerMaterials().isEmpty()) {
                blm.sendMessage(sender, ErrorMessage.EMPTY_LIST.message());
            }
            else {
                for(Material material : blm.getBlockLoggerMaterials()) {
                    blm.sendMessage(sender, material.name());
                }
            }
            return true;
        }
        else if(args.length == 2) {
            Material material = Material.getMaterial(args[1].toUpperCase());
            if(material == null) {
                blm.sendMessage(sender, String.format(UNKNOWN_MATERIAL, args[1]));
                return true;
            }

            if(args[0].equalsIgnoreCase(CommandStrings.ADD)) {
                blm.sendMessage(sender, String.format(ADDED_MESSAGE, args[1], blm.addBlockLogger(material)));
                return true;
            }
            else if(args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
                blm.sendMessage(sender, String.format(REMOVED_MESSAGE, args[1], blm.removeBlockLogger(material)));
                return true;
            }
            else {
                sender.sendMessage(ErrorMessage.UNKNOWN_ARGUMENT.message());
                return false;
            }
        }
        else {
            sender.sendMessage(ErrorMessage.UNKNOWN_SYNTAX.message());
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.FIRST_ARGUMENTS;
        }
        else if(args.length == 2 && args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
            return List.copyOf(blm.getBlockLoggerMaterials().stream().map(Material::name).toList());
        }
        else {
            return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
        }
    }
}
