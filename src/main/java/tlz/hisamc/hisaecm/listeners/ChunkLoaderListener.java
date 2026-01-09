package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.util.ChunkLoaderManager;
import tlz.hisamc.hisaecm.gui.LoaderMenu; // FIX: Added Import

public class ChunkLoaderListener implements Listener {

    private final HisaECM plugin;
    private final ChunkLoaderManager manager;
    public static final NamespacedKey KEY_LOADER_BOT = new NamespacedKey("hisaecm", "loader_bot");

    // FIX: Updated constructor to accept the manager passed from HisaECM
    public ChunkLoaderListener(HisaECM plugin, ChunkLoaderManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getType() == Material.ARMOR_STAND && 
            item.getItemMeta().getDisplayName().contains("ChunkLoader")) {
            
            event.setCancelled(true);
            Player player = event.getPlayer();
            org.bukkit.Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);

            if (manager.isLoaderAt(spawnLoc)) {
                player.sendMessage(Component.text("There is already a loader here!", NamedTextColor.RED));
                return;
            }

            ArmorStand bot = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
            bot.setGravity(false);
            bot.setSmall(true);
            bot.setCustomNameVisible(true);
            bot.customName(Component.text("§c§lChunkBot: §4NO FUEL"));
            bot.getEquipment().setHelmet(new ItemStack(Material.GOLD_BLOCK)); 
            bot.getPersistentDataContainer().set(KEY_LOADER_BOT, PersistentDataType.INTEGER, 1);
            
            manager.addLoader(spawnLoc);
            item.setAmount(item.getAmount() - 1);
            player.sendMessage(Component.text("ChunkLoader Bot placed! Right click to configure.", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onInteractBot(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand)) return;
        if (!entity.getPersistentDataContainer().has(KEY_LOADER_BOT, PersistentDataType.INTEGER)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        
        // FIX: Use 'Long' (capital L) to allow null checks
        Long expiry = manager.getExpiry(entity.getLocation());
        
        // FIX: Now the check (expiry != null) is valid
        LoaderMenu.open(player, entity.getLocation(), expiry != null ? expiry : System.currentTimeMillis());
    }

    @EventHandler
    public void onBreakBot(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ArmorStand)) return;
        if (!entity.getPersistentDataContainer().has(KEY_LOADER_BOT, PersistentDataType.INTEGER)) return;

        if (event.getDamager() instanceof Player player) {
            // Prevent accidental breaking. Use the GUI to pickup.
            event.setCancelled(true);
            
            // Allow picking up via GUI logic (handled in LoaderGuiListener), 
            // but just in case they try to punch it:
            player.sendMessage(Component.text("Right-click to open menu and pickup!", NamedTextColor.YELLOW));
        }
    }
}