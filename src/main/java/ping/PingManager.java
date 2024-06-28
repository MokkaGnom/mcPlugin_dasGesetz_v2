package ping;

import manager.Manager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class PingManager implements Listener
{
    private final String cooldownMetaKey = "pingTime";
    private final String colorMetaKey = "pingColor";
    private Manager manager;
    private int time;
    private int cooldown;

    public PingManager(Manager manager, int time, int cooldown)
    {
        this.manager = manager;
        this.time = time;
        this.cooldown = cooldown;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        Player p = event.getPlayer();
        if (p.hasPermission("dg.pingPermission") && checkCooldown(p) && p.getInventory().getItemInOffHand().getType().equals(Material.STICK)
                && (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)))
        {
            Block b = p.getTargetBlock(null, 255);
            String color = null;
            if (p.hasMetadata(colorMetaKey))
                color = p.getMetadata(colorMetaKey).get(0).asString();
            new Ping(this, b, p.getName(), color);
            p.removeMetadata(cooldownMetaKey, manager);
            p.setMetadata(cooldownMetaKey, new FixedMetadataValue(manager, System.currentTimeMillis()));
            p.sendMessage("You pinged at " + b.getX() + ", " + b.getY() + ", " + b.getZ());
        }
    }

    public void setPlayerColor(Player p, String color)
    {
        if (p.hasMetadata(colorMetaKey))
            p.removeMetadata(colorMetaKey, manager);

        p.setMetadata(colorMetaKey, new FixedMetadataValue(manager, color));
    }

    public boolean checkCooldown(Player p)
    {
        if (p.hasMetadata(cooldownMetaKey))
        {
            return p.getMetadata(cooldownMetaKey).get(0).asLong() + cooldown <= System.currentTimeMillis();
        }
        else
        {
            p.setMetadata(cooldownMetaKey, new FixedMetadataValue(manager, System.currentTimeMillis()));
            return true;
        }
    }

    public int getTime()
    {
        return time;
    }

    public int getCooldown()
    {
        return cooldown;
    }
}
