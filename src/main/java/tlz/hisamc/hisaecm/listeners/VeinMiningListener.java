package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.HashSet;
import java.util.Set;

public class VeinMiningListener implements Listener {

    private final NamespacedKey keyVein;
    private static final int MAX_BLOCKS = 64; // Prevent crashing

    public VeinMiningListener(HisaECM plugin) {
        this.keyVein = new NamespacedKey(plugin, "enchant_vein");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) return; // Shift disables it

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;

        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(keyVein, PersistentDataType.INTEGER)) return;

        Block startBlock = event.getBlock();
        // Only works on ORES
        if (!startBlock.getType().name().contains("ORE")) return;

        // Start Recursion
        mineVein(startBlock, startBlock.getType(), tool, player, new HashSet<>());
    }

    private void mineVein(Block current, Material targetType, ItemStack tool, Player player, Set<Block> visited) {
        if (visited.size() >= MAX_BLOCKS) return;
        if (isToolBroken(tool)) return;

        visited.add(current);

        // Scan neighbors
        BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block relative = current.getRelative(face);
            
            if (relative.getType() == targetType && !visited.contains(relative)) {
                relative.breakNaturally(tool); // Drops item
                applyDurability(player, tool); // Take durability
                
                mineVein(relative, targetType, tool, player, visited); // Recurse
            }
        }
    }

    private boolean isToolBroken(ItemStack tool) {
        if (tool.getType().getMaxDurability() <= 0) return false;
        if (tool.getItemMeta() instanceof Damageable damageable) {
            return damageable.getDamage() >= tool.getType().getMaxDurability();
        }
        return false;
    }

    private void applyDurability(Player player, ItemStack tool) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int unbreaking = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DURABILITY);
            if (unbreaking == 0 || Math.random() * 100 < (100.0 / (unbreaking + 1))) {
                damageable.setDamage(damageable.getDamage() + 1);
                tool.setItemMeta(meta);
            }
        }
    }
}