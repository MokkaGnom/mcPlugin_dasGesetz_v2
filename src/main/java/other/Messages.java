package other;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Messages implements Listener, ManagedPlugin
{
    public static final String MESSAGE_JSON_KEY = "Messages.Message";

    private final String message;

    public Messages() {
        this.message = Manager.getInstance().getConfig().getString(MESSAGE_JSON_KEY);
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage((message != null ? message : "Hi"));
    }

    @Override
    public boolean onEnable() {
        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        return true;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public String getName() {
        return "Messages";
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(MESSAGE_JSON_KEY, "Check out the plugin: https://github.com/MokkaGnom/mcPlugin_dasGesetz_v2");
    }
}
