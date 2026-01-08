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
import java.util.List;

public class EnchantMenu {

    public static final Component TITLE = Component.text("Hisa Custom Enchants");

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        FileConfiguration config = HisaECM.getInstance().getConfig();

        // 1. 3x3 Miner (Slot 11)
        gui.setItem(11, createIcon(
            Material.NETHER_STAR, 
            config.getString("enchants.miner-3x3.name"), 
            config.getInt("enchants.miner-3x3.price"),
            config.getString("enchants.miner-3x3.description")
        ));

        // 2. Explosive (Slot 13)
        gui.setItem(13, createIcon(
            Material.TNT, 
            config.getString("enchants.explosive.name"), 
            config.getInt("enchants.explosive.price"),
            config.getString("enchants.explosive.description")
        ));

        // 3. Haste (Slot 15)
        gui.setItem(15, createIcon(
            Material.GOLDEN_PICKAXE, 
            config.getString("enchants.haste.name"), 
            config.getInt("enchants.haste.price"),
            config.getString("enchants.haste.description")
        ));

        player.openInventory(gui);
    }

    private static ItemStack createIcon(Material mat, String name, int price, String desc) {
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