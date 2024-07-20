package other;

import manager.ManagedPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.permissions.Permissible;

public class VillagerCreator implements Listener, ManagedPlugin
{

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {

    }

    @Override
    public boolean hasPermission(Permissible permissible) {
        return false;
    }

    @Override
    public boolean onEnable() {
        return false;
    }

    @Override
    public void onDisable() {

    }

    @Override
    public String getName() {
        return "VillagerCreator";
    }
}
