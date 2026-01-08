package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        
        // 1. Check for Harvester Hoe
        if (meta == null || !meta.getPersistentDataContainer().has(ShopListener.KEY_HOE, PersistentDataType.INTEGER)) return;

        Block centerBlock = event.getBlock();
        
        // 2. Check if it is a crop
        if (!(centerBlock.getBlockData() instanceof Ageable)) return;

        // 3. CANCEL the break event so the block stays put (This fixes the replant issue)
        event.setCancelled(true);

        // 4. Harvest 3x3 Area
        harvestArea(centerBlock, player, tool);
    }

    private void harvestArea(Block center, Player player, ItemStack tool) {
        int radius = 1; // 1 block radius = 3x3 area

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block target = center.getRelative(x, 0, z);
                
                // Only harvest if it matches the center crop type (e.g. don't harvest carrots if hitting wheat)
                if (target.getType() == center.getType()) {
                    harvestSingleBlock(target, player, tool);
                }
            }
        }
    }

    private void harvestSingleBlock(Block block, Player player, ItemStack tool) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return;

        // Only harvest if Fully Grown
        if (ageable.getAge() < ageable.getMaximumAge()) return;

        // 1. Get Drops
        Collection<ItemStack> drops = block.getDrops(tool);

        // 2. Give to Inventory (Telekinesis style)
        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(drop);
            for (ItemStack l : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), l);
            }
        }

        // 3. Reset Age (Replant)
        ageable.setAge(0);
        block.setBlockData(ageable);

        // 4. Effects
        block.getWorld().spawnParticle(Particle.COMPOSTER, block.getLocation().add(0.5, 0.5, 0.5), 3);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 0.5f, 1.2f);
    }
}