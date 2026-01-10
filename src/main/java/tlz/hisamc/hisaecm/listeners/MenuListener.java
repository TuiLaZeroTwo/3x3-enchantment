package tlz.hisamc.hisaecm.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.EnchantMenu;
import tlz.hisamc.hisaecm.util.EnchantKeys;

import java.util.ArrayList;
import java.util.List;

public class MenuListener implements Listener {
    private final HisaECM plugin;
    private final String BALANCE_P = "%zessentials_user_formatted_balance_shards%";

    public MenuListener(HisaECM plugin) { this.plugin = plugin; }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(EnchantMenu.TITLE)) return;
        event.setCancelled(true);
        ItemStack held = event.getWhoClicked().getInventory().getItemInMainHand();
        if (held.getType().isAir()) return;

        int slot = event.getSlot();
        Player p = (Player) event.getWhoClicked();

        if (slot == 10) buy(p, held, EnchantKeys.MINER_3X3, "miner-3x3");
        else if (slot == 12) buy(p, held, EnchantKeys.EXPLOSIVE, "explosive");
        else if (slot == 16) buy(p, held, EnchantKeys.VEIN_MINER, "vein-miner");
        else if (slot == 14) buy(p, held, EnchantKeys.HASTE, "haste");
        else if (slot == 28) buy(p, held, EnchantKeys.AUTO_SMELT, "auto-smelt");
        else if (slot == 30) buy(p, held, EnchantKeys.TELEKINESIS, "telekinesis");
    }

    private void buy(Player p, ItemStack item, NamespacedKey key, String config) {
        int price = plugin.getConfig().getInt("enchants." + config + ".price");
        if (!hasShards(p, price)) { p.sendMessage(Component.text("Not enough shards!", NamedTextColor.RED)); return; }
        
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) return;
        
        takeShards(p, price);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        lore.add(Component.text(plugin.getConfig().getString("enchants." + config + ".name").replace("&", "§")));
        meta.lore(lore);
        item.setItemMeta(meta);
        p.sendMessage(Component.text("Purchased!", NamedTextColor.GREEN));
    }

    private boolean hasShards(Player p, int amount) {
        try {
            String raw = PlaceholderAPI.setPlaceholders(p, BALANCE_P).replace(",", "").replaceAll("[^0-9.]", "");
            double val = Double.parseDouble(raw);
            if (raw.toLowerCase().contains("k")) val *= 1000;
            return val >= amount;
        } catch (Exception e) { return false; }
    }

    private void takeShards(Player p, int amount) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco take shards " + p.getName() + " " + amount));
    }
}