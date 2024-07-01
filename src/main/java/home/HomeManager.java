package home;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;
import utility.ErrorMessage;

import java.io.*;
import java.util.*;

import static home.HomeConstants.*;

public class HomeManager implements ManagedPlugin
{
    private static final String MAX_HOMES_JSON_KEY = "MaxHomes";
    private static final String MAX_CREATE_DISTANCE_JSON_KEY = "MaxCreateDistance";

    public final int MAX_HOMES = Manager.getInstance().getConfig().getInt(Manager.getInstance().getConfigEntryPath(getName(), MAX_HOMES_JSON_KEY));
    public final int MAX_BLOCK_DISTANCE = Manager.getInstance().getConfig().getInt(Manager.getInstance().getConfigEntryPath(getName(), MAX_CREATE_DISTANCE_JSON_KEY));
    private final File SAVE_FILE = new File(Manager.getInstance().getDataFolder(), getName() + ".yml");
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();
    private final FileConfiguration saveConfigFile = YamlConfiguration.loadConfiguration(SAVE_FILE);

    public HomeManager() {
    }

    private String getMessageString(String message) {
        return ChatColor.GRAY + "[" + ChatColor.BLUE + getName() + ChatColor.GRAY + "] " + ChatColor.WHITE + message;
    }

    /**
     * Damit man beim teleportieren mittig auf dem Block landet
     * @param loc Der ursprungs Ort
     * @return Der neue Ort
     */
    private Location getTPLocation(Location loc) {
        return loc.add(
                loc.getX() % 1 == 0.0d ? 0.5d : 0.0d,
                1.0d,
                loc.getZ() % 1 == 0.0d ? 0.5d : 0.0d
        );
    }

    public boolean sendMessage(CommandSender sender, List<String> messages) {
        if(sender != null) {
            for(String message : messages) {
                sender.sendMessage(getMessageString(message));
            }
            return true;
        }
        return false;
    }

    public boolean sendMessage(CommandSender sender, String message) {
        if(sender != null) {
            sender.sendMessage(getMessageString(message));
            return true;
        }
        return false;
    }

    public boolean sendErrorMessage(CommandSender sender, String message) {
        if(sender != null) {
            sender.sendMessage(getMessageString(message));
            Manager.getInstance().sendWarningMessage(getMessagePrefix(), message);
            return true;
        }
        return false;
    }

    public ErrorMessage addHome(UUID playerUUID, String homeName, Location location) {
        if(!homes.containsKey(playerUUID)) {
            homes.put(playerUUID, new HashMap<>());
        }
        Map<String, Location> homeMap = homes.get(playerUUID);
        if(homeMap.containsKey(homeName)) {
            return new ErrorMessage(String.format(ERROR_HOME_EXISTS, homeName));
        }
        else if(homeMap.size() >= MAX_HOMES) {
            return new ErrorMessage(ERROR_MAX_HOME_COUNT);
        }
        else {
            homeMap.put(homeName, getTPLocation(location));
            return ErrorMessage.NO_ERROR;
        }
    }

    public ErrorMessage removeHome(UUID playerUUID, String homeName) {
        Map<String, Location> homeMap = homes.get(playerUUID);
        if(homeMap != null) {
            homeMap.remove(homeName);
            return ErrorMessage.NO_ERROR;
        }
        else {
            return new ErrorMessage(String.format(ERROR_HOME_NOT_FOUND, homeName));
        }
    }

    public List<String> getAllHomeNames(UUID playerUUID) {
        Map<String, Location> playerHomes = homes.get(playerUUID);
        return (playerHomes != null ? new ArrayList<>(playerHomes.keySet()) : ErrorMessage.COMMAND_NO_OPTION_AVAILABLE);
    }

    public ErrorMessage teleportToHome(UUID playerUUID, String homeName) {
        Map<String, Location> homeMap = homes.get(playerUUID);
        if(homeMap != null && homeMap.containsKey(homeName)) {
            Manager.getInstance().getServer().getPlayer(playerUUID).teleport(homeMap.get(homeName));
            return ErrorMessage.NO_ERROR;
        }
        else {
            return new ErrorMessage(String.format(ERROR_HOME_NOT_FOUND, homeName));
        }
    }

    public boolean saveToFile() {
        for(Map.Entry<UUID, Map<String, Location>> entry : homes.entrySet()) {
            String uuid = entry.getKey().toString();
            Map<String, Location> homeMap = entry.getValue();
            if(homeMap == null || homeMap.isEmpty())
                continue;

            ConfigurationSection uuidSection = saveConfigFile.createSection(uuid);
            for(Map.Entry<String, Location> home : homeMap.entrySet()) {
                uuidSection.set(home.getKey(), home.getValue());
            }
        }
        try {
            saveConfigFile.save(SAVE_FILE);
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(HOMES_SAVED, homes.keySet().size(), homes.entrySet().size()));
            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
    }

    public boolean loadFromFile() {
        try {
            saveConfigFile.load(SAVE_FILE);
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
        ConfigurationSection uuidSection = saveConfigFile.getConfigurationSection("");
        Set<String> uuids = uuidSection.getKeys(false);
        for(String uuid : uuids) {
            Map<String, Location> homeMap = new HashMap<>();
            Set<String> homeNames = uuidSection.getConfigurationSection(uuid).getKeys(false);
            for(String name : homeNames) {
                homeMap.put(name, saveConfigFile.getLocation(Manager.getConfigEntryPath(uuid, name)));
            }
            homes.put(UUID.fromString(uuid), homeMap);
        }
        Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(HOMES_LOADED, homes.keySet().size(), homes.entrySet().size()));
        return true;
    }

    @Override
    public boolean onEnable() {
        loadFromFile();
        try {
            HomeCommands hc = new HomeCommands(this);
            Manager.getInstance().getCommand(HomeCommands.CommandStrings.ROOT).setExecutor(hc);
            Manager.getInstance().getCommand(HomeCommands.CommandStrings.ROOT).setTabCompleter(hc);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(e.getMessage());
            onDisable();
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        saveToFile();
    }

    @Override
    public String getName() {
        return "Homes";
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(Manager.getInstance().getConfigEntryPath(getName(), MAX_HOMES_JSON_KEY), 5);
        config.addDefault(Manager.getInstance().getConfigEntryPath(getName(), MAX_CREATE_DISTANCE_JSON_KEY), 10);
    }
}
