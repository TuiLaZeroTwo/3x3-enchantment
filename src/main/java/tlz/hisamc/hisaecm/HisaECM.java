package tlz.hisamc.hisaecm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tlz.hisamc.hisaecm.commands.HisaCommand;
import tlz.hisamc.hisaecm.listeners.*;
import tlz.hisamc.hisaecm.util.BountyManager; // NEW

public final class HisaECM extends JavaPlugin {

    private static HisaECM instance;
    private CropBoosterListener boosterListener;
    private BountyManager bountyManager; // NEW

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found! Shard costs will not work.");
        }

        // Initialize Managers
        bountyManager = new BountyManager(this);
        boosterListener = new CropBoosterListener(this);

        getCommand("hisaecm").setExecutor(new HisaCommand(this));
        getCommand("bounty").setExecutor(new HisaCommand(this)); // Alias support

        // Register Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new MainMenuListener(this), this);
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new ShopListener(this), this);
        pm.registerEvents(new BoosterGuiListener(this, boosterListener), this);
        pm.registerEvents(boosterListener, this);
        
        // NEW BOUNTY LISTENERS
        pm.registerEvents(new BountyListener(this, bountyManager), this);
        pm.registerEvents(new BountyGuiListener(this, bountyManager), this);

        pm.registerEvents(new MiningListener(this), this);
        pm.registerEvents(new ExplosiveListener(this), this);
        pm.registerEvents(new VeinMiningListener(this), this);
        pm.registerEvents(new HasteListener(this), this);
        pm.registerEvents(new DropsListener(this), this);
        pm.registerEvents(new HarvesterHoeListener(this), this);

        getLogger().info("Hisa-ECM enabled with Bounty System!");
    }

    @Override
    public void onDisable() {
        if (boosterListener != null) boosterListener.saveBoosters();
        if (bountyManager != null) bountyManager.saveBounties();
    }

    public static HisaECM getInstance() { return instance; }
    public BountyManager getBountyManager() { return bountyManager; }
}