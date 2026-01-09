package tlz.hisamc.hisaecm.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DropsHandler {

    private static final NamespacedKey KEY_SMELT = new NamespacedKey(HisaECM.getInstance(), "enchant_smelt");
    private static final NamespacedKey KEY_TELE = new NamespacedKey(HisaECM.getInstance(), "enchant_tele");

    // FIX: Cache recipes to prevent massive lag during VeinMining
    private static final Map<Material, ItemStack> recipeCache = new HashMap<>();

    public static void handleBreak(Player player, Block block, ItemStack tool) {
        // 1. Get Drops
        Collection<ItemStack> drops = block.getDrops(tool);
        int xpToDrop = 0; // You can calculate XP here if you want accurate vanilla XP (complex)
        
        // 2. Check Enchants
        ItemMeta meta = tool.getItemMeta();
        boolean autoSmelt = meta != null && meta.getPersistentDataContainer().has(KEY_SMELT, PersistentDataType.INTEGER);
        boolean telekinesis = meta != null && meta.getPersistentDataContainer().has(KEY_TELE, PersistentDataType.INTEGER);

        // 3. Set Block to Air
        block.setType(Material.AIR);

        // 4. Process Drops
        for (ItemStack drop : drops) {
            if (autoSmelt) {
                ItemStack smelted = getCachedSmeltResult(drop.getType());
                if (smelted != null) {
                    drop = smelted.clone();
                    drop.setAmount(drop.getAmount()); // Preserve amount
                }
            }

            if (telekinesis) {
                // Give to Inventory
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
                
                // Drop Leftovers (Item Void Fix)
                for (ItemStack l : leftover.values()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), l);
                }
            } else {
                // Drop Naturally
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
        
        // Optional: Telekinesis XP (Give 1 XP per block as simple logic, or implement complex logic)
        if (telekinesis && xpToDrop > 0) {
            player.giveExp(xpToDrop);
        } else if (xpToDrop > 0) {
            block.getWorld().spawn(block.getLocation(), ExperienceOrb.class).setExperience(xpToDrop);
        }
    }

    // FIX: Fast Lookup Cached Smelting
    private static ItemStack getCachedSmeltResult(Material source) {
        if (recipeCache.containsKey(source)) {
            return recipeCache.get(source);
        }

        Iterator<Recipe> iter = Bukkit.recipeIterator();
        while (iter.hasNext()) {
            Recipe recipe = iter.next();
            if (recipe instanceof FurnaceRecipe furnaceRecipe) {
                if (furnaceRecipe.getInput().getType() == source) {
                    ItemStack result = furnaceRecipe.getResult();
                    recipeCache.put(source, result);
                    return result;
                }
            }
        }
        
        // Cache null result (Air) so we don't search again for non-smeltable items
        recipeCache.put(source, null);
        return null;
    }
}