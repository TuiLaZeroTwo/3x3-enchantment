package tlz.hisamc.hisaecm.util;

import org.bukkit.Bukkit;
import tlz.hisamc.hisaecm.HisaECM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BountyManager {

    private final HisaECM plugin;
    private final DatabaseManager db;

    public BountyManager(HisaECM plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public double getBounty(String playerName) {
        String query = "SELECT amount FROM bounties WHERE target = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("amount");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public Map<String, Double> getAllBounties() {
        Map<String, Double> map = new LinkedHashMap<>();
        String query = "SELECT target, amount FROM bounties ORDER BY amount DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("target"), rs.getDouble("amount"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    public void addBounty(String playerName, double amountToAdd) {
        double current = getBounty(playerName);
        double newAmount = current + amountToAdd;
        
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            String query = "INSERT OR REPLACE INTO bounties (target, amount) VALUES (?, ?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, playerName);
                ps.setDouble(2, newAmount);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void removeBounty(String playerName) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            String query = "DELETE FROM loaders WHERE target = ?"; // Ensure table name matches DB
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM bounties WHERE target = ?")) {
                ps.setString(1, playerName);
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }
}