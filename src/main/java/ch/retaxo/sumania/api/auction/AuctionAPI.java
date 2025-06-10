package ch.retaxo.sumania.api.auction;

import ch.retaxo.sumania.Sumania;
import ch.retaxo.sumania.models.Auction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API for auction-related operations
 */
public class AuctionAPI {

    private final Sumania plugin;
    private final Connection connection;
    private final String tablePrefix;
    private final Map<Integer, Auction> cachedAuctions;
    private BukkitTask cleanupTask;
    
    // Category names and their default icons
    private final Map<String, Material> categories;

    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public AuctionAPI(Sumania plugin) {
        this.plugin = plugin;
        this.connection = plugin.getConfigManager().getDbConnection();
        this.tablePrefix = plugin.getConfigManager().getTablePrefix();
        this.cachedAuctions = new ConcurrentHashMap<>();
        this.categories = new HashMap<>();
        
        // Initialize default categories
        initializeCategories();
        
        // Start cleanup task for expired auctions
        startCleanupTask();
    }
    
    /**
     * Initialize default categories
     */
    private void initializeCategories() {
        categories.put("blocks", Material.GRASS_BLOCK);
        categories.put("tools", Material.DIAMOND_PICKAXE);
        categories.put("weapons", Material.DIAMOND_SWORD);
        categories.put("armor", Material.DIAMOND_CHESTPLATE);
        categories.put("food", Material.COOKED_BEEF);
        categories.put("brewing", Material.BREWING_STAND);
        categories.put("redstone", Material.REDSTONE);
        categories.put("decoration", Material.FLOWER_POT);
        categories.put("misc", Material.COMPASS);
    }
    
    /**
     * Start the cleanup task for expired auctions
     */
    private void startCleanupTask() {
        // Cancel existing task if it exists
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        
        // Run cleanup task every 15 minutes (20 ticks * 60 seconds * 15 minutes)
        cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkExpiredAuctions, 20L * 60L, 20L * 60L * 15L);
    }
    
    /**
     * Check for expired auctions and update their status
     */
    private void checkExpiredAuctions() {
        try {
            // Get current time
            Timestamp now = Timestamp.from(Instant.now());
            
            // Update status of expired auctions
            String sql = "UPDATE " + tablePrefix + "auctions SET status = ? WHERE end_time < ? AND status = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Auction.Status.EXPIRED.name());
            statement.setTimestamp(2, now);
            statement.setString(3, Auction.Status.ACTIVE.name());
            int updated = statement.executeUpdate();
            
            if (updated > 0) {
                plugin.getLogger().info("Updated " + updated + " expired auctions");
                // Clear cache as expired auctions have been updated
                cachedAuctions.clear();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking expired auctions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create a new auction
     * @param seller The seller
     * @param item The item to auction
     * @param price The price of the auction
     * @param durationHours The duration of the auction in hours
     * @param category The category of the auction (can be null)
     * @return The created auction ID, or -1 if failed
     */
    public int createAuction(Player seller, ItemStack item, double price, int durationHours, String category) {
        if (item == null || item.getType() == Material.AIR) {
            return -1;
        }
        
        // Get auction configuration
        double minPrice = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.min-price", 10.0);
        double maxPrice = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.max-price", 1000000.0);
        int minDuration = plugin.getConfigManager().getConfig("config.yml").getInt("auction.min-duration", 1);
        int maxDuration = plugin.getConfigManager().getConfig("config.yml").getInt("auction.max-duration", 72);
        
        // Validate inputs
        if (price < minPrice || price > maxPrice) {
            return -1;
        }
        
        if (durationHours < minDuration || durationHours > maxDuration) {
            return -1;
        }
        
        // Check if player has too many active auctions
        int maxAuctions = plugin.getConfigManager().getConfig("config.yml").getInt("auction.max-active-auctions-per-player", 10);
        if (getActiveAuctionCount(seller.getUniqueId()) >= maxAuctions) {
            return -1;
        }
        
        try {
            // Serialize item
            String serializedItem = Auction.serializeItemStack(item);
            
            // Calculate end time
            Instant endTime = Instant.now().plus(durationHours, ChronoUnit.HOURS);
            
            // Create auction in database
            String sql = "INSERT INTO " + tablePrefix + "auctions (seller_uuid, item_data, price, end_time, status, category) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, seller.getUniqueId().toString());
            statement.setString(2, serializedItem);
            statement.setDouble(3, price);
            statement.setTimestamp(4, Timestamp.from(endTime));
            statement.setString(5, Auction.Status.ACTIVE.name());
            statement.setString(6, category);
            statement.executeUpdate();
            
            // Get the generated auction ID
            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int auctionId = generatedKeys.getInt(1);
                
                // Take the item from the player
                seller.getInventory().removeItem(item);
                
                // Calculate and charge listing fee
                double listingFeePercent = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.listing-fee-percent", 5.0);
                double minListingFee = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.min-listing-fee", 10.0);
                double maxListingFee = plugin.getConfigManager().getConfig("config.yml").getDouble("auction.max-listing-fee", 1000.0);
                
                double listingFee = price * (listingFeePercent / 100.0);
                listingFee = Math.max(minListingFee, Math.min(maxListingFee, listingFee));
                
                if (listingFee > 0) {
                    // Only charge if economy is enabled
                    if (plugin.getConfigManager().getConfig("config.yml").getBoolean("economy.enabled", true)) {
                        plugin.getAPI().getEconomyAPI().withdraw(seller, listingFee);
                    }
                }
                
                return auctionId;
            }
        } catch (SQLException | IOException e) {
            plugin.getLogger().severe("Error creating auction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Get the number of active auctions for a player
     * @param playerUuid The player's UUID
     * @return The number of active auctions
     */
    public int getActiveAuctionCount(UUID playerUuid) {
        try {
            String sql = "SELECT COUNT(*) FROM " + tablePrefix + "auctions WHERE seller_uuid = ? AND status = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, playerUuid.toString());
            statement.setString(2, Auction.Status.ACTIVE.name());
            ResultSet result = statement.executeQuery();
            
            if (result.next()) {
                return result.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting active auction count: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Get an auction by its ID
     * @param auctionId The auction ID
     * @return The auction, or null if not found
     */
    public Auction getAuction(int auctionId) {
        // Check cache first
        if (cachedAuctions.containsKey(auctionId)) {
            return cachedAuctions.get(auctionId);
        }
        
        try {
            String sql = "SELECT * FROM " + tablePrefix + "auctions WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, auctionId);
            ResultSet result = statement.executeQuery();
            
            if (result.next()) {
                Auction auction = createAuctionFromResultSet(result);
                
                // Cache the auction
                cachedAuctions.put(auctionId, auction);
                
                return auction;
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Error getting auction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get all active auctions
     * @return A list of all active auctions
     */
    public List<Auction> getActiveAuctions() {
        List<Auction> auctions = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM " + tablePrefix + "auctions WHERE status = ? ORDER BY end_time ASC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Auction.Status.ACTIVE.name());
            ResultSet result = statement.executeQuery();
            
            while (result.next()) {
                Auction auction = createAuctionFromResultSet(result);
                auctions.add(auction);
                
                // Update cache
                cachedAuctions.put(auction.getId(), auction);
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Error getting active auctions: " + e.getMessage());
            e.printStackTrace();
        }
        
        return auctions;
    }
    
    /**
     * Get active auctions by category
     * @param category The category
     * @return A list of active auctions in the category
     */
    public List<Auction> getAuctionsByCategory(String category) {
        List<Auction> auctions = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM " + tablePrefix + "auctions WHERE status = ? AND category = ? ORDER BY end_time ASC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Auction.Status.ACTIVE.name());
            statement.setString(2, category);
            ResultSet result = statement.executeQuery();
            
            while (result.next()) {
                Auction auction = createAuctionFromResultSet(result);
                auctions.add(auction);
                
                // Update cache
                cachedAuctions.put(auction.getId(), auction);
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Error getting auctions by category: " + e.getMessage());
            e.printStackTrace();
        }
        
        return auctions;
    }
    
    /**
     * Get auctions by seller
     * @param sellerUuid The seller's UUID
     * @return A list of auctions by the seller
     */
    public List<Auction> getAuctionsBySeller(UUID sellerUuid) {
        List<Auction> auctions = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM " + tablePrefix + "auctions WHERE seller_uuid = ? ORDER BY status, end_time ASC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, sellerUuid.toString());
            ResultSet result = statement.executeQuery();
            
            while (result.next()) {
                Auction auction = createAuctionFromResultSet(result);
                auctions.add(auction);
                
                // Update cache
                cachedAuctions.put(auction.getId(), auction);
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Error getting auctions by seller: " + e.getMessage());
            e.printStackTrace();
        }
        
        return auctions;
    }
    
    /**
     * Get auctions purchased by a buyer
     * @param buyerUuid The buyer's UUID
     * @return A list of auctions purchased by the buyer
     */
    public List<Auction> getAuctionsByBuyer(UUID buyerUuid) {
        List<Auction> auctions = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM " + tablePrefix + "auctions WHERE buyer_uuid = ? AND status = ? ORDER BY end_time DESC";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, buyerUuid.toString());
            statement.setString(2, Auction.Status.SOLD.name());
            ResultSet result = statement.executeQuery();
            
            while (result.next()) {
                Auction auction = createAuctionFromResultSet(result);
                auctions.add(auction);
                
                // Update cache
                cachedAuctions.put(auction.getId(), auction);
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Error getting auctions by buyer: " + e.getMessage());
            e.printStackTrace();
        }
        
        return auctions;
    }
    
    /**
     * Purchase an auction
     * @param auction The auction to purchase
     * @param buyer The buyer
     * @return True if the purchase was successful
     */
    public boolean purchaseAuction(Auction auction, Player buyer) {
        if (auction == null || !auction.isActive()) {
            return false;
        }
        
        // Check if player has enough money
        if (!plugin.getAPI().getEconomyAPI().has(buyer, auction.getPrice())) {
            return false;
        }
        
        // Check if buyer is the seller
        if (auction.getSellerUuid().equals(buyer.getUniqueId())) {
            return false;
        }
        
        // Check if inventory has space
        if (buyer.getInventory().firstEmpty() == -1) {
            return false;
        }
        
        try {
            // Update auction in database
            String sql = "UPDATE " + tablePrefix + "auctions SET status = ?, buyer_uuid = ? WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Auction.Status.SOLD.name());
            statement.setString(2, buyer.getUniqueId().toString());
            statement.setInt(3, auction.getId());
            int updated = statement.executeUpdate();
            
            if (updated > 0) {
                // Transfer money
                plugin.getAPI().getEconomyAPI().withdraw(buyer, auction.getPrice());
                
                // Pay the seller if they're online or in the database
                OfflinePlayer seller = Bukkit.getOfflinePlayer(auction.getSellerUuid());
                plugin.getAPI().getEconomyAPI().deposit(seller, auction.getPrice());
                
                // Give item to buyer
                buyer.getInventory().addItem(auction.getItem());
                
                // Update auction object
                auction.setStatus(Auction.Status.SOLD);
                auction.setBuyerUuid(buyer.getUniqueId());
                
                // Update cache
                cachedAuctions.put(auction.getId(), auction);
                
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error purchasing auction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Cancel an auction
     * @param auction The auction to cancel
     * @param player The player trying to cancel (must be the seller)
     * @return True if the cancellation was successful
     */
    public boolean cancelAuction(Auction auction, Player player) {
        if (auction == null || !auction.isActive()) {
            return false;
        }
        
        // Check if player is the seller
        if (!auction.getSellerUuid().equals(player.getUniqueId())) {
            return false;
        }
        
        // Check if inventory has space
        if (player.getInventory().firstEmpty() == -1) {
            return false;
        }
        
        try {
            // Update auction in database
            String sql = "UPDATE " + tablePrefix + "auctions SET status = ? WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Auction.Status.CANCELLED.name());
            statement.setInt(2, auction.getId());
            int updated = statement.executeUpdate();
            
            if (updated > 0) {
                // Return item to seller
                player.getInventory().addItem(auction.getItem());
                
                // Update auction object
                auction.setStatus(Auction.Status.CANCELLED);
                
                // Update cache
                cachedAuctions.put(auction.getId(), auction);
                
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cancelling auction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Claim an expired auction's item
     * @param auction The expired auction
     * @param player The player trying to claim (must be the seller)
     * @return True if the claim was successful
     */
    public boolean claimExpiredAuction(Auction auction, Player player) {
        if (auction == null || auction.getStatus() != Auction.Status.EXPIRED) {
            return false;
        }
        
        // Check if player is the seller
        if (!auction.getSellerUuid().equals(player.getUniqueId())) {
            return false;
        }
        
        // Check if inventory has space
        if (player.getInventory().firstEmpty() == -1) {
            return false;
        }
        
        try {
            // Update auction in database to mark it as claimed
            String sql = "UPDATE " + tablePrefix + "auctions SET status = ? WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Auction.Status.CANCELLED.name());
            statement.setInt(2, auction.getId());
            int updated = statement.executeUpdate();
            
            if (updated > 0) {
                // Return item to seller
                player.getInventory().addItem(auction.getItem());
                
                // Update auction object
                auction.setStatus(Auction.Status.CANCELLED);
                
                // Update cache
                cachedAuctions.put(auction.getId(), auction);
                
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error claiming expired auction: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Get all available categories
     * @return A map of category names to their icons
     */
    public Map<String, Material> getCategories() {
        return categories;
    }
    
    /**
     * Create an Auction object from a ResultSet
     * @param result The ResultSet containing auction data
     * @return The created Auction
     * @throws SQLException If an error occurs reading from the ResultSet
     * @throws IOException If an error occurs deserializing the item
     * @throws ClassNotFoundException If an error occurs deserializing the item
     */
    private Auction createAuctionFromResultSet(ResultSet result) throws SQLException, IOException, ClassNotFoundException {
        int id = result.getInt("id");
        UUID sellerUuid = UUID.fromString(result.getString("seller_uuid"));
        
        String buyerUuidStr = result.getString("buyer_uuid");
        UUID buyerUuid = buyerUuidStr != null ? UUID.fromString(buyerUuidStr) : null;
        
        String itemData = result.getString("item_data");
        ItemStack item = Auction.deserializeItemStack(itemData);
        
        double price = result.getDouble("price");
        Instant createdTime = result.getTimestamp("created_time").toInstant();
        Instant endTime = result.getTimestamp("end_time").toInstant();
        Auction.Status status = Auction.Status.valueOf(result.getString("status"));
        String category = result.getString("category");
        
        if (buyerUuid != null) {
            return new Auction(id, sellerUuid, buyerUuid, item, price, createdTime, endTime, status, category);
        } else {
            return new Auction(id, sellerUuid, item, price, createdTime, endTime, status, category);
        }
    }
    
    /**
     * Clean up expired auctions older than the configured retention period
     */
    public void cleanupOldAuctions() {
        int expirationDays = plugin.getConfigManager().getConfig("config.yml").getInt("auction.expired-auctions-days", 7);
        Instant cutoffDate = Instant.now().minus(expirationDays, ChronoUnit.DAYS);
        
        try {
            String sql = "DELETE FROM " + tablePrefix + "auctions WHERE status IN (?, ?) AND end_time < ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, Auction.Status.EXPIRED.name());
            statement.setString(2, Auction.Status.CANCELLED.name());
            statement.setTimestamp(3, Timestamp.from(cutoffDate));
            int deleted = statement.executeUpdate();
            
            if (deleted > 0) {
                plugin.getLogger().info("Deleted " + deleted + " old auctions");
                
                // Clear cache as auctions have been deleted
                cachedAuctions.clear();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cleaning up old auctions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Clear the auction cache
     */
    public void clearCache() {
        cachedAuctions.clear();
    }
    
    /**
     * Shutdown the API
     */
    public void shutdown() {
        // Cancel cleanup task
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        
        // Clear cache
        clearCache();
    }
}