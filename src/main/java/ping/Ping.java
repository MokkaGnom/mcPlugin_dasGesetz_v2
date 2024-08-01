package ping;

import manager.Manager;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import utility.HelperFunctions;

import java.util.Objects;

public class Ping
{
    public static final Color DEFAULT_COLOR = Color.fromRGB(255, 183, 197);
    public static final String PING_NAME_FORMAT = "%s's Ping";
    public static final String PING_NAME_FORMAT_2 = "%s' Ping";

    private final AreaEffectCloud effectCloud;
    private final Block block;

    public Ping(Block block, int time, String playerName, String colorHex) {
        this.block = HelperFunctions.getRelativeBlocks(block, Material.AIR).getFirst();
        this.effectCloud = (AreaEffectCloud) this.block.getWorld().spawnEntity(this.block.getLocation(), EntityType.AREA_EFFECT_CLOUD);
        this.effectCloud.setColor(Objects.requireNonNullElse(PingManager.getColorFromHexString(colorHex), DEFAULT_COLOR));
        this.effectCloud.setDuration((int) Manager.convertSecondsToTicks(time / 1000.d));
        this.effectCloud.setCustomName(String.format(
                (playerName.substring(playerName.length() - 1).equalsIgnoreCase("s") ? PING_NAME_FORMAT_2 : PING_NAME_FORMAT),
                playerName));
        this.effectCloud.setCustomNameVisible(true);
        this.effectCloud.setGlowing(true);
        this.effectCloud.setGravity(false);
        this.effectCloud.setRadius(1);
        this.effectCloud.setSilent(true);
    }

    public AreaEffectCloud getEffectCloud() {
        return effectCloud;
    }

    public Block getBlock() {
        return block;
    }
}
