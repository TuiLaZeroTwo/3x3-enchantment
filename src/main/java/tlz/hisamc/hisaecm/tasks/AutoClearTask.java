package tlz.hisamc.hisaecm.tasks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Entity;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoClearTask implements Runnable {

    private final HisaECM plugin;
    private int timeLeft;
    private final int interval;
    private final List<Integer> warnings;
    private final boolean ignoreNamed;

    public AutoClearTask(HisaECM plugin) {
        this.plugin = plugin;
        this.interval = plugin.getConfig().getInt("auto-clear.interval-seconds", 7200);
        this.warnings = plugin.getConfig().getIntegerList("auto-clear.warnings");
        this.ignoreNamed = plugin.getConfig().getBoolean("auto-clear.ignore-named-items", true);
        this.timeLeft = interval;
    }

    @Override
    public void run() {
        if (timeLeft <= 0) {
            clearItems();
            timeLeft = interval; // Reset Timer
        } else {
            // Check if we need to warn
            if (warnings.contains(timeLeft)) {
                String msg = plugin.getConfig().getString("auto-clear.messages.warning", "Clearing in %time%s")
                        .replace("%time%", String.valueOf(timeLeft))
                        .replace("&", "§");
                String prefix = plugin.getConfig().getString("auto-clear.messages.prefix", "").replace("&", "§");
                Bukkit.broadcast(Component.text(prefix + msg));
            }
            timeLeft--;
        }
    }

    private void clearItems() {
        AtomicInteger count = new AtomicInteger(0);

        for (World world : Bukkit.getWorlds()) {
            // Retrieve all Item entities in the world
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item) {
                    
                    // Filter: Ignore items with Custom Names (Player gear/Rare items)
                    if (ignoreNamed && item.getItemStack().hasItemMeta() && item.getItemStack().getItemMeta().hasDisplayName()) {
                        continue;
                    }

                    // FOLIA SAFE REMOVAL: Schedule the removal on the entity's own thread
                    entity.getScheduler().execute(plugin, () -> {
                        item.remove();
                        count.incrementAndGet();
                    }, null, 0);
                }
            }
        }

        // Announce Result (Delayed slightly to ensure counts are updated, though exact count on Folia is hard)
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (task) -> {
            String msg = plugin.getConfig().getString("auto-clear.messages.cleared", "Cleared %amount% items")
                    .replace("%amount%", String.valueOf(count.get()))
                    .replace("&", "§");
            String prefix = plugin.getConfig().getString("auto-clear.messages.prefix", "").replace("&", "§");
            Bukkit.broadcast(Component.text(prefix + msg));
        }, 20L); // 1 second delay for broadcast
    }
}