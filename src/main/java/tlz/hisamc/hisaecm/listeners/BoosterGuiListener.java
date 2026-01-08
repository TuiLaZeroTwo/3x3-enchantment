package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.BoosterMenu;

public class BoosterGuiListener implements Listener {

    private final HisaECM plugin;
    private final CropBoosterListener boosterLogic;
    
    // Keys to retrieve location from the GUI
    public static final NamespacedKey KEY_LOC_X = new NamespacedKey("hisaecm", "bx");
    public static final NamespacedKey KEY_LOC_Y = new NamespacedKey("hisaecm", "by");
    public static final NamespacedKey KEY_LOC_Z = new NamespacedKey("hisaecm", "bz");

    public BoosterGuiListener(HisaECM plugin, CropBoosterListener logic) {
        this.plugin = plugin;
        this.boosterLogic = logic;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(BoosterMenu.TITLE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;
        Player player = (Player) event.getWhoClicked();

        // 1. Retrieve Location from the Info Icon (Slot 4)
        ItemStack infoItem = event.getInventory().getItem(4);
        if (infoItem == null) return;
        
        PersistentDataContainer pdc = infoItem.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(KEY_LOC_X, PersistentDataType.INTEGER)) return;

        int x = pdc.get(KEY_LOC_X, PersistentDataType.INTEGER);
        int y = pdc.get(KEY_LOC_Y, PersistentDataType.INTEGER);
        int z = pdc.get(KEY_LOC_Z, PersistentDataType.INTEGER);
        Location loc = new Location(player.getWorld(), x, y, z);

        // 2. Handle Clicks
        
        // --- ADD TIME (Slot 11) ---
        if (event.getSlot() == 11) {
            handleAddTime(player, loc, event.getClick());
        }
        
        // --- VISUALIZE (Slot 13) ---
        else if (event.getSlot() == 13) {
            showVisuals(player, loc);
            player.closeInventory();
        }
        
        // --- PICK UP (Slot 15) ---
        else if (event.getSlot() == 15) {
            handlePickup(player, loc);
        }
    }

    private void handleAddTime(Player player, Location loc, ClickType click) {
        // Find booster items in inventory
        ItemStack[] contents = player.getInventory().getContents();
        int count = 0;
        
        for (ItemStack item : contents) {
            if (isBoosterItem(item)) {
                count += item.getAmount();
            }
        }

        if (count == 0) {
            player.sendMessage(Component.text("You don't have any Crop Boosters in your inventory!", NamedTextColor.RED));
            return;
        }

        long baseDuration = plugin.getConfig().getLong("shop.crop-booster.duration-seconds", 14400) * 1000;
        int used = 0;

        if (click.isShiftClick()) {
            // Use ALL
            used = count;
            removeBoosters(player, used);
            boosterLogic.addTime(loc, baseDuration * used);
            player.sendMessage(Component.text("Added " + used + " boosters worth of time!", NamedTextColor.GREEN));
        } else {
            // Use ONE
            used = 1;
            removeBoosters(player, 1);
            boosterLogic.addTime(loc, baseDuration);
            player.sendMessage(Component.text("Added 4 hours!", NamedTextColor.GREEN));
        }
        
        // Refresh GUI
        long newExpiry = boosterLogic.getBoosters().get(loc);
        BoosterMenu.open(player, loc, newExpiry);
    }

    private void handlePickup(Player player, Location loc) {
        // Remove block
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
             loc.getBlock().setType(Material.AIR);
        });

        // Remove data
        boosterLogic.removeBooster(loc);
        
        // Give item
        ItemStack booster = new ItemStack(Material.valueOf(plugin.getConfig().getString("shop.crop-booster.material", "BEACON")));
        ItemMeta meta = booster.getItemMeta();
        meta.displayName(Component.text("Crop Booster", NamedTextColor.GREEN));
        meta.getPersistentDataContainer().set(ShopListener.KEY_BOOSTER, PersistentDataType.INTEGER, 1);
        booster.setItemMeta(meta);
        
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(booster);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), booster);
        }

        player.sendMessage(Component.text("Booster picked up!", NamedTextColor.YELLOW));
        player.closeInventory();
    }

    private void showVisuals(Player player, Location center) {
        int radius = 20;
        int minX = center.getBlockX() - radius;
        int maxX = center.getBlockX() + radius;
        int minY = Math.max(-64, center.getBlockY() - radius);
        int maxY = Math.min(319, center.getBlockY() + radius);
        int minZ = center.getBlockZ() - radius;
        int maxZ = center.getBlockZ() + radius;

        player.sendMessage(Component.text("Displaying range for 10 seconds...", NamedTextColor.AQUA));

        // Send Red Wool Packets
        sendBox(player, minX, maxX, minY, maxY, minZ, maxZ, Material.RED_WOOL);

        // Clear after 10 seconds
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (task) -> {
            sendBox(player, minX, maxX, minY, maxY, minZ, maxZ, Material.AIR); 
        }, 200L); // 200 ticks = 10s
    }

    private void sendBox(Player player, int x1, int x2, int y1, int y2, int z1, int z2, Material mat) {
        // Draw the 12 edges of the cube
        drawLine(player, x1, y1, z1, x2, y1, z1, mat);
        drawLine(player, x1, y2, z1, x2, y2, z1, mat);
        drawLine(player, x1, y1, z2, x2, y1, z2, mat);
        drawLine(player, x1, y2, z2, x2, y2, z2, mat);

        drawLine(player, x1, y1, z1, x1, y2, z1, mat);
        drawLine(player, x2, y1, z1, x2, y2, z1, mat);
        drawLine(player, x1, y1, z2, x1, y2, z2, mat);
        drawLine(player, x2, y1, z2, x2, y2, z2, mat);

        drawLine(player, x1, y1, z1, x1, y1, z2, mat);
        drawLine(player, x2, y1, z1, x2, y1, z2, mat);
        drawLine(player, x1, y2, z1, x1, y2, z2, mat);
        drawLine(player, x2, y2, z1, x2, y2, z2, mat);
    }

    private void drawLine(Player p, int x1, int y1, int z1, int x2, int y2, int z2, Material mat) {
        // Simple loop to send block changes along the line
        // Since we are drawing axis-aligned boxes, we can just iterate.
        int xMin = Math.min(x1, x2); int xMax = Math.max(x1, x2);
        int yMin = Math.min(y1, y2); int yMax = Math.max(y1, y2);
        int zMin = Math.min(z1, z2); int zMax = Math.max(z1, z2);

        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    // Send Client-Side Block Change
                    p.sendBlockChange(new Location(p.getWorld(), x, y, z), mat.createBlockData());
                }
            }
        }
    }

    private boolean isBoosterItem(ItemStack item) {
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(ShopListener.KEY_BOOSTER, PersistentDataType.INTEGER);
    }

    private void removeBoosters(Player player, int amountToRemove) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isBoosterItem(item)) {
                if (item.getAmount() <= amountToRemove) {
                    amountToRemove -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - amountToRemove);
                    amountToRemove = 0;
                }
                if (amountToRemove <= 0) break;
            }
        }
    }
}