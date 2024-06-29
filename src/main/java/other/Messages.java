package other;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Calendar;

public class Messages implements Listener, ManagedPlugin
{
    private final String message;

    public Messages ()
    {
        this.message = Manager.getInstance().getConfig().getString("Messages.Message");
    }

    @EventHandler
    public void OnPlayerJoin (PlayerJoinEvent event)
    {
        // greetPlayer(event.getPlayer());
        event.getPlayer().sendMessage(message);
    }

    public void greetPlayer (Player p)
    {
        String message = "";
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour < 12) // Morgen
        {
            message = "Guten Morgen";
        } else if (hour >= 12 && hour < 15) // Mittag
        {
            message = "Guten Mittag";
        } else if (hour >= 15 && hour < 18) // Nachmittag
        {
            message = "Guten Nachmittag";
        } else if (hour >= 18 && hour < 23) // Abend
        {
            message = "Guten Abend";
        } else
        {
            message = "Hallo";
        }

        p.sendMessage(message + " " + p.getName() + "!");
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
    }

    @Override
    public String getName ()
    {
        return "Messages";
    }

    @Override
    public void createDefaultConfig (FileConfiguration config)
    {
        config.addDefault("Messages.Message", "Check out the plugin: https://github.com/MokkaGnom/mcPlugin_dasGesetz_v2");
    }
}
