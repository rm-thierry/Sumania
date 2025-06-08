package ch.retaxo.sumania.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs;
    private final Map<String, File> configFiles;
    
    // Main config keys
    private String prefix;
    private boolean debugMode;
    
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
}
