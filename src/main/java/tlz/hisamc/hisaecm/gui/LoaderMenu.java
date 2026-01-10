package tlz.hisamc.hisaecm.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class LoaderMenu {
    public static final Component TITLE = Component.text("Chunk Loader Settings");
    public static final NamespacedKey KEY_LOC_X = new NamespacedKey("hisaecm", "loader_x");
    public static final NamespacedKey KEY_LOC_Y = new NamespacedKey("hisaecm", "loader_y");
    public static final NamespacedKey KEY_LOC_Z = new NamespacedKey("hisaecm", "loader_z");
    public static final NamespacedKey KEY_WORLD = new NamespacedKey("hisaecm", "loader_world");

    public static void open(Player player, Location loc, long expiry) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(Component.text("Status", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        
        long left = expiry - System.currentTimeMillis();
        iMeta.lore(Arrays.asList(
            Component.text("Time: " + (left > 0 ? (left / 3600000) + "h" : "Expired"), NamedTextColor.YELLOW),
            Component.text("Chunk: " + (loc.getBlockX() >> 4) + ", " + (loc.getBlockZ() >> 4), NamedTextColor.DARK_GRAY)
        ));

        iMeta.getPersistentDataContainer().set(KEY_LOC_X, PersistentDataType.INTEGER, loc.getBlockX());
        iMeta.getPersistentDataContainer().set(KEY_LOC_Y, PersistentDataType.INTEGER, loc.getBlockY());
        iMeta.getPersistentDataContainer().set(KEY_LOC_Z, PersistentDataType.INTEGER, loc.getBlockZ());
        iMeta.getPersistentDataContainer().set(KEY_WORLD, PersistentDataType.STRING, loc.getWorld().getName());
        info.setItemMeta(iMeta);
        
        gui.setItem(4, info);
        gui.setItem(11, create(Material.COAL, "&e+3 Hours"));
        gui.setItem(15, create(Material.COAL_BLOCK, "&6+24 Hours"));
        gui.setItem(22, create(Material.HOPPER, "&cPickup Bot"));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) if (gui.getItem(i) == null) gui.setItem(i, glass);
        player.openInventory(gui);
    }

    private static ItemStack create(Material m, String n) {
        ItemStack i = new ItemStack(m);
        ItemMeta me = i.getItemMeta();
        me.displayName(Component.text(n.replace("&", "§")).decoration(TextDecoration.ITALIC, false));
        i.setItemMeta(me); return i;
    }
}