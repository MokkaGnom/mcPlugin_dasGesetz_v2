package commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.util.List;

public class Coords implements TabExecutor
{
    public static final String COMMAND = "coords";

    public String getMessageString(String message) {
        return ChatColor.GRAY + "[" + ChatColor.YELLOW + "Coords" + ChatColor.GRAY + "] " + ChatColor.WHITE + message;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.getMessage());
            return true;
        }

        if(!CommandsManager.getInstance().hasDefaultUsePermission(sender, this.getClass())) {
            CommandsManager.getInstance().sendMessage(player, ErrorMessage.NO_PERMISSION);
            return true;
        }

        if(args.length == 1) {
            Player p = Bukkit.getPlayer(args[0]);
            if(p == null) {
                CommandsManager.getInstance().sendMessageFormat(player, "playerNotFound", args[0]);
            }
            else {
                CommandsManager.getInstance().sendMessageFormat(player, "coords", p.getName(), p.getLocation().toString());
            }
            return true;
        }
        else {
            CommandsManager.getInstance().sendMessage(player, ErrorMessage.UNKNOWN_SYNTAX);
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return null;
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}
