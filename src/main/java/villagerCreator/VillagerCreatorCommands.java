package villagerCreator;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import utility.ErrorMessage;
import utility.HelperFunctions;

import java.util.List;

public class VillagerCreatorCommands implements TabExecutor
{
    public interface CommandStrings
    {
        String ROOT = "villagerCreator";
        String REMOVE = "remove";

        List<String> FIRST_ARGUMENT = List.of(REMOVE);
    }

    private final VillagerCreatorManager vcManager;

    public VillagerCreatorCommands(VillagerCreatorManager vcManager) {
        this.vcManager = vcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.getMessage());
            return true;
        }
        if(!vcManager.hasAdminPermission(sender)) {
            vcManager.sendMessage(player, ErrorMessage.NO_PERMISSION);
            return true;
        }

        if(args.length == 1) {
            if(args[0].equalsIgnoreCase(CommandStrings.REMOVE)) {
                Entity entity = HelperFunctions.getTargetEntity(player.getLocation(), player.getLocation().getDirection(), VillagerCreatorManager.MAX_REMOVE_DISTANCE);
                if(entity instanceof Villager villager && vcManager.removeCustomFromVillager(villager)) {
                    vcManager.sendMessage(player, "removed");
                }
                else {
                    vcManager.sendMessage(player, "invalidTarget");
                }
                return true;
            }
            else if(args[0].equalsIgnoreCase("test")) {

                return true;
            }
        }

        vcManager.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.FIRST_ARGUMENT;
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}
