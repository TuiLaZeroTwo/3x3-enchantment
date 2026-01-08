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

import java.util.ArrayList;
import java.util.List;

public class MiningListener implements Listener {

    private final NamespacedKey key3x3;

    public MiningListener(HisaECM plugin) {
        this.key3x3 = new NamespacedKey(plugin, "enchant_3x3");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // 1. Validation
        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(key3x3, PersistentDataType.INTEGER)) return;
        if (player.isSneaking()) return;

        // 2. Calculation
        Block centerBlock = event.getBlock();
        BlockFace face = getTargetBlockFace(player);
        if (face == null) return;

        List<Block> blocksToBreak = getSurroundingBlocks(centerBlock, face);

        // 3. Execution
        for (Block target : blocksToBreak) {
            if (target.getLocation().equals(centerBlock.getLocation())) continue;
            if (isUnbreakable(target.getType())) continue;

            // Durability Check
            if (isToolBroken(tool)) break;

            target.breakNaturally(tool);
            applyDurability(player, tool);
        }
    }

    private boolean isUnbreakable(Material type) {
        return type == Material.BEDROCK || type == Material.BARRIER || type == Material.AIR;
    }

    private boolean isToolBroken(ItemStack tool) {
        if (tool.getType().getMaxDurability() <= 0) return false;
        if (tool.getItemMeta() instanceof Damageable damageable) {
            return damageable.getDamage() >= tool.getType().getMaxDurability();
        }
        return false;
    }

    private List<Block> getSurroundingBlocks(Block center, BlockFace face) {
        List<Block> blocks = new ArrayList<>();
        // Logic for X, Y, Z planes
        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) blocks.add(center.getRelative(x, 0, z));
        } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
            for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) blocks.add(center.getRelative(0, y, z));
        } else {
            for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) blocks.add(center.getRelative(x, y, 0));
        }
        return blocks;
    }

    private BlockFace getTargetBlockFace(Player player) {
        var rayTrace = player.rayTraceBlocks(5.0);
        return (rayTrace == null) ? BlockFace.UP : rayTrace.getHitBlockFace();
    }

    private void applyDurability(Player player, ItemStack tool) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int unbreakingLevel = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DURABILITY);
            // Unbreaking logic: (100 / (Level + 1))% chance to take damage
            if (unbreakingLevel == 0 || Math.random() * 100 < (100.0 / (unbreakingLevel + 1))) {
                damageable.setDamage(damageable.getDamage() + 1);
            }
            
            tool.setItemMeta(meta);
            
            if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1, 1);
                tool.setAmount(0);
            }
        }
    }
}