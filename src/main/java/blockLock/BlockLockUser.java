package blockLock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;

@Deprecated(forRemoval = true)
public class BlockLockUser
{
    private UUID uuid;
    private List<BlockLock> blockLocks;

    public BlockLockUser(UUID uuid) {
        this.uuid = uuid;
        this.blockLocks = new ArrayList<>();
    }

    public BlockLock createBlockLock(Block b, BlockLockManager blm) {
        BlockLock bl = new BlockLock(b, this.uuid);
        blockLocks.add(bl);

        /*
         * int doubleChestIndex = bl.checkIfDoubleChest(); BlockLockManager.sendMessage(uuid, "doubleChestIndex: " + doubleChestIndex); if (doubleChestIndex > 1) { try { Block block = null;
         * if (doubleChestIndex == 2) { if (BlockLock.checkIfDoubleChest(b.getRelative(1, 0, 0)) == 3) block = b.getRelative(1, 0, 0); else if (BlockLock.checkIfDoubleChest(b.getRelative(-1,
         * 0, 0)) == 3) block = b.getRelative(-1, 0, 0); else if (BlockLock.checkIfDoubleChest(b.getRelative(0, 0, 1)) == 3) block = b.getRelative(0, 0, 1); else if
         * (BlockLock.checkIfDoubleChest(b.getRelative(0, 0, -1)) == 3) block = b.getRelative(0, 0, -1); } else if (doubleChestIndex == 3) { if (BlockLock.checkIfDoubleChest(b.getRelative(1,
         * 0, 0)) == 2) block = b.getRelative(1, 0, 0); else if (BlockLock.checkIfDoubleChest(b.getRelative(-1, 0, 0)) == 2) block = b.getRelative(-1, 0, 0); else if
         * (BlockLock.checkIfDoubleChest(b.getRelative(0, 0, 1)) == 2) block = b.getRelative(0, 0, 1); else if (BlockLock.checkIfDoubleChest(b.getRelative(0, 0, -1)) == 2) block =
         * b.getRelative(0, 0, -1); }
         *
         * if (block != null) { BlockLockManager.sendMessage(uuid, "Double Chest: " + block.getLocation().toString()); BlockLock bl2 = new BlockLock(block, this); blockLocks.add(bl2);
         * bl2.setBlockLockManagerMenu(bl.getBlockLockManagerMenu()); bl.setSecondBlockLock(bl2); bl2.setSecondBlockLock(bl); } } catch (Exception e) {
         * Bukkit.getLogger().severe("createBlockLock: Double Chest Exception: " + e.getLocalizedMessage()); BlockLockManager.sendMessage(uuid, "createBlockLock: Double Chest Exception: " +
         * e.getLocalizedMessage(), true); } }
         */

        if(bl.isDoor()) {
            try {
                if(bl.getBlock().getRelative(0, 1, 0).getBlockData() instanceof Door) {
                    BlockLock bl2 = new BlockLock(b.getRelative(0, 1, 0), this.uuid);
                    blockLocks.add(bl2);
                    bl.setSecondBlockLock(bl2);
                    bl2.setSecondBlockLock(bl);
                }
                else if(bl.getBlock().getRelative(0, -1, 0).getBlockData() instanceof Door) {
                    BlockLock bl2 = new BlockLock(b.getRelative(0, -1, 0), this.uuid);
                    blockLocks.add(bl2);
                    bl.setSecondBlockLock(bl2);
                    bl2.setSecondBlockLock(bl);
                }
            } catch(Exception e) {
                Bukkit.getLogger().severe("createBlockLock: Door Exception: " + e.getLocalizedMessage());
            }
        }

        return bl;
    }
}
