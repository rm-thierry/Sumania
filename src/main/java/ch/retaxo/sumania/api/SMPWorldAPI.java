package ch.retaxo.sumania.api;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.SMPWorld;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API for SMP world management
 */
public class SMPWorldAPI {

    private final Sumania plugin;
    private SMPWorld activeSMPWorld;
    
    // Cache of player stats in SMP world
    private final Map<UUID, Location> lastNonSMPLocations = new HashMap<>();
    private final Map<UUID, GameMode> lastGameModes = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public SMPWorldAPI(Sumania plugin) {
        this.plugin = plugin;
        loadSMPWorld();
    }
    
    /**
     * Load the SMP world from config
     */
    private void loadSMPWorld() {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if SMP system is enabled
        if (!config.getBoolean("smp.enabled", true)) {
            return;
        }
        
        // Get world name from config
        String worldName = config.getString("smp.world-name");
        
        if (worldName == null || worldName.isEmpty()) {
            // Set default world
            worldName = Bukkit.getWorlds().get(0).getName();
            config.set("smp.world-name", worldName);
            plugin.getConfigManager().saveConfig("config.yml");
        }
        
        // Load or create SMP world
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            // Create the world if it doesn't exist
            plugin.getLogger().info("Creating SMP world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.NORMAL);
            creator.generateStructures(true);
            world = creator.createWorld();
        }
        
        if (world == null) {
            plugin.getLogger().severe("Failed to load or create SMP world: " + worldName);
            return;
        }
        
        // Get spawn location
        double spawnX = config.getDouble("smp.spawn.x", world.getSpawnLocation().getX());
        double spawnY = config.getDouble("smp.spawn.y", world.getSpawnLocation().getY());
        double spawnZ = config.getDouble("smp.spawn.z", world.getSpawnLocation().getZ());
        float spawnYaw = (float) config.getDouble("smp.spawn.yaw", 0.0);
        float spawnPitch = (float) config.getDouble("smp.spawn.pitch", 0.0);
        
        Location spawnPoint = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
        
        // Get border size
        int borderSize = config.getInt("smp.border-size", 10000);
        
        // Set world border
        WorldBorder border = world.getWorldBorder();
        border.setCenter(spawnPoint);
        border.setSize(borderSize);
        
        // Set game rules
        configureGameRules(world, config);
        
        // Create SMP world object
        activeSMPWorld = new SMPWorld(worldName, spawnPoint, borderSize, true);
    }
    
    /**
     * Configure game rules for the SMP world
     * @param world The world to configure
     * @param config The config file
     */
    private void configureGameRules(World world, FileConfiguration config) {
        // Default game rules
        world.setGameRule(GameRule.KEEP_INVENTORY, config.getBoolean("smp.game-rules.keep-inventory", true));
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, config.getBoolean("smp.game-rules.immediate-respawn", false));
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, config.getBoolean("smp.game-rules.announce-advancements", true));
        world.setGameRule(GameRule.DO_INSOMNIA, config.getBoolean("smp.game-rules.do-insomnia", true));
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, config.getBoolean("smp.game-rules.do-patrol-spawning", true));
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, config.getBoolean("smp.game-rules.do-trader-spawning", true));
        world.setGameRule(GameRule.MOB_GRIEFING, config.getBoolean("smp.game-rules.mob-griefing", true));
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, config.getBoolean("smp.game-rules.show-death-messages", true));
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, config.getInt("smp.game-rules.random-tick-speed", 3));
    }
    
    /**
     * Get the active SMP world
     * @return The active SMP world
     */
    public SMPWorld getActiveSMPWorld() {
        return activeSMPWorld;
    }
    
    /**
     * Set the active SMP world
     * @param worldName The name of the world
     * @return True if the world was set successfully
     */
    public boolean setActiveSMPWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            return false;
        }
        
        // Save current SMP world if it exists
        if (activeSMPWorld != null) {
            saveWorldToConfig(activeSMPWorld);
        }
        
        // Update active SMP world
        activeSMPWorld = new SMPWorld(worldName, world.getSpawnLocation(), 10000, true);
        
        // Save to config
        saveWorldToConfig(activeSMPWorld);
        
        return true;
    }
    
    /**
     * Save SMP world to config
     * @param smpWorld The SMP world to save
     */
    private void saveWorldToConfig(SMPWorld smpWorld) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        config.set("smp.enabled", smpWorld.isEnabled());
        config.set("smp.world-name", smpWorld.getWorldName());
        
        if (smpWorld.getSpawnPoint() != null) {
            config.set("smp.spawn.x", smpWorld.getSpawnPoint().getX());
            config.set("smp.spawn.y", smpWorld.getSpawnPoint().getY());
            config.set("smp.spawn.z", smpWorld.getSpawnPoint().getZ());
            config.set("smp.spawn.yaw", smpWorld.getSpawnPoint().getYaw());
            config.set("smp.spawn.pitch", smpWorld.getSpawnPoint().getPitch());
        }
        
        config.set("smp.border-size", smpWorld.getBorderSize());
        
        plugin.getConfigManager().saveConfig("config.yml");
    }
    
    /**
     * Set the SMP world's spawn point
     * @param location The new spawn point
     * @return True if the spawn point was set successfully
     */
    public boolean setSpawnPoint(Location location) {
        if (activeSMPWorld == null || !activeSMPWorld.exists()) {
            return false;
        }
        
        World world = activeSMPWorld.getWorld();
        
        if (world == null || !world.equals(location.getWorld())) {
            return false;
        }
        
        // Set the world's spawn location
        world.setSpawnLocation(location);
        
        // Update SMP world model
        activeSMPWorld.setSpawnPoint(location);
        
        // Save to config
        saveWorldToConfig(activeSMPWorld);
        
        return true;
    }
    
    /**
     * Teleport a player to the SMP world spawn
     * @param player The player to teleport
     * @return True if the player was teleported successfully
     */
    public boolean teleportToSMP(Player player) {
        if (activeSMPWorld == null || !activeSMPWorld.exists()) {
            return false;
        }
        
        World smpWorld = activeSMPWorld.getWorld();
        Location spawnPoint = activeSMPWorld.getSpawnPoint();
        
        if (smpWorld == null || spawnPoint == null) {
            return false;
        }
        
        // If not already in SMP world, store current location and gamemode
        if (!player.getWorld().equals(smpWorld)) {
            lastNonSMPLocations.put(player.getUniqueId(), player.getLocation());
            lastGameModes.put(player.getUniqueId(), player.getGameMode());
            
            // Set appropriate game mode
            FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
            String defaultGameMode = config.getString("smp.default-game-mode", "SURVIVAL");
            try {
                GameMode gameMode = GameMode.valueOf(defaultGameMode);
                player.setGameMode(gameMode);
            } catch (IllegalArgumentException e) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        
        // Teleport player to spawn
        return plugin.getAPI().getTeleportAPI().teleport(player, spawnPoint);
    }
    
    /**
     * Teleport a player out of the SMP world
     * @param player The player to teleport
     * @return True if the player was teleported successfully
     */
    public boolean teleportFromSMP(Player player) {
        if (activeSMPWorld == null || !activeSMPWorld.exists() || 
            !player.getWorld().equals(activeSMPWorld.getWorld())) {
            return false;
        }
        
        // If we have a saved location, teleport back to it
        if (lastNonSMPLocations.containsKey(player.getUniqueId())) {
            Location lastLocation = lastNonSMPLocations.get(player.getUniqueId());
            boolean success = plugin.getAPI().getTeleportAPI().teleport(player, lastLocation);
            
            if (success) {
                // Restore game mode
                if (lastGameModes.containsKey(player.getUniqueId())) {
                    player.setGameMode(lastGameModes.get(player.getUniqueId()));
                }
                
                // Remove from cache
                lastNonSMPLocations.remove(player.getUniqueId());
                lastGameModes.remove(player.getUniqueId());
            }
            
            return success;
        } else {
            // If no saved location, teleport to server spawn
            World mainWorld = Bukkit.getWorlds().get(0);
            return plugin.getAPI().getTeleportAPI().teleport(player, mainWorld.getSpawnLocation());
        }
    }
    
    /**
     * Reset the SMP world
     * @return True if the world was reset successfully
     */
    public boolean resetSMPWorld() {
        if (activeSMPWorld == null) {
            return false;
        }
        
        String worldName = activeSMPWorld.getWorldName();
        World world = activeSMPWorld.getWorld();
        
        if (world == null) {
            return false;
        }
        
        // Teleport all players out of the world
        for (Player player : world.getPlayers()) {
            teleportFromSMP(player);
        }
        
        // Get border size and other settings to preserve
        int borderSize = activeSMPWorld.getBorderSize();
        
        // Unload the world
        if (!Bukkit.unloadWorld(world, false)) {
            plugin.getLogger().severe("Failed to unload SMP world: " + worldName);
            return false;
        }
        
        // Delete the world files
        File worldFolder = world.getWorldFolder();
        if (!deleteWorldFolder(worldFolder)) {
            plugin.getLogger().severe("Failed to delete SMP world folder: " + worldFolder.getPath());
            
            // Try to reload the world since deletion failed
            Bukkit.createWorld(new WorldCreator(worldName));
            return false;
        }
        
        // Create new world
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.NORMAL);
        creator.generateStructures(true);
        World newWorld = creator.createWorld();
        
        if (newWorld == null) {
            plugin.getLogger().severe("Failed to create new SMP world: " + worldName);
            return false;
        }
        
        // Set world border
        WorldBorder border = newWorld.getWorldBorder();
        border.setCenter(newWorld.getSpawnLocation());
        border.setSize(borderSize);
        
        // Configure game rules
        configureGameRules(newWorld, plugin.getConfigManager().getConfig("config.yml"));
        
        // Update SMP world object
        activeSMPWorld = new SMPWorld(worldName, newWorld.getSpawnLocation(), borderSize, true);
        
        // Save to config
        saveWorldToConfig(activeSMPWorld);
        
        return true;
    }
    
    /**
     * Delete a world folder recursively
     * @param folder The folder to delete
     * @return True if the folder was deleted successfully
     */
    private boolean deleteWorldFolder(File folder) {
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return folder.delete();
    }
    
    /**
     * Get a list of all available worlds
     * @return A list of world names
     */
    public List<String> getAvailableWorlds() {
        List<String> worldNames = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            worldNames.add(world.getName());
        }
        return worldNames;
    }
}