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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.ShopMenu;

import java.util.ArrayList;
import java.util.List;

public class ShopListener implements Listener {

    private final HisaECM plugin;
    public static final NamespacedKey KEY_BOOSTER = new NamespacedKey("hisaecm", "crop_booster_item");
    public static final NamespacedKey KEY_HOE = new NamespacedKey("hisaecm", "harvester_hoe_item");
    public static final NamespacedKey KEY_CHUNK = new NamespacedKey("hisaecm", "chunk_loader_item");

    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";

    public ShopListener(HisaECM plugin) { this.plugin = plugin; }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ShopMenu.TITLE)) return;
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        
        int slot = event.getSlot();

        if (slot == 11) buyItem(player, "crop-booster", KEY_BOOSTER);
        if (slot == 13) buyItem(player, "chunk-loader", KEY_CHUNK);
        if (slot == 15) buyItem(player, "harvester-hoe", KEY_HOE);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().title().equals(ShopMenu.TITLE)) {
            event.setCancelled(true);
        }
    }

    private void buyItem(Player player, String configKey, NamespacedKey key) {
        FileConfiguration config = plugin.getConfig();
        int price = config.getInt("shop." + configKey + ".price");
        
        if (!hasEnoughShards(player, price)) {
            player.sendMessage(Component.text("Not enough shards!", NamedTextColor.RED));
            return;
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Inventory full!", NamedTextColor.RED));
            return;
        }

        // 1. Deduct Balance via Console Command
        String cmd = "eco take shards " + player.getName() + " " + price;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

        // 2. Build the Item
        String matName = config.getString("shop." + configKey + ".material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String name = config.getString("shop." + configKey + ".name", "Item").replace("&", "§");
            String desc = config.getString("shop." + configKey + ".description", "").replace("&", "§");
            
            meta.displayName(Component.text(name));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(desc));
            meta.lore(lore);

            // Important: Apply PersistentData so the plugin recognizes the item
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
            item.setItemMeta(meta);
        }
        
        player.getInventory().addItem(item);
        player.sendMessage(Component.text("You bought a " + config.getString("shop." + configKey + ".name", "Item") + "!", NamedTextColor.GREEN));
    }

    private boolean hasEnoughShards(Player player, int amount) {
        try {
            // Converts "1.2k" or "2,000" into a double
            String rawBalance = PlaceholderAPI.setPlaceholders(player, BALANCE_PLACEHOLDER).toLowerCase();
            return parseValue(rawBalance) >= amount;
        } catch (Exception e) { 
            return false; 
        }
    }

    private double parseValue(String input) {
        String clean = input.replaceAll("[^0-9.km]", "");
        if (clean.isEmpty()) return 0;

        double multiplier = 1.0;
        if (clean.endsWith("k")) {
            multiplier = 1_000.0;
            clean = clean.substring(0, clean.length() - 1);
        } else if (clean.endsWith("m")) {
            multiplier = 1_000_000.0;
            clean = clean.substring(0, clean.length() - 1);
        }

        try {
            return Double.parseDouble(clean) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}