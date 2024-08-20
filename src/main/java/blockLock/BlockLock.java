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
            createBlockLock(secondBlock, BlockLock.getOwner(block), false);
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
                friendsList.stream().map(UUID::fromString).collect(Collectors.toSet()), false/*TODO: Evtl. true*/) ? location.getBlock() : null;
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

    public static void synchSecondBlock(Block block) {
        //TODO
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

    public static Set<UUID> getLocalFriends(Block block) {
        if(block != null && block.hasMetadata(META_DATA.LOCAL_FRIENDS)) {
            return (Set<UUID>) block.getMetadata(META_DATA.LOCAL_FRIENDS).getFirst().value(); //TODO
        }
        return null;
    }

    public static boolean setLocalFriends(Block block, Set<UUID> friends) {
        if(block != null && block.hasMetadata(META_DATA.LOCAL_FRIENDS)) {
            block.setMetadata(META_DATA.LOCAL_FRIENDS, new FixedMetadataValue(Manager.getInstance(), friends));
            return true;
        }
        return false;
    }

    public static boolean removeLocalFriend(Block block, UUID friend){
        Set<UUID> localFriends = getLocalFriends(block);
        if(localFriends != null && localFriends.contains(friend)){
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

    public static boolean switchRedstoneLock(Block block) {
        if(block.hasMetadata(META_DATA.REDSTONE_LOCK)) {
            block.setMetadata(META_DATA.REDSTONE_LOCK, new FixedMetadataValue(Manager.getInstance(),
                    !block.getMetadata(META_DATA.REDSTONE_LOCK).getFirst().asBoolean()));
        }
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

    public static boolean switchHopperLock(Block block) {
        if(block.hasMetadata(META_DATA.HOPPER_LOCK)) {
            block.setMetadata(META_DATA.HOPPER_LOCK, new FixedMetadataValue(Manager.getInstance(),
                    !block.getMetadata(META_DATA.HOPPER_LOCK).getFirst().asBoolean()));
            return true;
        }
        return false;
    }

    public static Block getSecondBlock(Block block) {
        if(block.hasMetadata(META_DATA.SECOND_BLOCK) && block.getMetadata(META_DATA.SECOND_BLOCK).getFirst().value() instanceof Block secondBlock) {
            return secondBlock;
        }
        return null;
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
        block.removeMetadata(META_DATA.LOCKED, Manager.getInstance());
        block.removeMetadata(META_DATA.OWNER, Manager.getInstance());
        block.removeMetadata(META_DATA.HOPPER_LOCK, Manager.getInstance());
        block.removeMetadata(META_DATA.REDSTONE_LOCK, Manager.getInstance());
        if(block.hasMetadata(META_DATA.SECOND_BLOCK)) {
            Block secondBlock = findSecondBlock(block);
            block.removeMetadata(META_DATA.SECOND_BLOCK, Manager.getInstance());
            secondBlock.removeMetadata(META_DATA.SECOND_BLOCK, Manager.getInstance());
            if(second || isDoor(block)) {
                secondBlock.removeMetadata(META_DATA.LOCKED, Manager.getInstance());
                secondBlock.removeMetadata(META_DATA.OWNER, Manager.getInstance());
                secondBlock.removeMetadata(META_DATA.HOPPER_LOCK, Manager.getInstance());
                secondBlock.removeMetadata(META_DATA.REDSTONE_LOCK, Manager.getInstance());
            }
        }
    }
}
