package manager.language;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public record LocalizedString(Map<Locale, String> stringMap)
{
    public LocalizedString(ConfigurationSection section) {
        this(new HashMap<>()
        {{
            for(String key : section.getKeys(false)) {
                put(Locale.forLanguageTag(key), section.getString(key));
            }
        }});
    }

    public String get(Locale locale) {
        return stringMap.get(locale);
    }

    public String getOrDefault(Locale locale) {
        return stringMap.getOrDefault(locale, getDefault());
    }

    public String get(String locale) {
        return stringMap.get(Locale.forLanguageTag(locale));
    }

    public String getOrDefault(String locale) {
        return stringMap.getOrDefault(Locale.forLanguageTag(locale), getDefault());
    }

    public String getDefault() {
        String eng = get(Locale.ENGLISH);
        if(eng == null && !stringMap.entrySet().isEmpty()) {
            return stringMap.entrySet().stream().toList().getFirst().getValue();
        }
        return eng;
    }
}
