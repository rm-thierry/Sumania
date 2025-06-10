package ch.retaxo.sumania.models;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Represents an auction in the auction house
 */
public class Auction {

    // Auction status
    public enum Status {
        ACTIVE,
        SOLD,
        EXPIRED,
        CANCELLED
    }

    private final int id;
    private final UUID sellerUuid;
    private UUID buyerUuid;
    private final ItemStack item;
    private final double price;
    private final Instant createdTime;
    private final Instant endTime;
    private Status status;
    private final String category;

    /**
     * Constructor for a new auction
     * @param id The auction ID
     * @param sellerUuid The seller's UUID
     * @param item The item being auctioned
     * @param price The price of the auction
     * @param createdTime The time the auction was created
     * @param endTime The time the auction ends
     * @param status The auction status
     * @param category The auction category
     */
    public Auction(int id, UUID sellerUuid, ItemStack item, double price, 
                  Instant createdTime, Instant endTime, Status status, String category) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.item = item;
        this.price = price;
        this.createdTime = createdTime;
        this.endTime = endTime;
        this.status = status;
        this.category = category;
    }
    
    /**
     * Constructor for a completed auction
     * @param id The auction ID
     * @param sellerUuid The seller's UUID
     * @param buyerUuid The buyer's UUID
     * @param item The item being auctioned
     * @param price The price of the auction
     * @param createdTime The time the auction was created
     * @param endTime The time the auction ends
     * @param status The auction status
     * @param category The auction category
     */
    public Auction(int id, UUID sellerUuid, UUID buyerUuid, ItemStack item, double price, 
                  Instant createdTime, Instant endTime, Status status, String category) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.buyerUuid = buyerUuid;
        this.item = item;
        this.price = price;
        this.createdTime = createdTime;
        this.endTime = endTime;
        this.status = status;
        this.category = category;
    }

    /**
     * Get the auction ID
     * @return The auction ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the seller's UUID
     * @return The seller's UUID
     */
    public UUID getSellerUuid() {
        return sellerUuid;
    }
    
    /**
     * Get the seller as a Player
     * @return The seller as a Player, or null if offline
     */
    public Player getSeller() {
        return Bukkit.getPlayer(sellerUuid);
    }
    
    /**
     * Get the seller's name
     * @return The seller's name
     */
    public String getSellerName() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(sellerUuid);
        return player.getName() != null ? player.getName() : "Unknown";
    }

    /**
     * Get the buyer's UUID
     * @return The buyer's UUID
     */
    public UUID getBuyerUuid() {
        return buyerUuid;
    }
    
    /**
     * Get the buyer as a Player
     * @return The buyer as a Player, or null if offline or not sold
     */
    public Player getBuyer() {
        return buyerUuid != null ? Bukkit.getPlayer(buyerUuid) : null;
    }
    
    /**
     * Get the buyer's name
     * @return The buyer's name, or null if not sold
     */
    public String getBuyerName() {
        if (buyerUuid == null) return null;
        OfflinePlayer player = Bukkit.getOfflinePlayer(buyerUuid);
        return player.getName() != null ? player.getName() : "Unknown";
    }
    
    /**
     * Set the buyer's UUID
     * @param buyerUuid The buyer's UUID
     */
    public void setBuyerUuid(UUID buyerUuid) {
        this.buyerUuid = buyerUuid;
    }

    /**
     * Get the item being auctioned
     * @return The item being auctioned
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Get the price of the auction
     * @return The price of the auction
     */
    public double getPrice() {
        return price;
    }

    /**
     * Get the time the auction was created
     * @return The time the auction was created
     */
    public Instant getCreatedTime() {
        return createdTime;
    }

    /**
     * Get the time the auction ends
     * @return The time the auction ends
     */
    public Instant getEndTime() {
        return endTime;
    }
    
    /**
     * Get the auction status
     * @return The auction status
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Set the auction status
     * @param status The new auction status
     */
    public void setStatus(Status status) {
        this.status = status;
    }
    
    /**
     * Get the auction category
     * @return The auction category
     */
    public String getCategory() {
        return category;
    }
    
    /**
     * Check if the auction is active
     * @return True if the auction is active
     */
    public boolean isActive() {
        return status == Status.ACTIVE;
    }
    
    /**
     * Check if the auction is expired
     * @return True if the auction is expired
     */
    public boolean isExpired() {
        return status == Status.EXPIRED || (status == Status.ACTIVE && Instant.now().isAfter(endTime));
    }
    
    /**
     * Get the remaining time for this auction
     * @return The remaining time in milliseconds, or 0 if expired
     */
    public long getRemainingTime() {
        if (!isActive()) return 0;
        
        long remaining = Instant.now().until(endTime, ChronoUnit.MILLIS);
        return Math.max(0, remaining);
    }
    
    /**
     * Get the formatted remaining time for this auction
     * @return The formatted remaining time
     */
    public String getFormattedRemainingTime() {
        if (!isActive()) {
            return "Abgelaufen";
        }
        
        long remainingMillis = getRemainingTime();
        if (remainingMillis <= 0) {
            return "Abgelaufen";
        }
        
        long seconds = remainingMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        hours = hours % 24;
        minutes = minutes % 60;
        seconds = seconds % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Get the formatted creation date
     * @return The formatted creation date
     */
    public String getFormattedCreationDate() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(createdTime, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return dateTime.format(formatter);
    }
    
    /**
     * Serialize an ItemStack to a Base64 string
     * @param item The ItemStack to serialize
     * @return The serialized ItemStack as a Base64 string
     * @throws IOException If an error occurs during serialization
     */
    public static String serializeItemStack(ItemStack item) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            
            dataOutput.writeObject(item);
            return Base64Coder.encodeLines(outputStream.toByteArray());
        }
    }
    
    /**
     * Deserialize an ItemStack from a Base64 string
     * @param data The Base64 string to deserialize
     * @return The deserialized ItemStack
     * @throws IOException If an error occurs during deserialization
     * @throws ClassNotFoundException If the class of the serialized object cannot be found
     */
    public static ItemStack deserializeItemStack(String data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            
            return (ItemStack) dataInput.readObject();
        }
    }
}