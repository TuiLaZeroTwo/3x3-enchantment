package tlz.hisamc.hisaecm.util;

import org.bukkit.configuration.file.YamlConfiguration;
import tlz.hisamc.hisaecm.HisaECM;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap; // Fix: Thread-safe map

public class BountyManager {

    private final HisaECM plugin;
    private final File file;
    // FIX: Use ConcurrentHashMap for Thread Safety on Folia
    private final Map<String, Double> bounties = new ConcurrentHashMap<>();

    public BountyManager(HisaECM plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bounties.yml");
        loadBounties();
    }

    public void addBounty(String targetName, double amount) {
        bounties.merge(targetName, amount, Double::sum); // Atomic update
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

    // FIX: Synchronized to prevent file corruption
    public synchronized void saveBounties() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Double> entry : bounties.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }
        try { yaml.save(file); } catch (Exception e) { e.printStackTrace(); }
    }

    private synchronized void loadBounties() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            bounties.put(key, yaml.getDouble(key));
        }
    }
}