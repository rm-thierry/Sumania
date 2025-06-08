package ch.retaxo.sumania.commands.rewards;

import ch.retaxo.sumania.Sumania;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the rewards menu
 */
public class RewardsMenuHandler implements Listener {

    private final Sumania plugin;
    private final RewardsCommand rewardsCommand;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     * @param rewardsCommand The rewards command instance
     */
    public RewardsMenuHandler(Sumania plugin, RewardsCommand rewardsCommand) {
        this.plugin = plugin;
        this.rewardsCommand = rewardsCommand;
        
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the rewards menu for a player
     * @param player The player to open the menu for
     */
    public void openRewardsMenu(Player player) {
        // Create inventory
        Inventory menu = Bukkit.createInventory(null, 3 * 9, "§8[§6Belohnungen§8]");
        
        // Initialize with glass panes
        ItemStack glassPane = createDecorativeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < menu.getSize(); i++) {
            menu.setItem(i, glassPane);
        }
        
        // Try to get player rewards info from database
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Get player rewards info
            String query = "SELECT * FROM " + tablePrefix + "rewards WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    LocalDateTime now = LocalDateTime.now();
                    
                    // Daily reward
                    LocalDateTime dailyClaim = null;
                    if (rs.next() && rs.getTimestamp("daily_claim") != null) {
                        dailyClaim = rs.getTimestamp("daily_claim").toLocalDateTime();
                    }
                    
                    // Check if daily reward is available
                    boolean dailyAvailable = dailyClaim == null || 
                            dailyClaim.until(now, ChronoUnit.HOURS) >= 24;
                    
                    // Weekly reward
                    LocalDateTime weeklyClaim = null;
                    if (rs.getTimestamp("weekly_claim") != null) {
                        weeklyClaim = rs.getTimestamp("weekly_claim").toLocalDateTime();
                    }
                    
                    // Check if weekly reward is available
                    boolean weeklyAvailable = weeklyClaim == null || 
                            weeklyClaim.until(now, ChronoUnit.DAYS) >= 7;
                    
                    // Monthly reward
                    LocalDateTime monthlyClaim = null;
                    if (rs.getTimestamp("monthly_claim") != null) {
                        monthlyClaim = rs.getTimestamp("monthly_claim").toLocalDateTime();
                    }
                    
                    // Check if monthly reward is available
                    boolean monthlyAvailable = monthlyClaim == null || 
                            monthlyClaim.until(now, ChronoUnit.DAYS) >= 30;
                    
                    // Streak
                    int streakDays = rs.getInt("streak_days");
                    
                    // Add reward items
                    menu.setItem(10, createDailyRewardItem(dailyAvailable, dailyClaim));
                    menu.setItem(13, createWeeklyRewardItem(weeklyAvailable, weeklyClaim));
                    menu.setItem(16, createMonthlyRewardItem(monthlyAvailable, monthlyClaim));
                    
                    // Add streak info
                    menu.setItem(22, createStreakInfoItem(streakDays));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Abrufen von Belohnungsinformationen: " + e.getMessage());
            e.printStackTrace();
            
            // Add default items if database error occurs
            menu.setItem(10, createDailyRewardItem(false, null));
            menu.setItem(13, createWeeklyRewardItem(false, null));
            menu.setItem(16, createMonthlyRewardItem(false, null));
            menu.setItem(22, createStreakInfoItem(0));
        }
        
        // Open menu
        player.openInventory(menu);
    }
    
    /**
     * Create a daily reward item
     * @param available Whether the reward is available
     * @param lastClaim The last claim time
     * @return The item
     */
    private ItemStack createDailyRewardItem(boolean available, LocalDateTime lastClaim) {
        Material material = available ? Material.GOLD_INGOT : Material.IRON_INGOT;
        String displayName = available ? "§a§lTägliche Belohnung" : "§c§lTägliche Belohnung (nicht verfügbar)";
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        if (available) {
            lore.add("§7Klicke, um deine tägliche Belohnung abzuholen!");
            lore.add("");
            lore.add("§7Belohnung: §e" + getRewardAmount("daily") + " " + getCurrencySymbol());
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeUntilAvailable = Duration.between(now, lastClaim.plus(1, ChronoUnit.DAYS));
            
            long hours = timeUntilAvailable.toHours();
            long minutes = timeUntilAvailable.toMinutes() % 60;
            
            lore.add("§cDu hast deine tägliche Belohnung bereits abgeholt.");
            lore.add("");
            lore.add("§7Verfügbar in: §e" + hours + "h " + minutes + "m");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a weekly reward item
     * @param available Whether the reward is available
     * @param lastClaim The last claim time
     * @return The item
     */
    private ItemStack createWeeklyRewardItem(boolean available, LocalDateTime lastClaim) {
        Material material = available ? Material.DIAMOND : Material.COAL;
        String displayName = available ? "§a§lWöchentliche Belohnung" : "§c§lWöchentliche Belohnung (nicht verfügbar)";
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        if (available) {
            lore.add("§7Klicke, um deine wöchentliche Belohnung abzuholen!");
            lore.add("");
            lore.add("§7Belohnung: §e" + getRewardAmount("weekly") + " " + getCurrencySymbol());
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeUntilAvailable = Duration.between(now, lastClaim.plus(7, ChronoUnit.DAYS));
            
            long days = timeUntilAvailable.toDays();
            long hours = timeUntilAvailable.toHours() % 24;
            
            lore.add("§cDu hast deine wöchentliche Belohnung bereits abgeholt.");
            lore.add("");
            lore.add("§7Verfügbar in: §e" + days + "d " + hours + "h");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a monthly reward item
     * @param available Whether the reward is available
     * @param lastClaim The last claim time
     * @return The item
     */
    private ItemStack createMonthlyRewardItem(boolean available, LocalDateTime lastClaim) {
        Material material = available ? Material.EMERALD : Material.EMERALD_ORE;
        String displayName = available ? "§a§lMonatliche Belohnung" : "§c§lMonatliche Belohnung (nicht verfügbar)";
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        if (available) {
            lore.add("§7Klicke, um deine monatliche Belohnung abzuholen!");
            lore.add("");
            lore.add("§7Belohnung: §e" + getRewardAmount("monthly") + " " + getCurrencySymbol());
        } else {
            LocalDateTime now = LocalDateTime.now();
            Duration timeUntilAvailable = Duration.between(now, lastClaim.plus(30, ChronoUnit.DAYS));
            
            long days = timeUntilAvailable.toDays();
            
            lore.add("§cDu hast deine monatliche Belohnung bereits abgeholt.");
            lore.add("");
            lore.add("§7Verfügbar in: §e" + days + " Tagen");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a streak info item
     * @param streakDays The number of streak days
     * @return The item
     */
    private ItemStack createStreakInfoItem(int streakDays) {
        Material material = Material.CLOCK;
        String displayName = "§6§lStreak: §e" + streakDays + " Tage";
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<>();
        if (streakDays > 0) {
            double streakBonusPerDay = plugin.getConfigManager().getConfig("config.yml")
                    .getDouble("rewards.streak-bonus-per-day", 10.0);
            double maxStreakBonus = plugin.getConfigManager().getConfig("config.yml")
                    .getDouble("rewards.max-streak-bonus", 500.0);
            double streakBonus = Math.min(streakDays * streakBonusPerDay, maxStreakBonus);
            
            lore.add("§7Du hast einen Streak von §e" + streakDays + " Tagen§7!");
            lore.add("");
            lore.add("§7Bonus: §e" + String.format("%.0f", streakBonus) + " " + getCurrencySymbol());
            lore.add("");
            lore.add("§7Hole deine tägliche Belohnung ab,");
            lore.add("§7um deinen Streak zu halten!");
        } else {
            lore.add("§7Du hast noch keinen Streak.");
            lore.add("");
            lore.add("§7Hole deine tägliche Belohnung ab,");
            lore.add("§7um einen Streak zu starten!");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Create a decorative item
     * @param material The material
     * @param displayName The display name
     * @return The item
     */
    private ItemStack createDecorativeItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        
        return item;
    }
    
    /**
     * Get the reward amount for a reward type
     * @param type The reward type
     * @return The reward amount
     */
    private double getRewardAmount(String type) {
        return plugin.getConfigManager().getConfig("config.yml")
                .getDouble("rewards." + type + "-reward", 100.0);
    }
    
    /**
     * Get the currency symbol
     * @return The currency symbol
     */
    private String getCurrencySymbol() {
        return plugin.getConfigManager().getConfig("config.yml")
                .getString("economy.currency-symbol", "$");
    }
    
    /**
     * Handle inventory click event
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8[§6Belohnungen§8]")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                Player player = (Player) event.getWhoClicked();
                String itemName = event.getCurrentItem().getItemMeta().getDisplayName();
                
                if (itemName.contains("Tägliche Belohnung") && !itemName.contains("nicht verfügbar")) {
                    // Close inventory
                    player.closeInventory();
                    
                    // Claim daily reward
                    rewardsCommand.claimDailyReward(player);
                } else if (itemName.contains("Wöchentliche Belohnung") && !itemName.contains("nicht verfügbar")) {
                    // Close inventory
                    player.closeInventory();
                    
                    // Claim weekly reward
                    rewardsCommand.claimWeeklyReward(player);
                } else if (itemName.contains("Monatliche Belohnung") && !itemName.contains("nicht verfügbar")) {
                    // Close inventory
                    player.closeInventory();
                    
                    // Claim monthly reward
                    rewardsCommand.claimMonthlyReward(player);
                }
            }
        }
    }
}