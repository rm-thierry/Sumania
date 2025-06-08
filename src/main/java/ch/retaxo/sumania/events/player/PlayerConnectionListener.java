package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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
