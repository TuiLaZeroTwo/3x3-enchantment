package tlz.hisamc.hisaecm.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.BountyMenu;
import tlz.hisamc.hisaecm.util.BountyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BountyListener implements Listener {

    private final HisaECM plugin;
    private final BountyManager manager;
    
    // State Management for Chat Input
    public static final Map<UUID, String> inputState = new HashMap<>();
    public static final Map<UUID, String> tempTarget = new HashMap<>();

    public BountyListener(HisaECM plugin, BountyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!inputState.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String state = inputState.get(player.getUniqueId());

        // --- FLOW: PLACE NEW BOUNTY ---
        if (state.equals("PLACE_TARGET")) {
            tempTarget.put(player.getUniqueId(), message);
            inputState.put(player.getUniqueId(), "PLACE_AMOUNT");
            player.sendMessage(Component.text("Target set to " + message + ". How much Shards?", NamedTextColor.YELLOW));
            return;
        }

        if (state.equals("PLACE_AMOUNT")) {
            try {
                double amount = Double.parseDouble(message);
                if (amount <= 0) throw new NumberFormatException();
                
                String target = tempTarget.get(player.getUniqueId());
                processTransaction(player, target, amount);
                
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number. Cancelled.", NamedTextColor.RED));
            }
            clearState(player);
            return;
        }

        // --- FLOW: RAISE BOUNTY ---
        if (state.equals("RAISE_TARGET")) {
            if (manager.getBounty(message) <= 0) {
                player.sendMessage(Component.text("That player has no bounty. Use 'Place Bounty' instead.", NamedTextColor.RED));
                clearState(player);
                return;
            }
            tempTarget.put(player.getUniqueId(), message);
            inputState.put(player.getUniqueId(), "RAISE_AMOUNT");
            player.sendMessage(Component.text("How much to add?", NamedTextColor.YELLOW));
            return;
        }

        if (state.equals("RAISE_AMOUNT")) {
            try {
                double amount = Double.parseDouble(message);
                if (amount <= 0) throw new NumberFormatException();
                String target = tempTarget.get(player.getUniqueId());
                processTransaction(player, target, amount);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid number. Cancelled.", NamedTextColor.RED));
            }
            clearState(player);
        }
    }

    private void processTransaction(Player player, String target, double amount) {
        // Run on main thread for Bukkit Commands
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Note: Add logic here to check if player actually has money using Vault or PlaceholderAPI
            // For now, we attempt to take it.
            String cmd = "eco take shards " + player.getName() + " " + amount;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

            manager.addBounty(target, amount);
            player.sendMessage(Component.text("Bounty placed/raised successfully!", NamedTextColor.GREEN));
            
            // Broadcast
            Bukkit.broadcast(Component.text(player.getName() + " placed a " + amount + " shard bounty on " + target + "!", NamedTextColor.RED));
            
            // Re-open GUI
            BountyMenu.open(player, 0);
        });
    }

    private void clearState(Player player) {
        inputState.remove(player.getUniqueId());
        tempTarget.remove(player.getUniqueId());
    }

    // --- PVP LOGIC ---
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        double bounty = manager.getBounty(victim.getName());
        if (bounty > 0) {
            manager.removeBounty(victim.getName());
            
            if (killer != null && !killer.equals(victim)) {
                // Give money to Killer
                String cmd = "eco give shards " + killer.getName() + " " + bounty;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                
                killer.sendMessage(Component.text("You claimed the bounty of " + bounty + " Shards!", NamedTextColor.GOLD));
                Bukkit.broadcast(Component.text(killer.getName() + " claimed the bounty on " + victim.getName() + "!", NamedTextColor.GOLD));
            } else {
                Bukkit.broadcast(Component.text("The bounty on " + victim.getName() + " was lost in the void.", NamedTextColor.GRAY));
            }
        }
    }
}