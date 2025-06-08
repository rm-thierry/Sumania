package ch.retaxo.sumania.api.player;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Home;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// PlaceholderAPI
import me.clip.placeholderapi.PlaceholderAPI;

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
        
        // Process general placeholders if sender is a player
        if (replacements != null && replacements.containsKey("player_obj")) {
            String playerObjStr = replacements.get("player_obj");
            if (playerObjStr != null && playerObjStr.equals("true")) {
                Player player = null;
                if (replacements.containsKey("player_name")) {
                    player = Bukkit.getPlayer(replacements.get("player_name"));
                }
                
                if (player != null) {
                    message = processPlaceholders(player, message);
                }
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
    
    /**
     * Get the number of kills for a player
     * @param player The player
     * @return The number of kills
     */
    public int getKills(OfflinePlayer player) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".stats.kills";
        
        return data.getInt(path, 0);
    }
    
    /**
     * Set the number of kills for a player
     * @param player The player
     * @param kills The number of kills
     */
    public void setKills(OfflinePlayer player, int kills) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId();
        
        // Ensure player exists in data file
        if (!data.contains(path + ".name")) {
            data.set(path + ".name", player.getName());
        }
        
        // Set the kills
        data.set(path + ".stats.kills", kills);
        
        // Save the data file
        plugin.getConfigManager().saveConfig("data.yml");
    }
    
    /**
     * Increment the number of kills for a player
     * @param player The player
     * @return The new number of kills
     */
    public int incrementKills(OfflinePlayer player) {
        int kills = getKills(player);
        kills++;
        setKills(player, kills);
        return kills;
    }
    
    /**
     * Get the number of deaths for a player
     * @param player The player
     * @return The number of deaths
     */
    public int getDeaths(OfflinePlayer player) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".stats.deaths";
        
        return data.getInt(path, 0);
    }
    
    /**
     * Set the number of deaths for a player
     * @param player The player
     * @param deaths The number of deaths
     */
    public void setDeaths(OfflinePlayer player, int deaths) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId();
        
        // Ensure player exists in data file
        if (!data.contains(path + ".name")) {
            data.set(path + ".name", player.getName());
        }
        
        // Set the deaths
        data.set(path + ".stats.deaths", deaths);
        
        // Save the data file
        plugin.getConfigManager().saveConfig("data.yml");
    }
    
    /**
     * Increment the number of deaths for a player
     * @param player The player
     * @return The new number of deaths
     */
    public int incrementDeaths(OfflinePlayer player) {
        int deaths = getDeaths(player);
        deaths++;
        setDeaths(player, deaths);
        return deaths;
    }
    
    /**
     * Ban a player
     * @param target The player to ban
     * @param reason The reason for the ban
     * @param admin The admin who issued the ban
     * @param duration The duration of the ban in milliseconds, or -1 for permanent
     * @return True if the player was banned successfully
     */
    public boolean banPlayer(OfflinePlayer target, String reason, String admin, long duration) {
        // Ensure player exists in database
        ensurePlayerInDatabase(target);
        
        // Set ban information
        long now = System.currentTimeMillis();
        long expiration = duration > 0 ? now + duration : -1;
        
        // Insert ban into database
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "INSERT INTO " + tablePrefix + "bans " +
                           "(uuid, reason, admin, ban_time, expiration, active) " +
                           "VALUES (?, ?, ?, ?, ?, 1)";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, target.getUniqueId().toString());
                stmt.setString(2, reason);
                stmt.setString(3, admin);
                stmt.setTimestamp(4, new java.sql.Timestamp(now));
                stmt.setTimestamp(5, expiration > 0 ? new java.sql.Timestamp(expiration) : null);
                
                stmt.executeUpdate();
            }
            
            // Also update legacy data.yml for backwards compatibility
            updateLegacyBanData(target, reason, admin, now, expiration);
            
            // Kick the player if online
            if (target.isOnline()) {
                Player player = target.getPlayer();
                if (player != null) {
                    String banMessage = formatBanMessage(reason, admin, expiration);
                    player.kickPlayer(banMessage);
                }
            }
            
            // Broadcast ban message
            String banMessage = "§c" + target.getName() + " §7wurde von §c" + admin + " §7gebannt.";
            String durationStr = formatDuration(duration);
            if (duration > 0) {
                banMessage += " §7Dauer: §c" + durationStr;
            } else {
                banMessage += " §7Dauer: §cPermanent";
            }
            banMessage += " §7Grund: §c" + reason;
            plugin.getAPI().getChatAPI().broadcast(banMessage);
            
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save ban to database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Unban a player
     * @param target The player to unban
     * @param admin The admin who issued the unban
     * @return True if the player was unbanned successfully
     */
    public boolean unbanPlayer(OfflinePlayer target, String admin) {
        // Check if player is banned
        if (!isBanned(target)) {
            return false;
        }
        
        // Update ban in database
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "UPDATE " + tablePrefix + "bans " +
                           "SET active = 0, unbanned_by = ?, unbanned_time = ? " +
                           "WHERE uuid = ? AND active = 1";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, admin);
                stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                stmt.setString(3, target.getUniqueId().toString());
                
                int affected = stmt.executeUpdate();
                
                if (affected > 0) {
                    // Also update legacy data.yml for backwards compatibility
                    updateLegacyUnbanData(target, admin);
                    
                    // Broadcast unban message
                    String unbanMessage = "§a" + target.getName() + " §7wurde von §a" + admin + " §7entbannt.";
                    plugin.getAPI().getChatAPI().broadcast(unbanMessage);
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update ban in database: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Check if a player is banned
     * @param player The player to check
     * @return True if the player is banned
     */
    public boolean isBanned(OfflinePlayer player) {
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "SELECT id, expiration FROM " + tablePrefix + "bans " +
                           "WHERE uuid = ? AND active = 1 " +
                           "ORDER BY ban_time DESC LIMIT 1";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Check if ban has expired
                        java.sql.Timestamp expiration = rs.getTimestamp("expiration");
                        
                        if (expiration == null) {
                            return true; // Permanent ban
                        }
                        
                        if (System.currentTimeMillis() > expiration.getTime()) {
                            // Ban has expired, set to inactive
                            expireActiveBan(player);
                            return false;
                        }
                        
                        return true; // Player is still banned
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check ban status: " + e.getMessage());
            e.printStackTrace();
            
            // Fall back to legacy method if database fails
            return isLegacyBanned(player);
        }
        
        return false;
    }
    
    /**
     * Get the ban reason for a player
     * @param player The player to check
     * @return The ban reason, or null if the player is not banned
     */
    public String getBanReason(OfflinePlayer player) {
        if (!isBanned(player)) {
            return null;
        }
        
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "SELECT reason FROM " + tablePrefix + "bans " +
                           "WHERE uuid = ? AND active = 1 " +
                           "ORDER BY ban_time DESC LIMIT 1";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("reason");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get ban reason: " + e.getMessage());
            e.printStackTrace();
            
            // Fall back to legacy method if database fails
            return getLegacyBanReason(player);
        }
        
        return "No reason specified";
    }
    
    /**
     * Get the ban expiration time for a player
     * @param player The player to check
     * @return The ban expiration time in milliseconds, or -1 if permanent
     */
    public long getBanExpiration(OfflinePlayer player) {
        if (!isBanned(player)) {
            return 0;
        }
        
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "SELECT expiration FROM " + tablePrefix + "bans " +
                           "WHERE uuid = ? AND active = 1 " +
                           "ORDER BY ban_time DESC LIMIT 1";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        java.sql.Timestamp expiration = rs.getTimestamp("expiration");
                        return expiration != null ? expiration.getTime() : -1;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get ban expiration: " + e.getMessage());
            e.printStackTrace();
            
            // Fall back to legacy method if database fails
            return getLegacyBanExpiration(player);
        }
        
        return -1;
    }
    
    /**
     * Get the ban history for a player
     * @param player The player to check
     * @return A list of ban entries
     */
    public List<Map<String, Object>> getBanHistory(OfflinePlayer player) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "SELECT id, reason, admin, ban_time, expiration, active, " +
                           "unbanned_by, unbanned_time " +
                           "FROM " + tablePrefix + "bans " +
                           "WHERE uuid = ? " +
                           "ORDER BY ban_time DESC";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("id", rs.getInt("id"));
                        entry.put("reason", rs.getString("reason"));
                        entry.put("admin", rs.getString("admin"));
                        
                        java.sql.Timestamp banTime = rs.getTimestamp("ban_time");
                        entry.put("time", banTime != null ? banTime.getTime() : 0);
                        
                        java.sql.Timestamp expiration = rs.getTimestamp("expiration");
                        entry.put("until", expiration != null ? expiration.getTime() : -1);
                        
                        entry.put("active", rs.getBoolean("active"));
                        entry.put("expired", !rs.getBoolean("active") && expiration != null && 
                                           System.currentTimeMillis() > expiration.getTime());
                        
                        entry.put("unbanned_by", rs.getString("unbanned_by"));
                        
                        java.sql.Timestamp unbannedTime = rs.getTimestamp("unbanned_time");
                        entry.put("unbanned_time", unbannedTime != null ? unbannedTime.getTime() : 0);
                        
                        history.add(entry);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get ban history: " + e.getMessage());
            e.printStackTrace();
            
            // Fall back to legacy method
            return getLegacyBanHistory(player);
        }
        
        return history;
    }
    
    /**
     * Get the mute history for a player
     * @param player The player to check
     * @return A list of mute entries
     */
    public List<Map<String, Object>> getMuteHistory(OfflinePlayer player) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "SELECT id, reason, admin, mute_time, expiration, active, " +
                           "unmuted_by, unmuted_time " +
                           "FROM " + tablePrefix + "mutes " +
                           "WHERE uuid = ? " +
                           "ORDER BY mute_time DESC";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("id", rs.getInt("id"));
                        entry.put("reason", rs.getString("reason"));
                        entry.put("admin", rs.getString("admin"));
                        
                        java.sql.Timestamp muteTime = rs.getTimestamp("mute_time");
                        entry.put("time", muteTime != null ? muteTime.getTime() : 0);
                        
                        java.sql.Timestamp expiration = rs.getTimestamp("expiration");
                        entry.put("until", expiration != null ? expiration.getTime() : -1);
                        
                        entry.put("active", rs.getBoolean("active"));
                        entry.put("expired", !rs.getBoolean("active") && expiration != null && 
                                           System.currentTimeMillis() > expiration.getTime());
                        
                        entry.put("unmuted_by", rs.getString("unmuted_by"));
                        
                        java.sql.Timestamp unmutedTime = rs.getTimestamp("unmuted_time");
                        entry.put("unmuted_time", unmutedTime != null ? unmutedTime.getTime() : 0);
                        
                        history.add(entry);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get mute history: " + e.getMessage());
            e.printStackTrace();
        }
        
        return history;
    }
    
    /**
     * Mark an active ban as expired
     * @param player The player to update
     */
    private void expireActiveBan(OfflinePlayer player) {
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "UPDATE " + tablePrefix + "bans " +
                           "SET active = 0 " +
                           "WHERE uuid = ? AND active = 1 AND expiration IS NOT NULL AND expiration < ?";
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to expire ban: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ensure player exists in database
     * @param player The player to check
     */
    private void ensurePlayerInDatabase(OfflinePlayer player) {
        try {
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            String query = "INSERT OR IGNORE INTO " + tablePrefix + "players (uuid, name) VALUES (?, ?)";
            
            if (plugin.getConfigManager().getDbType().equalsIgnoreCase("mysql")) {
                query = "INSERT IGNORE INTO " + tablePrefix + "players (uuid, name) VALUES (?, ?)";
            }
            
            try (PreparedStatement stmt = plugin.getConfigManager().getDbConnection().prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, player.getName());
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to ensure player in database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Legacy compatibility methods
    
    /**
     * Update legacy data.yml for backwards compatibility when banning
     */
    private void updateLegacyBanData(OfflinePlayer target, String reason, String admin, long time, long expiration) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + target.getUniqueId();
        String banPath = path + ".ban";
        
        // Ensure player exists in data file
        if (!data.contains(path + ".name")) {
            data.set(path + ".name", target.getName());
        }
        
        // Set ban information
        data.set(banPath + ".active", true);
        data.set(banPath + ".reason", reason);
        data.set(banPath + ".admin", admin);
        data.set(banPath + ".time", time);
        data.set(banPath + ".until", expiration);
        
        // Save the data file
        plugin.getConfigManager().saveConfig("data.yml");
    }
    
    /**
     * Update legacy data.yml for backwards compatibility when unbanning
     */
    private void updateLegacyUnbanData(OfflinePlayer target, String admin) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + target.getUniqueId() + ".ban";
        
        if (data.contains(path + ".active")) {
            // Set ban to inactive
            data.set(path + ".active", false);
            data.set(path + ".unbanned_by", admin);
            data.set(path + ".unbanned_time", System.currentTimeMillis());
            
            // Save the data file
            plugin.getConfigManager().saveConfig("data.yml");
        }
    }
    
    /**
     * Legacy method to check if a player is banned
     */
    private boolean isLegacyBanned(OfflinePlayer player) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".ban";
        
        if (!data.contains(path + ".active") || !data.getBoolean(path + ".active")) {
            return false; // Player is not banned
        }
        
        // Check if ban has expired
        long until = data.getLong(path + ".until", -1);
        if (until == -1) {
            return true; // Permanent ban
        }
        
        if (System.currentTimeMillis() > until) {
            // Ban has expired, set to inactive
            data.set(path + ".active", false);
            data.set(path + ".expired", true);
            plugin.getConfigManager().saveConfig("data.yml");
            return false;
        }
        
        return true; // Player is still banned
    }
    
    /**
     * Legacy method to get a player's ban reason
     */
    private String getLegacyBanReason(OfflinePlayer player) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".ban";
        
        return data.getString(path + ".reason", "No reason specified");
    }
    
    /**
     * Legacy method to get a player's ban expiration
     */
    private long getLegacyBanExpiration(OfflinePlayer player) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".ban";
        
        return data.getLong(path + ".until", -1);
    }
    
    /**
     * Legacy method to get a player's ban history
     */
    private List<Map<String, Object>> getLegacyBanHistory(OfflinePlayer player) {
        List<Map<String, Object>> history = new ArrayList<>();
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".ban_history";
        
        if (!data.contains(path)) {
            return history;
        }
        
        ConfigurationSection historySection = data.getConfigurationSection(path);
        if (historySection == null) {
            return history;
        }
        
        for (String key : historySection.getKeys(false)) {
            ConfigurationSection entrySection = historySection.getConfigurationSection(key);
            if (entrySection != null) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("reason", entrySection.getString("reason", "No reason specified"));
                entry.put("admin", entrySection.getString("admin", "Unknown"));
                entry.put("time", entrySection.getLong("time", 0));
                entry.put("until", entrySection.getLong("until", -1));
                entry.put("active", entrySection.getBoolean("active", false));
                entry.put("expired", entrySection.getBoolean("expired", false));
                entry.put("unbanned_by", entrySection.getString("unbanned_by", null));
                entry.put("unbanned_time", entrySection.getLong("unbanned_time", 0));
                
                history.add(entry);
            }
        }
        
        return history;
    }
    
    /**
     * Format a ban message for a player
     * @param reason The reason for the ban
     * @param admin The admin who issued the ban
     * @param until The ban expiration time in milliseconds, or -1 for permanent
     * @return The formatted ban message
     */
    public String formatBanMessage(String reason, String admin, long until) {
        StringBuilder message = new StringBuilder();
        message.append("§c§lDu wurdest gebannt!\n\n");
        message.append("§7Grund: §f").append(reason).append("\n");
        message.append("§7Gebannt von: §f").append(admin).append("\n");
        
        if (until > 0) {
            Date date = new Date(until);
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            message.append("§7Gebannt bis: §f").append(format.format(date)).append("\n");
            
            long duration = until - System.currentTimeMillis();
            message.append("§7Dauer: §f").append(formatDuration(duration)).append("\n");
        } else {
            message.append("§7Gebannt bis: §4PERMANENT\n");
        }
        
        message.append("\n§7Bei Fragen wende dich an einen Administrator.");
        
        return message.toString();
    }
    
    /**
     * Format a duration in milliseconds to a human-readable string
     * @param duration The duration in milliseconds
     * @return The formatted duration
     */
    public String formatDuration(long duration) {
        if (duration < 0) {
            return "Permanent";
        }
        
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(" Tag").append(days == 1 ? "" : "e").append(" ");
        }
        
        if (hours % 24 > 0) {
            sb.append(hours % 24).append(" Stunde").append(hours % 24 == 1 ? "" : "n").append(" ");
        }
        
        if (minutes % 60 > 0) {
            sb.append(minutes % 60).append(" Minute").append(minutes % 60 == 1 ? "" : "n").append(" ");
        }
        
        if (seconds % 60 > 0 && days == 0 && hours == 0) {
            sb.append(seconds % 60).append(" Sekunde").append(seconds % 60 == 1 ? "" : "n");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Get the ban history for a player
     * @param player The player to check
     * @return A list of ban entries
     */
    public List<Map<String, Object>> getBanHistory(OfflinePlayer player) {
        List<Map<String, Object>> history = new ArrayList<>();
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".ban_history";
        
        if (!data.contains(path)) {
            return history;
        }
        
        ConfigurationSection historySection = data.getConfigurationSection(path);
        if (historySection == null) {
            return history;
        }
        
        for (String key : historySection.getKeys(false)) {
            ConfigurationSection entrySection = historySection.getConfigurationSection(key);
            if (entrySection != null) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("reason", entrySection.getString("reason", "No reason specified"));
                entry.put("admin", entrySection.getString("admin", "Unknown"));
                entry.put("time", entrySection.getLong("time", 0));
                entry.put("until", entrySection.getLong("until", -1));
                entry.put("active", entrySection.getBoolean("active", false));
                entry.put("expired", entrySection.getBoolean("expired", false));
                entry.put("unbanned_by", entrySection.getString("unbanned_by", null));
                entry.put("unbanned_time", entrySection.getLong("unbanned_time", 0));
                
                history.add(entry);
            }
        }
        
        return history;
    }
    
    /**
     * Process placeholders in a string
     * @param player The player to get data for
     * @param input The input string with placeholders
     * @return The processed string
     */
    public String processPlaceholders(Player player, String input) {
        if (input == null) return "";
        
        // Check if PlaceholderAPI is available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            // Process with PlaceholderAPI
            input = PlaceholderAPI.setPlaceholders(player, input);
        } else {
            // Fall back to our internal placeholder system
            
            // Player placeholders
            input = input.replace("%player%", player.getName());
            input = input.replace("%displayname%", player.getDisplayName());
            input = input.replace("%uuid%", player.getUniqueId().toString());
            
            // Stats placeholders
            input = input.replace("%kills%", String.valueOf(getKills(player)));
            input = input.replace("%deaths%", String.valueOf(getDeaths(player)));
            
            // Economy placeholders
            double balance = plugin.getAPI().getEconomyAPI().getBalance(player);
            String formatted = plugin.getAPI().getEconomyAPI().format(balance);
            String raw = String.format("%.2f", balance);
            input = input.replace("%balance%", formatted);
            input = input.replace("%balance_raw%", raw);
            input = input.replace("%currency%", plugin.getAPI().getEconomyAPI().getCurrencyName());
            input = input.replace("%currency_symbol%", plugin.getAPI().getEconomyAPI().getCurrencySymbol());
            
            // Server placeholders
            input = input.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
            input = input.replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        }
        
        return input;
    }
}