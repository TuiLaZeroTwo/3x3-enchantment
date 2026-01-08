package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Bukkit;
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
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.*;

public class VeinMiningListener implements Listener {

    private final NamespacedKey keyVein;
    private final NamespacedKey keySmelt;
    private final NamespacedKey keyTele;
    private static final int MAX_BLOCKS = 64;

    public VeinMiningListener(HisaECM plugin) {
        this.keyVein = new NamespacedKey(plugin, "enchant_vein");
        this.keySmelt = new NamespacedKey(plugin, "enchant_smelt");
        this.keyTele = new NamespacedKey(plugin, "enchant_tele");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;

        ItemMeta meta = tool.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        // Must have Vein Miner
        if (!container.has(keyVein, PersistentDataType.INTEGER)) return;

        Block startBlock = event.getBlock();
        if (!startBlock.getType().name().contains("ORE") && !startBlock.getType().name().contains("LOG")) return;

        // check other enchants once
        boolean autoSmelt = container.has(keySmelt, PersistentDataType.INTEGER);
        boolean telekinesis = container.has(keyTele, PersistentDataType.INTEGER);

        mineVein(startBlock, startBlock.getType(), tool, player, new HashSet<>(), autoSmelt, telekinesis);
    }

    private void mineVein(Block current, Material targetType, ItemStack tool, Player player, Set<Block> visited, boolean autoSmelt, boolean telekinesis) {
        if (visited.size() >= MAX_BLOCKS) return;
        if (isToolBroken(tool)) return;

        visited.add(current);

        BlockFace[] faces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        
        for (BlockFace face : faces) {
            Block relative = current.getRelative(face);
            
            if (relative.getType() == targetType && !visited.contains(relative)) {
                
                // --- CUSTOM DROP LOGIC START ---
                // 1. Get drops manually
                Collection<ItemStack> drops = relative.getDrops(tool);
                
                // 2. Clear block
                relative.setType(Material.AIR);
                
                // 3. Process drops
                for (ItemStack drop : drops) {
                    if (autoSmelt) {
                        ItemStack smelted = getSmeltedResult(drop);
                        if (smelted != null) drop = smelted;
                    }

                    if (telekinesis) {
                        Map<Integer, ItemStack> left = player.getInventory().addItem(drop);
                        // Drop leftovers on ground if full
                        for (ItemStack l : left.values()) {
                            relative.getWorld().dropItemNaturally(relative.getLocation(), l);
                        }
                    } else {
                        // Drop naturally
                        relative.getWorld().dropItemNaturally(relative.getLocation(), drop);
                    }
                }
                // --- CUSTOM DROP LOGIC END ---

                applyDurability(player, tool);
                mineVein(relative, targetType, tool, player, visited, autoSmelt, telekinesis);
            }
        }
    }

    private ItemStack getSmeltedResult(ItemStack source) {
        Iterator<Recipe> iter = Bukkit.recipeIterator();
        while (iter.hasNext()) {
            Recipe recipe = iter.next();
            if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                if (furnaceRecipe.getInput().getType() == source.getType()) {
                    ItemStack result = furnaceRecipe.getResult().clone();
                    result.setAmount(source.getAmount());
                    return result;
                }
            }
        }
        return null;
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