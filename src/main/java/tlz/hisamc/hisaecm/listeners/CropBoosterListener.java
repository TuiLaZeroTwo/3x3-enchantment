package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.BoosterMenu;
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
    
    // Key to identify our hologram armor stand
    public static final NamespacedKey KEY_BOOSTER_HOLO = new NamespacedKey("hisaecm", "booster_holo");

    public CropBoosterListener(HisaECM plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        loadFromDB();
        
        // Tick every 1 second (20 ticks) for smooth updates
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tickBoosters(), 20L, 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON) return;

        if (boosters.containsKey(block.getLocation())) {
            event.setCancelled(true);
            long expiry = boosters.get(block.getLocation());
            BoosterMenu.open(event.getPlayer(), block.getLocation(), expiry);
        }
    }
    
    public Map<Location, Long> getBoosters() { return boosters; }

    public void addTime(Location loc, long millis) {
        long currentExpiry = boosters.getOrDefault(loc, System.currentTimeMillis());
        if (currentExpiry < System.currentTimeMillis()) currentExpiry = System.currentTimeMillis();
        long newExpiry = currentExpiry + millis;
        boosters.put(loc, newExpiry);
        saveToDB(loc, newExpiry);
        
        // Force visual update immediately
        updateHologram(loc, newExpiry);
    }

    public void removeBooster(Location loc) {
        boosters.remove(loc);
        deleteFromDB(loc);
        
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            // Remove Hologram
            removeHologram(loc);
            
            // Break Block
            if (loc.getBlock().getType() == Material.BEACON) {
                loc.getBlock().breakNaturally();
            }
        });
    }

    // --- HOLOGRAM LOGIC ---
    
    private void updateHologram(Location loc, long expiry) {
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
            if (!loc.isChunkLoaded()) return;

            // Center above the beacon
            Location holoLoc = loc.clone().add(0.5, 1.2, 0.5); 
            ArmorStand holo = null;

            // 1. Try to find existing hologram
            for (Entity e : holoLoc.getWorld().getNearbyEntities(holoLoc, 0.5, 0.5, 0.5)) {
                if (e instanceof ArmorStand as && e.getPersistentDataContainer().has(KEY_BOOSTER_HOLO)) {
                    holo = as;
                    break;
                }
            }

            // 2. If not found, spawn new one
            if (holo == null) {
                holo = (ArmorStand) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
                holo.setGravity(false);
                holo.setSmall(true);
                holo.setInvisible(true); // Invisible stand, visible name
                holo.setMarker(true);    // Tiny hitbox
                holo.setCustomNameVisible(true);
                holo.getPersistentDataContainer().set(KEY_BOOSTER_HOLO, PersistentDataType.INTEGER, 1);
            }

            // 3. Update Text
            long timeLeft = expiry - System.currentTimeMillis();
            if (timeLeft > 0) {
                long hours = timeLeft / 3600000;
                long minutes = (timeLeft % 3600000) / 60000;
                holo.customName(Component.text("§a§lBooster: §e" + hours + "h " + minutes + "m"));
            } else {
                holo.customName(Component.text("§c§lBooster: §4EXPIRED"));
            }
        });
    }

    private void removeHologram(Location loc) {
        Location holoLoc = loc.clone().add(0.5, 1.2, 0.5);
        if (!loc.isChunkLoaded()) return;
        
        for (Entity e : holoLoc.getWorld().getNearbyEntities(holoLoc, 1, 2, 1)) {
            if (e instanceof ArmorStand && e.getPersistentDataContainer().has(KEY_BOOSTER_HOLO)) {
                e.remove();
            }
        }
    }

    // --- DB OPERATIONS ---
    private void loadFromDB() {
        String query = "SELECT location, expiry FROM boosters";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Location loc = stringToLoc(rs.getString("location"));
                if (loc != null) boosters.put(loc, rs.getLong("expiry"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void saveToDB(Location loc, long expiry) {
        String locStr = locToString(loc);
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            String query = "INSERT OR REPLACE INTO boosters (location, expiry) VALUES (?, ?)";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
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
        if (item.getType() == Material.BEACON && item.hasItemMeta() && 
            item.getItemMeta().getDisplayName().contains("Crop Booster")) {
            
            long duration = plugin.getConfig().getLong("shop.crop-booster.duration-seconds", 14400) * 1000L;
            long expiry = System.currentTimeMillis() + duration;
            
            Location loc = event.getBlock().getLocation();
            boosters.put(loc, expiry);
            saveToDB(loc, expiry);
            
            // Spawn Hologram Immediately
            updateHologram(loc, expiry);
            
            event.getPlayer().sendMessage(Component.text("Crop Booster placed! Growing crops nearby...", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (boosters.containsKey(loc)) {
            boosters.remove(loc);
            deleteFromDB(loc);
            removeHologram(loc); // Kill the hologram
            event.getPlayer().sendMessage(Component.text("Crop Booster removed.", NamedTextColor.RED));
        }
    }

    private void tickBoosters() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Location, Long> entry : boosters.entrySet()) {
            Location loc = entry.getKey();
            
            // Update Hologram Text every tick
            updateHologram(loc, entry.getValue());

            if (now > entry.getValue()) {
                removeBooster(loc);
                continue;
            }

            // Grow Crops (Every tick might be too fast? Maybe run this part less often if laggy)
            // Since we tick every 20 ticks (1s) in constructor, this is fine.
            Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                if (!loc.isChunkLoaded()) return;
                
                // 20x20 Radius (10 blocks each side)
                for (int x = -10; x <= 10; x++) {
                    for (int z = -10; z <= 10; z++) {
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