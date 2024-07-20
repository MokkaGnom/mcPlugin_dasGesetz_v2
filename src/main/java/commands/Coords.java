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
        if(!CommandsManager.getInstance().hasPermission(sender, this.getClass())) {
            sender.sendMessage(ErrorMessage.NO_PERMISSION.message());
            return true;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.message());
            return true;
        }

        if(args.length == 1) {
            Player p = Bukkit.getPlayer(args[0]);
            if(p == null) {
                sender.sendMessage("Spieler \"" + args[0] + "\" nicht gefunden.");
            }
            else {
                sender.sendMessage("Spieler \"" + p.getName() + "\" ist bei:" + p.getLocation().toString());
            }
            return true;
        }
        else {
            sender.sendMessage(ErrorMessage.UNKNOWN_SYNTAX.message());
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return null; // Gibt automatische ne Liste mit allen Spielern
        }
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}
