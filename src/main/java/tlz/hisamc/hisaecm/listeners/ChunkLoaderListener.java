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
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().getDisplayName().contains("ChunkLoader")) return;

        event.setCancelled(true);
        org.bukkit.Location spawnLoc = event.getClickedBlock().getLocation().add(0.5, 1, 0.5);

        int currentCX = spawnLoc.getBlockX() >> 4;
        int currentCZ = spawnLoc.getBlockZ() >> 4;
        String worldName = spawnLoc.getWorld().getName();

        boolean alreadyHasLoader = manager.getLoaders().keySet().stream().anyMatch(loc -> 
            (loc.getBlockX() >> 4) == currentCX && (loc.getBlockZ() >> 4) == currentCZ && loc.getWorld().getName().equals(worldName));

        if (alreadyHasLoader) {
            event.getPlayer().sendMessage(Component.text("Only 1 ChunkLoader allowed per chunk!", NamedTextColor.RED));
            return;
        }

        ArmorStand bot = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        bot.setGravity(false); bot.setSmall(true); bot.setCustomNameVisible(true);
        bot.getEquipment().setHelmet(new ItemStack(Material.GOLD_BLOCK));
        bot.getPersistentDataContainer().set(KEY_LOADER_BOT, PersistentDataType.INTEGER, 1);
        
        manager.addLoader(spawnLoc);
        if (item.getItemMeta().getPersistentDataContainer().has(KEY_STORED_TIME, PersistentDataType.LONG)) {
            long stored = item.getItemMeta().getPersistentDataContainer().get(KEY_STORED_TIME, PersistentDataType.LONG);
            manager.addTime(spawnLoc, stored);
        }
        item.setAmount(item.getAmount() - 1);
        event.getPlayer().sendMessage(Component.text("ChunkLoader Bot placed!", NamedTextColor.GREEN));
    }

    @EventHandler public void onInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand as) || !as.getPersistentDataContainer().has(KEY_LOADER_BOT, PersistentDataType.INTEGER)) return;
        event.setCancelled(true);
        LoaderMenu.open(event.getPlayer(), as.getLocation(), manager.getExpiry(as.getLocation()));
    }

    @EventHandler public void onBreak(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand as && as.getPersistentDataContainer().has(KEY_LOADER_BOT, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player p) p.sendMessage(Component.text("Right-click to open menu and pickup!", NamedTextColor.YELLOW));
        }
    }
}