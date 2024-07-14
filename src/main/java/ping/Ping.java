package ping;

import manager.Manager;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;

import java.util.Objects;

public class Ping
{
    public static final Color DEFAULT_COLOR = Color.fromRGB(255, 183, 197);
    public static final String PING_NAME_FORMAT = "%s's Ping";
    public static final int[][] OFFSETS = {
            {0, 1, 0},
            {0, -1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1}
    };

    public Ping(Block block, int time, String playerName, String colorHex) {
        Block b = getAir(block);
        AreaEffectCloud aec = (AreaEffectCloud) b.getWorld().spawnEntity(b.getLocation(), EntityType.AREA_EFFECT_CLOUD);
        aec.setColor(Objects.requireNonNullElse(PingManager.getColorFromHexString(colorHex), DEFAULT_COLOR));
        aec.setDuration((int) Manager.convertSecondsToTicks(time / 1000.d));
        aec.setCustomName(String.format(PING_NAME_FORMAT, playerName));
        aec.setCustomNameVisible(true);
        aec.setGlowing(true);
        aec.setGravity(false);
        aec.setRadius(1);
        aec.setSilent(true);
    }

    public Block getAir(Block b) {
        for(int[] offset : OFFSETS) {
            Block relativeBlock = b.getRelative(offset[0], offset[1], offset[2]);
            if(relativeBlock.getType().equals(Material.AIR)) {
                return relativeBlock;
            }
        }
        return b;
    }
}
