package manager;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import blockLock.*;
import commands.*;
import deathChest.*;
import farming.*;
import home.*;
import other.*;
import ping.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;

public class Manager extends JavaPlugin
{
	private final String[] plugins = { "DasGesetz", "WeatherClear", "Coords", "BlockLogger", "Timber", "DeathChest", "BlockLock", "Messages", "Home", "EasyFarming", "Ping" };
	private final String[] commands = { "dasGesetz", "weatherClear", "coords", "blockLock", "home", "ping" };
	private final TabExecutor[] commandExe;
	private final Listener[] commandListener;

	public Manager()
	{
		this.getCommand("dgManager").setExecutor(this);
		this.getCommand("dgManager").setTabCompleter(this);

		commandListener = new Listener[] { new BlockLogger(), new Timber(this.getConfig().getBoolean("Timber.BreakLeaves"), this.getConfig().getInt("Timber.BreakLeavesRadius")),
				new DeathChestManager(this, this.getConfig().getInt("DeathChest.DespawnInTicks"), this.getConfig().getBoolean("DeathChest.DespawnDropping")), new BlockLockManager(this),
				new Messages(this.getConfig().getString("Messages.Message")), new EasyFarming(),
				new PingManager(this, this.getConfig().getInt("Ping.Duration"), this.getConfig().getInt("Ping.Cooldown")) };

		commandExe = new TabExecutor[] { new dasGesetz(), new weatherClear(), new coords(), new BlockLockCommands((BlockLockManager) commandListener[3]),
				new HomeCommands(this, this.getConfig().getInt("Homes.MaxHomes")), new PingCommands((PingManager) commandListener[6]) };
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if (args.length == 2)
		{
			int index = Arrays.asList(plugins).indexOf(args[0]);
			boolean deactivate = args[1] == "0";

			if (index == -1)
			{
				sender.sendMessage("Ungültiges Plugin");
				return false;
			}

			this.getConfig().set("Manager." + plugins[index], deactivate);

			if (deactivate)
				sender.sendMessage("Plugin: \"" + plugins[index] + "\" wurde aktiviert");
			else
				sender.sendMessage("Plugin: \"" + plugins[index] + "\" wurde deaktiviert");
			return true;
		}
		else
		{
			sender.sendMessage("Ungültiger Syntax");
			return false;
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
	{
		if (args.length == 1)
		{
			return Arrays.asList(plugins);
		}
		else if (args.length == 2)
		{
			return Arrays.asList("0", "1");
		}
		return Arrays.asList("");
	}

	// When plugin is enabled / on server start
	@Override
	public void onEnable ()
	{
		createConfig();
		enableAll();
	}

	// When plugin is disabled / on server shutdown

	@Override
	public void onDisable ()
	{
		disableAll();
	}

	public void createConfig()
	{
		FileConfiguration config = this.getConfig();

		config.addDefault("DeathChest.DespawnInTicks", 12000);
		config.addDefault("DeathChest.DespawnDropping", true);
		config.addDefault("Messages.Message", "Check out the plugin: https://github.com/MokkaGnom/mcPlugin_dasGesetz");
		config.addDefault("Homes.MaxHomes", 10);
		config.addDefault("Timber.BreakLeaves", true);
		config.addDefault("Timber.BreakLeavesRadius", 4);
		config.addDefault("Ping.Duration", 5000);
		config.addDefault("Ping.Cooldown", 5000);

		for (int i = 0; i < plugins.length; i++)
		{
			config.addDefault("Manager." + plugins[i], true);
		}

		config.options().copyDefaults(true);
		this.saveConfig();
	}

	public void enableAll()
	{
		try
		{
			FileConfiguration config = this.getConfig();

			for (int i = 0; i < plugins.length; i++)
			{
				if (config.getBoolean("Manager." + plugins[i]))
				{
					switch (i)
					{
					case 0: // dasGesetz:
						this.getCommand(commands[0]).setExecutor(commandExe[0]);
						this.getCommand(commands[0]).setTabCompleter(commandExe[0]);
						break;

					case 1: // weatherClear:
						this.getCommand(commands[1]).setExecutor(commandExe[1]);
						this.getCommand(commands[0]).setTabCompleter(commandExe[1]);
						break;

					case 2: // coords:
						this.getCommand(commands[2]).setExecutor(commandExe[2]);
						this.getCommand(commands[0]).setTabCompleter(commandExe[2]);
						break;

					case 3: // blockLogger:
						this.getCommand(commands[3]).setExecutor(commandExe[3]);
						this.getServer().getPluginManager().registerEvents(commandListener[0], this);
						break;

					case 4: // Timber:
						this.getServer().getPluginManager().registerEvents(commandListener[1], this);
						break;

					case 5: // DeathChest:
						this.getServer().getPluginManager().registerEvents(commandListener[2], this);
						break;

					case 6: // BlockLock:
						this.getCommand(commands[3]).setExecutor(commandExe[3]);
						this.getCommand(commands[3]).setTabCompleter(commandExe[3]);
						this.getServer().getPluginManager().registerEvents(commandListener[3], this);
						break;

					case 7: // Messages:
						this.getServer().getPluginManager().registerEvents(commandListener[4], this);
						break;

					case 8: // Home:
						this.getCommand(commands[4]).setExecutor(commandExe[4]);
						this.getCommand(commands[4]).setTabCompleter(commandExe[4]);
						break;

					case 9: // EasyFarming:
						this.getServer().getPluginManager().registerEvents(commandListener[5], this);
						break;

					case 10: // Ping:
						this.getServer().getPluginManager().registerEvents(commandListener[6], this);
						this.getCommand(commands[5]).setExecutor(commandExe[5]);
						this.getCommand(commands[5]).setTabCompleter(commandExe[5]);
						break;
					}
				}
			}

			((BlockLockManager) commandListener[3]).loadFromFile();
			((HomeCommands) commandExe[4]).loadFromFile();

			Bukkit.getConsoleSender().sendMessage("DGMANAGER: ENABLED PLUGINS");
		}
		catch (Exception e)
		{
			Bukkit.getConsoleSender().sendMessage("DGMANAGER ERROR: " + e.getLocalizedMessage());
		}
	}

	public void disableAll()
	{
		try
		{
			for (int i = 0; i < commandExe.length; i++)
			{
				this.getCommand(commands[i]).setExecutor(null);
				this.getCommand(commands[i]).setTabCompleter(null);
			}
			for (int i = 0; i < commandListener.length; i++)
			{
				HandlerList.unregisterAll(commandListener[i]);
			}

			((BlockLogger) commandListener[0]).deleteLogs();
			((DeathChestManager) commandListener[2]).removeAllDeathChests();
			((BlockLockManager) commandListener[3]).saveToFile();
			((HomeCommands) commandExe[4]).saveToFile();
			Bukkit.getConsoleSender().sendMessage("DGMANAGER: DISABLED PLUGINS");
		}
		catch (Exception e)
		{
			Bukkit.getConsoleSender().sendMessage("DGMANAGER ERROR: " + e.getLocalizedMessage());
		}
	}
}
