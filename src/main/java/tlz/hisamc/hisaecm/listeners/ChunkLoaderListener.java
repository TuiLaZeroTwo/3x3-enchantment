package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
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
import tlz.hisamc.hisaecm.gui.LoaderMenu;

public class ChunkLoaderListener implements Listener {

    private final HisaECM plugin;
    private final ChunkLoaderManager manager;
    public static final NamespacedKey KEY_LOADER_BOT = new NamespacedKey("hisaecm", "loader_bot");
    public static final NamespacedKey KEY_STORED_TIME = new NamespacedKey("hisaecm", "loader_stored_time");

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

            // --- CHECK LIMIT ---
            long nearbyLoaders = spawnLoc.getWorld().getNearbyEntities(spawnLoc, 16, 16, 16).stream()
                .filter(e -> e instanceof ArmorStand && e.getPersistentDataContainer().has(KEY_LOADER_BOT, PersistentDataType.INTEGER))
                .count();

            if (nearbyLoaders >= 1) {
                player.sendMessage(Component.text("Limit reached: 1 Chunk Loader per 16 blocks.", NamedTextColor.RED));
                return;
            }

            // --- SPAWN BOT ---
            ArmorStand bot = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
            bot.setGravity(false);
            bot.setSmall(true);
            bot.setCustomNameVisible(true);
            bot.getEquipment().setHelmet(new ItemStack(Material.GOLD_BLOCK)); 
            bot.getPersistentDataContainer().set(KEY_LOADER_BOT, PersistentDataType.INTEGER, 1);
            
            // --- APPLY STORED TIME ---
            manager.addLoader(spawnLoc); // Init (0 time)
            
            long storedTime = 0;
            if (item.getItemMeta().getPersistentDataContainer().has(KEY_STORED_TIME, PersistentDataType.LONG)) {
                storedTime = item.getItemMeta().getPersistentDataContainer().get(KEY_STORED_TIME, PersistentDataType.LONG);
                manager.addTime(spawnLoc, storedTime);
            }

            // Set Name based on time
            if (storedTime > 0) {
                 long hours = storedTime / 3600000;
                 bot.customName(Component.text("§b§lChunkBot: §e" + hours + "h"));
                 player.sendMessage(Component.text("ChunkLoader placed with " + hours + " hours of fuel retained!", NamedTextColor.GREEN));
            } else {
                 bot.customName(Component.text("§c§lChunkBot: §4NO FUEL"));
                 player.sendMessage(Component.text("ChunkLoader Bot placed! Right click to configure.", NamedTextColor.GREEN));
            }

            item.setAmount(item.getAmount() - 1);
        }
    }

    @EventHandler
    public void onInteractBot(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) return;
        ArmorStand as = (ArmorStand) event.getRightClicked();
        if (!as.getPersistentDataContainer().has(KEY_LOADER_BOT, PersistentDataType.INTEGER)) return;

        event.setCancelled(true);
        Long expiry = manager.getExpiry(as.getLocation());
        LoaderMenu.open(event.getPlayer(), as.getLocation(), expiry != null ? expiry : System.currentTimeMillis());
    }

    @EventHandler
    public void onBreakBot(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand)) return;
        if (!event.getEntity().getPersistentDataContainer().has(KEY_LOADER_BOT, PersistentDataType.INTEGER)) return;

        if (event.getDamager() instanceof Player player) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Right-click to open menu and pickup!", NamedTextColor.YELLOW));
        }
    }
}