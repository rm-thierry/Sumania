package ch.retaxo.sumania.events.player;

import ch.retaxo.sumania.Sumania;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChatEvent;

/**
 * Listener for player chat events
 */
public class PlayerChatListener implements Listener {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public PlayerChatListener(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player chat event
     * @param event The event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player can chat
        if (!plugin.getAPI().getChatAPI().canChat(player)) {
            event.setCancelled(true);
            return;
        }
        
        // Set chat cooldown
        plugin.getAPI().getChatAPI().setCooldown(player);
    }
}
