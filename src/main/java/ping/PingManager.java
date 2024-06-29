package ping;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class PingManager implements Listener, ManagedPlugin
{
    private final String cooldownMetaKey = "pingTime";
    private final String colorMetaKey = "pingColor";
    private int time;
    private int cooldown;

    public PingManager ()
    {
        this.time = Manager.getInstance().getConfig().getInt("Ping.Duration");
        this.cooldown = Manager.getInstance().getConfig().getInt("Ping.Cooldown");
    }

    @EventHandler
    public void onPlayerInteract (PlayerInteractEvent event)
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
            p.removeMetadata(cooldownMetaKey, Manager.getInstance());
            p.setMetadata(cooldownMetaKey, new FixedMetadataValue(Manager.getInstance(), System.currentTimeMillis()));
            p.sendMessage("You pinged at " + b.getX() + ", " + b.getY() + ", " + b.getZ());
        }
    }

    public void setPlayerColor (Player p, String color)
    {
        if (p.hasMetadata(colorMetaKey))
            p.removeMetadata(colorMetaKey, Manager.getInstance());

        p.setMetadata(colorMetaKey, new FixedMetadataValue(Manager.getInstance(), color));
    }

    public boolean checkCooldown (Player p)
    {
        if (p.hasMetadata(cooldownMetaKey))
        {
            return p.getMetadata(cooldownMetaKey).get(0).asLong() + cooldown <= System.currentTimeMillis();
        } else
        {
            p.setMetadata(cooldownMetaKey, new FixedMetadataValue(Manager.getInstance(), System.currentTimeMillis()));
            return true;
        }
    }

    public int getTime ()
    {
        return time;
    }

    public int getCooldown ()
    {
        return cooldown;
    }

    @Override
    public boolean onEnable ()
    {
        PingCommands pingCommands = new PingCommands(this);

        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        try
        {
            Manager.getInstance().getCommand(PingCommands.COMMAND).setExecutor(pingCommands);
            Manager.getInstance().getCommand(PingCommands.COMMAND).setTabCompleter(pingCommands);
            return true;
        } catch (NullPointerException e)
        {
            Manager.getInstance().sendErrorMessage(e.getMessage());
            onDisable();
            return false;
        }
    }

    @Override
    public void onDisable ()
    {
    }

    @Override
    public String getName ()
    {
        return "Ping";
    }

    @Override
    public void createDefaultConfig (FileConfiguration config)
    {
        config.addDefault("Ping.Duration", 5000);
        config.addDefault("Ping.Cooldown", 5000);
    }
}
