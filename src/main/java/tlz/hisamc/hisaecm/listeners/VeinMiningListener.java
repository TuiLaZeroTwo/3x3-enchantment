package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.util.DropsHandler;
import tlz.hisamc.hisaecm.util.EnchantKeys;

import java.util.LinkedList;
import java.util.Queue;

public class VeinMiningListener implements Listener {

    private final HisaECM plugin;

    public VeinMiningListener(HisaECM plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVeinMine(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // 1. Check for Vein Miner Enchant
        if (tool.getItemMeta() == null || !tool.getItemMeta().getPersistentDataContainer().has(EnchantKeys.VEIN_MINER, PersistentDataType.INTEGER)) return;
        if (player.isSneaking()) return;

        Block firstBlock = event.getBlock();
        Material type = firstBlock.getType();

        // 2. Validate if the block is "Veinable" based on the tool held
        if (!isVeinable(tool.getType(), type)) return;

        // 3. Cancel vanilla to prevent double drops on the first block
        event.setDropItems(false);
        
        // 4. BFS Queue to find connected blocks
        Queue<Block> targets = new LinkedList<>();
        targets.add(firstBlock);
        
        int count = 0;
        int maxBlocks = 64; // Safety limit to prevent lag

        while (!targets.isEmpty() && count < maxBlocks) {
            Block current = targets.poll();
            if (current.getType() != type) continue;

            // Use DropsHandler for Telekinesis/Auto-Smelt support
            DropsHandler.handleBreak(player, current, tool);
            count++;

            // Check all 26 surrounding blocks (3x3x3 cube)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block relative = current.getRelative(x, y, z);
                        if (relative.getType() == type) targets.add(relative);
                    }
                }
            }
        }
    }

    /**
     * Determines if a block is valid for Vein Mining based on the tool type.
     */
    private boolean isVeinable(Material tool, Material block) {
        // Pickaxe -> Ores
        if (Tag.ITEMS_PICKAXES.isTagged(tool)) {
            return block.name().contains("ORE") || block == Material.ANCIENT_DEBRIS || block == Material.AMETHYST_CLUSTER;
        }
        
        // Axe -> Logs
        if (Tag.ITEMS_AXES.isTagged(tool)) {
            return Tag.LOGS.isTagged(block);
        }
        
        // Shovel -> Sand, Gravel, Dirt, Clay
        if (Tag.ITEMS_SHOVELS.isTagged(tool)) {
            return block == Material.SAND || block == Material.RED_SAND || 
                   block == Material.GRAVEL || block == Material.CLAY || 
                   block == Material.DIRT || block == Material.SUSPICIOUS_SAND || 
                   block == Material.SUSPICIOUS_GRAVEL;
        }
        
        return false;
    }
}