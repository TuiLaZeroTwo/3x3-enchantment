package tlz.hisamc.hisaecm.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.EnchantMenu;

import java.util.ArrayList;
import java.util.List;

public class MenuListener implements Listener {

    private final HisaECM plugin;
    private final NamespacedKey key3x3;
    private final NamespacedKey keyExplosive;
    private final NamespacedKey keyHaste;

    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";

    public MenuListener(HisaECM plugin) {
        this.plugin = plugin;
        this.key3x3 = new NamespacedKey(plugin, "enchant_3x3");
        this.keyExplosive = new NamespacedKey(plugin, "enchant_explosive");
        this.keyHaste = new NamespacedKey(plugin, "enchant_haste");
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(EnchantMenu.TITLE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (!Tag.ITEMS_PICKAXES.isTagged(heldItem.getType())) {
            player.sendMessage(Component.text("You must hold a Pickaxe!", NamedTextColor.RED));
            return;
        }

        int slot = event.getSlot();

        if (slot == 11) {
            buyEnchant(player, heldItem, key3x3, "miner-3x3");
        } else if (slot == 13) {
            buyEnchant(player, heldItem, keyExplosive, "explosive");
        } else if (slot == 15) {
            buyEnchant(player, heldItem, keyHaste, "haste");
        }
    }

    private void buyEnchant(Player player, ItemStack item, NamespacedKey key, String configName) {
        FileConfiguration config = plugin.getConfig();
        int price = config.getInt("enchants." + configName + ".price");
        String displayName = config.getString("enchants." + configName + ".name");

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(key, PersistentDataType.INTEGER)) {
            player.sendMessage(Component.text("You already have " + displayName + "!", NamedTextColor.RED));
            return;
        }

        if (!hasEnoughShards(player, price)) {
            player.sendMessage(Component.text("Not enough shards! Need: " + price, NamedTextColor.RED));
            return;
        }

        // Take money (FIXED THREADING)
        takeShards(player, price);

        // Apply Enchant
        container.set(key, PersistentDataType.INTEGER, 1);
        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.add(Component.text(displayName, NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);

        player.sendMessage(Component.text("Purchased " + displayName + "!", NamedTextColor.GREEN));
        player.closeInventory();
    }

    private boolean hasEnoughShards(Player player, int amount) {
        try {
            String balanceStr = PlaceholderAPI.setPlaceholders(player, BALANCE_PLACEHOLDER);
            String cleanBalance = balanceStr.replaceAll("[^0-9.]", "");
            if (cleanBalance.isEmpty()) return false;
            return Double.parseDouble(cleanBalance) >= amount;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse balance: " + e.getMessage());
            return false;
        }
    }

    private void takeShards(Player player, int amount) {
        String cmd = "eco take " + " shards " + player.getName() + " " + amount;

        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        });
    }
}