package tlz.hisamc.hisaecm.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private final String BALANCE_PLACEHOLDER = "%zessentials_user_formatted_balance_shards%";
    
    public static final Map<UUID, String> inputState = new HashMap<>();
    public static final Map<UUID, String> tempTarget = new HashMap<>();

    public BountyListener(HisaECM plugin, BountyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!inputState.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        String state = inputState.get(player.getUniqueId());

        if (state.equals("PLACE_TARGET")) {
            tempTarget.put(player.getUniqueId(), message);
            inputState.put(player.getUniqueId(), "PLACE_AMOUNT");
            player.sendMessage(Component.text("Bounty amount for " + message + "?", NamedTextColor.YELLOW));
            return;
        }

        if (state.equals("PLACE_AMOUNT") || state.equals("RAISE_AMOUNT")) {
            processInput(player, message, state.contains("PLACE") ? "PLACE" : "RAISE");
            return;
        }

        if (state.equals("RAISE_TARGET")) {
            if (manager.getBounty(message) <= 0) {
                player.sendMessage(Component.text("Player has no bounty.", NamedTextColor.RED));
                clearState(player);
                return;
            }
            tempTarget.put(player.getUniqueId(), message);
            inputState.put(player.getUniqueId(), "RAISE_AMOUNT");
            player.sendMessage(Component.text("How much to add?", NamedTextColor.YELLOW));
        }
    }

    private void processInput(Player player, String message, String type) {
        try {
            String clean = message.replace(",", "").replaceAll("[^0-9.kK]", "").toLowerCase();
            double amount = Double.parseDouble(clean.replace("k", ""));
            if (clean.contains("k")) amount *= 1000;

            if (amount <= 0 || amount > 1_000_000_000) {
                player.sendMessage(Component.text("Invalid amount.", NamedTextColor.RED));
                clearState(player);
                return;
            }
            processTransaction(player, tempTarget.get(player.getUniqueId()), amount);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid number.", NamedTextColor.RED));
        }
        clearState(player);
    }

    private void processTransaction(Player player, String target, double amount) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            if (!hasEnoughShards(player, amount)) {
                player.sendMessage(Component.text("Insufficient Shards!", NamedTextColor.RED));
                return;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco take shards " + player.getName() + " " + amount);
            manager.addBounty(target, amount);
            Bukkit.broadcast(Component.text(player.getName() + " bountied " + target + " for " + amount + " Shards!", NamedTextColor.RED));
            BountyMenu.open(player, 0);
        });
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

    private void clearState(Player player) {
        inputState.remove(player.getUniqueId());
        tempTarget.remove(player.getUniqueId());
    }
}