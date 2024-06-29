package commands;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public class WeatherClear implements TabExecutor
{
	public static final String COMMAND = "weatherClear";

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "weather clear");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
	{
		return Arrays.asList("");
	}
}
