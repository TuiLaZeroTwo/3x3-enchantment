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

public class BoosterMenu {

    public static final Component TITLE = Component.text("Crop Booster Settings");
    public static final NamespacedKey KEY_LOC_X = new NamespacedKey("hisaecm", "booster_x");
    public static final NamespacedKey KEY_LOC_Y = new NamespacedKey("hisaecm", "booster_y");
    public static final NamespacedKey KEY_LOC_Z = new NamespacedKey("hisaecm", "booster_z");
    public static final NamespacedKey KEY_WORLD = new NamespacedKey("hisaecm", "booster_world");

    public static void open(Player player, Location loc, long expiryTime) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        // --- 1. INFO ICON ---
        ItemStack info = new ItemStack(Material.BEACON);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(Component.text("Booster Status", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        
        long timeLeft = expiryTime - System.currentTimeMillis();
        String timeStr = timeLeft > 0 ? formatTime(timeLeft) : "Expired";
        
        iMeta.lore(Arrays.asList(
            Component.text("Time Left: ", NamedTextColor.GRAY).append(Component.text(timeStr, NamedTextColor.YELLOW)),
            Component.text("Range: ", NamedTextColor.GRAY).append(Component.text("20x20 Area", NamedTextColor.AQUA))
        ));

        iMeta.getPersistentDataContainer().set(KEY_LOC_X, PersistentDataType.INTEGER, loc.getBlockX());
        iMeta.getPersistentDataContainer().set(KEY_LOC_Y, PersistentDataType.INTEGER, loc.getBlockY());
        iMeta.getPersistentDataContainer().set(KEY_LOC_Z, PersistentDataType.INTEGER, loc.getBlockZ());
        iMeta.getPersistentDataContainer().set(KEY_WORLD, PersistentDataType.STRING, loc.getWorld().getName());
        
        info.setItemMeta(iMeta);
        gui.setItem(4, info);

        // --- 2. FUEL OPTIONS ---
        gui.setItem(11, createItem(Material.DIAMOND, "&bAdd 1 Hour", Arrays.asList("&7Cost: &f4 Diamonds")));
        gui.setItem(15, createItem(Material.EMERALD_BLOCK, "&aAdd 24 Hours", Arrays.asList("&7Cost: &f32 Emerald Blocks")));

        // --- 3. SHOW RADIUS ---
        gui.setItem(13, createItem(Material.ENDER_EYE, "&eShow Range", Arrays.asList("&7Visualize the &a20x20&7 effect area.")));

        // --- 4. PICKUP ---
        gui.setItem(22, createItem(Material.HOPPER, "&cPickup Booster", Arrays.asList("&7Returns the item to inventory.")));

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, glass);
        }

        player.openInventory(gui);
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name.replace("&", "§")).decoration(TextDecoration.ITALIC, false));
        if (lore != null) {
            meta.lore(lore.stream().map(l -> Component.text(l.replace("&", "§")).decoration(TextDecoration.ITALIC, false)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String formatTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        return String.format("%dh %dm", hours, minutes);
    }
}