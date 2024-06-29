package other;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class BlockLogger implements Listener, ManagedPlugin
{
    private static final Material[] loggedMaterial = {Material.SWEET_BERRY_BUSH, Material.LIGHTNING_ROD};

    public BlockLogger ()
    {
    }

    @EventHandler
    public void onBlockPlace (BlockPlaceEvent event)
    {
        boolean blockPlaced = false;
        String blockPlacedStr = null;

        for (int i = 0; i < loggedMaterial.length; i++)
        {
            if (event.getBlockPlaced().getType().equals(loggedMaterial[i]))
            {
                blockPlaced = true;
                blockPlacedStr = event.getBlockPlaced().getType().name();
                break;
            }
        }

        if (blockPlaced)
        {

            Bukkit.getLogger().info(blockPlacedStr + ": " + event.getPlayer().toString() + " " + event.getBlockPlaced().getLocation().toString());

            FileWriter fw = null;
            try
            {
                fw = new FileWriter("plugins/DasGesetz/" + blockPlacedStr + ".txt", true);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
                String dateStr = dateFormat.format(new Date());

                fw.write("[" + dateStr + "]: ");

                fw.write(event.getPlayer().getName());
                fw.write("\n");
                fw.write(event.getBlockPlaced().getLocation().toString());
                fw.write("\n\n");

                fw.flush();
                fw.close();

            } catch (Exception e)
            {
                // e.printStackTrace();
                // Bukkit.getServer().getConsoleSender().sendMessage();
                Bukkit.getLogger().warning(e.getLocalizedMessage());
            }
        }
    }

    public void deleteLogs ()
    {
        for (int i = 0; i < loggedMaterial.length; i++)
        {
            FileWriter fw = null;
            String blockPlacedStr = loggedMaterial[i].name();
            try
            {
                fw = new FileWriter("plugins/DasGesetz/" + blockPlacedStr + ".txt", false);
                fw.write("");
                fw.flush();
                fw.close();
            } catch (Exception e)
            {
                // e.printStackTrace();
                // Bukkit.getServer().getConsoleSender().sendMessage();
                Bukkit.getLogger().warning(e.getLocalizedMessage());
            }
        }
    }

    @Override
    public boolean onEnable ()
    {
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        return true;
    }

    @Override
    public void onDisable ()
    {
        deleteLogs();
    }

    @Override
    public String getName ()
    {
        return "BlockLogger";
    }

    @Override
    public void createDefaultConfig (FileConfiguration config)
    {
    }
}