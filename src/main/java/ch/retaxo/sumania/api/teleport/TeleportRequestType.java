package ch.retaxo.sumania.api.teleport;

/**
 * Represents the type of teleport request
 */
public enum TeleportRequestType {
    /**
     * Teleport to the other player
     */
    TO_PLAYER,
    
    /**
     * Teleport the other player to you
     */
    FROM_PLAYER
}
