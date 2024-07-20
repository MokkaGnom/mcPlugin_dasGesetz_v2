package utility;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public final class BlockHelper
{
    private static final int[][] OFFSETS = {
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
}
