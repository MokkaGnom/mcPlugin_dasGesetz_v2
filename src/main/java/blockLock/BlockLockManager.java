package blockLock;

import manager.ManagedPlugin;
import manager.Manager;
import manager.Saveable;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import utility.ErrorMessage;
import utility.HelperFunctions;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static blockLock.BlockLockConstants.*;

//TODO: Das Plugin laggt. Problem ist, dass die Funktionen (selbst die getBlockLock(Block)) Methode zu lange braucht.
// Es kommt durchaus vor, dass die Funktion 50+ pro Tick aufgerufen wird, ein Tick is aber nur 50ms lang. Da die Funktion aber länger benötigt, dauert der Tick auch länger und dadurch laggt es.
// Um alles effizienter zu machen, müssten alle Infos aus dem Model (BlockLock) in die Meta-Daten des Blocks geschrieben werden.
// Sollte gefixt sein. Muss noch ausreichend getestet werden.
public class BlockLockManager implements Listener, ManagedPlugin, Saveable
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

    @Override
    public int getObjectCount() {
        return 4 +
                blockLocks.keySet().size() +
                blockLocks.values().size() +
                globalFriends.keySet().size() +
                globalFriends.values().size();
    }

    public interface META_DATA
    {
        String SHOW_SNEAK_MENU = "blockLockShowSneakMenu";
    }

    private final File SAVE_FILE = new File(Manager.getInstance().getDataFolder(), getName() + ".yml");
    private final File SAVE_FILE_FRIENDS = new File(Manager.getInstance().getDataFolder(), getName() + "_globalFriends.yml");

    private final FileConfiguration saveConfigFile;
    private final FileConfiguration saveFriendsConfigFile;
    private final Map<UUID, Set<Block>> blockLocks;
    private final Map<UUID, Set<UUID>> globalFriends;

    public BlockLockManager() {
        this.saveConfigFile = YamlConfiguration.loadConfiguration(SAVE_FILE);
        this.saveFriendsConfigFile = YamlConfiguration.loadConfiguration(SAVE_FILE_FRIENDS);
        this.blockLocks = new HashMap<>();
        this.globalFriends = new HashMap<>();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        if(isBlockLockable(block) && !BlockLock.isBlockLock(block)) {
            if(!checkIfNextBlockIsLocked(block, player)) {
                lock(player, block);
            }
            else {
                event.setCancelled(true);
                sendMessage(player, ErrorMessage.NO_PERMISSION.message());
            }
        }
    }

    @EventHandler
    public void onEntityInteract(EntityInteractEvent event) {
        if(BlockLock.isBlockLock(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && BlockLock.isBlockLock(block)) {
            if(hasPermissionToOpen(player, block)) {
                if(getShowSneakMenu(player) && player.getInventory().getItemInMainHand().getType() == Material.AIR && player.isSneaking()) {
                    (new BlockLockManagerMenu(this, block)).open(player);
                    event.setCancelled(true);
                }
            }
            else {
                sendMessage(player, ErrorMessage.NO_PERMISSION.message());
                event.setCancelled(true);
            }

        }
    }

    @Override
    public boolean saveToFile() {
        // Save BlockLocks
        long savedBlockLocks = 0;
        for(Map.Entry<UUID, Set<Block>> entry : blockLocks.entrySet()) {
            Set<Block> blockSet = entry.getValue();
            if(blockSet == null || blockSet.isEmpty())
                continue;

            ConfigurationSection uuidSection = saveConfigFile.createSection(entry.getKey().toString());
            for(Block bl : blockSet) {
                if(BlockLock.save(uuidSection.createSection(Integer.toString(bl.hashCode())), bl)) {
                    savedBlockLocks++;
                }
            }
        }

        // Save GlobalFriends
        long savedGlobalFriends = 0;
        for(Map.Entry<UUID, Set<UUID>> entry : globalFriends.entrySet()) {
            Set<UUID> friendSet = entry.getValue();
            if(friendSet == null || friendSet.isEmpty())
                continue;

            String player = entry.getKey().toString();
            ConfigurationSection friendsSection = saveFriendsConfigFile.createSection(player);
            for(UUID uuid : friendSet) {
                friendsSection.set(uuid.toString(), "");
                savedGlobalFriends++;
            }
        }

        try {
            saveConfigFile.save(SAVE_FILE);
            saveFriendsConfigFile.save(SAVE_FILE_FRIENDS);
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(BLOCKS_SAVED, blockLocks.keySet().size(), savedBlockLocks));
            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(FRIENDS_SAVED, globalFriends.keySet().size(), savedGlobalFriends));
            return true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            return false;
        }
    }

    @Override
    public boolean loadFromFile() {
        boolean loaded = true;

        // Load BlockLocks
        long blockLocksLoaded = 0;
        try {
            saveConfigFile.load(SAVE_FILE);
            ConfigurationSection uuidSection = saveConfigFile.getConfigurationSection("");
            Set<String> uuids = uuidSection.getKeys(false);
            for(String uuid : uuids) {
                UUID owner = UUID.fromString(uuid);
                ConfigurationSection blSection = uuidSection.getConfigurationSection(uuid);
                if(blSection == null)
                    continue;
                for(String blID : blSection.getKeys(false)) {
                    Block bl = BlockLock.load(owner, blSection.getConfigurationSection(blID));
                    if(bl != null) {
                        addBlockLock(bl);
                        blockLocksLoaded++;
                    }
                }
            }

            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(BLOCKS_LOADED, blockLocks.keySet().size(), blockLocksLoaded));
            loaded &= true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            loaded = false;
        }

        // Load Global Friends
        try {
            saveFriendsConfigFile.load(SAVE_FILE_FRIENDS);
            int globalFriendsLoaded = 0;
            ConfigurationSection uuidSection = saveFriendsConfigFile.getConfigurationSection("");
            Set<String> uuids = uuidSection.getKeys(false);
            for(String uuid : uuids) {
                UUID player = UUID.fromString(uuid);
                Set<String> friendsList = uuidSection.getConfigurationSection(uuid).getKeys(false);
                globalFriendsLoaded += friendsList.size();
                this.globalFriends.put(player, friendsList.stream().map(UUID::fromString).collect(Collectors.toSet()));
            }

            Manager.getInstance().sendInfoMessage(getMessagePrefix(), String.format(FRIENDS_LOADED, globalFriends.keySet().size(), globalFriendsLoaded));
            loaded &= true;
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            loaded = false;
        }
        return loaded;
    }

    public boolean lock(Player player, Block block) {
        if(!isBlockLockable(block)) {
            sendMessage(player, BLOCK_NOT_SUPPORTED);
            return false;
        }
        if(!hasDefaultUsePermission(player)) {
            sendMessage(player, ErrorMessage.NO_PERMISSION.message());
            return false;
        }

        if(!BlockLock.isBlockLock(block)) {
            if(BlockLock.createBlockLock(block, player.getUniqueId(), true) && addBlockLock(block)) {
                sendMessage(player, String.format(BLOCK_LOCKED, block.getType().toString()));
                return true;
            }
            else {
                sendMessage(player, "ERROR");
            }
        }
        else {
            sendMessage(player, String.format(BLOCK_ALREADY_LOCKED, block.getType().toString()));
        }
        return false;
    }

    public boolean unlock(Player player, Block block, boolean second) {
        if(block != null) {
            if(hasPermissionToOpen(player, block)) {
                UUID uuid = player.getUniqueId();
                removeBlockLock(uuid, block);
                if(second){
                    removeBlockLock(uuid, BlockLock.getSecondBlock(block));
                }
                BlockLock.delete(block, second);
                sendMessage(player, String.format(BLOCK_UNLOCKED, block.getType().name()));
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
        if(!BlockLock.isBlockLock(block)) {
            return null;
        }
        Set<UUID> set = new HashSet<>(getFriends(owner));
        set.addAll(BlockLock.getLocalFriends(block));
        return set;
    }

    public boolean addLocalFriend(UUID owner, Block block, UUID friend) {
        if(owner == null || block == null || friend == null || owner == friend)
            return false;

        if(BlockLock.isBlockLock(block) && BlockLock.getOwner(block).equals(owner)) {
            return BlockLock.getLocalFriends(block).add(friend);
        }
        return false;
    }

    public boolean removeLocalFriend(UUID owner, Block block, UUID friend) {
        if(owner == null || block == null || friend == null || owner == friend)
            return false;

        if(BlockLock.isBlockLock(block) && BlockLock.getOwner(block).equals(owner)) {
            return BlockLock.getLocalFriends(block).remove(friend);
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

    //TODO: Was macht die Methode bzw braucht man die?
    // Wahrscheinlich, um zu überprüfen, ob man z.B. eine Truhe neben eine andere fremde gelockte Truhe stellt (Testen!)
    public boolean checkIfNextBlockIsLocked(Block b, Player player) {
        Block nextBlock = null;
        for(int[] offset : HelperFunctions.OFFSETS) {
            Block relativeBlock = b.getRelative(offset[0], offset[1], offset[2]);
            if(BlockLock.isBlockLock(relativeBlock)) {
                nextBlock = relativeBlock;
                break;
            }
        }
        return BlockLock.isBlockLock(nextBlock) && !hasPermissionToOpen(player, nextBlock);
    }

    public boolean getShowSneakMenu(Player player) {
        return (player.hasMetadata(META_DATA.SHOW_SNEAK_MENU) ?
                player.getMetadata(META_DATA.SHOW_SNEAK_MENU).getFirst().asBoolean() :
                setShowSneakMenu(player, true));
    }

    /**
     * @param player
     * @param show
     * @return show (the set value)
     */
    public boolean setShowSneakMenu(Player player, boolean show) {
        player.setMetadata(META_DATA.SHOW_SNEAK_MENU, new FixedMetadataValue(Manager.getInstance(), show));
        return show;
    }

    public UUID getOwner(Block block) {
        if(BlockLock.isBlockLock(block)) {
            return BlockLock.getOwner(block);
        }
        return null;
    }

    /**
     * @return KeySet
     */
    public Set<UUID> getFriends() {
        return this.globalFriends.keySet();
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

    public Set<Block> getBlockLocks(UUID user) {
        return this.blockLocks.computeIfAbsent(user, k -> new HashSet<>());
    }

    public boolean addBlockLock(Block block) {
        return BlockLock.isBlockLock(block) && getBlockLocks(BlockLock.getOwner(block)).add(block);
    }

    public boolean removeBlockLock(UUID owner, Block block) {
        return getBlockLocks(owner).remove(block);
    }

    public boolean hasPermissionToOpen(Player player, Block block) {
        return hasAdminPermission(player) || hasDefaultUsePermission(player) && BlockLock.isBlockLock(block) && (
                BlockLock.hasPermissionToOpen(block, player.getUniqueId())
                        || getFriends(BlockLock.getOwner(block)).contains(player.getUniqueId())
        );
    }

    public boolean isBlockLockable(Block block) {
        return LOCKABLE_BLOCKS.contains(block.getType());
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
        if(Objects.requireNonNullElse(BlockLock.getRedstoneLock(event.getBlock()), false)) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    /**
     * Preventing anyone, except the owner, from breaking the Block or the block below
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if(BlockLock.getOwner(block) instanceof UUID owner) {
            if(owner.equals(player.getUniqueId())) {
                unlock(player, block, false);
            }
            else {
                event.setCancelled(true);
                sendMessage(player, ErrorMessage.NO_PERMISSION.message());
            }
        }
        else {
            Block upperBlock = block.getRelative(0, 1, 0);
            if(BlockLock.isBlockLock(upperBlock) && BlockLock.isDoor(upperBlock)) {
                event.setCancelled(true);
                sendMessage(player, ErrorMessage.NO_PERMISSION.message());
            }
        }
    }

    /**
     * Preventing the Block from being blown up by Creeper, Wither, TNT or Wind-Charge
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if(event.getEntity() instanceof Creeper || event.getEntity() instanceof TNTPrimed || event.getEntity() instanceof Wither || event.getEntity() instanceof AbstractWindCharge) {
            event.blockList().removeAll(event.blockList().stream()
                    .filter(BlockLock::isBlockLock).toList());
        }
    }

    /**
     * Preventing anyone(hoppers, etc.) than the player from grabbing items from the Container
     */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        //TODO: Stimmen die Bedingungen?
        if((// Prevents Hopper, etc. from PUTTING items IN the chest
                event.getSource().getHolder() instanceof BlockInventoryHolder blockInventoryHolderSrc
                        && Objects.requireNonNullElse(BlockLock.getHopperLock(blockInventoryHolderSrc.getBlock()), false)
                        && !event.getDestination().getType().equals(InventoryType.PLAYER)
        ) || (// Prevents Hopper, etc. from REMOVING items FROM the chest
                event.getDestination().getHolder() instanceof BlockInventoryHolder blockInventoryHolderDest
                        && Objects.requireNonNullElse(BlockLock.getHopperLock(blockInventoryHolderDest.getBlock()), false)
        )) {
            event.setCancelled(true);
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
