package farming;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.permissions.Permissible;

import java.util.Set;

public class EasyFarming implements Listener, ManagedPlugin
{
    public static final Set<Material> ALLOWED_BLOCKS = Set.of(Material.BEETROOTS, Material.WHEAT, Material.CARROTS, Material.POTATOES);

    public EasyFarming() {
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(hasPermission(event.getPlayer()) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            Block b = event.getClickedBlock();
            Material type = b.getType();
            if(ALLOWED_BLOCKS.contains(type) && b.getBlockData() instanceof Ageable ageable) {
                if(ageable.getAge() == ageable.getMaximumAge()) {
                    if(b.breakNaturally()) {
                        b.setType(type, true);
                    }
                }
            }
        }
    }

    @Override
    public boolean hasPermission(Permissible permissible) {
        return permissible.hasPermission("dg.easyFarmingPermission");
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
        return "EasyFarming";
    }
}
