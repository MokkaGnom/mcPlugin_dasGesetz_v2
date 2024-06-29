package farming;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;

public class EasyFarming implements Listener, ManagedPlugin
{
	private static final List<Material> allowedBlocks = Arrays.asList(Material.BEETROOTS, Material.WHEAT, Material.CARROTS, Material.POTATOES);

	public EasyFarming()
	{
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
	{
		if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
		{
			Block b = event.getClickedBlock();
			int index = allowedBlocks.indexOf(b.getType());
			if (index > -1 && event.getPlayer().hasPermission("dg.easyFarmingPermission"))
			{
				Ageable ageable = (Ageable) b.getBlockData();
				if (ageable.getAge() == ageable.getMaximumAge())
				{
					if (b.breakNaturally())
					{
						b.setType(allowedBlocks.get(index), true);
					}
				}
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

	}

	@Override
	public String getName ()
	{
		return "EasyFarming";
	}

	@Override
	public void createDefaultConfig (FileConfiguration config)
	{

	}
}
