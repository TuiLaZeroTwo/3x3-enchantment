package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.LoaderMenu;
import tlz.hisamc.hisaecm.util.ChunkLoaderManager;

import java.util.HashMap;

public class LoaderGuiListener implements Listener {

    private final HisaECM plugin;
    private final ChunkLoaderManager manager;

    public LoaderGuiListener(HisaECM plugin, ChunkLoaderManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(LoaderMenu.TITLE)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        ItemStack info = event.getInventory().getItem(4);
        if (info == null || !info.hasItemMeta()) return;
        ItemMeta im = info.getItemMeta();

        String worldName = im.getPersistentDataContainer().get(LoaderMenu.KEY_WORLD, PersistentDataType.STRING);
        Integer x = im.getPersistentDataContainer().get(LoaderMenu.KEY_LOC_X, PersistentDataType.INTEGER);
        Integer y = im.getPersistentDataContainer().get(LoaderMenu.KEY_LOC_Y, PersistentDataType.INTEGER);
        Integer z = im.getPersistentDataContainer().get(LoaderMenu.KEY_LOC_Z, PersistentDataType.INTEGER);

        if (worldName == null || x == null) return;

        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);

        // --- BUTTON LOGIC ---

        if (slot == 11) addFuel(player, loc, 1, 10800000L);
        else if (slot == 15) addFuel(player, loc, 8, 86400000L);

        // --- SHOW RADIUS (Slot 13) ---
        else if (slot == 13) {
            player.closeInventory();
            player.sendMessage(Component.text("Showing loaded chunk borders...", NamedTextColor.AQUA));
            showChunkBorders(player, loc);
        }

        // --- PICKUP (Slot 22) ---
        else if (slot == 22) {
            player.closeInventory();
            manager.removeLoader(loc);
            
            for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                if (e instanceof ArmorStand && e.getPersistentDataContainer().has(ChunkLoaderListener.KEY_LOADER_BOT)) {
                    e.remove();
                }
            }

            ItemStack item = new ItemStack(Material.ARMOR_STAND);
            item.editMeta(meta -> meta.displayName(Component.text("§b§lChunkLoader Bot")));
            
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
                player.sendMessage(Component.text("Inventory full! Item dropped on ground.", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("ChunkLoader Bot picked up.", NamedTextColor.YELLOW));
            }
        }
    }

    private void addFuel(Player player, Location loc, int cost, long timeToAdd) {
        if (player.getInventory().containsAtLeast(new ItemStack(Material.COAL_BLOCK), cost)) {
            player.getInventory().removeItem(new ItemStack(Material.COAL_BLOCK, cost));
            manager.addTime(loc, timeToAdd);
            player.sendMessage(Component.text("Added fuel successfully!", NamedTextColor.GREEN));
            LoaderMenu.open(player, loc, manager.getExpiry(loc));
        } else {
            player.sendMessage(Component.text("You need " + cost + " Coal Blocks for this!", NamedTextColor.RED));
        }
    }

    private void showChunkBorders(Player player, Location center) {
        Chunk chunk = center.getChunk();
        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        
        int py = player.getLocation().getBlockY() + 1; 

        for (int i = 0; i < 10; i++) {
            long delay = (i * 10L) + 1L; 

            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (task) -> {
                if (!player.isOnline()) return;
                
                World w = player.getWorld();
                Particle.DustOptions dust = new Particle.DustOptions(Color.AQUA, 1.0f);

                // FIX: Use Particle.REDSTONE for colored dust (DUST is for newer versions)
                for (int d = 0; d <= 16; d += 1) {
                    w.spawnParticle(Particle.REDSTONE, minX + d, py, minZ, 1, dust);
                    w.spawnParticle(Particle.REDSTONE, minX + d, py, minZ + 16, 1, dust);
                    w.spawnParticle(Particle.REDSTONE, minX, py, minZ + d, 1, dust);
                    w.spawnParticle(Particle.REDSTONE, minX + 16, py, minZ + d, 1, dust);
                }
            }, delay);
        }
    }
}