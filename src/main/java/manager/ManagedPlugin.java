package manager;

import manager.language.LocalizedString;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import utility.ErrorMessage;

import java.util.List;

public interface ManagedPlugin
{
    List<String> COMMAND_NO_OPTION_AVAILABLE = List.of("");
    List<String> DISABLE_STRINGS = List.of("0", "disable", "false");
    List<String> ENABLE_STRINGS = List.of("1", "enable", "true");

    String MESSAGE_PREFIX = "[%s] ";
    int PERMISSION_DEFAULT_USE_INDEX = 0;
    int PERMISSION_ADMIN_INDEX = 1;
    ChatColor DEFAULT_CHAT_COLOR = ChatColor.RED;

    /*-------------------------- Default --------------------------*/

    boolean onEnable();

    void onDisable();

    String getName();

    default void createDefaultConfig(FileConfiguration config) {
    }

    /*-------------------------- Message --------------------------*/

    default void sendErrorMessage(CommandSender sender, ErrorMessage errorMessage) {
        sender.sendMessage(getMessageColorPrefix() + errorMessage.getMessage());
    }

    default void sendMessageDirect(Player player, String message) {
        player.sendMessage(getMessageColorPrefix() + message);
    }

    default void sendMessageFormat(Player player, LocalizedString message, Object... args) {
        sendMessageDirect(player, String.format(message.getOrDefault(player.getLocale()), args));
    }

    default void sendMessageFormat(Player player, String localizedStringKey, Object... args) {
        sendMessageFormat(player, getLocalizedString(localizedStringKey), args);
    }

    default void sendMessage(Player player, LocalizedString message) {
        sendMessageDirect(player, message.getOrDefault(player.getLocale()));
    }

    default void sendMessage(Player player, String localizedStringKey) {
        sendMessage(player, getLocalizedString(localizedStringKey));
    }

    default void sendMessage(Player player, ErrorMessage errorMessage) {
        sendMessage(player, errorMessage.getLocalizedMessage());
    }

    ChatColor getMessageColor();

    default String getMessagePrefix() {
        return String.format(MESSAGE_PREFIX, getName());
    }

    default String getMessageColorPrefix() {
        return ChatColor.GRAY + "[" + getMessageColor() + getName() + ChatColor.GRAY + "] " + ChatColor.WHITE;
    }

    /*-------------------------- Permission --------------------------*/

    List<String> getPermissions();

    default boolean hasDefaultUsePermission(Permissible permissible) {
        return permissible.hasPermission(getPermissions().get(PERMISSION_DEFAULT_USE_INDEX));
    }

    default boolean hasAdminPermission(Permissible permissible) {
        List<String> permissions = getPermissions();
        return permissions.size() > PERMISSION_ADMIN_INDEX && permissible.hasPermission(permissions.get(PERMISSION_ADMIN_INDEX));
    }

    default boolean hasDefaultUsePermission(Permissible permissible, Class<?> supervisedClass) {
        return hasDefaultUsePermission(permissible);
    }

    /*-------------------------- Language --------------------------*/

    default LocalizedString getLocalizedString(String key) {
        return Manager.getInstance().getLanguageManager().getLocalizedString(this.getClass(), key);
    }
}
