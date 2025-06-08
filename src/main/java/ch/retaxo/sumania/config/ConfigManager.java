package ch.retaxo.sumania.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;
    
    // Main config keys
    private String prefix;
    private boolean debugMode;
    private Connection dbConnection;
    private String dbType;
    private String tablePrefix;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configs = new HashMap<>();
        this.configFiles = new HashMap<>();
    }
    
    /**
     * Setup all configuration files
     */
    public void setupConfigs() {
        // Create main config
        createConfig("config.yml");
        
        // Create messages config
        createConfig("messages.yml");
        
        // Create data config
        createConfig("data.yml");
        
        // Load configuration values
        loadConfigValues();
        
        // Setup database connection
        setupDatabase();
    }
    
    /**
     * Create a configuration file
     * @param configName The name of the config file
     */
    private void createConfig(String configName) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        File file = new File(plugin.getDataFolder(), configName);
        
        if (!file.exists()) {
            plugin.saveResource(configName, false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(configName, config);
        configFiles.put(configName, file);
    }
    
    /**
     * Load values from configuration files
     */
    private void loadConfigValues() {
        FileConfiguration mainConfig = getConfig("config.yml");
        
        // Load prefix
        prefix = mainConfig.getString("prefix", "§8[§6Sumania§8] §7");
        
        // Load debug mode
        debugMode = mainConfig.getBoolean("debug-mode", false);
        
        // Load database type
        dbType = mainConfig.getString("database.type", "sqlite");
        
        // Load table prefix
        tablePrefix = mainConfig.getString("database.table-prefix", "sumania_");
    }
    
    /**
     * Setup database connection
     */
    private void setupDatabase() {
        FileConfiguration config = getConfig("config.yml");
        boolean autoCreateTables = config.getBoolean("database.auto-create-tables", true);
        
        try {
            if (dbType.equalsIgnoreCase("mysql")) {
                // MySQL connection
                String host = config.getString("database.mysql.host", "localhost");
                int port = config.getInt("database.mysql.port", 3306);
                String database = config.getString("database.mysql.database", "sumania");
                String username = config.getString("database.mysql.username", "root");
                String password = config.getString("database.mysql.password", "password");
                boolean useSSL = config.getBoolean("database.mysql.use-ssl", false);
                
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL;
                
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                    dbConnection = DriverManager.getConnection(url, username, password);
                    plugin.getLogger().info("Connected to MySQL database!");
                } catch (ClassNotFoundException e) {
                    plugin.getLogger().severe("MySQL driver not found. Falling back to SQLite...");
                    setupSQLiteDatabase();
                }
            } else {
                // SQLite connection
                setupSQLiteDatabase();
            }
            
            // Create tables if needed
            if (autoCreateTables && dbConnection != null) {
                createTables();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Setup SQLite database connection
     */
    private void setupSQLiteDatabase() throws SQLException {
        File databaseFile = new File(plugin.getDataFolder(), "sumania.db");
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        
        dbConnection = DriverManager.getConnection(url);
        plugin.getLogger().info("Connected to SQLite database!");
    }
    
    /**
     * Create database tables
     */
    private void createTables() throws SQLException {
        // Players table
        String playersTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "players (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16) NOT NULL, " +
                "balance DOUBLE NOT NULL DEFAULT 0, " +
                "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        
        // Homes table
        String homesTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "homes (" +
                "id INTEGER PRIMARY KEY " + (dbType.equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                "uuid VARCHAR(36) NOT NULL, " +
                "name VARCHAR(32) NOT NULL, " +
                "world VARCHAR(64) NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "yaw FLOAT NOT NULL, " +
                "pitch FLOAT NOT NULL, " +
                "UNIQUE (uuid, name)" +
                ")";
        
        // Warps table
        String warpsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "warps (" +
                "id INTEGER PRIMARY KEY " + (dbType.equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                "name VARCHAR(32) NOT NULL UNIQUE, " +
                "world VARCHAR(64) NOT NULL, " +
                "x DOUBLE NOT NULL, " +
                "y DOUBLE NOT NULL, " +
                "z DOUBLE NOT NULL, " +
                "yaw FLOAT NOT NULL, " +
                "pitch FLOAT NOT NULL" +
                ")";
        
        // Claims table
        String claimsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "claims (" +
                "id INTEGER PRIMARY KEY " + (dbType.equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                "uuid VARCHAR(36) NOT NULL, " +
                "world VARCHAR(64) NOT NULL, " +
                "x1 INTEGER NOT NULL, " +
                "y1 INTEGER NOT NULL, " +
                "z1 INTEGER NOT NULL, " +
                "x2 INTEGER NOT NULL, " +
                "y2 INTEGER NOT NULL, " +
                "z2 INTEGER NOT NULL" +
                ")";
        
        // Discord links table
        String discordTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "discord_links (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "discord_id VARCHAR(20) NOT NULL UNIQUE, " +
                "link_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        
        // Lottery tickets table
        String lotteryTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "lottery_tickets (" +
                "id INTEGER PRIMARY KEY " + (dbType.equalsIgnoreCase("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ", " +
                "uuid VARCHAR(36) NOT NULL, " +
                "draw_id INTEGER NOT NULL, " +
                "purchase_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE (uuid, draw_id)" +
                ")";
        
        // Rewards table
        String rewardsTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "rewards (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "daily_claim TIMESTAMP NULL, " +
                "weekly_claim TIMESTAMP NULL, " +
                "monthly_claim TIMESTAMP NULL, " +
                "streak_days INTEGER DEFAULT 0, " +
                "last_streak_update TIMESTAMP NULL" +
                ")";
        
        // Execute all queries
        try (PreparedStatement playersStmt = dbConnection.prepareStatement(playersTable);
             PreparedStatement homesStmt = dbConnection.prepareStatement(homesTable);
             PreparedStatement warpsStmt = dbConnection.prepareStatement(warpsTable);
             PreparedStatement claimsStmt = dbConnection.prepareStatement(claimsTable);
             PreparedStatement discordStmt = dbConnection.prepareStatement(discordTable);
             PreparedStatement lotteryStmt = dbConnection.prepareStatement(lotteryTable);
             PreparedStatement rewardsStmt = dbConnection.prepareStatement(rewardsTable)) {
            
            playersStmt.executeUpdate();
            homesStmt.executeUpdate();
            warpsStmt.executeUpdate();
            claimsStmt.executeUpdate();
            discordStmt.executeUpdate();
            lotteryStmt.executeUpdate();
            rewardsStmt.executeUpdate();
            
            plugin.getLogger().info("Database tables created or verified!");
        }
    }
    
    /**
     * Get a configuration file
     * @param configName The name of the config file
     * @return The configuration file
     */
    public FileConfiguration getConfig(String configName) {
        return configs.get(configName);
    }
    
    /**
     * Save a configuration file
     * @param configName The name of the config file
     */
    public void saveConfig(String configName) {
        File file = configFiles.get(configName);
        FileConfiguration config = configs.get(configName);
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config file " + configName);
            e.printStackTrace();
        }
    }
    
    /**
     * Save all configuration files
     */
    public void saveAllConfigs() {
        for (String configName : configs.keySet()) {
            saveConfig(configName);
        }
    }
    
    /**
     * Reload a configuration file
     * @param configName The name of the config file
     */
    public void reloadConfig(String configName) {
        File file = configFiles.get(configName);
        configs.put(configName, YamlConfiguration.loadConfiguration(file));
        loadConfigValues();
    }
    
    /**
     * Reload all configuration files
     */
    public void reloadAllConfigs() {
        for (String configName : configs.keySet()) {
            reloadConfig(configName);
        }
    }
    
    /**
     * Get the prefix
     * @return The prefix
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Check if debug mode is enabled
     * @return True if debug mode is enabled
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Get the database connection
     * @return The database connection
     */
    public Connection getDbConnection() {
        return dbConnection;
    }
    
    /**
     * Get the database type
     * @return The database type
     */
    public String getDbType() {
        return dbType;
    }
    
    /**
     * Get the table prefix
     * @return The table prefix
     */
    public String getTablePrefix() {
        return tablePrefix;
    }
    
    /**
     * Close the database connection
     */
    public void closeDbConnection() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
