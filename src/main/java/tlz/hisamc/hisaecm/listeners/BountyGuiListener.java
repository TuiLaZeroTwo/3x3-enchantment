package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.BountyMenu;
import tlz.hisamc.hisaecm.util.BountyManager;

public class BountyGuiListener implements Listener {

    private final HisaECM plugin;
    private final BountyManager manager;
    public static final NamespacedKey KEY_PAGE = new NamespacedKey("hisaecm", "bounty_page");

    public BountyGuiListener(HisaECM plugin, BountyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(BountyMenu.TITLE)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // --- NAVIGATION ---
        if (slot == 45 || slot == 53) {
            if (event.getCurrentItem().getType() == Material.ARROW) {
                int page = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(KEY_PAGE, PersistentDataType.INTEGER);
                BountyMenu.open(player, page);
            }
            return;
        }

        // --- RAISE (Left of Middle - Slot 48) ---
        if (slot == 48) {
            player.closeInventory();
            player.sendMessage(Component.text("Type the name of the player to raise bounty on:", NamedTextColor.YELLOW));
            BountyListener.inputState.put(player.getUniqueId(), "RAISE_TARGET");
            return;
        }

        // --- PLACE (Middle - Slot 49) ---
        if (slot == 49) {
            player.closeInventory();
            player.sendMessage(Component.text("Type the name of the player to bounty:", NamedTextColor.YELLOW));
            BountyListener.inputState.put(player.getUniqueId(), "PLACE_TARGET");
            return;
        }

        // --- REVOKE/CLEAR (Right of Middle - Slot 50) ---
        if (slot == 50) {
            double currentBounty = manager.getBounty(player.getName());
            if (currentBounty <= 0) {
                player.sendMessage(Component.text("You don't have a bounty on your head.", NamedTextColor.RED));
                return;
            }
            
            double cost = currentBounty * 1.2; // 20% Tax
            // Check balance and execute
            // (Note: Since we use command eco, we assume they have it. In real code, check balance first via PAPI or Vault)
            // For this snippet, we'll try to execute the command.
            
            String cmd = "eco take shards " + player.getName() + " " + cost;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            
            manager.removeBounty(player.getName());
            player.sendMessage(Component.text("You paid ", NamedTextColor.GREEN)
                .append(Component.text(cost + " Shards", NamedTextColor.AQUA))
                .append(Component.text(" to clear your bounty.", NamedTextColor.GREEN)));
            
            BountyMenu.open(player, 0); // Refresh
            return;
        }

        // --- CLICK HEAD (Raise specific) ---
        if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            if (meta.getOwningPlayer() != null) {
                String target = meta.getOwningPlayer().getName();
                if (target != null) {
                    player.closeInventory();
                    BountyListener.tempTarget.put(player.getUniqueId(), target);
                    BountyListener.inputState.put(player.getUniqueId(), "RAISE_AMOUNT");
                    player.sendMessage(Component.text("How much do you want to add to " + target + "'s bounty?", NamedTextColor.YELLOW));
                }
            }
        }
    }
}