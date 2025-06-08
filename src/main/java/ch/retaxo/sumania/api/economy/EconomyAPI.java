package ch.retaxo.sumania.api.economy;

import ch.retaxo.sumania.Sumania;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

/**
 * API for economy-related operations
 */
public class EconomyAPI {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public EconomyAPI(Sumania plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get the balance of a player
     * @param player The player
     * @return The player's balance
     */
    public double getBalance(OfflinePlayer player) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId() + ".balance";
        
        if (data.contains(path)) {
            return data.getDouble(path);
        } else {
            // If the player doesn't have a balance yet, return the starting balance
            double startingBalance = plugin.getConfigManager().getConfig("config.yml")
                    .getDouble("economy.starting-balance", 1000.0);
            
            // Set the starting balance for the player
            setBalance(player, startingBalance);
            
            return startingBalance;
        }
    }
    
    /**
     * Set the balance of a player
     * @param player The player
     * @param amount The amount to set
     */
    public void setBalance(OfflinePlayer player, double amount) {
        FileConfiguration data = plugin.getConfigManager().getConfig("data.yml");
        String path = "players." + player.getUniqueId();
        
        // Ensure player exists in data file
        if (!data.contains(path + ".name")) {
            data.set(path + ".name", player.getName());
        }
        
        // Set the balance
        data.set(path + ".balance", amount);
        
        // Save the data file
        plugin.getConfigManager().saveConfig("data.yml");
    }
    
    /**
     * Add money to a player's balance
     * @param player The player
     * @param amount The amount to add
     * @return The new balance
     */
    public double deposit(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        double newBalance = balance + amount;
        
        setBalance(player, newBalance);
        
        return newBalance;
    }
    
    /**
     * Remove money from a player's balance
     * @param player The player
     * @param amount The amount to remove
     * @return The new balance
     */
    public double withdraw(OfflinePlayer player, double amount) {
        double balance = getBalance(player);
        double newBalance = Math.max(0, balance - amount);
        
        setBalance(player, newBalance);
        
        return newBalance;
    }
    
    /**
     * Check if a player has enough money
     * @param player The player
     * @param amount The amount to check
     * @return True if the player has enough money
     */
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }
    
    /**
     * Transfer money from one player to another
     * @param from The player to take money from
     * @param to The player to give money to
     * @param amount The amount to transfer
     * @return True if the transfer was successful
     */
    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        if (!has(from, amount)) {
            return false;
        }
        
        withdraw(from, amount);
        deposit(to, amount);
        
        return true;
    }
    
    /**
     * Get the currency name
     * @return The currency name
     */
    public String getCurrencyName() {
        return plugin.getConfigManager().getConfig("config.yml")
                .getString("economy.currency-name", "Coins");
    }
    
    /**
     * Get the currency symbol
     * @return The currency symbol
     */
    public String getCurrencySymbol() {
        return plugin.getConfigManager().getConfig("config.yml")
                .getString("economy.currency-symbol", "$");
    }
    
    /**
     * Format a money amount with the currency symbol
     * @param amount The amount to format
     * @return The formatted amount
     */
    public String format(double amount) {
        return getCurrencySymbol() + String.format("%.2f", amount);
    }
}
