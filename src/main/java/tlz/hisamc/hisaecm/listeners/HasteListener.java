package tlz.hisamc.hisaecm.listeners;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import tlz.hisamc.hisaecm.HisaECM;

public class HasteListener implements Listener {

    private final NamespacedKey keyHaste;

    public HasteListener(HisaECM plugin) {
        this.keyHaste = new NamespacedKey(plugin, "enchant_haste");
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        
        checkAndApplyHaste(player, newItem);
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        checkAndApplyHaste(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand());
    }

    private void checkAndApplyHaste(Player player, ItemStack item) {
        player.removePotionEffect(PotionEffectType.FAST_DIGGING);

        if (item == null || item.getType() == Material.AIR || !Tag.ITEMS_PICKAXES.isTagged(item.getType())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(keyHaste, PersistentDataType.INTEGER)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, PotionEffect.INFINITE_DURATION, 1, false, false));
        }
    }
}