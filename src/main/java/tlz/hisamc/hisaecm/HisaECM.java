package tlz.hisamc.hisaecm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tlz.hisamc.hisaecm.commands.HisaCommand;
import tlz.hisamc.hisaecm.listeners.*;
import tlz.hisamc.hisaecm.tasks.AutoClearTask;
import tlz.hisamc.hisaecm.util.BountyManager;
import tlz.hisamc.hisaecm.util.ChunkLoaderManager; 

public final class HisaECM extends JavaPlugin {

    private static HisaECM instance;
    private CropBoosterListener boosterListener;
    private BountyManager bountyManager;
    private AutoClearTask autoClearTask;
    private ChunkLoaderManager loaderManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // 1. Check Dependencies
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found! Shard costs will not work.");
        }

        // 2. Initialize Managers
        this.bountyManager = new BountyManager(this);
        this.loaderManager = new ChunkLoaderManager(this);
        this.boosterListener = new CropBoosterListener(this);

        // 3. Register Commands
        getCommand("hisaecm").setExecutor(new HisaCommand(this));
        getCommand("bounty").setExecutor(new HisaCommand(this)); 

        // 4. Register Listeners
        var pm = getServer().getPluginManager();
        
        // GUI & Main Listeners
        pm.registerEvents(new MainMenuListener(this), this);
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new ShopListener(this), this);
        
        // Booster System
        pm.registerEvents(new BoosterGuiListener(this, boosterListener), this);
        pm.registerEvents(boosterListener, this);
        
        // Bounty System
        pm.registerEvents(new BountyListener(this, bountyManager), this);
        pm.registerEvents(new BountyGuiListener(this, bountyManager), this);

        // Chunk Loader System (Fixed Constructor)
        pm.registerEvents(new ChunkLoaderListener(this, loaderManager), this);
        pm.registerEvents(new LoaderGuiListener(this, loaderManager), this);

        // Enchantment Listeners
        pm.registerEvents(new MiningListener(this), this);
        pm.registerEvents(new ExplosiveListener(this), this);
        pm.registerEvents(new VeinMiningListener(this), this);
        pm.registerEvents(new HasteListener(this), this);
        pm.registerEvents(new DropsListener(this), this);
        pm.registerEvents(new HarvesterHoeListener(this), this);
        
        pm.registerEvents(new MenuListener(this), this);

        // 5. Start Auto Clear Task
        if (getConfig().getBoolean("auto-clear.enabled", true)) {
            this.autoClearTask = new AutoClearTask(this);
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
                autoClearTask.run(); 
            }, 20L, 20L);
            getLogger().info("AutoClear task started.");
        }

        getLogger().info("Hisa-ECM enabled!");
    }

    @Override
    public void onDisable() {
        if (boosterListener != null) boosterListener.saveBoosters();
        if (bountyManager != null) bountyManager.saveBounties();
    }

    public static HisaECM getInstance() { return instance; }
    public BountyManager getBountyManager() { return bountyManager; }
    public ChunkLoaderManager getLoaderManager() { return loaderManager; } 
}