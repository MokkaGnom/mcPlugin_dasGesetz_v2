package ping;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class PingManager implements Listener, ManagedPlugin
{
    public static final String DURATION_JSON_KEY = "Ping.Duration";
    public static final String COOLDOWN_JSON_KEY = "Ping.Cooldown";
    public static final String PERMISSION = "dg.pingPermission";
    public static final String COOLDOWN_META_KEY = "pingTime";
    public static final String COLOR_META_KEY = "pingColor";
    public static final Material PING_ITEM_MATERIAL = Material.STICK;
    public static final String PING_MESSAGE_FORMAT = "You pinged at %s, %s, %s";

    private final int time;
    private final int cooldown;

    public PingManager() {
        this.time = Manager.getInstance().getConfig().getInt(DURATION_JSON_KEY);
        this.cooldown = Manager.getInstance().getConfig().getInt(COOLDOWN_JSON_KEY);
    }

    private static int getValueOfHex(char high, char low) {
        int value = 0;
        char[] hex = new char[2];
        hex[0] = high;
        hex[1] = low;

        for(int i = 0; i < 2; i++, value *= 10) {
            if(hex[i] >= 48 && hex[i] <= 57) {
                value += hex[i] - 48;
            }
            else {
                if(hex[i] == 'A' || hex[i] == 'a')
                    value += 10;
                else if(hex[i] == 'B' || hex[i] == 'b')
                    value += 11;
                else if(hex[i] == 'C' || hex[i] == 'c')
                    value += 12;
                else if(hex[i] == 'D' || hex[i] == 'd')
                    value += 13;
                else if(hex[i] == 'E' || hex[i] == 'e')
                    value += 14;
                else if(hex[i] == 'F' || hex[i] == 'f')
                    value += 15;
                else
                    return -1;
            }

        }
        return value / 10;
    }

    public static Color getColorFromHexString(String hexString) {
        if(hexString == null || hexString.length() != 6)
            return null;

        try {
            return Color.fromRGB(
                    PingManager.getValueOfHex(hexString.charAt(0), hexString.charAt(1)),
                    PingManager.getValueOfHex(hexString.charAt(2), hexString.charAt(3)),
                    PingManager.getValueOfHex(hexString.charAt(4), hexString.charAt(5))
            );
        } catch(IllegalArgumentException e) {
            return null;
        }
    }

    public String getMessageString(String message) {
        return org.bukkit.ChatColor.GRAY + "[" + ChatColor.DARK_PURPLE + getName() + org.bukkit.ChatColor.GRAY + "] " + ChatColor.WHITE + message;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if(p.hasPermission(PERMISSION) && checkCooldown(p) && p.getInventory().getItemInOffHand().getType().equals(PING_ITEM_MATERIAL)
                && (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))) {
            Block b = p.getTargetBlock(null, 255);
            String color = null;
            if(p.hasMetadata(COLOR_META_KEY))
                color = p.getMetadata(COLOR_META_KEY).getFirst().asString();
            new Ping(b, time, p.getName(), color);
            p.removeMetadata(COOLDOWN_META_KEY, Manager.getInstance());
            p.setMetadata(COOLDOWN_META_KEY, new FixedMetadataValue(Manager.getInstance(), System.currentTimeMillis()));
            p.sendMessage(this.getMessageString(String.format(PING_MESSAGE_FORMAT, b.getX(), b.getY(), b.getZ())));
        }
    }

    public void setPlayerColor(Player p, String color) {
        if(p.hasMetadata(COLOR_META_KEY))
            p.removeMetadata(COLOR_META_KEY, Manager.getInstance());

        p.setMetadata(COLOR_META_KEY, new FixedMetadataValue(Manager.getInstance(), color));
    }

    public boolean checkCooldown(Player p) {
        if(p.hasMetadata(COOLDOWN_META_KEY)) {
            return p.getMetadata(COOLDOWN_META_KEY).getFirst().asLong() + cooldown <= System.currentTimeMillis();
        }
        else {
            p.setMetadata(COOLDOWN_META_KEY, new FixedMetadataValue(Manager.getInstance(), System.currentTimeMillis()));
            return true;
        }
    }

    public int getTime() {
        return time;
    }

    public int getCooldown() {
        return cooldown;
    }

    @Override
    public boolean onEnable() {
        PingCommands pingCommands = new PingCommands(this);

        Manager.getInstance().getServer().getPluginManager().registerEvents(this, Manager.getInstance());
        try {
            Manager.getInstance().getCommand(PingCommands.CommandStrings.ROOT).setExecutor(pingCommands);
            Manager.getInstance().getCommand(PingCommands.CommandStrings.ROOT).setTabCompleter(pingCommands);
            return true;
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(e.getMessage());
            onDisable();
            return false;
        }
    }

    @Override
    public void onDisable() {
        //TODO: Evtl. die Ping-Farben in Datei speichern
        HandlerList.unregisterAll(this);
        try {
            Manager.getInstance().getCommand(PingCommands.CommandStrings.ROOT).setExecutor(null);
            Manager.getInstance().getCommand(PingCommands.CommandStrings.ROOT).setTabCompleter(null);
        } catch(NullPointerException e) {
            Manager.getInstance().sendErrorMessage(e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "Ping";
    }

    @Override
    public void createDefaultConfig(FileConfiguration config) {
        config.addDefault(DURATION_JSON_KEY, 5000);
        config.setInlineComments(DURATION_JSON_KEY, List.of("Wie lange der Ping angezeigt werden soll (in Millisekunden)"));
        config.addDefault(COOLDOWN_JSON_KEY, 5000);
        config.setInlineComments(COOLDOWN_JSON_KEY, List.of("Cooldown, bis der Ping erneut verwendet werden kann (in Millisekunden)"));
    }
}
