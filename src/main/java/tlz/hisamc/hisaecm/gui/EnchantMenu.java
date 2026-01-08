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

public class EnchantMenu {

    public static final Component TITLE = Component.text("Hisa Custom Enchants");

    public static void open(Player player) {
        // Increased size to 36 to fit more items
        Inventory gui = Bukkit.createInventory(null, 36, TITLE);
        FileConfiguration config = HisaECM.getInstance().getConfig();

        // Row 1
        gui.setItem(10, createIcon(Material.NETHER_STAR, "miner-3x3", config));
        gui.setItem(12, createIcon(Material.TNT, "explosive", config));
        gui.setItem(14, createIcon(Material.GOLDEN_PICKAXE, "haste", config));
        gui.setItem(16, createIcon(Material.DIAMOND, "vein-miner", config));
        
        // Row 2
        gui.setItem(28, createIcon(Material.FURNACE, "auto-smelt", config));
        gui.setItem(30, createIcon(Material.ENDER_PEARL, "telekinesis", config));

        player.openInventory(gui);
    }

    private static ItemStack createIcon(Material mat, String configKey, FileConfiguration config) {
        String name = config.getString("enchants." + configKey + ".name");
        int price = config.getInt("enchants." + configKey + ".price");
        String desc = config.getString("enchants." + configKey + ".description");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text(desc, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Cost: ", NamedTextColor.GRAY)
                    .append(Component.text(price + " Shards", NamedTextColor.AQUA))
                    .decoration(TextDecoration.ITALIC, false)
        ));
        
        item.setItemMeta(meta);
        return item;
    }
}