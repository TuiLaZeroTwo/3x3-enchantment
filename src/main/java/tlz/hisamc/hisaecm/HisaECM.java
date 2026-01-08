package tlz.hisamc.hisaecm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tlz.hisamc.hisaecm.commands.HisaCommand;
import tlz.hisamc.hisaecm.listeners.*;

public final class HisaECM extends JavaPlugin {

    private static HisaECM instance;
    private CropBoosterListener boosterListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found! Shard costs will not work.");
        }

        getCommand("hisaecm").setExecutor(new HisaCommand(this));

        // Initialize Listeners
        boosterListener = new CropBoosterListener(this); // Now handles saving/loading
        
        var pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new ShopListener(this), this);
        pm.registerEvents(boosterListener, this);
        
        pm.registerEvents(new MiningListener(this), this);
        pm.registerEvents(new ExplosiveListener(this), this);
        pm.registerEvents(new VeinMiningListener(this), this);
        
        pm.registerEvents(new HasteListener(this), this);
        pm.registerEvents(new DropsListener(this), this);

        getLogger().info("Hisa-ECM enabled! Boosters set to " + getConfig().getInt("shop.crop-booster.duration-seconds") + "s.");
    }

    @Override
    public void onDisable() {
        if (boosterListener != null) {
            boosterListener.saveBoosters(); // Save data on stop
        }
    }

    public static HisaECM getInstance() { return instance; }
}