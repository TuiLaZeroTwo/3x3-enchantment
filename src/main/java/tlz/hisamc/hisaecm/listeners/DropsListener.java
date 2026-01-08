package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

import java.util.Iterator;
import java.util.Map;

public class DropsListener implements Listener {

    private final NamespacedKey keySmelt;
    private final NamespacedKey keyTele;

    public DropsListener(HisaECM plugin) {
        this.keySmelt = new NamespacedKey(plugin, "enchant_smelt");
        this.keyTele = new NamespacedKey(plugin, "enchant_tele");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) return;

        boolean hasSmelt = meta.getPersistentDataContainer().has(keySmelt, PersistentDataType.INTEGER);
        boolean hasTele = meta.getPersistentDataContainer().has(keyTele, PersistentDataType.INTEGER);

        if (!hasSmelt && !hasTele) return;

        for (Item itemEntity : event.getItems()) {
            ItemStack stack = itemEntity.getItemStack();

            // Auto Smelt
            if (hasSmelt) {
                ItemStack smelted = getSmeltedResult(stack);
                if (smelted != null) {
                    stack = smelted;
                    itemEntity.setItemStack(stack);
                }
            }

            // Telekinesis
            if (hasTele) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
                if (leftover.isEmpty()) {
                    itemEntity.remove(); // All went to inv
                } else {
                    itemEntity.setItemStack(leftover.get(0)); // Drop leftover
                }
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
}