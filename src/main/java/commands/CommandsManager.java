package commands;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.command.TabExecutor;

import java.util.Map;

public class CommandsManager implements ManagedPlugin
{
    private final Map<String, TabExecutor> executors = Map.of(
            Coords.COMMAND, new Coords(),
            WeatherClear.COMMAND, new WeatherClear(),
            DasGesetz.COMMAND, new DasGesetz()
    );

    public CommandsManager() {
    }

    public Map<String, TabExecutor> getExecutors() {
        return executors;
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
}
