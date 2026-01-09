package tlz.hisamc.hisaecm.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent; // Fix for Menu Steal
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.ShopMenu;

public class ShopListener implements Listener {

    private final HisaECM plugin;
    public static final NamespacedKey KEY_BOOSTER = new NamespacedKey("hisaecm", "crop_booster_item");
    public static final NamespacedKey KEY_HOE = new NamespacedKey("hisaecm", "harvester_hoe_item");
    
    // Defines time for boosters
    public static final NamespacedKey KEY_TIME_LEFT = new NamespacedKey("hisaecm", "booster_time_left");

    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";

    public ShopListener(HisaECM plugin) { this.plugin = plugin; }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ShopMenu.TITLE)) return;
        event.setCancelled(true); // Stop taking items
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        
        int slot = event.getSlot();

        if (slot == 11) buyItem(player, "crop-booster", KEY_BOOSTER);
        if (slot == 13) buyItem(player, "chunk-loader", null); // Slot 13: Chunk Loader (No specific key needed on item, name is checked)
        if (slot == 15) buyItem(player, "harvester-hoe", KEY_HOE);
    }

    // --- SECURITY FIX: Prevent dragging items to steal them ---
    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().title().equals(ShopMenu.TITLE)) {
            event.setCancelled(true);
        }
    }

    private void buyItem(Player player, String configKey, NamespacedKey key) {
        FileConfiguration config = plugin.getConfig();
        int price = config.getInt("shop." + configKey + ".price");
        
        // 1. Check Money
        if (!hasEnoughShards(player, price)) {
            player.sendMessage(Component.text("Not enough shards!", NamedTextColor.RED));
            return;
        }
        
        // 2. Check Inventory Space (Prevents "Item Void" Bug)
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Inventory full! Clear some space first.", NamedTextColor.RED));
            return;
        }

        // 3. Take Money
        String cmd = "eco take shards " + player.getName() + " " + price;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

        // 4. Create Item
        String matName = config.getString("shop." + configKey + ".material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE; // Fallback

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String name = config.getString("shop." + configKey + ".name", "Item").replace("&", "§");
        
        meta.displayName(Component.text(name));
        
        // Apply special keys if needed (Boosters/Hoes)
        if (key != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        }
        item.setItemMeta(meta);
        
        // 5. Give Item
        player.getInventory().addItem(item);
        player.sendMessage(Component.text("Purchased " + name + "!", NamedTextColor.GREEN));
    }

    private boolean hasEnoughShards(Player player, int amount) {
        try {
            String b = PlaceholderAPI.setPlaceholders(player, BALANCE_PLACEHOLDER).replaceAll("[^0-9.]", "");
            return !b.isEmpty() && Double.parseDouble(b) >= amount;
        } catch (Exception e) { return false; }
    }
}