package deathChest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;
import utility.HelperFunctions;

import java.util.List;

public class DeathChestCommands implements TabExecutor
{
    public interface CommandStrings
    {
        String ROOT = "deathchest";
        String LIST = "list";
        String REMOVE = "remove";
        String REMOVE_ALL = "removeAll";

        List<String> FIRST_ARGUMENTS = List.of(LIST, REMOVE, REMOVE_ALL);
    }

    private final DeathChestManager dcManager;

    public DeathChestCommands(DeathChestManager dcManager) {
        this.dcManager = dcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player player) {
            if(args.length == 1) {
                if(args[0].equals(CommandStrings.LIST)) {
                    List<String> deathChestList = dcManager.getDeathChests(player.getUniqueId()).stream().map(dcManager::getDeathChestInfoForPlayer).toList();
                    if(deathChestList.isEmpty()) {
                        dcManager.sendMessage(player, "noDeathChestsAvailable");
                    }
                    else {
                        for(String s : deathChestList) {
                            dcManager.sendMessageDirect(player, s);
                        }
                    }
                }
                else if(args[0].equals(CommandStrings.REMOVE)) {
                    DeathChest dc = dcManager.getDeathChest(HelperFunctions.getTargetBlock(player));
                    if(dc != null) {
                        if(dc.checkIfOwner(player.getUniqueId()) || dcManager.hasAdminPermission(player)) {
                            dcManager.removeDeathChest(dc, false, true);
                        }
                    }
                    else {
                        dcManager.sendMessage(player, "noDeathChest");
                    }
                }
                else if(args[0].equals(CommandStrings.REMOVE_ALL)) {
                    for(DeathChest dc : dcManager.getDeathChests(player.getUniqueId())) {
                        dcManager.removeDeathChest(dc, false, true);
                    }
                }
                return true;
            }
            else {
                dcManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
            }
        }
        else {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.getMessage());
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
