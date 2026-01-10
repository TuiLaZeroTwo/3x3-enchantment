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
    private final Map<Location, Long> loaders = new ConcurrentHashMap<>();

    public ChunkLoaderManager(HisaECM plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        loadFromDB();
        
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tickExpiry(), 20L * 30L, 20L * 30L);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tickSpawners(), 20L, 20L);
    }

    public Map<Location, Long> getLoaders() {
        return this.loaders;
    }

    private void loadFromDB() {
        String query = "SELECT location, expiry FROM loaders";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String locStr = rs.getString("location");
                long expiry = rs.getLong("expiry");
                Location loc = stringToLoc(locStr);
                if (loc != null) loaders.put(loc, expiry);
            }
        } catch (SQLException e) { e.printStackTrace(); }
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
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void addLoader(Location loc) {
        long expiry = System.currentTimeMillis();
        loaders.put(loc, expiry);
        saveToDB(loc, expiry);
        updateVisuals(loc);
    }

    public void removeLoader(Location loc) {
        loaders.remove(loc);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM loaders WHERE location = ?")) {
                ps.setString(1, locToString(loc));
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void addTime(Location loc, long millis) {
        long current = loaders.getOrDefault(loc, System.currentTimeMillis());
        if (current < System.currentTimeMillis()) current = System.currentTimeMillis();
        long newExpiry = current + millis;
        loaders.put(loc, newExpiry);
        saveToDB(loc, newExpiry);
        updateVisuals(loc);
    }

    public Long getExpiry(Location loc) { return loaders.get(loc); }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location stringToLoc(String str) {
        try {
            String[] parts = str.split(",");
            return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]) + 0.5, Double.parseDouble(parts[2]), Double.parseDouble(parts[3]) + 0.5);
        } catch (Exception e) { return null; }
    }

    private void tickExpiry() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Location, Long> entry : loaders.entrySet()) {
            Location loc = entry.getKey();
            Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                if (now > entry.getValue()) loc.getChunk().removePluginChunkTicket(plugin);
                else loc.getChunk().addPluginChunkTicket(plugin);
                updateVisualsInternal(loc);
            });
        }
    }

    private void tickSpawners() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Location, Long> entry : loaders.entrySet()) {
            if (now < entry.getValue()) {
                Location loc = entry.getKey();
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                    if (!loc.isChunkLoaded()) return;
                    for (BlockState state : loc.getChunk().getTileEntities()) {
                        if (state instanceof CreatureSpawner spawner) handleVirtualSpawning(spawner);
                    }
                });
            }
        }
    }

    private void handleVirtualSpawning(CreatureSpawner spawner) {
        boolean playerNear = spawner.getWorld().getNearbyEntities(spawner.getLocation(), 16, 16, 16).stream().anyMatch(e -> e.getType() == EntityType.PLAYER);
        if (playerNear) return;
        int delay = spawner.getDelay();
        if (delay > 0) {
            spawner.setDelay(Math.max(0, delay - 20));
            spawner.update();
        } else {
            spawner.getWorld().spawn(spawner.getLocation().add(0.5, 0.5, 0.5), spawner.getSpawnedType().getEntityClass(), CreatureSpawnEvent.SpawnReason.SPAWNER);
            spawner.setDelay(200 + ThreadLocalRandom.current().nextInt(200));
            spawner.update();
        }
    }

    public void updateVisuals(Location loc) { Bukkit.getRegionScheduler().execute(plugin, loc, () -> updateVisualsInternal(loc)); }

    private void updateVisualsInternal(Location loc) {
        if (!loc.isChunkLoaded()) return;
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
            if (e instanceof ArmorStand as && e.getPersistentDataContainer().has(ChunkLoaderListener.KEY_LOADER_BOT)) {
                long timeLeft = loaders.getOrDefault(loc, 0L) - System.currentTimeMillis();
                if (timeLeft > 0) as.customName(Component.text("§b§lChunkBot: §e" + (timeLeft / 3600000) + "h " + ((timeLeft % 3600000) / 60000) + "m"));
                else as.customName(Component.text("§c§lChunkBot: §4OUT OF FUEL"));
            }
        }
    }
}