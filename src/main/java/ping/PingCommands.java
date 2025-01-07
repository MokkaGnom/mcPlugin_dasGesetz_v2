package ping;

import java.util.List;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import utility.ErrorMessage;

public class PingCommands implements TabExecutor
{
    public static final String SUCCESSFULLY_CHANGED_COLOR = "Successfully changed color";

    public interface CommandStrings
    {
        String ROOT = "ping";
        String SET = "setColor";

        List<String> FIRST_ARGUMENTS = List.of(SET);
        List<String> SECOND_ARGUMENTS = List.of("000000", "FFB7C5", "FFFFFF");
    }

    private final PingManager pm;

    public PingCommands(PingManager pm) {
        this.pm = pm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.getMessage());
            return true;
        }

        if(!pm.hasDefaultUsePermission(player)) {
            pm.sendMessage(player, ErrorMessage.NO_PERMISSION);
            return true;
        }

        if(args.length == 0) {
            if(!pm.handlePingEvent(player)) {
                pm.sendMessageFormat(player, "onCooldown", (pm.getCooldown(player) / 1000));
            }
            return true;
        }
        else if(args.length == 2 && args[0].equalsIgnoreCase(CommandStrings.SET) && args[1].length() == 6) {
            Color color = PingManager.getColorFromHexString(args[1]);
            if(color != null) {
                pm.setPlayerColor(player, args[1]);
                pm.sendMessageFormat(player, "changed", ChatColor.of("#" + args[1]));
                return true;
            }
            pm.sendMessage(player, ErrorMessage.UNKNOWN_ARGUMENT);
        }
        else {
            pm.sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return CommandStrings.FIRST_ARGUMENTS;
        }
        else if(args.length == 2) {
            return CommandStrings.SECOND_ARGUMENTS;
        }
        else {
            return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
        }
    }

}
