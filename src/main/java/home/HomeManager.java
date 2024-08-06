package home;

import manager.ManagedPlugin;
import manager.Manager;
import manager.Saveable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import utility.ErrorMessage;

import java.io.File;
import java.util.*;

public class HomeManager implements ManagedPlugin, Saveable
{
    public interface HomeConstants
    {
        String ERROR_HOME_EXISTS = "Home \"%s\" already exists";
        String ERROR_HOME_NOT_FOUND = "Home \"%s\" not found";
        String ERROR_MAX_HOME_COUNT = "Reached maximum amount of homes";
        String NO_HOMES_FOUND = "No Homes found";

        String HOME_ADDED = "Home \"%s\" added";
        String HOME_REMOVED = "Home \"%s\" removed";
        String HOME_TELEPORTED = "Teleported to home \"%s\"";

        String HOMES_SAVED = "Homes saved! (P:%d H:%d)";
        String HOMES_LOADED = "Homes loaded! (P:%d H:%d)";
    }

    private static final String MAX_HOMES_JSON_KEY = "Homes.MaxHomes";

    public final int MAX_HOMES = Manager.getInstance().getConfig().getInt(MAX_HOMES_JSON_KEY);
    private final File SAVE_FILE = new File(Manager.getInstance().getDataFolder(), getName() + ".yml");
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();
    private final FileConfiguration saveConfigFile = YamlConfiguration.loadConfiguration(SAVE_FILE);

    public HomeManager() {
    }

    /**
     * Damit man beim teleportieren mittig auf dem Block landet
     *
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

    public void sendMessage(CommandSender sender, List<String> messages) {
        for(String message : messages) {
            sendMessage(sender, message);
        }
    }

    public ErrorMessage addHome(UUID playerUUID, String homeName, Location location) {
        Map<String, Location> homeMap = getHomes(playerUUID);
        if(getHomeLocation(playerUUID, homeName) != null) {
            return new ErrorMessage(String.format(HomeConstants.ERROR_HOME_EXISTS, homeName));
        }
        else if(isMaxHomesReached(playerUUID)) {
            return new ErrorMessage(HomeConstants.ERROR_MAX_HOME_COUNT);
        }
        else {
            homeMap.put(homeName.toLowerCase(), getTPLocation(location));
            return ErrorMessage.NO_ERROR;
        }
    }

    public ErrorMessage removeHome(UUID playerUUID, String homeName) {
        Map<String, Location> homeMap = getHomes(playerUUID);
        if(!homeMap.isEmpty()) {
            homeMap.remove(homeName.toLowerCase());
            return ErrorMessage.NO_ERROR;
        }
        else {
            return new ErrorMessage(String.format(HomeConstants.ERROR_HOME_NOT_FOUND, homeName));
        }
    }

    public List<String> getAllHomeNames(UUID playerUUID) {
        Map<String, Location> playerHomes = getHomes(playerUUID);
        return (playerHomes.isEmpty() ? List.of(HomeConstants.NO_HOMES_FOUND) : new ArrayList<>(playerHomes.keySet()));
    }

    public ErrorMessage teleportToHome(UUID playerUUID, String homeName) {
        Location location = getHomeLocation(playerUUID, homeName);
        Player player = Manager.getInstance().getServer().getPlayer(playerUUID);
        if(location != null && player != null) {
            player.teleport(location);
            return ErrorMessage.NO_ERROR;
        }
        else {
            return new ErrorMessage(String.format(HomeConstants.ERROR_HOME_NOT_FOUND, homeName));
        }
    }

    public boolean isMaxHomesReached(UUID playerUUID) {
        return getHomes(playerUUID).size() >= MAX_HOMES;
    }

    public Location getHomeLocation(UUID playerUUID, String homeName) {
        Map<String, Location> playerHomes = getHomes(playerUUID);
        for(Map.Entry<String, Location> entry : playerHomes.entrySet()) {
            if(entry.getKey().equalsIgnoreCase(homeName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Map<String, Location> getHomes(UUID playerUUID) {
        return homes.computeIfAbsent(playerUUID, k -> new HashMap<>());
    }

    @Override
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
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(HomeConstants.HOMES_SAVED, homes.keySet().size(), homes.entrySet().size()));
            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
    }

    @Override
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
        Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(HomeConstants.HOMES_LOADED, homes.keySet().size(), homes.entrySet().size()));
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
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            onDisable();
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        saveToFile();
        try {
            Manager.getInstance().getCommand(HomeCommands.CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(HomeCommands.CommandStrings.ROOT).setTabCompleter(null);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "Homes";
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.homePermission");
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.BLUE;
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(MAX_HOMES_JSON_KEY, 5);
        config.setInlineComments(MAX_HOMES_JSON_KEY, List.of("Wie viele Homes ein Spieler maximal haben darf"));
    }
}
