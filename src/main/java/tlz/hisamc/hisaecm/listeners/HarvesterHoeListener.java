package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.Collection;
import java.util.Map;

public class HarvesterHoeListener implements Listener {

    private final HisaECM plugin;

    public HarvesterHoeListener(HisaECM plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType() == Material.AIR) return;
        ItemMeta meta = tool.getItemMeta();
        // Check if item is Harvester Hoe
        if (meta == null || !meta.getPersistentDataContainer().has(ShopListener.KEY_HOE, PersistentDataType.INTEGER)) return;

        Block block = event.getBlock();
        // Check if block is a valid crop
        if (!(block.getBlockData() instanceof Ageable ageable)) return;

        // Only harvest Fully Grown crops
        if (ageable.getAge() < ageable.getMaximumAge()) {
            event.setCancelled(true);
            player.sendMessage(org.bukkit.ChatColor.RED + "Not fully grown yet!");
            return;
        }

        // --- HARVEST LOGIC ---
        event.setDropItems(false); // Stop natural drops

        // 1. Get Drops
        Collection<ItemStack> drops = block.getDrops(tool);

        // 2. Give to Inventory
        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(drop);
            for (ItemStack l : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), l);
            }
        }

        // 3. Replant (Reset Age)
        // We use a slight delay or just set immediately. Setting immediately is safer for Folia.
        ageable.setAge(0);
        block.setBlockData(ageable);
    }
}