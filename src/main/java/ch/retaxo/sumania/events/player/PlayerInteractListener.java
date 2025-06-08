package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for player interact events
 */
public class PlayerInteractListener implements Listener {

    private final Sumania plugin;
    private final Map<UUID, String> lastClaimIds = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public PlayerInteractListener(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player interact event
     * @param event The event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Handle interactions
    }
    
    /**
     * Handle player move event for claim notifications
     * @param event The event
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Skip if only looking around
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location location = event.getTo();
        Claim claim = plugin.getAPI().getClaimAPI().getClaimAt(location);
        
        // Get UUID of current claim owner, or null if no claim
        String currentClaimId = claim != null ? claim.getId() : null;
        
        // Get UUID of last claim owner, or null if no claim
        String lastClaimId = lastClaimIds.get(player.getUniqueId());
        
        // If entering a new claim
        if (currentClaimId != null && !currentClaimId.equals(lastClaimId)) {
            // Get claim owner name
            UUID ownerUUID = claim.getOwnerUUID();
            String ownerName = plugin.getServer().getOfflinePlayer(ownerUUID).getName();
            
            // Send enter message
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", ownerName != null ? ownerName : "Unknown");
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.claim-enter",
                    replacements
            );
        }
        // If leaving a claim
        else if (lastClaimId != null && !lastClaimId.equals(currentClaimId)) {
            // Send leave message
            Claim lastClaim = lastClaimId != null ? plugin.getAPI().getClaimAPI().getClaimById(lastClaimId) : null;
            
            if (lastClaim != null) {
                UUID ownerUUID = lastClaim.getOwnerUUID();
                String ownerName = plugin.getServer().getOfflinePlayer(ownerUUID).getName();
                
                Map<String, String> replacements = new HashMap<>();
                replacements.put("player", ownerName != null ? ownerName : "Unknown");
                
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "claim.claim-leave",
                        replacements
                );
            }
        }
        
        // Update last claim
        lastClaimIds.put(player.getUniqueId(), currentClaimId);
    }
}
