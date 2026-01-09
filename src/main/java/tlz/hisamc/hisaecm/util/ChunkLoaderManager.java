package tlz.hisamc.hisaecm.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.listeners.ChunkLoaderListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ChunkLoaderManager {

    private final HisaECM plugin;
    private final DatabaseManager db;
    
    // Cache: Map<ExactLocation, Expiry>
    private final Map<Location, Long> loaders = new ConcurrentHashMap<>();

    public ChunkLoaderManager(HisaECM plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        
        loadFromDB(); // Load SQL data into Cache
        
        // Ticks
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tickExpiry(), 20L * 30L, 20L * 30L);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tickSpawners(), 20L, 20L);
    }

    // --- DB OPERATIONS ---

    private void loadFromDB() {
        String query = "SELECT location, expiry FROM loaders";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String locStr = rs.getString("location");
                long expiry = rs.getLong("expiry");
                Location loc = stringToLoc(locStr);
                if (loc != null) {
                    loaders.put(loc, expiry);
                }
            }
            plugin.getLogger().info("Loaded " + loaders.size() + " Chunk Loaders from Database.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveToDB(Location loc, long expiry) {
        String locStr = locToString(loc);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            String query = "INSERT OR REPLACE INTO loaders (location, expiry) VALUES (?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, locStr);
                ps.setLong(2, expiry);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteFromDB(Location loc) {
        String locStr = locToString(loc);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            String query = "DELETE FROM loaders WHERE location = ?";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, locStr);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    // --- HELPER CONVERTERS ---
    // Format: world,x,y,z (Using Integers for Block Consistency)
    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        try {
            String[] parts = str.split(",");
            return new Location(Bukkit.getWorld(parts[0]), 
                    Double.parseDouble(parts[1]) + 0.5, 
                    Double.parseDouble(parts[2]), // Y is usually precise or +1
                    Double.parseDouble(parts[3]) + 0.5);
        } catch (Exception e) { return null; }
    }
    
    // Helper to find key in cache
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

    // --- PUBLIC METHODS ---

    public void addLoader(Location loc) {
        Location existing = findStoredLocation(loc);
        if (existing != null) loaders.remove(existing);
        
        long expiry = System.currentTimeMillis(); // Starts expired/new
        loaders.put(loc, expiry);
        
        saveToDB(loc, expiry); // Async DB Save
        updateVisuals(loc);
    }

    public void removeLoader(Location loc) {
        Location stored = findStoredLocation(loc);
        if (stored != null) {
            loaders.remove(stored);
            deleteFromDB(stored); // Async DB Delete
            
            Bukkit.getRegionScheduler().execute(plugin, stored, () -> {
                if (stored.isWorldLoaded()) {
                    stored.getWorld().getChunkAt(stored).removePluginChunkTicket(plugin);
                }
            });
        }
    }

    public void addTime(Location queryLoc, long millis) {
        Location stored = findStoredLocation(queryLoc);
        Location key = (stored != null) ? stored : queryLoc;
        
        long current = loaders.getOrDefault(key, System.currentTimeMillis());
        if (current < System.currentTimeMillis()) current = System.currentTimeMillis();
        
        long newExpiry = current + millis;
        loaders.put(key, newExpiry);
        saveToDB(key, newExpiry); // Async Update

        Bukkit.getRegionScheduler().execute(plugin, key, () -> {
            key.getWorld().getChunkAt(key).addPluginChunkTicket(plugin);
            updateVisualsInternal(key);
        });
    }

    public Long getExpiry(Location loc) {
        Location stored = findStoredLocation(loc);
        if (stored != null) return loaders.get(stored);
        return null;
    }

    public boolean isLoaderAt(Location loc) {
        return findStoredLocation(loc) != null;
    }

    // --- TICKS & VISUALS (Same as before) ---

    private void tickExpiry() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Location, Long> entry : loaders.entrySet()) {
            Location loc = entry.getKey();
            long expires = entry.getValue();

            // Run on Region Thread
            Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
               if (!loc.isWorldLoaded()) return;
               
               if (now > expires) {
                   loc.getWorld().getChunkAt(loc).removePluginChunkTicket(plugin);
               } else {
                   loc.getWorld().getChunkAt(loc).addPluginChunkTicket(plugin);
               }
               updateVisualsInternal(loc);
            });
        }
    }

    private void tickSpawners() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Location, Long> entry : loaders.entrySet()) {
            if (now < entry.getValue()) { // If Active
                Location loc = entry.getKey();
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                    if (!loc.isChunkLoaded()) return;
                    Chunk chunk = loc.getChunk();
                    for (BlockState state : chunk.getTileEntities()) {
                        if (state instanceof CreatureSpawner spawner) {
                            handleVirtualSpawning(spawner);
                        }
                    }
                });
            }
        }
    }

    private void handleVirtualSpawning(CreatureSpawner spawner) {
        boolean realPlayerNear = spawner.getLocation().getWorld().getNearbyEntities(spawner.getLocation(), 16, 16, 16).stream()
                .anyMatch(e -> e.getType() == EntityType.PLAYER);

        if (realPlayerNear) return;

        int delay = spawner.getDelay();
        if (delay > 0) {
            spawner.setDelay(Math.max(0, delay - 20));
            spawner.update(); 
        } else {
            EntityType type = spawner.getSpawnedType();
            if (type != null && type.getEntityClass() != null) {
                Location spawnLoc = spawner.getLocation().add(0.5, 0.0, 0.5);
                try {
                    spawnLoc.getWorld().spawn(spawnLoc, type.getEntityClass(), CreatureSpawnEvent.SpawnReason.SPAWNER);
                    spawnLoc.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 5, 0.2, 0.2, 0.2, 0.05);
                } catch (Exception ignored) {}
            }
            spawner.setDelay(100 + ThreadLocalRandom.current().nextInt(200));
            spawner.update();
        }
    }

    private void updateVisuals(Location loc) {
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> updateVisualsInternal(loc));
    }

    private void updateVisualsInternal(Location loc) {
        if (!loc.isChunkLoaded()) return; 
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
            if (e instanceof ArmorStand as && e.getPersistentDataContainer().has(ChunkLoaderListener.KEY_LOADER_BOT)) {
                Long expiry = loaders.get(loc); 
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
}