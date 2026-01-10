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
import tlz.hisamc.hisaecm.util.EnchantKeys;

import java.util.ArrayList;
import java.util.List;

public class MenuListener implements Listener {

    private final HisaECM plugin;
    private final NamespacedKey key3x3;
    private final NamespacedKey keyExplosive;
    private final NamespacedKey keyVein;
    private final NamespacedKey keyHaste;
    private final NamespacedKey keySmelt;
    private final NamespacedKey keyTele;

    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";

    public MenuListener(HisaECM plugin) {
        this.plugin = plugin;
        this.key3x3 = EnchantKeys.MINER_3X3;
        this.keyExplosive = EnchantKeys.EXPLOSIVE;
        this.keyVein = EnchantKeys.VEIN_MINER;
        this.keyHaste = EnchantKeys.HASTE;
        this.keySmelt = EnchantKeys.AUTO_SMELT;
        this.keyTele = EnchantKeys.TELEKINESIS;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(EnchantMenu.TITLE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("You must hold a tool!", NamedTextColor.RED));
            return;
        }

        int slot = event.getSlot();

        if (slot == 10) {
            if (!isValidTool(heldItem, "PICKAXE", "SHOVEL")) return;
            buyEnchant(player, heldItem, key3x3, "miner-3x3", keyExplosive, keyVein);
        } else if (slot == 12) {
            if (!isValidTool(heldItem, "PICKAXE", "SHOVEL")) return;
            buyEnchant(player, heldItem, keyExplosive, "explosive", key3x3, keyVein);
        } else if (slot == 16) {
            if (!isValidTool(heldItem, "PICKAXE", "AXE")) return;
            buyEnchant(player, heldItem, keyVein, "vein-miner", key3x3, keyExplosive);
        } else if (slot == 14) buyEnchant(player, heldItem, keyHaste, "haste");
        else if (slot == 28) buyEnchant(player, heldItem, keySmelt, "auto-smelt");
        else if (slot == 30) buyEnchant(player, heldItem, keyTele, "telekinesis");
    }

    private boolean isValidTool(ItemStack item, String... allowedTypes) {
        for (String type : allowedTypes) {
            if (type.equals("PICKAXE") && Tag.ITEMS_PICKAXES.isTagged(item.getType())) return true;
            if (type.equals("AXE") && Tag.ITEMS_AXES.isTagged(item.getType())) return true;
            if (type.equals("SHOVEL") && Tag.ITEMS_SHOVELS.isTagged(item.getType())) return true;
        }
        return false; 
    }

    private void buyEnchant(Player player, ItemStack item, NamespacedKey key, String configName, NamespacedKey... conflicts) {
        FileConfiguration config = plugin.getConfig();
        int price = config.getInt("enchants." + configName + ".price");
        String displayName = config.getString("enchants." + configName + ".name");

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        if (container.has(key, PersistentDataType.INTEGER)) {
            player.sendMessage(Component.text("You already have " + displayName + "!", NamedTextColor.RED));
            return;
        }

        for (NamespacedKey conflict : conflicts) {
            if (container.has(conflict, PersistentDataType.INTEGER)) {
                player.sendMessage(Component.text("This conflicts with your current mining enchant!", NamedTextColor.RED));
                return;
            }
        }

        if (!hasEnoughShards(player, price)) {
            player.sendMessage(Component.text("Not enough shards! Need: " + price, NamedTextColor.RED));
            return;
        }

        takeShards(player, price);

        container.set(key, PersistentDataType.INTEGER, 1);
        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.add(Component.text(displayName.replace("&", "§")).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);

        player.sendMessage(Component.text("Purchased " + displayName + "!", NamedTextColor.GREEN));
        player.closeInventory();
    }

    private boolean hasEnoughShards(Player player, int amount) {
        try {
            String bRaw = PlaceholderAPI.setPlaceholders(player, BALANCE_PLACEHOLDER);
            
            String clean = bRaw.replace(",", "").replaceAll("[^0-9.]", "");
            if (clean.isEmpty()) return false;

            double current = Double.parseDouble(clean);
            
            if (bRaw.toLowerCase().contains("k")) current *= 1000;
            if (bRaw.toLowerCase().contains("m")) current *= 1000000;

            return current >= (double) amount;
        } catch (Exception e) { 
            return false; 
        }
    }

    private void takeShards(Player player, int amount) {
        String cmd = "eco take shards " + player.getName() + " " + amount;
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
    }
}