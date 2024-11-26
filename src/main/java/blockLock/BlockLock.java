package blockLock;

import manager.Manager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
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
        String LOCAL_FRIENDS = "blockLockLocalFriends";
        String REDSTONE_LOCK = "blockLockRedstoneLock";
        String HOPPER_LOCK = "blockLockHopperLock";
        String SECOND_BLOCK = "secondBlock";
    }

    public static boolean createBlockLock(Block block, UUID owner, boolean secondBlock) {
        if(block != null && owner != null && !BlockLock.isBlockLock(block)) {
            return createBlockLock(block, owner, block.getType() != Material.HOPPER, block.getType() != Material.HOPPER, new HashSet<>(), secondBlock);
        }
        return false;
    }

    private static boolean createBlockLock(Block block, UUID owner, boolean hopperLock, boolean redstoneLock, Set<UUID> localFriends, boolean secondBlock) {
        if(block != null) {
            block.setMetadata(META_DATA.LOCKED, new FixedMetadataValue(Manager.getInstance(), block.getType()));
            block.setMetadata(META_DATA.OWNER, new FixedMetadataValue(Manager.getInstance(), owner));
            block.setMetadata(META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(), hopperLock));
            block.setMetadata(META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(), redstoneLock));
            block.setMetadata(META_DATA.LOCAL_FRIENDS, new FixedMetadataValue(Manager.getInstance(), localFriends));
            if(secondBlock) {
                createSecondBlockLock(block);
            }
            return true;
        }
        return false;
    }

    private static boolean createSecondBlockLock(Block block) {
        Block secondBlock = findSecondBlock(block);
        if(secondBlock != null) {
            block.setMetadata(META_DATA.SECOND_BLOCK, new FixedMetadataValue(Manager.getInstance(), secondBlock));
            secondBlock.setMetadata(META_DATA.SECOND_BLOCK, new FixedMetadataValue(Manager.getInstance(), block));
            return true;
        }
        return false;
    }

    private static Block findSecondBlock(Block firstBlock) {
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


    public static boolean save(ConfigurationSection blockLockSection, Block block) {
        if(blockLockSection == null || !isBlockLock(block)) return false;
        blockLockSection.set("Material", block.getType().name());
        blockLockSection.set("Location", block.getLocation());
        blockLockSection.set("HopperLock", getHopperLock(block));
        blockLockSection.set("RedstoneLock", getRedstoneLock(block));

        Set<UUID> localFriends = getLocalFriends(block);
        if(localFriends != null && !localFriends.isEmpty()) {
            ConfigurationSection localFriendsSection = blockLockSection.createSection("LocalFriends");
            for(UUID uuid : localFriends) {
                localFriendsSection.set(uuid.toString(), "");
            }
        }
        return true;
    }

    public static Block load(UUID owner, ConfigurationSection blockLockSection) {
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

        return createBlockLock(location.getBlock(),
                owner, hopperLock, redstoneLock,
                friendsList.stream().map(UUID::fromString).collect(Collectors.toSet()), true) ? location.getBlock() : null;
    }

    private static String getBlockLockMetaValueAsString(Block block, String key) {
        StringBuilder builder = new StringBuilder();
        builder.append(key);
        builder.append("=");
        List<MetadataValue> meta = block.getMetadata(key);
        builder.append(!meta.isEmpty() ? meta.getFirst().asString() : "null");
        return builder.toString();
    }

    private static boolean hasMetadata(Block block, String key) {
        if(block == null || key == null) return false;
        if(block.hasMetadata(META_DATA.SECOND_BLOCK) && !block.hasMetadata(META_DATA.OWNER)) { // block is second
            if(block.getMetadata(META_DATA.SECOND_BLOCK).getFirst().value() instanceof Block mainBlock) {
                return mainBlock.hasMetadata(key);
            }
        }
        return block.hasMetadata(key);
    }

    private static List<MetadataValue> getMetadata(Block block, String key) {
        if(block == null || key == null) return null;
        if(block.hasMetadata(META_DATA.SECOND_BLOCK) && !block.hasMetadata(META_DATA.OWNER)) { // block is second
            if(block.getMetadata(META_DATA.SECOND_BLOCK).getFirst().value() instanceof Block mainBlock) {
                return mainBlock.getMetadata(key);
            }
        }
        return block.getMetadata(key);
    }

    private static void setMetadata(Block block, String key, MetadataValue value) {
        if(block == null || key == null || value == null) return;
        if(isSecondBlock(block)) { // block is second
            if(block.getMetadata(META_DATA.SECOND_BLOCK).getFirst().value() instanceof Block mainBlock) {
                mainBlock.setMetadata(key, value);
            }
        }
        block.setMetadata(key, value);
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
        return hasMetadata(block, META_DATA.LOCKED);
    }

    public static Block getSecondBlock(Block block) {
        if(block.hasMetadata(META_DATA.SECOND_BLOCK) && block.getMetadata(META_DATA.SECOND_BLOCK).getFirst().value() instanceof Block secondBlock) {
            return secondBlock;
        }
        return null;
    }

    public static boolean isSecondBlock(Block block) {
        return block.hasMetadata(META_DATA.SECOND_BLOCK) && !block.hasMetadata(META_DATA.OWNER);
    }

    public static UUID getOwner(Block block) {
        if(hasMetadata(block, META_DATA.OWNER)) {
            return UUID.fromString(getMetadata(block, META_DATA.OWNER).getFirst().asString());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static Set<UUID> getLocalFriends(Block block) {
        if(block == null) return null;
        if(hasMetadata(block, META_DATA.LOCAL_FRIENDS)) {
            return (Set<UUID>) getMetadata(block, META_DATA.LOCAL_FRIENDS).getFirst().value();
        }
        return isBlockLock(block) ? Collections.emptySet() : null;
    }

    public static boolean setLocalFriends(Block block, Set<UUID> friends) {
        if(hasMetadata(block, META_DATA.LOCAL_FRIENDS)) {
            setMetadata(block, META_DATA.LOCAL_FRIENDS, new FixedMetadataValue(Manager.getInstance(), friends));
            return true;
        }
        return false;
    }

    public static boolean removeLocalFriend(Block block, UUID friend) {
        Set<UUID> localFriends = getLocalFriends(block);
        if(localFriends != null && localFriends.contains(friend)) {
            localFriends.remove(friend);
            return setLocalFriends(block, localFriends);
        }
        return false;
    }

    public static Boolean hasPermissionToOpen(Block block, UUID uuid) {
        if(isBlockLock(block)) {
            return getOwner(block).equals(uuid) || getLocalFriends(block).contains(uuid);
        }
        else return null;
    }

    public static Boolean getRedstoneLock(Block block) {
        if(hasMetadata(block, META_DATA.REDSTONE_LOCK)) {
            return getMetadata(block, META_DATA.REDSTONE_LOCK).getFirst().asBoolean();
        }
        return null;
    }

    public static boolean setRedstoneLock(Block block, boolean redstoneLock) {
        if(hasMetadata(block, META_DATA.REDSTONE_LOCK)) {
            setMetadata(block, META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(), redstoneLock));
            return true;
        }
        return false;
    }

    public static boolean switchRedstoneLock(Block block) {
        if(hasMetadata(block, META_DATA.REDSTONE_LOCK)) {
            setMetadata(block, META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(),
                    !getMetadata(block, META_DATA.REDSTONE_LOCK).getFirst().asBoolean()));
            return true;
        }
        return false;
    }

    public static Boolean getHopperLock(Block block) {
        if(hasMetadata(block, META_DATA.HOPPER_LOCK)) {
            return getMetadata(block, META_DATA.HOPPER_LOCK).getFirst().asBoolean();
        }
        return null;
    }

    public static boolean setHopperLock(Block block, boolean hopperLock) {
        if(hasMetadata(block, META_DATA.HOPPER_LOCK)) {
            setMetadata(block, META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(), hopperLock));
            return true;
        }
        return false;
    }

    public static boolean switchHopperLock(Block block) {
        if(hasMetadata(block, META_DATA.HOPPER_LOCK)) {
            setMetadata(block, META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(),
                    !getMetadata(block, META_DATA.HOPPER_LOCK).getFirst().asBoolean()));
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

    public static void delete(Block block, boolean second) {
        if(isSecondBlock(block)) {
            block = getSecondBlock(block);
        }
        block.removeMetadata(META_DATA.LOCKED, Manager.getInstance());
        block.removeMetadata(META_DATA.OWNER, Manager.getInstance());
        block.removeMetadata(META_DATA.HOPPER_LOCK, Manager.getInstance());
        block.removeMetadata(META_DATA.REDSTONE_LOCK, Manager.getInstance());
        if(block.hasMetadata(META_DATA.SECOND_BLOCK)) {
            Block secondBlock = getSecondBlock(block);
            if(secondBlock != null) {
                secondBlock.removeMetadata(META_DATA.SECOND_BLOCK, Manager.getInstance());
            }
            block.removeMetadata(META_DATA.SECOND_BLOCK, Manager.getInstance());
        }
    }
}
