package blockLock;

// Bukkit-Event:

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
// Bukkit:
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.Permissible;
import utility.BlockHelper;

// Java:
import java.io.File;
import java.util.*;

import static blockLock.BlockLockConstants.BLOCKS_LOADED;
import static blockLock.BlockLockConstants.BLOCKS_SAVED;

public class BlockLockManager implements Listener, ManagedPlugin
{
    public static final Set<Material> LOCKABLE_BLOCKS = Set.of(Material.ACACIA_DOOR, Material.ACACIA_TRAPDOOR, Material.BIRCH_DOOR, Material.BIRCH_TRAPDOOR, Material.CRIMSON_DOOR,
            Material.CRIMSON_TRAPDOOR, Material.DARK_OAK_DOOR, Material.DARK_OAK_TRAPDOOR, Material.IRON_DOOR, Material.IRON_TRAPDOOR, Material.JUNGLE_DOOR, Material.JUNGLE_TRAPDOOR,
            Material.MANGROVE_DOOR, Material.MANGROVE_TRAPDOOR, Material.OAK_DOOR, Material.OAK_TRAPDOOR, Material.SPRUCE_DOOR, Material.SPRUCE_TRAPDOOR, Material.WARPED_DOOR,
            Material.WARPED_TRAPDOOR, Material.CHEST, Material.TRAPPED_CHEST, Material.HOPPER, Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER, Material.BARREL,
            Material.BREWING_STAND, Material.OAK_FENCE_GATE, Material.BIRCH_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.BAMBOO_FENCE_GATE, Material.CHERRY_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.WARPED_FENCE_GATE, Material.CRIMSON_FENCE_GATE, Material.DARK_OAK_FENCE_GATE, Material.MANGROVE_FENCE_GATE);

    public interface META_DATA
    {
        interface BLOCK
        {
            String LOCK = "blockLock";
            String OWNER = "blockLockOwner";
        }

        interface PLAYER
        {
            String SHOW_SNEAK_MENU = "blockLockShowSneakMenu";
        }
    }

    public static final int MAX_FRIENDS = 54; // Ansonsten müsste man ne zweite Seite im Menü machen
    private final File SAVE_FILE = new File(Manager.getInstance().getDataFolder(), getName() + ".yml");

    private final FileConfiguration saveConfigFile;
    private final Map<UUID, Set<BlockLock>> blockLocks; //TODO: Die sets werden noch nicht erstellt (sind also in get-Methoden immer null)
    private final Map<UUID, Set<UUID>> friends; //TODO: Die sets werden noch nicht erstellt (sind also in get-Methoden immer null)

    public BlockLockManager() {
        this.saveConfigFile = YamlConfiguration.loadConfiguration(SAVE_FILE);
        this.blockLocks = new HashMap<>();
        this.friends = new HashMap<>();
    }

    public String getMessageString(String message) {
        return ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + getName() + ChatColor.GRAY + "] " + ChatColor.WHITE + message;
    }

    public boolean sendMessage(CommandSender receiver, String message) {
        if(receiver != null) {
            receiver.sendMessage(getMessageString(message));
            return true;
        }
        return false;
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        if(event.getWorld().getName().equals(Bukkit.getWorlds().getFirst().getName()))
            saveToFile();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        if(isBlockLockable(b)) {
            if(!checkIfNextBlockIsLocked(b, event.getPlayer())) {
                lock(event.getPlayer(), b);
            }
            else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Player player = event.getPlayer();
            Block block = event.getClickedBlock();

            if(isBlockLock(block)) {
                BlockLock bl = getBlockLock(block);
                if(checkIfPermissionToOpen(player, bl)) {
                    if(player.isSneaking() && getShowSneakMenu(player)) {
                        bl.openManagerInventory(player);
                        event.setCancelled(true);
                    }
                }
                else {
                    sendMessage(player, "No permission!");
                    event.setCancelled(true);
                }
            }
        }
    }

    public boolean saveToFile() {

        //TODO

        try {
            saveConfigFile.save(SAVE_FILE);
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(BLOCKS_SAVED, blockLocks.keySet().size(), blockLocks.entrySet().size()));
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

        //TODO

        Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(BLOCKS_LOADED, blockLocks.keySet().size(), blockLocks.entrySet().size()));
        return true;
    }

    public boolean lock(Player player, Block block) {
        if(!isBlockLockable(block)) {
            sendMessage(player, "Block not supported");
            return false;
        }

        if(!isBlockLock(block) && hasPermission(player)) {
            BlockLock bl = new BlockLock(block, player.getUniqueId());
            blockLocks.get(player.getUniqueId()).add(bl);
            bl.createManagerMenu(this);
            sendMessage(player, block.getType().toString() + " locked!");
            return true;
        }
        sendMessage(player, block.getType().toString() + " is already locked!");
        return false;
    }

    public boolean unlock(Player p, Block b) {
        if(isBlockLock(b)) {
            BlockLock bl = getBlockLock(b);
            if(hasPermission(p) && checkIfPermissionToOpen(p, bl)) {
                bl.unlock();
                sendMessage(p, b.getType().toString() + " unlocked!");
                return true;
            }
            else {
                sendMessage(p, "No permission!");
                return false;
            }
        }
        sendMessage(p, b.getType().toString() + " is not locked!");
        return false;
    }

    public List<String> getFriends(UUID owner, Block b) {
        Set<UUID> set = new HashSet<>(friends.get(owner));
        BlockLock bl = getBlockLock(b);
        if(bl != null) {
            set.addAll(bl.getLocalFriends());
        }
        return (set.isEmpty() ? List.of("No friends") : set.stream().map(UUID::toString).toList());
    }

    public boolean addFriend(UUID owner, Block b, UUID friend) {
        if(owner == null || b == null || friend == null)
            return false;

        BlockLock bl = getBlockLock(b);
        if(bl != null && bl.getOwner().equals(owner)) {
            return bl.addFriend(friend);
        }
        return false;
    }

    public boolean removeFriend(UUID owner, Block b, UUID friend) {
        if(owner == null || b == null || friend == null)
            return false;

        BlockLock bl = getBlockLock(b);
        if(bl != null && bl.getOwner().equals(owner)) {
            return bl.removeFriend(friend);
        }
        return false;
    }

    public boolean addGlobalFriend(UUID owner, UUID friend) {
        if(owner == null || friend == null)
            return false;
        return friends.get(owner).add(friend);
    }

    public boolean removeGlobalFriend(UUID owner, UUID friend) {
        if(owner == null || friend == null)
            return false;
        return friends.get(owner).remove(friend);
    }

    public boolean checkIfNextBlockIsLocked(Block b, Player player) {
        BlockLock bl = null;
        for(int[] offset : BlockHelper.OFFSETS) {
            Block relativeBlock = b.getRelative(offset[0], offset[1], offset[2]);
            if(isBlockLock(relativeBlock)){
                bl = getBlockLock(relativeBlock);
                break;
            }
        }
        return bl != null && !checkIfPermissionToOpen(player, bl);
    }

    public boolean getShowSneakMenu(Player player) {
        return player.hasMetadata(META_DATA.PLAYER.SHOW_SNEAK_MENU) && player.getMetadata(META_DATA.PLAYER.SHOW_SNEAK_MENU).getFirst().asBoolean();
    }

    public void setShowSneakMenu(Player player, boolean show) {
        player.setMetadata(META_DATA.PLAYER.SHOW_SNEAK_MENU, new FixedMetadataValue(Manager.getInstance(), show));
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

    public boolean checkIfPermissionToOpen(Player player, BlockLock blockLock) {
        return hasPermission(player) && (
                blockLock.getOwner().equals(player.getUniqueId())
                        || blockLock.checkIfFriend(player.getUniqueId())
                        || friends.get(blockLock.getOwner()).contains(player.getUniqueId())
        );
    }

    public boolean isBlockLockable(Block block) {
        return LOCKABLE_BLOCKS.contains(block.getType());
    }

    public BlockLock getBlockLock(Block block) {
        UUID owner = getOwner(block);
        if(owner != null) {
            for(BlockLock bl : blockLocks.get(owner)) {
                if(bl.getBlock().equals(block)) {
                    return bl;
                }
            }
        }
        return null;
    }

    public BlockLock getBlockLockFromInventory(Inventory inventory) {
        if(inventory == null) {
            return null;
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
     * Preventing BlockLocks (example: doors, hoppers) to be activated by Redstone
     */
    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        if(isBlockLock(block) && getBlockLock(block).isRedstoneLock()) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    /**
     * Preventing anyone from breaking the Block, or the block below
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if(isBlockLockable(b)) // Block
        {
            if(isBlockLock(b)) {
                BlockLock bl = getBlockLock(b);
                if(bl.getOwner().equals(event.getPlayer().getUniqueId()))
                    unlock(event.getPlayer(), b);
                else
                    event.setCancelled(true);
            }
            /*
             * else { int cibb = checkIfBlockBelow(b, event.getPlayer()); if (cibb == 1) event.setCancelled(true); else if (cibb == 2 && getBlockLock(b.getRelative(0, 1, 0)).checkIfDoor())
             * unlock(event.getPlayer(), b.getRelative(0, 1, 0)); }
             */

        }
        /*
         * else { int cibb = checkIfBlockBelow(b, event.getPlayer()); if (cibb == 1) event.setCancelled(true); else if (cibb == 2 && getBlockLock(b.getRelative(0, 1, 0)).checkIfDoor())
         * unlock(event.getPlayer(), b.getRelative(0, 1, 0)); }
         */

    }

    /**
     * Checks if Block b is below a BlockLock (0/1) and if Player p has permission to open it (1/2)
     */
    public int checkIfBlockBelow(Block b, Player p) {
        Block b2 = b.getRelative(0, 1, 0);
        BlockLock bl2 = getBlockLock(b2);
        if(bl2 != null && bl2.isBlockBelowLock()) {
            if(checkIfPermissionToOpen(p, bl2))
                return 2;
            else
                return 1;
        }
        return 0;
    }

    /**
     * Preventing the Block from being blown up by Creeper, Wither or TNT
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        try {
            if(event.getEntity() instanceof Creeper || event.getEntity() instanceof TNTPrimed || event.getEntity() instanceof Wither) {
                List<Block> removeBlocks = new ArrayList<Block>();
                for(Block i : event.blockList()) {
                    if(isBlockLock(i))
                        removeBlocks.add(i);
                }
                event.blockList().removeAll(removeBlocks);
            }
        } catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage("BL onEntityExplode: " + e.getLocalizedMessage());
        }
    }

    /**
     * Preventing anyone(hoppers, etc) than the player from grabbing items from the Container
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if(true)
            return; // TODO

        BlockLock source = getBlockLockFromInventory(event.getSource());
        BlockLock dest = getBlockLockFromInventory(event.getDestination());
        if((source != null && !event.getDestination().getType().equals(InventoryType.PLAYER) && source.isHopperLock()) // Prevents Hopper, etc. from PUTTING items IN the chest
                || (dest != null && dest.isHopperLock())) // Prevents Hopper, etc. from REMOVING items FROM the chest
        {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean hasPermission(Permissible permissible) {
        return permissible.hasPermission("dg.blockLockPermission");
    }

    @Override
    public boolean onEnable() {
        BlockLockCommands blc = new BlockLockCommands(this);
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        try {
            Manager.getInstance().getCommand(BlockLockCommands.CommandStrings.ROOT).setExecutor(blc);
            Manager.getInstance().getCommand(BlockLockCommands.CommandStrings.ROOT).setTabCompleter(blc);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(e.getMessage());
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
            Manager.getInstance().sendErrorMessage(e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "BlockLock";
    }

}
