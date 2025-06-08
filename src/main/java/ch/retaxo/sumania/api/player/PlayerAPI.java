package ch.retaxo.sumania.api.player;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Home;
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
 * API for player-related operations
 */
public class PlayerAPI {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public PlayerAPI(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get all homes of a player
     * @param player The player
     * @return A map of home names to home objects
     */
    public Map<String, Home> getHomes(OfflinePlayer player) {
        Map<String, Home> homes = new HashMap<>();
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".homes";
        
        if (data.contains(path)) {
            ConfigurationSection homesSection = data.getConfigurationSection(path);
            
            if (homesSection != null) {
                for (String homeName : homesSection.getKeys(false)) {
                    String locationStr = homesSection.getString(homeName);
                    
                    if (locationStr != null) {
                        String[] parts = locationStr.split(",");
                        
                        if (parts.length == 6) {
                            String worldName = parts[0];
                            double x = Double.parseDouble(parts[1]);
                            double y = Double.parseDouble(parts[2]);
                            double z = Double.parseDouble(parts[3]);
                            float yaw = Float.parseFloat(parts[4]);
                            float pitch = Float.parseFloat(parts[5]);
                            
                            Location location = new Location(
                                    Bukkit.getWorld(worldName),
                                    x, y, z, yaw, pitch
                            );
                            
                            homes.put(homeName, new Home(homeName, location));
                        }
                    }
                }
            }
        }
        
        return homes;
    }
    
    /**
     * Get a specific home of a player
     * @param player The player
     * @param homeName The name of the home
     * @return The home, or null if not found
     */
    public Home getHome(OfflinePlayer player, String homeName) {
        Map<String, Home> homes = getHomes(player);
        return homes.get(homeName);
    }
    
    /**
     * Set a home for a player
     * @param player The player
     * @param homeName The name of the home
     * @param location The location of the home
     * @return True if the home was set successfully
     */
    public boolean setHome(OfflinePlayer player, String homeName, Location location) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String playerPath = "players." + player.getUniqueId();
        String homesPath = playerPath + ".homes";
        
        // Ensure player exists in data file
        if (!data.contains(playerPath + ".name")) {
            data.set(playerPath + ".name", player.getName());
        }
        
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
        
        // Set the home
        data.set(homesPath + "." + homeName, locationStr);
        
        // Save the data file
        plugin.getConfigManager().saveConfig("data.yml");
        
        return true;
    }
    
    /**
     * Delete a home for a player
     * @param player The player
     * @param homeName The name of the home
     * @return True if the home was deleted successfully
     */
    public boolean deleteHome(OfflinePlayer player, String homeName) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".homes." + homeName;
        
        if (data.contains(path)) {
            data.set(path, null);
            plugin.getConfigManager().saveConfig("data.yml");
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a player has reached their home limit
     * @param player The player
     * @return True if the player has reached their home limit
     */
    public boolean hasReachedHomeLimit(OfflinePlayer player) {
        int homeCount = getHomes(player).size();
        int homeLimit = getHomeLimit(player);
        
        return homeCount >= homeLimit;
    }
    
    /**
     * Get the home limit for a player
     * @param player The player
     * @return The home limit
     */
    public int getHomeLimit(OfflinePlayer player) {
        // TODO: Implement permission-based home limits
        return 3;
    }
    
    /**
     * Get a message from the messages.yml file
     * @param path The path to the message
     * @param replacements The replacements to make in the message
     * @return The formatted message
     */
    public String getMessage(String path, Map<String, String> replacements) {
        FileConfiguration messages = plugin.getConfigManager().getConfig("messages.yml");
        String message = messages.getString(path, "Message not found: " + path);
        
        // Replace color codes
        message = message.replace("&", "§");
        
        // Replace placeholders
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        return message;
    }
    
    /**
     * Send a message to a player
     * @param player The player
     * @param path The path to the message in messages.yml
     * @param replacements The replacements to make in the message
     */
    public void sendMessage(Player player, String path, Map<String, String> replacements) {
        // Get the prefix from the config
        String prefix = plugin.getConfigManager().getPrefix();
        
        // Add prefix to the message
        player.sendMessage(prefix + getMessage(path, replacements));
    }
    
    /**
     * Send a message to a command sender
     * @param sender The command sender
     * @param path The path to the message in messages.yml
     * @param replacements The replacements to make in the message
     */
    public void sendMessage(CommandSender sender, String path, Map<String, String> replacements) {
        if (sender instanceof Player) {
            sendMessage((Player) sender, path, replacements);
        } else {
            // Get the prefix from the config
            String prefix = plugin.getConfigManager().getPrefix();
            
            // Add prefix to the message
            sender.sendMessage(prefix + getMessage(path, replacements));
        }
    }
    
    /**
     * Send a direct message to a command sender with the prefix
     * @param sender The command sender
     * @param message The message to send
     */
    public void sendPrefixedMessage(CommandSender sender, String message) {
        // Get the prefix from the config
        String prefix = plugin.getConfigManager().getPrefix();
        
        // Add prefix to the message
        sender.sendMessage(prefix + message);
    }
    
    /**
     * Format a message with the prefix
     * @param path The path to the message in messages.yml
     * @param replacements The replacements to make in the message
     * @return The formatted message with prefix
     */
    public String formatMessage(String path, Map<String, String> replacements) {
        // Get the prefix from the config
        String prefix = plugin.getConfigManager().getPrefix();
        
        // Add prefix to the message
        return prefix + getMessage(path, replacements);
    }
}
