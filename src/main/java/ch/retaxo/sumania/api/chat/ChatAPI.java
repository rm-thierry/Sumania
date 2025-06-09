package ch.retaxo.sumania.api.chat;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API for chat-related operations
 */
public class ChatAPI {

    private final Sumania plugin;
    private boolean chatMuted = false;
    private final Map<UUID, Long> chatCooldowns = new HashMap<>();
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public ChatAPI(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Format a chat message
     * @param player The player sending the message
     * @param message The message to format
     * @return The formatted message
     */
    public String formatMessage(Player player, String message) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if chat formatting is enabled
        if (!config.getBoolean("chat.format-enabled", true)) {
            return message;
        }
        
        String format = config.getString("chat.format", "§8[§6%rank%§8] §7%player% §8» §f%message%");
        
        // Replace basic placeholders
        format = format.replace("%player%", player.getName());
        format = format.replace("%message%", message);
        
        // Process other placeholders
        format = plugin.getAPI().getPlayerAPI().processPlaceholders(player, format);
        
        return format;
    }
    
    /**
     * Check if a player can chat
     * @param player The player
     * @return True if the player can chat, false otherwise with a message sent to the player
     */
    public boolean canChat(Player player) {
        // Check if chat is muted
        if (isChatMuted() && !player.hasPermission("sumania.chat.bypass.mute")) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "chat.chat-muted",
                    null
            );
            
            return false;
        }
        
        // Check if player is in cooldown
        if (isInCooldown(player) && !player.hasPermission("sumania.chat.bypass.cooldown")) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("seconds", String.valueOf(getCooldownTimeLeft(player)));
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "chat.chat-cooldown",
                    replacements
            );
            
            return false;
        }
        
        return true;
    }
    
    /**
     * Set a chat cooldown for a player
     * @param player The player
     */
    public void setCooldown(Player player) {
        if (player.hasPermission("sumania.chat.bypass.cooldown")) {
            return;
        }
        
        int cooldown = 3; // Default cooldown in seconds
        chatCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
    }
    
    /**
     * Check if a player is in chat cooldown
     * @param player The player
     * @return True if the player is in cooldown
     */
    private boolean isInCooldown(Player player) {
        if (!chatCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        
        return System.currentTimeMillis() < chatCooldowns.get(player.getUniqueId());
    }
    
    /**
     * Get the time left on a player's cooldown
     * @param player The player
     * @return The time left in seconds
     */
    private int getCooldownTimeLeft(Player player) {
        if (!chatCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        
        long timeLeft = chatCooldowns.get(player.getUniqueId()) - System.currentTimeMillis();
        return Math.max(0, (int) (timeLeft / 1000));
    }
    
    /**
     * Mute the chat
     * @param muted True to mute the chat, false to unmute
     */
    public void setChatMuted(boolean muted) {
        chatMuted = muted;
    }
    
    /**
     * Check if the chat is muted
     * @return True if the chat is muted
     */
    public boolean isChatMuted() {
        return chatMuted;
    }
    
    /**
     * Clear the chat for all players
     */
    public void clearChat() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < 100; i++) {
                player.sendMessage("");
            }
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "chat.chat-cleared",
                    null
            );
        }
    }
    
    /**
     * Broadcast a message to all players
     * @param message The message to broadcast
     */
    public void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }
    
    /**
     * Broadcast a message from the messages.yml file to all players
     * @param path The path to the message in messages.yml
     * @param replacements The replacements to make in the message
     */
    public void broadcastMessage(String path, Map<String, String> replacements) {
        // Get the prefix from the config
        String prefix = plugin.getConfigManager().getPrefix();
        
        // Add prefix to the message
        String message = prefix + plugin.getAPI().getPlayerAPI().getMessage(path, replacements);
        broadcast(message);
    }
}
