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
import tlz.hisamc.hisaecm.listeners.ChunkLoaderListener;

import java.util.Arrays;
import java.util.List;

public class LoaderMenu {

    public static final Component TITLE = Component.text("Chunk Loader Settings");
    public static final NamespacedKey KEY_LOC_X = new NamespacedKey("hisaecm", "loader_x");
    public static final NamespacedKey KEY_LOC_Y = new NamespacedKey("hisaecm", "loader_y");
    public static final NamespacedKey KEY_LOC_Z = new NamespacedKey("hisaecm", "loader_z");
    public static final NamespacedKey KEY_WORLD = new NamespacedKey("hisaecm", "loader_world");

    public static void open(Player player, Location loc, long expiryTime) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        // --- 1. INFO ICON (Center) ---
        ItemStack info = new ItemStack(Material.ENDER_EYE);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.displayName(Component.text("Loader Status", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        
        long timeLeft = expiryTime - System.currentTimeMillis();
        String timeStr = timeLeft > 0 ? formatTime(timeLeft) : "Expired";
        NamedTextColor statusColor = timeLeft > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;

        iMeta.lore(Arrays.asList(
            Component.text("Status: ", NamedTextColor.GRAY).append(Component.text(timeLeft > 0 ? "ACTIVE" : "INACTIVE", statusColor)),
            Component.text("Time Left: ", NamedTextColor.GRAY).append(Component.text(timeStr, NamedTextColor.YELLOW)),
            Component.empty(),
            Component.text("Chunk: " + loc.getChunk().getX() + ", " + loc.getChunk().getZ(), NamedTextColor.DARK_GRAY)
        ));

        // Store Location in PDC so the Listener knows which bot to update
        iMeta.getPersistentDataContainer().set(KEY_LOC_X, PersistentDataType.INTEGER, loc.getBlockX());
        iMeta.getPersistentDataContainer().set(KEY_LOC_Y, PersistentDataType.INTEGER, loc.getBlockY());
        iMeta.getPersistentDataContainer().set(KEY_LOC_Z, PersistentDataType.INTEGER, loc.getBlockZ());
        iMeta.getPersistentDataContainer().set(KEY_WORLD, PersistentDataType.STRING, loc.getWorld().getName());
        
        info.setItemMeta(iMeta);
        gui.setItem(13, info);

        // --- 2. FUEL OPTIONS ---
        gui.setItem(11, createItem(Material.COAL, "&eAdd 3 Hours", Arrays.asList("&7Cost: &f1 Coal Block")));
        gui.setItem(15, createItem(Material.COAL_BLOCK, "&6Add 24 Hours", Arrays.asList("&7Cost: &f8 Coal Blocks", "&a&lBEST VALUE")));

        // --- 3. PICKUP BUTTON ---
        gui.setItem(22, createItem(Material.HOPPER, "&cPickup Bot", Arrays.asList("&7Returns the bot to your inventory.")));

        // Fill Glass
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