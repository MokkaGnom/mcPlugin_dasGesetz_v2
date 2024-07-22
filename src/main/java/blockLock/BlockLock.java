package blockLock;

// Bukkit:

import manager.Manager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;
import java.util.stream.Collectors;

public class BlockLock
{
    private final UUID owner;
    private final Set<UUID> localFriends;
    private final Block block;
    private boolean hopperLock;
    private boolean redstoneLock;
    private boolean blockBelowLock;
    private BlockLock secondBlockLock;

    public BlockLock(Block block, UUID owner) {
        this(block, owner, block.getType() != Material.HOPPER, true, block.getBlockData() instanceof Door, new HashSet<>());
    }

    public BlockLock(Block block, UUID owner, boolean hopperLock, boolean redstoneLock, boolean blockBelowLock, Set<UUID> localFriends) {
        block.setMetadata(BlockLockManager.META_DATA.BLOCK.LOCK, new FixedMetadataValue(Manager.getInstance(), block.getLocation().toString()));
        block.setMetadata(BlockLockManager.META_DATA.BLOCK.OWNER, new FixedMetadataValue(Manager.getInstance(), owner.toString()));
        this.block = block;
        this.owner = owner;
        this.localFriends = localFriends;
        this.hopperLock = hopperLock;
        this.redstoneLock = redstoneLock;
        this.blockBelowLock = blockBelowLock;
        this.secondBlockLock = null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, block.getLocation());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(obj instanceof BlockLock bl) {
            return this.owner.equals(bl.owner) && this.block.equals(bl.block);
        }
        return false;
    }

    public boolean save(ConfigurationSection blockLockSection) {
        if(blockLockSection == null) return false;
        blockLockSection.set("Location", this.getBlock().getLocation());
        blockLockSection.set("HopperLock", this.isHopperLock());
        blockLockSection.set("RedstoneLock", this.isRedstoneLock());
        blockLockSection.set("BlockBelowLock", this.isBlockBelowLock());

        for(UUID uuid : localFriends) {
            blockLockSection.set("LocalFriends", uuid.toString());
        }

        //blockLockSection.set("SecondBlockLock", ); //TODO
        return true;
    }

    public static BlockLock load(UUID owner, ConfigurationSection blockLockSection) {
        if(owner == null || blockLockSection == null) return null;
        Location location = blockLockSection.getLocation("Location");
        if(location == null) return null;
        boolean hopperLock = blockLockSection.getBoolean("HopperLock");
        boolean redstoneLock = blockLockSection.getBoolean("RedstoneLock");
        boolean blockBelowLock = blockLockSection.getBoolean("BlockBelowLock");
        List<String> friendsList = blockLockSection.getStringList("LocalFriends");
        return new BlockLock(location.getBlock(),
                owner, hopperLock, redstoneLock, blockBelowLock,
                friendsList.stream().map(UUID::fromString).collect(Collectors.toSet()));
    }

    public void removeMetadata() {
        block.removeMetadata(BlockLockManager.META_DATA.BLOCK.LOCK, Manager.getInstance());
        block.removeMetadata(BlockLockManager.META_DATA.BLOCK.OWNER, Manager.getInstance());
    }

    public boolean openManagerMenu(Player p, BlockLockManager blManager) {
        BlockLockManagerMenu blmm = new BlockLockManagerMenu(blManager, this);
        Manager.getInstance().getServer().getPluginManager().registerEvents(blmm, Manager.getInstance());
        return blmm.open(p);
    }

    /**
     * @return 0: No Chest | 1: Single | 2: Right | 3: Left
     */
    static public int isDoubleChest(Block b) {
        if(b.getBlockData() instanceof Chest chest) {
            Chest.Type type = chest.getType();
            if(type.equals(Chest.Type.SINGLE))
                return 1;
            else if(type.equals(Chest.Type.LEFT))
                return 2;
            else if(type.equals(Chest.Type.RIGHT))
                return 3;
        }
        return 0;
    }

    /**
     * @return 0: No Chest | 1: Single | 2: Right | 3: Left
     */
    public int isDoubleChest() {
        return isDoubleChest(getBlock());
    }

    public boolean isDoor() {
        return (getBlock().getBlockData() instanceof Door);
    }

    public boolean addFriend(UUID friend) {
        if(friend != owner) {
            return localFriends.add(friend);
        }
        return false;
    }

    public boolean removeFriend(UUID friend) {
        return localFriends.remove(friend);
    }

    public boolean checkIfFriend(UUID uuid) {
        return localFriends.contains(uuid);
    }

    // Getter/Setter

    public boolean isHopperLock() {
        return hopperLock;
    }

    public void setHopperLock(boolean hopperLock) {
        if(this.block.getType() != Material.HOPPER) {
            this.hopperLock = hopperLock;
        }
    }

    public boolean isRedstoneLock() {
        return redstoneLock;
    }

    public void setRedstoneLock(boolean redstoneLock) {
        this.redstoneLock = redstoneLock;
    }

    public boolean isBlockBelowLock() {
        return blockBelowLock;
    }

    public void setBlockBelowLock(boolean blockBelowLock) {
        if(isDoor()) {
            this.blockBelowLock = blockBelowLock;
        }
    }

    public Block getBlock() {
        return this.block;
    }

    public UUID getOwner() {
        return owner;
    }

    public Inventory getInventory() {
        if(getBlock().getState() instanceof InventoryHolder inventoryHolder) {
            return inventoryHolder.getInventory();
        }
        return null;
    }

    public Set<UUID> getLocalFriends() {
        return localFriends;
    }

    public BlockLock getSecondBlockLock() {
        return secondBlockLock;
    }

    public void setSecondBlockLock(BlockLock bl) {
        secondBlockLock = bl;
    }

}
