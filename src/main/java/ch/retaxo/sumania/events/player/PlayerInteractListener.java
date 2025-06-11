package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Claim;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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
     * Handle player interact event for claim marker interactions
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Skip if off-hand
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        
        // Handle right-click on blocks
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock != null) {
            // Check if it's a claim marker block
            Claim claim = plugin.getAPI().getClaimAPI().getClaimByMarker(clickedBlock.getLocation());
            
            if (claim != null) {
                // Check if player is the owner or trusted
                if (claim.getOwnerUUID().equals(player.getUniqueId()) || claim.isTrusted(player.getUniqueId())) {
                    // Open claim management menu
                    openClaimManagementMenu(player, claim);
                    event.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * Handle block place event for claim marker placement
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack item = event.getItemInHand();
        
        // Check if the item is a claim marker
        if (plugin.getAPI().getClaimAPI().isClaimMarker(item)) {
            // Try to create a claim with this marker
            Claim claim = plugin.getAPI().getClaimAPI().createClaim(player, block);
            
            if (claim == null) {
                // Failed to create claim, cancel the block placement
                event.setCancelled(true);
            }
        }
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
    
    /**
     * Open the claim management menu for a player
     * @param player The player to open the menu for
     * @param claim The claim to manage
     */
    private void openClaimManagementMenu(Player player, Claim claim) {
        // Create and open the claim management menu
        ClaimManagementMenu menu = new ClaimManagementMenu(plugin, claim);
        menu.open(player);
    }
}