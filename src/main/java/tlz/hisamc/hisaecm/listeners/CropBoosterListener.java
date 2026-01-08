package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.io.File;
import java.util.*;

public class CropBoosterListener implements Listener {

    private final HisaECM plugin;
    // Map stores <Location, ExpiryTime (Millis)>
    private final Map<Location, Long> boosters = new HashMap<>();
    private final Random random = new Random();
    private final File boostersFile;

    public CropBoosterListener(HisaECM plugin) {
        this.plugin = plugin;
        this.boostersFile = new File(plugin.getDataFolder(), "boosters.yml");
        
        loadBoosters();

        // Run Global Task every 5 seconds (100 ticks)
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            tickBoosters();
        }, 100L, 100L);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getItemInHand().getItemMeta().getPersistentDataContainer().has(ShopListener.KEY_BOOSTER, PersistentDataType.INTEGER)) {
            
            // Calculate Expiry
            long durationSeconds = plugin.getConfig().getLong("shop.crop-booster.duration-seconds", 14400); // Default 4 hours
            long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
            
            boosters.put(event.getBlock().getLocation(), expiryTime);
            event.getPlayer().sendMessage(org.bukkit.ChatColor.GREEN + "Crop Booster Placed! Active for " + (durationSeconds / 3600) + " hours.");
            saveBoosters(); // Save immediately
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (boosters.containsKey(event.getBlock().getLocation())) {
            boosters.remove(event.getBlock().getLocation());
            event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "Crop Booster Destroyed.");
            event.setDropItems(false);
            saveBoosters();
        }
    }

    private void tickBoosters() {
        long now = System.currentTimeMillis();
        List<Location> toRemove = new ArrayList<>();

        for (Map.Entry<Location, Long> entry : new HashMap<>(boosters).entrySet()) {
            Location loc = entry.getKey();
            Long expiresAt = entry.getValue();

            // 1. Check Expiry
            if (now > expiresAt) {
                toRemove.add(loc);
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                    if (loc.getBlock().getType() != Material.AIR) {
                        loc.getBlock().setType(Material.AIR); // Remove block
                        loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 10);
                        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1, 0.5f);
                    }
                });
                continue;
            }

            // 2. Pulse if active
            Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                if (loc.getBlock().getType() == Material.AIR) {
                    // Block gone but still in map? Remove it next tick logic
                } else {
                    pulseBooster(loc.getBlock());
                }
            });
        }

        // Clean up expired
        if (!toRemove.isEmpty()) {
            for (Location l : toRemove) boosters.remove(l);
            saveBoosters();
        }
    }

    private void pulseBooster(Block center) {
        center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, center.getLocation().add(0.5, 1, 0.5), 10, 0.5, 0.5, 0.5);
        int radius = 20;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {
                    Block target = center.getRelative(x, y, z);
                    if (target.getBlockData() instanceof Ageable ageable) {
                        if (ageable.getAge() < ageable.getMaximumAge() && random.nextBoolean()) {
                            ageable.setAge(ageable.getAge() + 1);
                            target.setBlockData(ageable);
                            target.getWorld().spawnParticle(Particle.COMPOSTER, target.getLocation().add(0.5, 0.5, 0.5), 1);
                        }
                    }
                }
            }
        }
    }

    // --- Persistence Logic ---

    public void saveBoosters() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> data = new ArrayList<>();
        
        for (Map.Entry<Location, Long> entry : boosters.entrySet()) {
            Location l = entry.getKey();
            // Format: World,x,y,z,ExpiryTime
            String line = l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," + entry.getValue();
            data.add(line);
        }
        
        yaml.set("active-boosters", data);
        try { yaml.save(boostersFile); } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadBoosters() {
        if (!boostersFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(boostersFile);
        List<String> data = yaml.getStringList("active-boosters");
        
        for (String line : data) {
            try {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    org.bukkit.World w = Bukkit.getWorld(parts[0]);
                    if (w == null) continue;
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    long expire = Long.parseLong(parts[4]);
                    
                    boosters.put(new Location(w, x, y, z), expire);
                }
            } catch (Exception e) { 
                plugin.getLogger().warning("Skipped invalid booster entry: " + line); 
            }
        }
    }
}