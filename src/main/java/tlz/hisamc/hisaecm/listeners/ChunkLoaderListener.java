package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;

public class ChunkLoaderListener implements Listener {

    private final HisaECM plugin;

    public ChunkLoaderListener(HisaECM plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBotPlace(EntityPlaceEvent event) {
        if (event.getEntityType() != EntityType.ARMOR_STAND) return;
        
        // Check if the item used to place it has the Chunk Loader key
        // Note: For newer API versions, you may need to check the item in the player's hand
        if (event.getPlayer() == null) return;
        
        var item = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(ShopListener.KEY_CHUNK, PersistentDataType.INTEGER)) {
            return;
        }

        ArmorStand bot = (ArmorStand) event.getEntity();
        bot.getPersistentDataContainer().set(ShopListener.KEY_CHUNK, PersistentDataType.INTEGER, 1);
        
        Chunk chunk = bot.getLocation().getChunk();
        // Use the plugin to set the chunk as force-loaded
        chunk.setForceLoaded(true);
        
        event.getPlayer().sendMessage(Component.text("Chunk Loader Bot activated! This chunk will now stay loaded.", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onBotDestroy(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        if (entity.getPersistentDataContainer().has(ShopListener.KEY_CHUNK, PersistentDataType.INTEGER)) {
            Chunk chunk = entity.getLocation().getChunk();
            chunk.setForceLoaded(false);
            
            entity.getWorld().dropItemNaturally(entity.getLocation(), [The Shop Item]);
        }
    }
}