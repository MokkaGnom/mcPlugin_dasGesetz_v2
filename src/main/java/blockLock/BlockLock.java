package blockLock;

// Bukkit:

import manager.Manager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
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
    private final Block secondBlock;
    private boolean hopperLock;
    private boolean redstoneLock;

    public BlockLock(Block block, UUID owner) {
        this(block, owner, block.getType() != Material.HOPPER, true, new HashSet<>());
    }

    public BlockLock(Block block, UUID owner, boolean hopperLock, boolean redstoneLock, Set<UUID> localFriends) {
        block.setMetadata(BlockLockManager.META_DATA.BLOCK.LOCK, new FixedMetadataValue(Manager.getInstance(), block.getLocation().toString()));
        block.setMetadata(BlockLockManager.META_DATA.BLOCK.OWNER, new FixedMetadataValue(Manager.getInstance(), owner.toString()));
        this.block = block;
        this.owner = owner;
        this.localFriends = localFriends;
        this.hopperLock = hopperLock;
        this.redstoneLock = redstoneLock;
        this.secondBlock = getSecondBlock(block);
    }

    private static Block getSecondBlock(Block firstBlock) {
        if(firstBlock.getBlockData() instanceof Door door) {
            return firstBlock.getRelative(0, (door.getHalf() == Bisected.Half.TOP ? -1 : 1), 0);
        }
        else if(firstBlock.getBlockData() instanceof Chest chest && chest.getType() != Chest.Type.SINGLE) {
            if(chest.getType() == Chest.Type.RIGHT) {
                //TODO: Über Facing an anderen Teil kommen
                return null;
            }
            else if(chest.getType() == Chest.Type.LEFT) {
                //TODO: Über Facing an anderen Teil kommen
                return null;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "BlockLock{" +
                "owner=" + owner +
                ", block=" + block +
                ", hopperLock=" + hopperLock +
                ", redstoneLock=" + redstoneLock +
                ", secondBlock=" + secondBlock +
                '}';
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
            return this.owner.equals(bl.owner) && this.block.getLocation().equals(bl.block.getLocation());
        }
        return false;
    }

    public boolean save(ConfigurationSection blockLockSection) {
        if(blockLockSection == null) return false;
        blockLockSection.set("Material", this.getBlock().getType().name());
        blockLockSection.set("Location", this.getBlock().getLocation());
        blockLockSection.set("HopperLock", this.isHopperLock());
        blockLockSection.set("RedstoneLock", this.isRedstoneLock());

        for(UUID uuid : localFriends) {
            blockLockSection.set("LocalFriends", uuid.toString());
        }

        if(this.getSecondBlock() != null) {
            ConfigurationSection secondBlockSection = blockLockSection.createSection("SecondBlock");
            secondBlockSection.set("Material", this.getSecondBlock().getType().name());
            secondBlockSection.set("Location", this.getSecondBlock().getLocation());
        }
        return true;
    }

    public static BlockLock load(UUID owner, ConfigurationSection blockLockSection) {
        if(owner == null || blockLockSection == null) return null;

        Material material = Material.getMaterial(blockLockSection.getString("Material"));
        Location location = blockLockSection.getLocation("Location");

        if(location == null || material == null) return null;
        if(!location.getBlock().getType().equals(material)) return null;

        //TODO: SecondBlock

        boolean hopperLock = blockLockSection.getBoolean("HopperLock");
        boolean redstoneLock = blockLockSection.getBoolean("RedstoneLock");
        List<String> friendsList = blockLockSection.getStringList("LocalFriends");

        return new BlockLock(location.getBlock(),
                owner, hopperLock, redstoneLock,
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
     * @return 0: No Chest | 1: Single | 2: Left | 3: Right
     */
    public int getDoubleChest() {
        if(block.getBlockData() instanceof Chest chest) {
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

    public boolean isChest() {
        return (getBlock().getBlockData() instanceof Chest);
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

    public Block getSecondBlock() {
        return secondBlock;
    }

}
