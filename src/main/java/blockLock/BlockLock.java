package blockLock;

// Bukkit:

import manager.Manager;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BlockLock
{
	private final UUID owner;
	private final Set<UUID> friends;
	private final Block block;
	private boolean hopperLock;
	private boolean redstoneLock;
	private boolean blockBelowLock;
	private BlockLock secondBlockLock;
	private BlockLockManagerMenu blmm;

	public BlockLock(Block block, UUID owner)
	{
		block.setMetadata(BlockLockManager.META_DATA.BLOCK.LOCK, new FixedMetadataValue(Manager.getInstance(), block.getLocation().toString()));
		block.setMetadata(BlockLockManager.META_DATA.BLOCK.OWNER, new FixedMetadataValue(Manager.getInstance(), owner.toString()));
		this.block = block;
		this.blmm = null;
		this.owner = owner;
		this.friends = new HashSet<>();
		this.hopperLock = true;
		this.redstoneLock = true;
		this.blockBelowLock = true;
		secondBlockLock = null;
	}

	public void unlock()
	{
		block.removeMetadata(BlockLockManager.META_DATA.BLOCK.LOCK, Manager.getInstance());
		block.removeMetadata(BlockLockManager.META_DATA.BLOCK.OWNER, Manager.getInstance());
	}

	public boolean createManagerMenu(BlockLockManager blManager)
	{
		if (blmm == null)
		{
			this.blmm = new BlockLockManagerMenu(blManager, this);
			Manager.getInstance().getServer().getPluginManager().registerEvents(blmm, Manager.getInstance());
			return true;
		}
		return false;
	}

	public boolean openManagerInventory(Player p)
	{
		return blmm.open(p);
	}

	/**
	 * 
	 * @return 0: No Chest | 1: Single | 2: Right | 3: Left
	 */
	static public int checkIfDoubleChest(Block b)
	{
		if (b.getBlockData() instanceof Chest chest)
		{
			Chest.Type type = chest.getType();
			if (type.equals(Chest.Type.SINGLE))
				return 1;
			else if (type.equals(Chest.Type.LEFT))
				return 2;
			else if (type.equals(Chest.Type.RIGHT))
				return 3;
		}
		return 0;
	}

	/**
	 * 
	 * @return 0: No Chest | 1: Single | 2: Right | 3: Left
	 */
	public int checkIfDoubleChest()
	{
		return checkIfDoubleChest(getBlock());
	}

	public boolean checkIfDoor()
	{
		return (getBlock().getBlockData() instanceof Door);
	}

	public boolean addFriend(UUID friend)
	{
		if (!friends.contains(friend))
		{
			friends.add(friend);
			return true;
		}
		return false;
	}

	public boolean removeFriend(UUID friend)
	{
		return friends.remove(friend);
	}

	public boolean checkIfFriend(UUID uuid) {
		return friends.contains(uuid);
	}

	// Getter/Setter

	public boolean isHopperLock()
	{
		return hopperLock;
	}

	public void setHopperLock(boolean hopperLock)
	{
		this.hopperLock = hopperLock;
	}

	public boolean isRedstoneLock()
	{
		return redstoneLock;
	}

	public void setRedstoneLock(boolean redstoneLock)
	{
		this.redstoneLock = redstoneLock;
	}

	public boolean isBlockBelowLock()
	{
		return blockBelowLock;
	}

	public void setBlockBelowLock(boolean blockBelowLock)
	{
		this.blockBelowLock = blockBelowLock;
	}

	public Block getBlock()
	{
		return this.block;
	}

	public UUID getOwner()
	{
		return owner;
	}

	public Inventory getInventory()
	{
		if (getBlock().getState() instanceof InventoryHolder inventoryHolder)
		{
			return inventoryHolder.getInventory();
		}
		return null;
	}

	public Set<UUID> getLocalFriends()
	{
		return friends;
	}

	public BlockLockManagerMenu getBlockLockManagerMenu()
	{
		return blmm;
	}

	public void setBlockLockManagerMenu(BlockLockManagerMenu blmm)
	{
		this.blmm = blmm;
	}

	public BlockLock getSecondBlockLock()
	{
		return secondBlockLock;
	}

	public void setSecondBlockLock(BlockLock bl)
	{
		secondBlockLock = bl;
	}

}
