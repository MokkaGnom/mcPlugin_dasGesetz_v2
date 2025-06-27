package manager;

import blockLock.BlockLockManager;
import blockLogger.BlockLoggerManager;
import commands.CommandsManager;
import deathChest.DeathChestManager;
import entityTrophy.EntityTrophyManager;
import farming.EasyFarming;
import farming.Timber;
import home.HomeManager;
import manager.language.LanguageManager;
import messagePrefix.PrefixManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import other.WelcomeMessages;
import ping.PingManager;
import entityTrophy.PlayerTrophyManager;
import utility.ConfigFile;
import villagerCreator.VillagerCreatorManager;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Main-Class
 */
public class Manager extends JavaPlugin {
    private static final String JSON_PLUGIN_KEY = "Manager";
    private static final String MESSAGE_PREFIX = String.format(ManagedPlugin.MESSAGE_PREFIX, "DG-Manager");
    private static Manager instance = null;

    private final ManagerCommands managerCommands;
    private final ManagerEvents managerEvents;
    private final Map<ManagedPlugin, Boolean> plugins;
    private final Map<String, ConfigFile> configFiles;
    private final LanguageManager languageManager;

    /**
     * NICHT BENUTZEN !!!
     * <p>Stattdessen: {@link Manager#getInstance()}</p>
     */
    public Manager() {
        assert instance == null;
        this.languageManager = new LanguageManager();
        this.managerCommands = new ManagerCommands();
        this.managerEvents = new ManagerEvents();
        this.plugins = new HashMap<>();
        this.configFiles = new HashMap<>();
    }

    public static synchronized Manager getInstance() {
        return instance;
    }

    // Benötigt für Artifact-Build
    public static void main(String[] args) {
        throw new IllegalStateException("ERROR: MAIN CALLED!");
    }

    private void createDefaultConfig() {
        FileConfiguration config = this.getConfig();

        for(Map.Entry<ManagedPlugin, Boolean> pluginEntry : plugins.entrySet()) {
            pluginEntry.getKey().createDefaultConfig(config);
            try {
                config.addDefault(getConfigEntryPath(JSON_PLUGIN_KEY, pluginEntry.getKey().getName()), pluginEntry.getValue());
            } catch(Exception e) {
                sendWarningMessage(MESSAGE_PREFIX, e.getMessage());
                sendInfoMessage(MESSAGE_PREFIX, getConfigEntryPath(JSON_PLUGIN_KEY, pluginEntry.getKey().getName()));
            }
        }

        saveDefaultConfig();
        config.options().copyDefaults(true);
    }

    @Override
    public void onEnable() {
        instance = this;

        sendInfoMessage(MESSAGE_PREFIX, "Startup...");

        this.plugins.put(new BlockLockManager(), true);
        this.plugins.put(new CommandsManager(), true);
        this.plugins.put(new DeathChestManager(), true);
        this.plugins.put(new EasyFarming(), true);
        this.plugins.put(new Timber(), true);
        this.plugins.put(new HomeManager(), true);
        this.plugins.put(new BlockLoggerManager(), false);
        this.plugins.put(new WelcomeMessages(), true);
        this.plugins.put(new PingManager(), true);
        this.plugins.put(new VillagerCreatorManager(), true);
        this.plugins.put(new PrefixManager(), true);
        this.plugins.put(new EntityTrophyManager(), true);

        int loadedLanguages = languageManager.loadFromFile();
        if(loadedLanguages >= 0) {
            sendInfoMessage(MESSAGE_PREFIX, "Loaded languages for " + loadedLanguages + " sub-plugins");
        }
        managerCommands.onEnable();
        managerEvents.onEnable();
        createDefaultConfig();

        sendInfoMessage(MESSAGE_PREFIX, "Enable plugins...");

        Map<ManagedPlugin, Boolean> newPlugins = new HashMap<>();
        for(Map.Entry<ManagedPlugin, Boolean> pluginEntry : plugins.entrySet()) {
            boolean enable = this.getConfig().getBoolean(getConfigEntryPath(JSON_PLUGIN_KEY, pluginEntry.getKey().getName()));
            newPlugins.put(pluginEntry.getKey(), enable);

            if(enable) {
                sendInfoMessage(MESSAGE_PREFIX, "Enable \"" + pluginEntry.getKey().getName() + "\"...");
                pluginEntry.getKey().onEnable();
            }
        }
        this.plugins.putAll(newPlugins);

        sendInfoMessage(MESSAGE_PREFIX, "Plugins enabled");
    }

    @Override
    public void onDisable() {
        managerCommands.onDisable();
        managerEvents.onDisable();

        sendInfoMessage(MESSAGE_PREFIX, "Saving configs...");
        try {
            this.saveConfig();
            configFiles.values().forEach(ConfigFile::save);
        } catch(Exception e) {
            sendErrorMessage(MESSAGE_PREFIX, e.getLocalizedMessage());
        }

        sendInfoMessage(MESSAGE_PREFIX, "Disabling plugins...");
        for(Map.Entry<ManagedPlugin, Boolean> pluginEntry : plugins.entrySet()) {
            if(pluginEntry.getValue()) {
                pluginEntry.getKey().onDisable();
            }
        }

        sendInfoMessage(MESSAGE_PREFIX, "Plugins disabled");
        sendInfoMessage(MESSAGE_PREFIX, "Byeee...");
    }

    public void enablePlugin(ManagedPlugin plugin) {
        plugin.onEnable();
        this.getConfig().set(getConfigEntryPath(JSON_PLUGIN_KEY, plugin.getName()), true);
    }

    public void disablePlugin(ManagedPlugin plugin) {
        plugin.onDisable();
        this.getConfig().set(getConfigEntryPath(JSON_PLUGIN_KEY, plugin.getName()), false);
    }

    public void addPermissionToUser(Player player, String permission) {
        player.addAttachment(this).setPermission(permission, true);
    }

    public void removePermissionFromUser(Player player, String permission) {
        player.addAttachment(this).unsetPermission(permission);
    }

    public List<String> getPermissions() {
        return plugins.keySet().stream()
                .flatMap(plugin -> plugin.getPermissions().stream())
                .toList();
    }

    public ManagedPlugin getSubPlugin(Class<? extends ManagedPlugin> pluginClass) {
        for(Map.Entry<ManagedPlugin, Boolean> pluginEntry : plugins.entrySet()) {
            if(pluginEntry.getKey().getClass().equals(pluginClass) && pluginEntry.getValue()) {
                return pluginEntry.getKey();
            }
        }
        return null;
    }

    public ConfigurationSection getConfigSection(ManagedPlugin p) {
        return Objects.requireNonNullElse(
                getConfig().getConfigurationSection(p.getConfigKey()),
                getConfig().createSection(p.getConfigKey())
        );
    }

    public Map<ManagedPlugin, Boolean> getSubPlugins() {
        return plugins;
    }

    @Deprecated(forRemoval = true)
    public static String getConfigEntryPath(String... path) {
        StringBuilder finalPath = new StringBuilder();
        for(String s : path) {
            finalPath.append(s);
            finalPath.append(".");
        }
        return finalPath.substring(0, finalPath.toString().length() - 1);
    }

    @Deprecated(forRemoval = true)
    public Object getConfigEntry(String... path) {
        return getConfig().getString(getConfigEntryPath(path));
    }

    @Deprecated(forRemoval = true)
    public Object getConfigEntry(String path) {
        return this.getConfig().get(path);
    }

    public boolean setConfigEntry(String path, Object value) {
        if(this.getConfigEntry(path) != null) {
            getConfig().set(path, value);
            return true;
        }
        return false;
    }

    public FileConfiguration createConfigFile(String name) {
        ConfigFile configFile = configFiles.get(name);
        if(configFile != null) return configFile.config();

        File file = new File(Manager.getInstance().getDataFolder(), name + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        /*if(file.exists()){
            try {
                config.load(file);
            } catch(Exception e) {
                sendErrorMessage(MESSAGE_PREFIX, String.format("Error creating configFile \"%s\": %s", name, e.getLocalizedMessage()));
                return null;
            }
        }*/

        configFiles.put(name, new ConfigFile(config, file));
        return config;
    }

    public boolean saveConfigFile(String name) {
        ConfigFile configFile = configFiles.get(name);
        return configFile != null && configFile.save();
    }

    public Class<? extends ManagedPlugin> getPluginClassFromName(String subPluginName) {
        if(this.managerCommands.getName().equalsIgnoreCase(subPluginName)) {
            return this.managerCommands.getClass();
        }
        else if(this.managerEvents.getName().equalsIgnoreCase(subPluginName)) {
            return this.managerEvents.getClass();
        }
        for(Class<? extends ManagedPlugin> plugin : getSubPlugins().keySet().stream().map(ManagedPlugin::getClass).toList()) {
            if(plugin.getName().equals(subPluginName)) {
                return plugin;
            }
        }
        return null;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public void sendErrorMessage(ManagedPlugin subPlugin, String message) {
        Bukkit.getLogger().severe(subPlugin.getMessagePrefix() + message);
    }

    public void sendWarningMessage(ManagedPlugin subPlugin, String message) {
        Bukkit.getLogger().warning(subPlugin.getMessagePrefix() + message);
    }

    public void sendInfoMessage(ManagedPlugin subPlugin, String message) {
        Bukkit.getLogger().info(subPlugin.getMessagePrefix() + message);
    }

    @Deprecated(forRemoval = true)
    public void sendErrorMessage(String prefix, String message) {
        Bukkit.getLogger().severe(prefix + message);
    }

    @Deprecated(forRemoval = true)
    public void sendWarningMessage(String prefix, String message) {
        Bukkit.getLogger().warning(prefix + message);
    }

    @Deprecated(forRemoval = true)
    public void sendInfoMessage(String prefix, String message) {
        Bukkit.getLogger().info(prefix + message);
    }
}
