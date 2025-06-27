package utility;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;

public record ConfigFile(FileConfiguration config, File file) {
    public boolean save() {
        try {
            config.save(file);
            return true;
        } catch(IOException e) {
            return false;
        }
    }
}
