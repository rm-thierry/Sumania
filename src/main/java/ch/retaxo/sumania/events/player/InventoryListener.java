package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Listener for inventory events
 */
public class InventoryListener implements Listener {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public InventoryListener(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle inventory click event
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Handle warp menu click
        plugin.getAPI().getTeleportAPI().handleWarpMenuClick(event);
    }
}