package blockLock;

import manager.Manager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import utility.HelperFunctions;

import java.util.*;
import java.util.stream.Collectors;

public class BlockLock
{
    public interface META_DATA
    {
        String LOCKED = "blockLock";
        String OWNER = "blockLockOwner";
        String REDSTONE_LOCK = "blockLockRedstoneLock";
        String HOPPER_LOCK = "blockLockHopperLock";
        String SECOND_BLOCK = "secondBlock";
    }

    private final Set<UUID> localFriends;
    private final Block block;
    private Block secondBlock;
    private BlockLockManagerMenu blockLockManagerMenu;

    public BlockLock(Block block, UUID owner) {
        this(block, owner, block.getType() != Material.HOPPER, block.getType() != Material.HOPPER, new HashSet<>());
    }

    public BlockLock(Block block, UUID owner, boolean hopperLock, boolean redstoneLock, Set<UUID> localFriends) {
        block.setMetadata(META_DATA.LOCKED, new FixedMetadataValue(Manager.getInstance(), block.getLocation()));
        block.setMetadata(META_DATA.OWNER, new FixedMetadataValue(Manager.getInstance(), owner.toString()));
        block.setMetadata(META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(), hopperLock));
        block.setMetadata(META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(), redstoneLock));
        this.blockLockManagerMenu = null;
        this.block = block;
        this.localFriends = localFriends;
        this.secondBlock = getSecondBlock(block);
        if(secondBlock != null) {
            block.setMetadata(META_DATA.SECOND_BLOCK, new FixedMetadataValue(Manager.getInstance(), secondBlock.getLocation()));
            secondBlock.setMetadata(META_DATA.LOCKED, new FixedMetadataValue(Manager.getInstance(), secondBlock.getLocation()));
            secondBlock.setMetadata(META_DATA.OWNER, new FixedMetadataValue(Manager.getInstance(), owner.toString()));
            secondBlock.setMetadata(META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(), hopperLock));
            secondBlock.setMetadata(META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(), redstoneLock));
            secondBlock.setMetadata(META_DATA.SECOND_BLOCK, new FixedMetadataValue(Manager.getInstance(), block.getLocation()));
        }
    }

    private static Block getSecondBlock(Block firstBlock) {
        if(firstBlock.getBlockData() instanceof Door door) {
            Block secondBlock = firstBlock.getRelative(0, (door.getHalf() == Bisected.Half.TOP ? -1 : 1), 0);
            return secondBlock.getBlockData() instanceof Door ? secondBlock : null;
        }
        else if(firstBlock.getBlockData() instanceof Chest chest && chest.getType() != Chest.Type.SINGLE) {
            if(chest.getType() == Chest.Type.RIGHT) {
                Block secondBlock = firstBlock.getRelative(HelperFunctions.BLOCK_FACE_LEFT.get(chest.getFacing()));
                return secondBlock.getBlockData() instanceof Chest c2 && c2.getType() == Chest.Type.LEFT ? secondBlock : null;
            }
            else if(chest.getType() == Chest.Type.LEFT) {
                Block secondBlock = firstBlock.getRelative(HelperFunctions.BLOCK_FACE_RIGHT.get(chest.getFacing()));
                return secondBlock.getBlockData() instanceof Chest c2 && c2.getType() == Chest.Type.RIGHT ? secondBlock : null;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "BlockLock{" +
                "owner=" + getOwner(block) +
                ", block=" + block +
                ", hopperLock=" + getHopperLock(block) +
                ", redstoneLock=" + getRedstoneLock(block) +
                ", secondBlock=" + secondBlock +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwner(), block.getLocation());
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(obj instanceof BlockLock bl) {
            return this.getOwner().equals(bl.getOwner()) && this.block.getLocation().equals(bl.block.getLocation());
        }
        return false;
    }

    public boolean save(ConfigurationSection blockLockSection) {
        if(blockLockSection == null) return false;
        blockLockSection.set("Material", this.getBlock().getType().name());
        blockLockSection.set("Location", this.getBlock().getLocation());
        blockLockSection.set("HopperLock", this.isHopperLock());
        blockLockSection.set("RedstoneLock", this.isRedstoneLock());

        if(!localFriends.isEmpty()) {
            ConfigurationSection localFriendsSection = blockLockSection.createSection("LocalFriends");
            for(UUID uuid : localFriends) {
                localFriendsSection.set(uuid.toString(), "");
            }
        }
        return true;
    }

    public static BlockLock load(UUID owner, ConfigurationSection blockLockSection) {
        if(owner == null || blockLockSection == null) return null;

        Material material = Material.getMaterial(blockLockSection.getString("Material"));
        Location location = blockLockSection.getLocation("Location");

        if(location == null || material == null) return null;
        if(!location.getBlock().getType().equals(material) || !BlockLockManager.LOCKABLE_BLOCKS.contains(material))
            return null;

        boolean hopperLock = blockLockSection.getBoolean("HopperLock");
        boolean redstoneLock = blockLockSection.getBoolean("RedstoneLock");

        ConfigurationSection localFriendsSection = blockLockSection.getConfigurationSection("LocalFriends");
        List<String> friendsList = new ArrayList<>();
        if(localFriendsSection != null) {
            friendsList.addAll(localFriendsSection.getKeys(false));
        }

        return new BlockLock(location.getBlock(),
                owner, hopperLock, redstoneLock,
                friendsList.stream().map(UUID::fromString).collect(Collectors.toSet()));
    }

    private static String getBlockLockMetaValueAsString(Block block, String key) {
        StringBuilder builder = new StringBuilder();
        builder.append(key);
        builder.append("=");
        List<MetadataValue> meta = block.getMetadata(key);
        builder.append(!meta.isEmpty() ? meta.getFirst().asString() : "null");
        return builder.toString();
    }

    public static String getBlockLockMeta(Block block) {
        return getBlockLockMetaValueAsString(block, META_DATA.LOCKED) +
                "\n" +
                getBlockLockMetaValueAsString(block, META_DATA.OWNER) +
                "\n" +
                getBlockLockMetaValueAsString(block, META_DATA.HOPPER_LOCK) +
                "\n" +
                getBlockLockMetaValueAsString(block, META_DATA.REDSTONE_LOCK) +
                "\n" +
                getBlockLockMetaValueAsString(block, META_DATA.SECOND_BLOCK);
    }

    public static boolean isBlockLock(Inventory inventory) {
        return inventory.getHolder() instanceof BlockInventoryHolder blockInventoryHolder && isBlockLock(blockInventoryHolder.getBlock());
    }

    public static boolean isBlockLock(Block block) {
        return block != null && block.hasMetadata(META_DATA.LOCKED);
    }

    public static UUID getOwner(Block block) {
        if(block != null && block.hasMetadata(META_DATA.OWNER)) {
            return UUID.fromString(block.getMetadata(META_DATA.OWNER).getFirst().asString());
        }
        return null;
    }

    public static Boolean getRedstoneLock(Block block) {
        if(block != null && block.hasMetadata(META_DATA.REDSTONE_LOCK)) {
            return block.getMetadata(META_DATA.REDSTONE_LOCK).getFirst().asBoolean();
        }
        return null;
    }

    public static boolean setRedstoneLock(Block block, boolean redstoneLock) {
        if(block.hasMetadata(META_DATA.REDSTONE_LOCK)) {
            block.setMetadata(META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(), redstoneLock));
            return true;
        }
        return false;
    }

    public static Boolean getHopperLock(Block block) {
        if(block != null && block.hasMetadata(META_DATA.HOPPER_LOCK)) {
            return block.getMetadata(META_DATA.HOPPER_LOCK).getFirst().asBoolean();
        }
        return null;
    }

    public static boolean setHopperLock(Block block, boolean hopperLock) {
        if(block.hasMetadata(META_DATA.HOPPER_LOCK)) {
            block.setMetadata(META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(), hopperLock));
            return true;
        }
        return false;
    }

    public static boolean isDoor(Block block) {
        return block.getBlockData() instanceof Door;
    }

    public static boolean isChest(Block block) {
        return block.getBlockData() instanceof Chest;
    }

    /**
     * @return 0: No Chest | 1: Single | 2: Left | 3: Right
     */
    public static int getDoubleChest(Block block) {
        return block.getBlockData() instanceof Chest chest ? switch(chest.getType()) {
            case SINGLE -> 1;
            case LEFT -> 2;
            case RIGHT -> 3;
        } : 0;
    }

    public boolean compareBlock(Block block) {
        return block != null && (block.equals(this.block) || block.equals(this.secondBlock));
    }

    public void synchSecondBlock(){
        this.secondBlock = getSecondBlock(block);
        if(secondBlock != null) {
            block.setMetadata(META_DATA.SECOND_BLOCK, new FixedMetadataValue(Manager.getInstance(), secondBlock.getLocation()));
            secondBlock.setMetadata(META_DATA.LOCKED, new FixedMetadataValue(Manager.getInstance(), secondBlock.getLocation()));
            secondBlock.setMetadata(META_DATA.OWNER, new FixedMetadataValue(Manager.getInstance(), getOwner().toString()));
            secondBlock.setMetadata(META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(), isHopperLock()));
            secondBlock.setMetadata(META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(), isRedstoneLock()));
            secondBlock.setMetadata(META_DATA.SECOND_BLOCK, new FixedMetadataValue(Manager.getInstance(), block.getLocation()));
        }
    }

    public void delete(boolean second) {
        if(blockLockManagerMenu != null) {
            HandlerList.unregisterAll(blockLockManagerMenu);
        }
        block.removeMetadata(META_DATA.LOCKED, Manager.getInstance());
        block.removeMetadata(META_DATA.OWNER, Manager.getInstance());
        block.removeMetadata(META_DATA.HOPPER_LOCK, Manager.getInstance());
        block.removeMetadata(META_DATA.REDSTONE_LOCK, Manager.getInstance());
        if(secondBlock != null) {
            block.removeMetadata(META_DATA.SECOND_BLOCK, Manager.getInstance());
            secondBlock.removeMetadata(META_DATA.SECOND_BLOCK, Manager.getInstance());
            if(second || isDoor()){
                secondBlock.removeMetadata(META_DATA.LOCKED, Manager.getInstance());
                secondBlock.removeMetadata(META_DATA.OWNER, Manager.getInstance());
                secondBlock.removeMetadata(META_DATA.HOPPER_LOCK, Manager.getInstance());
                secondBlock.removeMetadata(META_DATA.REDSTONE_LOCK, Manager.getInstance());
            }
        }
    }

    public boolean openManagerMenu(Player p, BlockLockManager blManager) {
        if(blockLockManagerMenu == null) {
            blockLockManagerMenu = new BlockLockManagerMenu(blManager, this);
            Manager.getInstance().getServer().getPluginManager().registerEvents(blockLockManagerMenu, Manager.getInstance());
        }
        return blockLockManagerMenu.open(p);
    }

    /**
     * @return 0: No Chest | 1: Single | 2: Left | 3: Right
     */
    public int getDoubleChest() {
        return getDoubleChest(block);
    }

    public boolean isChest() {
        return isChest(block);
    }

    public boolean isDoor() {
        return isDoor(block);
    }

    public boolean addFriend(UUID friend) {
        if(friend != getOwner() && localFriends.size() < BlockLockManager.MAX_LOCAL_FRIENDS) {
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

    public UUID getOwner() {
        return getOwner(block);
    }

    public boolean isHopperLock() {
        return Objects.requireNonNullElse(getHopperLock(block), false);
    }

    public void setHopperLock(boolean hopperLock) {
        if(this.block.getType() != Material.HOPPER) {
            setHopperLock(block, hopperLock);
        }
    }

    public boolean isRedstoneLock() {
        return Objects.requireNonNullElse(getRedstoneLock(block), false);
    }

    public void setRedstoneLock(boolean redstoneLock) {
        if(this.block.getType() != Material.HOPPER) {
            setRedstoneLock(block, redstoneLock);
        }
    }

    public Block getBlock() {
        return this.block;
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
