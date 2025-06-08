package ch.retaxo.sumania.api;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.api.economy.EconomyAPI;
import ch.retaxo.sumania.api.player.PlayerAPI;
import ch.retaxo.sumania.api.teleport.TeleportAPI;
import ch.retaxo.sumania.api.claim.ClaimAPI;
import ch.retaxo.sumania.api.chat.ChatAPI;

/**
 * The main API class for Sumania
 */
public class SumaniaAPI {

    private final Sumania plugin;
    private final EconomyAPI economyAPI;
    private final PlayerAPI playerAPI;
    private final TeleportAPI teleportAPI;
    private final ClaimAPI claimAPI;
    private final ChatAPI chatAPI;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public SumaniaAPI(Sumania plugin) {
        this.plugin = plugin;
        this.economyAPI = new EconomyAPI(plugin);
        this.playerAPI = new PlayerAPI(plugin);
        this.teleportAPI = new TeleportAPI(plugin);
        this.claimAPI = new ClaimAPI(plugin);
        this.chatAPI = new ChatAPI(plugin);
    }
    
    /**
     * Get the economy API
     * @return The economy API
     */
    public EconomyAPI getEconomyAPI() {
        return economyAPI;
    }
    
    /**
     * Get the player API
     * @return The player API
     */
    public PlayerAPI getPlayerAPI() {
        return playerAPI;
    }
    
    /**
     * Get the teleport API
     * @return The teleport API
     */
    public TeleportAPI getTeleportAPI() {
        return teleportAPI;
    }
    
    /**
     * Get the claim API
     * @return The claim API
     */
    public ClaimAPI getClaimAPI() {
        return claimAPI;
    }
    
    /**
     * Get the chat API
     * @return The chat API
     */
    public ChatAPI getChatAPI() {
        return chatAPI;
    }
}
