package ch.retaxo.sumania.events;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.events.player.PlayerChatListener;
import ch.retaxo.sumania.events.player.PlayerConnectionListener;
import ch.retaxo.sumania.events.player.PlayerInteractListener;
import ch.retaxo.sumania.events.protection.BlockProtectionListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

/**
 * Manages all event listeners for the plugin
 */
public class EventManager {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public EventManager(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Register all event listeners
     */
    public void registerEvents() {
        PluginManager pm = Bukkit.getPluginManager();
        
        // Register player event listeners
        pm.registerEvents(new PlayerConnectionListener(plugin), plugin);
        pm.registerEvents(new PlayerChatListener(plugin), plugin);
        pm.registerEvents(new PlayerInteractListener(plugin), plugin);
        
        // Register protection event listeners
        pm.registerEvents(new BlockProtectionListener(plugin), plugin);
    }
}
