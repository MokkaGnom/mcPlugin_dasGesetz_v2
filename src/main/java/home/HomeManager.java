package home;

import manager.ManagedPlugin;
import manager.Manager;
import manager.Saveable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

public class HomeManager implements ManagedPlugin, Saveable
{
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
    private static Location getTPLocation(Location loc) {
        return loc.add(
                loc.getX() % 1 == 0.0d ? 0.5d : 0.0d,
                0.0d,
                loc.getZ() % 1 == 0.0d ? 0.5d : 0.0d
        );
    }

    /**
     *
     * @param playerUUID
     * @param homeName
     * @param location
     * @return 0: Success | 1: Already exists | 2: maximum reached
     */
    public int addHome(UUID playerUUID, String homeName, Location location) {
        if(getHome(playerUUID, homeName) != null) {
            return 1;
        }
        else if(isMaxHomesReached(playerUUID)) {
            return 2;
        }
        else {
            getAllHomes(playerUUID).put(homeName, getTPLocation(location));
            return 0;
        }
    }

    /**
     *
     * @param playerUUID
     * @param homeName
     * @return 0: Success | 1: Not found | 2: Error
     */
    public int removeHome(UUID playerUUID, String homeName) {
        Map.Entry<String, Location> home = getHome(playerUUID, homeName);
        if(home != null) {
            if(getAllHomes(playerUUID).remove(home.getKey()) != null) {
                return 0;
            }
            else {
                Manager.getInstance().sendWarningMessage(getMessagePrefix(),
                        "Home-Remove: Could not remove existing home: " + playerUUID + ": " + home.getKey());
                return 2;
            }
        }
        else {
            return 1;
        }
    }

    public List<String> getAllHomeNames(UUID playerUUID) {
        Map<String, Location> playerHomes = getAllHomes(playerUUID);
        return new ArrayList<>(playerHomes.keySet());
    }

    /**
     *
     * @param playerUUID
     * @param homeName
     * @return 0: Success | 1: Not found
     */
    public int teleportToHome(UUID playerUUID, String homeName) {
        Map.Entry<String, Location> entry = getHome(playerUUID, homeName);
        Player player = Manager.getInstance().getServer().getPlayer(playerUUID);
        if(entry != null && entry.getValue() != null && player != null) {
            player.teleport(entry.getValue());
            return 0;
        }
        else {
            return 1;
        }
    }

    public boolean isMaxHomesReached(UUID playerUUID) {
        return getAllHomes(playerUUID).size() >= MAX_HOMES;
    }

    public Map.Entry<String, Location> getHome(UUID playerUUID, String homeName) {
        Map<String, Location> playerHomes = getAllHomes(playerUUID);
        for(Map.Entry<String, Location> entry : playerHomes.entrySet()) {
            if(entry.getKey().equalsIgnoreCase(homeName)) {
                return entry;
            }
        }
        return null;
    }

    public Map<String, Location> getAllHomes(UUID playerUUID) {
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
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format("Homes saved! (P:%d H:%d)", homes.keySet().size(), homes.entrySet().size()));
            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public boolean loadFromFile() {
        try {
            saveConfigFile.load(SAVE_FILE);
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getLocalizedMessage());
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
        Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format("Homes loaded! (P:%d H:%d)", homes.keySet().size(), homes.entrySet().size()));
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
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getLocalizedMessage());
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
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getLocalizedMessage());
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
