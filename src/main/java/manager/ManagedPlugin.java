package manager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permissible;

import java.util.List;

public interface ManagedPlugin
{
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

    default void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(getMessageColorPrefix() + message);
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
}
