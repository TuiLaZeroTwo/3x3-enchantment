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
import tlz.hisamc.hisaecm.util.DropsHandler; // Import Handler

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

        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(key3x3, PersistentDataType.INTEGER)) return;
        if (player.isSneaking()) return;

        Block center = event.getBlock();
        BlockFace face = getTargetBlockFace(player);
        List<Block> blocks = getSurroundingBlocks(center, face);

        for (Block target : blocks) {
            if (target.getLocation().equals(center.getLocation())) continue;
            if (target.getType() == Material.BEDROCK || target.getType() == Material.AIR) continue;

            // USE DROPS HANDLER HERE
            DropsHandler.handleBreak(player, target, tool);
            
            applyDurability(player, tool);
        }
    }

    // (Keep helper methods: getSurroundingBlocks, getTargetBlockFace, applyDurability)
    // For brevity, assume standard 3x3 logic helpers are here as before.
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
        return res == null ? BlockFace.UP : res.getHitBlockFace();
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