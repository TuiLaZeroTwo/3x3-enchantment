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

        boosterListener = new CropBoosterListener(this);
        
        var pm = getServer().getPluginManager();
        pm.registerEvents(new MainMenuListener(this), this); // NEW
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new ShopListener(this), this);
        pm.registerEvents(new BoosterGuiListener(this, boosterListener), this);
        pm.registerEvents(boosterListener, this);
        
        pm.registerEvents(new MiningListener(this), this);
        pm.registerEvents(new ExplosiveListener(this), this);
        pm.registerEvents(new VeinMiningListener(this), this);
        pm.registerEvents(new HasteListener(this), this);
        pm.registerEvents(new DropsListener(this), this);

        getLogger().info("Hisa-ECM enabled with Main Menu GUI!");
    }

    @Override
    public void onDisable() {
        if (boosterListener != null) boosterListener.saveBoosters();
    }

    public static HisaECM getInstance() { return instance; }
}