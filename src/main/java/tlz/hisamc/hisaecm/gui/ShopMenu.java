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

        // Crop Booster (Slot 13)
        gui.setItem(13, createIcon(
                Material.valueOf(config.getString("shop.crop-booster.material", "BEACON")),
                "crop-booster",
                config
        ));

        player.openInventory(gui);
    }

    private static ItemStack createIcon(Material mat, String key, FileConfiguration config) {
        String path = "shop." + key;
        String name = config.getString(path + ".name");
        int price = config.getInt(path + ".price");
        String desc = config.getString(path + ".description");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(name, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
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