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

public class BountyMenu {

    public static final Component TITLE = Component.text("Bounties - Click to view");

    public static void open(Player player, int page) {
        BountyManager manager = HisaECM.getInstance().getBountyManager();
        Inventory gui = Bukkit.createInventory(null, 54, TITLE);

        List<Map.Entry<String, Double>> allBounties = new ArrayList<>(manager.getAllBounties().entrySet());
        // Sort by highest amount
        allBounties.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // --- PAGINATION LOGIC ---
        int slotsPerPage = 45;
        int startIndex = page * slotsPerPage;
        int endIndex = Math.min(startIndex + slotsPerPage, allBounties.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Double> entry = allBounties.get(i);
            gui.setItem(i - startIndex, createHead(entry.getKey(), entry.getValue()));
        }

        // --- CONTROLS (Bottom Row) ---
        
        // Previous Page (Slot 45)
        if (page > 0) {
            gui.setItem(45, createItem(Material.ARROW, "Previous Page", page - 1));
        }

        // Next Page (Slot 53)
        if (endIndex < allBounties.size()) {
            gui.setItem(53, createItem(Material.ARROW, "Next Page", page + 1));
        }

        // Raise Bounty (Slot 48)
        ItemStack raise = createItem(Material.GOLD_INGOT, "Raise Bounty", 0);
        ItemMeta rMeta = raise.getItemMeta();
        rMeta.lore(Arrays.asList(Component.text("Add money to an existing bounty.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        raise.setItemMeta(rMeta);
        gui.setItem(48, raise);

        // Place Bounty (Slot 49 - Middle)
        ItemStack place = createItem(Material.DIAMOND_SWORD, "Place Bounty", 0);
        ItemMeta pMeta = place.getItemMeta();
        pMeta.lore(Arrays.asList(
            Component.text("Click to set a bounty on", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("someone using chat.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        place.setItemMeta(pMeta);
        gui.setItem(49, place);

        // Revoke/Clear Bounty (Slot 50)
        ItemStack revoke = createItem(Material.TNT, "Clear My Bounty", 0);
        ItemMeta rvMeta = revoke.getItemMeta();
        rvMeta.lore(Arrays.asList(
            Component.text("Pay to remove the bounty", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("on your own head.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Cost: Bounty Value + 20% Tax", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
        ));
        revoke.setItemMeta(rvMeta);
        gui.setItem(50, revoke);

        // Glass Fillers
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.displayName(Component.empty());
        glass.setItemMeta(gMeta);
        for (int i = 45; i < 54; i++) {
            if (gui.getItem(i) == null) gui.setItem(i, glass);
        }

        player.openInventory(gui);
    }

    @SuppressWarnings("deprecation") // Suppresses warning for getOfflinePlayer(String)
    private static ItemStack createHead(String name, double amount) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        // FIX: Replaced setOwner(String) with setOwningPlayer(OfflinePlayer)
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(name)); 
        
        meta.displayName(Component.text(name, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
            Component.text("Bounty: ", NamedTextColor.GRAY).append(Component.text(amount + " Shards", NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Click to Raise Bounty", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        ));
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack createItem(Material mat, String name, int pageData) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        // Store Page Number in PDC for arrows
        meta.getPersistentDataContainer().set(BountyGuiListener.KEY_PAGE, PersistentDataType.INTEGER, pageData);
        item.setItemMeta(meta);
        return item;
    }
}