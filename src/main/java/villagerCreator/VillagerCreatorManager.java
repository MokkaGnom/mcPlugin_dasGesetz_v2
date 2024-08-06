package villagerCreator;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

public class VillagerCreatorManager implements Listener, ManagedPlugin
{
    public static final String MAX_TIME_JSON_KEY = "VillagerCreator.MaxTime";
    public static final String MAX_DISTANCE_JSON_KEY = "VillagerCreator.MaxDistance";
    public static final String SNEAK_COUNT_JSON_KEY = "VillagerCreator.SneakCount";

    public static final String VILLAGER_META_KEY = "villagerCreator";
    public static final String VILLAGER_META_VALUE = "%s+%s";
    public static final String VILLAGER_NAME_FORMAT = "Child from %s and %s";

    public static final int MAX_REMOVE_DISTANCE = 10;

    private final int maxTime;
    private final int maxDistance;
    private final int sneakCount;
    private final Map<UUID, PlayerSneakInfo> playerSneakMap;

    public VillagerCreatorManager() {
        this.playerSneakMap = new HashMap<>();
        maxTime = Manager.getInstance().getConfig().getInt(MAX_TIME_JSON_KEY);
        maxDistance = Manager.getInstance().getConfig().getInt(MAX_DISTANCE_JSON_KEY);
        sneakCount = Manager.getInstance().getConfig().getInt(SNEAK_COUNT_JSON_KEY);
    }

    private void remove(UUID uuid1, UUID uuid2) {
        playerSneakMap.remove(uuid1);
        playerSneakMap.remove(uuid2);
    }

    private void spawnVillager(PlayerSneakInfo playerSneakInfo) {
        try {
            Entity entity = Objects.requireNonNull(playerSneakInfo.getPlayer1().getLocation().getWorld()).spawnEntity(playerSneakInfo.getPlayer1().getLocation(), EntityType.VILLAGER);
            if(entity instanceof Villager villager) {
                villager.setBaby();
                villager.setCustomName(String.format(VILLAGER_NAME_FORMAT, playerSneakInfo.getPlayer1().getName(), playerSneakInfo.getPlayer2().getName()));
                villager.setCustomNameVisible(true);
                villager.setMetadata(VILLAGER_META_KEY, new FixedMetadataValue(Manager.getInstance(), String.format(VILLAGER_META_VALUE, playerSneakInfo.getPlayer1().getName(), playerSneakInfo.getPlayer2().getName())));
                Bukkit.getScheduler().runTaskLater(Manager.getInstance(), () -> removeCustomFromVillager(villager), 25000); // >20min
            }
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
        remove(playerSneakInfo.getPlayer1().getUniqueId(), playerSneakInfo.getPlayer2().getUniqueId());
    }

    public boolean removeCustomFromVillager(Villager villager) {
        if(villager.hasMetadata(VILLAGER_META_KEY) && villager.isAdult() && villager.isValid()) {
            villager.setCustomName("");
            villager.setCustomNameVisible(false);
            villager.removeMetadata(VILLAGER_META_KEY, Manager.getInstance());
            return true;
        }
        return false;
    }

    public Player getOtherPlayer(Player player) {
        for(Entity entity : player.getLocation().getChunk().getEntities()) {
            if(entity instanceof Player p && p.getLocation().distance(player.getLocation()) <= maxDistance && p != player) {
                return p;
            }
        }
        return null;
    }

    public boolean isPrevention(Player p1, Player p2) {
        return p1.getInventory().getLeggings() != null || p2.getInventory().getLeggings() != null;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if(event.isSneaking()) {
            Player other = getOtherPlayer(player);
            if(other == null) {
                return;
            }
            if(!hasDefaultUsePermission(player) || !hasDefaultUsePermission(other) || isPrevention(player, other)) {
                remove(player.getUniqueId(), other.getUniqueId());
                return;
            }

            if(!playerSneakMap.containsKey(player.getUniqueId()) && !playerSneakMap.containsKey(other.getUniqueId())) {
                PlayerSneakInfo info = new PlayerSneakInfo(player, other, System.currentTimeMillis());
                playerSneakMap.put(player.getUniqueId(), info);
                playerSneakMap.put(other.getUniqueId(), info);
            }
            else {
                PlayerSneakInfo info = playerSneakMap.get(player.getUniqueId());
                if(info == null) {
                    return;
                }
                long time = System.currentTimeMillis() - info.getStartTime();

                if(info.incrementSneakCount() >= sneakCount && time <= maxTime) {
                    spawnVillager(info);
                }
                else if(time > maxTime) {
                    remove(player.getUniqueId(), other.getUniqueId());
                }
            }
        }
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(MAX_TIME_JSON_KEY, 15000);
        config.setInlineComments(MAX_TIME_JSON_KEY, List.of("Maximale Zeit (ms)"));

        config.addDefault(MAX_DISTANCE_JSON_KEY, 2);
        config.setInlineComments(MAX_DISTANCE_JSON_KEY, List.of("Maximale Distanz (Blocks)"));

        config.addDefault(SNEAK_COUNT_JSON_KEY, 20);
        config.setInlineComments(SNEAK_COUNT_JSON_KEY, List.of("Sneak Anzahl"));
    }

    @Override
    public boolean onEnable() {
        VillagerCreatorCommands vcc = new VillagerCreatorCommands(this);
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        try {
            Manager.getInstance().getCommand(VillagerCreatorCommands.CommandStrings.ROOT).setExecutor(vcc);
            Manager.getInstance().getCommand(VillagerCreatorCommands.CommandStrings.ROOT).setTabCompleter(vcc);
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
            onDisable();
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        try {
            Manager.getInstance().getCommand(VillagerCreatorCommands.CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(VillagerCreatorCommands.CommandStrings.ROOT).setTabCompleter(null);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "VillagerCreator";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.YELLOW;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.createVillagerPermission");
    }
}
