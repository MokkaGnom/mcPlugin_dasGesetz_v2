package messagePrefix;

import blockLock.BlockLock;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

public record Prefix(String prefix, ChatColor prefixColor, ChatColor nameColor, boolean isAdminPrefix)
{
    @Override
    public String toString() {
        return ChatColor.GRAY + "[" + prefixColor + prefix + ChatColor.GRAY + "]" + ChatColor.RESET + "<" + nameColor + "%1$s" + ChatColor.RESET + "> %2$s";
    }

    public String getTabPrefix(){
        return ChatColor.GRAY + "[" + prefixColor + prefix + ChatColor.GRAY + "] " + nameColor + "%1$s";
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(obj instanceof Prefix prefixObj) {
            return prefixObj.prefix.equalsIgnoreCase(this.prefix);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return prefix.hashCode();
    }

    public boolean save(ConfigurationSection prefixSection) {
        if(prefixSection == null) return false;
        prefixSection.set("prefix", prefix);
        prefixSection.set("prefixColor", prefixColor.name());
        prefixSection.set("nameColor", nameColor.name());
        prefixSection.set("isAdminPrefix", isAdminPrefix);
        return true;
    }

    public static Prefix load(ConfigurationSection prefixSection) {
        if(prefixSection == null) return null;
        String prefix = prefixSection.getString("prefix");
        ChatColor prefixColor = ChatColor.valueOf(prefixSection.getString("prefixColor"));
        ChatColor nameColor = ChatColor.valueOf(prefixSection.getString("nameColor"));
        boolean isAdminPrefix = prefixSection.getBoolean("isAdminPrefix");
        return new Prefix(prefix, prefixColor, nameColor, isAdminPrefix);
    }
}
