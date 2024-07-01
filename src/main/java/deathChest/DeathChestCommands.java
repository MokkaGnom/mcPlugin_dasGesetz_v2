package deathChest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.util.List;

public class DeathChestCommands implements TabExecutor
{
    public interface CommandStrings
    {
        String ROOT = "deathchest";
        String LIST = "list";

        List<String> FIRST_ARGUMENTS = List.of(LIST);
    }

    private final DeathChestManager dcManager;

    public DeathChestCommands(DeathChestManager dcManager) {
        this.dcManager = dcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player player) {
            if(args.length == 1 && args[0].equals(CommandStrings.LIST)) {
                return dcManager.sendMessage(sender, dcManager.getDeathChests(player.getUniqueId()).stream().map(dcManager::getDeathChestInfoForPlayer).toList());
            }
            else
            {
                dcManager.sendMessage(sender, ErrorMessage.UNKNOWN_SYNTAX.getMessage());
            }
        }
        else{
            dcManager.sendMessage(sender, ErrorMessage.NOT_A_PLAYER.getMessage());
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.FIRST_ARGUMENTS;
        }
        else {
            return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
        }
    }
}
