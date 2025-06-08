package ch.retaxo.sumania.commands.rewards;

import ch.retaxo.sumania.Sumania;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Command to manage rewards
 */
public class RewardsCommand implements CommandExecutor {

    private final Sumania plugin;
    
    /**
     * Constructor
     * @param plugin The plugin instance
     */
    public RewardsCommand(Sumania plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        // Check if rewards are enabled
        if (!config.getBoolean("rewards.enabled", true)) {
            if (sender instanceof Player) {
                plugin.getAPI().getPlayerAPI().sendMessage(
                        (Player) sender,
                        "general.feature-disabled",
                        null
                );
            } else {
                sender.sendMessage("§cBelohnungen sind deaktiviert.");
            }
            
            return true;
        }
        
        // Check if command is run by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern verwendet werden.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("sumania.rewards")) {
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.no-permission",
                    null
            );
            
            return true;
        }
        
        if (args.length == 0) {
            // Show rewards info
            return showRewardsInfo(player);
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("daily") || args[0].equalsIgnoreCase("täglich")) {
                // Claim daily reward
                return claimDailyReward(player);
            } else if (args[0].equalsIgnoreCase("weekly") || args[0].equalsIgnoreCase("wöchentlich")) {
                // Claim weekly reward
                return claimWeeklyReward(player);
            } else if (args[0].equalsIgnoreCase("monthly") || args[0].equalsIgnoreCase("monatlich")) {
                // Claim monthly reward
                return claimMonthlyReward(player);
            } else if (args[0].equalsIgnoreCase("info")) {
                // Show rewards info
                return showRewardsInfo(player);
            }
        }
        
        // Show usage
        Map<String, String> replacements = new HashMap<>();
        replacements.put("usage", "/rewards [daily|weekly|monthly|info]");
        
        plugin.getAPI().getPlayerAPI().sendMessage(
                player,
                "general.invalid-args",
                replacements
        );
        
        return true;
    }
    
    /**
     * Show rewards info
     * @param player The player to show info to
     * @return True if the info was shown successfully
     */
    private boolean showRewardsInfo(Player player) {
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
                    
                    // Send rewards info
                    sendRewardInfo(player, "daily", dailyClaim, dailyAvailable);
                    sendRewardInfo(player, "weekly", weeklyClaim, weeklyAvailable);
                    sendRewardInfo(player, "monthly", monthlyClaim, monthlyAvailable);
                    
                    // Send streak info
                    if (streakDays > 0) {
                        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
                        boolean streakEnabled = config.getBoolean("rewards.streak-enabled", true);
                        
                        if (streakEnabled) {
                            double streakBonusPerDay = config.getDouble("rewards.streak-bonus-per-day", 10.0);
                            double maxStreakBonus = config.getDouble("rewards.max-streak-bonus", 500.0);
                            double streakBonus = Math.min(streakDays * streakBonusPerDay, maxStreakBonus);
                            
                            Map<String, String> replacements = new HashMap<>();
                            replacements.put("days", String.valueOf(streakDays));
                            replacements.put("bonus", String.format("%.0f", streakBonus));
                            replacements.put("currency", config.getString("economy.currency-symbol", "$"));
                            
                            plugin.getAPI().getPlayerAPI().sendMessage(
                                    player,
                                    "rewards.streak-bonus",
                                    replacements
                            );
                        }
                    }
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Abrufen von Belohnungsinformationen: " + e.getMessage());
            e.printStackTrace();
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.database-error",
                    null
            );
            
            return false;
        }
    }
    
    /**
     * Send reward info to a player
     * @param player The player to send info to
     * @param type The reward type
     * @param lastClaim The last claim time
     * @param available Whether the reward is available
     */
    private void sendRewardInfo(Player player, String type, LocalDateTime lastClaim, boolean available) {
        FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
        
        if (available) {
            // Reward is available
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "rewards." + type + "-available",
                    null
            );
        } else {
            // Reward is not available
            LocalDateTime now = LocalDateTime.now();
            Duration timeUntilAvailable = Duration.ZERO;
            
            if (type.equals("daily")) {
                timeUntilAvailable = Duration.between(now, lastClaim.plus(1, ChronoUnit.DAYS));
            } else if (type.equals("weekly")) {
                timeUntilAvailable = Duration.between(now, lastClaim.plus(7, ChronoUnit.DAYS));
            } else if (type.equals("monthly")) {
                timeUntilAvailable = Duration.between(now, lastClaim.plus(30, ChronoUnit.DAYS));
            }
            
            long hours = timeUntilAvailable.toHours();
            long minutes = timeUntilAvailable.toMinutes() % 60;
            
            String timeString = hours + "h " + minutes + "m";
            
            Map<String, String> replacements = new HashMap<>();
            replacements.put("time", timeString);
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "rewards.already-claimed-" + type,
                    replacements
            );
        }
    }
    
    /**
     * Claim a daily reward
     * @param player The player claiming the reward
     * @return True if the reward was claimed successfully
     */
    private boolean claimDailyReward(Player player) {
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Check if daily reward is available
            String query = "SELECT daily_claim, streak_days, last_streak_update FROM " + 
                    tablePrefix + "rewards WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    LocalDateTime now = LocalDateTime.now();
                    Timestamp lastDailyClaim = null;
                    int streakDays = 0;
                    Timestamp lastStreakUpdate = null;
                    
                    if (rs.next()) {
                        lastDailyClaim = rs.getTimestamp("daily_claim");
                        streakDays = rs.getInt("streak_days");
                        lastStreakUpdate = rs.getTimestamp("last_streak_update");
                    }
                    
                    // Check if daily reward is available
                    if (lastDailyClaim != null && 
                            lastDailyClaim.toLocalDateTime().until(now, ChronoUnit.HOURS) < 24) {
                        // Daily reward is not available
                        Duration timeUntilAvailable = Duration.between(
                                now, 
                                lastDailyClaim.toLocalDateTime().plus(1, ChronoUnit.DAYS)
                        );
                        
                        long hours = timeUntilAvailable.toHours();
                        long minutes = timeUntilAvailable.toMinutes() % 60;
                        
                        String timeString = hours + "h " + minutes + "m";
                        
                        Map<String, String> replacements = new HashMap<>();
                        replacements.put("time", timeString);
                        
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "rewards.already-claimed-daily",
                                replacements
                        );
                        
                        return false;
                    }
                    
                    // Update streak
                    FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
                    boolean streakEnabled = config.getBoolean("rewards.streak-enabled", true);
                    double streakBonusPerDay = config.getDouble("rewards.streak-bonus-per-day", 10.0);
                    double maxStreakBonus = config.getDouble("rewards.max-streak-bonus", 500.0);
                    
                    if (streakEnabled && lastStreakUpdate != null) {
                        LocalDateTime lastUpdate = lastStreakUpdate.toLocalDateTime();
                        
                        // Check if streak should be continued or reset
                        if (lastUpdate.until(now, ChronoUnit.HOURS) <= 48) {
                            // Continue streak
                            streakDays++;
                        } else {
                            // Reset streak
                            streakDays = 1;
                        }
                    } else {
                        // Start streak
                        streakDays = 1;
                    }
                    
                    // Calculate reward amount
                    double rewardAmount = config.getDouble("rewards.daily.amount", 100.0);
                    double streakBonus = Math.min(streakDays * streakBonusPerDay, maxStreakBonus);
                    
                    if (streakEnabled) {
                        rewardAmount += streakBonus;
                    }
                    
                    // Update or insert reward data
                    String upsertQuery;
                    
                    if (lastDailyClaim == null) {
                        // Insert new record
                        upsertQuery = "INSERT INTO " + tablePrefix + "rewards " +
                                "(uuid, daily_claim, streak_days, last_streak_update) VALUES (?, ?, ?, ?)";
                    } else {
                        // Update existing record
                        upsertQuery = "UPDATE " + tablePrefix + "rewards SET " +
                                "daily_claim = ?, streak_days = ?, last_streak_update = ? WHERE uuid = ?";
                    }
                    
                    try (PreparedStatement upsertStmt = conn.prepareStatement(upsertQuery)) {
                        Timestamp nowTimestamp = Timestamp.valueOf(now);
                        
                        if (lastDailyClaim == null) {
                            upsertStmt.setString(1, player.getUniqueId().toString());
                            upsertStmt.setTimestamp(2, nowTimestamp);
                            upsertStmt.setInt(3, streakDays);
                            upsertStmt.setTimestamp(4, nowTimestamp);
                        } else {
                            upsertStmt.setTimestamp(1, nowTimestamp);
                            upsertStmt.setInt(2, streakDays);
                            upsertStmt.setTimestamp(3, nowTimestamp);
                            upsertStmt.setString(4, player.getUniqueId().toString());
                        }
                        
                        upsertStmt.executeUpdate();
                    }
                    
                    // Give reward
                    plugin.getAPI().getEconomyAPI().depositMoney(player, rewardAmount);
                    
                    // Send confirmation
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("amount", String.format("%.0f", rewardAmount));
                    replacements.put("currency", config.getString("economy.currency-symbol", "$"));
                    
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "rewards.daily-claimed",
                            replacements
                    );
                    
                    // Send streak info
                    if (streakEnabled && streakDays > 1) {
                        replacements = new HashMap<>();
                        replacements.put("days", String.valueOf(streakDays));
                        replacements.put("bonus", String.format("%.0f", streakBonus));
                        replacements.put("currency", config.getString("economy.currency-symbol", "$"));
                        
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "rewards.streak-bonus",
                                replacements
                        );
                    }
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Beanspruchen der täglichen Belohnung: " + e.getMessage());
            e.printStackTrace();
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.database-error",
                    null
            );
            
            return false;
        }
    }
    
    /**
     * Claim a weekly reward
     * @param player The player claiming the reward
     * @return True if the reward was claimed successfully
     */
    private boolean claimWeeklyReward(Player player) {
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Check if weekly reward is available
            String query = "SELECT weekly_claim FROM " + tablePrefix + "rewards WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    LocalDateTime now = LocalDateTime.now();
                    Timestamp lastWeeklyClaim = null;
                    
                    if (rs.next()) {
                        lastWeeklyClaim = rs.getTimestamp("weekly_claim");
                    }
                    
                    // Check if weekly reward is available
                    if (lastWeeklyClaim != null && 
                            lastWeeklyClaim.toLocalDateTime().until(now, ChronoUnit.DAYS) < 7) {
                        // Weekly reward is not available
                        Duration timeUntilAvailable = Duration.between(
                                now, 
                                lastWeeklyClaim.toLocalDateTime().plus(7, ChronoUnit.DAYS)
                        );
                        
                        long days = timeUntilAvailable.toDays();
                        long hours = timeUntilAvailable.toHours() % 24;
                        
                        String timeString = days + "d " + hours + "h";
                        
                        Map<String, String> replacements = new HashMap<>();
                        replacements.put("time", timeString);
                        
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "rewards.already-claimed-weekly",
                                replacements
                        );
                        
                        return false;
                    }
                    
                    // Update or insert reward data
                    String upsertQuery;
                    
                    if (lastWeeklyClaim == null) {
                        // Insert new record
                        upsertQuery = "INSERT INTO " + tablePrefix + "rewards " +
                                "(uuid, weekly_claim) VALUES (?, ?) " +
                                "ON CONFLICT(uuid) DO UPDATE SET weekly_claim = excluded.weekly_claim";
                    } else {
                        // Update existing record
                        upsertQuery = "UPDATE " + tablePrefix + "rewards SET weekly_claim = ? WHERE uuid = ?";
                    }
                    
                    try (PreparedStatement upsertStmt = conn.prepareStatement(upsertQuery)) {
                        Timestamp nowTimestamp = Timestamp.valueOf(now);
                        
                        if (lastWeeklyClaim == null) {
                            upsertStmt.setString(1, player.getUniqueId().toString());
                            upsertStmt.setTimestamp(2, nowTimestamp);
                        } else {
                            upsertStmt.setTimestamp(1, nowTimestamp);
                            upsertStmt.setString(2, player.getUniqueId().toString());
                        }
                        
                        upsertStmt.executeUpdate();
                    }
                    
                    // Give reward
                    FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
                    double rewardAmount = config.getDouble("rewards.weekly.amount", 500.0);
                    
                    plugin.getAPI().getEconomyAPI().depositMoney(player, rewardAmount);
                    
                    // Send confirmation
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("amount", String.format("%.0f", rewardAmount));
                    replacements.put("currency", config.getString("economy.currency-symbol", "$"));
                    
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "rewards.weekly-claimed",
                            replacements
                    );
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Beanspruchen der wöchentlichen Belohnung: " + e.getMessage());
            e.printStackTrace();
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.database-error",
                    null
            );
            
            return false;
        }
    }
    
    /**
     * Claim a monthly reward
     * @param player The player claiming the reward
     * @return True if the reward was claimed successfully
     */
    private boolean claimMonthlyReward(Player player) {
        try {
            Connection conn = plugin.getConfigManager().getDbConnection();
            String tablePrefix = plugin.getConfigManager().getTablePrefix();
            
            // Check if monthly reward is available
            String query = "SELECT monthly_claim FROM " + tablePrefix + "rewards WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, player.getUniqueId().toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    LocalDateTime now = LocalDateTime.now();
                    Timestamp lastMonthlyClaim = null;
                    
                    if (rs.next()) {
                        lastMonthlyClaim = rs.getTimestamp("monthly_claim");
                    }
                    
                    // Check if monthly reward is available
                    if (lastMonthlyClaim != null && 
                            lastMonthlyClaim.toLocalDateTime().until(now, ChronoUnit.DAYS) < 30) {
                        // Monthly reward is not available
                        Duration timeUntilAvailable = Duration.between(
                                now, 
                                lastMonthlyClaim.toLocalDateTime().plus(30, ChronoUnit.DAYS)
                        );
                        
                        long days = timeUntilAvailable.toDays();
                        long hours = timeUntilAvailable.toHours() % 24;
                        
                        String timeString = days + "d " + hours + "h";
                        
                        Map<String, String> replacements = new HashMap<>();
                        replacements.put("time", timeString);
                        
                        plugin.getAPI().getPlayerAPI().sendMessage(
                                player,
                                "rewards.already-claimed-monthly",
                                replacements
                        );
                        
                        return false;
                    }
                    
                    // Update or insert reward data
                    String upsertQuery;
                    
                    if (lastMonthlyClaim == null) {
                        // Insert new record
                        upsertQuery = "INSERT INTO " + tablePrefix + "rewards " +
                                "(uuid, monthly_claim) VALUES (?, ?) " +
                                "ON CONFLICT(uuid) DO UPDATE SET monthly_claim = excluded.monthly_claim";
                    } else {
                        // Update existing record
                        upsertQuery = "UPDATE " + tablePrefix + "rewards SET monthly_claim = ? WHERE uuid = ?";
                    }
                    
                    try (PreparedStatement upsertStmt = conn.prepareStatement(upsertQuery)) {
                        Timestamp nowTimestamp = Timestamp.valueOf(now);
                        
                        if (lastMonthlyClaim == null) {
                            upsertStmt.setString(1, player.getUniqueId().toString());
                            upsertStmt.setTimestamp(2, nowTimestamp);
                        } else {
                            upsertStmt.setTimestamp(1, nowTimestamp);
                            upsertStmt.setString(2, player.getUniqueId().toString());
                        }
                        
                        upsertStmt.executeUpdate();
                    }
                    
                    // Give reward
                    FileConfiguration config = plugin.getConfigManager().getConfig("config.yml");
                    double rewardAmount = config.getDouble("rewards.monthly.amount", 2000.0);
                    
                    plugin.getAPI().getEconomyAPI().depositMoney(player, rewardAmount);
                    
                    // Send confirmation
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("amount", String.format("%.0f", rewardAmount));
                    replacements.put("currency", config.getString("economy.currency-symbol", "$"));
                    
                    plugin.getAPI().getPlayerAPI().sendMessage(
                            player,
                            "rewards.monthly-claimed",
                            replacements
                    );
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Beanspruchen der monatlichen Belohnung: " + e.getMessage());
            e.printStackTrace();
            
            plugin.getAPI().getPlayerAPI().sendMessage(
                    player,
                    "general.database-error",
                    null
            );
            
            return false;
        }
    }
}