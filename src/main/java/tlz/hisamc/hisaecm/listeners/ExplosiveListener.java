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
import tlz.hisamc.hisaecm.util.DropsHandler;

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

        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer().has(keyExplosive, PersistentDataType.INTEGER)) return;

        int chance = plugin.getConfig().getInt("enchants.explosive.chance", 20);
        if (random.nextInt(100) >= chance) return;

        Block center = event.getBlock();
        center.getWorld().playSound(center.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.8F);
        center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, center.getLocation().toCenterLocation(), 1);

        List<Block> diamond = getDiamondShape(center, 3);

        for (Block target : diamond) {
            if (target.getType() == Material.BEDROCK || target.getType() == Material.AIR || target.getType() == Material.BARRIER) continue;
            
            // Randomize scattering
            if (random.nextInt(100) > 60) continue; 

            // NO PROTECTION CHECKS - ALLOW DESTRUCTION
            DropsHandler.handleBreak(player, target, tool);

            if (random.nextBoolean()) applyDurability(player, tool);
        }
    }

    private List<Block> getDiamondShape(Block center, int radius) {
        List<Block> blocks = new ArrayList<>();
        int cx = center.getX(), cy = center.getY(), cz = center.getZ();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= radius) {
                        blocks.add(center.getWorld().getBlockAt(cx + x, cy + y, cz + z));
                    }
                }
            }
        }
        return blocks;
    }

    private void applyDurability(Player player, ItemStack tool) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable d) {
             d.setDamage(d.getDamage() + 1);
             tool.setItemMeta(meta);
        }
    }
}