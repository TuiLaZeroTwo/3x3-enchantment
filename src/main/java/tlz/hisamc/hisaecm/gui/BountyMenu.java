package tlz.hisamc.hisaecm.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.listeners.BountyGuiListener;
import tlz.hisamc.hisaecm.util.BountyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyMenu {

    public static final Component TITLE = Component.text("Bounties - Click to view");

    public static void open(Player player, int page) {
        BountyManager manager = HisaECM.getInstance().getBountyManager();
        Inventory gui = Bukkit.createInventory(null, 54, TITLE);

        List<Map.Entry<String, Double>> allBounties = new ArrayList<>(manager.getAllBounties().entrySet());
        allBounties.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int slotsPerPage = 45;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, allBounties.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Double> entry = allBounties.get(i);
            gui.setItem(i - startIndex, createHead(entry.getKey(), entry.getValue()));
        }

        if (page > 0) gui.setItem(45, createItem(Material.ARROW, "Previous Page", page - 1));
        if (endIndex < allBounties.size()) gui.setItem(53, createItem(Material.ARROW, "Next Page", page + 1));

        gui.setItem(48, createItem(Material.GOLD_INGOT, "Raise Bounty", 0));
        gui.setItem(49, createItem(Material.DIAMOND_SWORD, "Place Bounty", 0));
        gui.setItem(50, createItem(Material.TNT, "Clear My Bounty", 0));

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.displayName(Component.empty());
        glass.setItemMeta(gMeta);
        for (int i = 45; i < 54; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, glass);
        }

        player.openInventory(gui);
    }

    private static ItemStack createHead(String identifier, double amount) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null && identifier != null) {
            if (identifier.length() <= 16) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(identifier));
            } else if (identifier.length() == 36 || identifier.length() == 32) {
                try {
                    UUID uuid = identifier.length() == 32 
                        ? UUID.fromString(identifier.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"))
                        : UUID.fromString(identifier);
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
                } catch (IllegalArgumentException e) { }
            }

            meta.displayName(Component.text(identifier, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(Arrays.asList(
                Component.text("Bounty: ", NamedTextColor.GRAY).append(Component.text(amount + " Shards", NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Click to Raise Bounty", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
            ));
            head.setItemMeta(meta);
        }
        return head;
    }

    private static ItemStack createItem(Material mat, String name, int pageData) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(BountyGuiListener.KEY_PAGE, PersistentDataType.INTEGER, pageData);
            item.setItemMeta(meta);
        }
        return item;
    }
}