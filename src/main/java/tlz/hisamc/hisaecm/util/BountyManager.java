package tlz.hisamc.hisaecm.util;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import tlz.hisamc.hisaecm.HisaECM;

import java.io.File;
import java.util.*;

public class BountyManager {

    private final HisaECM plugin;
    private final File file;
    // Maps Target Name -> Amount
    private final Map<String, Double> bounties = new HashMap<>();

    public BountyManager(HisaECM plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bounties.yml");
        loadBounties();
    }

    public void addBounty(String targetName, double amount) {
        bounties.put(targetName, bounties.getOrDefault(targetName, 0.0) + amount);
        saveBounties();
    }

    public void removeBounty(String targetName) {
        bounties.remove(targetName);
        saveBounties();
    }

    public double getBounty(String targetName) {
        return bounties.getOrDefault(targetName, 0.0);
    }

    public Map<String, Double> getAllBounties() {
        return bounties;
    }

    public void saveBounties() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Double> entry : bounties.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }
        try { yaml.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadBounties() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            bounties.put(key, yaml.getDouble(key));
        }
    }
}