package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
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
import java.util.Random;

public class ExplosiveListener implements Listener {

    private final HisaECM plugin;
    private final NamespacedKey keyExplosive;
    private final Random random = new Random();

    public ExplosiveListener(HisaECM plugin) {
        this.plugin = plugin;
        this.keyExplosive = new NamespacedKey(plugin, "enchant_explosive");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // 1. Validation
        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;

        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(keyExplosive, PersistentDataType.INTEGER)) return;

        // 2. Chance Check
        int chance = plugin.getConfig().getInt("enchants.explosive.chance", 20);
        if (random.nextInt(100) >= chance) return;

        // 3. Custom Diamond Explosion Logic
        Block center = event.getBlock();
        int radius = 3; // Size of the diamond
        
        // Visuals (Sound + Particles) - No Damage!
        center.getWorld().playSound(center.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
        center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, center.getLocation().toCenterLocation(), 1);

        List<Block> blocksToBreak = getDiamondShape(center, radius);

        for (Block target : blocksToBreak) {
            // Safety Checks
            if (target.getType() == Material.BEDROCK || target.getType() == Material.AIR || target.getType() == Material.BARRIER) continue;
            
            // Randomize: 60% chance to break this specific block in the diamond
            if (random.nextInt(100) > 60) continue;

            // Stop if tool breaks
            if (isToolBroken(tool)) break;

            // Break Block
            target.breakNaturally(tool);
            
            // Apply Durability (Optional: reduce chance of damage since we break many blocks)
            if (random.nextBoolean()) {
                applyDurability(player, tool);
            }
        }
    }

    /**
     * Calculates blocks in a 3D Diamond shape (Manhattan Distance).
     */
    private List<Block> getDiamondShape(Block center, int radius) {
        List<Block> blocks = new ArrayList<>();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    // Manhattan Distance Formula: |x| + |y| + |z| <= r
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= radius) {
                        blocks.add(center.getWorld().getBlockAt(cx + x, cy + y, cz + z));
                    }
                }
            }
        }
        return blocks;
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
            int unbreakingLevel = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DURABILITY);
            if (unbreakingLevel == 0 || random.nextInt(unbreakingLevel + 1) == 0) {
                damageable.setDamage(damageable.getDamage() + 1);
                tool.setItemMeta(meta);
            }
            
            if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
                tool.setAmount(0);
            }
        }
    }
}