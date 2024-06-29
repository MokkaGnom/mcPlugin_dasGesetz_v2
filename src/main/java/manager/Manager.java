package manager;

import blockLock.BlockLockManager;
import commands.CommandsManager;
import deathChest.DeathChestManager;
import farming.EasyFarming;
import farming.Timber;
import home.HomeManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import other.BlockLogger;
import other.Messages;
import ping.PingManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Main-Class
 */
public class Manager extends JavaPlugin
{
    private static final String JSON_PLUGIN_KEY = "Manager";
    private static final String MESSAGE_PREFIX = String.format(ManagedPlugin.MESSAGE_PREFIX, "DG-Manager");
    private static Manager instance;
    private final ManagerCommands managerCommands;

    private final Map<ManagedPlugin, Boolean> plugins = new HashMap<>();

    /**
     * NICHT BENUTZEN !!!
     * <p>Stattdessen: {@link Manager#getInstance()}</p>
     */
    public Manager ()
    {
        this.managerCommands = new ManagerCommands();
    }

    public static Manager getInstance ()
    {
        return instance;
    }

    // Benötigt für Artifact-Build
    public static void main (String[] args)
    {
        System.out.println("ERROR: MAIN CALLED!");
        Bukkit.getLogger().info("ERROR: MAIN CALLED!");
    }

    private void createDefaultConfig ()
    {
        FileConfiguration config = this.getConfig();

        for (Map.Entry<ManagedPlugin, Boolean> pluginEntry : plugins.entrySet())
        {
            pluginEntry.getKey().createDefaultConfig(config);
            try
            {
                config.addDefault(getConfigEntryPath(JSON_PLUGIN_KEY, pluginEntry.getKey().getName()), pluginEntry.getValue());
            } catch (Exception e)
            {
                Bukkit.getLogger().warning(e.getMessage());
                Bukkit.getLogger().info(getConfigEntryPath(JSON_PLUGIN_KEY, pluginEntry.getKey().getName()));
            }
        }

        config.options().copyDefaults(true);
        this.saveConfig();
    }

    @Override
    public void onEnable ()
    {
        instance = this;
        this.plugins.put(new BlockLockManager(), true);
        this.plugins.put(new CommandsManager(), true);
        this.plugins.put(new DeathChestManager(), true);
        this.plugins.put(new EasyFarming(), true);
        this.plugins.put(new Timber(), true);
        this.plugins.put(new HomeManager(), true);
        this.plugins.put(new BlockLogger(), true);
        this.plugins.put(new Messages(), true);
        this.plugins.put(new PingManager(), true);

        sendInfoMessage("Enable plugins...");

        managerCommands.onEnable();
        createDefaultConfig();

        Map<ManagedPlugin, Boolean> newPlugins = new HashMap<>();
        for (Map.Entry<ManagedPlugin, Boolean> pluginEntry : plugins.entrySet())
        {
            boolean enable = this.getConfig().getBoolean(getConfigEntryPath(JSON_PLUGIN_KEY, pluginEntry.getKey().getName()));
            newPlugins.put(pluginEntry.getKey(), enable);

            if (enable)
            {
                sendInfoMessage("Enable \"" + pluginEntry.getKey().getName() + "\"...");
                pluginEntry.getKey().onEnable();
            }
        }
        this.plugins.putAll(newPlugins);

        sendInfoMessage("All plugins enabled");
    }

    @Override
    public void onDisable ()
    {
        managerCommands.onDisable();

        for (Map.Entry<ManagedPlugin, Boolean> pluginEntry : plugins.entrySet())
        {
            if (pluginEntry.getValue())
            {
                pluginEntry.getKey().onDisable();
            }
        }
    }

    public void enablePlugin (ManagedPlugin plugin)
    {
        plugin.onEnable();
        this.getConfig().set(getConfigEntryPath(JSON_PLUGIN_KEY, plugin.getName()), true);
    }

    public void disablePlugin (ManagedPlugin plugin)
    {
        plugin.onDisable();
        this.getConfig().set(getConfigEntryPath(JSON_PLUGIN_KEY, plugin.getName()), false);
    }

    public Map<ManagedPlugin, Boolean> getPlugins ()
    {
        return plugins;
    }

    public String getConfigEntryPath (String... path)
    {
        StringBuilder finalPath = new StringBuilder();
        for (String s : path)
        {
            finalPath.append(s);
            finalPath.append(".");
        }
        return finalPath.toString().substring(0, finalPath.toString().length() - 1);
    }

    public Object getConfigEntry (String... path)
    {
        return getConfig().getString(getConfigEntryPath(path));
    }

    public Object getConfigEntry (String path)
    {
        return this.getConfig().get(path);
    }

    public void sendErrorMessage (String prefix, String message)
    {
        Bukkit.getLogger().severe(prefix + " " + message);
    }

    public void sendErrorMessage (String message)
    {
        sendErrorMessage(MESSAGE_PREFIX, message);
    }

    public void sendWarningMessage (String prefix, String message)
    {
        Bukkit.getLogger().warning(prefix + " " + message);
    }

    public void sendWarningMessage (String message)
    {
        sendWarningMessage(MESSAGE_PREFIX, message);
    }

    public void sendInfoMessage (String prefix, String message)
    {
        Bukkit.getLogger().info(prefix + " " + message);
    }

    public void sendInfoMessage (String message)
    {
        sendInfoMessage(MESSAGE_PREFIX, message);
    }
}
