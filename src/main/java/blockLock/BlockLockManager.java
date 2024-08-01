package blockLock;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import utility.HelperFunctions;
import utility.ErrorMessage;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static blockLock.BlockLockConstants.*;

public class BlockLockManager implements Listener, ManagedPlugin
{
    public static final Set<Material> LOCKABLE_BLOCKS = Set.of(
            Material.ACACIA_DOOR, Material.ACACIA_TRAPDOOR, Material.ACACIA_FENCE_GATE,
            Material.BIRCH_DOOR, Material.BIRCH_TRAPDOOR, Material.BIRCH_FENCE_GATE,
            Material.BAMBOO_DOOR, Material.BAMBOO_TRAPDOOR, Material.BAMBOO_FENCE_GATE,
            Material.CRIMSON_DOOR, Material.CRIMSON_TRAPDOOR, Material.CRIMSON_FENCE_GATE,
            Material.CHERRY_DOOR, Material.CHERRY_TRAPDOOR, Material.CHERRY_FENCE_GATE,
            Material.DARK_OAK_DOOR, Material.DARK_OAK_TRAPDOOR, Material.DARK_OAK_FENCE_GATE,
            Material.JUNGLE_DOOR, Material.JUNGLE_TRAPDOOR, Material.JUNGLE_FENCE_GATE,
            Material.MANGROVE_DOOR, Material.MANGROVE_TRAPDOOR, Material.MANGROVE_FENCE_GATE,
            Material.OAK_DOOR, Material.OAK_TRAPDOOR, Material.OAK_FENCE_GATE,
            Material.SPRUCE_DOOR, Material.SPRUCE_TRAPDOOR, Material.SPRUCE_FENCE_GATE,
            Material.WARPED_DOOR, Material.WARPED_TRAPDOOR, Material.WARPED_FENCE_GATE,
            Material.IRON_DOOR, Material.IRON_TRAPDOOR,
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.HOPPER,
            Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
            Material.CRAFTER, Material.BREWING_STAND);

    public static final int MAX_LOCAL_FRIENDS = 54;

    public interface META_DATA
    {
        interface BLOCK
        {
            String LOCK = "blockLock";
            String OWNER = "blockLockOwner";
            String SECOND_BLOCK = "secondBlock";
        }

        interface PLAYER
        {
            String SHOW_SNEAK_MENU = "blockLockShowSneakMenu";
        }
    }

    private final File SAVE_FILE = new File(Manager.getInstance().getDataFolder(), getName() + ".yml");
    private final File SAVE_FILE_FRIENDS = new File(Manager.getInstance().getDataFolder(), getName() + "_globalFriends.yml");

    private final FileConfiguration saveConfigFile;
    private final FileConfiguration saveFriendsConfigFile;
    private final Map<UUID, Set<BlockLock>> blockLocks;
    private final Map<UUID, Set<UUID>> globalFriends;

    public BlockLockManager() {
        this.saveConfigFile = YamlConfiguration.loadConfiguration(SAVE_FILE);
        this.saveFriendsConfigFile = YamlConfiguration.loadConfiguration(SAVE_FILE_FRIENDS);
        this.blockLocks = new HashMap<>();
        this.globalFriends = new HashMap<>();
    }


    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        if(event.getWorld().getName().equals(Bukkit.getWorlds().getFirst().getName()))
            saveToFile();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        if(isBlockLockable(block) && !isBlockLock(block)) {
            if(!checkIfNextBlockIsLocked(block, player)) {
                lock(player, block);
            }
            else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if(getShowSneakMenu(player) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            BlockLock bl = getBlockLock(event.getClickedBlock());
            if(bl != null) {
                if(hasPermissionToOpen(player, bl)) {
                    bl.openManagerMenu(player, this);
                    event.setCancelled(true);
                }
                else {
                    sendMessage(player, ErrorMessage.NO_PERMISSION.message());
                    event.setCancelled(true);
                }
            }
        }
    }

    public boolean saveToFile() {

        // Save BlockLocks
        for(Map.Entry<UUID, Set<BlockLock>> entry : blockLocks.entrySet()) {
            Set<BlockLock> blockLockSet = entry.getValue();
            if(blockLockSet == null || blockLockSet.isEmpty())
                continue;

            ConfigurationSection uuidSection = saveConfigFile.createSection(entry.getKey().toString());
            for(BlockLock bl : blockLockSet) {
                bl.save(uuidSection.createSection(Integer.toString(bl.hashCode())));
            }
        }

        // Save GlobalFriends
        for(Map.Entry<UUID, Set<UUID>> entry : globalFriends.entrySet()) {
            Set<UUID> friendSet = entry.getValue();
            if(friendSet == null || friendSet.isEmpty())
                continue;

            //ConfigurationSection uuidSection = saveFriendsConfigFile.createSection(entry.getKey().toString());
            String player = entry.getKey().toString();
            for(UUID uuid : friendSet) {
                saveFriendsConfigFile.set(player, uuid.toString());
            }
        }

        try {
            saveConfigFile.save(SAVE_FILE);
            saveFriendsConfigFile.save(SAVE_FILE_FRIENDS);
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(BLOCKS_SAVED, blockLocks.keySet().size(), blockLocks.entrySet().size()));
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(FRIENDS_SAVED, globalFriends.keySet().size(), globalFriends.entrySet().size()));
            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
    }

    public boolean loadFromFile() {
        try {
            saveConfigFile.load(SAVE_FILE);
            saveFriendsConfigFile.load(SAVE_FILE_FRIENDS);
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }

        // Load BlockLocks
        ConfigurationSection uuidSection = saveConfigFile.getConfigurationSection("");
        Set<String> uuids = uuidSection.getKeys(false);
        for(String uuid : uuids) {
            UUID owner = UUID.fromString(uuid);
            ConfigurationSection blSection = uuidSection.getConfigurationSection(uuid);
            if(blSection == null)
                continue;
            for(String blID : blSection.getKeys(false)) {
                BlockLock bl = BlockLock.load(owner, blSection.getConfigurationSection(blID));
                if(bl != null) {
                    addBlockLock(bl);
                }
            }
        }

        // Load Global Friends
        uuidSection = saveFriendsConfigFile.getConfigurationSection("");
        uuids = uuidSection.getKeys(false);
        for(String uuid : uuids) {
            UUID player = UUID.fromString(uuid);
            List<String> friendsList = uuidSection.getStringList(uuid);
            this.globalFriends.put(player, friendsList.stream().map(UUID::fromString).collect(Collectors.toSet()));
        }

        Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(BLOCKS_LOADED, blockLocks.keySet().size(), blockLocks.entrySet().size()));
        Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(FRIENDS_LOADED, globalFriends.keySet().size(), globalFriends.entrySet().size()));
        return true;
    }

    public boolean lock(Player player, Block block) {
        if(!isBlockLockable(block)) {
            sendMessage(player, BLOCK_NOT_SUPPORTED);
            return false;
        }

        if(!isBlockLock(block) && hasDefaultUsePermission(player)) {
            BlockLock bl = new BlockLock(block, player.getUniqueId());
            if(addBlockLock(bl)) {
                sendMessage(player, String.format(BLOCK_LOCKED, block.getType().toString()));
                return true;
            }
        }
        sendMessage(player, String.format(BLOCK_ALREADY_LOCKED, block.getType().toString()));
        return false;
    }

    public void forceUnlock(BlockLock blockLock, Player player) {
        Manager.getInstance().sendInfoMessage(getMessagePrefix(), "Force-Unlock: " + blockLock.hashCode());
        blockLock.removeMetadata();
        removeBlockLock(blockLock);
        if(player != null) {
            sendMessage(player, String.format(BLOCK_UNLOCKED, blockLock.getBlock().getType().toString()));
        }
    }

    public boolean unlock(Player player, Block b) {
        return unlock(player, getBlockLock(b));
    }

    public boolean unlock(Player player, BlockLock blockLock) {
        if(blockLock != null) {
            if(hasPermissionToOpen(player, blockLock)) {
                blockLock.removeMetadata();
                removeBlockLock(blockLock);
                sendMessage(player, String.format(BLOCK_UNLOCKED, blockLock.getBlock().getType().toString()));
                return true;
            }
            else {
                sendMessage(player, ErrorMessage.NO_PERMISSION.message());
                return false;
            }
        }
        sendMessage(player, BLOCK_NOT_LOCKED);
        return false;
    }

    /**
     * @param owner Der Besitzer
     * @param block Der Block
     * @return Globale und lokale Freunde oder null, wenn Block kein BlockLock ist
     */
    public Set<UUID> getFriends(UUID owner, Block block) {
        BlockLock bl = getBlockLock(block);
        if(bl == null) {
            return null;
        }
        Set<UUID> set = new HashSet<>(getFriends(owner));
        set.addAll(bl.getLocalFriends());
        return set;
    }

    public boolean addLocalFriend(UUID owner, Block b, UUID friend) {
        if(owner == null || b == null || friend == null || owner == friend)
            return false;

        BlockLock bl = getBlockLock(b);
        if(bl != null && bl.getOwner().equals(owner)) {
            return bl.addFriend(friend);
        }
        return false;
    }

    public boolean removeLocalFriend(UUID owner, Block b, UUID friend) {
        if(owner == null || b == null || friend == null || owner == friend)
            return false;

        BlockLock bl = getBlockLock(b);
        if(bl != null && bl.getOwner().equals(owner)) {
            return bl.removeFriend(friend);
        }
        return false;
    }

    public boolean addGlobalFriend(UUID owner, UUID friend) {
        if(owner == null || friend == null || owner == friend)
            return false;
        return addFriend(owner, friend);
    }

    public boolean removeGlobalFriend(UUID owner, UUID friend) {
        if(owner == null || friend == null || owner == friend)
            return false;
        return removeFriend(owner, friend);
    }

    public boolean checkIfNextBlockIsLocked(Block b, Player player) {
        BlockLock bl = null;
        for(int[] offset : HelperFunctions.OFFSETS) {
            Block relativeBlock = b.getRelative(offset[0], offset[1], offset[2]);
            if(isBlockLock(relativeBlock)) {
                bl = getBlockLock(relativeBlock);
                break;
            }
        }
        return bl != null && !hasPermissionToOpen(player, bl);
    }

    public boolean getShowSneakMenu(Player player) {
        return (player.hasMetadata(META_DATA.PLAYER.SHOW_SNEAK_MENU) ?
                player.getMetadata(META_DATA.PLAYER.SHOW_SNEAK_MENU).getFirst().asBoolean() :
                setShowSneakMenu(player, true));
    }

    /**
     * @param player
     * @param show
     * @return show (the set value)
     */
    public boolean setShowSneakMenu(Player player, boolean show) {
        player.setMetadata(META_DATA.PLAYER.SHOW_SNEAK_MENU, new FixedMetadataValue(Manager.getInstance(), show));
        return show;
    }

    public boolean isBlockLock(Block block) {
        return isBlockLockable(block) && block.hasMetadata(META_DATA.BLOCK.LOCK);
    }

    public UUID getOwner(Block block) {
        if(isBlockLock(block)) {
            return UUID.fromString(block.getMetadata(META_DATA.BLOCK.OWNER).getFirst().asString());
        }
        return null;
    }

    public Set<UUID> getFriends(UUID user) {
        return this.globalFriends.computeIfAbsent(user, k -> new HashSet<>());
    }

    public boolean addFriend(UUID user, UUID friend) {
        return user != null && getFriends(user).add(friend);
    }

    public boolean removeFriend(UUID user, UUID friend) {
        return user != null && getFriends(user).remove(friend);
    }

    public Set<BlockLock> getBlockLocks(UUID user) {
        return this.blockLocks.computeIfAbsent(user, k -> new HashSet<>());
    }

    public boolean addBlockLock(BlockLock bl) {
        return bl != null && getBlockLocks(bl.getOwner()).add(bl);
    }

    public boolean removeBlockLock(BlockLock bl) {
        return bl != null && getBlockLocks(bl.getOwner()).remove(bl);
    }

    public boolean hasPermissionToOpen(Player player, BlockLock blockLock) {
        return hasDefaultUsePermission(player) && (
                blockLock.getOwner().equals(player.getUniqueId())
                        || blockLock.checkIfFriend(player.getUniqueId())
                        || getFriends(blockLock.getOwner()).contains(player.getUniqueId())
        );
    }

    public boolean isBlockLockable(Block block) {
        return LOCKABLE_BLOCKS.contains(block.getType());
    }

    public BlockLock getBlockLock(Block block) {
        UUID owner = getOwner(block);
        if(owner != null) {
            for(BlockLock bl : getBlockLocks(owner)) {
                if(bl.isBlockLock(block)) {
                    return bl;
                }
            }
        }
        return null;
    }

    public BlockLock getBlockLock(Inventory inventory) {
        if(inventory == null) {
            return null;
        }

        // Sollte immer funktionieren
        if(inventory.getHolder() instanceof BlockInventoryHolder blockInventoryHolder) {
            return getBlockLock(blockInventoryHolder.getBlock());
        }

        for(Map.Entry<UUID, Set<BlockLock>> entry : blockLocks.entrySet()) {
            for(BlockLock bl : entry.getValue()) {
                Inventory inv = bl.getInventory();
                if(inventory.equals(inv)) {
                    return bl;
                }
            }
        }
        return null;
    }

    /* ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- */
    // Protecting Block:

    /**
     * Preventing BlockLocks (example: doors, trapdoors) to be activated by Redstone
     * TODO: Funktioniert nicht bei Hoppern (Hopper lösen das Event nicht aus)
     *  Definitiv Spigot Bug (mit https://www.9minecraft.net/blockprot-plugin/ getestet)
     */
    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        BlockLock bl = getBlockLock(event.getBlock());
        if(bl != null && bl.isRedstoneLock()) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    /**
     * Preventing anyone, except the owner, from breaking the Block or the block below
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        BlockLock bl = getBlockLock(b);
        if(bl != null) {
            if(bl.getOwner().equals(event.getPlayer().getUniqueId()))
                unlock(event.getPlayer(), bl);
            else
                event.setCancelled(true);
        }
        else {
            BlockLock blUpper = getBlockLock(b.getRelative(0, 1, 0));
            if(blUpper != null && blUpper.isDoor()) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Preventing the Block from being blown up by Creeper, Wither or TNT
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if(event.getEntity() instanceof Creeper || event.getEntity() instanceof TNTPrimed || event.getEntity() instanceof Wither) {
            event.blockList().removeAll(event.blockList().stream()
                    .filter(this::isBlockLock).toList());
        }
    }

    /**
     * Preventing anyone(hoppers, etc.) than the player from grabbing items from the Container
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // Prevents Hopper, etc. from PUTTING items IN the chest
        BlockLock source = getBlockLock(event.getSource());
        if(source != null && !event.getDestination().getType().equals(InventoryType.PLAYER) && source.isHopperLock())
        {
            event.setCancelled(true);
            return;
        }
        // Prevents Hopper, etc. from REMOVING items FROM the chest
        BlockLock dest = getBlockLock(event.getDestination());
        if(dest != null && dest.isHopperLock())
        {
            event.setCancelled(true);
            return;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean onEnable() {
        BlockLockCommands blc = new BlockLockCommands(this);
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        try {
            Manager.getInstance().getCommand(BlockLockCommands.CommandStrings.ROOT).setExecutor(blc);
            Manager.getInstance().getCommand(BlockLockCommands.CommandStrings.ROOT).setTabCompleter(blc);
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
        saveToFile();
        HandlerList.unregisterAll(this);
        try {
            Manager.getInstance().getCommand(BlockLockCommands.CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(BlockLockCommands.CommandStrings.ROOT).setTabCompleter(null);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "BlockLock";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.LIGHT_PURPLE;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.blockLockPermission", "dg.blockLockByPassPermission");
    }
}
