package tlz.hisamc.hisaecm.listeners;

import me.clip.placeholderapi.PlaceholderAPI; 
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BountyGuiListener implements Listener {

    private final HisaECM plugin;
    private final BountyManager manager;
    private final Set<UUID> processing = ConcurrentHashMap.newKeySet();
    
    public static final NamespacedKey KEY_PAGE = new NamespacedKey("hisaecm", "bounty_page");
    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";

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

        if (slot == 45 || slot == 53) {
            if (event.getCurrentItem().getType() == Material.ARROW) {
                int page = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(KEY_PAGE, PersistentDataType.INTEGER);
                BountyMenu.open(player, page);
            }
            return;
        }

        if (slot == 48) {
            player.closeInventory();
            player.sendMessage(Component.text("Type the player name to raise bounty:", NamedTextColor.YELLOW));
            BountyListener.inputState.put(player.getUniqueId(), "RAISE_TARGET");
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            player.sendMessage(Component.text("Type the player name to bounty:", NamedTextColor.YELLOW));
            BountyListener.inputState.put(player.getUniqueId(), "PLACE_TARGET");
            return;
        }

        if (slot == 50) {
            if (processing.contains(player.getUniqueId())) return;
            processing.add(player.getUniqueId());
            
            double currentBounty = manager.getBounty(player.getName());
            if (currentBounty <= 0) {
                player.sendMessage(Component.text("No bounty to clear.", NamedTextColor.RED));
                processing.remove(player.getUniqueId());
                return;
            }
            
            double cost = currentBounty * 1.2;
            
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
                try {
                    if (!hasEnoughShards(player, cost)) {
                        player.sendMessage(Component.text("Need " + cost + " Shards to clear!", NamedTextColor.RED));
                        return;
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco take shards " + player.getName() + " " + cost);
                    manager.removeBounty(player.getName());
                    player.sendMessage(Component.text("Bounty cleared for " + cost + " Shards.", NamedTextColor.GREEN));
                    player.getScheduler().execute(plugin, () -> BountyMenu.open(player, 0), null, 0);
                } finally { processing.remove(player.getUniqueId()); }
            });
            return;
        }

        if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
            if (meta.getOwningPlayer() != null) {
                String target = meta.getOwningPlayer().getName();
                if (target != null) {
                    player.closeInventory();
                    BountyListener.tempTarget.put(player.getUniqueId(), target);
                    BountyListener.inputState.put(player.getUniqueId(), "RAISE_AMOUNT");
                    player.sendMessage(Component.text("Amount to add to " + target + "?", NamedTextColor.YELLOW));
                }
            }
        }
    }

    private boolean hasEnoughShards(Player player, double amount) {
        try {
            String raw = PlaceholderAPI.setPlaceholders(player, BALANCE_PLACEHOLDER);
            String clean = raw.replace(",", "").replaceAll("[^0-9.kK]", "").toLowerCase();
            double val = Double.parseDouble(clean.replace("k", ""));
            if (clean.contains("k")) val *= 1000;
            return val >= amount;
        } catch (Exception e) { return false; }
    }
}