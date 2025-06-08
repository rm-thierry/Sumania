package ch.retaxo.sumania.api.teleport;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a teleport request between two players
 */
public class TeleportRequest {

    private final UUID fromUUID;
    private final UUID toUUID;
    private final TeleportRequestType type;
    private final long timestamp;
    
    /**
     * Constructor
     * @param fromUUID The UUID of the player requesting the teleport
     * @param toUUID The UUID of the player receiving the request
     * @param type The type of teleport request
     */
    public TeleportRequest(UUID fromUUID, UUID toUUID, TeleportRequestType type) {
        this.fromUUID = fromUUID;
        this.toUUID = toUUID;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get the UUID of the player requesting the teleport
     * @return The UUID
     */
    public UUID getFromUUID() {
        return fromUUID;
    }
    
    /**
     * Get the UUID of the player receiving the request
     * @return The UUID
     */
    public UUID getToUUID() {
        return toUUID;
    }
    
    /**
     * Get the type of teleport request
     * @return The type
     */
    public TeleportRequestType getType() {
        return type;
    }
    
    /**
     * Get the timestamp of when the request was created
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeleportRequest that = (TeleportRequest) o;
        return Objects.equals(fromUUID, that.fromUUID) &&
                Objects.equals(toUUID, that.toUUID) &&
                type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(fromUUID, toUUID, type);
    }
}
