package tlz.hisamc.hisaecm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tlz.hisamc.hisaecm.commands.HisaCommand;
import tlz.hisamc.hisaecm.listeners.ExplosiveListener;
import tlz.hisamc.hisaecm.listeners.HasteListener;
import tlz.hisamc.hisaecm.listeners.MenuListener;
import tlz.hisamc.hisaecm.listeners.MiningListener;

public final class HisaECM extends JavaPlugin {

    private static HisaECM instance;

    @Override
    public void onEnable() {
        instance = this;

        // Save Default Config
        saveDefaultConfig();

        // Check for PAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found! Shard costs will not work.");
        }

        // Register Command
        getCommand("hisaecm").setExecutor(new HisaCommand(this));

        // Register Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new MiningListener(this), this);
        pm.registerEvents(new ExplosiveListener(this), this);
        pm.registerEvents(new HasteListener(this), this);

        getLogger().info("Hisa-ECM enabled with new Enchants!");
    }

    public static HisaECM getInstance() { return instance; }
}