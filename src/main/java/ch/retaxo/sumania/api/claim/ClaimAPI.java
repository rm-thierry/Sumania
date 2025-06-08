package ch.retaxo.sumania.api.claim;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API for claim-related operations
 */
public class ClaimAPI {

    private final Sumania plugin;
    private final Map<String, Claim> claims = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public ClaimAPI(Sumania plugin) {
        this.plugin = plugin;
        loadClaims();
    }
    
    /**
     * Load all claims from the data file
     */
    private void loadClaims() {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        ConfigurationSection claimsSection = data.getConfigurationSection("claims");
        
        if (claimsSection != null) {
            for (String claimId : claimsSection.getKeys(false)) {
                ConfigurationSection claimSection = claimsSection.getConfigurationSection(claimId);
                
                if (claimSection != null) {
                    UUID ownerUUID = UUID.fromString(claimSection.getString("owner"));
                    String worldName = claimSection.getString("world");
                    int minX = claimSection.getInt("min_x");
                    int minY = claimSection.getInt("min_y");
                    int minZ = claimSection.getInt("min_z");
                    int maxX = claimSection.getInt("max_x");
                    int maxY = claimSection.getInt("max_y");
                    int maxZ = claimSection.getInt("max_z");
                    
                    List<UUID> trustedPlayers = new ArrayList<>();
                    List<String> trustedPlayersStr = claimSection.getStringList("trusted_players");
                    
                    for (String uuidStr : trustedPlayersStr) {
                        trustedPlayers.add(UUID.fromString(uuidStr));
                    }
                    
                    Claim claim = new Claim(
                            claimId,
                            ownerUUID,
                            worldName,
                            minX, minY, minZ,
                            maxX, maxY, maxZ,
                            trustedPlayers
                    );
                    
                    claims.put(claimId, claim);
                }
            }
        }
    }
    
    /**
     * Save all claims to the data file
     */
    private void saveClaims() {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        
        // Clear existing claims
        data.set("claims", null);
        
        // Save each claim
        for (Claim claim : claims.values()) {
            String claimId = claim.getId();
            String path = "claims." + claimId;
            
            data.set(path + ".owner", claim.getOwnerUUID().toString());
            data.set(path + ".world", claim.getWorldName());
            data.set(path + ".min_x", claim.getMinX());
            data.set(path + ".min_y", claim.getMinY());
            data.set(path + ".min_z", claim.getMinZ());
            data.set(path + ".max_x", claim.getMaxX());
            data.set(path + ".max_y", claim.getMaxY());
            data.set(path + ".max_z", claim.getMaxZ());
            
            List<String> trustedPlayersStr = new ArrayList<>();
            for (UUID uuid : claim.getTrustedPlayers()) {
                trustedPlayersStr.add(uuid.toString());
            }
            
            data.set(path + ".trusted_players", trustedPlayersStr);
        }
        
        plugin.getConfigManager().saveConfig("data.yml");
    }
    
    /**
     * Get a claim by ID
     * @param claimId The ID of the claim
     * @return The claim, or null if not found
     */
    public Claim getClaimById(String claimId) {
        return claims.get(claimId);
    }
    
    /**
     * Create a new claim
     * @param player The player creating the claim
     * @param location1 The first corner of the claim
     * @param location2 The second corner of the claim
     * @return The created claim, or null if the claim could not be created
     */
    public Claim createClaim(Player player, Location location1, Location location2) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if claims are enabled
        if (!config.getBoolean("protection.claims-enabled", true)) {
            return null;
        }
        
        // Check if player has reached claim limit
        if (hasReachedClaimLimit(player)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("limit", String.valueOf(getClaimLimit(player)));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.claim-limit-reached",
                    replacements
            );
            
            return null;
        }
        
        // Check if locations are in the same world
        if (!location1.getWorld().equals(location2.getWorld())) {
            return null;
        }
        
        // Get min and max coordinates
        int minX = Math.min(location1.getBlockX(), location2.getBlockX());
        int minY = Math.min(location1.getBlockY(), location2.getBlockY());
        int minZ = Math.min(location1.getBlockZ(), location2.getBlockZ());
        int maxX = Math.max(location1.getBlockX(), location2.getBlockX());
        int maxY = Math.max(location1.getBlockY(), location2.getBlockY());
        int maxZ = Math.max(location1.getBlockZ(), location2.getBlockZ());
        
        // Check if claim overlaps with existing claims
        for (Claim existingClaim : claims.values()) {
            if (existingClaim.getWorldName().equals(location1.getWorld().getName())) {
                if (existingClaim.overlaps(minX, minY, minZ, maxX, maxY, maxZ)) {
                    return null;
                }
            }
        }
        
        // Generate claim ID
        String claimId = UUID.randomUUID().toString();
        
        // Create claim
        Claim claim = new Claim(
                claimId,
                player.getUniqueId(),
                location1.getWorld().getName(),
                minX, minY, minZ,
                maxX, maxY, maxZ,
                new ArrayList<>()
        );
        
        // Add claim to map
        claims.put(claimId, claim);
        
        // Save claims
        saveClaims();
        
        return claim;
    }
    
    /**
     * Delete a claim
     * @param claim The claim to delete
     * @return True if the claim was deleted successfully
     */
    public boolean deleteClaim(Claim claim) {
        if (claims.containsKey(claim.getId())) {
            claims.remove(claim.getId());
            saveClaims();
            return true;
        }
        
        return false;
    }
    
    /**
     * Get a claim at a location
     * @param location The location
     * @return The claim at the location, or null if no claim exists
     */
    public Claim getClaimAt(Location location) {
        for (Claim claim : claims.values()) {
            if (claim.getWorldName().equals(location.getWorld().getName())) {
                if (claim.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                    return claim;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get all claims owned by a player
     * @param player The player
     * @return A list of claims owned by the player
     */
    public List<Claim> getClaimsByPlayer(OfflinePlayer player) {
        List<Claim> playerClaims = new ArrayList<>();
        
        for (Claim claim : claims.values()) {
            if (claim.getOwnerUUID().equals(player.getUniqueId())) {
                playerClaims.add(claim);
            }
        }
        
        return playerClaims;
    }
    
    /**
     * Check if a player can build at a location
     * @param player The player
     * @param location The location
     * @return True if the player can build at the location
     */
    public boolean canBuild(Player player, Location location) {
        Claim claim = getClaimAt(location);
        
        if (claim == null) {
            return true; // No claim, so player can build
        }
        
        if (claim.getOwnerUUID().equals(player.getUniqueId())) {
            return true; // Player owns the claim
        }
        
        if (claim.isTrusted(player.getUniqueId())) {
            return true; // Player is trusted in the claim
        }
        
        return false; // Player cannot build in this claim
    }
    
    /**
     * Add a trusted player to a claim
     * @param claim The claim
     * @param player The player to trust
     * @return True if the player was added successfully
     */
    public boolean addTrustedPlayer(Claim claim, OfflinePlayer player) {
        if (!claim.isTrusted(player.getUniqueId())) {
            claim.addTrustedPlayer(player.getUniqueId());
            saveClaims();
            return true;
        }
        
        return false;
    }
    
    /**
     * Remove a trusted player from a claim
     * @param claim The claim
     * @param player The player to untrust
     * @return True if the player was removed successfully
     */
    public boolean removeTrustedPlayer(Claim claim, OfflinePlayer player) {
        if (claim.isTrusted(player.getUniqueId())) {
            claim.removeTrustedPlayer(player.getUniqueId());
            saveClaims();
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a player has reached their claim limit
     * @param player The player
     * @return True if the player has reached their claim limit
     */
    public boolean hasReachedClaimLimit(OfflinePlayer player) {
        int claimCount = getClaimsByPlayer(player).size();
        int claimLimit = getClaimLimit(player);
        
        return claimCount >= claimLimit;
    }
    
    /**
     * Get the claim limit for a player
     * @param player The player
     * @return The claim limit
     */
    public int getClaimLimit(OfflinePlayer player) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        int defaultLimit = config.getInt("protection.max-claims-per-player", 3);
        
        // TODO: Implement permission-based claim limits
        return defaultLimit;
    }
}
