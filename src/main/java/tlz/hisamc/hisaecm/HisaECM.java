package tlz.hisamc.hisaecm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tlz.hisamc.hisaecm.commands.HisaCommand;
import tlz.hisamc.hisaecm.listeners.*;
import tlz.hisamc.hisaecm.tasks.AutoClearTask;
import tlz.hisamc.hisaecm.util.BountyManager;
import tlz.hisamc.hisaecm.util.ChunkLoaderManager;
import tlz.hisamc.hisaecm.util.ConfigUpdater;
import tlz.hisamc.hisaecm.util.DatabaseManager;
import tlz.hisamc.hisaecm.util.EnchantKeys; // Make sure to import this!

public final class HisaECM extends JavaPlugin {

    private static HisaECM instance;
    private CropBoosterListener boosterListener;
    private BountyManager bountyManager;
    private AutoClearTask autoClearTask;
    private ChunkLoaderManager loaderManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Setup Config & Keys
        saveDefaultConfig();
        ConfigUpdater.update(this);
        EnchantKeys.init(this); // <--- IMPORTANT: Initialize Keys Here

        // 2. Initialize Database
        this.databaseManager = new DatabaseManager(this);

        // 3. Initialize Managers
        this.bountyManager = new BountyManager(this, databaseManager);
        this.loaderManager = new ChunkLoaderManager(this, databaseManager);
        this.boosterListener = new CropBoosterListener(this, databaseManager);

        // 4. Register Commands
        getCommand("hisaecm").setExecutor(new HisaCommand(this));
        getCommand("bounty").setExecutor(new HisaCommand(this)); 

        // 5. Register Listeners
        var pm = getServer().getPluginManager();
        
        pm.registerEvents(new MainMenuListener(this), this);
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new ShopListener(this), this);
        
        pm.registerEvents(new BoosterGuiListener(this, boosterListener), this);
        pm.registerEvents(boosterListener, this); 
        
        pm.registerEvents(new BountyListener(this, bountyManager), this);
        pm.registerEvents(new BountyGuiListener(this, bountyManager), this);
        
        pm.registerEvents(new ChunkLoaderListener(this, loaderManager), this); 
        pm.registerEvents(new LoaderGuiListener(this, loaderManager), this);
        
        // Mining Listeners (These use DropsHandler internally)
        pm.registerEvents(new MiningListener(this), this);
        pm.registerEvents(new ExplosiveListener(this), this);
        pm.registerEvents(new VeinMiningListener(this), this);
        pm.registerEvents(new HasteListener(this), this);
        pm.registerEvents(new HarvesterHoeListener(this), this);

        // 6. Auto Clear Task
        if (getConfig().getBoolean("auto-clear.enabled", true)) {
            this.autoClearTask = new AutoClearTask(this);
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
                autoClearTask.run(); 
            }, 20L, 20L);
        }
        
        getLogger().info("Hisa-ECM enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.close();
    }

    public static HisaECM getInstance() { return instance; }
    public BountyManager getBountyManager() { return bountyManager; }
    public ChunkLoaderManager getLoaderManager() { return loaderManager; } 
}