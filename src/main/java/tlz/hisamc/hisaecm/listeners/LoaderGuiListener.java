package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.LoaderMenu;
import tlz.hisamc.hisaecm.util.ChunkLoaderManager;

import java.util.Collections;
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

        // --- BUTTONS ---
        if (slot == 11) addFuel(player, loc, 1, 10800000L);
        else if (slot == 15) addFuel(player, loc, 8, 86400000L);
        else if (slot == 13) {
            player.closeInventory();
            showChunkBorders(player, loc);
        }

        // --- PICKUP (Save Time Logic) ---
        else if (slot == 22) {
            player.closeInventory();
            
            // 1. Calculate Remaining Time
            Long expiry = manager.getExpiry(loc);
            long timeLeft = 0;
            if (expiry != null && expiry > System.currentTimeMillis()) {
                timeLeft = expiry - System.currentTimeMillis();
            }

            // 2. Remove from DB/World
            manager.removeLoader(loc);
            for (Entity e : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                if (e instanceof ArmorStand && e.getPersistentDataContainer().has(ChunkLoaderListener.KEY_LOADER_BOT)) {
                    e.remove();
                }
            }

            // 3. Create Item with Stored Time
            ItemStack item = new ItemStack(Material.ARMOR_STAND);
            long finalTimeLeft = timeLeft;
            
            item.editMeta(meta -> {
                meta.displayName(Component.text("§b§lChunkLoader Bot").decoration(TextDecoration.ITALIC, false));
                if (finalTimeLeft > 0) {
                    // Save exact milliseconds to PDC
                    meta.getPersistentDataContainer().set(ChunkLoaderListener.KEY_STORED_TIME, PersistentDataType.LONG, finalTimeLeft);
                    
                    long hours = finalTimeLeft / 3600000;
                    long mins = (finalTimeLeft % 3600000) / 60000;
                    meta.lore(Collections.singletonList(
                        Component.text("§7Fuel Stored: §e" + hours + "h " + mins + "m").decoration(TextDecoration.ITALIC, false)
                    ));
                }
            });

            // 4. Give Item
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            if (!overflow.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
                player.sendMessage(Component.text("Inventory full! Item dropped.", NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("Picked up! Stored fuel preserved.", NamedTextColor.GREEN));
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
        // (Existing display entity logic...)
        Chunk chunk = center.getChunk();
        int minX = chunk.getX() * 16;
        int minZ = chunk.getZ() * 16;
        double y = player.getLocation().getY();
        Location origin = new Location(player.getWorld(), minX, y, minZ);

        Bukkit.getRegionScheduler().execute(plugin, origin, () -> {
            BlockDisplay display = (BlockDisplay) origin.getWorld().spawnEntity(origin, EntityType.BLOCK_DISPLAY);
            display.setBlock(Material.LIGHT_BLUE_STAINED_GLASS.createBlockData());
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0), new org.joml.Quaternionf(),
                new Vector3f(16f, 0.1f, 16f), new org.joml.Quaternionf()
            ));
            display.setGlowing(true);
            display.setGlowColorOverride(Color.AQUA); 
            Bukkit.getRegionScheduler().runDelayed(plugin, origin, (task) -> display.remove(), 100L);
        });
    }
}