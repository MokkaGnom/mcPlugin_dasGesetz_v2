package playerTrophy;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class PlayerTrophyManager implements Listener, ManagedPlugin
{
    public static final String PROBABILITY_JSON_KEY = "PlayerTrophy.DropProbability";
    public static final String CHANGE_SOUND_JSON_KEY = "PlayerTrophy.ChangeSound";
    public static final String PLAYER_TROPHY_LORE = "Killed by %s";
    public static final NamespacedKey PLAYER_TROPHY_NOTEBLOCK_SOUND = new NamespacedKey("minecraft", "entity.slime.jump");

    private final double dropProbability;
    private final boolean changeSound;

    public PlayerTrophyManager() {
        this.dropProbability = Manager.getInstance().getConfig().getDouble(PROBABILITY_JSON_KEY);
        this.changeSound = Manager.getInstance().getConfig().getBoolean(CHANGE_SOUND_JSON_KEY);

    }

    private ItemStack createTrophy(OfflinePlayer owningPlayer, OfflinePlayer killer) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(owningPlayer);
        meta.setLore(Arrays.asList(String.format(PLAYER_TROPHY_LORE, killer.getName())));
        if(changeSound) {
            meta.setNoteBlockSound(PLAYER_TROPHY_NOTEBLOCK_SOUND);
        }
        head.setItemMeta(meta);
        return head;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if(event.getDamageSource().getCausingEntity() instanceof Player killer) {
            Player player = event.getEntity();
            Bukkit.getScheduler().runTaskAsynchronously(Manager.getInstance(), () ->
            {
                if(hasDefaultUsePermission(player) && Math.random() <= dropProbability) {
                    ItemStack head = createTrophy(player, killer);
                    Bukkit.getScheduler().runTask(Manager.getInstance(), () -> killer.getInventory().addItem(head));
                }
            });
        }
    }

    public double getDropProbability() {
        return dropProbability;
    }

    public boolean isChangeSound() {
        return changeSound;
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
        return "PlayerTrophy";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.YELLOW;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.playerTrophyPermission");
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(PROBABILITY_JSON_KEY, 1.0);
        config.setInlineComments(PROBABILITY_JSON_KEY, List.of("Wahrscheinlichkeit (in Dezimal), dass ein Kopf beim pvp gedroppt wird"));
        config.addDefault(CHANGE_SOUND_JSON_KEY, true);
        config.setInlineComments(PROBABILITY_JSON_KEY, List.of("Ändert den Notenblockton des Kopfes zum Sprungton eines Slimes"));
    }
}
