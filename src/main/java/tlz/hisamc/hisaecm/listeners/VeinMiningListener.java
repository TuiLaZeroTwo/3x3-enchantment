package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.*;

public class VeinMiningListener implements Listener {

    private final HisaECM plugin;
    // Set to prevent infinite loops (BlockBreakEvent triggering itself)
    private final Set<UUID> isVeinMining = new HashSet<>();

    public VeinMiningListener(HisaECM plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        
        // Prevent recursion
        if (isVeinMining.contains(player.getUniqueId())) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !tool.hasItemMeta()) return;

        ItemMeta meta = tool.getItemMeta();
        if (!meta.getPersistentDataContainer().has(EnchantMenuListener.KEY_VEIN, PersistentDataType.INTEGER)) return;

        Block startBlock = event.getBlock();
        Material startType = startBlock.getType();

        // 1. Check Logic: Pickaxe + Ore OR Axe + Log
        boolean isOre = isOre(startType);
        boolean isLog = isLog(startType);
        boolean validTool = false;

        if (isOre && Tag.ITEMS_PICKAXES.isTagged(tool.getType())) validTool = true;
        if (isLog && Tag.ITEMS_AXES.isTagged(tool.getType())) validTool = true;

        if (!validTool) return;

        // 2. Perform Vein Mine
        isVeinMining.add(player.getUniqueId());
        try {
            veinMine(startBlock, player, tool, startType);
        } finally {
            isVeinMining.remove(player.getUniqueId());
        }
    }

    private void veinMine(Block start, Player player, ItemStack tool, Material targetType) {
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();
        
        queue.add(start);
        visited.add(start);

        int maxBlocks = 128; // Limit to prevent crash
        int broken = 0;

        while (!queue.isEmpty() && broken < maxBlocks) {
            Block current = queue.poll();

            // Skip the very first block (it is broken by the event naturally)
            if (!current.equals(start)) {
                if (!current.getType().equals(targetType) && !areOresRelated(targetType, current.getType())) continue;
                
                // Break naturally drops items (works with Fortune/Silk Touch automatically)
                current.breakNaturally(tool);
                broken++;
                
                // Optional: Damage Tool (1 durability per block)
                // tool.setDurability((short) (tool.getDurability() + 1));
            }

            // Search Neighbors
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        
                        Block neighbor = current.getRelative(x, y, z);
                        if (visited.contains(neighbor)) continue;

                        // Check if match
                        if (neighbor.getType() == targetType || areOresRelated(targetType, neighbor.getType())) {
                            queue.add(neighbor);
                            visited.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    private boolean isLog(Material mat) {
        return Tag.LOGS.isTagged(mat);
    }

    private boolean isOre(Material mat) {
        String name = mat.name();
        return name.endsWith("_ORE") || mat == Material.ANCIENT_DEBRIS || mat == Material.AMETHYST_BLOCK || mat == Material.NETHER_QUARTZ_ORE;
    }

    private boolean areOresRelated(Material m1, Material m2) {
        if (m1 == m2) return true;
        String n1 = m1.name().replace("DEEPSLATE_", "");
        String n2 = m2.name().replace("DEEPSLATE_", "");
        return n1.equals(n2) && isOre(m1) && isOre(m2);
    }
}