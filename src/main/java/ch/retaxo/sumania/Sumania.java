package ch.retaxo.sumania;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ch.retaxo.sumania.config.ConfigManager;
import ch.retaxo.sumania.api.SumaniaAPI;
import ch.retaxo.sumania.api.SumaniaPlaceholderExpansion;
import ch.retaxo.sumania.commands.CommandManager;
import ch.retaxo.sumania.events.EventManager;

public final class Sumania extends JavaPlugin implements Listener {
    
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
        
        // Register this class as listener for MOTD
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getLogger().info(configManager.getPrefix() + "Found PlaceholderAPI! Registering placeholders...");
            new SumaniaPlaceholderExpansion(this).register();
        }
        
        // Ensure default SMP config values are set
        ensureDefaultSMPConfig();
        
        // Log startup
        Bukkit.getLogger().info(configManager.getPrefix() + "Plugin enabled successfully!");
    }
    
    /**
     * Ensure default SMP config values are set
     */
    private void ensureDefaultSMPConfig() {
        // Ensure SMP config has default values if missing
        if (!configManager.getConfig("config.yml").contains("smp")) {
            configManager.getConfig("config.yml").set("smp.enabled", true);
            configManager.getConfig("config.yml").set("smp.world-name", getServer().getWorlds().get(0).getName());
            configManager.getConfig("config.yml").set("smp.border-size", 10000);
            configManager.getConfig("config.yml").set("smp.min-teleport-range", 1000);
            configManager.getConfig("config.yml").set("smp.max-teleport-range", 8000);
            configManager.getConfig("config.yml").set("smp.rtp-cooldown", 300);
            configManager.getConfig("config.yml").set("smp.default-game-mode", "SURVIVAL");
            configManager.saveConfig("config.yml");
        }
    }
    
    @Override
    public void onDisable() {
        // Save configs
        configManager.saveAllConfigs();
        
        // Cleanup auctions
        if (api != null && api.getAuctionAPI() != null) {
            api.getAuctionAPI().shutdown();
        }
        
        // Close database connection
        configManager.closeDbConnection();
        
        // Log shutdown
        Bukkit.getLogger().info(configManager.getPrefix() + "Plugin disabled successfully!");
    }
    
    /**
     * Handle server list ping event to customize MOTD
     * @param event The server list ping event
     */
    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        boolean motdEnabled = configManager.getConfig("config.yml").getBoolean("server.motd-enabled", true);
        
        if (motdEnabled) {
            String motd = configManager.getConfig("config.yml").getString("server.motd", "§6Willkommen auf dem §lSumania SMP§r §6Server!");
            String secondLine = configManager.getConfig("config.yml").getString("server.motd-second-line", "§eDein deutsches SMP-Erlebnis!");
            
            // Set MOTD with both lines
            event.setMotd(motd + "\n" + secondLine);
            
            // Set max players from config
            int maxPlayers = configManager.getConfig("config.yml").getInt("server.max-players", 100);
            event.setMaxPlayers(maxPlayers);
        }
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
