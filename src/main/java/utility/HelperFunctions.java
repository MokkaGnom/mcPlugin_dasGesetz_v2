package utility;

import manager.ManagedPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface HelperFunctions
{
    Map<BlockFace, BlockFace> BLOCK_FACE_RIGHT = Map.of(
            BlockFace.NORTH, BlockFace.EAST,
            BlockFace.EAST, BlockFace.SOUTH,
            BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.WEST, BlockFace.NORTH
    );
    Map<BlockFace, BlockFace> BLOCK_FACE_LEFT = Map.of(
            BlockFace.NORTH, BlockFace.WEST,
            BlockFace.WEST, BlockFace.SOUTH,
            BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.EAST, BlockFace.NORTH
    );

    int[][] OFFSETS = {
            {0, 1, 0},
            {0, -1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    static List<Block> getRelativeBlocks(Block firstBlock, Material material) {
        List<Block> blocks = new ArrayList<Block>();
        for(int[] offset : OFFSETS) {
            Block relativeBlock = firstBlock.getRelative(offset[0], offset[1], offset[2]);
            if(relativeBlock.getType().equals(material)) {
                blocks.add(relativeBlock);
            }
        }
        return blocks;
    }

    static Block getTargetBlock(LivingEntity entity) {
        return entity.getTargetBlock(null, 255);
    }

    static Entity getTargetEntity(Location startLocation, Vector direction, int maxDistance) {
        if(startLocation == null || startLocation.getWorld() == null || direction == null || maxDistance <= 0)
            return null;
        RayTraceResult result = startLocation.getWorld().rayTraceEntities(startLocation, direction, maxDistance);
        if(result != null) {
            return result.getHitEntity();
        }
        return null;
    }

    static boolean isArgumentTrue(String argument) {
        return !(ManagedPlugin.ENABLE_STRINGS.stream().filter(s -> s.equalsIgnoreCase(argument)).toList().isEmpty());
    }

    static long convertSecondsToTicks(double seconds) {
        return (long) (seconds * (double) Bukkit.getServerTickManager().getTickRate());
    }
}
