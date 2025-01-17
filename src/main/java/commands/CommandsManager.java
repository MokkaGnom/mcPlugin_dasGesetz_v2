package commands;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.command.TabExecutor;
import org.bukkit.permissions.Permissible;

import java.util.List;
import java.util.Map;

public class CommandsManager implements ManagedPlugin
{
    private final Map<String, TabExecutor> executors = Map.of(
            Coords.COMMAND, new Coords(),
            WeatherClear.COMMAND, new WeatherClear(),
            DasGesetz.COMMAND, new DasGesetz(),
            GetPing.COMMAND, new GetPing()
    );

    private static CommandsManager instance;

    public static CommandsManager getInstance() {
        return instance;
    }

    public CommandsManager() {
        instance = this;
    }

    public Map<String, TabExecutor> getExecutors() {
        return executors;
    }

    @Override
    public boolean hasDefaultUsePermission(Permissible permissible) {
        return false;
    }

    @Override
    public boolean hasDefaultUsePermission(Permissible permissible, Class<?> supervisedClass) {
        if(supervisedClass == Coords.class) {
            return permissible.hasPermission("dg.coordsPermission");
        }
        else if(supervisedClass == DasGesetz.class) {
            return permissible.hasPermission("dg.dasGesetzPermission");
        }
        else if(supervisedClass == WeatherClear.class) {
            return permissible.hasPermission("dg.weatherClearPermission");
        }
        else if(supervisedClass == GetPing.class) {
            return permissible.hasPermission("dg.getPingPermission");
        }
        return hasDefaultUsePermission(permissible);
    }

    @Override
    public boolean onEnable() {
        for(Map.Entry<String, TabExecutor> entry : executors.entrySet()) {
            try {
                Manager.getInstance().getCommand(entry.getKey()).setExecutor(entry.getValue());
                Manager.getInstance().getCommand(entry.getKey()).setTabCompleter(entry.getValue());
            } catch(NullPointerException e) {
                Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDisable() {
        for(Map.Entry<String, TabExecutor> entry : executors.entrySet()) {
            try {
                Manager.getInstance().getCommand(entry.getKey()).setExecutor(null);
            } catch(NullPointerException e) {
                Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            }
        }
    }

    @Override
    public String getName() {
        return "Commands";
    }

    @Override
    public List<String> getPermissions() {
        return List.of("");
    }

    @Override
    public ChatColor getMessageColor() {
        return ManagedPlugin.DEFAULT_CHAT_COLOR;
    }
}
