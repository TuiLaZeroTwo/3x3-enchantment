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

        // 3. CANCEL the natural break so we handle everything (drops + replant)
        event.setCancelled(true);

        // 4. Harvest 3x3 Area
        harvestArea(centerBlock, player, tool);
    }

    private void harvestArea(Block center, Player player, ItemStack tool) {
        int radius = 1; // 1 block radius = 3x3 area

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block target = center.getRelative(x, 0, z);
                
                // Only harvest if it matches the center crop type
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

        // 2. Give Drops to Inventory (Auto-Collect)
        // We do this BEFORE replanting so the player can use the drops they just got to replant.
        for (ItemStack drop : drops) {
            Map<Integer, ItemStack> left = player.getInventory().addItem(drop);
            for (ItemStack l : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), l);
            }
        }

        // 3. Replant Logic
        Material seedType = getSeedForCrop(block.getType());
        
        if (seedType != null && hasItem(player, seedType)) {
            // Player has seeds -> Take 1 and Replant
            removeItem(player, seedType);
            
            ageable.setAge(0);
            block.setBlockData(ageable);
            
            block.getWorld().spawnParticle(Particle.COMPOSTER, block.getLocation().add(0.5, 0.5, 0.5), 3);
        } else {
            // No seeds -> Break to Air
            block.setType(Material.AIR);
        }

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_CROP_BREAK, 0.5f, 1.2f);
    }

    private Material getSeedForCrop(Material crop) {
        switch (crop) {
            case WHEAT: return Material.WHEAT_SEEDS;
            case BEETROOTS: return Material.BEETROOT_SEEDS;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case NETHER_WART: return Material.NETHER_WART;
            default: return null;
        }
    }

    private boolean hasItem(Player player, Material mat) {
        return player.getInventory().contains(mat);
    }

    private void removeItem(Player player, Material mat) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() == mat) {
                if (contents[i].getAmount() > 1) {
                    contents[i].setAmount(contents[i].getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null); // Remove item if amount is 1
                }
                return; // Removed 1, we are done
            }
        }
    }
}