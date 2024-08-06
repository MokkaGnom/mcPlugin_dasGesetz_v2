package messagePrefix;

import manager.ManagedPlugin;
import manager.Manager;
import manager.Saveable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.*;

public class PrefixManager implements Listener, ManagedPlugin, Saveable
{
    private final File SAVE_FILE = new File(Manager.getInstance().getDataFolder(), getName() + ".yml");
    public static final String META_DATA_KEY = "prefix";
    public static final int MAX_PREFIX_LENGTH = 20;

    private final FileConfiguration saveFile;
    private final List<Prefix> prefixes;
    private final Map<UUID, Prefix> offlinePrefixes;

    public PrefixManager() {
        this.saveFile = YamlConfiguration.loadConfiguration(SAVE_FILE);
        this.prefixes = new ArrayList<>();
        this.offlinePrefixes = new HashMap<>();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Prefix prefix = getPrefix(event.getPlayer());
        if(prefix != null) {
            event.setFormat(prefix.toString());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        Prefix prefix = getOfflinePrefix(player);
        if(prefix != null) {
            addPrefixToPlayer(player, prefix);
            removeOfflinePrefix(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Prefix prefix = getPrefix(player);
        if(prefix != null) {
            addOfflinePrefix(player, prefix);
        }
    }

    public Prefix getPrefix(Player player) {
        if(player.hasMetadata(META_DATA_KEY)) {
            return (Prefix) player.getMetadata(META_DATA_KEY).getFirst().value();
        }
        return null;
    }

    public void addOfflinePrefix(Player player, Prefix prefix) {
        offlinePrefixes.put(player.getUniqueId(), prefix);
    }

    public void removeOfflinePrefix(Player player) {
        offlinePrefixes.remove(player.getUniqueId());
    }

    public Prefix getOfflinePrefix(Player player) {
        return offlinePrefixes.get(player.getUniqueId());
    }

    public boolean addPrefixToPlayer(Player player, String prefixName) {
        Prefix prefix = getPrefix(prefixName);
        return addPrefixToPlayer(player, prefix);
    }

    public boolean addPrefixToPlayer(Player player, Prefix prefix) {
        if(prefix == null || (prefix.isAdminPrefix() && !hasAdminPermission(player))) {
            return false;
        }
        player.setPlayerListName(String.format(prefix.getTabPrefix(), player.getName()));
        player.setMetadata(META_DATA_KEY, new FixedMetadataValue(Manager.getInstance(), prefix));
        return true;
    }

    public boolean removePrefixFromPlayer(Player player) {
        if(!player.hasMetadata(META_DATA_KEY)) {
            return false;
        }
        player.setPlayerListName(player.getName());
        player.removeMetadata(META_DATA_KEY, Manager.getInstance());
        return true;
    }

    public boolean addNewPrefix(String prefixName, String prefixColorString, String nameColorString, boolean isAdminPrefix) {
        if(prefixName.length() > MAX_PREFIX_LENGTH) {
            return false;
        }
        ChatColor prefixColor;
        ChatColor nameColor;
        try {
            prefixColor = ChatColor.valueOf(prefixColorString);
            nameColor = ChatColor.valueOf(nameColorString);
        } catch(Exception e) {
            return false;
        }

        Prefix prefix = new Prefix(prefixName, prefixColor, nameColor, isAdminPrefix);

        if(!prefixes.contains(prefix)) {
            return prefixes.add(prefix);
        }
        return false;
    }

    public boolean removePrefix(String prefixName) {
        Prefix prefix = getPrefix(prefixName);
        if(prefix != null) {
            return prefixes.remove(prefix);
        }
        return false;
    }

    public Prefix getPrefix(String prefixName) {
        for(Prefix prefix : prefixes) {
            if(prefix.prefix().equalsIgnoreCase(prefixName)) {
                return prefix;
            }
        }
        return null;
    }

    public List<Prefix> getPrefixes() {
        return prefixes;
    }

    public List<Prefix> getPrefixes(Player player){
        return hasAdminPermission(player) ? getPrefixes() : prefixes.stream().filter(prefix -> !prefix.isAdminPrefix()).toList();
    }

    public List<String> getPrefixesAsPreviewStrings(Player player) {
        List<String> p = new ArrayList<>();
        for(Prefix prefix : getPrefixes(player)) {
            p.add(getPrefixAsPreviewString(prefix));
        }
        return p;
    }

    public String getPrefixAsPreviewString(Prefix prefix) {
        return String.format(prefix.toString(), "Name", "Nachricht");
    }

    @Override
    public boolean saveToFile() {
        // Save Prefixes
        for(Prefix prefix : prefixes) {
            if(prefix != null){
                prefix.save(saveFile.createSection(prefix.prefix()));
            }
        }

        try {
            saveFile.save(SAVE_FILE);
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format("Saved %s prefixes", prefixes.size()));
            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean loadFromFile() {
        try {
            saveFile.load(SAVE_FILE);
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }

        ConfigurationSection section = saveFile.getConfigurationSection("");
        for(String prefixName : section.getKeys(false)) {
            Prefix prefix = Prefix.load(section.getConfigurationSection(prefixName));
            if(prefix != null) {
                prefixes.add(prefix);
            }
        }

        if(prefixes.isEmpty()) {
            prefixes.add(new Prefix("Admin", ChatColor.RED, ChatColor.GOLD, true));
        }
        Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format("Loaded %s prefixes", prefixes.size()));
        return true;
    }

    @Override
    public boolean onEnable() {
        PrefixCommands pc = new PrefixCommands(this);
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        try {
            Manager.getInstance().getCommand(PrefixCommands.CommandStrings.ROOT).setExecutor(pc);
            Manager.getInstance().getCommand(PrefixCommands.CommandStrings.ROOT).setTabCompleter(pc);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            onDisable();
            return false;
        }
        loadFromFile();
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        try {
            Manager.getInstance().getCommand(PrefixCommands.CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(PrefixCommands.CommandStrings.ROOT).setTabCompleter(null);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
        saveToFile();
    }

    @Override
    public String getName() {
        return "MessagePrefix";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.DARK_GREEN;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.prefixUsePermission", "dg.prefixAdminPermission");
    }
}
