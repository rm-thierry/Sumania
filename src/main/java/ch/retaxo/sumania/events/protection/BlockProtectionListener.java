package ch.retaxo.sumania.events.protection;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener for block protection events
 */
public class BlockProtectionListener implements Listener {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public BlockProtectionListener(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle block break event
     * @param event The event
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        
        // Skip if claims are disabled
        if (!plugin.getConfigManager().getConfig("config.yml").getBoolean("protection.claims-enabled", true)) {
            return;
        }
        
        // Check if player can build at location
        if (!plugin.getAPI().getClaimAPI().canBuild(player, location)) {
            event.setCancelled(true);
            
            // Get claim at location
            String ownerName = plugin.getServer().getOfflinePlayer(
                    plugin.getAPI().getClaimAPI().getClaimAt(location).getOwnerUUID()
            ).getName();
            
            // Send message
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", ownerName != null ? ownerName : "Unknown");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.cannot-build",
                    replacements
            );
        }
    }
    
    /**
     * Handle block place event
     * @param event The event
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        
        // Skip if claims are disabled
        if (!plugin.getConfigManager().getConfig("config.yml").getBoolean("protection.claims-enabled", true)) {
            return;
        }
        
        // Check if player can build at location
        if (!plugin.getAPI().getClaimAPI().canBuild(player, location)) {
            event.setCancelled(true);
            
            // Get claim at location
            String ownerName = plugin.getServer().getOfflinePlayer(
                    plugin.getAPI().getClaimAPI().getClaimAt(location).getOwnerUUID()
            ).getName();
            
            // Send message
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", ownerName != null ? ownerName : "Unknown");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.cannot-build",
                    replacements
            );
        }
    }
}
