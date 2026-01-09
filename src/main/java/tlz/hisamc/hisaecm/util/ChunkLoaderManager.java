package tlz.hisamc.hisaecm.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
        
        // Tick every 30 seconds to update visuals
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tick(), 20L * 30L, 20L * 30L);
    }

    public void addLoader(Location loc) {
        loaders.put(loc, System.currentTimeMillis()); 
        save();
        updateVisuals(loc);
    }

    public void removeLoader(Location loc) {
        loaders.remove(loc);
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            loc.getWorld().getChunkAt(loc).removePluginChunkTicket(plugin);
        });
        save();
    }

    public void addTime(Location loc, long millis) {
        long current = loaders.getOrDefault(loc, System.currentTimeMillis());
        
        // If currently out of fuel (expired), reset start time to NOW so they get the full duration
        if (current < System.currentTimeMillis()) {
            current = System.currentTimeMillis();
        }
        
        loaders.put(loc, current + millis);
        save();

        // FIX: Force wake-up the chunk immediately!
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            // 1. Re-add the ticket so the chunk loads
            loc.getWorld().getChunkAt(loc).addPluginChunkTicket(plugin);
            
            // 2. Update the text immediately (nested to ensure chunk is ready)
            updateVisualsInternal(loc);
        });
    }

    public Long getExpiry(Location loc) {
        return loaders.get(loc);
    }

    public boolean isLoaderAt(Location loc) {
        for (Location l : loaders.keySet()) {
            if (l.getBlockX() == loc.getBlockX() && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    // Global tick to refresh all loaders
    private void tick() {
        long now = System.currentTimeMillis();

        for (Map.Entry<Location, Long> entry : loaders.entrySet()) {
            Location loc = entry.getKey();
            long expires = entry.getValue();

            if (now > expires) {
                // EXPIRED: Remove ticket so server can unload chunk if it wants
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                   loc.getWorld().getChunkAt(loc).removePluginChunkTicket(plugin);
                   // We still try to update text to say "EXPIRED" if the chunk happens to be loaded
                   updateVisualsInternal(loc);
                });
            } else {
                // ACTIVE: Keep chunk loaded
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                    loc.getWorld().getChunkAt(loc).addPluginChunkTicket(plugin);
                    updateVisualsInternal(loc);
                });
            }
        }
    }

    // Public method that schedules the update
    private void updateVisuals(Location loc) {
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> updateVisualsInternal(loc));
    }

    // Internal logic that assumes we are already on the region thread
    private void updateVisualsInternal(Location loc) {
        // If chunk isn't loaded, we can't find entities.
        if (!loc.isChunkLoaded()) return; 

        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
            if (e instanceof ArmorStand as && e.getPersistentDataContainer().has(ChunkLoaderListener.KEY_LOADER_BOT)) {
                long timeLeft = loaders.getOrDefault(loc, 0L) - System.currentTimeMillis();
                
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
                Location loc = new Location(Bukkit.getWorld(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]));
                loaders.put(loc, Long.parseLong(p[4]));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}