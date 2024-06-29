package manager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManagerCommands implements TabExecutor, ManagedPlugin
{
    public static final String COMMAND = "dgManager";

    private static final List<String> PLUGIN_DISABLE_STRINGS = List.of("0", "disable", "false");
    private static final List<String> PLUGIN_ENABLE_STRINGS = List.of("1", "enable", "true");
    private static final String PLUGIN_ENABLED = "Plugin: \"%s\" wurde aktiviert";
    private static final String PLUGIN_DISABLED = "Plugin: \"%s\" wurde deaktiviert";

    public ManagerCommands ()
    {
    }

    @Override
    public boolean onCommand (CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 2)
        {
            List<ManagedPlugin> foundPluginList = Manager.getInstance().getPlugins().keySet().stream()
                    .filter(plugin -> plugin.getName().equals(args[0])).toList();

            if (foundPluginList.isEmpty())
            {
                sender.sendMessage(OutputStrings.ERROR.UNKNOWN_PLUGIN);
                return false;
            }

            ManagedPlugin plugin = foundPluginList.getFirst();

            if (!PLUGIN_ENABLE_STRINGS.stream().filter(string -> string.contains(args[1].toLowerCase())).toList().isEmpty())
            {
                Manager.getInstance().enablePlugin(plugin);
                sender.sendMessage(String.format(PLUGIN_ENABLED, plugin.getName()));

            } else
            {
                Manager.getInstance().disablePlugin(plugin);
                sender.sendMessage(String.format(PLUGIN_DISABLED, plugin.getName()));
            }
            return true;
        } else
        {
            sender.sendMessage(OutputStrings.ERROR.UNKNOWN_SYNTAX);
            return false;
        }
    }

    @Override
    public List<String> onTabComplete (CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 1)
        {
            return Manager.getInstance().getPlugins().keySet().stream().map(ManagedPlugin::getName).collect(Collectors.toList());
        } else if (args.length == 2)
        {
            return Stream.concat(PLUGIN_DISABLE_STRINGS.stream(), PLUGIN_ENABLE_STRINGS.stream()).collect(Collectors.toList());
        }
        return OutputStrings.COMMAND_NO_OPTION_AVAILABLE;
    }

    @Override
    public boolean onEnable ()
    {
        try
        {
            Manager.getInstance().getCommand(COMMAND).setExecutor(this);
            Manager.getInstance().getCommand(COMMAND).setTabCompleter(this);
            return true;
        } catch (NullPointerException e)
        {
            Manager.getInstance().sendErrorMessage(e.getMessage());
            return false;
        }
    }

    @Override
    public void onDisable ()
    {
    }

    @Override
    public String getName ()
    {
        return "Manager";
    }
}
