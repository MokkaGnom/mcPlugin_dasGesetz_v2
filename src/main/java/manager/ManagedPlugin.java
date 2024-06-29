package manager;

import org.bukkit.configuration.file.FileConfiguration;

public interface ManagedPlugin
{
    String MESSAGE_PREFIX = "[%s]";

    boolean onEnable ();

    void onDisable ();

    String getName ();

    default String getMessagePrefix()
    {
        return String.format(MESSAGE_PREFIX, getName());
    }

    default void createDefaultConfig (FileConfiguration config)
    {
    }
}
