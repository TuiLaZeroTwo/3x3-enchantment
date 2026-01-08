package tlz.hisamc.hisaecm.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class DropsHandler {

    private static final NamespacedKey KEY_SMELT = new NamespacedKey(HisaECM.getInstance(), "enchant_smelt");
    private static final NamespacedKey KEY_TELE = new NamespacedKey(HisaECM.getInstance(), "enchant_tele");

    public static void handleBreak(Player player, Block block, ItemStack tool) {
        // 1. Get Drops
        Collection<ItemStack> drops = block.getDrops(tool);
        
        // 2. Check Enchants
        ItemMeta meta = tool.getItemMeta();
        boolean autoSmelt = meta != null && meta.getPersistentDataContainer().has(KEY_SMELT, PersistentDataType.INTEGER);
        boolean telekinesis = meta != null && meta.getPersistentDataContainer().has(KEY_TELE, PersistentDataType.INTEGER);

        // 3. Set Block to Air (Manually break it)
        block.setType(Material.AIR);

        // 4. Process Drops
        for (ItemStack drop : drops) {
            if (autoSmelt) {
                ItemStack smelted = getSmeltedResult(drop);
                if (smelted != null) drop = smelted;
            }

            if (telekinesis) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                // Drop leftovers if inventory full
                for (ItemStack l : leftover.values()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), l);
                }
            } else {
                // Drop naturally
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }

    private static ItemStack getSmeltedResult(ItemStack source) {
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
}