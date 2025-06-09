package ch.retaxo.sumania.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents an SMP world
 */
public class SMPWorld {
    
    private String worldName;
    private Location spawnPoint;
    private final int borderSize;
    private boolean enabled;
    
    /**
     * Constructor with minimal parameters
     * @param worldName The name of the world
     */
    public SMPWorld(String worldName) {
        this.worldName = worldName;
        World world = Bukkit.getWorld(worldName);
        this.spawnPoint = world != null ? world.getSpawnLocation() : null;
        this.borderSize = 10000;
        this.enabled = true;
    }
    
    /**
     * Constructor with all parameters
     * @param worldName The name of the world
     * @param spawnPoint The spawn point in the world
     * @param borderSize The world border size
     * @param enabled Whether the SMP world is enabled
     */
    public SMPWorld(String worldName, Location spawnPoint, int borderSize, boolean enabled) {
        this.worldName = worldName;
        this.spawnPoint = spawnPoint;
        this.borderSize = borderSize;
        this.enabled = enabled;
    }
    
    /**
     * Get the world object
     * @return The world object
     */
    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }
    
    /**
     * Get the world name
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * Set the world name
     * @param worldName The world name
     */
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    /**
     * Get the spawn point
     * @return The spawn point
     */
    public Location getSpawnPoint() {
        return spawnPoint;
    }
    
    /**
     * Set the spawn point
     * @param spawnPoint The spawn point
     */
    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
    }
    
    /**
     * Get the border size
     * @return The border size
     */
    public int getBorderSize() {
        return borderSize;
    }
    
    /**
     * Check if the SMP world is enabled
     * @return True if the SMP world is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set whether the SMP world is enabled
     * @param enabled Whether the SMP world is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Check if the world exists
     * @return True if the world exists
     */
    public boolean exists() {
        return Bukkit.getWorld(worldName) != null;
    }
}