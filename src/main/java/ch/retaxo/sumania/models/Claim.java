package ch.retaxo.sumania.models;

import java.util.List;
import java.util.UUID;

/**
 * Represents a land claim
 */
public class Claim {

    private final String id;
    private final UUID ownerUUID;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;
    private final List<UUID> trustedPlayers;
    private final int markerX, markerY, markerZ;
    
    /**
     * Constructor
     * @param id The unique ID of the claim
     * @param ownerUUID The UUID of the player who owns the claim
     * @param worldName The name of the world the claim is in
     * @param minX The minimum X coordinate
     * @param minY The minimum Y coordinate
     * @param minZ The minimum Z coordinate
     * @param maxX The maximum X coordinate
     * @param maxY The maximum Y coordinate
     * @param maxZ The maximum Z coordinate
     * @param markerX The X coordinate of the marker block
     * @param markerY The Y coordinate of the marker block
     * @param markerZ The Z coordinate of the marker block
     * @param trustedPlayers A list of UUIDs of players who are trusted in the claim
     */
    public Claim(String id, UUID ownerUUID, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, 
                int markerX, int markerY, int markerZ, List<UUID> trustedPlayers) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.markerX = markerX;
        this.markerY = markerY;
        this.markerZ = markerZ;
        this.trustedPlayers = trustedPlayers;
    }
    
    /**
     * Get the unique ID of the claim
     * @return The ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the UUID of the player who owns the claim
     * @return The UUID
     */
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    /**
     * Get the name of the world the claim is in
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Get the minimum X coordinate
     * @return The minimum X coordinate
     */
    public int getMinX() {
        return minX;
    }
    
    /**
     * Get the minimum Y coordinate
     * @return The minimum Y coordinate
     */
    public int getMinY() {
        return minY;
    }
    
    /**
     * Get the minimum Z coordinate
     * @return The minimum Z coordinate
     */
    public int getMinZ() {
        return minZ;
    }
    
    /**
     * Get the maximum X coordinate
     * @return The maximum X coordinate
     */
    public int getMaxX() {
        return maxX;
    }
    
    /**
     * Get the maximum Y coordinate
     * @return The maximum Y coordinate
     */
    public int getMaxY() {
        return maxY;
    }
    
    /**
     * Get the maximum Z coordinate
     * @return The maximum Z coordinate
     */
    public int getMaxZ() {
        return maxZ;
    }
    
    /**
     * Get the X coordinate of the marker block
     * @return The X coordinate of the marker block
     */
    public int getMarkerX() {
        return markerX;
    }
    
    /**
     * Get the Y coordinate of the marker block
     * @return The Y coordinate of the marker block
     */
    public int getMarkerY() {
        return markerY;
    }
    
    /**
     * Get the Z coordinate of the marker block
     * @return The Z coordinate of the marker block
     */
    public int getMarkerZ() {
        return markerZ;
    }
    
    /**
     * Get a list of UUIDs of players who are trusted in the claim
     * @return The list of trusted players
     */
    public List<UUID> getTrustedPlayers() {
        return trustedPlayers;
    }
    
    /**
     * Check if a player is trusted in the claim
     * @param playerUUID The UUID of the player to check
     * @return True if the player is trusted
     */
    public boolean isTrusted(UUID playerUUID) {
        return trustedPlayers.contains(playerUUID);
    }
    
    /**
     * Add a trusted player to the claim
     * @param playerUUID The UUID of the player to add
     */
    public void addTrustedPlayer(UUID playerUUID) {
        if (!trustedPlayers.contains(playerUUID)) {
            trustedPlayers.add(playerUUID);
        }
    }
    
    /**
     * Remove a trusted player from the claim
     * @param playerUUID The UUID of the player to remove
     */
    public void removeTrustedPlayer(UUID playerUUID) {
        trustedPlayers.remove(playerUUID);
    }
    
    /**
     * Check if a point is within the claim
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @return True if the point is within the claim
     */
    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
    
    /**
     * Check if this claim overlaps with another region
     * @param otherMinX The minimum X coordinate of the other region
     * @param otherMinY The minimum Y coordinate of the other region
     * @param otherMinZ The minimum Z coordinate of the other region
     * @param otherMaxX The maximum X coordinate of the other region
     * @param otherMaxY The maximum Y coordinate of the other region
     * @param otherMaxZ The maximum Z coordinate of the other region
     * @return True if the regions overlap
     */
    public boolean overlaps(int otherMinX, int otherMinY, int otherMinZ, int otherMaxX, int otherMaxY, int otherMaxZ) {
        return minX <= otherMaxX && maxX >= otherMinX && minY <= otherMaxY && maxY >= otherMinY && minZ <= otherMaxZ && maxZ >= otherMinZ;
    }
    
    /**
     * Check if the given coordinates match the marker block location
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @return True if the coordinates match the marker block location
     */
    public boolean isMarkerBlock(int x, int y, int z) {
        return x == markerX && y == markerY && z == markerZ;
    }
}