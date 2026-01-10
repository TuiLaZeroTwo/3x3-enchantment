package tlz.hisamc.hisaecm.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import tlz.hisamc.hisaecm.HisaECM;
import tlz.hisamc.hisaecm.gui.BoosterMenu;

public class BoosterGuiListener implements Listener {

    private final HisaECM plugin;
    private final CropBoosterListener boosterLogic;

    public BoosterGuiListener(HisaECM plugin, CropBoosterListener boosterLogic) {
        this.plugin = plugin;
        this.boosterLogic = boosterLogic;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(BoosterMenu.TITLE)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        ItemStack info = event.getInventory().getItem(4);
        if (info == null || !info.hasItemMeta()) return;
        ItemMeta im = info.getItemMeta();

        String worldName = im.getPersistentDataContainer().get(BoosterMenu.KEY_WORLD, PersistentDataType.STRING);
        Integer x = im.getPersistentDataContainer().get(BoosterMenu.KEY_LOC_X, PersistentDataType.INTEGER);
        Integer y = im.getPersistentDataContainer().get(BoosterMenu.KEY_LOC_Y, PersistentDataType.INTEGER);
        Integer z = im.getPersistentDataContainer().get(BoosterMenu.KEY_LOC_Z, PersistentDataType.INTEGER);

        if (worldName == null || x == null) return;
        Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);

        if (slot == 11) addFuel(player, loc, new ItemStack(Material.DIAMOND, 4), 3600000L);
        else if (slot == 15) addFuel(player, loc, new ItemStack(Material.EMERALD_BLOCK, 32), 86400000L);
        
        else if (slot == 13) {
            player.closeInventory();
            player.sendMessage(Component.text("Visualizing 20x20 crop area...", NamedTextColor.GREEN));
            showRadius(player, loc);
        }

        else if (slot == 22) {
            player.closeInventory();
            boosterLogic.removeBooster(loc);
            player.sendMessage(Component.text("Booster removed.", NamedTextColor.YELLOW));
        }
    }

    private void addFuel(Player player, Location loc, ItemStack cost, long time) {
        if (player.getInventory().containsAtLeast(cost, cost.getAmount())) {
            player.getInventory().removeItem(cost);
            boosterLogic.addTime(loc, time);
            player.sendMessage(Component.text("Fuel added!", NamedTextColor.GREEN));
            long expiry = boosterLogic.getBoosters().getOrDefault(loc, System.currentTimeMillis());
            BoosterMenu.open(player, loc, expiry);
        } else {
            player.sendMessage(Component.text("Not enough materials!", NamedTextColor.RED));
        }
    }

    private void showRadius(Player player, Location center) {
        // Radius 10 = Center +/- 10 blocks.
        // Total width = 10 (left) + 1 (center) + 10 (right) = 21 blocks.
        
        double minX = center.getBlockX() - 10;
        double minZ = center.getBlockZ() - 10;
        double y = center.getBlockY(); 

        Location origin = new Location(center.getWorld(), minX, y, minZ);

        Bukkit.getRegionScheduler().execute(plugin, origin, () -> {
            BlockDisplay display = (BlockDisplay) origin.getWorld().spawnEntity(origin, EntityType.BLOCK_DISPLAY);
            display.setBlock(Material.LIME_STAINED_GLASS.createBlockData());
            
            // SCALE: 21x21 (Covers ~20x20 region nicely)
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),         
                new org.joml.Quaternionf(),    
                new Vector3f(21f, 0.1f, 21f),  
                new org.joml.Quaternionf()     
            ));

            display.setGlowing(true);
            display.setGlowColorOverride(Color.LIME); 
            
            Bukkit.getRegionScheduler().runDelayed(plugin, origin, (task) -> {
                display.remove();
            }, 100L);
        });
    }
}