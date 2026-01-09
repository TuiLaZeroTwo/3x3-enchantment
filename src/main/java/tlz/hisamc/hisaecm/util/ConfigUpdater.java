package tlz.hisamc.hisaecm.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ConfigUpdater {

    public static void update(JavaPlugin plugin) {
        // 1. Get the config file on the disk
        FileConfiguration diskConfig = plugin.getConfig();
        boolean changed = false;

        // 2. Load the default config from inside the JAR
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream == null) return;

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
        Set<String> defaultKeys = defaultConfig.getKeys(true);

        // 3. Loop through all default keys
        for (String key : defaultKeys) {
            // If the key is missing in the disk config, add it!
            if (!diskConfig.contains(key)) {
                diskConfig.set(key, defaultConfig.get(key));
                changed = true;
            }
        }

        // 4. Save if changes were made
        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("Configuration file updated with new missing keys!");
        }
    }
}