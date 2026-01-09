package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CropBoosterListener implements Listener {

    private final HisaECM plugin;
    private final DatabaseManager db;
    private final Map<Location, Long> boosters = new ConcurrentHashMap<>();

    public CropBoosterListener(HisaECM plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        loadFromDB();
        
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tickBoosters(), 100L, 100L);
    }
    
    // --- PUBLIC METHODS FOR GUI ---
    
    public Map<Location, Long> getBoosters() {
        return boosters;
    }

    public void addTime(Location loc, long millis) {
        long currentExpiry = boosters.getOrDefault(loc, System.currentTimeMillis());
        if (currentExpiry < System.currentTimeMillis()) currentExpiry = System.currentTimeMillis();
        
        long newExpiry = currentExpiry + millis;
        boosters.put(loc, newExpiry);
        saveToDB(loc, newExpiry);
    }

    public void removeBooster(Location loc) {
        boosters.remove(loc);
        deleteFromDB(loc);
        
        // Break the block visually on main thread
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            if (loc.getBlock().getType() == Material.BEACON) {
                loc.getBlock().breakNaturally();
            }
        });
    }

    // --- DB OPERATIONS ---
    private void loadFromDB() {
        String query = "SELECT location, expiry FROM boosters";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String locStr = rs.getString("location");
                long expiry = rs.getLong("expiry");
                Location loc = stringToLoc(locStr);
                if (loc != null) boosters.put(loc, expiry);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void saveToDB(Location loc, long expiry) {
        String locStr = locToString(loc);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            String query = "INSERT OR REPLACE INTO boosters (location, expiry) VALUES (?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, locStr);
                ps.setLong(2, expiry);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    private void deleteFromDB(Location loc) {
        String locStr = locToString(loc);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            String query = "DELETE FROM boosters WHERE location = ?";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, locStr);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }
    
    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    
    private Location stringToLoc(String str) {
        try {
            String[] parts = str.split(",");
            return new Location(Bukkit.getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        } catch (Exception e) { return null; }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() == Material.BEACON && item.getItemMeta().getDisplayName().contains("Crop Booster")) {
            long duration = plugin.getConfig().getLong("shop.crop-booster.duration-seconds", 14400) * 1000L;
            long expiry = System.currentTimeMillis() + duration;
            
            Location loc = event.getBlock().getLocation();
            boosters.put(loc, expiry);
            saveToDB(loc, expiry);
            
            event.getPlayer().sendMessage(Component.text("Crop Booster placed! Growing crops nearby...", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (boosters.containsKey(loc)) {
            boosters.remove(loc);
            deleteFromDB(loc);
            event.getPlayer().sendMessage(Component.text("Crop Booster removed.", NamedTextColor.RED));
        }
    }

    private void tickBoosters() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Location, Long> entry : boosters.entrySet()) {
            Location loc = entry.getKey();
            if (now > entry.getValue()) {
                removeBooster(loc);
                continue;
            }

            Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                if (!loc.isChunkLoaded()) return;
                
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        Block b = loc.clone().add(x, 0, z).getBlock();
                        if (b.getBlockData() instanceof Ageable crop) {
                            if (crop.getAge() < crop.getMaximumAge()) {
                                crop.setAge(Math.min(crop.getMaximumAge(), crop.getAge() + 1));
                                b.setBlockData(crop);
                                b.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, b.getLocation().add(0.5, 0.5, 0.5), 1);
                            }
                        }
                    }
                }
            });
        }
    }

    public void saveBoosters() {}
}