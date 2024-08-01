package utility;

import manager.ManagedPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HelperFunctions
{
    public static final Map<BlockFace, BlockFace> BLOCK_FACE_RIGHT = Map.of(
            BlockFace.NORTH, BlockFace.EAST,
            BlockFace.EAST, BlockFace.SOUTH,
            BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.WEST, BlockFace.NORTH
    );
    public static final Map<BlockFace, BlockFace> BLOCK_FACE_LEFT = Map.of(
            BlockFace.NORTH, BlockFace.WEST,
            BlockFace.WEST, BlockFace.SOUTH,
            BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.EAST, BlockFace.NORTH
    );

    public static final int[][] OFFSETS = {
            {0, 1, 0},
            {0, -1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    public static List<Block> getRelativeBlocks(Block firstBlock, Material material) {
        List<Block> blocks = new ArrayList<Block>();
        for(int[] offset : OFFSETS) {
            Block relativeBlock = firstBlock.getRelative(offset[0], offset[1], offset[2]);
            if(relativeBlock.getType().equals(material)) {
                blocks.add(relativeBlock);
            }
        }
        return blocks;
    }

    public static Block getTargetBlock(LivingEntity entity) {
        return entity.getTargetBlock(null, 255);
    }

    public static boolean isArgumentTrue(String argument) {
        return !(ManagedPlugin.ENABLE_STRINGS.stream().filter(s -> s.equalsIgnoreCase(argument)).toList().isEmpty());
    }
}
