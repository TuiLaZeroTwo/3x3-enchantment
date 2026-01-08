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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.ShopMenu;

public class ShopListener implements Listener {

    private final HisaECM plugin;
    public static final NamespacedKey KEY_BOOSTER = new NamespacedKey("hisaecm", "crop_booster_item");
    public static final NamespacedKey KEY_HOE = new NamespacedKey("hisaecm", "harvester_hoe_item");
    
    // NEW KEY: Stores remaining time (in seconds) on the ItemStack
    public static final NamespacedKey KEY_TIME_LEFT = new NamespacedKey("hisaecm", "booster_time_left");

    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";

    public ShopListener(HisaECM plugin) { this.plugin = plugin; }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ShopMenu.TITLE)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        
        if (event.getSlot() == 11) buyItem(player, "crop-booster", KEY_BOOSTER);
        if (event.getSlot() == 15) buyItem(player, "harvester-hoe", KEY_HOE);
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

        String cmd = "eco take shards " + player.getName() + " " + price;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

        Material mat = Material.valueOf(config.getString("shop." + configKey + ".material"));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(config.getString("shop." + configKey + ".name"), NamedTextColor.GREEN));
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        
        player.getInventory().addItem(item);
        player.sendMessage(Component.text("Purchased " + config.getString("shop." + configKey + ".name") + "!", NamedTextColor.GREEN));
    }

    private boolean hasEnoughShards(Player player, int amount) {
        try {
            String b = PlaceholderAPI.setPlaceholders(player, BALANCE_PLACEHOLDER).replaceAll("[^0-9.]", "");
            return !b.isEmpty() && Double.parseDouble(b) >= amount;
        } catch (Exception e) { return false; }
    }
}