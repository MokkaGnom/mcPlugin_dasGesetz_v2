package villagerCreator;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.*;
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
import utility.HelperFunctions;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class VillagerCreatorManager implements Listener, ManagedPlugin
{
    public static final String MAX_TIME_JSON_KEY = "VillagerCreator.MaxTime";
    public static final String MAX_DISTANCE_JSON_KEY = "VillagerCreator.MaxDistance";
    public static final String SNEAK_COUNT_JSON_KEY = "VillagerCreator.SneakCount";
    public static final String BASE_CHANCE_JSON_KEY = "VillagerCreator.BaseChance";
    public static final String MULTIPLE_CHANCE_JSON_KEY = "VillagerCreator.MultipleChance";
    public static final String USE_CUSTOM_NAMES_JSON_KEY = "VillagerCreator.UseCustomNames";
    public static final String PUBLIC_ANNOUNCEMENT_JSON_KEY = "VillagerCreator.PublicAnnouncement";

    public static final String VILLAGER_META_KEY = "villagerCreator";
    public static final String VILLAGER_META_VALUE = "%s+%s";
    public static final String VILLAGER_NAME_FORMAT = "Child of %s and %s";
    public static final String PUBLIC_ANNOUNCEMENT_FORMAT = "%s and %s made a baby!";

    public static final Particle SPAWN_PARTICLE = Particle.HEART;
    public static final int SPAWN_PARTICLE_COUNT = 4;
    public static final int[] SPAWN_PARTICLE_OFFSET = {0, 3, 0};
    public static final double SPAWN_PARTICLE_EXTRA = 0.1d;

    public static final Sound SPAWN_SOUND = Sound.ENTITY_VILLAGER_YES;
    public static final float SPAWN_SOUND_VOLUME = 3.0f;
    public static final float SPAWN_SOUND_PITCH = 1.0f;

    public static final Sound PREVENTION_SOUND = Sound.ITEM_SHIELD_BLOCK;
    public static final float PREVENTION_SOUND_VOLUME = 3.0f;
    public static final float PREVENTION_SOUND_PITCH = 1.0f;

    public static final String CUSTOM_NAMES_FILE_PATH = Manager.getInstance().getDataFolder() + File.separator + VILLAGER_META_KEY + File.separator + "customNames.txt";

    public static final int MAX_REMOVE_DISTANCE = 10;
    public static final int REMOVE_CUSTOM_DELAY_TIME = 25000; // Ticks: >20min
    public static final int MAX_MULTIPLE = 4;

    private final int maxTime;
    private final int maxDistance;
    private final int sneakCount;
    private final double baseChance;
    private final double multipleChance;
    private final boolean useCustomNames;
    private final boolean publicAnnouncement;
    private final Map<UUID, PlayerSneakInfo> playerSneakInfoMap;
    private final List<String> customNames;

    public VillagerCreatorManager() {
        this.customNames = HelperFunctions.getFromCSVFile(CUSTOM_NAMES_FILE_PATH);
        this.playerSneakInfoMap = new HashMap<>();
        this.maxTime = Manager.getInstance().getConfig().getInt(MAX_TIME_JSON_KEY);
        this.maxDistance = Manager.getInstance().getConfig().getInt(MAX_DISTANCE_JSON_KEY);
        this.sneakCount = Manager.getInstance().getConfig().getInt(SNEAK_COUNT_JSON_KEY);
        this.baseChance = Manager.getInstance().getConfig().getDouble(BASE_CHANCE_JSON_KEY);
        this.multipleChance = Manager.getInstance().getConfig().getDouble(MULTIPLE_CHANCE_JSON_KEY);
        this.useCustomNames = Manager.getInstance().getConfig().getBoolean(USE_CUSTOM_NAMES_JSON_KEY);
        this.publicAnnouncement = Manager.getInstance().getConfig().getBoolean(PUBLIC_ANNOUNCEMENT_JSON_KEY);
    }

    private void removePlayerSneakInfo(UUID player1, UUID player2) {
        playerSneakInfoMap.remove(player1);
        playerSneakInfoMap.remove(player2);
    }

    private PlayerSneakInfo addNewPlayerSneakInfo(Player player, Player other) {
        PlayerSneakInfo info = new PlayerSneakInfo(player, other, System.currentTimeMillis());
        playerSneakInfoMap.put(player.getUniqueId(), info);
        playerSneakInfoMap.put(other.getUniqueId(), info);
        return info;
    }

    private void setVillagerName(Villager villager, PlayerSneakInfo psi) {
        String name = "";
        if(useCustomNames && !customNames.isEmpty()) {
            name = customNames.get((new Random()).nextInt(customNames.size()));
        }
        else {
            name = String.format(VILLAGER_NAME_FORMAT, psi.getMain().getName(), psi.getOther().getName());
        }
        villager.setCustomName(name);
        villager.setCustomNameVisible(true);
    }

    private void spawnVillager(PlayerSneakInfo playerSneakInfo) {
        try {
            Entity entity = Objects.requireNonNull(playerSneakInfo.getMain().getLocation().getWorld()).spawnEntity(playerSneakInfo.getMain().getLocation(), EntityType.VILLAGER);
            if(entity instanceof Villager villager) {
                villager.setBaby();
                villager.setMetadata(VILLAGER_META_KEY, new FixedMetadataValue(Manager.getInstance(), String.format(VILLAGER_META_VALUE, playerSneakInfo.getMain().getName(), playerSneakInfo.getOther().getName())));
                setVillagerName(villager, playerSneakInfo);
                if(!useCustomNames) {
                    Bukkit.getScheduler().runTaskLater(Manager.getInstance(), () -> removeCustomFromVillager(villager), REMOVE_CUSTOM_DELAY_TIME);
                }
            }
        } catch(Exception e) {
            Manager.getInstance().sendErrorMessage(getMessagePrefix(), e.getMessage());
        }
    }

    private int getSpawnCount(double random) {
        int spawnCount = 1;
        for(int i = MAX_MULTIPLE; i > 1; i--) {
            if(random <= multipleChance / i) {
                spawnCount++;
                break;
            }
        }
        return spawnCount;
    }

    private void sendPublicAnnouncement(Player p1, Player p2) {
        Bukkit.getServer().broadcastMessage(
                ChatColor.GRAY + "[" + ChatColor.LIGHT_PURPLE + "Congratulation!" + ChatColor.GRAY + "] " +
                        ChatColor.WHITE + String.format(PUBLIC_ANNOUNCEMENT_FORMAT, p1.getName(), p2.getName())
        );
    }

    private int finishSneakInfo(PlayerSneakInfo psi) {
        double random = Math.random();
        int spawnCount = 0;
        if(random <= baseChance) {
            psi.getMain().getWorld().spawnParticle(
                    SPAWN_PARTICLE, psi.getMain().getLocation(),
                    SPAWN_PARTICLE_COUNT,
                    SPAWN_PARTICLE_OFFSET[0], SPAWN_PARTICLE_OFFSET[1], SPAWN_PARTICLE_OFFSET[2],
                    SPAWN_PARTICLE_EXTRA
            );
            psi.getMain().getWorld().playSound(
                    psi.getMain().getLocation(),
                    SPAWN_SOUND, SPAWN_SOUND_VOLUME, SPAWN_SOUND_PITCH
            );
            spawnCount = getSpawnCount(random);
            for(int i = 0; i < spawnCount; i++) {
                spawnVillager(psi);
            }
            if(publicAnnouncement) {
                sendPublicAnnouncement(psi.getMain(), psi.getOther());
            }
        }
        removePlayerSneakInfo(psi.getMain().getUniqueId(), psi.getOther().getUniqueId());
        return spawnCount;
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
        List<Entity> entities = player.getNearbyEntities(maxDistance, maxDistance, maxDistance);
        entities.sort(Comparator.comparing(entity -> entity.getLocation().getX() + entity.getLocation().getY() + entity.getLocation().getZ()));
        for(Entity entity : entities) {
            if(entity instanceof Player other) {
                return other;
            }
        }
        return null;
    }

    public boolean isPrevention(Player p) {
        return p.getInventory().getLeggings() != null;
    }

    public boolean isPrevention(Player p1, Player p2) {
        return isPrevention(p1) || isPrevention(p2);
    }

    public boolean isTimeValid(PlayerSneakInfo psi) {
        return (System.currentTimeMillis() - psi.getStartTime()) <= maxTime;
    }

    public void handlePlayerSneakEvent(Player player) {
        Player other = getOtherPlayer(player);
        if(other == null) {
            return;
        }
        if(!hasDefaultUsePermission(player) || !hasDefaultUsePermission(other) || isPrevention(player, other)) {
            if(isPrevention(other)) {
                other.getWorld().playSound(other.getLocation(), PREVENTION_SOUND, PREVENTION_SOUND_VOLUME, PREVENTION_SOUND_PITCH);
            }
            removePlayerSneakInfo(player.getUniqueId(), other.getUniqueId());
            return;
        }

        if(!playerSneakInfoMap.containsKey(player.getUniqueId()) && !playerSneakInfoMap.containsKey(other.getUniqueId())) {
            PlayerSneakInfo psi = addNewPlayerSneakInfo(player, other);
        }
        else {
            PlayerSneakInfo psi = playerSneakInfoMap.get(player.getUniqueId());
            if(psi == null) {
                Manager.getInstance().sendWarningMessage(getMessagePrefix(), "PlayerSneakInfo is null!");
                return;
            }

            if(!isTimeValid(psi)) {
                removePlayerSneakInfo(player.getUniqueId(), other.getUniqueId());
                return;
            }

            psi.setStartTime();
            if(psi.incrementSneakCount() >= sneakCount) {
                finishSneakInfo(psi); //TODO: Dauert manchmal lange. Evtl in Thread starten
            }
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if(event.isSneaking()) {
            handlePlayerSneakEvent(event.getPlayer());
        }
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(MAX_TIME_JSON_KEY, 1000);
        config.setInlineComments(MAX_TIME_JSON_KEY, List.of("Maximale Zeit (ms)"));

        config.addDefault(MAX_DISTANCE_JSON_KEY, 2);
        config.setInlineComments(MAX_DISTANCE_JSON_KEY, List.of("Maximale Distanz (Block)"));

        config.addDefault(SNEAK_COUNT_JSON_KEY, 20);
        config.setInlineComments(SNEAK_COUNT_JSON_KEY, List.of("Sneak Anzahl"));

        config.addDefault(BASE_CHANCE_JSON_KEY, 0.95);
        config.setInlineComments(BASE_CHANCE_JSON_KEY, List.of("Wahrscheinlichkeit (dezimal), dass ein Baby entsteht"));

        config.addDefault(MULTIPLE_CHANCE_JSON_KEY, 0.2);
        config.setInlineComments(MULTIPLE_CHANCE_JSON_KEY, List.of("Wahrscheinlichkeit (dezimal) von Zwillingen. Drillinge haben die halbe Wahrscheinlichkeit, Vierlinge ein viertel"));

        config.addDefault(USE_CUSTOM_NAMES_JSON_KEY, false);
        config.setInlineComments(USE_CUSTOM_NAMES_JSON_KEY, List.of("Ob die Namen aus \"customNames.txt\" verwendet werden sollen"));

        config.addDefault(PUBLIC_ANNOUNCEMENT_JSON_KEY, true);
        config.setInlineComments(PUBLIC_ANNOUNCEMENT_JSON_KEY, List.of("Serverweite Bekanntgabe der Geburt"));
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

        try {
            File file = new File(CUSTOM_NAMES_FILE_PATH);
            if(file.getParentFile().mkdirs() && file.createNewFile()) {
                Manager.getInstance().sendInfoMessage(getMessagePrefix(), "CustomNames file created!");
            }
            else {
                Manager.getInstance().sendInfoMessage(getMessagePrefix(), "CustomNames file already exists!");
            }
        } catch(IOException e) {
            Manager.getInstance().sendWarningMessage(getMessagePrefix(), "Error creating file \"" + CUSTOM_NAMES_FILE_PATH + "\": " + e.getMessage());
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
        return List.of("dg.createVillagerPermission", "dg.createVillagerAdminPermission");
    }
}
