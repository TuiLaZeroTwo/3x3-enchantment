package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.BoosterMenu;

import java.io.File;
import java.util.*;

public class CropBoosterListener implements Listener {

    private final HisaECM plugin;
    private final Map<Location, Long> boosters = new HashMap<>();
    private final Random random = new Random();
    private final File boostersFile;

    public CropBoosterListener(HisaECM plugin) {
        this.plugin = plugin;
        this.boostersFile = new File(plugin.getDataFolder(), "boosters.yml");
        loadBoosters();
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> tickBoosters(), 100L, 100L);
    }

    public void addTime(Location loc, long millisToAdd) {
        if (boosters.containsKey(loc)) {
            long currentExpiry = boosters.get(loc);
            long now = System.currentTimeMillis();
            if (currentExpiry < now) currentExpiry = now;
            long newExpiry = currentExpiry + millisToAdd;
            boosters.put(loc, newExpiry);
            saveBoosters();
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> boosters.containsKey(block.getLocation()));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> boosters.containsKey(block.getLocation()));
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        // FIX: Use getInventory().getItem(getHand()) instead of deprecated getItemInHand()
        ItemStack item = event.getPlayer().getInventory().getItem(event.getHand());
        
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(ShopListener.KEY_BOOSTER, PersistentDataType.INTEGER)) {
            
            long durationSeconds;

            if (item.getItemMeta().getPersistentDataContainer().has(ShopListener.KEY_TIME_LEFT, PersistentDataType.LONG)) {
                durationSeconds = item.getItemMeta().getPersistentDataContainer().get(ShopListener.KEY_TIME_LEFT, PersistentDataType.LONG);
                event.getPlayer().sendMessage(Component.text("Restored Booster: " + formatDuration(durationSeconds * 1000), NamedTextColor.YELLOW));
            } else {
                durationSeconds = plugin.getConfig().getLong("shop.crop-booster.duration-seconds", 14400);
                event.getPlayer().sendMessage(Component.text("New Crop Booster Placed!", NamedTextColor.GREEN));
            }

            long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
            boosters.put(event.getBlock().getLocation(), expiryTime);
            saveBoosters();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && boosters.containsKey(event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
            BoosterMenu.open(event.getPlayer(), event.getClickedBlock().getLocation(), boosters.get(event.getClickedBlock().getLocation()));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (boosters.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Use the GUI (Right Click) to pick this up!", NamedTextColor.RED));
        }
    }

    private void tickBoosters() {
        long now = System.currentTimeMillis();
        List<Location> toRemove = new ArrayList<>();

        for (Map.Entry<Location, Long> entry : new HashMap<>(boosters).entrySet()) {
            Location loc = entry.getKey();
            if (now > entry.getValue()) {
                toRemove.add(loc);
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                    if (loc.getBlock().getType() != Material.AIR) {
                        loc.getBlock().setType(Material.AIR);
                        loc.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 1, 0.5f);
                    }
                });
            } else {
                Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
                    if (loc.getBlock().getType() != Material.AIR) pulseBooster(loc.getBlock());
                });
            }
        }
        if (!toRemove.isEmpty()) {
            for (Location l : toRemove) boosters.remove(l);
            saveBoosters();
        }
    }

    private void pulseBooster(Block center) {
        center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, center.getLocation().add(0.5, 1, 0.5), 5, 0.5, 0.5, 0.5);
        double multiplier = plugin.getConfig().getDouble("shop.crop-booster.multiplier", 1.0);
        int radius = 20;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {
                    Block target = center.getRelative(x, y, z);
                    if (target.getBlockData() instanceof Ageable ageable) {
                        if (ageable.getAge() >= ageable.getMaximumAge()) continue;
                        
                        double chance = 0.5 * multiplier;
                        while (chance >= 1.0) {
                            if (ageable.getAge() < ageable.getMaximumAge()) {
                                ageable.setAge(ageable.getAge() + 1);
                                chance -= 1.0;
                            } else break;
                        }
                        if (chance > 0 && random.nextDouble() < chance) {
                            if (ageable.getAge() < ageable.getMaximumAge()) ageable.setAge(ageable.getAge() + 1);
                        }
                        target.setBlockData(ageable);
                        target.getWorld().spawnParticle(Particle.COMPOSTER, target.getLocation().add(0.5, 0.5, 0.5), 1);
                    }
                }
            }
        }
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%dh %dm", hours, minutes);
    }

    public Map<Location, Long> getBoosters() { return boosters; }
    public void removeBooster(Location loc) { boosters.remove(loc); saveBoosters(); }

    public void saveBoosters() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> data = new ArrayList<>();
        for (Map.Entry<Location, Long> entry : boosters.entrySet()) {
            Location l = entry.getKey();
            data.add(l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," + entry.getValue());
        }
        yaml.set("active-boosters", data);
        try { yaml.save(boostersFile); } catch (Exception ignored) {}
    }

    private void loadBoosters() {
        if (!boostersFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(boostersFile);
        for (String line : yaml.getStringList("active-boosters")) {
            try {
                String[] parts = line.split(",");
                if (parts.length == 5) boosters.put(new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])), Long.parseLong(parts[4]));
            } catch (Exception ignored) {}
        }
    }
}