package commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.util.List;

public class WeatherClear implements TabExecutor
{
    public static final String COMMAND = "weatherClear";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!CommandsManager.getInstance().hasDefaultUsePermission(sender, this.getClass())) {
            sender.sendMessage(ErrorMessage.NO_PERMISSION.message());
            return true;
        }

        if(!(sender instanceof Player)) {
            sender.sendMessage(ErrorMessage.NOT_A_PLAYER.message());
            return true;
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "weather clear");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return ErrorMessage.COMMAND_NO_OPTION_AVAILABLE;
    }
}
