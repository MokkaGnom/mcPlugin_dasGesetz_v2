package blockLogger;


import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockLoggerManager implements ManagedPlugin
{
    public static final String MESSAGE_PREFIX_BLOCKLOGGER_FORMAT = "%s (%s): ";
    public static final String LOGGED_BLOCKS_JSON_KEY = "BlockLogger.LoggedBlocks";
    public static final String DIR_PATH = Manager.getInstance().getDataFolder() + File.separator + "blockLogger";
    public static final String FILE_PATH = DIR_PATH + File.separator + "%s.txt";

    private static BlockLoggerManager instance;

    private final Map<Material, BlockLogger> blockLoggerMap;

    public BlockLoggerManager() {
        instance = this;
        this.blockLoggerMap = new HashMap<>();
        List<String> materialNames = Manager.getInstance().getConfig().getStringList(LOGGED_BLOCKS_JSON_KEY);
        for(String materialName : materialNames) {
            Material material = Material.getMaterial(materialName.toUpperCase());
            if(material != null) {
                this.blockLoggerMap.put(material, new BlockLogger(material));
            }
        }
    }

    public static BlockLoggerManager getInstance() {
        return instance;
    }

    public void logWarning(Material material, String message) {
        Manager.getInstance().sendWarningMessage(String.format(MESSAGE_PREFIX_BLOCKLOGGER_FORMAT, getMessagePrefix(), material.name()), message);
    }

    public void logInfo(Material material, String message) {
        Manager.getInstance().sendInfoMessage(String.format(MESSAGE_PREFIX_BLOCKLOGGER_FORMAT, getMessagePrefix(), material.name()), message);
    }

    private void updateConfig() {
        Manager.getInstance().getConfig().set(LOGGED_BLOCKS_JSON_KEY, blockLoggerMap.keySet().stream().map(Material::name).toList());
    }

    public boolean addBlockLogger(Material material) {
        if(!blockLoggerMap.containsKey(material)) {
            BlockLogger blockLogger = new BlockLogger(material);
            blockLoggerMap.put(material, blockLogger);
            Manager.getInstance().getServer().getPluginManager().registerEvents(blockLogger, Manager.getInstance());
            updateConfig();
            return true;
        }
        return false;
    }

    public boolean removeBlockLogger(Material material) {
        BlockLogger blockLogger = blockLoggerMap.remove(material);
        if(blockLogger != null) {
            updateConfig();
            HandlerList.unregisterAll(blockLogger);
            return true;
        }
        return false;
    }

    public Set<Material> getBlockLoggerMaterials() {
        return this.blockLoggerMap.keySet();
    }

    @Override
    public boolean onEnable() {
        try {
            BlockLoggerCommands blc = new BlockLoggerCommands(this);
            Manager.getInstance().getCommand(BlockLoggerCommands.CommandStrings.ROOT).setExecutor(blc);
            Manager.getInstance().getCommand(BlockLoggerCommands.CommandStrings.ROOT).setTabCompleter(blc);

            (new File(DIR_PATH)).mkdirs();

        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }

        for(BlockLogger blockLogger : blockLoggerMap.values()) {
            blockLogger.deleteLog();
            Manager.getInstance().getServer().getPluginManager().registerEvents(blockLogger, Manager.getInstance());
        }
        return true;
    }

    @Override
    public void onDisable() {
        for(BlockLogger blockLogger : blockLoggerMap.values()) {
            HandlerList.unregisterAll(blockLogger);
        }
        try {
            Manager.getInstance().getCommand(BlockLoggerCommands.CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(BlockLoggerCommands.CommandStrings.ROOT).setTabCompleter(null);

        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "BlockLogger";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.YELLOW;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.blockLoggerPerm");
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(LOGGED_BLOCKS_JSON_KEY, List.of(Material.SWEET_BERRY_BUSH.name()));
        config.setInlineComments(LOGGED_BLOCKS_JSON_KEY, List.of("Blöcke, welche gelogged werden sollen"));
    }

    @Override
    public int getObjectCount() {
        return blockLoggerMap.size();
    }
}
