package tlz.hisamc.hisaecm.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.HashMap;

public class DropsHandler {

    public static void handleBreak(Player player, Block block, ItemStack tool) {
        handleDropsOnly(player, block, tool);
        block.setType(Material.AIR);
    }

    public static void handleDropsOnly(Player player, Block block, ItemStack tool) {
        Collection<ItemStack> drops = block.getDrops(tool);
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        boolean tele = meta.getPersistentDataContainer().has(EnchantKeys.TELEKINESIS, PersistentDataType.INTEGER);
        boolean smelt = meta.getPersistentDataContainer().has(EnchantKeys.AUTO_SMELT, PersistentDataType.INTEGER);

        if (drops.isEmpty()) return;

        for (ItemStack drop : drops) {
            ItemStack finalDrop = drop;

            if (smelt) {
                Material smelted = getSmeltedMaterial(drop.getType());
                if (smelted != null) finalDrop = new ItemStack(smelted, drop.getAmount());
            }

            if (tele) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(finalDrop);
                if (!overflow.isEmpty()) {
                    for (ItemStack left : overflow.values()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), left);
                    }
                }
            } else {
                block.getWorld().dropItemNaturally(block.getLocation(), finalDrop);
            }
        }
    }

    private static Material getSmeltedMaterial(Material input) {
        return switch (input) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            case RAW_COPPER -> Material.COPPER_INGOT;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case COBBLESTONE -> Material.STONE;
            case STONE -> Material.SMOOTH_STONE;
            case SAND -> Material.GLASS;
            // FIX: Use unqualified names (no "Material." prefix) inside the case labels
            case OAK_LOG, BIRCH_LOG, SPRUCE_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, 
                 MANGROVE_LOG, CHERRY_LOG -> Material.CHARCOAL;
            default -> null;
        };
    }
}