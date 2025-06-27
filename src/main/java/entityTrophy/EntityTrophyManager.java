package entityTrophy;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.net.URL;
import java.util.*;

public class EntityTrophyManager implements Listener, ManagedPlugin {

    public static final String PROBABILITY_JSON_KEY = "DropProbability";
    public static final String LORE_JSON_KEY = "Lore";
    public static final String DISPLAY_NAME_JSON_KEY = "DisplayName";
    public static final String TEXTURE_URL_JSON_KEY = "TextureURL";
    public static final String LORE_DEFAULT = "Killed by %s";
    public static final String DISPLAY_NAME_DEFAULT = "Head of %s";
    public static final String TROPHY_CONFIG_FILE_NAME = "EntityTrophyConfig";
    public static final String TEXTURE_URL_DEFAULT_VALUE = "http://textures.minecraft.net/texture/3725da82aa0ade5d52bd2024f4bc1d019fc030e9ec5e0ec158c7f9a6aa0c43ba";
    public static final double PROBABILITY_DEFAULT_VALUE = 0.5;

    private final FileConfiguration trophyConfigFile;
    private final Map<EntityType, EntityTrophyInfo> entityTrophyInfos;
    private String loreText;
    private String displayName;

    public EntityTrophyManager() {
        this.entityTrophyInfos = new HashMap<>();
        this.loreText = LORE_DEFAULT;
        this.displayName = DISPLAY_NAME_DEFAULT;
        this.trophyConfigFile = Manager.getInstance().createConfigFile(TROPHY_CONFIG_FILE_NAME);
    }

    private ItemStack createTrophyItem(OfflinePlayer killer, Entity killed, PlayerProfile profile) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(String.format(getTrophyDisplayName(), killed.getName()));
        meta.setLore(Arrays.asList(String.format(getTrophyLoreText(), killer.getName())));
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createTrophy(OfflinePlayer killer, Entity killed, EntityTrophyInfo eti) {
        if(killer == null || killed == null) return null;
        if(eti == null) eti = getEntityTrophyInfo(EntityType.UNKNOWN);
        return createTrophyItem(
                killer,
                killed,
                (killed instanceof OfflinePlayer killedPlayer) ? killedPlayer.getPlayerProfile() : eti.profile()
        );
    }

    private PlayerProfile createTrophyHeadProfile(URL textureURL) {
        if(textureURL.getPath().isBlank()) return null;
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(textureURL);
        profile.setTextures(textures);
        return profile;
    }

    private boolean addEntityTrophyInfo(EntityTrophyInfo eti) {
        return this.entityTrophyInfos.put(eti.type(), eti) == null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if(event.getDamageSource().getCausingEntity() instanceof Player killer) {
            Entity killed = event.getEntity();
            Bukkit.getScheduler().runTaskAsynchronously(Manager.getInstance(), () ->
            {
                EntityTrophyInfo eti = getEntityTrophyInfo(killed.getType());
                if(eti == null) return;
                if(hasDefaultUsePermission(killer) && Math.random() <= eti.dropProbability()) {
                    ItemStack trophy = createTrophy(killer, killed, eti);
                    //TODO: Test if addItem with type=AIR is ignored
                    Bukkit.getScheduler().runTask(Manager.getInstance(), () -> killer.getInventory().addItem(trophy));
                    //TODO: Probably cant run async
                    Manager.getInstance().sendInfoMessage(this, String.format("Trophy created for \"%s\"{%s}", killer.getName(), killer.getUniqueId()));
                }
            });
        }
    }

    public EntityTrophyInfo getEntityTrophyInfo(EntityType entityType) {
        return entityTrophyInfos.get(entityType);
    }

    public String getTrophyLoreText() {
        return loreText;
    }

    public String getTrophyDisplayName() {
        return displayName;
    }

    @Override
    public boolean onEnable() {
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());

        this.loreText = getConfigurationValueAsString(LORE_JSON_KEY);
        this.displayName = getConfigurationValueAsString(DISPLAY_NAME_JSON_KEY);
        try {
            addEntityTrophyInfo(new EntityTrophyInfo(
                    EntityType.UNKNOWN,
                    PROBABILITY_DEFAULT_VALUE,
                    createTrophyHeadProfile(URI.create(TEXTURE_URL_DEFAULT_VALUE).toURL())));
        } catch(Exception ignored) {
        }
        ;

        for(String key : this.trophyConfigFile.getKeys(false)) {
            try {
                ConfigurationSection section = this.trophyConfigFile.getConfigurationSection(key);
                EntityTrophyInfo eti = new EntityTrophyInfo(
                        EntityType.valueOf(key),
                        section.getDouble(PROBABILITY_JSON_KEY),
                        createTrophyHeadProfile(URI.create(section.getString(TEXTURE_URL_JSON_KEY)).toURL())
                );
                addEntityTrophyInfo(eti);
            } catch(Exception e) {
                Manager.getInstance().sendErrorMessage(this, "EntityTrophy for : \"" + key + "\" could not be created! Error: " + e.getLocalizedMessage());
            }
        }

        return true;
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public String getName() {
        return "EntityTrophy";
    }

    @Override
    public String getConfigKey() {
        return "EntityTrophy";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.GOLD;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("dg.entityTrophyPermission");
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        // Trophy Lore:
        config.addDefault(getConfigPath(LORE_JSON_KEY), LORE_DEFAULT);
        config.setInlineComments(getConfigPath(LORE_JSON_KEY), List.of("Beschreibung, welche beim Kopf angezeigt wird (muss \"%s\" als Platzhalter für den tötenden Spieler enthalten)"));

        // Trophy Display-Name:
        config.addDefault(getConfigPath(DISPLAY_NAME_JSON_KEY), DISPLAY_NAME_DEFAULT);
        config.setInlineComments(getConfigPath(DISPLAY_NAME_JSON_KEY), List.of("Titel des Kopfes (muss \"%s\" als Platzhalter für den getöteten Spieler enthalten)"));

        // Entity drop-chance und Trophy texture URL:
        for(String entityTypeName : EntityTrophiesAvailableEntities.DEFAULT_SET.stream().map(EntityType::name).toList()) {
            trophyConfigFile.addDefault(getConfigPath(entityTypeName, TEXTURE_URL_JSON_KEY), "");
            trophyConfigFile.setInlineComments(getConfigPath(entityTypeName, TEXTURE_URL_JSON_KEY), List.of("Textur-URL des Kopfes"));
            trophyConfigFile.addDefault(getConfigPath(entityTypeName, PROBABILITY_JSON_KEY), PROBABILITY_DEFAULT_VALUE);
            trophyConfigFile.setInlineComments(getConfigPath(entityTypeName, PROBABILITY_JSON_KEY), List.of("Wahrscheinlichkeit (in Dezimal)"));
        }

        trophyConfigFile.options().copyDefaults(true);

        if(!Manager.getInstance().saveConfigFile(TROPHY_CONFIG_FILE_NAME))
            Manager.getInstance().sendErrorMessage(this, "Unable to save trophy-config-file");
    }
}
