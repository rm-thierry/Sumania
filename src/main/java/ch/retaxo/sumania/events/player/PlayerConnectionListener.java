package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player connection events
 */
public class PlayerConnectionListener implements Listener {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public PlayerConnectionListener(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player login event (before they join the server)
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is banned
        if (plugin.getAPI().getPlayerAPI().isBanned(player)) {
            String reason = plugin.getAPI().getPlayerAPI().getBanReason(player);
            long expiration = plugin.getAPI().getPlayerAPI().getBanExpiration(player);
            
            // Get admin who banned the player
            String banPath = "players." + player.getUniqueId() + ".ban";
            String admin = plugin.getConfigManager().getConfig("data.yml")
                    .getString(banPath + ".admin", "Unknown");
            
            // Format ban message
            String banMessage = plugin.getAPI().getPlayerAPI().formatBanMessage(reason, admin, expiration);
            
            // Disallow login
            event.disallow(Result.KICK_BANNED, banMessage);
        }
    }
    
    /**
     * Handle player join event
     * @param event The event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Set custom join message if enabled
        if (plugin.getConfigManager().getConfig("config.yml").getBoolean("chat.format-enabled", true)) {
            event.setJoinMessage(null);
            plugin.getAPI().getChatAPI().broadcast("§8[§a+§8] §7" + player.getName() + " joined the server.");
        }
        
        // Initialize player data if not exists
        if (!player.hasPlayedBefore()) {
            // Set initial balance
            double startingBalance = plugin.getConfigManager().getConfig("config.yml")
                    .getDouble("economy.starting-balance", 1000.0);
            
            plugin.getAPI().getEconomyAPI().setBalance(player, startingBalance);
        }
    }
    
    /**
     * Handle player quit event
     * @param event The event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Set custom quit message if enabled
        if (plugin.getConfigManager().getConfig("config.yml").getBoolean("chat.format-enabled", true)) {
            event.setQuitMessage(null);
            plugin.getAPI().getChatAPI().broadcast("§8[§c-§8] §7" + player.getName() + " left the server.");
        }
    }
}
