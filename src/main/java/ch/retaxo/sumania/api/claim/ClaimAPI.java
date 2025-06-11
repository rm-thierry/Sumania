package ch.retaxo.sumania.api.claim;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Claim;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
                    int markerX = claimSection.getInt("marker_x");
                    int markerY = claimSection.getInt("marker_y");
                    int markerZ = claimSection.getInt("marker_z");
                    
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
                            markerX, markerY, markerZ,
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
            data.set(path + ".marker_x", claim.getMarkerX());
            data.set(path + ".marker_y", claim.getMarkerY());
            data.set(path + ".marker_z", claim.getMarkerZ());
            
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
     * Create a new claim using a marker block
     * @param player The player creating the claim
     * @param block The marker block
     * @return The created claim, or null if the claim could not be created
     */
    public Claim createClaim(Player player, Block block) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if claims are enabled
        if (!config.getBoolean("protection.claims-enabled", true)) {
            plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.claims-disabled");
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
        
        // Get claim radius from config
        int radius = config.getInt("protection.claim-marker.radius", 16);
        
        // Calculate claim boundaries
        int markerX = block.getX();
        int markerY = block.getY();
        int markerZ = block.getZ();
        int minX = markerX - radius;
        int minY = Math.max(0, markerY - radius);
        int minZ = markerZ - radius;
        int maxX = markerX + radius;
        int maxY = Math.min(255, markerY + radius);
        int maxZ = markerZ + radius;
        
        // Check if claim overlaps with existing claims
        for (Claim existingClaim : claims.values()) {
            if (existingClaim.getWorldName().equals(block.getWorld().getName())) {
                if (existingClaim.overlaps(minX, minY, minZ, maxX, maxY, maxZ)) {
                    plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.claim-overlap");
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
                block.getWorld().getName(),
                minX, minY, minZ,
                maxX, maxY, maxZ,
                markerX, markerY, markerZ,
                new ArrayList<>()
        );
        
        // Add claim to map
        claims.put(claimId, claim);
        
        // Save claims
        saveClaims();
        
        // Send success message
        plugin.getAPI().getPlayerAPI().sendMessage(player, "claim.claim-created");
        
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
     * Get a claim by marker block location
     * @param location The location of the marker block
     * @return The claim, or null if not found
     */
    public Claim getClaimByMarker(Location location) {
        for (Claim claim : claims.values()) {
            if (claim.getWorldName().equals(location.getWorld().getName())) {
                if (claim.isMarkerBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
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
    
    /**
     * Create a claim marker item
     * @return The claim marker item
     */
    public ItemStack createClaimMarkerItem() {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        String materialName = config.getString("protection.claim-marker.material", "EMERALD_BLOCK");
        String displayName = config.getString("protection.claim-marker.name", "§a§lGrundstücks-Marker");
        List<String> lore = config.getStringList("protection.claim-marker.lore");
        
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.EMERALD_BLOCK;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            
            // Add custom tag to identify as claim marker
            // Note: In a real implementation, you would use PersistentDataContainer
            // For simplicity, we'll rely on the display name and lore
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Check if an item is a claim marker
     * @param item The item to check
     * @return True if the item is a claim marker
     */
    public boolean isClaimMarker(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        String materialName = config.getString("protection.claim-marker.material", "EMERALD_BLOCK");
        String displayName = config.getString("protection.claim-marker.name", "§a§lGrundstücks-Marker");
        
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.EMERALD_BLOCK;
        }
        
        return item.getType() == material && item.getItemMeta().getDisplayName().equals(displayName);
    }
    
    /**
     * Get the price of a claim marker
     * @return The price
     */
    public double getClaimMarkerPrice() {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        return config.getDouble("protection.claim-marker.price", 5000.0);
    }
    
    /**
     * Buy a claim marker
     * @param player The player buying the marker
     * @return True if the purchase was successful
     */
    public boolean buyClaimMarker(Player player) {
        double price = getClaimMarkerPrice();
        
        // Check if player has enough money
        if (plugin.getAPI().getEconomyAPI().getBalance(player) < price) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("price", String.format("%.2f", price));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "claim.not-enough-money",
                    replacements
            );
            
            return false;
        }
        
        // Take money from player
        plugin.getAPI().getEconomyAPI().withdraw(player, price);
        
        // Give claim marker item
        player.getInventory().addItem(createClaimMarkerItem());
        
        // Send success message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("price", String.format("%.2f", price));
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "claim.claim-marker-bought",
                replacements
        );
        
        return true;
    }
    
    /**
     * Get the trusted players of a claim
     * @param claim The claim
     * @return A list of player names
     */
    public List<String> getTrustedPlayerNames(Claim claim) {
        return claim.getTrustedPlayers().stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .filter(name -> name != null)
                .collect(Collectors.toList());
    }
}