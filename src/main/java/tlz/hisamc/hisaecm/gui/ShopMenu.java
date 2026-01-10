package tlz.hisamc.hisaecm.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.Arrays;

public class ShopMenu {

    public static final Component TITLE = Component.text("Hisa Item Shop");

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        FileConfiguration config = HisaECM.getInstance().getConfig();

        // 1. Crop Booster (Slot 11)
        gui.setItem(11, createIcon(
                Material.valueOf(config.getString("shop.crop-booster.material", "BEACON")),
                "crop-booster", config
        ));
        
        // 2. Chunk Loader Bot (Slot 13)
        gui.setItem(13, createIcon(
                Material.valueOf(config.getString("shop.chunk-loader.material", "ARMOR_STAND")),
                "chunk-loader", config
        ));

        // 3. Harvester Hoe (Slot 15)
        gui.setItem(15, createIcon(
                Material.valueOf(config.getString("shop.harvester-hoe.material", "DIAMOND_HOE")),
                "harvester-hoe", config
        ));

        // Fill background with glass
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        if (gMeta != null) {
            gMeta.displayName(Component.text(" "));
            glass.setItemMeta(gMeta);
        }
        
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, glass);
        }

        player.openInventory(gui);
    }

    private static ItemStack createIcon(Material mat, String key, FileConfiguration config) {
        String path = "shop." + key;
        String name = config.getString(path + ".name", "Unknown Item").replace("&", "§");
        int price = config.getInt(path + ".price", 0);
        String desc = config.getString(path + ".description", "").replace("&", "§");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(
                    Component.text(desc).decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Cost: ", NamedTextColor.GRAY)
                        .append(Component.text(formatPrice(price) + " Shards", NamedTextColor.AQUA))
                        .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatPrice(double value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fk", value / 1_000.0);
        return String.valueOf((int)value);
    }
}