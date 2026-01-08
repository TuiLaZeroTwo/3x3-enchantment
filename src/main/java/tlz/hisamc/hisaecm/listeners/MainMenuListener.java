package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.EnchantMenu;
import tlz.hisamc.hisaecm.gui.MainMenu;
import tlz.hisamc.hisaecm.gui.ShopMenu;

public class MainMenuListener implements Listener {

    private final HisaECM plugin;

    public MainMenuListener(HisaECM plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(MainMenu.TITLE)) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        Player player = (Player) event.getWhoClicked();

        // Slot 11: Enchant
        if (event.getSlot() == 11) {
            EnchantMenu.open(player); // Switch to Enchant GUI
        }
        
        // Slot 15: Shop
        else if (event.getSlot() == 15) {
            ShopMenu.open(player); // Switch to Shop GUI
        }
        
        // Slot 22: Guide (Just prevents taking the item, maybe play a sound)
        else if (event.getSlot() == 22) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1, 1);
        }
    }
}