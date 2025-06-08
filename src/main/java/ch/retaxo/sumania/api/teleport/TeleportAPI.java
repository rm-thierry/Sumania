package ch.retaxo.sumania.api.teleport;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API for teleportation-related operations
 */
public class TeleportAPI {

    private final Sumania plugin;
    private final Map<UUID, TeleportRequest> teleportRequests = new HashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();
    private final Map<UUID, Location> lastLocations = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public TeleportAPI(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Teleport a player to a location with a delay
     * @param player The player to teleport
     * @param location The location to teleport to
     * @return True if the teleport was initiated successfully
     */
    public boolean teleport(Player player, Location location) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if teleportation is enabled
        if (!config.getBoolean("teleportation.enabled", true)) {
            return false;
        }
        
        // Check if player is in cooldown
        if (isInCooldown(player)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(getCooldownTimeLeft(player)));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleport-cooldown",
                    replacements
            );
            
            return false;
        }
        
        // Get teleport delay
        int delay = config.getInt("teleportation.delay", 5);
        
        // Store player's last location for movement check
        lastLocations.put(player.getUniqueId(), player.getLocation());
        
        // Send teleporting message if delay > 0
        if (delay > 0) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(delay));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleporting",
                    replacements
            );
        }
        
        // Cancel any existing teleport task
        if (teleportTasks.containsKey(player.getUniqueId())) {
            teleportTasks.get(player.getUniqueId()).cancel();
        }
        
        // Create teleport task
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check if player has moved
                if (delay > 0 && hasPlayerMoved(player)) {
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "teleport.teleport-cancelled",
                            null
                    );
                    
                    teleportTasks.remove(player.getUniqueId());
                    lastLocations.remove(player.getUniqueId());
                    return;
                }
                
                // Teleport player
                player.teleport(location);
                
                // Send success message
                plugin.getAPI().getPlayerAPI().sendMessage(
                        player,
                        "teleport.teleport-success",
                        null
                );
                
                // Set cooldown
                setCooldown(player);
                
                // Remove task and last location
                teleportTasks.remove(player.getUniqueId());
                lastLocations.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, delay * 20L);
        
        // Store task
        teleportTasks.put(player.getUniqueId(), task);
        
        return true;
    }
    
    /**
     * Check if a player has moved since teleport was initiated
     * @param player The player to check
     * @return True if the player has moved
     */
    private boolean hasPlayerMoved(Player player) {
        if (!lastLocations.containsKey(player.getUniqueId())) {
            return false;
        }
        
        Location last = lastLocations.get(player.getUniqueId());
        Location current = player.getLocation();
        
        return last.getWorld() != current.getWorld()
                || last.getBlockX() != current.getBlockX()
                || last.getBlockY() != current.getBlockY()
                || last.getBlockZ() != current.getBlockZ();
    }
    
    /**
     * Set a teleport cooldown for a player
     * @param player The player
     */
    private void setCooldown(Player player) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        int cooldown = config.getInt("teleportation.cooldown", 60);
        
        if (cooldown > 0) {
            teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
        }
    }
    
    /**
     * Check if a player is in teleport cooldown
     * @param player The player
     * @return True if the player is in cooldown
     */
    private boolean isInCooldown(Player player) {
        if (!teleportCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        
        return System.currentTimeMillis() < teleportCooldowns.get(player.getUniqueId());
    }
    
    /**
     * Get the time left on a player's cooldown
     * @param player The player
     * @return The time left in seconds
     */
    private int getCooldownTimeLeft(Player player) {
        if (!teleportCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        
        long timeLeft = teleportCooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
        return Math.max(0, (int) (timeLeft / 1000));
    }
    
    /**
     * Create a teleport request from one player to another
     * @param from The player requesting the teleport
     * @param to The player to teleport to
     * @param type The type of teleport request
     * @return True if the request was created successfully
     */
    public boolean createTeleportRequest(Player from, Player to, TeleportRequestType type) {
        // Create teleport request
        TeleportRequest request = new TeleportRequest(from.getUniqueId(), to.getUniqueId(), type);
        teleportRequests.put(to.getUniqueId(), request);
        
        // Send request messages
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", to.getName());
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                from,
                "teleport.teleport-request-sent",
                replacements
        );
        
        replacements = new HashMap<>();
        replacements.put("player", from.getName());
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                to,
                "teleport.teleport-request-received",
                replacements
        );
        
        // Expire request after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (teleportRequests.containsKey(to.getUniqueId()) &&
                    teleportRequests.get(to.getUniqueId()).equals(request)) {
                teleportRequests.remove(to.getUniqueId());
            }
        }, 1200L);
        
        return true;
    }
    
    /**
     * Accept a teleport request
     * @param player The player accepting the request
     * @return True if the request was accepted successfully
     */
    public boolean acceptTeleportRequest(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (!teleportRequests.containsKey(playerUUID)) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleport-no-requests",
                    null
            );
            
            return false;
        }
        
        TeleportRequest request = teleportRequests.get(playerUUID);
        Player requester = Bukkit.getPlayer(request.getFromUUID());
        
        if (requester == null || !requester.isOnline()) {
            teleportRequests.remove(playerUUID);
            return false;
        }
        
        // Send acceptance message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", player.getName());
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                requester,
                "teleport.teleport-request-accepted",
                replacements
        );
        
        // Perform teleport based on request type
        if (request.getType() == TeleportRequestType.TO_PLAYER) {
            teleport(requester, player.getLocation());
        } else if (request.getType() == TeleportRequestType.FROM_PLAYER) {
            teleport(player, requester.getLocation());
        }
        
        // Remove request
        teleportRequests.remove(playerUUID);
        
        return true;
    }
    
    /**
     * Deny a teleport request
     * @param player The player denying the request
     * @return True if the request was denied successfully
     */
    public boolean denyTeleportRequest(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (!teleportRequests.containsKey(playerUUID)) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "teleport.teleport-no-requests",
                    null
            );
            
            return false;
        }
        
        TeleportRequest request = teleportRequests.get(playerUUID);
        Player requester = Bukkit.getPlayer(request.getFromUUID());
        
        if (requester != null && requester.isOnline()) {
            // Send denial message
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", player.getName());
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    requester,
                    "teleport.teleport-request-denied",
                    replacements
            );
        }
        
        // Remove request
        teleportRequests.remove(playerUUID);
        
        return true;
    }
    
    /**
     * Set a warp location
     * @param name The name of the warp
     * @param location The location of the warp
     */
    public void setWarp(String name, Location location) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        
        // Format location as string
        String locationStr = String.format(
                "%s,%f,%f,%f,%f,%f",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
        
        // Set the warp
        data.set("warps." + name, locationStr);
        
        // Save the data file
        plugin.getConfigManager().saveConfig("data.yml");
    }
    
    /**
     * Get a warp location
     * @param name The name of the warp
     * @return The warp location, or null if not found
     */
    public Location getWarp(String name) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String locationStr = data.getString("warps." + name);
        
        if (locationStr != null) {
            String[] parts = locationStr.split(",");
            
            if (parts.length == 6) {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                float yaw = Float.parseFloat(parts[4]);
                float pitch = Float.parseFloat(parts[5]);
                
                return new Location(
                        Bukkit.getWorld(worldName),
                        x, y, z, yaw, pitch
                );
            }
        }
        
        return null;
    }
    
    /**
     * Delete a warp
     * @param name The name of the warp
     * @return True if the warp was deleted successfully
     */
    public boolean deleteWarp(String name) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        
        if (data.contains("warps." + name)) {
            data.set("warps." + name, null);
            plugin.getConfigManager().saveConfig("data.yml");
            return true;
        }
        
        return false;
    }
    
    /**
     * Get all warps
     * @return A map of warp names to locations
     */
    public Map<String, Location> getWarps() {
        Map<String, Location> warps = new HashMap<>();
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        
        if (data.contains("warps")) {
            for (String name : data.getConfigurationSection("warps").getKeys(false)) {
                Location location = getWarp(name);
                
                if (location != null) {
                    warps.put(name, location);
                }
            }
        }
        
        return warps;
    }
}
