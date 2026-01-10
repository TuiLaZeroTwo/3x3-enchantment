package tlz.hisamc.hisaecm.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class EnchantKeys {
    public static NamespacedKey MINER_3X3;
    public static NamespacedKey EXPLOSIVE;
    public static NamespacedKey VEIN_MINER;
    public static NamespacedKey HASTE;
    public static NamespacedKey AUTO_SMELT;
    public static NamespacedKey TELEKINESIS;

    public static void init(JavaPlugin plugin) {
        // These MUST match the MenuListener keys exactly
        MINER_3X3 = new NamespacedKey(plugin, "enchant_3x3");
        EXPLOSIVE = new NamespacedKey(plugin, "enchant_explosive");
        VEIN_MINER = new NamespacedKey(plugin, "enchant_vein");
        HASTE = new NamespacedKey(plugin, "enchant_haste");
        AUTO_SMELT = new NamespacedKey(plugin, "enchant_smelt");
        TELEKINESIS = new NamespacedKey(plugin, "enchant_tele");
    }
}