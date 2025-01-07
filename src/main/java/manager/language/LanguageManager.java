package manager.language;

import manager.ManagedPlugin;
import manager.Manager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import utility.ErrorMessage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager
{
    public static final String PREFIX = "LanguageManager";
    public static final String LANGUAGE_FILE_NAME = "locale.yml";
    private final Map<Class<? extends ManagedPlugin>, Map<String, LocalizedString>> pluginLocalizedStringsMap;

    public LanguageManager() {
        this.pluginLocalizedStringsMap = new HashMap<>();
    }

    private void loadErrorMessages(ConfigurationSection section) {
        for(String key : section.getKeys(false)) {
            try {
                ErrorMessage.valueOf(key).setLocalizedString(
                        new LocalizedString(section.getConfigurationSection(key))
                );
            } catch(Exception e) {
                Manager.getInstance().sendWarningMessage(PREFIX, e.getLocalizedMessage());
            }
        }
    }

    public int loadFromFile() {
        InputStream inputStream = Manager.getInstance().getResource(LANGUAGE_FILE_NAME);
        if(inputStream == null) {
            Manager.getInstance().sendErrorMessage(PREFIX, "Couldn't find language file!");
            return -1;
        }
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(new BufferedReader(new InputStreamReader(inputStream)));
        for(String subPluginName : fileConfig.getKeys(false)) {
            if(subPluginName.equalsIgnoreCase("ErrorMessages")) {
                loadErrorMessages(fileConfig.getConfigurationSection(subPluginName));
                continue;
            }
            ConfigurationSection subPluginSection = fileConfig.getConfigurationSection(subPluginName);
            Map<String, LocalizedString> localizedStrings = new HashMap<>();
            this.pluginLocalizedStringsMap.put(Manager.getInstance().getPluginClassFromName(subPluginName), localizedStrings);
            for(String localizedStringKey : subPluginSection.getKeys(false)) {
                localizedStrings.put(localizedStringKey,
                        new LocalizedString(subPluginSection.getConfigurationSection(localizedStringKey))
                );
            }
        }
        return this.pluginLocalizedStringsMap.keySet().size();
    }

    public Map<Class<? extends ManagedPlugin>, Map<String, LocalizedString>> getPluginLocalizedStringsMap() {
        return pluginLocalizedStringsMap;
    }

    public Map<String, LocalizedString> getPluginLocalizedStrings(Class<? extends ManagedPlugin> pluginClass) {
        return pluginLocalizedStringsMap.get(pluginClass);
    }

    public LocalizedString getLocalizedString(Class<? extends ManagedPlugin> pluginClass, String key) {
        return pluginLocalizedStringsMap.get(pluginClass).get(key);
    }

    public Map<String, LocalizedString> getPluginLocalizedStrings(String subPluginName) {
        return getPluginLocalizedStrings(Manager.getInstance().getPluginClassFromName(subPluginName));
    }

    public LocalizedString getLocalizedString(String subPluginName, String key) {
        return getLocalizedString(Manager.getInstance().getPluginClassFromName(subPluginName), key);
    }

}
