package other;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permissible;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Messages implements Listener, ManagedPlugin
{
    private interface HOURS
    {
        List<Integer> EVENING = List.of(18, 19, 20, 21, 22, 23, 24, 0, 1, 2);
        List<Integer> AFTERNOON = List.of(15, 16, 17);
        List<Integer> NOON = List.of(11, 12, 13, 14);
        List<Integer> MORNING = List.of(3, 4, 5, 6, 7, 8, 9, 10);
    }

    public static final String USE_WELCOME_MESSAGE_JSON_KEY = "Messages.UseWelcomeMessage";
    public static final String MESSAGE_JSON_KEY = "Messages.Message";

    private static final boolean DEFAULT_USE_WELCOME_MESSAGE = true;
    private static final String DEFAULT_MESSAGE = "Check out the plugin: https://dasGesetz.mokkagnom.de";

    private final boolean useWelcomeMessage;
    private final String message;

    public Messages() {
        this.useWelcomeMessage = Manager.getInstance().getConfig().getBoolean(USE_WELCOME_MESSAGE_JSON_KEY);
        this.message = Objects.requireNonNullElse(Manager.getInstance().getConfig().getString(MESSAGE_JSON_KEY), DEFAULT_MESSAGE);
    }

    private String getCurrentWelcomeMessage(String playerName) {
        String welcomeMessage = "%s : %s";
        LocalDateTime now = LocalDateTime.now();
        String day = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.GERMAN);
        int hour = now.getHour();

        if(HOURS.EVENING.contains(hour)) {
            welcomeMessage = "Einen schönen %s Abend, %s!";
        }
        else if(HOURS.AFTERNOON.contains(hour)) {
            welcomeMessage = "Einen schönen %s Nachmittag, %s!";
        }
        else if(HOURS.NOON.contains(hour)) {
            welcomeMessage = "Einen schönen %s Mittag, %s!";
        }
        else if(HOURS.MORNING.contains(hour)) {
            welcomeMessage = "Einen schönen %s Morgen %s!";
        }
        return String.format(welcomeMessage, day, playerName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if(useWelcomeMessage) {
            sendMessage(player, getCurrentWelcomeMessage(player.getName()));
        }
        else {
            sendMessage(player, message);
        }
    }

    @Override
    public boolean hasDefaultUsePermission(Permissible permissible) {
        return true;
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
        return "Messages";
    }

    @Override
    public ChatColor getMessageColor() {
        return ChatColor.MAGIC;
    }

    @Override
    public List<String> getPermissions() {
        return List.of("");
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(MESSAGE_JSON_KEY, DEFAULT_MESSAGE);
        config.setInlineComments(MESSAGE_JSON_KEY, List.of("Die Nachricht, welche einem Spieler angezeigt werden soll, wenn er joined"));

        config.addDefault(USE_WELCOME_MESSAGE_JSON_KEY, DEFAULT_USE_WELCOME_MESSAGE);
        config.setInlineComments(USE_WELCOME_MESSAGE_JSON_KEY, List.of("Anstatt der Nachricht wird der Spieler begrüßt"));
    }
}
