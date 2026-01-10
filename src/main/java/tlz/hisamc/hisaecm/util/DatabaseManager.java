package tlz.hisamc.hisaecm.util;

import tlz.hisamc.hisaecm.HisaECM;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final HisaECM plugin;
    private Connection connection;

    public DatabaseManager(HisaECM plugin) {
        this.plugin = plugin;
        connect();
    }

    private void connect() {
        try {
            String type = plugin.getConfig().getString("database.type", "sqlite");

            // --- MYSQL CONNECTION ---
            if (type.equalsIgnoreCase("mysql")) {
                String host = plugin.getConfig().getString("database.host", "localhost");
                int port = plugin.getConfig().getInt("database.port", 3306);
                String dbName = plugin.getConfig().getString("database.database", "minecraft");
                String user = plugin.getConfig().getString("database.username", "root");
                String pass = plugin.getConfig().getString("database.password", "");
                boolean ssl = plugin.getConfig().getBoolean("database.ssl", false);

                String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=" + ssl + "&autoReconnect=true";
                connection = DriverManager.getConnection(url, user, pass);
                
                // SILENCED LOG
                // plugin.getLogger().info("Connected to MySQL database.");
            } 
            
            // --- SQLITE CONNECTION (Default) ---
            else {
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(url);
                
                // SILENCED LOG
                // plugin.getLogger().info("Connected to local SQLite database.");
            }
            
            initTables();
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to database! " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initTables() {
        try (Statement stmt = connection.createStatement()) {
            // 1. Bounties
            stmt.execute("CREATE TABLE IF NOT EXISTS bounties (" +
                    "target VARCHAR(32) PRIMARY KEY," +
                    "amount DOUBLE NOT NULL" +
                    ");");

            // 2. Loaders
            stmt.execute("CREATE TABLE IF NOT EXISTS loaders (" +
                    "location VARCHAR(64) PRIMARY KEY," +
                    "expiry BIGINT NOT NULL" +
                    ");");

            // 3. Boosters
            stmt.execute("CREATE TABLE IF NOT EXISTS boosters (" +
                    "location VARCHAR(64) PRIMARY KEY," +
                    "expiry BIGINT NOT NULL," +
                    "multiplier DOUBLE DEFAULT 1.0" +
                    ");");

        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        try {
            // Reconnect if the previous connection was closed by a manager
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}