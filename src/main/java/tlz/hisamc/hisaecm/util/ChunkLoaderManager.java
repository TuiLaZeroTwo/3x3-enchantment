package tlz.hisamc.hisaecm.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.listeners.ChunkLoaderListener;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkLoaderManager {

    private final HisaECM plugin;
    private final File file;
    private final Map<Location, Long> loaders = new ConcurrentHashMap<>();

    public ChunkLoaderManager(HisaECM plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "loaders.yml");
        load();
        
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tick(), 20L * 30L, 20L * 30L);
    }

    private Location findStoredLocation(Location query) {
        for (Location stored : loaders.keySet()) {
            if (stored.getWorld().getName().equals(query.getWorld().getName()) &&
                stored.getBlockX() == query.getBlockX() &&
                stored.getBlockY() == query.getBlockY() &&
                stored.getBlockZ() == query.getBlockZ()) {
                return stored;
            }
        }
        return null;
    }

    public void addLoader(Location loc) {
        Location existing = findStoredLocation(loc);
        if (existing != null) loaders.remove(existing);

        loaders.put(loc, System.currentTimeMillis()); 
        save();
        updateVisuals(loc); // This calls the method below
    }

    // FIX: Method is explicitly defined here
    private void updateVisuals(Location loc) {
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> updateVisualsInternal(loc));
    }

    public void removeLoader(Location loc) {
        Location stored = findStoredLocation(loc);
        if (stored != null) {
            loaders.remove(stored);
            Bukkit.getRegionScheduler().execute(plugin, stored, () -> {
                if (stored.isWorldLoaded()) {
                    stored.getWorld().getChunkAt(stored).removePluginChunkTicket(plugin);
                }
            });
            save();
        }
    }

    public void addTime(Location queryLoc, long millis) {
        Location stored = findStoredLocation(queryLoc);
        Location key = (stored != null) ? stored : queryLoc;
        
        long current = loaders.getOrDefault(key, System.currentTimeMillis());
        if (current < System.currentTimeMillis()) {
            current = System.currentTimeMillis();
        }
        
        loaders.put(key, current + millis);
        save();

        Bukkit.getRegionScheduler().execute(plugin, key, () -> {
            key.getWorld().getChunkAt(key).addPluginChunkTicket(plugin);
            updateVisualsInternal(key);
        });
    }

    public Long getExpiry(Location loc) {
        Location stored = findStoredLocation(loc);
        if (stored != null) {
            return loaders.get(stored);
        }
        return null;
    }

    public boolean isLoaderAt(Location loc) {
        return findStoredLocation(loc) != null;
    }

    private void tick() {
        long now = System.currentTimeMillis();

        for (Map.Entry<Location, Long> entry : loaders.entrySet()) {
            Location loc = entry.getKey();
            long expires = entry.getValue();

            if (now > expires) {
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                   if (loc.isWorldLoaded()) {
                       loc.getWorld().getChunkAt(loc).removePluginChunkTicket(plugin);
                       updateVisualsInternal(loc);
                   }
                });
            } else {
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                    if (loc.isWorldLoaded()) {
                        loc.getWorld().getChunkAt(loc).addPluginChunkTicket(plugin);
                        updateVisualsInternal(loc);
                    }
                });
            }
        }
    }

    private void updateVisualsInternal(Location loc) {
        if (!loc.isChunkLoaded()) return; 

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
            if (e instanceof ArmorStand as && e.getPersistentDataContainer().has(ChunkLoaderListener.KEY_LOADER_BOT)) {
                Long expiry = getExpiry(loc); // Use helper to find correct key
                if (expiry == null) continue;

                long timeLeft = expiry - System.currentTimeMillis();
                
                if (timeLeft > 0) {
                    long hours = timeLeft / 3600000;
                    long minutes = (timeLeft % 3600000) / 60000;
                    as.customName(Component.text("§b§lChunkBot: §e" + hours + "h " + minutes + "m"));
                } else {
                    as.customName(Component.text("§c§lChunkBot: §4OUT OF FUEL"));
                }
            }
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (Map.Entry<Location, Long> entry : loaders.entrySet()) {
            Location l = entry.getKey();
            list.add(l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," + entry.getValue());
        }
        yaml.set("loaders", list);
        try { yaml.save(file); } catch (Exception ignored) {}
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String s : yaml.getStringList("loaders")) {
            try {
                String[] p = s.split(",");
                Location loc = new Location(Bukkit.getWorld(p[0]), 
                        Double.parseDouble(p[1]) + 0.5, 
                        Double.parseDouble(p[2]) + 1.0, 
                        Double.parseDouble(p[3]) + 0.5);
                loaders.put(loc, Long.parseLong(p[4]));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}