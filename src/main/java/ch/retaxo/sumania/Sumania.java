package ch.retaxo.sumania;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ch.retaxo.sumania.config.ConfigManager;
import ch.retaxo.sumania.api.SumaniaAPI;
import ch.retaxo.sumania.commands.CommandManager;
import ch.retaxo.sumania.events.EventManager;

public final class Sumania extends JavaPlugin {
    
    private static Sumania instance;
    private ConfigManager configManager;
    private SumaniaAPI api;
    private CommandManager commandManager;
    private EventManager eventManager;
    
    @Override
    public void onEnable() {
        // Set instance
        instance = this;
        
        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.setupConfigs();
        
        // Initialize API
        api = new SumaniaAPI(this);
        
        // Register commands
        commandManager = new CommandManager(this);
        commandManager.registerCommands();
        
        // Register events
        eventManager = new EventManager(this);
        eventManager.registerEvents();
        
        // Log startup
        Bukkit.getLogger().info(configManager.getPrefix() + "Plugin enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Save configs
        configManager.saveAllConfigs();
        
        // Log shutdown
        Bukkit.getLogger().info(configManager.getPrefix() + "Plugin disabled successfully!");
    }
    
    /**
     * Get the plugin instance
     * @return The plugin instance
     */
    public static Sumania getInstance() {
        return instance;
    }
    
    /**
     * Get the config manager
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Get the API
     * @return The API
     */
    public SumaniaAPI getAPI() {
        return api;
    }
    
    /**
     * Get the command manager
     * @return The command manager
     */
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    /**
     * Get the event manager
     * @return The event manager
     */
    public EventManager getEventManager() {
        return eventManager;
    }
}
