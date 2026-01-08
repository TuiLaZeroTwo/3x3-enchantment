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

import java.util.Arrays;

public class MainMenu {

    public static final Component TITLE = Component.text("Hisa-ECM Main Menu");

    public static void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TITLE);

        // 1. Enchant Menu (Slot 11)
        ItemStack enchant = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta encMeta = enchant.getItemMeta();
        encMeta.displayName(Component.text("Custom Enchants", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        encMeta.lore(Arrays.asList(
            Component.text("Click to browse custom", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("mining enchantments.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        enchant.setItemMeta(encMeta);
        gui.setItem(11, enchant);

        // 2. Shop Menu (Slot 15)
        ItemStack shop = new ItemStack(Material.EMERALD);
        ItemMeta shopMeta = shop.getItemMeta();
        shopMeta.displayName(Component.text("Item Shop", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        shopMeta.lore(Arrays.asList(
            Component.text("Click to buy utility items", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("like Crop Boosters.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        shop.setItemMeta(shopMeta);
        gui.setItem(15, shop);

        // 3. Guidance Book (Slot 22 - Bottom Middle)
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        bookMeta.displayName(Component.text("Plugin Guide", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        bookMeta.lore(Arrays.asList(
            Component.empty(),
            Component.text("How to use:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
            Component.text("1. Get a Pickaxe.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
            Component.text("2. Buy enchants using Shards.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
            Component.text("3. Combine with Auto-Smelt & Telekinesis", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
            Component.text("   for maximum efficiency!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Note: 3x3, Explosive, and VeinMiner", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
            Component.text("cannot be used together.", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
        ));
        book.setItemMeta(bookMeta);
        gui.setItem(22, book);

        // Filler Glass (Optional, makes it look nicer)
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(Component.empty());
        glass.setItemMeta(glassMeta);
        
        // Fill empty spots if you want, or just leave empty
        // for (int i = 0; i < 27; i++) { if (gui.getItem(i) == null) gui.setItem(i, glass); }

        player.openInventory(gui);
    }
}