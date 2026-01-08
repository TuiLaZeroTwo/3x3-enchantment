package tlz.hisamc.hisaecm.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.listeners.BoosterGuiListener;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class BoosterMenu {

    public static final Component TITLE = Component.text("Crop Booster Settings");

    public static void open(Player player, Location loc, long expiryTime) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        long timeLeftMillis = expiryTime - System.currentTimeMillis();
        String timeString = formatTime(timeLeftMillis);

        // 1. Info Icon (Center)
        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("Status: ", NamedTextColor.GRAY).append(Component.text("Active", NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false));
        infoMeta.lore(Arrays.asList(
                Component.text("Time Remaining: ", NamedTextColor.GRAY).append(Component.text(timeString, NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Location: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        // Store Location in item to identify which booster we are editing
        infoMeta.getPersistentDataContainer().set(BoosterGuiListener.KEY_LOC_X, PersistentDataType.INTEGER, loc.getBlockX());
        infoMeta.getPersistentDataContainer().set(BoosterGuiListener.KEY_LOC_Y, PersistentDataType.INTEGER, loc.getBlockY());
        infoMeta.getPersistentDataContainer().set(BoosterGuiListener.KEY_LOC_Z, PersistentDataType.INTEGER, loc.getBlockZ());
        info.setItemMeta(infoMeta);
        gui.setItem(4, info);

        // 2. Add Time (Left)
        ItemStack addTime = new ItemStack(Material.EMERALD);
        ItemMeta addMeta = addTime.getItemMeta();
        addMeta.displayName(Component.text("Add Time", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        addMeta.lore(Arrays.asList(
                Component.text("Requires 'Crop Booster' items in inventory.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Right-Click: ", NamedTextColor.YELLOW).append(Component.text("Use 1 Item", NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false),
                Component.text("Shift-Right-Click: ", NamedTextColor.YELLOW).append(Component.text("Use All Items", NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false)
        ));
        addTime.setItemMeta(addMeta);
        gui.setItem(11, addTime);

        // 3. Visualize Range (Right)
        ItemStack visual = new ItemStack(Material.SPYGLASS);
        ItemMeta visMeta = visual.getItemMeta();
        visMeta.displayName(Component.text("Show Range", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        visMeta.lore(Arrays.asList(
            Component.text("Click to toggle a visual box", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("showing the effective area.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        visual.setItemMeta(visMeta);
        gui.setItem(13, visual);

        // 4. Pickup (Far Right)
        ItemStack pickup = new ItemStack(Material.HOPPER);
        ItemMeta pickMeta = pickup.getItemMeta();
        pickMeta.displayName(Component.text("Pick Up Booster", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        pickMeta.lore(Arrays.asList(
                Component.text("Remove the booster and return", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("it to your inventory.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        pickup.setItemMeta(pickMeta);
        gui.setItem(15, pickup);

        player.openInventory(gui);
    }

    private static String formatTime(long millis) {
        if (millis <= 0) return "Expired";
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return String.format("%dh %dm", hours, minutes);
    }
}