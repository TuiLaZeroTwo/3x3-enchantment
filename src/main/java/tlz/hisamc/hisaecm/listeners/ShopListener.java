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

import java.util.Map;

public class ShopListener implements Listener {

    private final HisaECM plugin;
    public static final NamespacedKey KEY_BOOSTER = new NamespacedKey("hisaecm", "crop_booster_item");
    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";

    public ShopListener(HisaECM plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(ShopMenu.TITLE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        
        // Slot 13 = Crop Booster
        if (event.getSlot() == 13) {
            buyBooster(player);
        }
    }

    private void buyBooster(Player player) {
        FileConfiguration config = plugin.getConfig();
        int price = config.getInt("shop.crop-booster.price");
        
        if (!hasEnoughShards(player, price)) {
            player.sendMessage(Component.text("Not enough shards!", NamedTextColor.RED));
            return;
        }
        
        // Check inv space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(Component.text("Inventory full!", NamedTextColor.RED));
            return;
        }

        // Take Money
        String cmd = "eco take shards " + player.getName() + " " + price;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));

        // Give Item
        ItemStack booster = new ItemStack(Material.valueOf(config.getString("shop.crop-booster.material", "BEACON")));
        ItemMeta meta = booster.getItemMeta();
        meta.displayName(Component.text("Crop Booster", NamedTextColor.GREEN));
        meta.getPersistentDataContainer().set(KEY_BOOSTER, PersistentDataType.INTEGER, 1);
        booster.setItemMeta(meta);
        
        player.getInventory().addItem(booster);
        player.sendMessage(Component.text("Purchased Crop Booster!", NamedTextColor.GREEN));
    }

    private boolean hasEnoughShards(Player player, int amount) {
        try {
            String b = PlaceholderAPI.setPlaceholders(player, BALANCE_PLACEHOLDER).replaceAll("[^0-9.]", "");
            return !b.isEmpty() && Double.parseDouble(b) >= amount;
        } catch (Exception e) { return false; }
    }
}