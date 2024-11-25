package manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;

import java.util.List;

public class ManagerEvents implements Listener, ManagedPlugin
{
    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        if(event.getWorld().getName().equals(Bukkit.getWorlds().getFirst().getName())) {
            for(ManagedPlugin plugin : Manager.getInstance().getSubPlugins().keySet()) {
                if(plugin instanceof Saveable saveable) {
                    saveable.saveToFile();
                }
            }
        }
    }

    @Override
    public boolean onEnable() {
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public String getName() {
        return "DG-Manager-E";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.GOLD;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.dgManagerPermission");
    }
}
