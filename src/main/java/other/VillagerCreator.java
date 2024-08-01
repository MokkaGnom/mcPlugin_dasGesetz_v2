package other;

import manager.ManagedPlugin;
import manager.Manager;
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
import org.bukkit.permissions.Permissible;

import java.util.*;

public class VillagerCreator implements Listener, ManagedPlugin
{
    private static class PlayerSneakInfo
    {
        private final Player player1;
        private final Player player2;
        private final Long startTime;
        private int sneakCount;

        public PlayerSneakInfo(Player player1, Player player2, Long startTime) {
            this.player1 = player1;
            this.player2 = player2;
            this.startTime = startTime;
        }

        public Player getPlayer1() {
            return player1;
        }

        public Player getPlayer2() {
            return player2;
        }

        public Long getStartTime() {
            return startTime;
        }

        public int getSneakCount() {
            return sneakCount;
        }

        public int incrementSneakCount() {
            return sneakCount++;
        }
    }

    public static final String MAX_TIME_JSON_KEY = "VillagerCreator.MaxTime";
    public static final String MAX_DISTANCE_JSON_KEY = "VillagerCreator.MaxDistance";
    public static final String SNEAK_COUNT_JSON_KEY = "VillagerCreator.SneakCount";

    private final int maxTime;
    private final int maxDistance;
    private final int sneakCount;
    private final Map<UUID, PlayerSneakInfo> playerSneakMap;

    public VillagerCreator() {
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
            }
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
        remove(playerSneakInfo.getPlayer1().getUniqueId(), playerSneakInfo.getPlayer2().getUniqueId());
    }

    public Player getOtherPlayer(Player player) {
        for(Entity entity : player.getLocation().getChunk().getEntities()) {
            if(entity instanceof Player p && p.getLocation().distance(player.getLocation()) <= maxDistance && p != player) {
                return p;
            }
        }
        return null;
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if(event.isSneaking()) {
            Player other = getOtherPlayer(player);
            if(other == null) {
                return;
            }
            if(!hasDefaultUsePermission(player) || !hasDefaultUsePermission(other)) {
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

        config.addDefault(MAX_DISTANCE_JSON_KEY, 1);
        config.setInlineComments(MAX_DISTANCE_JSON_KEY, List.of("Maximale Distanz (Blocks)"));

        config.addDefault(SNEAK_COUNT_JSON_KEY, 20);
        config.setInlineComments(SNEAK_COUNT_JSON_KEY, List.of("Sneak Anzahl"));
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
        return "VillagerCreator";
    }

    @Override
    public ChatColor getMessageColor() {
        return ManagedPlugin.DEFAULT_CHAT_COLOR;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.createVillagerPermission");
    }
}
