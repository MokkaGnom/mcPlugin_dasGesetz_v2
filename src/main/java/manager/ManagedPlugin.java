package manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permissible;

public interface ManagedPlugin
{
    String MESSAGE_PREFIX = "[%s]";

    boolean onEnable();

    void onDisable();

    String getName();

    default String getMessagePrefix() {
        return String.format(MESSAGE_PREFIX, getName());
    }

    default void createDefaultConfig(FileConfiguration config) {
    }

    boolean hasPermission(Permissible permissible);

    default boolean hasAdminPermission(Permissible permissible){
        return false;
    }

    default boolean hasPermission(Permissible permissible, Class<?> supervisedClass) {
        return hasPermission(permissible);
    }
}
