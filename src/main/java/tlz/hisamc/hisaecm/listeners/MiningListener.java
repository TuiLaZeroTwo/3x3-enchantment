package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
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
import tlz.hisamc.hisaecm.util.DropsHandler; 
import tlz.hisamc.hisaecm.util.EnchantKeys;

import java.util.ArrayList;
import java.util.List;

public class MiningListener implements Listener {

    private final HisaECM plugin;

    public MiningListener(HisaECM plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block center = event.getBlock();

        // 1. Basic Checks
        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        // 2. Check Enchants
        boolean has3x3 = meta.getPersistentDataContainer().has(EnchantKeys.MINER_3X3, PersistentDataType.INTEGER);
        boolean hasTelekinesis = meta.getPersistentDataContainer().has(EnchantKeys.TELEKINESIS, PersistentDataType.INTEGER);
        boolean hasSmelt = meta.getPersistentDataContainer().has(EnchantKeys.AUTO_SMELT, PersistentDataType.INTEGER);

        // If tool has NONE of our enchants, let vanilla handle it.
        if (!has3x3 && !hasTelekinesis && !hasSmelt) return;

        // 3. Handle Center Block (Telekinesis / AutoSmelt)
        // If we have either enchant, we MUST override vanilla drops.
        if (hasTelekinesis || hasSmelt) {
            event.setDropItems(false); // Stop vanilla drop
            DropsHandler.handleDropsOnly(player, center, tool); // Give custom items
        }

        // 4. Handle 3x3 Logic (Neighbors)
        if (has3x3 && !player.isSneaking()) {
            BlockFace face = getTargetBlockFace(player);
            List<Block> neighbors = getSurroundingBlocks(center, face);

            for (Block target : neighbors) {
                if (target.getLocation().equals(center.getLocation())) continue; // Skip center
                if (target.getType() == Material.BEDROCK || target.getType() == Material.AIR || target.getType() == Material.BARRIER) continue;

                // Break Neighbor (This handles drops + air + durability)
                DropsHandler.handleBreak(player, target, tool);
                applyDurability(player, tool);
            }
        }
    }

    // --- Helpers ---

    private List<Block> getSurroundingBlocks(Block center, BlockFace face) {
        List<Block> blocks = new ArrayList<>();
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
        var res = player.rayTraceBlocks(5.0);
        if (res != null && res.getHitBlockFace() != null) return res.getHitBlockFace();
        float pitch = player.getEyeLocation().getPitch();
        if (pitch > 45) return BlockFace.DOWN;
        if (pitch < -45) return BlockFace.UP;
        return player.getFacing();
    }

    private void applyDurability(Player player, ItemStack tool) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable d) {
            int unbreaking = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DURABILITY);
            if (unbreaking == 0 || Math.random() * 100 < (100.0 / (unbreaking + 1))) {
                d.setDamage(d.getDamage() + 1);
                tool.setItemMeta(meta);
            }
        }
    }
}